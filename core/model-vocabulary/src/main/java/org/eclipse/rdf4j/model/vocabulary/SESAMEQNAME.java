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
package org.eclipse.rdf4j.model.vocabulary;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;

/**
 * Defines constants for the Sesame QName schema namespace.
 *
 * @author Peter Ansell
 */
public class SESAMEQNAME {

	/**
	 * The Sesame QName Schema namespace ( <var>http://www.openrdf.org/schema/qname#</var>).
	 */
	public static final String NAMESPACE = "http://www.openrdf.org/schema/qname#";

	/**
	 * Recommended prefix for the Sesame QName Schema namespace: "q"
	 */
	public static final String PREFIX = "q";

	/**
	 * An immutable {@link Namespace} constant that represents the Sesame QName namespace.
	 */
	public static final Namespace NS = Vocabularies.createNamespace(PREFIX, NAMESPACE);

	/** <var>http://www.openrdf.org/schema/qname#qname</var> */
	public final static IRI QNAME;

	static {
		QNAME = Vocabularies.createIRI(SESAMEQNAME.NAMESPACE, "qname");
	}
}
