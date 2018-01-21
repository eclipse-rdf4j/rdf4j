package org.eclipse.rdf4j.spanqit.examples.sparql11spec;

import org.junit.Test;

import org.eclipse.rdf4j.spanqit.core.Prefix;
import org.eclipse.rdf4j.spanqit.core.Spanqit;
import org.eclipse.rdf4j.spanqit.core.Variable;
import org.eclipse.rdf4j.spanqit.examples.BaseExamples;
import org.eclipse.rdf4j.spanqit.graphpattern.GraphPattern;
import org.eclipse.rdf4j.spanqit.graphpattern.GraphPatterns;
import org.eclipse.rdf4j.spanqit.rdf.Iri;

import static org.eclipse.rdf4j.spanqit.rdf.Rdf.iri;

public class Section7 extends BaseExamples {
	@Test
	public void example_7() {
		String DC_10_NS = "http://purl.org/dc/elements/1.0/";
		Prefix dc10 = Spanqit.prefix("dc10", iri(DC_10_NS)),
			   dc11 = Spanqit.prefix("dc11", iri(DC_NS));
		Variable title = Spanqit.var("title"), book = Spanqit
				.var("book");
		
		Iri dc10TitleIri = dc10.iri("title");
		Iri dc11TitleIri = dc11.iri("title");

		GraphPattern titlePattern = GraphPatterns.union(
				book.has(dc10TitleIri, title), book.has(dc11TitleIri, title));

		query.prefix(dc10).prefix(dc11).select(title).where(titlePattern);
		p();

		resetQuery();
		Variable x = query.var(), y = query.var(), author = Spanqit
				.var("author");
		GraphPattern dc10Title = book.has(dc10TitleIri, x),
				dc11Title = book.has(dc11TitleIri, y);
		query.prefix(dc10, dc11).select(x, y)
				.where(GraphPatterns.union(dc10Title, dc11Title));
		p();

		resetQuery();
		GraphPattern dc10Author = book.has(dc10.iri("creator"), author),
				dc11Author = book.has(dc11.iri("creator"), author);
		dc10Title = book.has(dc10TitleIri, title);
		dc11Title = book.has(dc11TitleIri, title);
		query.prefix(dc10, dc11)
				.select(title, author)
				.where(GraphPatterns.and(dc10Title, dc10Author).union(
						GraphPatterns.and(dc11Title, dc11Author)));
		p();
	}
}