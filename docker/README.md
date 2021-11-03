# Eclipse RDF4J server and workbench

Docker image for RDF4J server and workbench, based on the Tomcat 8.5 (JRE 11) image.

A slightly modified web.mxl is used to fix a known UTF-8 issue 
(see also http://docs.rdf4j.org/server-workbench-console)

## Port

By default port 8080 is exposed.

## Volumes
 
  * RDF4J data will be stored in `/var/rdf4j`
  * Tomcat server logs in `/usr/local/tomcat/logs`

## Running the docker container 

The default java runtime options (-Xmx2g) can be changed by setting the 
`JAVA_OPTS` environment variable.

To avoid data loss between restarts of the docker container, 
the exposed volumes can be mapped to existing directories on the host. 

Example:
```
docker run -d -p 127.0.0.1:8080:8080 -e JAVA_OPTS="-Xms1g -Xmx4g" \
	-v data:/var/rdf4j -v logs:/usr/local/tomcat/logs eclipse/rdf4j
```

To access your server from another machine you will need to bind to `0.0.0.0` 
instead of `127.0.0.1`.

The workbench will be accessible via http://localhost:8080/rdf4j-workbench

The server will be accessible via http://localhost:8080/rdf4j-server

## Security

Please note that the RDF4J server in the docker container is not 
password-protected, nor is it configured to use HTTPS. 
Additional configuration may be required for running the container in 
production environments.
