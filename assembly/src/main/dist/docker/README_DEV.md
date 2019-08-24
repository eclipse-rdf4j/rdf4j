# Eclipse RDF4J server and workbench

Documentation for building and distributing the Server and Workbench image

## Full build
The simplest way to build RDF4J and the docker image is to run: `./build.sh`

This will
 - clean and build the project, including the one-jar zip file
 - build the docker image
 - tag the image with `eclipse/rdf4j-workbench:${maven.project.version}`
 
 To run the container, use: `docker-compose up -d`

## Deploy

 * login: `docker login --username=yourhubusername --email=youremail@company.com`
 * push: `docker push eclipse/rdf4j-workbench:VERSION_GOES_HERE`
   * VERSION_GOES_HERE is the version you want to push.
   * to list all versions you have locally: `docker images | grep "eclipse/rdf4j-workbench"` 