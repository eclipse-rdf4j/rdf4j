/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.rio.rdfxml;

/**
 * An XML attribute.
 */
class Att {

	/*-----------*
	 * Variables *
	 *-----------*/

	private final String namespace;

	private final String localName;

	private final String qName;

	private final String value;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public Att(String namespace, String localName, String qName, String value) {
		this.namespace = namespace;
		this.localName = localName;
		this.qName = qName;
		this.value = value;
	}

	/*---------*
	 * Methods *
	 *---------*/

	public String getNamespace() {
		return namespace;
	}

	public String getLocalName() {
		return localName;
	}

	public String getURI() {
		return namespace + localName;
	}

	public String getQName() {
		return qName;
	}

	public String getValue() {
		return value;
	}
}
