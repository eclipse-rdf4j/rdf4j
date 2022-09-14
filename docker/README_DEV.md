# Eclipse RDF4J server and workbench

Documentation for building and distributing the Server and Workbench image

## Getting started

Run `./run.sh` to build and start the server and workbench.

## Full build
Before building the docker image we need to get hold of the WAR files. These are built when 
the maven project is assembled and packed in the sdk ZIP-file under `assembly/target`.

The Dockerfile expects that the sdk ZIP-file is copied and renamed as `ignore/rdf4j.zip`.

Building the docker image can be done with a docker command directly, but it's easier to use 
docker-compose instead. 

Once the image is built it needs to be tagged as `eclipse/rdf4j-workbench:${maven.project.version}`.

The simplest way to accomplish all of the above is to run the `build.sh` script.

## Running up the docker container

Use docker-compose to run up the container. This uses the `docker-compose.yml` file to start
a container with the port mapped to 8080 and persistence using docker volumes.

`docker-compose up -d`

### Other useful commands

#### Stop
`docker-compose stop`

#### Follow logs
`docker-compose logs -f`

#### Show all logs
`docker-compose logs --tail="all"`

#### Start containers in foreground (prints logs and stops containers with Ctrl-C)
`docker-compose up`

#### Stop and remove all containers and delete all my volumes (for all containers and all volumes)
`docker stop $(docker ps -a -q); docker rm $(docker ps -a -q); docker system prune -f --volumes`

## Deploy

 * login: `docker login --username=yourhubusername --email=youremail@company.com`
 * push: `docker push eclipse/rdf4j-workbench:VERSION_GOES_HERE`
   * VERSION_GOES_HERE is the version (tag) you want to push.
   * to list all versions you have locally: `docker images | grep "eclipse/rdf4j-workbench"` 
