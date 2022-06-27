/*******************************************************************************
 Copyright (c) 2018 Eclipse RDF4J contributors.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the Eclipse Distribution License v1.0
 which accompanies this distribution, and is available at
 http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sparqlbuilder.examples.sparql11spec;

import static org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf.iri;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.sparqlbuilder.constraint.Expression;
import org.eclipse.rdf4j.sparqlbuilder.constraint.Expressions;
import org.eclipse.rdf4j.sparqlbuilder.core.Assignment;
import org.eclipse.rdf4j.sparqlbuilder.core.Prefix;
import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.examples.BaseExamples;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Iri;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf;
import org.junit.Assert;
import org.junit.Test;

public class Section11Test extends BaseExamples {
	@Test
	public void example_11_1() {
		Prefix base = SparqlBuilder.prefix(iri("http://books.example/"));
		Variable lprice = SparqlBuilder.var("lprice"), totalPrice = SparqlBuilder.var("totalPrice");

		Expression<?> sum = Expressions.sum(lprice);
		Assignment sumAsTotal = SparqlBuilder.as(sum, totalPrice);

		Variable org = SparqlBuilder.var("org"), auth = SparqlBuilder.var("auth"), book = SparqlBuilder.var("book");

		query.prefix(base)
				.select(sumAsTotal)
				.where(org.has(base.iri("affiliates"), auth), auth.has(base.iri("writesBook"), book),
						book.has(base.iri("price"), lprice))
				.groupBy(org)
				.having(Expressions.gt(sum, 10));
		Assert.assertThat(query.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"PREFIX : <http://books.example/>\n"
						+ "SELECT (SUM(?lprice) AS ?totalPrice)\n"
						+ "WHERE {\n"
						+ "  ?org :affiliates ?auth .\n"
						+ "  ?auth :writesBook ?book .\n"
						+ "  ?book :price ?lprice .\n"
						+ "}\n"
						+ "GROUP BY ?org\n"
						+ "HAVING (SUM(?lprice) > 10)"));
	}

	@Test
	public void example_11_1_model() {
		String ex = EXAMPLE_ORG_BOOK_NS;
		IRI affiliates = VF.createIRI(ex, "affiliates");
		IRI writesBook = VF.createIRI(ex, "writesBook");
		IRI price = VF.createIRI(ex, "price");
		IRI baseIri = VF.createIRI("http://books.example/");

		Prefix base = SparqlBuilder.prefix(baseIri);
		Variable lprice = SparqlBuilder.var("lprice"), totalPrice = SparqlBuilder.var("totalPrice");

		Expression<?> sum = Expressions.sum(lprice);
		Assignment sumAsTotal = SparqlBuilder.as(sum, totalPrice);

		Variable org = SparqlBuilder.var("org"), auth = SparqlBuilder.var("auth"), book = SparqlBuilder.var("book");

		query.prefix(base)
				.select(sumAsTotal)
				.where(org.has(affiliates, auth), auth.has(writesBook, book), book.has(price, lprice))
				.groupBy(org)
				.having(Expressions.gt(sum, 10));
		Assert.assertThat(query.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"PREFIX : <http://books.example/>\n"
						+ "SELECT (SUM(?lprice) AS ?totalPrice)\n"
						+ "WHERE {\n"
						+ "  ?org <http://example.org/book/affiliates> ?auth .\n"
						+ "  ?auth <http://example.org/book/writesBook> ?book .\n"
						+ "  ?book <http://example.org/book/price> ?lprice .\n"
						+ "}\n"
						+ "GROUP BY ?org\n"
						+ "HAVING (SUM(?lprice) > 10)"));
	}

	@Test
	public void example_11_2() {
		Prefix base = SparqlBuilder.prefix((Iri) null);
		Variable y = SparqlBuilder.var("y"), avg = SparqlBuilder.var("avg"), a = SparqlBuilder.var("a"),
				x = SparqlBuilder.var("x");

		query.select(SparqlBuilder.as(Expressions.avg(y), avg))
				.where(a.has(base.iri("x"), x).andHas(base.iri("y"), y))
				.groupBy(x);
		Assert.assertThat(query.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"SELECT (AVG(?y) AS ?avg)\n"
						+ "WHERE {\n"
						+ "  ?a :x ?x ;\n"
						+ "     :y ?y .\n"
						+ "}\n"
						+ "GROUP BY ?x"));
	}

	@Test
	public void example_11_3() {
		Prefix base = SparqlBuilder.prefix(iri("http://data.example/"));
		Variable size = SparqlBuilder.var("size"), asize = SparqlBuilder.var("asize"), x = SparqlBuilder.var("x");
		Expression<?> avgSize = Expressions.avg(size);

		query.prefix(base)
				.select(avgSize.as(asize))
				.where(x.has(base.iri("size"), size))
				.groupBy(x)
				.having(Expressions.gt(avgSize, 10));
		Assert.assertThat(query.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"PREFIX : <http://data.example/>\n"
						+ "SELECT (AVG(?size) AS ?asize)\n"
						+ "WHERE {\n"
						+ "  ?x :size ?size .\n"
						+ "}\n"
						+ "GROUP BY ?x\n"
						+ "HAVING(AVG(?size) > 10)"));
	}

	@Test
	public void example_11_4() {
		Prefix base = SparqlBuilder.prefix(iri("http://example.com/data/#"));
		Variable x = SparqlBuilder.var("x"), y = SparqlBuilder.var("y"), z = SparqlBuilder.var("z"),
				min = SparqlBuilder.var("min");
		Expression<?> twiceMin = Expressions.multiply(Expressions.min(y), Rdf.literalOf(2));

		query.prefix(base)
				.select(x, twiceMin.as(min))
				.where(x.has(base.iri("p"), y), x.has(base.iri("q"), z))
				.groupBy(x, Expressions.str(z));
		Assert.assertThat(query.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"PREFIX : <http://example.com/data/#>\n"
						+ "SELECT ?x ((MIN(?y) * 2) AS ?min)\n"
						+ "WHERE {\n"
						+ "  ?x :p ?y .\n"
						+ "  ?x :q ?z .\n"
						+ "} GROUP BY ?x STR(?z)"));
	}

	@Test
	public void example_11_5() {
		Prefix base = SparqlBuilder.prefix(iri("http://example.com/data/#"));
		Variable g = SparqlBuilder.var("g"), p = SparqlBuilder.var("p"), avg = SparqlBuilder.var("avg"),
				c = SparqlBuilder.var("c");
		Expression<?> midRange = Expressions
				.divide(Expressions.add(Expressions.min(p), Expressions.max(p)).parenthesize(), Rdf.literalOf(2));

		query.prefix(base)
				.select(g, Expressions.avg(p).as(avg), midRange.as(c))
				.where(g.has(base.iri("p"), p))
				.groupBy(g);
		Assert.assertThat(query.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"PREFIX : <http://example.com/data/#>\n"
						+ "SELECT ?g (AVG(?p) AS ?avg) (((MIN(?p) + MAX(?p)) / 2) AS ?c)\n"
						+ "WHERE {\n"
						+ "  ?g :p ?p .\n"
						+ "}\n"
						+ "GROUP BY ?g"));
	}
}
