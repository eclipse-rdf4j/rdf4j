---
title: "Full-text indexing with the Lucene SAIL"
layout: "doc"
hide_page_title: "true"
---

# Full text indexing with the Lucene SAIL

The LuceneSail enables you to add full text search of RDF literals to find subject resources to any Sail stack. It provides querying support for the following statement patterns:

    PREFIX search: <http://www.openrdf.org/contrib/lucenesail#>

    ?subj search:matches [
                  search:query "search terms...";
                  search:property my:property;
                  search:score ?score;
                  search:snippet ?snippet ] .

The 'virtual' properties in the `search:` namespace have the following meaning:

- `search:matches` – links the resource to be found with the following query statements (required)
- `search:query` – specifies the Lucene query (required)
- `search:property` – specifies the property to search. If omitted all properties are searched (optional)
- `search:score` – specifies a variable for the score (optional)
- `search:snippet` – specifies a variable for a highlighted snippet (optional)

## Configuration

The LuceneSail is a stacked Sail: to use it, simply wrap your base SAIL with it:

{{< highlight java >}}
Sail baseSail = new NativeStore(new File("."));
LuceneSail lucenesail = new LuceneSail();
// set any parameters, this one stores the Lucene index files into memory
lucenesail.setParameter(LuceneSail.LUCENE_RAMDIR_KEY, "true");
...
// wrap base sail
lucenesail.setBaseSail(baseSail);
{{< / highlight >}}

## Full text search

Search is case-insensitive, wildcards and other modifiers can be used to broaden the search. For example, search all literals containing words starting with "alic" (e.g. persons named "Alice"):

{{< highlight java >}}
....
Repository repo = new SailRepository(lucenesail);
repo.init();

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
{{< / highlight >}}

## SearchIndex implementations

The LuceneSail can currently be used with five SearchIndex implementations:

<table border=1 style="padding: 10px;">
<tr>
<th></th>
<th style="padding: 4px">SearchIndex implementation</th>
<th style="padding: 4px">Maven module</th>
</tr>
<tr>
<td style="padding: 4px">Apache Lucene 5</td>
<td style="padding: 4px"><code>org.eclipse.rdf4j.sail.lucene.LuceneIndex</code></td>
<td style="padding: 4px"><code>org.eclipse.rdf4j:rdf4j-sail-lucene</code></td>
</tr>
<tr>
<td style="padding: 4px">ElasticSearch</td>
<td style="padding: 4px"><code>org.eclipse.rdf4j.sail.elasticsearch.ElasticSearchIndex</code></td>
<td style="padding: 4px"><code>org.eclipse.rdf4j:rdf4j-sail-elasticsearch</code></td>
</tr>
<tr>
<td style="padding: 4px">Apache Solr Embedded</td>
<td style="padding: 4px"><code>org.eclipse.rdf4j.sail.solr.SolrIndex</code></td>
<td style="padding: 4px"><code>org.eclipse.rdf4j:rdf4j-sail-solr</code></td>
</tr>
</table>


Each SearchIndex implementation can easily be extended if you need to add extra features or store/access data with a different schema.

The following example uses a local Solr instance running on the default port 8983. Make sure that both the Apache httpcore and commons-logging jars are in the classpath, and that the Solr core uses an appropriate schema (an example can be found in RDF4J’s embedded solr source code on GitHub).

{{< highlight java >}}
import org.eclipse.rdf4j.sail.solr.SolrIndex;
....
LuceneSail luceneSail = new LuceneSail();
luceneSail.setParameter(LuceneSail.INDEX_CLASS_KEY, SolrIndex.class.getName());
luceneSail.setParameter(SolrIndex.SERVER_KEY, "http://localhost:8983/solr/rdf4j");

If needed, the Solr Client can be accessed via:

SolrIndex index = (SolrIndex) luceneSail.getLuceneIndex();
SolrClient client = index.getClient();
{{< / highlight >}}

