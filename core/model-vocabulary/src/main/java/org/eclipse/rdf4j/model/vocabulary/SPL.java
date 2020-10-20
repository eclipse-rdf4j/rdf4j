/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.model.vocabulary;

import static org.eclipse.rdf4j.model.base.AbstractIRI.createIRI;

import org.eclipse.rdf4j.model.IRI;

/**
 * SPIN Standard Module library.
 */
public final class SPL {

	private SPL() {
	}

	/**
	 * http://spinrdf.org/spl
	 */
	public static final String NAMESPACE = "http://spinrdf.org/spl#";

	public static final String PREFIX = "spl";

	/**
	 * http://spinrdf.org/spl#Argument Provides metadata about an argument of a SPIN Function or Template.
	 */
	public static final IRI ARGUMENT_TEMPLATE;

	/**
	 * http://spinrdf.org/spl#predicate
	 */
	public static final IRI PREDICATE_PROPERTY;

	/**
	 * http://spinrdf.org/spl#valueType
	 */
	public static final IRI VALUE_TYPE_PROPERTY;

	/**
	 * http://spinrdf.org/spl#optional
	 */
	public static final IRI OPTIONAL_PROPERTY;

	/**
	 * http://spinrdf.org/spl#defaultValue
	 */
	public static final IRI DEFAULT_VALUE_PROPERTY;

	/**
	 * http://spinrdf.org/spl#object
	 */
	public static final IRI OBJECT_FUNCTION;

	static {

		ARGUMENT_TEMPLATE = createIRI(NAMESPACE, "Argument");
		PREDICATE_PROPERTY = createIRI(NAMESPACE, "predicate");
		VALUE_TYPE_PROPERTY = createIRI(NAMESPACE, "valueType");
		OPTIONAL_PROPERTY = createIRI(NAMESPACE, "optional");
		DEFAULT_VALUE_PROPERTY = createIRI(NAMESPACE, "defaultValue");

		OBJECT_FUNCTION = createIRI(NAMESPACE, "object");
	}
}
