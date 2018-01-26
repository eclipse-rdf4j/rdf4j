package org.eclipse.rdf4j.spanqit.examples.sparql11spec;

import org.junit.Test;

import org.eclipse.rdf4j.spanqit.constraint.Expressions;
import org.eclipse.rdf4j.spanqit.core.Prefix;
import org.eclipse.rdf4j.spanqit.core.Spanqit;
import org.eclipse.rdf4j.spanqit.core.Variable;
import org.eclipse.rdf4j.spanqit.examples.BaseExamples;
import org.eclipse.rdf4j.spanqit.graphpattern.GraphPatterns;
import org.eclipse.rdf4j.spanqit.graphpattern.SubSelect;

import static org.eclipse.rdf4j.spanqit.rdf.Rdf.iri;

public class Section12Test extends BaseExamples {
	@Test
	public void example_12() {
		Prefix base = Spanqit.prefix(iri("http://people.example/"));
		
		// using this method of variable creation, as ?y and ?minName will be 
		// used in both the outer and inner queries
		Variable y = Spanqit.var("y"), minName = Spanqit.var("minName");
	
		SubSelect sub = GraphPatterns.select();
		Variable name = sub.var();
		sub.select(y, Expressions.min(name).as(minName)).where(y.has(base.iri("name"), name)).groupBy(y);
		
		query.prefix(base, base) // Spanqit even fixes typos for you ;)
				.select(y, minName).where(base.iri("alice").has(base.iri("knows"), y), sub);
		p();
	}
}