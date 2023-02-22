# Eclipse RDF4J server and workbench

Documentation for building and distributing the docker image containing the Server and Workbench.

## Getting started

Run `./run.sh` to build and start the server and workbench. This will invoke the `build.sh` script.

### Full build (build.sh)

The simplest way to build the image and tag it, is to run the `build.sh` script.

This script will perform the following tasks:

1. Before building the docker image the WAR files need to be available. These are built when 
the maven project is assembled and the result is packed in the SDK ZIP-file under `assembly/target`.

2. The SDK ZIP-file is copied and renamed as `ignore/rdf4j.zip` (as required by the Dockerfile script).

3. Building the docker image can be done with a docker command directly, 
but the script takes an alternative approach and uses docker compose instead. 

4. Once the image is built, it is tagged as `eclipse/rdf4j-workbench:${maven.project.version}`.


### Running up the docker container (docker compose)

Use `docker compose` to run up the container. This uses the `docker-compose.yml` file to start
a container with the port mapped to 8080 and persistence using docker volumes.

`docker compose up -d`

#### Other useful docker compose commands

Stop:

`docker compose stop`

Follow logs:

`docker compose logs -f`

Show all logs:

`docker compose logs --tail="all"`

Start containers in foreground (prints logs and stops containers with Ctrl-C):

`docker compose up`

Stop and remove all containers and delete all my volumes (for all containers and all volumes):

`docker stop $(docker ps -a -q); docker rm $(docker ps -a -q); docker system prune -f --volumes`

List all versions on the local machine:

`docker images | grep "eclipse/rdf4j-workbench"`

## Deploy the image on hub.docker.com

The docker images on hub.docker.com are stored as part of the Eclipse organizational account. 

Since this account is managed separately by the Eclipse Foundation,
only a limited number of committers will be granted access by the EMO.

### Method 1: using the build script and docker push

Build the SDK ZIP file and docker image using the `build.sh` script mentioned above.
Both the Workbench and the server will be part of the same image.

Log into hub.docker.com:

`docker login --username=yourhubusername`

Push the image:

`docker push eclipse/rdf4j-workbench:VERSION_TAG`
 
`VERSION_TAG` is the version (tag) you want to push, e.g. `4.3.0`

Note that hub.docker.com does not update the `latest` tag automatically,
the newly created image has also to be tagged `latest` and pushed to hub.docker.com.

### Method 2: multi-platform docker image using buildx

Since the base image being used is available for multiple architectures,
it is quite easy to build a [multi-platform image](https://docs.docker.com/build/building/multi-platform/).
Currently the Workbench/Server image is made available for 64-bit AMD/Intel and ARM v8.

Check if [Docker Buildx](https://docs.docker.com/build/buildx/install/) is installed on your system.

Build the SDK ZIP file using the `build.sh` script mentioned above,
or download the SDK from https://rdf4j.org/download/ and store the ZIP as `ignore/rdf4j.zip`.

Log into hub.docker.com using your username and password.

`docker login --username=yourhubusername`

Build and push the image (note the `.` at the end of the command):

`docker buildx build --push --platform linux/arm64/v8,linux/amd64 --tag eclipse/rdf4j-workbench:VERSION_TAG .`

`VERSION_TAG` is the version (tag) you want to push, e.g. `4.3.0`
 
Note that hub.docker.com does not update the `latest` tag automatically,
the newly created image has also to be tagged `latest` and pushed to hub.docker.com.
