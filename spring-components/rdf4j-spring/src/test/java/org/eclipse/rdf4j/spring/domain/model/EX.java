package org.eclipse.rdf4j.spring.domain.model;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.MultiIRI;

public class EX {
	private static final String base = "http://example.org/";
	public static final MultiIRI Artist = new MultiIRI(base, "Artist");
	public static final MultiIRI Gallery = new MultiIRI(base, "Gallery");
	public static final MultiIRI Painting = new MultiIRI(base, "Painting");

	public static IRI of(String localName) {
		return new MultiIRI(base, localName);
	}
}
