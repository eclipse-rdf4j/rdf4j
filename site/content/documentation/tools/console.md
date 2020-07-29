---
title: "RDF4J Console"
toc: true
weight: 1
---

The RDF4J Console is a text console application for interacting with RDF4J. It can be used to create and use local RDF databases, or to connect to a running <a href="/documentation/tools/workbench-server/">RDF4J Server</a>.
<!--more-->

# Getting started

Rdf4j Console can be started using the `console.bat`/`.sh` scripts in the bin directory of the Rdf4j SDK. By default, the console will connect to the “default data directory”, which contains the console’s own set of repositories.

The console is operated by typing commands. For example, to get an overview of the available commands, type:

   help

To get help for a specific command, type ‘help’ followed by the command name, e.g.:

   help connect
 
## History

The Console has a built-in history, use the `Up` and `Down` arrows to cycle through the history of commands.

By default all commands will be saved to the history.txt file in the Console's application directory, and this file will be loaded when the Console is started again.
To prevent newly entered commands from being saved to file, set the `savehistory` setting to false:

   set savehistory=false

To re-enable, simply set `savehistory` to true.

## Connecting to a set of repositories

As indicated in the previous section, the Console connects to its own set of repositories by default. Using the `connect` command you can make the console connect to a Rdf4j Server or to a set of repositories on your file system. For example, to connect to a Rdf4j Server that is listening to port 8080 on localhost, enter the following command:

    connect http://localhost:8080/rdf4j-server
 
To connect to the default set of repositories, enter:

    connect default

When connecting to a remote server, a user name and password can be provided as well:

    connect http://example.rdf4j.org/rdfj-server myname mypassword

Not surprisingly, the `disconnect` command disconnects the console from the set of repository.

## Showing the list of repositories

To get an overview of the repositories that are available in the set that your console is connected to, use the `show` command:

     show repositories

## Creating a new repository

The `create` command creates a new repository in the set the console is connected to. This command expects the name of a template describing the <a href="/documentation/tools/repository-configuration/">repository's configuration</a>. Several templates are available, including:

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

## Opening and closing a repository

The `open` command opens a specific repository. For example, to open the `myrepo` repository, enter:

    open myrepo

The `close` command closes the connection.

## Verifying a file

The `verify` command verifies the validity of an RDF file. Several formats (serializations) are supported, including JSON-LD, Turtle, N-Triples and RDF/XML. The console will select the format based upon the extension of the file name. For example, to verify a JSON-LD file: 
 
    verify data.jsonld

On a MS-Windows system, forward slashes or double backward slashes are to be used when specifying the file path, for example:

    verify C:\\data\\rdf\\data.jsonld

or:

    verify C:/data/rdf/data.jsonld
  
Validating the file against a set of shapes and constraints in a SHACL file, and storing the validation report to a file, is equally straightforward:   

   verify data.jsonld shacl-file.ttl validation-report.ttl

## Loading a file into a repository

The `load` command loads a file into the opened repository.  Several formats (serializations) are supported, including JSON-LD, Turtle, N-Triples and RDF/XML. The console will select the format based upon the extension of the file name.

    load import.nt

Specifying a base IRI for resolving relative IRIs:

    load import.nt from http://example.org

## Exporting a repository to a file

The `export` command exports statements from a repository to a file. Either the entire repository can be exported, or a (list of) named graphs / contexts.

    export export.nt

## Executing a SPARQL query

The `sparql` command executes a sparql query. 

    sparql

Multiple lines can be entered. To terminate the input, enter a new line containing only a single dot `.`

    select ?s ?p ?o
    where { ?s ?p ?o }
    .

##= reading queries from and exporting results to a file

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


## Setting namespace prefixes

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

## Other commands

Please check the documentation that is provided by the console itself for help on how to use the other commands. Most commands should be self explanatory.
 
