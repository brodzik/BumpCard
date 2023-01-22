import base64
import secrets
import bcrypt
from flask import (Flask, jsonify, redirect, render_template, request, session,
                   url_for)
from flask_sqlalchemy import SQLAlchemy
from geopy import distance

TIME_THRESHOLD = 10000  # milliseconds
DISTANCE_THRESHOLD = 50  # meters

app = Flask(__name__)
app.config["SQLALCHEMY_DATABASE_URI"] = "sqlite:///db.sqlite"
db = SQLAlchemy(app)


class User(db.Model):
    __tablename__ = "users"

    id = db.Column(db.Integer, primary_key=True)
    username = db.Column(db.String(100), unique=True, nullable=False)
    password = db.Column(db.String(100), nullable=False)
    api_key = db.Column(db.String(100), unique=True, nullable=False)
    first_name = db.Column(db.String(100))
    last_name = db.Column(db.String(100))
    headline = db.Column(db.String(100))
    email = db.Column(db.String(100))
    phone = db.Column(db.String(20))

    bumps = db.relationship("Bump")

    def __init__(self, username, password):
        self.username = username
        self.password = bcrypt.hashpw(password.encode("utf-8"), bcrypt.gensalt()).decode("utf-8")
        self.api_key = base64.b64encode(secrets.token_bytes(64)).decode("utf-8")

    def verify_password(self, password):
        return bcrypt.checkpw(password.encode("utf-8"), self.password.encode("utf-8"))


class Bump(db.Model):
    __tablename__ = "bumps"

    user_id = db.Column(db.Integer, db.ForeignKey("users.id"), primary_key=True)
    timestamp = db.Column(db.Integer, primary_key=True)
    latitude = db.Column(db.Float, nullable=False)
    longitude = db.Column(db.Float, nullable=False)
    magnitude = db.Column(db.Float, nullable=False)

    user = db.relationship("User")

    def __init__(self, user_id, timestamp, latitude, longitude, magnitude):
        self.user_id = user_id
        self.timestamp = timestamp
        self.latitude = latitude
        self.longitude = longitude
        self.magnitude = magnitude
        self.match_id = None

    def update(self, timestamp, latitude, longitude, magnitude):
        self.timestamp = timestamp
        self.latitude = latitude
        self.longitude = longitude
        self.magnitude = magnitude

    def as_dict(self):
        return {c.name: getattr(self, c.name) for c in self.__table__.columns}


class Connection(db.Model):
    __tablename__ = "connections"

    user1_id = db.Column(db.Integer, db.ForeignKey("users.id"), primary_key=True)
    user2_id = db.Column(db.Integer, db.ForeignKey("users.id"), primary_key=True)
    active = db.Column(db.Boolean, nullable=False)  # `user1` can allow `user2` to read `user1` data

    user1 = db.relationship("User", foreign_keys=user1_id)
    user2 = db.relationship("User", foreign_keys=user2_id)

    def __init__(self, user1_id, user2_id):
        self.user1_id = user1_id
        self.user2_id = user2_id
        self.active = False

    def as_dict(self):
        return {c.name: getattr(self, c.name) for c in self.__table__.columns}


with app.app_context():
    db.drop_all()
    db.create_all()


@app.route("/register", methods=["POST"])
def register():
    username = request.form.get("username")
    password = request.form.get("password")

    user = User(username, password)
    db.session.add(user)
    db.session.commit()

    return jsonify(api_key=user.api_key)


