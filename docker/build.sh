#!/usr/bin/env bash
set -e

# current working directory
CURRENT=$(pwd)
cd ..
PROJECT_ROOT=$(pwd)
MVN_VERSION=$(xmllint --xpath "//*[local-name()='project']/*[local-name()='version']/text()" pom.xml)

cd "$CURRENT"

if [ -z ${SKIP_BUILD+x} ]; then
  # clean "ignore" directory
  cd ignore
  rm -f *.zip

  cd "$PROJECT_ROOT"

  # remove assembly/target since this is not removed by mvn clean
  rm -rf assembly/target/

  #Clean, format and package
  echo "Building with Maven"
  mvn clean
  mvn -T 2C formatter:format impsort:sort && mvn xml-format:xml-format
  mvn install -DskipTests
  mvn -Passembly package -DskipTests -Dmaven.javadoc.skip=true -Dformatter.skip=true -Dimpsort.skip=true -Dxml-format.skip=true  -Djapicmp.skip -Denforcer.skip=true -Dbuildnumber.plugin.phase=none -Danimal.sniffer.skip=true

  # find .zip file
  ZIP=$(find assembly/target/*.zip)
  echo "$ZIP"

  # copy zip file into rdf4j.zip
  cp "$ZIP" "${CURRENT}/ignore/rdf4j.zip"

  cd "$CURRENT"
fi

# build
APP_SERVER=${APP_SERVER:-tomcat}
echo "Building docker image for ${APP_SERVER}"

docker compose build --pull --no-cache
docker tag docker-rdf4j:latest eclipse/rdf4j-workbench-${APP_SERVER}:${MVN_VERSION}

echo "
Docker image tagged as:
    docker-rdf4j:latest
    eclipse/rdf4j-workbench-${APP_SERVER}:${MVN_VERSION}

To start the workbench and server:
    docker compose up -d

Workbench will be available at http://localhost:8080/rdf4j-workbench
"
