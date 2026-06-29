---
title: "Testing"
toc: true
autonumbering: true
---

## Testing the RDF4J Workbench Locally

The `docker/` directory contains scripts and a Docker Compose setup for building and running RDF4J Server and Workbench locally. All commands below are run from that directory.

```sh
cd docker
```

### Quick start

The simplest way to build and start everything in one step:

```sh
./run.sh
```

This script will:

1. Run `build.sh` to build the Maven project and produce the SDK ZIP
2. Copy the ZIP into `ignore/rdf4j.zip` (required by the Dockerfile)
3. Build the Docker image via `docker compose build`
4. Start the container with `docker compose up`
5. Wait until the server is ready and print the Workbench URL

Once ready, the output will confirm:

```
Workbench is available at http://localhost:8080/rdf4j-workbench
```

### Choosing the application server

By default `tomcat` is used. To use Jetty instead, set the `APP_SERVER` environment variable before running the scripts:

```sh
APP_SERVER=jetty ./run.sh
```

This controls which Dockerfile (`Dockerfile-tomcat` or `Dockerfile-jetty`) and which log volume are used.

### Skipping the Maven build

If you only want to rebuild the Docker image, you can set `SKIP_BUILD` **only if**
`ignore/rdf4j.zip` already exists in the `docker/` directory (for example, from a previous
`build.sh` or `run.sh` execution). Skipping the build does not create or refresh that ZIP:

```sh
SKIP_BUILD=1 ./run.sh
```

### Access

| Application     | URL                                    |
|-----------------|----------------------------------------|
| RDF4J Server    | http://localhost:8080/rdf4j-server     |
| RDF4J Workbench | http://localhost:8080/rdf4j-workbench  |

### Useful Docker Compose commands

Follow logs in real time:

```sh
docker compose logs -f
```

Show all logs:

```sh
docker compose logs --tail="all"
```

Stop the container (keep volumes):

```sh
docker compose stop
```

### Stopping and cleaning up

To stop the container and remove the image and all volumes:

```sh
./shutdown.sh
```
