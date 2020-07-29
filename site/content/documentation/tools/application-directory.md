---
title: "Application directory configuration"
toc: true
weight: 4
---
In this section we explain how to configure the application directory of the various RDF4J tools.
<!--more-->
[RDF4J Server, Workbench](/documentation/tools/server-workbench) and [Console](/documentation/tools/console/) all store configuration files and repository data in a single directory (with subdirectories). On Windows machines, this directory is `%APPDATA%\Rdf4j\` by default, where `%APPDATA%` is the application data directory of the user that runs the application. For example, in case the application runs under the ‘LocalService’ user account on Windows XP, the directory is `C:\Documents and Settings\LocalService\Application Data\Rdf4j\`. On Linux/UNIX, the default location is `$HOME/.Rdf4j/`, for example `/home/tomcat/.rdf4j/`. We will refer to this data directory as `[RDF4J_DATA]` in the rest of this manual.

The location of this data directory can be reconfigured using the Java system property `org.eclipse.rdf4j.appdata.basedir`. When you are using Tomcat as the servlet container then you can set this property using the `JAVA_OPTS` parameter, for example:

    set JAVA_OPTS=-Dorg.eclipse.rdf4j.appdata.basedir=\path\to\other\dir\ (on Windows)
    export JAVA_OPTS='-Dorg.eclipse.rdf4j.appdata.basedir=/path/to/other/dir/' (on Linux/UNIX)

If you are using Apache Tomcat as a Windows Service you should use the Windows Services configuration tool to set this property. Other users can either edit the Tomcat startup script or set the property some other way.

One easy way to find out what the directory is in a running instance of the Rdf4j Server, is to go to http://localhost:8080/rdf4j-server/home/overview.view in your browser and click on ‘System’ in the navigation menu on the left. The data directory will be listed as one of the configuration settings of the current server.

