---
title: "Download RDF4J"
toc: true
---

You can either retrieve RDF4J via Apache Maven, or download the SDK or onejar directly.

## RDF4J 4.1.0 (latest)

RDF4J 4.1.0 is our latest stable release. It requires Java 11 minimally.
For details on what’s new and how to upgrade, see the [release and upgrade notes](/release-notes/4.1.0).

- [RDF4J 4.1.0 SDK (zip)](http://www.eclipse.org/downloads/download.php?file=/rdf4j/eclipse-rdf4j-4.1.0-sdk.zip)<br/>
  Full Eclipse RDF4J SDK, containing all libraries, RDF4J Server, Workbench, and Console applications, and Javadoc API.

- [RDF4J 4.1.0 onejar](http://www.eclipse.org/downloads/download.php?file=/rdf4j/eclipse-rdf4j-4.1.0-onejar.jar)<br/>
  Single jar file for easy inclusion of the full RDF4J toolkit in your Java project.

- [RDF4J artifacts](https://search.maven.org/search?q=org.eclipse.rdf4j) on the [Maven Central Repository](http://search.maven.org/)

### Apache Maven

You can include RDF4J as a Maven dependency in your Java project by including the following BOM (Bill-of-Materials):

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.eclipse.rdf4j</groupId>
            <artifactId>rdf4j-bom</artifactId>
            <version>4.1.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

RDF4J is a multi-module project, you can pick and choose which libraries you need. To include the full project, simply import the following dependency:

```xml
<dependency>
    <groupId>org.eclipse.rdf4j</groupId>
    <artifactId>rdf4j-storage</artifactId>
    <type>pom</type>
</dependency>
```

See the [Setup instructions](/documentation/programming/setup) in the
[Programmer’s documentation](/documentation/) for more details on Maven and
which artifacts RDF4J provides.


## Older releases

### RDF4J 4.0

- [RDF4J 4.0.4 SDK (zip)](http://www.eclipse.org/downloads/download.php?file=/rdf4j/eclipse-rdf4j-4.0.4-sdk.zip)
- [RDF4J 4.0.4 onejar](http://www.eclipse.org/downloads/download.php?file=/rdf4j/eclipse-rdf4j-4.0.4-onejar.jar)


### RDF4J 3.7

- [RDF4J 3.7.7 SDK (zip)](http://www.eclipse.org/downloads/download.php?file=/rdf4j/eclipse-rdf4j-3.7.7-sdk.zip)
- [RDF4J 3.7.7 onejar](http://www.eclipse.org/downloads/download.php?file=/rdf4j/eclipse-rdf4j-3.7.7-onejar.jar)

### RDF4J 3.6

- [RDF4J 3.6.3 SDK (zip)](http://www.eclipse.org/downloads/download.php?file=/rdf4j/eclipse-rdf4j-3.6.3-sdk.zip)
- [RDF4J 3.6.3 onejar](http://www.eclipse.org/downloads/download.php?file=/rdf4j/eclipse-rdf4j-3.6.3-onejar.jar)

### RDF4J 3.5

- [RDF4J 3.5.1 SDK (zip)](http://www.eclipse.org/downloads/download.php?file=/rdf4j/eclipse-rdf4j-3.5.1-sdk.zip)
- [RDF4J 3.5.1 onejar](http://www.eclipse.org/downloads/download.php?file=/rdf4j/eclipse-rdf4j-3.5.1-onejar.jar)

## Source code and nightly builds

You can access the RDF4J source code directly from [our GitHub repositories](https://github.com/eclipse/rdf4j). Maven nightly snapshot builds for the main and develop branch are available from the [Sonatype snapshot repository](https://oss.sonatype.org/content/repositories/snapshots/org/eclipse/rdf4j/).

To include nightly snapshot builds in your project, add this repository to your project’s POM:

```xml
<repositories>
    <repository>
        <id>oss.sonatype.org-snapshot</id>
        <url>http://oss.sonatype.org/content/repositories/snapshots</url>
        <releases>
            <enabled>false</enabled>
        </releases>
        <snapshots>
            <enabled>true</enabled>
        </snapshots>
    </repository>
</repositories>
```

Then use RDF4J dependencies as normal, using \<version\>-SNAPSHOT as the version number.

## Archives

Old releases of OpenRDF Sesame (the predecessor of Eclipse RDF4J) can be found on [Sourceforge](http://sourceforge.net/projects/sesame).

## License

Eclipse RDF4J is licensed to you under the terms of the [Eclipse Distribution License (EDL), v1.0](https://eclipse.org/org/documents/edl-v10.php).
