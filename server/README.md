# BumpCard: Server

## Run with Docker

### Build an image

```
docker image build -t bump-card .
```

### Run the container

```
docker container run -p 5000:5000 bump-card
```

### Check the logs

```
docker container logs -f <container id>
```

You can find your `container id` with `docker container ls`.
