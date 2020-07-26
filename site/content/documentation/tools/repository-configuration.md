---
title: "Repository configuration and templates"
toc: true
weight: 4
---
An overview of the configuration parameters for the various out-of-the-box stores RDF4J offers.
<!--more-->

# Memory store configuration

A memory store is an RDF repository that stores its data in main memory. Apart from the standard ID and title parameters, this type of repository has a Persist and Sync delay parameter.

## Memory Store persistence

The Persist parameter controls whether the memory store will use a data file for persistence over sessions. Persistent memory stores write their data to disk before being shut down and read this data back in the next time they are initialized. Non-persistent memory stores are always empty upon initialization.

## Synchronization delay

By default, the memory store persistence mechanism synchronizes the disk backup directly upon any change to the contents of the store. That means that directly after an update operation (upload, removal) completes, the disk backup is updated. It is possible to configure a synchronization delay however. This can be useful if your application performs several transactions in sequence and you want to prevent disk synchronization in the middle of this sequence to improve update performance.

The synchronization delay is specified by a number, indicating the time in milliseconds that the store will wait before it synchronizes changes to disk. The value 0 indicates that there should be no delay. Negative values can be used to postpone the synchronization indefinitely, i.e. until the store is shut down.

# Native store configuration

A native store stores and retrieves its data directly to/from disk. The advantage of this over the memory store is that it scales much better as it is not limited to the size of available memory. Of course, since it has to access the disk, it is also slower than the in-memory store, but it is a good solution for larger data sets.

## Native store indexes

The native store uses on-disk indexes to speed up querying. It uses B-Trees for indexing statements, where the index key consists of four fields: subject (s), predicate (p), object (o) and context (c). The order in which each of these fields is used in the key determines the usability of an index on a specify statement query pattern: searching statements with a specific subject in an index that has the subject as the first field is significantly faster than searching these same statements in an index where the subject field is second or third. In the worst case, the ‘wrong’ statement pattern will result in a sequential scan over the entire set of statements.

By default, the native repository only uses two indexes, one with a subject-predicate-object-context (spoc) key pattern and one with a predicate-object-subject-context (posc) key pattern. However, it is possible to define more or other indexes for the native repository, using the Triple indexes parameter. This can be used to optimize performance for query patterns that occur frequently.

The subject, predicate, object and context fields are represented by the characters ‘s’, ‘p’, ‘o’ and ‘c’ respectively. Indexes can be specified by creating 4-letter words from these four characters. Multiple indexes can be specified by separating these words with commas, spaces and/or tabs. For example, the string “spoc, posc” specifies two indexes; a subject-predicate-object-context index and a predicate-object-subject-context index.

Creating more indexes potentially speeds up querying (a lot), but also adds overhead for maintaining the indexes. Also, every added index takes up additional disk space.

The native store automatically creates/drops indexes upon (re)initialization, so the parameter can be adjusted and upon the first refresh of the configuration the native store will change its indexing strategy, without loss of data.

# HTTP repository configuration

An HTTP repository is not an actual store by itself, but serves as a proxy for a store on a (remote) Rdf4j Server. Apart from the standard ID and title parameters, this type of repository has a Rdf4j Server location and a Remote repository ID parameter.

## Rdf4j Server location

This parameter specifies the URL of the Rdf4j Server instance that the repository should communicate with. Default value is http://localhost:8080/rdf4j-server, which corresponds to an Rdf4j Server instance that is running on your own machine.

## Remote repository ID

This is the ID of the remote repository that the HTTP repository should communicate with. Please note an HTTP repository in the Console has two repository ID parameters: one identifying the remote repository and one that specifies the HTTP repository’s own ID.

# Repository configuration templates (advanced)

In <a href="/documentation/tools/server-workbench/">Rdf4j Server</a>, repository configurations with all their parameters are modeled in RDF and stored in the SYSTEM repository. So, in order to create a new repository, the Console needs to create such an RDF document and submit it to the SYSTEM repository. The Console uses so called repository configuration templates to accomplish this.

Repository configuration templates are simple Turtle RDF files that describe a repository configuration, where some of the parameters are replaced with variables. The Console parses these templates and asks the user to supply values for the variables. The variables are then substituted with the specified values, which produces the required configuration data.

The <a href="/documentation/tools/console/">Rdf4j Console</a> comes with a number of default templates. The Console tries to resolve the parameter specified with the ‘create’ command (e.g. “memory”) to a template file with the same name (e.g. “memory.ttl”). The default templates are included in Console library, but the Console also looks in the templates subdirectory of `[Rdf4j_DATA]`. You can define your own templates by placing template files in this directory.

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

