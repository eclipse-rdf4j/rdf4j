---
title: "Reasoning and Validation with SPIN"
layout: "doc"
hide_page_title: "true"
---

# Reasoning and Validation with SPIN

The SPARQL Inferencing Notation (SPIN) is a way to represent a wide range of business rules on top of an RDF dataset. These rules can be anything from constraint validation to inferred property value calculation.

**SPIN is no longer recommended. The SpinSail does not scale and simple delete operations take seconds to execute. If you are considering using SPIN for validation, try SHACL and the ShaclSail instead. Should you still want to use SPIN for inference, it is recommended to disable the validation step for better performance: `spinSail.setValidateConstraints(false)` (since 3.1.0). SPIN was never designed to work in a transactional environment, which means that you should expect odd scenarios where you update your data without new data being inferred or old inferred data still sticking around.**

The `SpinSail` is a StackedSail component that adds a forward-chaining SPIN rule engine on top of any store. In its most basic form it can be used directly on top of a Sail:

{{< highlight java >}}
// create a basic Sail Stack with a simple Memory Store and SPIN inferencing support
SpinSail spinSail = new SpinSail();
spinSail.setBaseSail(new MemoryStore());
// create a repository with the Sail stack:
Repository rep = new SailRepository(spinSail);
rep.init();
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

# Limitations
The SpinSail attempts to only run relevant rules by detecting if data related to the rule has changed. This is only done by checking if any of the subjects in the added data have the type required by the rule. There is no analysis of the query, so if your query contains more than a simple `?a ex:pred ?b` then you will run into incomplete inference in the face of updates.

An example of a rule that will lead to incomplete inference results in the face of updates:

    @prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>.
    @prefix sp: <http://spinrdf.org/sp#>.
    @prefix spin: <http://spinrdf.org/spin#>.
    @prefix ex: <http://example.org/>.

    // if you are the parent of a parent of a child, that child is your grandchild.
    ex:Person a rdfs:Class ;
            spin:rule [
                    a sp:Construct ;
            sp:text """PREFIX ex: <http://example.org/>
                       CONSTRUCT { ?this ex:grandchildOf ?grandparent . }
                       WHERE { ?grandparent ex:parentOf/ex:parentOf ?this . }"""
    ] .

If `ex:Peter ex:parentOf ex:PeterJr` and `ex:PeterJr a ex:Person`, then adding `ex:Selma ex:parentOf ex:Peter` will not lead to `ex:PeterJr ex:grandchildOf ex:Selma` being true because the SpinSail does not understand that adding `ex:Selma ex:parentOf ex:Peter` should trigger the rule for `ex:PeterJr`.

Removal of already infered data is only done when a statement is deleted. This means that the use of aggregation, negation og subselects (and possibly other cases) will lead to incorrect inference where old inferred statements will still remain in your data.

An example of a rule with negation that will lead to incorrect (stale) inference:

    @prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>.
    @prefix sp: <http://spinrdf.org/sp#>.
    @prefix spin: <http://spinrdf.org/spin#>.
    @prefix ex: <http://example.org/>.

    // A child is an Only Child if their parent's have no other children
    ex:Person a rdfs:Class ;
            spin:rule [
                    a sp:Construct ;
            sp:text """PREFIX ex: <http://example.org/>
                       CONSTRUCT { ?this a ex:OnlyChild . }
                       WHERE { 
                        ?parent ex:parentOf ?this . 
                        FILTER( NOT EXISTS {?parent ex:parentOf ?otherChild. FILTER(?this != ?otherChild)} )
                       
                       }"""
    ] .

Adding `ex:Peter ex:parentOf ex:PeterJr` and `ex:PeterJr a ex:Person` will lead to `ex:PeterJr a ex:OnlyChild` being true. This is correct. Adding `ex:Peter ex:parentOf ex:Caroline` means that `ex:PeterJr` should not be an Only Child anymore (according to the rule). `ex:PeterJr a ex:OnlyChild` will still be true even after adding `ex:Peter ex:parentOf ex:Caroline` because the SpinSail does not refresh already inferred data when there are no user-initiated deletions.

# Performance

Performance is largely dictated by how complex your rules and constraints are.

Removing a statement will force all inferred data to be removed and reinferred. In the best case this will take a second or two on modern hardware, because even an empty SpinSail contains a number of default SPIN rules and constraints. Adding your own rules, constraints and data will only make this slower. 

Disabling constraint validation will improve performance: `spinSail.setValidateConstraints(false)`

# Further reading

Here are some useful links to learn more about SPIN:

- [SPIN Primer](http://spinrdf.org/spinsquare.html)
- [Getting Started with SPIN (by Topquadrant)](http://www.topquadrant.com/spin/tutorial/)

