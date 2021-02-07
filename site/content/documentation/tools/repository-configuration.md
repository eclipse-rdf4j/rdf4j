---
title: "Repository configuration templates"
weight: 4
---

In [RDF4J Server](/documentation/tools/server-workbench/), repository configurations with all their parameters are modeled in RDF. The syntax is documented in more detail in [Repository and SAIL Configuration](/documentation/reference/configuration/). In order to create a new repository, the Console needs to create such an RDF document and submit it to the repository manager. The Console uses configuration *templates* to accomplish this.
<!--more-->

Repository configuration templates are simple Turtle RDF files that describe a repository configuration, where some of the parameters are replaced with variables. The Console parses these templates and asks the user to supply values for the variables. The variables are then substituted with the specified values, which produces the required configuration data.

The [RDF4J Console](/documentation/tools/console/) comes with a number of default templates. The Console tries to resolve the parameter specified with the ‘create’ command (e.g. “memory”) to a template file with the same name (e.g. “memory.ttl”). The default templates are included in Console library, but the Console also looks in the templates subdirectory of `[Rdf4j_DATA]`. You can define your own templates by placing template files in this directory.

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

The URIs that are used in the templates are the URIs that are specified by the `RepositoryConfig` and `SailConfig` classes of RDF4J’s repository configuration mechanism. The relevant namespaces and URIs can be found in the javadoc of these classes, or in [Repository and Sail Configuration](/documentation/reference/configuration/).

