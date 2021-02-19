#!/usr/bin/env bash
set -e

# current working directory
CURRENT=$(pwd)

# clean "ignore" directory
cd ignore
rm -f *.zip
cd "$CURRENT"

# remove assembly/target since this is not removed by mvn clean
rm -rf ../../../../target/

# go to root of project and do clean, format, install and assembly
cd ../../../../..
MVN_VERSION=$(xmllint --xpath "//*[local-name()='project']/*[local-name()='version']/text()" pom.xml)

mvn clean
mvn -T 2 -Passembly install -DskipTests -Dmaven.javadoc.skip=true -Dformatter.skip=true -Dimpsort.skip=true -Dxml-format.skip=true  -Djapicmp.skip -Denforcer.skip=true -Dbuildnumber.plugin.phase=none -Danimal.sniffer.skip=true
cd "$CURRENT"

# find .zip file
ZIP=$(find ../../../../target/*.zip)
echo "$ZIP"

# copy zip file into rdf4j.zip
cp "$ZIP" ./ignore/rdf4j.zip

# build
docker-compose build

docker tag docker_rdf4j:latest eclipse/rdf4j-workbench:${MVN_VERSION}
