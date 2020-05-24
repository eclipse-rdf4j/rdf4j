/*******************************************************************************
 * Copyright (c) ${year} Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sparqlbuilder.core;

import org.eclipse.rdf4j.model.vocabulary.DC;
import org.eclipse.rdf4j.sparqlbuilder.constraint.Expressions;
import org.eclipse.rdf4j.sparqlbuilder.core.query.SelectQuery;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatternNotTriples;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatterns;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

import static org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf.iri;


public class SparqlBuilderTest {

    protected SelectQuery query;
    protected static final String EXAMPLE_ORG_NS = "https://example.org/ns#";
    protected static final String DC_NS = DC.NAMESPACE;

    @Test
    public void testLogicalOperator1() {
        Prefix dc = SparqlBuilder.prefix("dc", iri(DC_NS)), ns = SparqlBuilder.prefix("ns", iri(EXAMPLE_ORG_NS));
        Variable title = SparqlBuilder.var("title"), price = SparqlBuilder.var("price"), x = SparqlBuilder.var("x");

        GraphPatternNotTriples pricePattern = GraphPatterns.and(x.has(ns.iri("price"), price))
                .filter(Expressions.or(Expressions.lt(price, 20),
                        Expressions.and(Expressions.lt(price, 50), Expressions.gt(price, 30))))
                .optional();

        query.prefix(dc, ns).select(title, price).where(x.has(dc.iri("title"), title), pricePattern);
        Assert.assertThat(query.getQueryString(), CoreMatchers.containsString("( ?price < 50 && ?price > 30 )"));
    }

    @Test
    public void testLogicalOperator2() {
        Prefix dc = SparqlBuilder.prefix("dc", iri(DC_NS)), ns = SparqlBuilder.prefix("ns", iri(EXAMPLE_ORG_NS));
        Variable title = SparqlBuilder.var("title"), price = SparqlBuilder.var("price"), x = SparqlBuilder.var("x");

        GraphPatternNotTriples pricePattern = GraphPatterns.and(x.has(ns.iri("price"), price))
                .filter(Expressions.or(Expressions.lt(price, 20),
                        Expressions.and(Expressions.gt(price, 50),
                                Expressions.or(Expressions.gt(price, 60), Expressions.lt(price, 70)))))
                .optional();

        query.prefix(dc, ns).select(title, price).where(x.has(dc.iri("title"), title), pricePattern);
        Assert.assertThat(query.getQueryString(), CoreMatchers.containsString("( ?price < 20 || ( ?price > 50 &&" +
                " ( ?price > 60 || ?price < 70 ) ) )"));
    }

    @Test
    public void testArithmeticOperator1() {
        Prefix dc = SparqlBuilder.prefix("dc", iri(DC_NS)), ns = SparqlBuilder.prefix("ns", iri(EXAMPLE_ORG_NS));
        Variable title = SparqlBuilder.var("title"), price = SparqlBuilder.var("price"), x = SparqlBuilder.var("x");

        GraphPatternNotTriples pricePattern = GraphPatterns.and(x.has(ns.iri("price"), price))
                .filter(Expressions.or(Expressions.lt(price, Expressions.subtract(Rdf.literalOf(20),
                        Expressions.multiply(Rdf.literalOf(2), Rdf.literalOf(5)))),
                        Expressions.lt(price, 50)))
                .optional();

        query.prefix(dc, ns).select(title, price).where(x.has(dc.iri("title"), title), pricePattern);
        Assert.assertThat(query.getQueryString(), CoreMatchers.containsString("( 20 - ( 2 * 5 ) )"));
    }

    @Test
    public void testArithmeticOperator2() {
        Prefix dc = SparqlBuilder.prefix("dc", iri(DC_NS)), ns = SparqlBuilder.prefix("ns", iri(EXAMPLE_ORG_NS));
        Variable title = SparqlBuilder.var("title"), price = SparqlBuilder.var("price"), x = SparqlBuilder.var("x");

        GraphPatternNotTriples pricePattern = GraphPatterns.and(x.has(ns.iri("price"), price))
                .filter(Expressions.or(Expressions.lt(price, Expressions.add(Rdf.literalOf(20),
                        Expressions.divide(Rdf.literalOf(10), Rdf.literalOf(5)))),
                        Expressions.lt(price, 50)))
                .optional();

        query.prefix(dc, ns).select(title, price).where(x.has(dc.iri("title"), title), pricePattern);
        Assert.assertThat(query.getQueryString(), CoreMatchers.containsString("( 20 + ( 10 / 5 ) )"));
    }

    @Test
    public void testArithmeticOperator3() {
        Prefix dc = SparqlBuilder.prefix("dc", iri(DC_NS)), ns = SparqlBuilder.prefix("ns", iri(EXAMPLE_ORG_NS));
        Variable title = SparqlBuilder.var("title"), price = SparqlBuilder.var("price"), x = SparqlBuilder.var("x");

        GraphPatternNotTriples pricePattern = GraphPatterns.and(x.has(ns.iri("price"), price))
                .filter(Expressions.lt(price, Expressions.multiply(Expressions.subtract(Rdf.literalOf(20),
                        Rdf.literalOf(2)), Rdf.literalOf(5))))
                .optional();

        query.prefix(dc, ns).select(title, price).where(x.has(dc.iri("title"), title), pricePattern);
        Assert.assertThat(query.getQueryString(), CoreMatchers.containsString("( ( 20 - 2 ) * 5 ) )"));
    }

}
