#!/usr/bin/env bash
set -e

# current working directory
CURRENT=`pwd`

# clean "ignore" directory
cd ignore
rm -f *.zip
cd $CURRENT

# remove assembly/target since this is not removed by mvn clean
rm -rf ../../../../target/

# go to root of project and do clean, format, install and assembly
cd ../../../../..
mvn clean
mvn formatter:format
mvn -Passembly install -DskipTests
cd $CURRENT

# find .zip file
ZIP=`find ../../../../target/*.zip`
echo $ZIP

# copy zip file into rdf4j.zip
cp $ZIP ./ignore/rdf4j.zip

# build
docker-compose build

