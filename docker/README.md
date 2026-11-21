# Eclipse RDF4J server and workbench

Docker image for RDF4J server and workbench, based on a Tomcat 10 image running JDK 25.

A slightly modified web.mxl is used for Tomcat to fix a known UTF-8 issue 
(see also http://docs.rdf4j.org/server-workbench-console)

## Port

By default port 8080 is exposed.

## Volumes
 
  * RDF4J data will be stored in `/var/rdf4j`
  * Server logs go to `/usr/local/tomcat/logs` or `/usr/local/jetty/logs`

## Running the docker container 

The default java runtime options (-Xmx2g) can be changed by setting the 
`JAVA_OPTS` environment variable.

To avoid data loss between restarts of the docker container, 
the exposed volumes can be mapped to existing (and writable) directories on the host. 

Example:
```
docker run -d -p 127.0.0.1:8080:8080 -e JAVA_OPTS="-Xms1g -Xmx4g" \
	-v /home/rdf4j/data:/var/rdf4j -v /home/rdf4j/logs:/usr/local/tomcat/logs \
    eclipse/rdf4j-workbench
```

The workbench will be accessible via http://localhost:8080/rdf4j-workbench

The server will be accessible via http://localhost:8080/rdf4j-server


In order to access the workbench and server from another machine, 
you will need to bind the docker container to `0.0.0.0` instead of `127.0.0.1`.

## Security

Please note that the RDF4J server in the docker container is not 
password-protected, nor is it configured to use HTTPS. 
Additional configuration may be required for running the container in 
production environments.

JSON-LD secure mode is enabled by default. When parsing JSON-LD from untrusted
sources, remote or local `@context` URLs are loaded only if they are in the
JSON-LD whitelist. Deployments that previously relied on unrestricted contexts
must add the required URLs to the `org.eclipse.rdf4j.rio.jsonld_whitelist`
system property (a JSON array of URLs). Use `RDF4J_OPTS` to pass this property
to either Docker image. For example:

```
docker run -e 'RDF4J_OPTS=-Dorg.eclipse.rdf4j.rio.jsonld_whitelist=["https://example.org/context.jsonld"]' \
    eclipse/rdf4j-workbench
```

If unrestricted context loading is required, set
`RDF4J_OPTS=-Dorg.eclipse.rdf4j.rio.jsonld_secure_mode=false` explicitly.
This also permits contexts to read local files and make arbitrary outbound
requests, so use it only with trusted input.
