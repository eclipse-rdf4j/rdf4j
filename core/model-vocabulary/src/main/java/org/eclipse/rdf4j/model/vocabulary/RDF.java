/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.model.vocabulary;

import static org.eclipse.rdf4j.model.base.AbstractIRI.createIRI;
import static org.eclipse.rdf4j.model.base.AbstractNamespace.createNamespace;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;

/**
 * Constants for RDF primitives and for the RDF namespace.
 *
 * @see <a href="http://www.w3.org/TR/REC-rdf-syntax/">RDF/XML Syntax Specification (Revised)</a>
 */
public class RDF {

	/** http://www.w3.org/1999/02/22-rdf-syntax-ns# */
	public static final String NAMESPACE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";

	/**
	 * Recommended prefix for the RDF namespace: "rdf"
	 */
	public static final String PREFIX = "rdf";

	/**
	 * An immutable {@link Namespace} constant that represents the RDF namespace.
	 */
	public static final Namespace NS = createNamespace(PREFIX, NAMESPACE);

	/** http://www.w3.org/1999/02/22-rdf-syntax-ns#type */
	public final static IRI TYPE;

	/** http://www.w3.org/1999/02/22-rdf-syntax-ns#Property */
	public final static IRI PROPERTY;

	/** http://www.w3.org/1999/02/22-rdf-syntax-ns#XMLLiteral */
	public final static IRI XMLLITERAL;

	/** http://www.w3.org/1999/02/22-rdf-syntax-ns#subject */
	public final static IRI SUBJECT;

	/** http://www.w3.org/1999/02/22-rdf-syntax-ns#predicate */
	public final static IRI PREDICATE;

	/** http://www.w3.org/1999/02/22-rdf-syntax-ns#object */
	public final static IRI OBJECT;

	/** http://www.w3.org/1999/02/22-rdf-syntax-ns#Statement */
	public final static IRI STATEMENT;

	/** http://www.w3.org/1999/02/22-rdf-syntax-ns#Bag */
	public final static IRI BAG;

	/** http://www.w3.org/1999/02/22-rdf-syntax-ns#Alt */
	public final static IRI ALT;

	/** http://www.w3.org/1999/02/22-rdf-syntax-ns#Seq */
	public final static IRI SEQ;

	/** http://www.w3.org/1999/02/22-rdf-syntax-ns#value */
	public final static IRI VALUE;

	/** http://www.w3.org/1999/02/22-rdf-syntax-ns#li */
	public final static IRI LI;

	/** http://www.w3.org/1999/02/22-rdf-syntax-ns#List */
	public final static IRI LIST;

	/** http://www.w3.org/1999/02/22-rdf-syntax-ns#first */
	public final static IRI FIRST;

	/** http://www.w3.org/1999/02/22-rdf-syntax-ns#rest */
	public final static IRI REST;

	/** http://www.w3.org/1999/02/22-rdf-syntax-ns#nil */
	public final static IRI NIL;

	/** http://www.w3.org/1999/02/22-rdf-syntax-ns#langString */
	public static final IRI LANGSTRING;

	/** http://www.w3.org/1999/02/22-rdf-syntax-ns#HTML */
	public static final IRI HTML;

	static {
		TYPE = createIRI(RDF.NAMESPACE, "type");
		PROPERTY = createIRI(RDF.NAMESPACE, "Property");
		XMLLITERAL = createIRI(RDF.NAMESPACE, "XMLLiteral");
		SUBJECT = createIRI(RDF.NAMESPACE, "subject");
		PREDICATE = createIRI(RDF.NAMESPACE, "predicate");
		OBJECT = createIRI(RDF.NAMESPACE, "object");
		STATEMENT = createIRI(RDF.NAMESPACE, "Statement");
		BAG = createIRI(RDF.NAMESPACE, "Bag");
		ALT = createIRI(RDF.NAMESPACE, "Alt");
		SEQ = createIRI(RDF.NAMESPACE, "Seq");
		VALUE = createIRI(RDF.NAMESPACE, "value");
		LI = createIRI(RDF.NAMESPACE, "li");
		LIST = createIRI(RDF.NAMESPACE, "List");
		FIRST = createIRI(RDF.NAMESPACE, "first");
		REST = createIRI(RDF.NAMESPACE, "rest");
		NIL = createIRI(RDF.NAMESPACE, "nil");
		LANGSTRING = createIRI(RDF.NAMESPACE, "langString");
		HTML = createIRI(RDF.NAMESPACE, "HTML");
	}
}
