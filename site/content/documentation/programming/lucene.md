---
title: "Full-text indexing with the Lucene SAIL"
toc: true
weight: 5
autonumbering: true
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

## Configuration

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

### Language filtering

You can add a filter to only index literals with particular languages, for example:

```java
// this sail will now will only index French literals
lucenesail.setParameter(LuceneSail.INDEXEDLANG, "fr");
```

To use multiple languages, split them with spaces, for example:

```java
// this sail will now only index French and English literals
lucenesail.setParameter(LuceneSail.INDEXEDLANG, "fr en");
```

### Type filtering

You can add a filter to only index literals of subject with particular type, for example with the subject/literals

```turle
@prefix my: <http://example.org/> .

my:subject1 my:oftype my:type1 ;
            my:prop   "text"   .

my:subject2 my:oftype my:type2 ;
            my:prop   "text"   .
```

To only index the literals of the subjects with the type ``my:type1``, you can use the type filter parameter:

```java
// this sail will now only index literals of subjects ?s with the triple (?s ex:oftype ex:type1).
lucenesail.setParameter(LuceneSail.INDEXEDTYPES, "http\\://example.org/oftype=http\\://example.org/type1");
```

You can specify multiple types for the same type predicate by splitting them with spaces, you can specify multiple type predicates by splitting them with new lines, example:

```java
// this sail will now only index literals of subjects ?s with the triple:
// (?s ex:oftype1 ex:type11), (?s ex:oftype1 ex:type12), (?s ex:oftype2 ex:type21) 
// or (?s ex:oftype2 ex:type22).
lucenesail.setParameter(LuceneSail.INDEXEDTYPES, 
		"http\\://example.org/oftype1=http\\://example.org/type11 http\\://example.org/type12\n"
		"http\\://example.org/oftype2=http\\://example.org/type21 http\\://example.org/type22"
);
```

You can use the special predicate ``a`` instead of ``rdf:type``.

You can also reduce the usage of the base sail to set the type of backtracking:

- ``TypeBacktraceMode.COMPLETE``: (**default**) will check every triples with ?s and try to add or remove them in the Lucene Index.
- ``TypeBacktraceMode.PARTIAL``: won't check previous triples in the store, assume that the user would add new elements to the index after and with the add of a type triple and would remove elements to the index with the remove of type.

```java
// the sail won't search for the type a triple if the type isn't in the UPDATE request
lucenesail.setIndexBacktraceMode(TypeBacktraceMode.PARTIAL);
```

## Full text search

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

## SearchIndex implementations

The LuceneSail can currently be used with three SearchIndex implementations:

|                   |  SearchIndex implementation                 | Maven module                          |
|------------------ |---------------------------------------------|---------------------|
| Apache Lucene     | `org.eclipse.rdf4j.sail.lucene.impl.LuceneIndex` | `rdf4j-sail-lucene` |
| ElasticSearch     | `org.eclipse.rdf4j.sail.elasticsearch.ElasticsearchIndex` | `rdf4j-sail-elasticsearch` |
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

