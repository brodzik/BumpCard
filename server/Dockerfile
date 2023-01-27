FROM python:3.11.1-alpine3.17

WORKDIR /bump-card

COPY requirements.txt requirements.txt
RUN pip3 install -r requirements.txt

COPY main.py app.py

CMD [ "python3", "-m", "flask", "run", "--host=0.0.0.0" ]
