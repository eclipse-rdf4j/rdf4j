package org.eclipse.rdf4j.spanqit.core;

import java.util.Collections;
import java.util.function.Function;

import org.eclipse.rdf4j.spanqit.graphpattern.TriplePattern;
import org.eclipse.rdf4j.spanqit.util.SpanqitUtils;

/**
 * Represents a collection of triple patterns
 */
public class TriplesTemplate extends StandardQueryElementCollection<TriplePattern> {
	private static final Function<String, String> WRAPPER = SpanqitUtils::getBracedString;
	TriplesTemplate(TriplePattern... triples) {
		super("\n");
		setWrapperMethod(WRAPPER);
		and(triples);
	}

	/**
	 * add triples to this template
	 * 
	 * @param triples the triples to add
	 * 
	 * @return this TriplesTemplate instance
	 */
	public TriplesTemplate and(TriplePattern... triples) {
		Collections.addAll(elements, triples);
		
		return this;
	}
}
