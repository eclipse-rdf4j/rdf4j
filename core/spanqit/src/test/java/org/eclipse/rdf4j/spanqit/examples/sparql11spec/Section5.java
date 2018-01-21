package org.eclipse.rdf4j.spanqit.examples.sparql11spec;

import org.junit.Test;

import org.eclipse.rdf4j.spanqit.constraint.Expressions;
import org.eclipse.rdf4j.spanqit.core.Prefix;
import org.eclipse.rdf4j.spanqit.core.QueryPattern;
import org.eclipse.rdf4j.spanqit.core.Spanqit;
import org.eclipse.rdf4j.spanqit.core.Variable;
import org.eclipse.rdf4j.spanqit.examples.BaseExamples;
import org.eclipse.rdf4j.spanqit.graphpattern.GraphPattern;
import org.eclipse.rdf4j.spanqit.graphpattern.GraphPatterns;

import static org.eclipse.rdf4j.spanqit.rdf.Rdf.iri;

public class Section5 extends BaseExamples {
	@Test
	public void example_5_2() {
		Prefix foaf = Spanqit.prefix("foaf", iri(FOAF_NS));
		Variable name = Spanqit.var("name"), mbox = Spanqit.var("mbox");
		Variable x = query.var();

		query.prefix(foaf)
				.select(name, mbox)
				.where(x.has(foaf.iri("name"), name),
						x.has(foaf.iri("mbox"), mbox));
		p();

		GraphPattern namePattern = GraphPatterns.and(x.has(
				foaf.iri("name"), name));
		GraphPattern mboxPattern = GraphPatterns.and(x.has(
				foaf.iri("mbox"), mbox));
		QueryPattern where = Spanqit.where(GraphPatterns.and(namePattern,
				mboxPattern));
		query.where(where);
		p();
	}

	@Test
	public void example_5_2_1() {
		p(GraphPatterns.and());

		query.select(query.var());
		p();
	}

	@Test
	public void example_5_2_3() {
		Prefix foaf = Spanqit.prefix("foaf", iri(FOAF_NS));
		Variable x = Spanqit.var("x"), name = Spanqit.var("name"), mbox = Spanqit
				.var("mbox");

		p(GraphPatterns.and(x.has(foaf.iri("name"), name),
				x.has(foaf.iri("mbox"), mbox)));
		p("");
		p(GraphPatterns.and(x.has(foaf.iri("name"), name),
				x.has(foaf.iri("mbox"), mbox)).filter(
				Expressions.regex(name, "Smith")));
		p("");
		p(GraphPatterns.and(x.has(foaf.iri("name"), name),
				GraphPatterns.and(), x.has(foaf.iri("mbox"), mbox)));
	}
}