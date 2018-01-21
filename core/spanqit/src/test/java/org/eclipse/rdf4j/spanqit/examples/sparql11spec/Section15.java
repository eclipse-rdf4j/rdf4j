package org.eclipse.rdf4j.spanqit.examples.sparql11spec;

import org.eclipse.rdf4j.spanqit.graphpattern.TriplePattern;
import org.junit.Test;

import org.eclipse.rdf4j.spanqit.core.OrderBy;
import org.eclipse.rdf4j.spanqit.core.OrderCondition;
import org.eclipse.rdf4j.spanqit.core.Prefix;
import org.eclipse.rdf4j.spanqit.core.PrefixDeclarations;
import org.eclipse.rdf4j.spanqit.core.Spanqit;
import org.eclipse.rdf4j.spanqit.core.Variable;
import org.eclipse.rdf4j.spanqit.examples.BaseExamples;

import static org.eclipse.rdf4j.spanqit.rdf.Rdf.iri;

public class Section15 extends BaseExamples {
	@Test
	public void example_15_1() {
		Prefix foaf = Spanqit.prefix("foaf", iri(FOAF_NS));
		Variable name = query.var(), x = query.var();

        TriplePattern employeePattern = x.has(foaf.iri("name"), name);
		query.prefix(foaf).select(name).where(employeePattern).orderBy(name);
		p();
		
		Prefix base = Spanqit.prefix(iri("http://example.org/ns#"));
		PrefixDeclarations prefixes = Spanqit.prefixes(foaf, base);
		Variable emp = query.var();
		
		OrderCondition empDesc = Spanqit.desc(emp);
		
		// calling prefix() with a PrefixDeclarations instance (rather than
		// Prefix objects) replaces (rather than augments) the query's
		// prefixes
		query.prefix(prefixes);

		// we can still modify graph patterns
        employeePattern.andHas(base.iri("empId"), emp);

        // similarly, calling orderBy() with an OrderBy instance (rather
        // than Orderable instances) replaces (rather than augments)
        // the query's order conditions
        query.orderBy(Spanqit.orderBy(empDesc));
		p();
		
		OrderBy order = Spanqit.orderBy(name, empDesc);
		query.orderBy(order);
		p();
	}
	
	@Test
	public void example_15_3_1() {
		Prefix foaf = Spanqit.prefix("foaf", iri(FOAF_NS));
		Variable name = query.var(), x = query.var();
		
		query.prefix(foaf).select(name).distinct().where(x.has(foaf.iri("name"), name));
		p();
	}
	
	@Test
	public void example_15_3_2() {
		p("REDUCED not yet implemented");
	}
	
	@Test
	public void example_15_4() {
		Prefix foaf = Spanqit.prefix("foaf", iri(FOAF_NS));
		Variable name = query.var(), x = query.var();
		
		query.prefix(foaf).select(name).where(x.has(foaf.iri("name"), name))
			.orderBy(name)
			.limit(5)
			.offset(10);
		p();
	}
	
	@Test
	public void example_15_5() {
		Prefix foaf = Spanqit.prefix("foaf", iri(FOAF_NS));
		Variable name = query.var(), x = query.var();
		
		query.prefix(foaf).select(name).where(x.has(foaf.iri("name"), name))
			.limit(20);
		p();
	}
}