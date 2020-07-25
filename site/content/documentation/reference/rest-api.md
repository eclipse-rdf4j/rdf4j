---
title: "The RDF4J REST API"
toc: true
weight: 1
---
The RDF4J server REST API is a HTTP Protocol that covers a fully compliant implementation of the [SPARQL 1.1 Protocol W3C Recommendation](https://www.w3.org/TR/sparql11-protocol/). This ensures that RDF4J server functions as a fully standards-compliant **SPARQL endpoint**. 
<!--more-->
The current version of the API additionally supports the [SPARQL 1.1 Graph Store HTTP Protocol W3C Recommendation](https://www.w3.org/TR/sparql11-http-rdf-update/). The RDF4J REST API extends the W3C standards in several aspects, the most important of which is that it supports a full database transaction mechanism.

The [REST architectural style](https://en.wikipedia.org/wiki/Representational_state_transfer) implies that URLs are used to represent the various resources that are available on a server. Here, we give a summary of the resources that are available from a RDF4J server sinstance with the HTTP-methods that can be used on them. In this overview, `<RDF4J_URL>` is used to denote the location of the RDF4J server instance, e.g. `http://localhost:8080/rdf4j-server`. Likewise, `<REP_ID>` denotes the ID of a specific repository (e.g. “mem-rdf”), and `<PREFIX>` denotes a namespace prefix (e.g. “rdfs”).

The following is an overview of the resources that are available from RDF4J server.

    <RDF4J_URL>
       /protocol         : protocol version (GET)
       /repositories     : overview of available repositories (GET)
          /<REP_ID>      : query evaluation and administration tasks on a repository
                           (GET/POST/PUT/DELETE)
           /config       : repository configuration (GET/POST) 
           /statements   : repository statements (GET/POST/PUT/DELETE)
           /contexts     : context overview (GET)
           /size         : # statements in repository (GET)
           /rdf-graphs   : named graphs overview (GET)
               /service  : SPARQL Graph Store operations on indirectly referenced named
                           graphs in repository (GET/PUT/POST/DELETE)
               /<NAME>   : SPARQL Graph Store operations on directly referenced named
                           graphs in repository (GET/PUT/POST/DELETE)
           /namespaces   : overview of namespace definitions (GET/DELETE)
               /<PREFIX> : namespace-prefix definition (GET/PUT/DELETE)
           /transactions : starting point for creating transactions on the current repository (POST)
               /<TXN_ID> : a specific transaction which can be updated (PUT/DELETE)

# Protocol version

The version of the protocol that the server uses to communicate over HTTP is available at: `<RDF4J_URL>/protocol`. The version described by this chapter is "10".

Supported methods on this URL are:

- `GET`: Gets the protocol version string, e.g. “1”, “2”, etc.

## Version 10

New in RDF4J release 3.1.0.

- repository configuration / retrieval endpoint at `/repositories/<REP_ID>/config`.
- modified behavior of PUT on `/repositories/<REP_ID>`.

## Request examples
### Fetch the protocol version

Request:

    GET /rdf4j-server/protocol HTTP/1.1
    Host: localhost

Response:

    HTTP/1.1 200 OK
    Content-Type: text/plain;charset=UTF-8
    Content-Length: 1

    4

# Repository list

An overview of the repositories that are available on a server can be retrieved from `<RDF4J_URL>/repositories`.

Supported methods on this URL are:

- `GET`: Gets an list of available repositories, including ID, title, read- and write access parameters for each listed repository. The list is formatted as a tuple query result with variables “uri”, “id”, “title”, “readable” and “writable”. The “uri” value gives the URI/URL for the repository and the “readable” and “writable” values are xsd:boolean typed literals indicating read- and write permissions.

Request headers:

- `Accept`: Relevant values are the MIME types of supported variable binding formats.

## Request examples
### Fetch the repository list

Request:

    GET /rdf4j-server/repositories HTTP/1.1
    Host: localhost
    Accept: application/sparql-results+xml, */*;q=0.5

Response:

    HTTP/1.1 200 OK
    Content-Type: application/sparql-results+xml;charset=UTF-8

    <?xml version='1.0' encoding='UTF-8'?>
    <sparql xmlns='http://www.w3.org/2005/sparql-results#'>
      <head>
             <variable name='uri'/>
             <variable name='id'/>
             <variable name='title'/>
             <variable name='readable'/>
             <variable name='writable'/>
      </head>
      <results ordered='false' distinct='false'>
             <result>
                    <binding name='uri'>
                      <uri>http://localhost/rdf4j-server/repositories/mem-rdf</uri>
                    </binding>
                    <binding name='id'>
                      <literal>mem-rdf</literal>
                    </binding>
                    <binding name='title'>
                      <literal>Main Memory RDF repository</literal>
                    </binding>
                    <binding name='readable'>
                      <literal datatype='http://www.w3.org/2001/XMLSchema#boolean'>true</literal>
                    </binding>
                    <binding name='writable'>
                      <literal datatype='http://www.w3.org/2001/XMLSchema#boolean'>false</literal>
                    </binding>
             </result>
      </results>
    </sparql>

# Repository queries

Queries on a specific repository with ID `<ID>` can be evaluated by sending requests to: `<RDF4J_URL>/repositories/<ID>`. This resource represents a SPARQL query endpoint. Both `GET` and `POST` methods are supported. The `GET` method is preferred as it adheres to the REST architectural style. The `POST` method should be used in cases where the length of the (URL-encoded) query exceeds practicable limits of proxies, servers, etc. In case a `POST` request is used, the query parameters should be sent to the server as www-form-urlencoded data.

Parameters:

- `query`: The query to evaluate.
- `queryLn` (optional): Specifies the query language that is used for the query. Acceptable values are strings denoting the query languages supported by the server, i.e. “serql” for SeRQL queries and “sparql” for SPARQL queries. If not specified, the server assumes the query is a SPARQL query.
- `infer` (optional): Specifies whether inferred statements should be included in the query evaluation. Inferred statements are included by default. Specifying any value other than “true” (ignoring case) restricts the query evluation to explicit statements only.
- `$<varname>` (optional): specifies variable bindings. Variables appearing in the query can be bound to a specific value outside the actual query using this option. The value should be an N-Triples encoded RDF value.
- `timeout` (optional): specifies a maximum query execution time, in whole seconds. The value should be an integer. A setting of 0 or a negative number indicates unlimited query time (the default).
- `distinct` (optional): specifies if only distinct query solutions should be returned. The value should be `true` or `false`. If the supplied SPARQL query itself already has a `DISTINCT` modifier, this parameter will have no effect.
- `limit` (optional): specifies the maximum number of query solutions to return. The value should be a positive integer. If the supplied SPARQL query itself already has a `LIMIT` modifier, this parameter will only have an effect if the supplied value is lower than the `LIMIT` value in the query.
- `offset` (optional): specifies the number of query solutions to skip. The value should be a positive inteer. This parameter is cumulative with any `OFFSET` modifier in the supplied SPARQL query itself.

Request headers:

- `Accept`: Relevant values are the MIME types of supported RDF formats for graph queries, the MIME types of supported variable binding formats for tuple queries, and the MIME types of supported boolean result formats for boolean queries.
- `Content-Type`: specifies the mediatype of a POST request body. Possible values are “application/x-www-form-urlencoded” (for a SPARQL query or update encoded as a form parameter), “application/sparql-query” (for an unencoded SPARQL query string) or “application/sparql-update” (for an unencoded SPARQL update string).

## Request examples
### Evaluate a SeRQL-select query on repository “mem-ref”

Request:

    GET /rdf4j-server/repositories/mem-rdf?query=select%20%3Cfoo:bar%3E&queryLn=serql HTTP/1.1
    Host: localhost
    Accept: application/sparql-results+xml, */*;q=0.5

Response:

    HTTP/1.1 200 OK
    Content-Type: application/sparql-results+xml;charset=UTF-8

    <?xml version='1.0' encoding='UTF-8'?>
    <sparql xmlns='http://www.w3.org/2005/sparql-results#'>
      <head>
             <variable name='&lt;foo:bar&gt;'/>
      </head>
      <results ordered='false' distinct='false'>
             <result>
                    <binding name='&lt;foo:bar&gt;'>
                      <uri>foo:bar</uri>
                    </binding>
             </result>
      </results>
    </sparql>

### Evaluate a SPARQL-construct query on repository “mem-rdf” using a POST request

Request:

    POST /rdf4j-server/repositories/mem-rdf HTTP/1.1
    Host: localhost
    Content-Type: application/sparql-query
    Accept: application/rdf+xml, */*;q=0.5

    construct {?s ?p ?o} where {?s ?p ?o}

Response:

    HTTP/1.1 200 OK
    Content-Type: application/rdf+xml;charset=UTF-8

    <?xml version="1.0" encoding="UTF-8"?>
    <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
    </rdf:RDF>

### Evaluate a SPARQL-construct query on repository “mem-rdf” using a POST request and form encoding
 
Request:

    POST /rdf4j-server/repositories/mem-rdf HTTP/1.1
    Host: localhost
    Content-Type: application/x-www-form-urlencoded
    Accept: application/rdf+xml, */*;q=0.5

    query=construct%20{?s%20?p%20?o}%20where%20{?s%20?p%20?o}

Response:

    HTTP/1.1 200 OK
    Content-Type: application/rdf+xml;charset=UTF-8

    <?xml version="1.0" encoding="UTF-8"?>
    <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
    </rdf:RDF>

### Evaluate a SPARQL-ask query on repository “mem-ref”

Request:

    GET /rdf4j-server/repositories/mem-rdf?query=ask%20{?s%20?p%20?o} HTTP/1.1
    Host: localhost
    Accept: text/boolean, */*;q=0.5

Response:

    HTTP/1.1 200 OK
    Content-Type: text/boolean;charset=US-ASCII

    true

# Repository creation

A new repository with ID `<ID>` can be created on the server by sending requests to: `<RDF4J_URL>/repositories/<ID>`. The `PUT` method should be used for this.

The payload supplied with this request is expected to contain an RDF document, containing an RDF-serialized form of a repository configuration. If the repository with the specified id previously existed, the Server will refuse the request. If it does not exist, a new, empty, repository will be created.

An example payload, containing a repository configuration for an in-memory store:

```turtle
@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>.
@prefix rep: <http://www.openrdf.org/config/repository#>.
@prefix sr: <http://www.openrdf.org/config/repository/sail#>.
@prefix sail: <http://www.openrdf.org/config/sail#>.
@prefix ms: <http://www.openrdf.org/config/sail/memory#>.

[] a rep:Repository ;
   rep:repositoryID "test" ;
   rdfs:label "test memory store" ;
   rep:repositoryImpl [
      rep:repositoryType "openrdf:SailRepository" ;
      sr:sailImpl [
	 sail:sailType "openrdf:MemoryStore" ;
	 ms:persist true ;
	 ms:syncDelay 120
      ]
   ].
```

# Repository removal

A specific repository with ID `<ID>` can be deleted from the server by sending requests to: `<RDF4J_URL>/repositories/<ID>`. The `DELETE` method should be used for this, and the request accepts no parameters.

Care should be taken with the use of this method: the result of this operation is the complete removal of the repository from the server, including its configuration settings and (if present) data directory.

## Request examples
### Remove the “mem-rdf” repository

Request:

    DELETE /rdf4j-server/repositories/mem-rdf HTTP/1.1
    Host: localhost

Response:

    HTTP/1.1 204 NO CONTENT

# Repository configuration

(new in RDF4J 3.1.0) 

The configuration for a specific repositroy with ID `<ID>` is available at: `<RDF4J_URL>/repositories/<ID>/config`.

Supported methods on this URL are:

- `GET`: Retrieves the current configuration of the repository.
- `POST`: Updates the configuration of the repository. Care should be taken as this operation may result in data loss if the new configuration is not compatible with the existing configuration.

Request headers:

-    `Accept`: Relevant values for GET requests are the MIME types of supported RDF formats.
-    `Content-Type`: Must specify the encoding of any request data that is sent to a server. Relevant values are the MIME types of supported RDF formats.

The payload supplied with POST request is expected to contain an RDF document, containing an RDF-serialized form of a repository configuration. The Server will attempt to reconfigure the existing repository with the supplied configuration data. 

An example payload, containing a repository configuration for an in-memory store:

```turtle
@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>.
@prefix rep: <http://www.openrdf.org/config/repository#>.
@prefix sr: <http://www.openrdf.org/config/repository/sail#>.
@prefix sail: <http://www.openrdf.org/config/sail#>.
@prefix ms: <http://www.openrdf.org/config/sail/memory#>.

[] a rep:Repository ;
   rep:repositoryID "test" ;
   rdfs:label "test memory store" ;
   rep:repositoryImpl [
      rep:repositoryType "openrdf:SailRepository" ;
      sr:sailImpl [
	 sail:sailType "openrdf:MemoryStore" ;
	 ms:persist true ;
	 ms:syncDelay 120
      ]
   ].
```

# Repository statements

The statements for a specific repository with ID `<ID>` are available at: `<RDF4J_URL>/repositories/<ID>/statements`

Supported methods on this URL are:

- `GET`: Fetches statements from the repository.
- `PUT`: Updates data in the repository, replacing any existing data with the supplied data. The data supplied with this request is expected to contain an RDF document in one of the supported RDF formats.
- `DELETE`: Deletes statements from the repository.
- `POST`: Performs updates on the data in the repository. The data supplied with this request is expected to contain either an RDF document, a SPARQL 1.1 Update string, or a special purpose transaction document. If an RDF document is supplied, the statements found in the RDF document will be added to the repository. If a SPARQL 1.1 Update string is supplied, the update operation will be parsed and executed. If a transaction document is supplied, the updates specified in the transaction document will be executed.

Parameters:

- `subj` (optional): Restricts a GET or DELETE operation to statements with the specified N-Triples encoded resource as subject.
- `pred` (optional): Restricts a GET or DELETE operation to statements with the specified N-Triples encoded URI as predicate.
- `obj` (optional): Restricts a GET or DELETE operation to statements with the specified N-Triples encoded value as object.
- `update` (optional): Only relevant for POST operations. Specifies the SPARQL 1.1 Update string to be executed. The value is expected to be a syntactically valid SPARQL 1.1 Update string.
- `context` (optional): If specified, restricts the operation to one or more specific contexts in the repository. The value of this parameter is either an N-Triples encoded URI or bnode ID, or the special value ‘null’ which represents all context-less statements. If multiple ‘context’ parameters are specified, the request will operate on the union of all specified contexts. The operation is executed on all statements that are in the repository if no context is specified.
- `infer` (optional): Specifies whether inferred statements should be included in the result of GET requests. Inferred statements are included by default. Specifying any value other than “true” (ignoring case) restricts the request to explicit statements only.
- `baseURI` (optional): Specifies the base URI to resolve any relative URIs found in uploaded data against. This parameter only applies to the PUT and POST method.
- `timeout` (optional): specifies a maximum update execution time, in whole seconds. The value should be an integer. A setting of 0 or a negative number indicates unlimited execution time (the default). This parameter only applies to SPARQL update operations.

Optionally, update operations can specify a custom dataset on which the operation is to be executed. Dataset parameters:

- `using-graph-uri` (optional): one or more named graph URIs to be used as the default graph(s) for retrieving statements
- `using-named-graph-uri` (optional): one or more named graph URIs to be used as named graphs for retrieving statements
- `remove-graph-uri` (optional): one or more named graph URIs to be used as the default graph(s) for removing statements
- `insert-graph-uri` (optional): one or more named graph URIs to be used as the default graph(s) for inserting statements

Request headers:

-    `Accept`: Relevant values for GET requests are the MIME types of supported RDF formats.
-    `Content-Type`: Must specify the encoding of any request data that is sent to a server. Relevant values are the MIME types of supported RDF formats, `application/x-rdftransaction` for a transaction document and `application/x-www-form-urlencoded` in case the parameters are encoded in the request body (as opposed to the being part of the request URL).

## Request examples
### Fetch all statements from repository “mem-rdf”

Request:

    GET /rdf4j-server/repositories/mem-rdf/statements HTTP/1.1
    Host: localhost
    Accept: application/rdf+xml

Response:

    HTTP/1.1 200 OK
    Content-Type: application/rdf+xml;charset=UTF-8

    [RDF/XML ENCODED RDF DATA]

### Fetch all statements from a specific context in repository “mem-rdf”

Request:

    GET /rdf4j-server/repositories/mem-rdf/statements?context=_:n1234x5678 HTTP/1.1
    Host: localhost
    Accept: application/rdf+xml

Response:

    HTTP/1.1 200 OK
    Content-Type: application/rdf+xml;charset=UTF-8

    [RDF/XML ENCODED RDF DATA]

### Remove all statements from the “mem-rdf” repository

Request:

    DELETE /rdf4j-server/repositories/mem-rdf/statements HTTP/1.1
    Host: localhost

Response:

    HTTP/1.1 204 NO CONTENT

### Add data to the “mem-rdf” repository

Request:

    POST /rdf4j-server/repositories/mem-rdf/statements HTTP/1.1
    Host: localhost
    Content-Type: application/rdf+xml;charset=UTF-8

    [RDF/XML ENCODED RDF DATA]

Response:

    HTTP/1.1 204 NO CONTENT

### Add data to the “mem-rdf” repository, replacing any and all existing data

Request:

    PUT /rdf4j-server/repositories/mem-rdf/statements HTTP/1.1
    Host: localhost
    Content-Type: application/rdf+xml;charset=UTF-8

    [RDF/XML ENCODED RDF DATA]

Response:

    HTTP/1.1 204 NO CONTENT

### Add data to a specific context in the “mem-rdf” repository, replacing any data that is currently in this context

Request:

    PUT /rdf4j-server/repositories/mem-rdf/statements?context=%3Curn:x-local:graph1%3E&baseURI=%3Curn:x-local:graph1%3E HTTP/1.1
    Host: localhost
    Content-Type: application/x-turtle;charset=UTF-8

    [TURTLE ENCODED RDF DATA]

Response:

    HTTP/1.1 204 NO CONTENT

### Add statements without a context to the “mem-rdf” repository, ignoring any context information that is encoded in the supplied data

Request:

    POST /rdf4j-server/repositories/mem-rdf/statements?context=null HTTP/1.1
    Host: localhost
    Content-Type: application/x-turtle;charset=UTF-8

    [TURTLE ENCODED RDF DATA]

Response:

    HTTP/1.1 204 NO CONTENT

### Perform update described in a SPARQL 1.1 Update string

Request:

    POST /rdf4j-server/repositories/mem-rdf/statements HTTP/1.1
    Host: localhost
    Content-Type: application/x-www-form-urlencoded

    update=INSERT%20{?s%20?p%20?o}%20WHERE%20{?s%20?p%20?o}

Response:

    HTTP/1.1 204 NO CONTENT

### Perform updates described in a transaction document and treat it as a single transaction

Request:

    POST /rdf4j-server/repositories/mem-rdf/statements HTTP/1.1
    Host: localhost
    Content-Type: application/x-rdftransaction

    [TRANSACTION DATA]

Response:

    HTTP/1.1 204 NO CONTENT

# Repository size

The repository size (defined as the number of statements it contains) is available at: `<RDF4J_URL>/repositories/<ID>/size`.

Supported methods on this URL are:

-    GET: Gets the number of statements in a repository.

Parameters:

-    `context` (optional): If specified, restricts the operation to one or more specific contexts in the repository. The value of this parameter is either an N-Triples encoded URI or bnode ID, or the special value `null` which represents all context-less statements. If multiple `context` parameters are specified, the request will operate on the union of all specified contexts. The operation is executed on all statements that are in the repository if no context is specified.

## Request examples
### Get the size of repository ‘mem-rdf’

Request

    GET /rdf4j-server/repositories/mem-rdf/size HTTP/1.1
    Host: localhost

Response:

    HTTP/1.1 200 OK
    Content-Type: text/plain

    123456

### Get the size of a specific context in repository ‘mem-rdf’

Request

    GET /rdf4j-server/repositories/mem-rdf/size?context=%3Curn:x-local:graph1%3E HTTP/1.1
    Host: localhost

Response:

    HTTP/1.1 200 OK
    Content-Type: text/plain

    4321

# Context list

A list of resources that are used as context identifiers in a repository with ID `<ID>` is available at: `<RDF4J_URL>/repositories/<ID>/contexts`

Supported methods on this URL are:

 -   `GET`: Gets a list of resources that are used as context identifiers. The list is formatted as a tuple query result with a single variable `contextID`, which is bound to URIs and bnodes that are used as context identifiers.

Request headers:

 -  `Accept`: Relevant values are the MIME types of supported variable binding formats.

## Request examples
### Fetch all context identifiers from repository “mem-rdf”

Request:

    GET /rdf4j-server/repositories/mem-rdf/contexts HTTP/1.1
    Host: localhost
    Accept: application/sparql-results+xml

Response:

    HTTP/1.1 200 OK
    Content-Type: application/sparql-results+xml

    <?xml version='1.0' encoding='UTF-8'?>
    <sparql xmlns='http://www.w3.org/2005/sparql-results#'>
      <head>
             <variable name='contextID'/>
      </head>
      <results ordered='false' distinct='false'>
             <result>
                    <binding name='contextID'>
                      <uri>urn:x-local:graph1</uri>
                    </binding>
             </result>
      </results>
    </sparql>

# Namespace declaration lists

Namespace declaration lists for a repository with ID `<ID>` are available at:`<RDF4J_URL>/repositories/<ID>/namespaces`.

Supported methods on this URL are:

- `GET`: Gets a list of namespace declarations that have been defined for the repository. The list is formatted as a tuple query result with variables “prefix” and “namespace”, which are both bound to literals.
- `DELETE`: Removes all namespace declarations from the repository.

Request headers:

- `Accept`: Relevant values for GET requests are the MIME types of supported variable binding formats.

## Request examples
### Fetch all namespace declaration info

Request

    GET /rdf4j-server/repositories/mem-rdf/namespaces HTTP/1.1
    Host: localhost
    Accept: application/sparql-results+xml, */*;q=0.5

Response:

    HTTP/1.1 200 OK
    Content-Type: application/sparql-results+xml

    <?xml version='1.0' encoding='UTF-8'?>
    <sparql xmlns='http://www.w3.org/2005/sparql-results#'>
      <head>
             <variable name='prefix'/>
             <variable name='namespace'/>
      </head>
      <results ordered='false' distinct='false'>
             <result>
                    <binding name='prefix'>
                      <literal>rdf</literal>
                    </binding>
                    <binding name='namespace'>
                      <literal>http://www.w3.org/1999/02/22-rdf-syntax-ns#</literal>
                    </binding>
             </result>
      </results>
    </sparql>

### Remove all namespace declarations from the repository

Request:

    DELETE /rdf4j-server/repositories/mem-rdf/namespaces HTTP/1.1
    Host: localhost

Response:

    HTTP/1.1 204 NO CONTENT

# Namespace declarations

Namespace declarations with prefix `<PREFIX>` for a repository with ID `<ID>` are available at: `<RDF4J_URL>/repositories/<ID>/namespaces/<PREFIX>`.

Supported methods on this URL are:

- `GET`: Gets the namespace that has been defined for a particular prefix.
- `PUT`: Defines or updates a namespace declaration, mapping the prefix to the namespace that is supplied in plain text in the request body.
- `DELETE`: Removes a namespace declaration.

## Request examples
### Get the namespace for prefix ‘rdf’

Request

    GET /rdf4j-server/repositories/mem-rdf/namespaces/rdf HTTP/1.1
    Host: localhost

Response:

    HTTP/1.1 200 OK
    Content-Type: text/plain;charset=UTF-8

    http://www.w3.org/1999/02/22-rdf-syntax-ns#

### Set the namespace for a specific prefix

Request:

    PUT /rdf4j-server/repositories/mem-rdf/namespaces/example HTTP/1.1
    Host: localhost
    Content-Type: text/plain

    http://www.example.com

Response:

    HTTP/1.1 204 NO CONTENT

### Remove the namespace for a specific prefix

Request:

    DELETE /rdf4j-serverrepositories/mem-rdf/namespaces/example HTTP/1.1
    Host: localhost

Response:

    HTTP/1.1 204 NO CONTENT

# Graph Store support

The SPARQL 1.1 Graph Store HTTP Protocol is supported on a per-repository basis. The functionality is accessible at `<RDF4J_URL>/repositories/<ID>/rdf-graphs/service` (for indirectly referenced named graphs), and `<RDF4J_URL>/repositories/<ID>/rdf-graphs/<NAME>` (for directly referenced named graphs). A request on a directly referenced named graph entails that the request URL itself is used as the named graph identifier in the repository.

Supported methods on these resources are:

- `GET`: fetches statements in the named graph from the repository.
- `PUT`: Updates data in the named graph in the repository, replacing any existing data in the named graph with the supplied data. The data supplied with this request is expected to contain an RDF document in one of the supported RDF formats.
- `DELETE`: Delete all data in the named graph in the repository.
- `POST`: Updates data in the named graph in the repository, adding to any existing data in the named graph with the supplied data. The data supplied with this request is expected to contain an RDF document in one of the supported RDF formats.

Request headers:

- `Accept`: Relevant values for GET requests are the MIME types of supported RDF formats.
- `Content-Type`: Must specify the encoding of any request data that is sent to a server. Relevant values are the MIME types of supported RDF formats.

For requests on indirectly referenced graphs, the following parameters are supported:

- `graph` (optional): specifies the URI of the named graph to be accessed.
- `default` (optional): specifies that the default graph is to be accessed. This parameter is expected to be present but have no value.

Each request on an indirectly referenced graph needs to specify precisely one of the above parameters.

## Request examples
### Fetch all statements from a directly referenced named graph in repository “mem-rdf”

Request:

    GET /rdf4j-server/repositories/mem-rdf/rdf-graphs/graph1 HTTP/1.1
    Host: localhost
    Accept: application/rdf+xml

Response:

    HTTP/1.1 200 OK
    Content-Type: application/rdf+xml;charset=UTF-8

    [RDF/XML ENCODED RDF DATA]

### Fetch all statements from an indirectly referenced named graph in repository “mem-rdf”

Request:

    GET /rdf4j-server/repositories/mem-rdf/rdf-graphs/service?graph=http%3A%2F%2Fexample.org%2Fgraph1 HTTP/1.1
    Host: localhost
    Accept: application/rdf+xml

Response:

    HTTP/1.1 200 OK
    Content-Type: application/rdf+xml;charset=UTF-8

    [RDF/XML ENCODED RDF DATA]

### Fetch all statements from the default graph in repository “mem-rdf”

Request:

    GET /rdf4j-server/repositories/mem-rdf/rdf-graphs/service?default HTTP/1.1
    Host: localhost
    Accept: application/rdf+xml

Response:

    HTTP/1.1 200 OK
    Content-Type: application/rdf+xml;charset=UTF-8

    [RDF/XML ENCODED RDF DATA]

### Add statements to a directly referenced named graph in the “mem-rdf” repository

Request:

    POST /rdf4j-server/repositories/mem-rdf/rdf-graphs/graph1 HTTP/1.1
    Host: localhost
    Content-Type: application/x-turtle;charset=UTF-8

    [TURTLE ENCODED RDF DATA]

Response:

    HTTP/1.1 204 NO CONTENT

### Clear a directly referenced named graph in the “mem-rdf” repository

Request:

    DELETE /rdf4j-server/repositories/mem-rdf/rdf-graphs/graph1 HTTP/1.1
    Host: localhost

Response:

    HTTP/1.1 204 NO CONTENT

# Starting transactions

Rdf4j supports a RESTful implementation of a transactional protocol, by means of treating the transaction itself as a new resource that can be updated with consecutive operations.

Transactions are available at `<RDF4J_URL>/repositories/<ID>/transactions`.  The  supported methods on this URI are:

- `POST`: starts a new transaction. The server assigns the transaction a new unique identifier (a URI), which is returned in the response Location header.

Parameters:

- `isolation-level` (optional): specifies the transaction isolation level to be used for this transaction. Possible values are store-extensible, the default set of levels includes `NONE`, `READ_UNCOMMITTED`, `READ_COMMITTED`, `SNAPSHOT_READ`, `SNAPSHOT`,  and `SERIALIZABLE`.  Note that an RDF4J store implementation is not guaranteed to support all possible isolation levels. If no isolation level is specified, the default isolation level for the store will be used.

## Request examples
### Starting a new transaction

Request:

    POST /rdf4j-server/repositories/<ID>/transactions HTTP/1.1

Response:

    HTTP/1.1 201 CREATED

    Location: /rdf4j-server/repositories/<ID>/transactions/64a5937f-c112-d014-a044-f0123b93 HTTP/1.1

# Transaction operations

Transaction operations are carried out on URIs of the form `<RDF4J_URL>/repositories/<ID>/transactions/<TXN_ID>`. The full URI is the exact value of the Location header as returned by the request to start a transaction.

Supported method are:

- `PUT`: update the transaction with an additional operation to perform in it.
- `DELETE`: roll back the transaction and close/remove it.

Parameters:

- `action` (required): specified on `PUT` requests only, this specifies the specific type of operation being carried out. Possible values are `ADD`, `DELETE`, `GET`, `SIZE`, `QUERY`, `UPDATE`, and `COMMIT`.

Other parameters depend on the type of action being performed.

## The ADD operation

The `ADD` operation expects a parsable RDF document in the request body. It adds the contents of this document as part of the current transaction.

### Request example: adding a Turtle document

Request:

    PUT /rdf4j-server/repositories/<ID>/transactions/<TXN_ID>?action=ADD HTTP/1.1
    Content-Type: text/turtle

    [Turtle-serialized data]

Response:

    HTTP/1.1 200 OK

## The DELETE operation

The `DELETE` operation expects a parsable RDF document in the request body. It removes the contents of this document as part of the current transaction.

Any RDF statement in the parsed document that uses the special URI reference `http://www.openrdf.org/schema/sesame#wildcard` as either a subject, predicate, or object, is treated as a wildcard statement: the special URI reference is treated as a wildcard value and all statements matching the pattern thus formed are removed.

### Request example: remove three specific RDF statements

Request:

    PUT /rdf4j-server/repositories/<ID>/transactions/<TXN-ID>?action=DELETE HTTP/1.1
    Content-Type: text/turtle

    <urn:george> <urn:lastName> "Harrisson" .
    <urn:paul> <urn:lastName> "McCartney" .
    <urn:ringo> <urn:lastName> "Starr" .

Response:

    HTTP/1.1 200 OK

### Request example: remove all statements with property urn:lastName

Request:

    PUT /rdf4j-server/repositories/<ID>/transactions/<TXN-ID>?action=DELETE HTTP/1.1
    Content-Type: text/turtle


    @prefix rdf4j: <http://www.openrdf.org/schema/sesame#> .

    rdf4j:wildcard <urn:lastName> rdf4j:wildcard .

Response:

    HTTP/1.1 200 OK

### Request example: remove all statements in context urn:context1

Request:

    PUT /rdf4j-server/repositories/<ID>/transactions/<TXN-ID>?action=DELETE HTTP/1.1
    Content-Type: application/x-trig

    @prefix rdf4j: <http://www.openrdf.org/schema/sesame#> .

    <urn:context1> {
       rdf4j:wildcard rdf4j:wildcard rdf4j:wildcard .
    }

Response:

    HTTP/1.1 200 OK

## The GET operation

The `GET` operation retrieves statements from the repository as part of the current transaction, and returns the result as an RDF document.

Parameters:

- `subj` (optional): restricts the operation to statements with the specified N-Triples encoded resource as subject.
- `pred` (optional): restricts the operation to statements with the specified N-Triples encoded resource as predicate.
- `obj` (optional): restricts the operation to statements with the specified N-Triples encoded resource as object.
- `context` (optional): If specified, restricts the operation to one or more specific contexts in the repository. The value of this parameter is either an N-Triples encoded URI or bnode ID, or the special value `null` which represents all context-less statements. If multiple `context` parameters are specified, the request will operate on the union of all specified contexts. The operation is executed on all statements that are in the repository if no context is specified.
- `infer` (optional): Specifies whether inferred statements should be included in the result of `GET` requests. Inferred statements are included by default. Specifying any value other than `true` (ignoring case) restricts the request to explicit statements only.

### Request example: get all Statements about George

Request:

    PUT /rdf4j-server/repositories/<ID>/transactions/<TXN_ID>?action=GET&subj=urn:george HTTP/1.1
    Accept: text/turtle

Response:

    HTTP/1.1 200 OK
    Content-Type: text/turtle

    <urn:george> <urn:firstName> "George" .
    <urn:george> <urn:lastName> "Harrisson" .

## The QUERY operation

The `QUERY` operation executes a SPARQL or SeRQL query on the repository as part of the current transaction, and returns the result as an RDF document.

Parameters:

- `query`: The query to evaluate.
- `queryLn` (optional): Specifies the query language that is used for the query. Acceptable values are strings denoting the query languages supported by the server, i.e. `serql` for SeRQL queries and `sparql` for SPARQL queries. If not specified, the server assumes the query is a SPARQL query.
- `infer` (optional): Specifies whether inferred statements should be included in the query evaluation. Inferred statements are included by default. Specifying any value other than `true` (ignoring case) restricts the query evluation to explicit statements only.

Request Headers:

- `Accept`: Relevant values are the MIME types of supported RDF formats for graph queries, the MIME types of supported variable binding formats for tuple queries, and the MIME types of supported boolean result formats for boolean queries.

## The SIZE operation

The `SIZE` operation retrieves the number of (explicit) statements present in the repository, as part of the current transaction.

Parameters:

- `context` (optional): If specified, restricts the operation to one or more specific contexts in the repository. The value of this parameter is either an N-Triples encoded URI or bnode ID, or the special value `null` which represents all context-less statements. If multiple `context` parameters are specified, the request will operate on the union of all specified contexts. The operation is executed on all statements that are in the repository if no context is specified.

### Example request: retrieve current size of repository

Request:

    PUT /rdf4j-server/repositories/<ID>/transactions/<TXN_ID>?action=SIZE HTTP/1.1

Response:

    HTTP/1.1 200 OK
    Content-Type: text/plain

    4000

## The UPDATE operation

The `UPDATE` operation executes a SPARQL update query on the repository as part of the current transaction.

Parameters:

- `update` (required): specifies the Update operation to be executed. The value is expected to be a syntactically valid SPARQL 1.1 Update string.
- `baseURI` (optional): specifies a base URI to be used when parsing the SPARQL update operation.
- `infer` (optional): specifies if inferred statements should be taken into account when executing the operation. Default is `true`.
- `$<varname>` (optional): specifies variable bindings. Variables appearing in the update operation can be bound to a specific value outside the actual query using this option. The value should be an N-Triples encoded RDF value.

Optionally, an update operation can specify a custom dataset on which the Update operation is to be executed. Dataset parameters:

- `using-graph-uri` (optional): one or more named graph URIs to be used as the default graph(s) for retrieving statements
- `using-named-graph-uri` (optional): one or more named graph URIs to be used as named graphs for retrieving statements
- `remove-graph-uri` (optional): one or more named graph URIs to be used as the default graph(s) for removing statements
- `insert-graph-uri` (optional): one or more named graph URIs to be used as the default graph(s) for inserting statements

### Example request: execute SPARQL INSERT update

Request:

    PUT /rdf4j-server/repositories/<ID>/transactions/<TXN_ID>?action=UPDATE HTTP/1.1
    Content-Type: application/x-www-form-urlencoded

    update=INSERT%20%7B%3FS%20a%20%3Curn%3ASinger%3E%20%20%7D%20WHERE%20%7B%20%3FS%20%3Curn%3Aname%3E%20%22John%22%20%7D

Response:

    HTTP/1.1 201 No Content

## The COMMIT operation

The `COMMIT` operation signals to finalize and commit the current transaction. After this has been successfully executed, all modifications done by this transaction will be final and the general repository state will reflect the changes.

Executing a commit ends the transaction: after commit, further operations on the same transaction will result in an error.

### Example: execute COMMIT

Request:

    PUT /rdf4j-server/repositories/<ID>/transactions/<TXN_ID>?action=COMMIT HTTP/1.1

Response:

    HTTP/1.1 200 OK

## Aborting a transaction by executing a DELETE

An active transaction can be aborted by means of a HTTP `DELETE` request on the transaction resource. This will execute a transaction rollback on the repository and will close the transacion. After executing a `DELETE`, further operations on the same transaction will result in an error.

### Example: rollback transaction by means of a HTTP DELETE

Request:

    DELETE /rdf4j-server/repositories/<ID>/transactions/<TXN_ID> HTTP/1.1

Response:

    HTTP/1.1 201 No Content

# Content types

The following tables summarizes the MIME types for various document formats that are relevant to this protocol.

## MIME types for RDF formats

| Format           | MIME type                |
|------------------|--------------------------|
| RDF/XML          | application/rdf+xml      |
| N-Triples        | text/plain               |
| Turtle           | text/turtle              |
| N3               | text/rdf+n3              |
| N-Quads          | text/x-nquads            |
| JSON-LD          | application/ld+json      |
| RDF/JSON         | application/rdf+json     |
| TriX             | application/trix         |
| TriG             | application/x-trig       |
| RDF4J Binary RDF | application/x-binary-rdf |

## MIME types for variable binding formats

| Format                    | MIME type                              |
|---------------------------|----------------------------------------|
| SPARQL Query Results XML  | application/sparq-results+xml          |
| SPARQL Query Results JSON | application/sparq-results+json         |
| Binary Results Format     | application/x-binary-rdf-results-table |

## MIME types for boolean result formats

| Format                    | MIME type                      |
|---------------------------|--------------------------------|
| SPARQL Query Results XML  | application/sparq-results+xml  |
| SPARQL Query Results JSON | application/sparq-results+json |
| Plain Text Boolean Result | text/boolean                   |
