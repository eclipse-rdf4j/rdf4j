---
title: "Rdf4j Server, Workbench, and Console"
layout: "doc"
hide_page_title: "true"
---

# Installing Rdf4j Server and Rdf4j Workbench

In this chapter, we explain how you can install Rdf4j Server (the actual database server and SPARQL endpoint service) and Rdf4j Workbench (a web-based client UI for managing databases and executing queries).

## Required software

Rdf4j Server and Rdf4j Workbench requires the following software:

- Java 8 Runtime Environment (either [OpenJDK](https://openjdk.java.net/) or [Oracle Java](https://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html))
- A Java Servlet Container that supports Java Servlet API 2.5 and Java Server Pages (JSP) 2.0, or newer. 

We recommend using a recent, stable version of [Apache Tomcat](https://tomcat.apache.org/) ([version 9.0](https://tomcat.apache.org/download-90.cgi) at the time of writing).

## Deploying Server and Workbench

Rdf4j Server is a database management application: it provides HTTP access to Rdf4j repositories, exposing them as SPARQL endpoints. Rdf4j Server is meant to be accessed by other applications. Apart from some functionality to view the server’s log messages, it doesn’t provide any user oriented functionality. Instead, the user oriented functionality is part of Rdf4j Workbench. The Workbench provides a web interface for querying, updating and exploring the repositories of an Rdf4j Server.

If you have not done so already, you will first need to <a href="/download">download the Rdf4j SDK</a>. Both Rdf4j Server and Rdf4j Workbench can be found in the `war` directory of the SDK. The war-files in this directory need to be deployed in a Java Servlet Container. The deployment process is container-specific, please consult the documentation for your container on how to deploy a web application. For Apache Tomcat, we recommend using the [Tomcat Manager](https://tomcat.apache.org/tomcat-9.0-doc/manager-howto.html) to make deployment easier.

After you have deployed the Rdf4j Workbench webapp, you should be able to access it, by default, at path <a href="http://localhost:8080/rdf4j-workbench">http://localhost:8080/rdf4j-workbench</a>. You can point your browser at this location to verify that the deployment succeeded.

### Configuring Rdf4j Workbench for UTF-8 Support

#### UTF-8 in the Request URI (GET)

There is a known issue (https://github.com/eclipse/rdf4j/issues/464[464]) affecting the proper exploring of resources that use an extended character set. Workbench client-side code generates URI’s assuming an ISO-8859-1 character encoding, and often Tomcat comes pre-configured to expect UTF-8 encoded URI’s. It will be necessary to change the HTTP Connector configuration, or to add a separate HTTP Connector that uses ISO-8859-1. For details, see https://tomcat.apache.org/tomcat-6.0-doc/config/http.html[here] for Tomcat 6 or https://tomcat.apache.org/tomcat-7.0-doc/config/http.html[here] for Tomcat 7.

#### UTF-8 in the Request Body (POST)

To resolve issues where the request body is not getting properly interpreted as UTF-8, it is necessary to configure Tomcat to use its built in SetCharacterEncodingFilter. Some details are at https://wiki.apache.org/tomcat/FAQ/CharacterEncoding#Q3. With Tomcat 6, version 6.0.36 or later is required. On Tomcat 7, un-commenting the <filter> and <filter-mapping> elements for setCharacterEncodingFilter in `$CATALINA_BASE/conf/web.xml`, and restarting the server, were the only necessary steps.

## Application directory configuration

Rdf4j Server, Workbench, and Console all store configuration files and repository data in a single directory (with subdirectories). On Windows machines, this directory is `%APPDATA%\Rdf4j\` by default, where `%APPDATA%` is the application data directory of the user that runs the application. For example, in case the application runs under the ‘LocalService’ user account on Windows XP, the directory is `C:\Documents and Settings\LocalService\Application Data\Rdf4j\`. On Linux/UNIX, the default location is `$HOME/.Rdf4j/`, for example `/home/tomcat/.rdf4j/`. We will refer to this data directory as `[Rdf4j_DATA]` in the rest of this manual.

The location of this data directory can be reconfigured using the Java system property `org.eclipse.rdf4j.appdata.basedir`. When you are using Tomcat as the servlet container then you can set this property using the `JAVA_OPTS` parameter, for example:

    set JAVA_OPTS=-Dorg.eclipse.rdf4j.appdata.basedir=\path\to\other\dir\ (on Windows)
    export JAVA_OPTS='-Dorg.eclipse.rdf4j.appdata.basedir=/path/to/other/dir/' (on Linux/UNIX)

If you are using Apache Tomcat as a Windows Service you should use the Windows Services configuration tool to set this property. Other users can either edit the Tomcat startup script or set the property some other way.

One easy way to find out what the directory is in a running instance of the Rdf4j Server, is to go to http://localhost:8080/rdf4j-server/home/overview.view in your browser and click on ‘System’ in the navigation menu on the left. The data directory will be listed as one of the configuration settings of the current server.

### Repository Configuration

Each repository in Rdf4j Server stores both its configuration and the actual persisted data in the application dir. The location is `[Rdf4j_DATA]/server/repositories/[REPOSITORY_ID]`. The configuration is stored as a file `config.ttl` in that directory. The other files in this directory represent stored data, indexes and other files that the database needs to persist its data (in other words: best don't touch).

The easiest way to create and manage repositories on an Rdf4j Server to use the Rdf4j Console or Rdf4j Workbench. Both offer commands to quickly create a new repository and guide you through the various configuration options. 

However, you can also directly edit the `config.ttl` of your repository to change its configuration. For example, you can use this to change the repository name as it is shown in the Workbench, or perhaps to change configuration parameters, or change the repository type. However, proceed with caution: if you make a mistake, your repository may become unreadable until after you've rectified the mistake. Also note that if you change the actual store type (e.g. switching from a memory store to a native store), it _won't_ migrate your existing data to the new store configuration!

### Logging Configuration

Both Rdf4j Server and Rdf4j Workbench use the Logback logging framework. In its default configuration, all Rdf4j Server log messages are sent to the log file `[Rdf4j_DATA]/Server/logs/main.log` (and log messages for the Workbench to the same file in `[Rdf4j_DATA]/Workbench` ).

The default log level is INFO, indicating that only important status messages, warnings and errors are logged. The log level and -behaviour can be adjusted by modifying the `[Rdf4j_DATA]/Server/conf/logback.xml` file. This file will be generated when the server is first run. Please consult the logback manual for configuration instructions.

## Access Rights and Security

It is possible to set up your Rdf4j Server to authenticate named users and
restrict their permissions.  Rdf4j Server is a servlet-based Web
application deployed to any standard servlet container (for the remainder of
this section it is assumed that Tomcat is being used).

The Rdf4j Server exposes its functionality using a [REST
API](/documentation/rest-api) that is an extension of the SPARQL protocol for
RDF. This protocol defines exactly what operations can be achieved using
specific URL patterns and HTTP methods (`GET`, `POST`, `PUT`, `DELETE`). Each
combination of URL pattern and HTTP method can be associated with a set of user
roles, thus giving very fine-grained control.

In general, read operations are effected using `GET` and write operations using
`PUT`, `POST` and `DELETE`. The exception to this is that POST is allowed for
SPARQL queries. This is for practical reasons, because some HTTP servers have
limits on the length of the parameter values for GET requests.

### Security constraints and roles

The association between operations and security roles is specified using
security constraints in Rdf4j Server’s _deployment descriptor_ - a file
called `web.xml` that can be found in the `.../webapps/rdf4j-server/WEB-INF`
directory. `web.xml` becomes available immediately after the installation without
any security roles defined.

*Warning*: When redeployed, the `web.xml` file gets overwritten with the
default version. Therefore, if you change it, make sure you create a backup. In
particular, do not edit `web.xml` while Tomcat is running.

The deployment descriptor defines:

- authentication mechanism/configuration;
- security constraints in terms of operations (URL pattern plus HTTP method);
- security roles associated with security constraints.

To enable authentication, add the following XML element to `web.xml` inside the `<web-app>` element:

{{< highlight xml >}}
    <login-config>
        <auth-method>BASIC</auth-method>
        <realm-name>rdf4j</realm-name>
    </login-config>
{{< / highlight >}}

Security constraints associate operations (URL pattern plus HTTP method) with
security roles. Both security constraints and security roles are nested in the
`<web-app>` element.

A security constraint minimally consists of a collection of web resources
(defined in terms of URL patterns and HTTP methods) and an authorisation
constraint (the role name that has access to the resource collection). Some
example security constraints are shown below:

{{< highlight xml >}}
<security-constraint>
    <web-resource-collection>
        <web-resource-name>SPARQL query access to the 'test' repository</web-resource-name>
        <url-pattern>/repositories/test</url-pattern>
        <http-method>GET</http-method>
        <http-method>POST</http-method>
    </web-resource-collection>
    <auth-constraint>
        <role-name>viewer</role-name>
        <role-name>editor</role-name>
    </auth-constraint>
</security-constraint>

<security-constraint>
    <web-resource-collection>
        <web-resource-name>
        Read access to 'test' repository's namespaces, size, contexts, etc
        </web-resource-name>
        <url-pattern>/repositories/test/*</url-pattern>
        <http-method>GET</http-method>
</web-resource-collection>
    <auth-constraint>
        <role-name>viewer</role-name>
        <role-name>editor</role-name>
    </auth-constraint>
</security-constraint>

<security-constraint>
    <web-resource-collection>
        <web-resource-name>Write access</web-resource-name>
        <url-pattern>/repositories/test/*</url-pattern>
        <http-method>POST</http-method>
        <http-method>PUT</http-method>
        <http-method>DELETE</http-method>
    </web-resource-collection>
    <auth-constraint>
        <role-name>editor</role-name>
    </auth-constraint>
</security-constraint>
{{< / highlight >}}

The ability to create and delete repositories requires access to the SYSTEM repository. An administrator security constraint for this looks like the following:

{{< highlight xml >}}
<security-constraint>
    <web-resource-collection>
        <web-resource-name>Administrator access to SYSTEM</web-resource-name>
        <url-pattern>/repositories/SYSTEM/</url-pattern>
        <url-pattern>/repositories/SYSTEM/*/</url-pattern>
        <http-method>GET</http-method>
        <http-method>POST</http-method>
        <http-method>PUT</http-method>
        <http-method>DELETE</http-method>
    </web-resource-collection>
    <auth-constraint>
        <role-name>administrator</role-name>
    </auth-constraint>
</security-constraint>
{{< / highlight >}}

Also nested inside the `<web-app>` element are definitions of security roles. The format is shown by the example:

{{< highlight xml >}}
<security-role>
    <description>
        Read only access to repository data
    </description>
    <role-name>viewer</role-name>
</security-role>

<security-role>
    <description>
        Read/write access to repository data
    </description>
    <role-name>editor</role-name>
</security-role>

<security-role>
    <description>
        Full control over the repository, as well as creating/deleting repositories
    </description>
    <role-name>administrator</role-name>
</security-role>
{{< / highlight >}}

### User accounts

Tomcat has a number of ways to manage user accounts. The different techniques
are called 'realms' and the default one is called 'UserDatabaseRealm'. This is
the simplest one to manage, but also the least secure, because usernames and
passwords are stored in plain text.

For the default security realm, usernames and passwords are stored in the file
`tomcat-users.xml` in the Tomcat configuration directory, usually
`/etc/tomcat/tomcat-users.xml` on Linux systems. To add user accounts, add
`<user>` elements inside the `<tomcat-users>` element, for example:

{{< highlight xml >}}
<user username="adam" password="secret" roles="viewer" />
<user username="eve" password="password" roles="viewer,editor,administrator" />
{{< / highlight >}}

### Programmatic authentication

To use a remote repository where authentication has been enabled, it is
necessary to provide the username and password to the rdf4j API. Remote
repositories are usually accessed via the {{< javadoc "RemoteRepositoryManager"
"repository/manager/RemoteRepositoryManager.html" >}} class. Tell the
repository manager what the security credentials are using the following
method:

{{< highlight java >}}
void setUsernameAndPassword(String username, String password)
{{< / highlight >}}

Alternatively, they can be passed in the factory method:

{{< highlight java >}}
static RemoteRepositoryManager getInstance(String serverURL, String username, String password)
{{< / highlight >}}

# Rdf4j Console

The Rdf4j Console is a text console application for interacting with Rdf4j. It can be used to create and use local RDF databases, or to connect to a running Rdf4j Server.

## Getting started

Rdf4j Console can be started using the `console.bat`/`.sh` scripts in the bin directory of the Rdf4j SDK. By default, the console will connect to the “default data directory”, which contains the console’s own set of repositories.

The console is operated by typing commands. For example, to get an overview of the available commands, type:

   help

To get help for a specific command, type ‘help’ followed by the command name, e.g.:

   help connect
 
### History

The Console has a built-in history, use the `Up` and `Down` arrows to cycle through the history of commands.

### Connecting to a set of repositories

As indicated in the previous section, the Console connects to its own set of repositories by default. Using the `connect` command you can make the console connect to a Rdf4j Server or to a set of repositories on your file system. For example, to connect to a Rdf4j Server that is listening to port 8080 on localhost, enter the following command:

    connect http://localhost:8080/rdf4j-server
 
To connect to the default set of repositories, enter:

    connect default

When connecting to a remote server, a user name and password can be provided as well:

    connect http://example.rdf4j.org/rdfj-server myname mypassword

Not surprisingly, the `disconnect` command disconnects the console from the set of repository.

### Showing the list of repositories

To get an overview of the repositories that are available in the set that your console is connected to, use the `show` command:

     show repositories

### Creating a new repository

The `create` command creates a new repository in the set the console is connected to. This command expects the name of a template describing the repository's configuration. Several templates are available, including:

- memory — a memory based RDF repository
- memory-rdfs — a main-memory repository with RDF Schema inferencing
- memory-rdfs-dt — a main-memory repository with RDF Schema and direct type hierarchy inferencing
- native — a repository that uses on-disk data structure
- native-rdfs — a native repository with RDF Schema inferencing
- native-rdfs-dt — a native repository with RDF Schema and direct type hierarchy inferencing
- remote — a repository that serves as a proxy for a repository on a Rdf4j Server
- sparql — a repository that serves as a proxy for a SPARQL endpoint


When the `create` command is executed, the console will ask you to fill in a number of parameters for the type of repository that you chose. For example, to create a native repository, you execute the following command:

     create native

The console will ask you to provide an ID and title for the repository, as well as the triple indexes that need to be created for this kind of store. The values between square brackets indicate default values which you can select by simply hitting enter. The output of this dialogue looks something like this:

     Please specify values for the following variables:
     Repository ID [native]: myRepo
     Repository title [Native store]: My repository
     Triple indexes [spoc,posc]: 
     Repository created

### Opening and closing a repository

The `open` command opens a specific repository. For example, to open the `myrepo` repository, enter:

    open myrepo

The `close` command closes the connection.

### Verifying a file

The `verify` command verifies the validity of an RDF file. Several formats (serializations) are supported, including JSON-LD, Turtle, N-Triples and RDF/XML. The console will select the format based upon the extension of the file name. For example, to verify a JSON-LD file: 
 
    verify data.jsonld

On a MS-Windows system, forward slashes or double backward slashes are to be used when specifying the file path, for example:

    verify C:\\data\\rdf\\data.jsonld

or

    verify C:/data/rdf/data.jsonld
  
### Loading a file into a repository

The `load` command loads a file into the opened repository.  Several formats (serializations) are supported, including JSON-LD, Turtle, N-Triples and RDF/XML. The console will select the format based upon the extension of the file name.

    load import.nt

Specifying a base IRI for resolving relative IRIs:

    load import.nt from http://example.org

### Exporting a repository to a file

The `export` command exports statements from a repository to a file. Either the entire repository can be exported, or a (list of) named graphs / contexts.

    export export.nt

### Executing a SPARQL query

The `sparql` command executes a sparql query. 

    sparql

Multiple lines can be entered. To terminate the input, enter a new line containing only a single dot `.`

    select ?s ?p ?o
    where { ?s ?p ?o }
    .

###= reading queries from and exporting results to a file

NOTE: new in Rdf4j 2.5 

Queries can be read from an existing file:

    sparql infile="file.qr"

Results can be saved to an output file. The file type extension is used to determine the output format, but the exact list of available file formats depends on the type of the query.
Graph queries (`construct`) can be saved as JSON-LD, RDF/XML, N-Triples or Turtle, by using the respective extensions `.jsonld`, `.xml`, `.nt` or `.ttl`.
Tuple queries (`select`) can be saved as SPARQL Results CSV, TSV, JSON or XML, by using the respective extensions `.csv`, `.tsv`, `.srj` or `.srx`.

For example:

    sparql outfile="result.srj" select ?s where { ?s ?p ?o }

Or:

    sparql outfile="result.nt" construct { ?s ?p ?o } where { ?s ?p ?o }

Combining input file for reading a query and an output for writing the result is also possible:

    sparql infile="query.txt" outfile="result.tsv"

When relative paths are used, files are read from or saved to the working directory, which can be changed using the following command:

   set workdir=/path/to/working/dir


### Setting namespace prefixes

NOTE: new in Rdf4j 2.5

Using prefixes for namespaces (e.g. `dcterms:` instead of `http://purl.org/dc/terms/`) makes queries and results easier to read, and queries less error-prone to write.
By default a few well-known prefixes are available, including `dcterms`, `foaf`, `rdfs` and `skos`.

For a complete list, see:

    set prefixes

Adding and clearing a namespace prefix is quite straightforward:

    set prefixes=ex http://example.com
    set prefixes=ex <none>

Enter the following command to remove all namespace prefixes:

    set prefixes=<none>

Going back to the built-in list of well-know prefixes is easy, even when the list of prefixes was cleared:

    set prefixes=<default>

In addition, it is possible to toggle between using / showing the short prefix or using / showing the full namespace URI, without actually changing the prefixes: 

    set queryprefix=true
    set showprefix=true

### Other commands

Please check the documentation that is provided by the console itself for help on how to use the other commands. Most commands should be self explanatory. 

## Repository configuration
### Memory store configuration

A memory store is an RDF repository that stores its data in main memory. Apart from the standard ID and title parameters, this type of repository has a Persist and Sync delay parameter.

#### Memory Store persistence

The Persist parameter controls whether the memory store will use a data file for persistence over sessions. Persistent memory stores write their data to disk before being shut down and read this data back in the next time they are initialized. Non-persistent memory stores are always empty upon initialization.

#### Synchronization delay

By default, the memory store persistence mechanism synchronizes the disk backup directly upon any change to the contents of the store. That means that directly after an update operation (upload, removal) completes, the disk backup is updated. It is possible to configure a synchronization delay however. This can be useful if your application performs several transactions in sequence and you want to prevent disk synchronization in the middle of this sequence to improve update performance.

The synchronization delay is specified by a number, indicating the time in milliseconds that the store will wait before it synchronizes changes to disk. The value 0 indicates that there should be no delay. Negative values can be used to postpone the synchronization indefinitely, i.e. until the store is shut down.

### Native store configuration

A native store stores and retrieves its data directly to/from disk. The advantage of this over the memory store is that it scales much better as it is not limited to the size of available memory. Of course, since it has to access the disk, it is also slower than the in-memory store, but it is a good solution for larger data sets.

#### Native store indexes

The native store uses on-disk indexes to speed up querying. It uses B-Trees for indexing statements, where the index key consists of four fields: subject (s), predicate (p), object (o) and context (c). The order in which each of these fields is used in the key determines the usability of an index on a specify statement query pattern: searching statements with a specific subject in an index that has the subject as the first field is significantly faster than searching these same statements in an index where the subject field is second or third. In the worst case, the ‘wrong’ statement pattern will result in a sequential scan over the entire set of statements.

By default, the native repository only uses two indexes, one with a subject-predicate-object-context (spoc) key pattern and one with a predicate-object-subject-context (posc) key pattern. However, it is possible to define more or other indexes for the native repository, using the Triple indexes parameter. This can be used to optimize performance for query patterns that occur frequently.

The subject, predicate, object and context fields are represented by the characters ‘s’, ‘p’, ‘o’ and ‘c’ respectively. Indexes can be specified by creating 4-letter words from these four characters. Multiple indexes can be specified by separating these words with commas, spaces and/or tabs. For example, the string “spoc, posc” specifies two indexes; a subject-predicate-object-context index and a predicate-object-subject-context index.

Creating more indexes potentially speeds up querying (a lot), but also adds overhead for maintaining the indexes. Also, every added index takes up additional disk space.

The native store automatically creates/drops indexes upon (re)initialization, so the parameter can be adjusted and upon the first refresh of the configuration the native store will change its indexing strategy, without loss of data.

### HTTP repository configuration

An HTTP repository is not an actual store by itself, but serves as a proxy for a store on a (remote) Rdf4j Server. Apart from the standard ID and title parameters, this type of repository has a Rdf4j Server location and a Remote repository ID parameter.

#### Rdf4j Server location

This parameter specifies the URL of the Rdf4j Server instance that the repository should communicate with. Default value is http://localhost:8080/rdf4j-server, which corresponds to an Rdf4j Server instance that is running on your own machine.

#### Remote repository ID

This is the ID of the remote repository that the HTTP repository should communicate with. Please note an HTTP repository in the Console has two repository ID parameters: one identifying the remote repository and one that specifies the HTTP repository’s own ID.

### Repository configuration templates (advanced)

In Rdf4j Server, repository configurations with all their parameters are modeled in RDF and stored in the SYSTEM repository. So, in order to create a new repository, the Console needs to create such an RDF document and submit it to the SYSTEM repository. The Console uses so called repository configuration templates to accomplish this.

Repository configuration templates are simple Turtle RDF files that describe a repository configuration, where some of the parameters are replaced with variables. The Console parses these templates and asks the user to supply values for the variables. The variables are then substituted with the specified values, which produces the required configuration data.

The Rdf4j Console comes with a number of default templates. The Console tries to resolve the parameter specified with the ‘create’ command (e.g. “memory”) to a template file with the same name (e.g. “memory.ttl”). The default templates are included in Console library, but the Console also looks in the templates subdirectory of `[Rdf4j_DATA]`. You can define your own templates by placing template files in this directory.

To create your own templates, it’s easiest to start with an existing template and modify that to your needs. The default “memory.ttl” template looks like this:

    #
    # Rdf4j configuration template for a main-memory repository
    #
    @prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>.
    @prefix rep: <http://www.openrdf.org/config/repository#>.
    @prefix sr: <http://www.openrdf.org/config/repository/sail#>.
    @prefix sail: <http://www.openrdf.org/config/sail#>.
    @prefix ms: <http://www.openrdf.org/config/sail/memory#>.

    [] a rep:Repository ;
       rep:repositoryID "{%Repository ID|memory%}" ;
       rdfs:label "{%Repository title|Memory store%}" ;
       rep:repositoryImpl [
          rep:repositoryType "openrdf:SailRepository" ;
          sr:sailImpl [
             sail:sailType "openrdf:MemoryStore" ;
             ms:persist {%Persist|true|false%} ;
             ms:syncDelay {%Sync delay|0%}
          ]
       ].

Template variables are written down as `{%var name%}` and can specify zero or more values, seperated by vertical bars (“|”). If one value is specified then this value is interpreted as the default value for the variable. The Console will use this default value when the user simply hits the Enter key. If multiple variable values are specified, e.g. `{%Persist|true|false%}`, then this is interpreted as set of all possible values. If the user enters an unspecified value then that is considered to be an error. The value that is specified first is used as the default value.

The URIs that are used in the templates are the URIs that are specified by the `RepositoryConfig` and `SailConfig` classes of Rdf4j’s repository configuration mechanism. The relevant namespaces and URIs can be found in the javadoc of these classes.

# Rdf4j Workbench

This chapter describes the Rdf4j Workbench, a web application for interacting with Rdf4j and/or other SPARQL endpoints.

This chapter will refer to URLs on a local server served from port 8080, which is possibly the most common “out-of-the-box” configuration. That is, Workbench URLs will start with `http://localhost:8080/`.

## Browser Client Support

.Browser Client Support Matrix
|===
<table border=1 style="cellpadding: 4px;">
<tr><th>Browser</th><th>Fully Working Versions</th><th>Non-working Versions</th></tr>
<tr><td>Firefox</td><td> Any recent</td><td> </td></tr>
<tr><td>Chrome </td><td> Any recent</td><td>	</td></tr>
<tr><td>Internet Explorer</td><td> 8.0, 9.0 (Compatibility View),  10.0 (Compatibility View)</td><td> 9.0 (Other modes/views), 10.0 (Other modes/views)</td></tr>
</table>

## Getting Started

To start using Workbench for the first time, point your browser to `http://localhost:8080/rdf4j-workbench`. Your browser will be automatically redirected to `http://localhost:8080/rdf4j-workbench/repositories/NONE/repositories`. This page will display all repositories in the default server, as indicated by the “default-server” property in `WEB-INF/web.xml`. Normally this is set to `/rdf4j-server`. That is, the default server for Workbench is usually the Rdf4j Server instance at the path `/rdf4j-server` on the same web server. To view information about the Rdf4j Server instance, click on “Rdf4j Server” at the top of the side menu.

### Setting the Server, Repository and User Credentials

A “current selection” section sits at the top right in the Workbench, informing you of the URL of the server you are using, the repository you are currently using, and the user name used when accessing the server. Each of these items can be changed by clicking the “change” link immediately to the right of them. Since the Workbench is generally used for prototyping and exploration, “user” is commonly set to “none”. In this case, the Workbench is connecting to the Rdf4j Server without authenticating, and below we refer to the user in this mode as the anonymous user.

### Setting the Server and User Credentials

There are two ways to reach the “Change Server” page, which allows you to enter a URL for the server and, optionally, user credentials:

1. Clicking on “change” for either the server or the user.
2. Clicking on “Rdf4j Server” on the sidebar menu.

A full URL is expected in the “Change Server” field. You may enter a `file:///` URL to access a local repository on the Workbench server, but need to be sure that the Workbench server process has permission to access the given folder.

### Important Security Consideration

Workbench stores user name and password credentials in plain-text cookies in the browser. You will need to configure your Workbench server for HTTPS to properly protect these credentials. See https://tomcat.apache.org/tomcat-6.0-doc/ssl-howto.html or https://tomcat.apache.org/tomcat-7.0-doc/ssl-howto.html for more information.

### Setting the Repository

There are two ways to change the current repsository:

1. Clicking on “change” for the current repository in the “current selection” section.
2. Clicking on “Repositories” in the sidebar menu.

You will be presented with a table listing of all the repositories available on the current server, with the following columns:

- Readable
- Writable
- Id
- Description
- Location

“Location” is the URL of the repository, useful for accessing it via the Rdf4j REST API. “Id” is presented as a clickable hyperlink that will open that repository in the Workbench, bringing the user to a summary page for the repository.

###  Creating a Repository

Click on “New repository” in the sidebar menu. This brings up the “New Repository” page. You are presented with a simple form that provides a “Type:” selector with several repository types:

- 1-4 In Memory Stores: Simple, RDF Schema, RDF Schema and Direct Type Inferencing, or Custom Graph Query Inference
- 5-8 Native Stores: Simple, RDF Schema, RDF Schema and Direct Type Inferencing, or Custom Graph Query Inference
- 11 Remote RDF Store:	References a Rdf4j repository external to the present server.
- 12 SPARQL Endpoint Proxy: References a SPARQL Endpoint (See SPARQL 1.1 Protocol).
- 1| Federation Store: Presents other stores referenced on the present server as a single federation store for querying purposes.

The “ID:” and “Title:” fields are optional in this form. Clicking “Next” brings up a form with more fields specific to the repository type selected. On that form, it will be necessary to enter something in the “ID:” field before the “Create” button may be clicked. If creation is successful, the new repository is also opened and its “Summary” page is presented.

### Modifying the Data Contents of a Repository

Data may be added to or removed from current repository using any of the sidebar menu items under “Modify”. After all successful operations, the user is presented with the repository “Summary” page.

### Add

The “Add” page allows you to specify a URL with RDF data, a local file on on your client system, or to enter serialized RDF data into its text area for loading into the present repository. It is also possible to specify the Base URI and a Context for the triples. Think of the Context as a 4th element of each RDF statement, specifying a graph within the repository. You may specify one of eight serialization formats, or select “auto-detect” to let the server do a best guess at the format.

#### Remove

The “Remove Statements” page presents you with a form where you may enter values for subject, predicate, object or context. Clicking on “Remove” then removes all statements from the repository which match the given values. Leaving an item blank means that any value will match. If all values are left blank, clicking “Remove” will not do anything except present a warning message.

#### Clear

The “Clear Repository” page is powerful. Leaving the lone “Context:” field blank and clicking “Clear Context(s)” will remove all statements from all graphs in the repository. It is also possible to enter a resource value corresponding to a context that exists in the repository, and the statements for that graph only will be removed.

#### SPARQL Update

The “Execute SPARQL Update on Repository” page gives a text area where you enter a SPARQL 1.1 Update command. SPARQL Update is an extension to the SPARQL query language that provides full CRUD (Create Read Update Delete) capabilities. For more information see the W3C Recommendation for SPARQL 1.1 Update. Clicking “Execute” executes the specified SPARQL Update operation.

### Exploring a Repository

#### Summary Page

Click on “Summary” on the sidebar menu. A simple summary is displayed with the repository’s id, description, URL for remote access and the associated server’s URL for remote access. Many operations when repositories are created and updated display this page afterwards.

#### Namespaces Page

Namespace-prefix pairings can be defined within a repository, so that URIs can be displayed in shorthand form as a qualified name. To edit them, click on “Namespaces” on the sidebar menu. A page is displayed with a table of all presently defined pairs. Existing namespaces may be edited by selecting them in the drop-down list, which populates the text fields. The text fields may then be edited, and the “Update” button will make the change on the repository. The “Delete” button will remove whichever pair has been selected.

#### Contexts Page

“Context” is the Rdf4j construct for implementing RDF Named Graphs, which allow a repository to group data into separately addressable graphs. The Explore page always displays the context (always a URI or blank node) with each triple, the combination of which is often referred to as a quad.

To view all the contexts for the present repository, click on “Contexts” on the sidebar menu. Each context is clickable, bringing you to the “Explore” page for that context value.

#### Types Page

Click on “Types” on the sidebar menu. A list of types is displayed. These types are the resulting output from this SPARQL query:

   SELECT DISTINCT ?type WHERE { ?subj a ?type }

#### Explore Page

Click on “Explore” in the sidebar menu. You are presented with an “Explore” page. Type a resource value into the empty “Resource” field, and hit Enter. You will be presented with a table listing all triples where your given resource is a part of the statement, or is the context (graph) name. Currently allowable resource values are:

- URI’s enclosed in angle brackets, e.g., `<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>`
- Qualified Names (qnames), e.g. rdf:type, where the prefix “rdf” is associated with the namespace “http://www.w3.org/1999/02/22-rdf-syntax-ns#” in the repository.
- Literal values with an explicit datatype or language tag, e.g., `“dog”@en` or `“hund”@de` or `“1”^^xsd:integer` or `“9.99”^^<http://www.w3.org/2001/XMLSchema#decimal>`

Data types expressed with qnames also need to have their namespace defined in the repository.

By using the “Results per page” setting and the “Previous …” and “Next …” buttons, you may page through a long set of results, or display all of the results at once. There is also a “Show data types & language tags” checkbox which, when un-checked, allows a less verbose table view.

### Querying a Repository

Clicking on “Query” on the sidebar menu brings you to Workbench’s querying interface. Here, you may enter queries in the SPARQL or SeRQL query languages, save them for future access, and execute them against your repository.

If you have executed queries previously, the query text area will show the most recently executed query. If not, it will be pre-populated with a prefix header (SPARQL) or footer (SeRQL) containing all the defined namespaces for the repository. The “Clear” button below the text area gives you the option to restore this pre-populated state for the currently selected query language.

The two other action buttons are “Save Query” and “Execute”:

- “Save Query” is only enabled when a name has been entered into the adjacent text field. Once clicked, your query is saved under the given name. An option to back out or overwrite is given if the name already exists. Saved queries are associated with the current repository and user name. If the “Save privately (do not share)” option is checked, then the saved query will only be visible to the current user.
- “Execute” attempts to execute the given query text, and then you are presented with a query results page. Values are clickable, and clicking on a value brings you to its “Explore” page. Similar display options are presented as the “Explore” page, as well.

### Working with Saved Queries

Clicking “Saved queries” on the sidebar menu brings you to the Workbench’s interface for working with previously saved queries. All saved queries accessible to the current user are listed in alphabetical order by

- the user that saved them, then
- the query name

The query name is displayed as a clickable link that will execute the query, followed by 3 buttons:

- `Show`:	Toggles the display of the query metadata and query text. When the “Save Queries” page loads, this information is not showing to conserve screen real estate.
- `Edit`: 	Brings you to the query entry page, pre-populated with the query text.
- `Delete...`: Deletes the saved query, with a confirmation dialog provided for safety. Users may only delete their own queries or queries that were saved anonymously.

The query metadata fields, aside from query name and user, are:

- Query Language: either SPARQL or SeRQL
- Include Inferred Statements: whether to use any inferencing defined on the repository to expand the result set
- Rows per page: How many results to display per page at first
- Shared: whether this query is visible to users other than the one that saved it, restricted to always be true for the “anonymous” user

Note that it is only possible to save queries as the present user. If you edit another user’s query and save it with the same query name, a new saved query will be created associated with your user name.

### Viewing all Triples and Exporting the Data

The “Export” link on the sidebar menu is convenient for bringing up a paged view of all quads in your triple store. As with other result views, resources are displayed as clickable links that bring you to that resource’s “Explore” page. In addition, it is possible to select from a number of serialization formats to download the entire contents of the triple store in:

- TriG
- BinaryRDF
- TriX
- N-Triples
- N-Quads
- N3
- RDF/XML
- RDF/JSON
- Turtle

