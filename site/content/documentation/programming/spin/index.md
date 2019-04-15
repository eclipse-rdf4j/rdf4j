---
title: "Reasoning and Validation with SPIN"
layout: "doc"
hide_page_title: "true"
---

# Reasoning and Validation with SPIN

The SPARQL Inferencing Notation (SPIN) is a way to represent a wide range of business rules on top of an RDF dataset. These rules can be anything from constraint validation to inferred property value calculation. Configuration

The `SpinSail` (currently in beta) is a StackedSail component that adds a forward-chaining SPIN rule engine on top of any store. In its most basic form it can be used directly on top of a Sail:

{{< highlight java >}}
// create a basic Sail Stack with a simple Memory Store and SPIN inferencing support
SpinSail spinSail = new SpinSail();
spinSail.setBaseSail(new MemoryStore());
// create a repository with the Sail stack:
Repository rep = new SailRepository(spinSail);
rep.initialize();
{{< / highlight >}}

Alternatively, a SpinSail can be configured via the `RepositoryManager`:

{{< highlight java >}}
// create the config for the sail stack
SailImplConfig spinSailConfig = new SpinSailConfig(new MemoryStoreConfig());
RepositoryImplConfig repositoryTypeSpec = new SailRepositoryConfig(spinSailConfig);
// create the config for the actual repository
String repositoryId = "spin-test";
RepositoryConfig repConfig = new RepositoryConfig(repositoryId, repositoryTypeSpec);
manager.addRepositoryConfig(repConfig);

// get the Repository from the manager
Repository repository = manager.getRepository(repositoryId);
{{< / highlight >}}

While this configuration already allows you to do many useful things, it does not do complete SPIN reasoning: the SpinSail relies on basic RDFS inferencing to be supplied by the underlying Sail stack. This means that for use cases where you need to rely on things like transitivity of rdfs:subClassOf relations, you should configure a Sail stack that includes the SchemaCachingRDFSInferencer. In addition, a DedupingInferencer is supplied which is a small optimization for both reasoners: it takes care to filter out potential duplicate results – though at the cost of an increase in memory usage. The full configuration with both additional inferencers looks like this:

{{< highlight java >}}
// create a basic Sail Stack with a simple Memory Store, full RDFS reasoning,
// and SPIN inferencing support
SpinSail spinSail = new SpinSail();
spinSail.setBaseSail(
        new SchemaCachingRDFSInferencer(
               new DedupingInferencr(new MemoryStore())
        )
);
// create a repository with the Sail stack:
Repository rep = new SailRepository(spinSail);
rep.init();
{{< / highlight >}}

or using configuration via the RepositoryManager:

{{< highlight java >}}
// create the config for the sail stack
SailImplConfig spinSailConfig = new SpinSailConfig(
           new SchemaCachingRDFSInferencerConfig(
                 new DedupingInferencerConfig(new MemoryStoreConfig())
           )
);
RepositoryImplConfig repositoryTypeSpec = new SailRepositoryConfig(spinSailConfig);
// create the config for the actual repository
String repositoryId = "spin-test";
RepositoryConfig repConfig = new RepositoryConfig(repositoryId, repositoryTypeSpec);
manager.addRepositoryConfig(repConfig);

// get the Repository from the manager
Repository repository = manager.getRepository(repositoryId);
{{< / highlight >}}

# Adding rules

Once your repository is set up with SPIN support, you can add rules by simply uploading an RDF document contain SPIN rules (which are expressed in RDF using the SPIN vocabulary). The SpinSail will automatically execute these rules on the data.

As an example, consider the following data:

    @prefix ex: <http://example.org/>.

    ex:John a ex:Father ;
            ex:parentOf ex:Lucy .

    ex:Lucy a ex:Person .

Now assume we wish to introduce a rule that defines persons who are the object of the ex:parentOf relation to be subject of an ex:childOf relation (in other words, we want to infer the inverse relationship for the parent-child relation). In SPIN, this could be done with the following rule:

    @prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>.
    @prefix sp: <http://spinrdf.org/sp#>.
    @prefix spin: <http://spinrdf.org/spin#>.
    @prefix ex: <http://example.org/>.

    // every person who has a parent is a child of that parent.
    ex:Person a rdfs:Class ;
            spin:rule [
                    a sp:Construct ;
            sp:text """PREFIX ex: <http://example.org/>
                       CONSTRUCT { ?this ex:childOf ?parent . }
                       WHERE { ?parent ex:parentOf ?this . }"""
    ] .

To get the SpinSail to execute this rule, all you need to do is upload both above RDF datasets to the Repository. The relation will be automatically inferred at data upload time, so the query:

    SELECT ?child WHERE { ?child ex:childOf ?parent }

will give this result:
<table border=1>
<tr><th style="padding: 4px">child</th></tr>
<tr><td style="padding: 4px"><code>ex:Lucy</code></td></tr>
</table>

# Further reading

Here are some useful links to learn more about SPIN:

- [SPIN Primer](http://spinrdf.org/spinsquare.html)
- [Getting Started with SPIN (by Topquadrant)](http://www.topquadrant.com/spin/tutorial/)

