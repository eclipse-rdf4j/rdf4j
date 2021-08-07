/*
 * ******************************************************************************
 *  * Copyright (c) 2021 Eclipse RDF4J contributors.
 *  * All rights reserved. This program and the accompanying materials
 *  * are made available under the terms of the Eclipse Distribution License v1.0
 *  * which accompanies this distribution, and is available at
 *  * http://www.eclipse.org/org/documents/edl-v10.php.
 *  ******************************************************************************
 */

package org.eclipse.rdf4j.model.impl;

import static org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf.iri;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Iri;

/**
 * @since 4.0.0
 * @author Gabriel Pickl
 */
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
