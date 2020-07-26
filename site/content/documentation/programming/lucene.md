---
title: "Full-text indexing with the Lucene SAIL"
toc: true
weight: 5
---
The LuceneSail enables you to add full text search of RDF literals to find subject resources to any Sail stack. 
<!--more-->
It provides querying support for the following statement patterns:

```sparql
PREFIX search: <http://www.openrdf.org/contrib/lucenesail#>

?subj search:matches [
	      search:query "search terms...";
	      search:property my:property;
	      search:score ?score;
	      search:snippet ?snippet ] .
```

The 'virtual' properties in the `search:` namespace have the following meaning:

- `search:matches` – links the resource to be found with the following query statements (required)
- `search:query` – specifies the Lucene query (required)
- `search:property` – specifies the property to search. If omitted all properties are searched (optional)
- `search:score` – specifies a variable for the score (optional)
- `search:snippet` – specifies a variable for a highlighted snippet (optional)

# Configuration

The LuceneSail is a stacked Sail: to use it, simply wrap your base SAIL with it:

```java
Sail baseSail = new NativeStore(new File("."));
LuceneSail lucenesail = new LuceneSail();
// set any parameters, this one stores the Lucene index files into memory
lucenesail.setParameter(LuceneSail.LUCENE_RAMDIR_KEY, "true");
...
// wrap base sail
lucenesail.setBaseSail(baseSail);
```

# Full text search

Search is case-insensitive, wildcards and other modifiers can be used to broaden the search. For example, search all literals containing words starting with "alic" (e.g. persons named "Alice"):

```java
....
Repository repo = new SailRepository(lucenesail);

// Get the subjects and a highlighted snippet
String qry = "PREFIX search: <http://www.openrdf.org/contrib/lucenesail#> " +
			"SELECT ?subj ?text " +
			"WHERE { ?subj search:matches [" +
					" search:query ?term ; " +
					" search:snippet ?text ] } ";

List<BindingSet> results;
try (RepositoryConnection con = repo.getConnection()) {
	ValueFactory fac = con.getValueFactory();

	TupleQuery tq = con.prepareTupleQuery(QueryLanguage.SPARQL, qry);
	// add wildcard '*' to perform wildcard search
	tq.setBinding("term", fac.createLiteral("alic" + "*"));

	// copy the results and processs them after the connection is closed
	results = QueryResults.asList(tq.evaluate());
}

results.forEach(res -> {
		System.out.println(res.getValue("subj").stringValue());
		System.out.println(res.getValue("text").stringValue());
});
```

# SearchIndex implementations

The LuceneSail can currently be used with five SearchIndex implementations:

|                   |  SearchIndex implementation                 | Maven module                          |
|------------------ |---------------------------------------------|---------------------|
| Apachce Lucence 6 | `org.eclipse.rdf4j.sail.lucene.LuceneIndex` | `rdf4j-sail-lucene` |
| ElasticSearch     | `org.eclipse.rdf4j.sail.elasticSearch.ElasticSearchIndex` | `rdf4j-sail-elasticsearch` |
| Apache Solr       | `org.eclipse.rdf4j.sail.solr.SolrIndex`     | `rdf4j-sail-solr`   |
 
Each SearchIndex implementation can easily be extended if you need to add extra features or store/access data with a different schema.

The following example uses a local Solr instance running on the default port 8983. Make sure that both the Apache httpcore and commons-logging jars are in the classpath, and that the Solr core uses an appropriate schema (an example can be found in RDF4J’s embedded solr source code on GitHub).

```java
import org.eclipse.rdf4j.sail.solr.SolrIndex;
....
LuceneSail luceneSail = new LuceneSail();
luceneSail.setParameter(LuceneSail.INDEX_CLASS_KEY, SolrIndex.class.getName());
luceneSail.setParameter(SolrIndex.SERVER_KEY, "http://localhost:8983/solr/rdf4j");
````

If needed, the Solr Client can be accessed via:

```java
SolrIndex index = (SolrIndex) luceneSail.getLuceneIndex();
SolrClient client = index.getClient();
```

