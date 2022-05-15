/*******************************************************************************
 Copyright (c) 2018 Eclipse RDF4J contributors.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the Eclipse Distribution License v1.0
 which accompanies this distribution, and is available at
 http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sparqlbuilder.examples.updatespec;

import static org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatterns.and;
import static org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf.iri;

import org.eclipse.rdf4j.sparqlbuilder.constraint.Expressions;
import org.eclipse.rdf4j.sparqlbuilder.core.Prefix;
import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.core.query.AddQuery;
import org.eclipse.rdf4j.sparqlbuilder.core.query.ClearQuery;
import org.eclipse.rdf4j.sparqlbuilder.core.query.CopyQuery;
import org.eclipse.rdf4j.sparqlbuilder.core.query.CreateQuery;
import org.eclipse.rdf4j.sparqlbuilder.core.query.DeleteDataQuery;
import org.eclipse.rdf4j.sparqlbuilder.core.query.DropQuery;
import org.eclipse.rdf4j.sparqlbuilder.core.query.InsertDataQuery;
import org.eclipse.rdf4j.sparqlbuilder.core.query.LoadQuery;
import org.eclipse.rdf4j.sparqlbuilder.core.query.ModifyQuery;
import org.eclipse.rdf4j.sparqlbuilder.core.query.MoveQuery;
import org.eclipse.rdf4j.sparqlbuilder.core.query.Queries;
import org.eclipse.rdf4j.sparqlbuilder.examples.BaseExamples;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPattern;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatterns;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.TriplePattern;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Iri;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf;
import org.junit.Assert;
import org.junit.Test;

/**
 * Follows the SPARQL 1.1 Update Spec starting <a href="https://www.w3.org/TR/sparql11-update/#graphManagement">here</a>
 */
public class Section3Test extends BaseExamples {
	Prefix dc = SparqlBuilder.prefix("dc", iri("http://purl.org/dc/elements/1.1/"));
	Prefix ns = SparqlBuilder.prefix("ns", iri(EXAMPLE_ORG_NS));
	Prefix foaf = SparqlBuilder.prefix("foaf", iri("http://xmlns.com/foaf/0.1/"));
	Prefix xsd = SparqlBuilder.prefix("xsd", iri("http://www.w3.org/2001/XMLSchema#"));
	Prefix rdf = SparqlBuilder.prefix("rdf", iri("http://www.w3.org/1999/02/22-rdf-syntax-ns#"));

