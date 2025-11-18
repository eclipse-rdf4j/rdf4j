RDF4J Server Boot Distribution
==============================

Usage
-----
1. Unzip the distribution archive.
2. From the unzip root, run `bin/rdf4j-server.sh`.
3. Open `http://localhost:8080/rdf4j-workbench/` (or the port you configure).

Directory layout
----------------
- `bin/` : executable launcher script
- `config/` : `logback-spring.xml` and `application.properties` defaults
- `lib/` : the Spring Boot fat jar
- `data/` : RDF4J app data (repositories, configs, uploads)
- `logs/` : logback rolling files

Configuration knobs
-------------------
Environment variables (can also be exported in the shell before launching):
- `JAVA_CMD` – Java binary to use (default `java`)
- `RDF4J_JVM_MIN_HEAP` – JVM `-Xms` (default `512m`)
- `RDF4J_JVM_MAX_HEAP` – JVM `-Xmx` (default `2g`)
- `RDF4J_JAVA_OPTS` – extra JVM options appended before `JAVA_OPTS`
- `JAVA_OPTS` – final JVM options appended (e.g., debugging flags)
- `RDF4J_DATA_DIR` – overrides the RDF4J app data base directory (default `<dist>/data`)
- `RDF4J_LOG_DIR` – overrides the log directory (default `<dist>/logs`)
- `RDF4J_LOGGING_CONFIG` – alternate logback XML file (default `<dist>/config/logback-spring.xml`)
- `RDF4J_SPRING_CONFIG` – alternate Spring Boot `application.properties` file (default `<dist>/config/application.properties`)
- `RDF4J_SERVER_PORT` – HTTP port injected into `application.properties` (default `8080`)

`config/application.properties`
-------------------------------
The launcher passes `--spring.config.additional-location` so Spring Boot loads the distribution's
`config/application.properties` in addition to the defaults baked into the jar. The file ships with:

```
server.port=${RDF4J_SERVER_PORT:8080}
```

Edit the file or export `RDF4J_SERVER_PORT` to change the HTTP port. Any other standard Spring Boot
properties can be added to this file and will be honored on startup.