@app.route("/info", methods=["GET", "POST"], defaults={"user_id": None})
@app.route("/info/<int:user_id>", methods=["GET"])
def basic_info(user_id):
    if request.method == "GET":
        api_key = request.headers.get("api_key")
        user = User.query.filter_by(api_key=api_key).first()

        if (user_id is None) or (user_id == user.id):  # Return my data
            return jsonify(first_name=user.first_name, last_name=user.last_name, headline=user.headline, email=user.email, phone=user.phone)

        # conn = Connection.query.filter_by(user1_id=user_id, user2_id=user.id, active=True).first()
        conn = Connection.query.filter_by(user1_id=user_id, user2_id=user.id).first()

        # if conn:  # Return stranger's data if I have permission
        return jsonify(first_name=conn.user1.first_name, last_name=conn.user1.last_name, headline=conn.user1.headline, email=conn.user1.email, phone=conn.user1.phone)

    if request.method == "POST":
        api_key = request.headers.get("api_key")
        user = User.query.filter_by(api_key=api_key).first()
        user.first_name = request.form.get("first_name")
        user.last_name = request.form.get("last_name")
        user.headline = request.form.get("headline")
        user.email = request.form.get("email")
        user.phone = request.form.get("phone")
        db.session.commit()

        return jsonify(msg="User information has been updated.")

    return "", 404


@app.route("/info/basic/<int:user_id>", methods=["GET"])
def info(user_id):
    user = User.query.filter_by(user_id).first()
    return jsonify(first_name=user.first_name, last_name=user.last_name)


@app.route("/bump", methods=["GET", "POST"])
def bump():
    if request.method == "GET":
        api_key = request.headers.get("api_key")
        user = User.query.filter_by(api_key=api_key).first()

        return jsonify(bumps=[b.as_dict() for b in user.bumps])

    if request.method == "POST":
        api_key = request.headers.get("api_key")
        user = User.query.filter_by(api_key=api_key).first()

        user_id = user.id
        timestamp = request.form.get("timestamp")
        latitude = request.form.get("latitude")
        longitude = request.form.get("longitude")
        magnitude = request.form.get("magnitude")

        bump = Bump(user_id, timestamp, latitude, longitude, magnitude)
        db.session.add(bump)
        db.session.commit()

        bump1 = bump

        bump_candidates = Bump.query\
            .filter(Bump.user_id != bump1.user_id)\
            .filter(Bump.timestamp >= bump1.timestamp - TIME_THRESHOLD)\
            .filter(Bump.timestamp <= bump1.timestamp + TIME_THRESHOLD).all()

        bump_candidates = [(b, distance.distance((bump1.latitude, bump1.longitude), (b.latitude, b.longitude)).m) for b in bump_candidates]

        if len(bump_candidates) > 0:
            bump2, dist = min(bump_candidates, key=lambda b: b[1])
            if dist < DISTANCE_THRESHOLD:
                # Create an inactive bidirectional connection
                db.session.add(Connection(bump1.user_id, bump2.user_id))
                db.session.add(Connection(bump2.user_id, bump1.user_id))
                db.session.commit()

                return jsonify(msg="Registered bump and created connection.")

        return jsonify(msg="Registered bump.")


@app.route("/connection", methods=["GET"])
def connection():
    api_key = request.headers.get("api_key")
    user = User.query.filter_by(api_key=api_key).first()
    conns = Connection.query.filter(Connection.user1_id == user.id).all()

    return jsonify(connection=[x.as_dict() for x in conns])


@app.route("/connection/allow/<int:user2_id>", methods=["POST"])
def connection_allow(user2_id):
    api_key = request.headers.get("api_key")
    user = User.query.filter_by(api_key=api_key).first()
    conn = Connection.query.filter_by(user1_id=user.id, user2_id=user2_id).first()
    conn.active = True
    db.session.commit()

    return jsonify(msg=f"Connection with user {user2_id} has been allowed.")


@app.route("/connection/allowed", methods=["GET"])
def connection_allowed():
    api_key = request.headers.get("api_key")
    user = User.query.filter_by(api_key=api_key).first()
    conns = Connection.query.filter_by(user1_id=user.id, active=True).all()

    return jsonify(allowed_connection=[x.as_dict() for x in conns])


@app.route("/connection/delete/<int:user2_id>", methods=["POST"])
def connection_delete(user2_id):
    api_key = request.headers.get("api_key")
    user = User.query.filter_by(api_key=api_key).first()
    conn = Connection.query.filter_by(user1_id=user.id, user2_id=user2_id).first()
    db.session.delete(conn)
    db.session.commit()

    return jsonify(msg="Connection deleted.")


if __name__ == "__main__":
    app.run(host='0.0.0.0')
