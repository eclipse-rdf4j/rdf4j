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
    # RDF4J configuration template for a main-memory repository
    #
    @prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>.
    @prefix config: <tag:rdf4j.org,2023:config/>.

    [] a config:Repository ;
       config:rep.id "{%Repository ID|memory%}" ;
       rdfs:label "{%Repository title|Memory store%}" ;
       config:rep.impl [
          config:rep.type "openrdf:SailRepository" ;
          config:sail.impl [
             config:sail.type "openrdf:MemoryStore" ;
             config:mem.persist {%Persist|true|false%} ;
             config:mem.syncDelay {%Sync delay|0%}
          ]
       ].

Template variables are written down as `{%var name%}` and can specify zero or more values, seperated by vertical bars (“|”). If one value is specified then this value is interpreted as the default value for the variable. The Console will use this default value when the user simply hits the Enter key. If multiple variable values are specified, e.g. `{%Persist|true|false%}`, then this is interpreted as set of all possible values. If the user enters an unspecified value then that is considered to be an error. The value that is specified first is used as the default value.

The URIs that are used in the templates are terms defined in the RDF4J Config vocabulary. The relevant namespace and URIs can be found in the {{< javadoc "CONFIG" "model/vocabulary/config.html" >}} vocabulary javadoc, or in [Repository and Sail Configuration](/documentation/reference/configuration/).


## Workbench form annotations

The [RDF4J Workbench](/documentation/tools/server-workbench/) builds its “New repository” form from the same templates. Two kinds of comment lines let a template control how that form looks. They are plain Turtle comments, so the Console ignores them.

A template-level annotation, usually at the top of the file, describes the template as a whole:

    # @workbench.template label="Acme graph" order=20 hidden=false

| Key      | Meaning                                                                                      |
|----------|----------------------------------------------------------------------------------------------|
| `label`  | The name shown in the repository type picker (defaults to the template's file name).         |
| `order`  | Sort position in the picker; lower values come first.                                         |
| `hidden` | `true` leaves the template out of the default picker. It can still be offered with `create.templates` (see below). |

A field annotation is placed on the line(s) *after* the line that holds the `{%...%}` placeholder it describes. With several placeholders on one line, use `name="Placeholder name"` to pick one; otherwise the comments apply to the placeholders in order of appearance.

    config:mem.persist {%Persist|true|false%} ;
    # @workbench.field readonly=true
    config:mem.syncDelay {%Sync delay|0%}
    # @workbench.field group="Advanced options" len=8 placeholder="milliseconds"

| Key           | Meaning                                                                                                   |
|---------------|-----------------------------------------------------------------------------------------------------------|
| `name`        | The placeholder the comment applies to, when a line holds more than one.                                  |
| `len`         | Width of a text input, in characters.                                                                     |
| `rows`, `cols`| Size of a textarea.                                                                                       |
| `placeholder` | Hint text shown in an empty input.                                                                        |
| `control`     | `text`, `textarea`, `select` or `radio`. By default a placeholder with several values is a select (a radio pair for `true`/`false`), a multi-line value a textarea, anything else a text input. |
| `unset`       | `true` adds a “Repository default” choice that leaves the line out of the generated configuration. Only on lines with a single placeholder. |
| `readonly`    | `true` or `false`. A read-only field is shown but cannot be changed: text inputs and textareas are read-only, selects and radio buttons are disabled. |
| `group`       | A non-blank section name. Fields with the same group are collected in one collapsible section with that name. |

Values that contain spaces must be quoted, as in `group="Advanced options"`. Unknown keys, a `readonly` value other than `true` or `false`, and a blank `group` make the template fail to load.

`readonly` is enforced by the server, not only by the form: whatever a client submits for a read-only field is ignored and the template default (the first value of the placeholder) is used. A read-only field therefore cannot also be `unset`; combining `readonly=true` with `unset=true` on the same field is an error.

Without any `group`, the Workbench keeps the identity and endpoint fields in the main form and gathers the remaining fields into an “Advanced settings” section, grouped by shared label prefixes. As soon as *any* field of a template has a `group`, the form switches to annotation-driven sections: fields without a group stay in the main form, and each group becomes its own collapsible section, labelled with the group name and ordered by the first appearance of that group in the template. The automatic “Advanced settings” section is not built in that case.

Your own templates can be added to the “New repository” page, and the page restricted or reordered, with the `create.templates` key of the Workbench policy; see [Repository types offered when creating a repository](/documentation/tools/server-workbench/#repository-types-offered-when-creating-a-repository).
