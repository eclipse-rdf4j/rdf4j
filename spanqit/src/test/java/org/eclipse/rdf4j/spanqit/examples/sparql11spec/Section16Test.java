/*******************************************************************************
Copyright (c) 2018 Eclipse RDF4J contributors.
All rights reserved. This program and the accompanying materials
are made available under the terms of the Eclipse Distribution License v1.0
which accompanies this distribution, and is available at
http://www.eclipse.org/org/documents/edl-v10.php.
*******************************************************************************/

package org.eclipse.rdf4j.spanqit.examples.sparql11spec;

import static org.eclipse.rdf4j.spanqit.rdf.Rdf.iri;

import org.eclipse.rdf4j.spanqit.core.*;
import org.eclipse.rdf4j.spanqit.core.query.ConstructQuery;
import org.eclipse.rdf4j.spanqit.core.query.Queries;
import org.eclipse.rdf4j.spanqit.core.query.Query;
import org.eclipse.rdf4j.spanqit.graphpattern.GraphPattern;
import org.eclipse.rdf4j.spanqit.graphpattern.GraphPatterns;
import org.eclipse.rdf4j.spanqit.rdf.Iri;
import org.eclipse.rdf4j.spanqit.rdf.RdfBlankNode;
import org.eclipse.rdf4j.spanqit.rdf.RdfLiteral;
import org.junit.Test;

import org.eclipse.rdf4j.spanqit.constraint.Expression;
import org.eclipse.rdf4j.spanqit.constraint.Operand;
import org.eclipse.rdf4j.spanqit.constraint.Expressions;
import org.eclipse.rdf4j.spanqit.examples.BaseExamples;
import org.eclipse.rdf4j.spanqit.rdf.Rdf;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Section16Test extends BaseExamples {
	@Test
	public void example_16_1_2() {
		Prefix dc = Spanqit.prefix("dc", iri(DC_NS));
		Prefix ns = Spanqit.prefix("ns", iri(EXAMPLE_ORG_NS));
		Variable title = query.var(), p = query.var(), discount = query.var(), price = query
				.var(), x = query.var();
		Operand one = Rdf.literalOf(1);

		Assignment discountedPrice = Expressions.multiply(p,
				Expressions.subtract(one, discount).parenthesize()).as(price);

		query.prefix(dc, ns)
				.select(title, discountedPrice)
				.where(x.has(ns.iri("price"), p),
						x.has(dc.iri("title"), title),
						x.has(ns.iri("discount"), discount));
		p();

		Variable fullPrice = query.var(), customerPrice = query.var();
		Expression<?> cPrice = Expressions.multiply(fullPrice,
				Expressions.subtract(one, discount).parenthesize());
		Projection newProjection = Spanqit.select(title, p.as(fullPrice),
				cPrice.as(customerPrice));

		// similar to other elements, calling select() with a Projection instance
		// (rather than Projectable instances) replaces (rather than augments)
		// the query's projections
		query.select(newProjection);
		p();
	}

	@Test
    public void example_16_2() {
	    Prefix foaf = Spanqit.prefix("foaf", iri(FOAF_NS)),
                vcard = Spanqit.prefix("vcard", iri("http://www.w3.org/2001/vcard-rdf/3.0#"));
        Iri aliceIri = Rdf.iri("http://example.org/person#", "Alice");
        Variable name = Spanqit.var("name"), x = Spanqit.var("x");
        p(Queries.CONSTRUCT(aliceIri.has(vcard.iri("FN"), name)).where(x.has(foaf.iri("name"), name)).prefix(foaf, vcard));
    }

    @Test
    public void example_16_2_1() {
        Prefix foaf = Spanqit.prefix("foaf", iri(FOAF_NS)),
                vcard = Spanqit.prefix("vcard", iri("http://www.w3.org/2001/vcard-rdf/3.0#"));

        ConstructQuery cQuery = Queries.CONSTRUCT();
        Variable x = cQuery.var(), gname = cQuery.var(), fname = cQuery.var();
        RdfBlankNode v = cQuery.bNode();
        GraphTemplate template = Spanqit.construct(
                x.has(vcard.iri("N"), v),
                v.has(vcard.iri("givenName"), gname),
                v.has(vcard.iri("familyName"), fname));

        cQuery.prefix(foaf, vcard).construct(template).where(
                x.has(foaf.iri("firstName"), gname).union(x.has(foaf.iri("givenname"), gname)),
                x.has(foaf.iri("surname"), fname).union(x.has(foaf.iri("family_name"), fname)));

        p(cQuery);
    }

    @Test
    public void example_16_2_2() {
	    Prefix dc = Spanqit.prefix("dc", iri("http://purl.org/dc/elements/1.1/")),
                app = Spanqit.prefix("app", iri("http://example.org/ns#")),
                xsd = Spanqit.prefix("xsd", iri("http://www.w3.org/2001/XMLSchema#"));

	    Map<String, Variable> vars = Arrays.stream("s,p,o,g,date".split(","))
                .collect(Collectors.toMap(Function.identity(), Spanqit::var));
	    Variable s = vars.get("s"), p = vars.get("p"), o = vars.get("o"), g = vars.get("g"), date = vars.get("date");

        QueryPattern where = Spanqit.where(GraphPatterns.and(
                GraphPatterns.and(s.has(p, o)).from(g),
                g.has(dc.iri("publisher"), iri("http://www.w3.org/")),
                g.has(dc.iri("date"), date))
            .filter(Expressions.gt(
                    Expressions.custom(app.iri("customDate"), date),
                    Rdf.literalOfType("2005-02-28T00:00:00Z", xsd.iri("dateTime")))));

	    ConstructQuery query = Queries.CONSTRUCT(s.has(p, o)).where(where).prefix(dc, app, xsd);

	    p(query);
    }

    @Test
    public void example_16_2_3() {
        Prefix foaf = Spanqit.prefix("foaf", iri("http://xmlns.com/foaf/0.1/")),
                site = Spanqit.prefix("site", iri("http://example.org/stats#"));
        Variable name = Spanqit.var("name"), hits = Spanqit.var("hits");
        RdfBlankNode subject = Rdf.bNode();

        p(Queries.CONSTRUCT(subject.has(foaf.iri("name"), name))
                .where(subject.has(foaf.iri("name"), name).andHas(site.iri("hits"), hits))
                .orderBy(hits.desc())
                .limit(2));
    }

    @Test
    public void example_16_2_4() {
        Prefix foaf = Spanqit.prefix("foaf", iri("http://xmlns.com/foaf/0.1/"));
        Variable x= Spanqit.var("x"), name = Spanqit.var("name");

        p(Queries.CONSTRUCT().where(x.has(foaf.iri("name"), name)).prefix(foaf));
    }
}