#!/usr/bin/env bash
set -e

# current working directory
CURRENT=$(pwd)
cd ..
PROJECT_ROOT=$(pwd)
cd "$CURRENT"

# clean "ignore" directory
cd ignore
rm -f *.zip

cd "$PROJECT_ROOT"

# remove assembly/target since this is not removed by mvn clean
rm -rf assembly/target/

MVN_VERSION=$(xmllint --xpath "//*[local-name()='project']/*[local-name()='version']/text()" pom.xml)

#Clean, format and package
echo "Building with Maven"
mvn clean
mvn -T 2C formatter:format impsort:sort && mvn xml-format:xml-format
mvn -T 2C -Passembly,-use-sonatype-snapshots package -DskipTests -Dmaven.javadoc.skip=true -Dformatter.skip=true -Dimpsort.skip=true -Dxml-format.skip=true  -Djapicmp.skip -Denforcer.skip=true -Dbuildnumber.plugin.phase=none -Danimal.sniffer.skip=true

# find .zip file
ZIP=$(find assembly/target/*.zip)
echo "$ZIP"

# copy zip file into rdf4j.zip
cp "$ZIP" "${CURRENT}/ignore/rdf4j.zip"

cd "$CURRENT"

# build
echo "Building docker image"
docker-compose build

docker tag docker_rdf4j:latest eclipse/rdf4j-workbench:${MVN_VERSION}

echo "
Docker image tagged as:
  docker_rdf4j:latest
  eclipse/rdf4j-workbench:${MVN_VERSION}

To start the workbench and server:
    docker-compose up -d

Workbench will be available at http://localhost:8080/rdf4j-workbench
"
