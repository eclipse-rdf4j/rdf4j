/*
 * ******************************************************************************
 *  * Copyright (c) 2021 Eclipse RDF4J contributors.
 *  * All rights reserved. This program and the accompanying materials
 *  * are made available under the terms of the Eclipse Distribution License v1.0
 *  * which accompanies this distribution, and is available at
 *  * http://www.eclipse.org/org/documents/edl-v10.php.
 *  ******************************************************************************
 */

package org.eclipse.rdf4j.sparqlbuilder.core;

import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.impl.MultiIRI;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf;

public class ExtendedPrefix extends org.eclipse.rdf4j.sparqlbuilder.core.Prefix {
	private final String namespace;

	public ExtendedPrefix(Namespace namespace) {
		this(namespace.getPrefix(), namespace.getName());
	}

	public ExtendedPrefix(String alias, String namespace) {
		super(alias, Rdf.iri(namespace));
		this.namespace = namespace;
	}

	protected MultiIRI modelIri(String localName) {
		return new MultiIRI(namespace, localName);
	}
}
