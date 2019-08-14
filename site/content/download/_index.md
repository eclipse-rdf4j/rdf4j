---
title: "Download rdf4j"
slug: "download"
layout: "doc"
hide_page_title: "true"
---

# Get rdf4j

You can either retrieve rdf4j via Apache Maven, or download the SDK or onejar directly.

# Download rdf4j 2.5.4 (latest)

rdf4j 2.5.4 is our latest stable release. It requires Java 8. For details on what’s new, see the [release notes](/release-notes/#2-5-4).

- [rdf4j 2.5.4 SDK (zip)](http://www.eclipse.org/downloads/download.php?file=/rdf4j/eclipse-rdf4j-2.5.4-sdk.zip)<br/>
  Full Eclipse rdf4j SDK, containing all libraries, rdf4j Server, Workbench, and Console applications, and Javadoc API.

- [rdf4j 2.5.4 onejar](http://www.eclipse.org/downloads/download.php?file=/rdf4j/eclipse-rdf4j-2.5.4-onejar.jar)<br/>
  Single jar file for easy inclusion of the full rdf4j toolkit in your Java project.

- [rdf4j artifacts](https://search.maven.org/search?q=org.eclipse.rdf4j) on the [Maven Central Repository](http://search.maven.org/)

# Using Maven Dependencies

You can include rdf4j as a Maven dependency in your Java project by including the following BOM (Bill-of-Materials):

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.eclipse.rdf4j</groupId>
                <artifactId>rdf4j-bom</artifactId>
                <version>2.5.4</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

rdf4j is a multi-module project, you can pick and choose which libraries you need. To include the full project, simply import the following dependency:

    <dependency>
      <groupId>org.eclipse.rdf4j</groupId>
      <artifactId>rdf4j-runtime</artifactId>
    </dependency>

See the Setup instructions in the Programmer’s documentation for more details on Maven and which artifacts RDF4J provides.

# Download rdf4j 3.0.0-M2 (milestone build)

rdf4j 3.0 is our upcoming major new release, milestone 2 is the second milestone build for this release, intended for 
early testing and feedback. Please note that this build is not intended for production usage.

- [rdf4j 3.0.0-M2 SDK (zip)](http://www.eclipse.org/downloads/download.php?file=/rdf4j/eclipse-rdf4j-3.0.0-M2-sdk.zip)<br/>
  Full Eclipse rdf4j SDK, containing all libraries, rdf4j Server, Workbench, and Console applications, and Javadoc API.

- [rdf4j 3.0.0-M2 onejar](http://www.eclipse.org/downloads/download.php?file=/rdf4j/eclipse-rdf4j-3.0.0-M2-onejar.jar)<br/>
  Single jar file for easy inclusion of the full rdf4j toolkit in your Java project.

- [rdf4j artifacts](https://search.maven.org/search?q=org.eclipse.rdf4j) on the [Maven Central Repository](http://search.maven.org/)

# Older releases

## rdf4j 2.4

- [rdf4j 2.4.6 SDK (zip)](http://www.eclipse.org/downloads/download.php?file=/rdf4j/eclipse-rdf4j-2.4.6-sdk.zip)
- [rdf4j 2.4.6 onejar](http://www.eclipse.org/downloads/download.php?file=/rdf4j/eclipse-rdf4j-2.4.6-onejar.jar)

## rdf4j 2.3

- [rdf4j 2.3.3 SDK (zip)](http://www.eclipse.org/downloads/download.php?file=/rdf4j/eclipse-rdf4j-2.3.3-sdk.zip)
- [rdf4j 2.3.3 onejar](http://www.eclipse.org/downloads/download.php?file=/rdf4j/eclipse-rdf4j-2.3.3-onejar.jar)

## rdf4j 2.2

- [rdf4j 2.2.4 SDK (zip)](http://www.eclipse.org/downloads/download.php?file=/rdf4j/eclipse-rdf4j-2.2.4-sdk.zip)
- [rdf4j 2.2.4 onejar](http://www.eclipse.org/downloads/download.php?file=/rdf4j/eclipse-rdf4j-2.2.4-onejar.jar)

# Source code and nightly builds

You can access the rdf4j source code directly from [our GitHub repositories](https://github.com/eclipse/rdf4j). Maven nightly snapshot builds for the master branch are available from the [Sonatype snapshot repository](https://oss.sonatype.org/content/repositories/snapshots/org/eclipse/rdf4j/).

To include nightly snapshot builds in your project, add this repository to your project’s POM:

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

Then use rdf4j dependencies as normal, using 2.5-SNAPSHOT as the version number.

# Archives

Old releases of OpenRDF Sesame (the predecessor of Eclipse rdf4j) can be found on [Sourceforge](http://sourceforge.net/projects/sesame).

# License

Eclipse rdf4j is licensed to you under the terms of the [Eclipse Distribution License (EDL), v1.0](https://eclipse.org/org/documents/edl-v10.php).
