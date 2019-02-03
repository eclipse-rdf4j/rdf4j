/*******************************************************************************
Copyright (c) 2018 Eclipse RDF4J contributors.
All rights reserved. This program and the accompanying materials
are made available under the terms of the Eclipse Distribution License v1.0
which accompanies this distribution, and is available at
http://www.eclipse.org/org/documents/edl-v10.php.
*******************************************************************************/

package org.eclipse.rdf4j.sparqlbuilder.examples.sparql11spec;

import org.junit.Test;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.vocabulary.DC;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.sparqlbuilder.constraint.Expressions;
import org.eclipse.rdf4j.sparqlbuilder.core.Assignment;
import org.eclipse.rdf4j.sparqlbuilder.core.Prefix;
import org.eclipse.rdf4j.sparqlbuilder.core.PrefixDeclarations;
import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.core.query.ConstructQuery;
import org.eclipse.rdf4j.sparqlbuilder.core.query.Queries;
import org.eclipse.rdf4j.sparqlbuilder.core.query.SelectQuery;
import org.eclipse.rdf4j.sparqlbuilder.examples.BaseExamples;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatterns;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.TriplePattern;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf;

public class Section2Test extends BaseExamples {
	@Test
	public void example_2_1() {
		Variable title = SparqlBuilder.var("title");

		TriplePattern book1_has_title = GraphPatterns.tp(Rdf.iri(EXAMPLE_ORG_BOOK_NS, "book1"), Rdf.iri(DC_NS, "title"), title);

		query.select(title).where(book1_has_title);

		p();
	}
	
	@Test
	public void example_2_1_model() {
		Variable title = SparqlBuilder.var("title");
		String ex = EXAMPLE_ORG_BOOK_NS;
		IRI book1 = VF.createIRI(ex, "book1");
		
		TriplePattern book1_has_title = GraphPatterns.tp(book1, DC.TITLE, title);

		query.select(title).where(book1_has_title);

		p();
	}

	@Test
	public void example_2_2() {
		Prefix foaf = SparqlBuilder.prefix("foaf", Rdf.iri(FOAF_NS));

		/**
		 * As a shortcut, Query objects can create variables that will be unique to the
		 * query instance.
		 */
		Variable name = query.var(), mbox = query.var(), x = query.var();

		TriplePattern x_hasFoafName_name = GraphPatterns.tp(x, foaf.iri("name"), name);
		TriplePattern x_hasFoafMbox_mbox = GraphPatterns.tp(x, foaf.iri("mbox"), mbox);

		query.prefix(foaf).select(name, mbox).where(x_hasFoafName_name, x_hasFoafMbox_mbox);

		p();
	}

	@Test
	public void example_2_2_model() {
		Prefix foaf = SparqlBuilder.prefix(FOAF.NS);

		/**
		 * As a shortcut, Query objects can create variables that will be unique to the
		 * query instance.
		 */
		Variable name = query.var(), mbox = query.var(), x = query.var();

		TriplePattern x_hasFoafName_name = GraphPatterns.tp(x, FOAF.NAME, name);
		TriplePattern x_hasFoafMbox_mbox = GraphPatterns.tp(x, FOAF.MBOX, mbox);

		query.prefix(foaf).select(name, mbox).where(x_hasFoafName_name, x_hasFoafMbox_mbox);

		p();
	}
	
	@Test
	public void example_2_3_1() {
		Variable v = query.var(), p = query.var();

		TriplePattern v_hasP_cat = GraphPatterns.tp(v, p, Rdf.literalOf("cat"));

		query.select(v).where(v_hasP_cat);
		p();


		TriplePattern v_hasP_cat_en = GraphPatterns.tp(v, p, Rdf.literalOfLanguage("cat", "en"));
		SelectQuery queryWithLangTag = Queries.SELECT(v).where(v_hasP_cat_en);
		p(queryWithLangTag);
	}

	@Test
	public void example_2_3_2() {
		Variable v = query.var(), p = query.var();

		TriplePattern v_hasP_42 = GraphPatterns.tp(v, p, Rdf.literalOf(42));

		query.select(v).where(v_hasP_42);
		p();
	}

	@Test
	public void example_2_3_3() {
		String datatype = "specialDatatype";
		Variable v = query.var(), p = query.var();
		TriplePattern v_hasP_abc_dt = GraphPatterns.tp(v, p, Rdf.literalOfType("abc", Rdf.iri(EXAMPLE_DATATYPE_NS, datatype)));

		query.select(v).where(v_hasP_abc_dt);
		p();
	}
	
	@Test
	public void example_2_3_3_model() {
		String datatype = "specialDatatype";
		Variable v = query.var(), p = query.var();
		
		Literal lit = VF.createLiteral("abc", VF.createIRI(EXAMPLE_DATATYPE_NS, datatype));
		
		TriplePattern v_hasP_abc_dt = GraphPatterns.tp(v, p, lit);

		query.select(v).where(v_hasP_abc_dt);
		p();
		
	}
	
	@Test
	public void example_2_4() {
		Prefix foaf = SparqlBuilder.prefix("foaf", Rdf.iri(FOAF_NS));

		Variable x = query.var(), name = query.var();
		query.prefix(foaf).select(x, name).where(x.has(foaf.iri("name"), name));
		p();
	}

	@Test
	public void example_2_5() {
		Prefix foaf = SparqlBuilder.prefix("foaf", Rdf.iri(FOAF_NS));
		Variable G = SparqlBuilder.var("G"),
				P = SparqlBuilder.var("P"), 
				S = SparqlBuilder.var("S"),
				name = SparqlBuilder.var("name");

		Assignment concatAsName = SparqlBuilder.as(Expressions.concat(G, Rdf.literalOf(" "), S), name);

		query.prefix(foaf).select(concatAsName).where(
				GraphPatterns.tp(P, foaf.iri("givenName"), G).andHas(foaf.iri("surname"), S));
		p();

		// TODO add BIND() capability in graph patterns (also show example of
		// saving PrefixDeclarations object and using it in both queries)
		p("Missing BIND capability right now");
	}

	@Test
	public void example_2_6() {
		Prefix foaf = SparqlBuilder.prefix("foaf", Rdf.iri(FOAF_NS)),
				org = SparqlBuilder.prefix("org", Rdf.iri(EXAMPLE_COM_NS));
		PrefixDeclarations prefixes = SparqlBuilder.prefixes(foaf, org);

		ConstructQuery graphQuery = Queries.CONSTRUCT();
		Variable x = graphQuery.var(), name = SparqlBuilder.var("name");

		TriplePattern foafName = GraphPatterns.tp(x, foaf.iri("name"), name);
		TriplePattern orgName = GraphPatterns.tp(x, org.iri("employeeName"), name);

		graphQuery.prefix(prefixes).construct(foafName).where(orgName);
		p(graphQuery);
	}
}