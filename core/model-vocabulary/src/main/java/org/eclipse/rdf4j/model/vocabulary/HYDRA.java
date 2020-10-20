/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
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
 * Constants for the Hydra Core Vocabulary.
 *
 * @see <a href="http://www.hydra-cg.com/spec/latest/core/">Hydra Core Vocabulary</a>
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
	public static final Namespace NS = createNamespace(PREFIX, NAMESPACE);

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

		// ----- Classes ------
		API_DOCUMENTATION = createIRI(HYDRA.NAMESPACE, "ApiDocumentation");
		CLASS = createIRI(HYDRA.NAMESPACE, "Class");
		COLLECTION = createIRI(HYDRA.NAMESPACE, "Collection");
		ERROR = createIRI(HYDRA.NAMESPACE, "Error");
		IRI_TEMPLATE = createIRI(HYDRA.NAMESPACE, "IriTemplate");
		IRI_TEMPLATE_MAPPING = createIRI(HYDRA.NAMESPACE, "IriTemplateMapping");
		LINK = createIRI(HYDRA.NAMESPACE, "Link");
		OPERATION = createIRI(HYDRA.NAMESPACE, "Operation");
		PARTIAL_COLLECTION_VIEW = createIRI(HYDRA.NAMESPACE, "PartialCollectionView");
		RESOURCE = createIRI(HYDRA.NAMESPACE, "Resource");
		STATUS = createIRI(HYDRA.NAMESPACE, "Status");
		SUPPORTED_PROPERTY = createIRI(HYDRA.NAMESPACE, "SupportedProperty");
		TEMPLATED_LINK = createIRI(HYDRA.NAMESPACE, "TemplatedLink");
		VARIABLE_REPRESENTATION = createIRI(HYDRA.NAMESPACE, "VariableRepresentation");

		// ----- Properties ------
		API_DOCUMENTATION_PROP = createIRI(HYDRA.NAMESPACE, "apiDocumentation");
		COLLECTION_PROP = createIRI(HYDRA.NAMESPACE, "collection");
		DESCRIPTION = createIRI(HYDRA.NAMESPACE, "description");
		ENTRYPOINT = createIRI(HYDRA.NAMESPACE, "entrypoint");
		EXPECTS = createIRI(HYDRA.NAMESPACE, "expects");
		EXPECTS_HEADER = createIRI(HYDRA.NAMESPACE, "expectsHeader");
		FIRST = createIRI(HYDRA.NAMESPACE, "first");
		FREETEXT_QUERY = createIRI(HYDRA.NAMESPACE, "freetextQuery");
		LAST = createIRI(HYDRA.NAMESPACE, "last");
		LIMIT = createIRI(HYDRA.NAMESPACE, "limit");
		MAPPING = createIRI(HYDRA.NAMESPACE, "mapping");
		MEMBER = createIRI(HYDRA.NAMESPACE, "member");
		METHOD = createIRI(HYDRA.NAMESPACE, "method");
		NEXT = createIRI(HYDRA.NAMESPACE, "next");
		OFFSET = createIRI(HYDRA.NAMESPACE, "offset");
		OPERATION_PROP = createIRI(HYDRA.NAMESPACE, "operation");
		PAGE_INDEX = createIRI(HYDRA.NAMESPACE, "pageIndex");
		PAGE_REFERENCE = createIRI(HYDRA.NAMESPACE, "pageReference");
		POSSIBLE_STATUS = createIRI(HYDRA.NAMESPACE, "possibleStatus");
		PREVIOUS = createIRI(HYDRA.NAMESPACE, "previous");
		PROPERTY = createIRI(HYDRA.NAMESPACE, "property");
		READABLE = createIRI(HYDRA.NAMESPACE, "readable");
		REQUIRED = createIRI(HYDRA.NAMESPACE, "required");
		RETURNS = createIRI(HYDRA.NAMESPACE, "returns");
		RETURNS_HEADER = createIRI(HYDRA.NAMESPACE, "returnsHeader");
		SEARCH = createIRI(HYDRA.NAMESPACE, "search");
		STATUS_CODE = createIRI(HYDRA.NAMESPACE, "statusCode");
		SUPPORTED_CLASS = createIRI(HYDRA.NAMESPACE, "supportedClass");
		SUPPORTED_OPERATION = createIRI(HYDRA.NAMESPACE, "supportedOperation");
		SUPPORTED_PROPERTY_PROP = createIRI(HYDRA.NAMESPACE, "supportedProperty");
		TEMPLATE = createIRI(HYDRA.NAMESPACE, "template");
		TITLE = createIRI(HYDRA.NAMESPACE, "title");
		TOTAL_ITEMS = createIRI(HYDRA.NAMESPACE, "totalItems");
		VARIABLE = createIRI(HYDRA.NAMESPACE, "variable");
		VARIABLE_REPRESENTATION_PROP = createIRI(HYDRA.NAMESPACE, "variableRepresentation");
		VIEW = createIRI(HYDRA.NAMESPACE, "view");
		WRITABLE = createIRI(HYDRA.NAMESPACE, "writeable");

	}
}
