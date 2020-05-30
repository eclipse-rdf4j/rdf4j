/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.model.vocabulary;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

/**
 * Constants for the Hydra Core Vocabulary.
 *
 * @see <a href="http://www.hydra-cg.com/spec/latest/core/">Hydra Core Vocabulary</a>
 *
 */
public class HYDRA {

	/**
	 * The HYDRA namespace: http://www.w3.org/ns/hydra/core#
	 */
	public static final String NAMESPACE = "http://www.w3.org/ns/hydra/core#";

	/**
	 * The recommended prefix for the HYDRA namespace: "hydra"
	 */
	public static final String PREFIX = "hydra";

	/**
	 * An immutable {@link Namespace} constant that represents the HYDRA namespace.
	 */
	public static final Namespace NS = new SimpleNamespace(PREFIX, NAMESPACE);

	// ----- Classes ------
	public final static IRI API_DOCUMENTATION;

	public final static IRI CLASS;

	public final static IRI COLLECTION;

	public final static IRI ERROR;

	public final static IRI IRI_TEMPLATE;

	public final static IRI IRI_TEMPLATE_MAPPING;

	public final static IRI LINK;

	public final static IRI OPERATION;

	public final static IRI PARTIAL_COLLECTION_VIEW;

	public final static IRI RESOURCE;

	public final static IRI STATUS;

	public final static IRI SUPPORTED_PROPERTY;

	public final static IRI TEMPLATED_LINK;

	public final static IRI VARIABLE_REPRESENTATION;

	// ----- Properties ------
	public final static IRI API_DOCUMENTATION_PROP;

	public final static IRI COLLECTION_PROP;

	public final static IRI DESCRIPTION;

	public final static IRI ENTRYPOINT;

	public final static IRI EXPECTS;

	public final static IRI EXPECTS_HEADER;

	public final static IRI FIRST;

	public final static IRI FREETEXT_QUERY;

	public final static IRI LAST;

	public final static IRI LIMIT;

	public final static IRI MAPPING;

	public final static IRI MEMBER;

	public final static IRI METHOD;

	public final static IRI NEXT;

	public final static IRI OFFSET;

	public final static IRI OPERATION_PROP;

	public final static IRI PAGE_INDEX;

	public final static IRI PAGE_REFERENCE;

	public final static IRI POSSIBLE_STATUS;

	public final static IRI PREVIOUS;

	public final static IRI PROPERTY;

	public final static IRI READABLE;

	public final static IRI REQUIRED;

	public final static IRI RETURNS;

	public final static IRI RETURNS_HEADER;

	public final static IRI SEARCH;

	public final static IRI STATUS_CODE;

	public final static IRI SUPPORTED_CLASS;

	public final static IRI SUPPORTED_OPERATION;

	public final static IRI SUPPORTED_PROPERTY_PROP;

	public final static IRI TEMPLATE;

	public final static IRI TITLE;

	public final static IRI TOTAL_ITEMS;

	public final static IRI VARIABLE;

	public final static IRI VARIABLE_REPRESENTATION_PROP;

	public final static IRI VIEW;

	public final static IRI WRITABLE;

