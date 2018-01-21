package org.eclipse.rdf4j.spanqit.examples.sparql11spec;

import static org.eclipse.rdf4j.spanqit.rdf.Rdf.iri;

import org.junit.Test;

import org.eclipse.rdf4j.spanqit.constraint.Expression;
import org.eclipse.rdf4j.spanqit.constraint.Expressions;
import org.eclipse.rdf4j.spanqit.core.Prefix;
import org.eclipse.rdf4j.spanqit.core.Spanqit;
import org.eclipse.rdf4j.spanqit.core.Variable;
import org.eclipse.rdf4j.spanqit.examples.BaseExamples;
import org.eclipse.rdf4j.spanqit.graphpattern.GraphPattern;
import org.eclipse.rdf4j.spanqit.graphpattern.GraphPatternNotTriple;
import org.eclipse.rdf4j.spanqit.graphpattern.GraphPatterns;
import org.eclipse.rdf4j.spanqit.graphpattern.TriplePattern;
import org.eclipse.rdf4j.spanqit.rdf.Rdf;

public class Section3 extends BaseExamples {
	@Test
	public void example_3_1() {
		Prefix dc = Spanqit.prefix("dc", iri(DC_NS));
		
		Variable x = query.var(), title = Spanqit.var("title");
		TriplePattern xTitle = GraphPatterns.tp(x, dc.iri("title"), title);
		
		Expression<?> regex = Expressions.regex(title, Rdf.literalOf("^SPARQL"));
		GraphPatternNotTriple where = GraphPatterns.and(xTitle).filter(regex);

		query.prefix(dc).select(title).where(where);
		p();

		where.filter(Expressions.regex(title, "web", "i"));
		p();
	}

	@Test
	public void example_3_2() {
		Prefix dc = Spanqit.prefix("dc", iri(DC_NS)),
			   ns = Spanqit.prefix("ns", iri(EXAMPLE_COM_NS));
		
		Variable title = Spanqit.var("title"), price = Spanqit
				.var("price");
		Variable x = query.var();
		Expression<?> priceConstraint = Expressions.lt(price, 30.5);

		GraphPattern where = GraphPatterns.and(
				x.has(ns.iri("price"), price),
				x.has(dc.iri("title"), title)).filter(priceConstraint);

		query.prefix(dc, ns).select(title, price).where(where);
		p();
	}
}