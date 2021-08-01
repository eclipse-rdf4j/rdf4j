package org.eclipse.rdf4j.model.impl;

import static org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf.iri;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Iri;

public class MultiIRI extends SimpleIRI implements Iri {

	public MultiIRI(String baseName, String localName) {
		this(SimpleValueFactory.getInstance().createIRI(baseName, localName));
	}

	public MultiIRI(String name) {
		this(SimpleValueFactory.getInstance().createIRI(name));
	}

	public MultiIRI(IRI iri) {
		super(iri.stringValue());
	}

	public Iri getIri() {
		return iri(getNamespace(), getLocalName());
	}

	@Override
	public String getQueryString() {
		return getIri().getQueryString();
	}
}