	static {
		ValueFactory factory = SimpleValueFactory.getInstance();

		// ----- Classes ------
		API_DOCUMENTATION = factory.createIRI(HYDRA.NAMESPACE, "ApiDocumentation");
		CLASS = factory.createIRI(HYDRA.NAMESPACE, "Class");
		COLLECTION = factory.createIRI(HYDRA.NAMESPACE, "Collection");
		ERROR = factory.createIRI(HYDRA.NAMESPACE, "Error");
		IRI_TEMPLATE = factory.createIRI(HYDRA.NAMESPACE, "IriTemplate");
		IRI_TEMPLATE_MAPPING = factory.createIRI(HYDRA.NAMESPACE, "IriTemplateMapping");
		LINK = factory.createIRI(HYDRA.NAMESPACE, "Link");
		OPERATION = factory.createIRI(HYDRA.NAMESPACE, "Operation");
		PARTIAL_COLLECTION_VIEW = factory.createIRI(HYDRA.NAMESPACE, "PartialCollectionView");
		RESOURCE = factory.createIRI(HYDRA.NAMESPACE, "Resource");
		STATUS = factory.createIRI(HYDRA.NAMESPACE, "Status");
		SUPPORTED_PROPERTY = factory.createIRI(HYDRA.NAMESPACE, "SupportedProperty");
		TEMPLATED_LINK = factory.createIRI(HYDRA.NAMESPACE, "TemplatedLink");
		VARIABLE_REPRESENTATION = factory.createIRI(HYDRA.NAMESPACE, "VariableRepresentation");

		// ----- Properties ------
		API_DOCUMENTATION_PROP = factory.createIRI(HYDRA.NAMESPACE, "apiDocumentation");
		COLLECTION_PROP = factory.createIRI(HYDRA.NAMESPACE, "collection");
		DESCRIPTION = factory.createIRI(HYDRA.NAMESPACE, "description");
		ENTRYPOINT = factory.createIRI(HYDRA.NAMESPACE, "entrypoint");
		EXPECTS = factory.createIRI(HYDRA.NAMESPACE, "expects");
		EXPECTS_HEADER = factory.createIRI(HYDRA.NAMESPACE, "expectsHeader");
		FIRST = factory.createIRI(HYDRA.NAMESPACE, "first");
		FREETEXT_QUERY = factory.createIRI(HYDRA.NAMESPACE, "freetextQuery");
		LAST = factory.createIRI(HYDRA.NAMESPACE, "last");
		LIMIT = factory.createIRI(HYDRA.NAMESPACE, "limit");
		MAPPING = factory.createIRI(HYDRA.NAMESPACE, "mapping");
		MEMBER = factory.createIRI(HYDRA.NAMESPACE, "member");
		METHOD = factory.createIRI(HYDRA.NAMESPACE, "method");
		NEXT = factory.createIRI(HYDRA.NAMESPACE, "next");
		OFFSET = factory.createIRI(HYDRA.NAMESPACE, "offset");
		OPERATION_PROP = factory.createIRI(HYDRA.NAMESPACE, "operation");
		PAGE_INDEX = factory.createIRI(HYDRA.NAMESPACE, "pageIndex");
		PAGE_REFERENCE = factory.createIRI(HYDRA.NAMESPACE, "pageReference");
		POSSIBLE_STATUS = factory.createIRI(HYDRA.NAMESPACE, "possibleStatus");
		PREVIOUS = factory.createIRI(HYDRA.NAMESPACE, "previous");
		PROPERTY = factory.createIRI(HYDRA.NAMESPACE, "property");
		READABLE = factory.createIRI(HYDRA.NAMESPACE, "readable");
		REQUIRED = factory.createIRI(HYDRA.NAMESPACE, "required");
		RETURNS = factory.createIRI(HYDRA.NAMESPACE, "returns");
		RETURNS_HEADER = factory.createIRI(HYDRA.NAMESPACE, "returnsHeader");
		SEARCH = factory.createIRI(HYDRA.NAMESPACE, "search");
		STATUS_CODE = factory.createIRI(HYDRA.NAMESPACE, "statusCode");
		SUPPORTED_CLASS = factory.createIRI(HYDRA.NAMESPACE, "supportedClass");
		SUPPORTED_OPERATION = factory.createIRI(HYDRA.NAMESPACE, "supportedOperation");
		SUPPORTED_PROPERTY_PROP = factory.createIRI(HYDRA.NAMESPACE, "supportedProperty");
		TEMPLATE = factory.createIRI(HYDRA.NAMESPACE, "template");
		TITLE = factory.createIRI(HYDRA.NAMESPACE, "title");
		TOTAL_ITEMS = factory.createIRI(HYDRA.NAMESPACE, "totalItems");
		VARIABLE = factory.createIRI(HYDRA.NAMESPACE, "variable");
		VARIABLE_REPRESENTATION_PROP = factory.createIRI(HYDRA.NAMESPACE, "variableRepresentation");
		VIEW = factory.createIRI(HYDRA.NAMESPACE, "view");
		WRITABLE = factory.createIRI(HYDRA.NAMESPACE, "writeable");

	}
}
