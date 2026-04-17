---
title: "Testing"
toc: true
autonumbering: true
---

## Testing the RDF4J Workbench Locally

### Build the project

```sh
mvn -Passembly package
cd assembly/target
unzip eclipse-rdf4j-*-sdk.zip
cd eclipse-rdf4j-*-SNAPSHOT
```


### Run the workbench in Docker

```sh
docker run -d \
    --name rdf4j \
    --platform linux/amd64 \
    --restart unless-stopped \
    -p 8080:8080 \
    -v "$(pwd)/war/rdf4j-server.war:/var/lib/jetty/webapps/rdf4j-server.war" \
    -v "$(pwd)/war/rdf4j-workbench.war:/var/lib/jetty/webapps/rdf4j-workbench.war" \
    -v rdf4j-data:/var/rdf4j \
    jetty:12.1-jdk25-alpine \
    --module=ee10-deploy
```

### Access

| Application | URL |
|---|---|
| RDF4J Server | http://localhost:8080/rdf4j-server |
| RDF4J Workbench | http://localhost:8080/rdf4j-workbench |
