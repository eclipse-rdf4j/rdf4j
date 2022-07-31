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
import org.eclipse.rdf4j.model.base.CoreDatatype;

/**
 * Constants for RDF primitives and for the RDF namespace.
 *
 * @see <a href="http://www.w3.org/TR/REC-rdf-syntax/">RDF/XML Syntax Specification (Revised)</a>
 */
public class RDF {

	/** http://www.w3.org/1999/02/22-rdf-syntax-ns# */
	public static final String NAMESPACE = CoreDatatype.RDF.NAMESPACE;

	/**
	 * Recommended prefix for the RDF namespace: "rdf"
	 */
	public static final String PREFIX = "rdf";

	/**
	 * An immutable {@link Namespace} constant that represents the RDF namespace.
	 */
	public static final Namespace NS = Vocabularies.createNamespace(PREFIX, NAMESPACE);

	/** http://www.w3.org/1999/02/22-rdf-syntax-ns#type */
	public final static IRI TYPE = Vocabularies.createIRI(RDF.NAMESPACE, "type");

	/** http://www.w3.org/1999/02/22-rdf-syntax-ns#Property */
	public final static IRI PROPERTY = Vocabularies.createIRI(RDF.NAMESPACE, "Property");

	/** http://www.w3.org/1999/02/22-rdf-syntax-ns#XMLLiteral */
	public final static IRI XMLLITERAL = CoreDatatype.RDF.XMLLITERAL.getIri();

	/** http://www.w3.org/1999/02/22-rdf-syntax-ns#subject */
	public final static IRI SUBJECT = Vocabularies.createIRI(RDF.NAMESPACE, "subject");

	/** http://www.w3.org/1999/02/22-rdf-syntax-ns#predicate */
	public final static IRI PREDICATE = Vocabularies.createIRI(RDF.NAMESPACE, "predicate");

	/** http://www.w3.org/1999/02/22-rdf-syntax-ns#object */
	public final static IRI OBJECT = Vocabularies.createIRI(RDF.NAMESPACE, "object");

	/** http://www.w3.org/1999/02/22-rdf-syntax-ns#Statement */
	public final static IRI STATEMENT = Vocabularies.createIRI(RDF.NAMESPACE, "Statement");

	/** http://www.w3.org/1999/02/22-rdf-syntax-ns#Bag */
	public final static IRI BAG = Vocabularies.createIRI(RDF.NAMESPACE, "Bag");

	/** http://www.w3.org/1999/02/22-rdf-syntax-ns#Alt */
	public final static IRI ALT = Vocabularies.createIRI(RDF.NAMESPACE, "Alt");

	/** http://www.w3.org/1999/02/22-rdf-syntax-ns#Seq */
	public final static IRI SEQ = Vocabularies.createIRI(RDF.NAMESPACE, "Seq");

	/** http://www.w3.org/1999/02/22-rdf-syntax-ns#value */
	public final static IRI VALUE = Vocabularies.createIRI(RDF.NAMESPACE, "value");

	/** http://www.w3.org/1999/02/22-rdf-syntax-ns#li */
	public final static IRI LI = Vocabularies.createIRI(RDF.NAMESPACE, "li");

	/** http://www.w3.org/1999/02/22-rdf-syntax-ns#List */
	public final static IRI LIST = Vocabularies.createIRI(RDF.NAMESPACE, "List");

	/** http://www.w3.org/1999/02/22-rdf-syntax-ns#first */
	public final static IRI FIRST = Vocabularies.createIRI(RDF.NAMESPACE, "first");

	/** http://www.w3.org/1999/02/22-rdf-syntax-ns#rest */
	public final static IRI REST = Vocabularies.createIRI(RDF.NAMESPACE, "rest");

	/** http://www.w3.org/1999/02/22-rdf-syntax-ns#nil */
	public final static IRI NIL = Vocabularies.createIRI(RDF.NAMESPACE, "nil");

	/** http://www.w3.org/1999/02/22-rdf-syntax-ns#langString */
	public static final IRI LANGSTRING = CoreDatatype.RDF.LANGSTRING.getIri();

	/** http://www.w3.org/1999/02/22-rdf-syntax-ns#HTML */
	public static final IRI HTML = CoreDatatype.RDF.HTML.getIri();

}