	/**
	 * PREFIX dc: <http://purl.org/dc/elements/1.1/> INSERT DATA { <http://example/book1> dc:title "A new book" ;
	 * dc:creator "A.N.Other" . }
	 */
	@Test
	public void example_1() {
		InsertDataQuery insertDataQuery = Queries.INSERT_DATA();

		insertDataQuery.prefix(dc)
				.insertData(iri("http://example/book1").has(dc.iri("title"), Rdf.literalOf("A new book"))
						.andHas(dc.iri("creator"), Rdf.literalOf("A.N.Other")));
		Assert.assertThat(insertDataQuery.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"PREFIX dc: <http://purl.org/dc/elements/1.1/> "
						+ "INSERT DATA { "
						+ "<http://example/book1> dc:title \"A new book\" ;\n"
						+ "	dc:creator \"A.N.Other\" . "
						+ "}"
		));
	}

	/**
	 * PREFIX dc: <http://purl.org/dc/elements/1.1/> PREFIX ns: <http://example.org/ns#> INSERT DATA { GRAPH
	 * <http://example/bookStore> { <http://example/book1> ns:price 42 } }
	 */
	@Test
	public void example_2() {
		InsertDataQuery insertDataQuery = Queries.INSERT_DATA();

		insertDataQuery.prefix(dc, ns)
				.insertData(iri("http://example/book1").has(ns.iri("price"), Rdf.literalOf(42)))
				.into(iri("http://example/bookStore"));

		Assert.assertThat(insertDataQuery.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"PREFIX dc: <http://purl.org/dc/elements/1.1/> "
						+ "PREFIX ns: <https://example.org/ns#> "
						+ "INSERT DATA { GRAPH "
						+ "<http://example/bookStore> { <http://example/book1> ns:price 42 . } "
						+ "}"
		));
	}

	/**
	 * PREFIX dc: <http://purl.org/dc/elements/1.1/>
	 *
	 * DELETE DATA { <http://example/book2> dc:title "David Copperfield" ; dc:creator "Edmund Wells" . }
	 */
	@Test
	public void example_3() {
		DeleteDataQuery deleteDataQuery = Queries.DELETE_DATA().prefix(dc);

		deleteDataQuery.deleteData(iri("http://example/book2").has(dc.iri("title"), Rdf.literalOf("David Copperfield"))
				.andHas(dc.iri("creator"), Rdf.literalOf("Edmund Wells")));

		Assert.assertThat(deleteDataQuery.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"PREFIX dc: <http://purl.org/dc/elements/1.1/>\n"
						+ "DELETE DATA { "
						+ "<http://example/book2> dc:title \"David Copperfield\" ; "
						+ "dc:creator \"Edmund Wells\" . "
						+ "}"
		));
	}

	/**
	 * PREFIX dc: <http://purl.org/dc/elements/1.1/> DELETE DATA { GRAPH <http://example/bookStore> {
	 * <http://example/book1> dc:title "Fundamentals of Compiler Desing" } } ;
	 *
	 * PREFIX dc: <http://purl.org/dc/elements/1.1/> INSERT DATA { GRAPH <http://example/bookStore> {
	 * <http://example/book1> dc:title "Fundamentals of Compiler Design" } }
	 */
	@Test
	public void example_4() {
		Iri bookStore = iri("http://example/bookStore"), exampleBook = iri("http://example/book1"),
				title = dc.iri("title");

		DeleteDataQuery deleteTypoQuery = Queries.DELETE_DATA()
				.prefix(dc)
				.deleteData(exampleBook.has(title, "Fundamentals of Compiler Desing"))
				.from(bookStore);
		Assert.assertThat(deleteTypoQuery.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"PREFIX dc: <http://purl.org/dc/elements/1.1/> "
						+ "DELETE DATA { "
						+ "GRAPH <http://example/bookStore> {\n"
						+ " <http://example/book1> dc:title \"Fundamentals of Compiler Desing\" . } "
						+ "} "
		));
		InsertDataQuery insertFixedTitleQuery = Queries.INSERT_DATA()
				.prefix(dc)
				.insertData(exampleBook.has(title, "Fundamentals of Compiler Design"))
				.into(bookStore);
		Assert.assertThat(insertFixedTitleQuery.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"PREFIX dc: <http://purl.org/dc/elements/1.1/> "
						+ "INSERT DATA { "
						+ "GRAPH <http://example/bookStore> {\n"
						+ "<http://example/book1> dc:title \"Fundamentals of Compiler Design\" . }"
						+ " }"
		));
	}

	@Test
	public void example_with() {
		TriplePattern abc = GraphPatterns.tp(SparqlBuilder.var("a"), SparqlBuilder.var("b"), SparqlBuilder.var("c"));
		TriplePattern xyz = GraphPatterns.tp(SparqlBuilder.var("x"), SparqlBuilder.var("y"), SparqlBuilder.var("z"));
		ModifyQuery modify = Queries.MODIFY();
		Iri g1 = () -> "<g1>";
		GraphPattern examplePattern = () -> " ... ";

		// WITH <g1> DELETE { a b c } INSERT { x y z } WHERE { ... }
		modify.with(g1).delete(abc).insert(xyz).where(examplePattern);
		Assert.assertThat(modify.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"WITH <g1> DELETE { ?a ?b ?c . } INSERT { ?x ?y ?z . } where { ... }"
		));

		// DELETE { GRAPH <g1> { a b c } } INSERT { GRAPH <g1> { x y z } } USING <g1>
		// WHERE { ... }
		modify.with((Iri) null).delete(abc).from(g1).insert(xyz).into(g1).using(g1).where(examplePattern);
		Assert.assertThat(modify.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"DELETE { GRAPH <g1> {?a ?b ?c . } } "
						+ "INSERT { GRAPH <g1> { ?x ?y ?z .} } "
						+ "USING <g1> WHERE { ... }"
		));
	}

	/**
	 * PREFIX foaf: <http://xmlns.com/foaf/0.1/>
	 *
	 * WITH <http://example/addresses> DELETE { ?person foaf:givenName 'Bill' } INSERT { ?person foaf:givenName
	 * 'William' } WHERE { ?person foaf:givenName 'Bill' }
	 */
	@Test
	public void example_5() {
		Variable person = SparqlBuilder.var("person");
		ModifyQuery modify = Queries.MODIFY();

		modify.prefix(foaf)
				.with(iri("http://example/addresses"))
				.delete(person.has(foaf.iri("givenName"), "Bill"))
				.insert(person.has(foaf.iri("givenName"), "William"))
				.where(person.has(foaf.iri("givenName"), "Bill"));

		Assert.assertThat(modify.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n"
						+ "WITH <http://example/addresses> "
						+ "DELETE { ?person foaf:givenName \"Bill\" .} "
						+ "INSERT { ?person foaf:givenName \"William\" .} "
						+ "WHERE { ?person foaf:givenName \"Bill\" . }"
		));
	}

	/**
	 * PREFIX dc: <http://purl.org/dc/elements/1.1/> PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
	 *
	 * DELETE { ?book ?p ?v } WHERE { ?book dc:date ?date . FILTER ( ?date > "1970-01-01T00:00:00-02:00"^^xsd:dateTime )
	 * ?book ?p ?v }
	 */
	@Test
	public void example_6() {
		Variable book = SparqlBuilder.var("book"), p = SparqlBuilder.var("p"), v = SparqlBuilder.var("v"),
				date = SparqlBuilder.var("date");

		ModifyQuery modify = Queries.MODIFY();

		modify.prefix(dc, xsd)
				.delete(book.has(p, v))
				.where(GraphPatterns.and(book.has(dc.iri("date"), date), book.has(p, v))
						.filter(Expressions.gt(date,
								Rdf.literalOfType("1970-01-01T00:00:00-02:00", xsd.iri("dateTime")))));
		Assert.assertThat(modify.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"PREFIX dc: <http://purl.org/dc/elements/1.1/> "
						+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n"
						+ "DELETE { ?book ?p ?v . } "
						+ "WHERE { "
						+ "?book dc:date ?date . "
						+ "?book ?p ?v ."
						+ "FILTER ( ?date > \"1970-01-01T00:00:00-02:00\"^^xsd:dateTime )\n"
						+ "}"
		));
	}

	/**
	 * PREFIX foaf: <http://xmlns.com/foaf/0.1/>
	 *
	 * WITH <http://example/addresses> DELETE { ?person ?property ?value } WHERE { ?person ?property ?value ;
	 * foaf:givenName 'Fred' }
	 */
	@Test
	public void example_7() {
		Variable person = SparqlBuilder.var("person"), property = SparqlBuilder.var("property"),
				value = SparqlBuilder.var("value");

		ModifyQuery modify = Queries.MODIFY()
				.prefix(foaf)
				.with(iri("http://example/addresses"))
				.delete(person.has(property, value))
				.where(person.has(property, value).andHas(foaf.iri("givenName"), "Fred"));

		Assert.assertThat(modify.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n"
						+ "WITH <http://example/addresses> "
						+ "DELETE { ?person ?property ?value .} "
						+ "WHERE { "
						+ "?person ?property ?value ;\n"
						+ "foaf:givenName \"Fred\" ."
						+ "}"
		));
	}

	/**
	 * PREFIX dc: <http://purl.org/dc/elements/1.1/> PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
	 *
	 * INSERT { GRAPH <http://example/bookStore2> { ?book ?p ?v } } WHERE { GRAPH <http://example/bookStore> { ?book
	 * dc:date ?date . FILTER ( ?date > "1970-01-01T00:00:00-02:00"^^xsd:dateTime ) ?book ?p ?v } }
	 */
	@Test
	public void example_8() {
		Variable book = SparqlBuilder.var("book"), p = SparqlBuilder.var("p"), v = SparqlBuilder.var("v"),
				date = SparqlBuilder.var("date");

		ModifyQuery modify = Queries.MODIFY()
				.prefix(dc, xsd)
				.insert(book.has(p, v))
				.into(iri("http://example/bookStore2"))
				.where(and(book.has(dc.iri("date"), date), book.has(p, v)).from(iri("http://example/bookStore"))
						.filter(Expressions.gt(date,
								Rdf.literalOfType("1970-01-01T00:00:00-02:00", xsd.iri("dateTime")))));
		Assert.assertThat(modify.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"PREFIX dc: <http://purl.org/dc/elements/1.1/> "
						+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n"
						+ "INSERT { "
						+ "GRAPH <http://example/bookStore2> { ?book ?p ?v .} } "
						+ "WHERE { GRAPH <http://example/bookStore> { "
						+ "?book dc:date ?date . "
						+ "?book ?p ?v ."
						+ "FILTER ( ?date > \"1970-01-01T00:00:00-02:00\"^^xsd:dateTime ) "
						+ "} "
						+ "}"
		));
	}

	/**
	 * PREFIX foaf: <http://xmlns.com/foaf/0.1/> PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
	 *
	 * INSERT { GRAPH <http://example/addresses> { ?person foaf:name ?name . ?person foaf:mbox ?email } } WHERE { GRAPH
	 * <http://example/people> { ?person foaf:name ?name . OPTIONAL { ?person foaf:mbox ?email } } }
	 */
	@Test
	public void example_9() {
		Variable person = SparqlBuilder.var("person"), name = SparqlBuilder.var("name"),
				email = SparqlBuilder.var("email");
		TriplePattern personNameTriple = person.has(foaf.iri("name"), name),
				personEmailTriple = person.has(foaf.iri("mbox"), email);

		ModifyQuery insertAddressesQuery = Queries.MODIFY()
				.prefix(foaf, rdf)
				.insert(personNameTriple, personEmailTriple)
				.into(iri("http://example/addresses"))
				.where(and(personNameTriple, GraphPatterns.optional(personEmailTriple))
						.from(iri("http://example/people")));

		Assert.assertThat(insertAddressesQuery.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"PREFIX foaf: <http://xmlns.com/foaf/0.1/> "
						+ "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
						+ "INSERT { "
						+ "GRAPH <http://example/addresses> { "
						+ "?person foaf:name ?name . "
						+ "?person foaf:mbox ?email ."
						+ "} "
						+ "} "
						+ "WHERE { "
						+ "GRAPH <http://example/people> { "
						+ "?person foaf:name ?name . "
						+ "OPTIONAL { ?person foaf:mbox ?email .} "
						+ "} "
						+ "}"
		));
	}

	/**
	 * PREFIX dc: <http://purl.org/dc/elements/1.1/> PREFIX dcmitype: <http://purl.org/dc/dcmitype/> PREFIX xsd:
	 * <http://www.w3.org/2001/XMLSchema#>
	 *
	 * INSERT { GRAPH <http://example/bookStore2> { ?book ?p ?v } } WHERE { GRAPH <http://example/bookStore> { ?book
	 * dc:date ?date . FILTER ( ?date < "2000-01-01T00:00:00-02:00"^^xsd:dateTime ) ?book ?p ?v } } ;
	 *
	 * WITH <http://example/bookStore> DELETE { ?book ?p ?v } WHERE { ?book dc:date ?date ; dc:type
	 * dcmitype:PhysicalObject . FILTER ( ?date < "2000-01-01T00:00:00-02:00"^^xsd:dateTime ) ?book ?p ?v }
	 */
	@Test
	public void example_10() {
		Variable book = SparqlBuilder.var("book"), p = SparqlBuilder.var("p"), v = SparqlBuilder.var("v"),
				date = SparqlBuilder.var("date");
		Prefix dcmitype = SparqlBuilder.prefix("dcmitype", iri("http://purl.org/dc/dcmitype/"));

		ModifyQuery insertIntobookStore2Query = Queries.MODIFY()
				.prefix(dc, dcmitype, xsd)
				.insert(book.has(p, v))
				.into(iri("http://example/bookStore2"))
				.where(and(book.has(dc.iri("date"), date), book.has(p, v)).from(iri("http://example/bookStore"))
						.filter(Expressions.lt(date,
								Rdf.literalOfType("1970-01-01T00:00:00-02:00", xsd.iri("dateTime")))));
		Assert.assertThat(insertIntobookStore2Query.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"PREFIX dc: <http://purl.org/dc/elements/1.1/> "
						+ "PREFIX dcmitype: <http://purl.org/dc/dcmitype/> "
						+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n"
						+ "INSERT { "
						+ "GRAPH <http://example/bookStore2> { "
						+ "?book ?p ?v ."
						+ "} "
						+ "} "
						+ "WHERE { "
						+ "GRAPH <http://example/bookStore> { "
						+ "?book dc:date ?date . "
						+ "?book ?p ?v ."
						+ "FILTER ( ?date < \"1970-01-01T00:00:00-02:00\"^^xsd:dateTime ) "
						+ "} "
						+ "}"
		));
		ModifyQuery deleteFromBookStoreQuery = Queries.MODIFY()
				.with(iri("http://example/bookStore"))
				.delete(book.has(p, v))
				.where(and(book.has(dc.iri("date"), date).andHas(dc.iri("type"), dcmitype.iri("PhysicalObject")),
						book.has(p, v))
								.filter(Expressions.lt(date,
										Rdf.literalOfType("2000-01-01T00:00:00-02:00", xsd.iri("dateTime")))));

		Assert.assertThat(deleteFromBookStoreQuery.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"WITH <http://example/bookStore>\n"
						+ "DELETE { ?book ?p ?v . }\n"
						+ "WHERE { ?book dc:date ?date ;\n"
						+ "    dc:type dcmitype:PhysicalObject .\n"
						+ "?book ?p ?v .\n"
						+ "FILTER ( ?date < \"2000-01-01T00:00:00-02:00\"^^xsd:dateTime ) }"
		));
	}

	/**
	 * PREFIX foaf: <http://xmlns.com/foaf/0.1/>
	 *
	 * DELETE WHERE { ?person foaf:givenName 'Fred'; ?property ?value }
	 */
	@Test
	public void example_11() {
		Variable person = SparqlBuilder.var("person"), property = SparqlBuilder.var("property"),
				value = SparqlBuilder.var("value");

		ModifyQuery modify = Queries.MODIFY()
				.prefix(foaf)
				.delete()
				.where(person.has(foaf.iri("givenName"), "Fred").andHas(property, value));
		Assert.assertThat(modify.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n"
						+ "DELETE WHERE {"
						+ " ?person foaf:givenName \"Fred\";"
						+ " ?property ?value . "
						+ "}"
		));
	}

	/**
	 * PREFIX foaf: <http://xmlns.com/foaf/0.1/>
	 *
	 * DELETE WHERE { GRAPH <http://example.com/names> { ?person foaf:givenName 'Fred' ; ?property1 ?value1 } GRAPH
	 * <http://example.com/addresses> { ?person ?property2 ?value2 } }
	 */
	@Test
	public void example_12() {
		Variable person = SparqlBuilder.var("person"), property1 = SparqlBuilder.var("property1"),
				value1 = SparqlBuilder.var("value1"), property2 = SparqlBuilder.var("property2"),
				value2 = SparqlBuilder.var("value2");
		Iri namesGraph = iri("http://example.com/names"), addressesGraph = iri("http://example.com/addresses");

		ModifyQuery deleteFredFromNamesAndAddressesQuery = Queries.MODIFY()
				.prefix(foaf)
				.delete()
				.where(and(person.has(foaf.iri("givenName"), "Fred").andHas(property1, value1)).from(namesGraph),
						and(person.has(property2, value2)).from(addressesGraph));

		Assert.assertThat(deleteFredFromNamesAndAddressesQuery.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n"
						+ "DELETE WHERE { "
						+ "GRAPH <http://example.com/names> { "
						+ "?person foaf:givenName \"Fred\" ; "
						+ "?property1 ?value1 . "
						+ "} "
						+ "GRAPH <http://example.com/addresses> { "
						+ "?person ?property2 ?value2 ."
						+ " } "
						+ "}"
		));
	}

	@Test
	public void example_load() {
		LoadQuery load = Queries.LOAD().from(iri(EXAMPLE_ORG_NS));
		Assert.assertThat(load.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"LOAD <https://example.org/ns#>"
		));
		load = Queries.LOAD().silent().from(iri(EXAMPLE_ORG_NS)).to(iri(EXAMPLE_COM_NS));
		Assert.assertThat(load.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"LOAD SILENT <https://example.org/ns#> INTO GRAPH <https://example.com/ns#>"
		));
	}

	@Test
	public void example_clear() {
		ClearQuery clear = Queries.CLEAR().def();
		Assert.assertThat(clear.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"CLEAR DEFAULT"
		));
		clear = Queries.CLEAR().silent().graph(iri(EXAMPLE_ORG_NS));
		Assert.assertThat(clear.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"CLEAR SILENT GRAPH <https://example.org/ns#>"
		));
	}

	@Test
	public void example_create() {
		CreateQuery create = Queries.CREATE().graph(iri(EXAMPLE_ORG_NS));
		Assert.assertThat(create.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"CREATE GRAPH <https://example.org/ns#>"
		));
		create = Queries.CREATE().silent().graph(iri(EXAMPLE_ORG_NS));
		Assert.assertThat(create.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"CREATE SILENT GRAPH <https://example.org/ns#>"
		));
	}

	@Test
	public void example_drop() {
		DropQuery drop = Queries.DROP().def();
		Assert.assertThat(drop.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"DROP DEFAULT"
		));
		drop = Queries.DROP().silent().graph(iri(EXAMPLE_ORG_NS));
		Assert.assertThat(drop.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"DROP SILENT GRAPH <https://example.org/ns#>"
		));
	}

	/** COPY DEFAULT TO \<http://example.org/named> */
	@Test
	public void example_13() {
		CopyQuery copy = Queries.COPY().fromDefault().to(iri("http://example.org/named"));
		Assert.assertThat(copy.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"COPY DEFAULT TO <http://example.org/named>"
		));
	}

	/** MOVE DEFAULT TO <http://example.org/named> */
	@Test
	public void example_14() {
		MoveQuery move = Queries.MOVE().fromDefault().to(iri("http://example.org/named"));
		Assert.assertThat(move.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"MOVE DEFAULT TO <http://example.org/named>"
		));
	}

	/** ADD DEFAULT TO <http://example.org/named> */
	@Test
	public void example_15() {
		AddQuery add = Queries.ADD().fromDefault().to(iri("http://example.org/named"));
		Assert.assertThat(add.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"ADD DEFAULT TO <http://example.org/named>"
		));
	}

}
