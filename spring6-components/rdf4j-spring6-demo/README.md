# RDF4J-Spring Demo

Small demo application for `rdf4j-spring`. 

The purpose of `rdf4j-spring` is to use an RDF4J repository as the data backend of a spring or spring boot application.

To run the demo, do 

```$bash
mvn spring-boot:run
```

The program writes to stdout and exits. The class [ArtDemoCli](src/main/java/org/eclipse/rdf4j/spring.demo/ArtDemoCli.java) is a good starting point for looking at the code. 

