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
 * Constants for the <a href="http://www.w3.org/TR/rdf-schema/">RDF Vocabulary Description Language 1.0: RDF Schema</a>
 * (RDFS)
 *
 * @see <a href="http://www.w3.org/TR/rdf-schema/">RDF Vocabulary Description Language 1.0: RDF Schema (RDFS)</a>
 */
public class RDFS {

	/** The RDF Schema namepace: http://www.w3.org/2000/01/rdf-schema# */
	public static final String NAMESPACE = "http://www.w3.org/2000/01/rdf-schema#";

	/** Recommended prefix for the RDF Schema namespace: "rdfs" */
	public static final String PREFIX = "rdfs";

	/**
	 * An immutable {@link Namespace} constant that represents the RDFS namespace.
	 */
	public static final Namespace NS = createNamespace(PREFIX, NAMESPACE);

	/** http://www.w3.org/2000/01/rdf-schema#Resource */
	public final static IRI RESOURCE;

	/** http://www.w3.org/2000/01/rdf-schema#Literal */
	public final static IRI LITERAL;

	/** http://www.w3.org/2000/01/rdf-schema#Class */
	public final static IRI CLASS;

	/** http://www.w3.org/2000/01/rdf-schema#subClassOf */
	public final static IRI SUBCLASSOF;

	/** http://www.w3.org/2000/01/rdf-schema#subPropertyOf */
	public final static IRI SUBPROPERTYOF;

	/** http://www.w3.org/2000/01/rdf-schema#domain */
	public final static IRI DOMAIN;

	/** http://www.w3.org/2000/01/rdf-schema#range */
	public final static IRI RANGE;

	/** http://www.w3.org/2000/01/rdf-schema#comment */
	public final static IRI COMMENT;

	/** http://www.w3.org/2000/01/rdf-schema#label */
	public final static IRI LABEL;

	/** http://www.w3.org/2000/01/rdf-schema#Datatype */
	public final static IRI DATATYPE;

	/** http://www.w3.org/2000/01/rdf-schema#Container */
	public final static IRI CONTAINER;

	/** http://www.w3.org/2000/01/rdf-schema#member */
	public final static IRI MEMBER;

	/** http://www.w3.org/2000/01/rdf-schema#isDefinedBy */
	public final static IRI ISDEFINEDBY;

	/** http://www.w3.org/2000/01/rdf-schema#seeAlso */
	public final static IRI SEEALSO;

	/** http://www.w3.org/2000/01/rdf-schema#ContainerMembershipProperty */
	public final static IRI CONTAINERMEMBERSHIPPROPERTY;

	static {
		RESOURCE = createIRI(RDFS.NAMESPACE, "Resource");
		LITERAL = createIRI(RDFS.NAMESPACE, "Literal");
		CLASS = createIRI(RDFS.NAMESPACE, "Class");
		SUBCLASSOF = createIRI(RDFS.NAMESPACE, "subClassOf");
		SUBPROPERTYOF = createIRI(RDFS.NAMESPACE, "subPropertyOf");
		DOMAIN = createIRI(RDFS.NAMESPACE, "domain");
		RANGE = createIRI(RDFS.NAMESPACE, "range");
		COMMENT = createIRI(RDFS.NAMESPACE, "comment");
		LABEL = createIRI(RDFS.NAMESPACE, "label");
		DATATYPE = createIRI(RDFS.NAMESPACE, "Datatype");
		CONTAINER = createIRI(RDFS.NAMESPACE, "Container");
		MEMBER = createIRI(RDFS.NAMESPACE, "member");
		ISDEFINEDBY = createIRI(RDFS.NAMESPACE, "isDefinedBy");
		SEEALSO = createIRI(RDFS.NAMESPACE, "seeAlso");
		CONTAINERMEMBERSHIPPROPERTY = createIRI(RDFS.NAMESPACE, "ContainerMembershipProperty");
	}
}
