/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.model.vocabulary;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.ValueFactoryImpl;

/**
 * http://spinrdf.org/spif#.
 */
public final class SPIF {

	private SPIF() {
	}

	/**
	 * http://spinrdf.org/spif
	 */
	public static final String NAMESPACE = "http://spinrdf.org/spif#";

	public static final String PREFIX = "spif";

	public static final IRI MOD_FUNCTION;

	public static final IRI PARSE_DATE_FUNCTION;

	public static final IRI DATE_FORMAT_FUNCTION;

	public static final IRI DECIMAL_FORMAT_FUNCTION;

	public static final IRI TRIM_FUNCTION;

	public static final IRI CURRENT_TIME_MILLIS_FUNCTION;

	public static final IRI TIME_MILLIS_FUNCTION;

	public static final IRI GENERATE_UUID_FUNCTION;

	public static final IRI RANDOM_FUNCTION;

	public static final IRI CAST_FUNCTION;

	public static final IRI INDEX_OF_FUNCTION;

	public static final IRI LAST_INDEX_OF_FUNCTION;

	public static final IRI ENCODE_URL_FUNCTION;

	public static final IRI DECODE_URL_FUNCTION;

	public static final IRI BUILD_STRING_FUNCTION;

	public static final IRI BUILD_URI_FUNCTION;

	public static final IRI CONVERT_SPIN_RDF_TO_STRING_FUNCTION;

	public static final IRI REPLACE_ALL_FUNCTION;

	public static final IRI NAME_FUNCTION;

	public static final IRI UN_CAMEL_CASE_FUNCTION;

	public static final IRI IS_VALID_URI_FUNCTION;

	public static final IRI HAS_ALL_OBJECTS_FUNCTION;

	public static final IRI INVOKE_FUNCTION;

	public static final IRI CAN_INVOKE_FUNCTION;

	public static final IRI UPPER_CASE_FUNCTION;

	public static final IRI LOWER_CASE_FUNCTION;

	public static final IRI TITLE_CASE_FUNCTION;

	public static final IRI LOWER_TITLE_CASE_FUNCTION;

	public static final IRI FOR_EACH_PROPERTY;

	public static final IRI FOR_PROPERTY;

	public static final IRI SPLIT_PROPERTY;

	static {
		ValueFactory factory = ValueFactoryImpl.getInstance();
		MOD_FUNCTION = factory.createIRI(NAMESPACE, "mod");
		PARSE_DATE_FUNCTION = factory.createIRI(NAMESPACE, "parseDate");
		DATE_FORMAT_FUNCTION = factory.createIRI(NAMESPACE, "dateFormat");
		DECIMAL_FORMAT_FUNCTION = factory.createIRI(NAMESPACE, "decimalFormat");
		TRIM_FUNCTION = factory.createIRI(NAMESPACE, "trim");
		CURRENT_TIME_MILLIS_FUNCTION = factory.createIRI(NAMESPACE, "currentTimeMillis");
		TIME_MILLIS_FUNCTION = factory.createIRI(NAMESPACE, "timeMillis");
		GENERATE_UUID_FUNCTION = factory.createIRI(NAMESPACE, "generateUUID");
		RANDOM_FUNCTION = factory.createIRI(NAMESPACE, "random");
		CAST_FUNCTION = factory.createIRI(NAMESPACE, "cast");
		INDEX_OF_FUNCTION = factory.createIRI(NAMESPACE, "indexOf");
		LAST_INDEX_OF_FUNCTION = factory.createIRI(NAMESPACE, "lastIndexOf");
		ENCODE_URL_FUNCTION = factory.createIRI(NAMESPACE, "encodeURL");
		DECODE_URL_FUNCTION = factory.createIRI(NAMESPACE, "decodeURL");
		BUILD_STRING_FUNCTION = factory.createIRI(NAMESPACE, "buildString");
		BUILD_URI_FUNCTION = factory.createIRI(NAMESPACE, "buildURI");
		CONVERT_SPIN_RDF_TO_STRING_FUNCTION = factory.createIRI(NAMESPACE, "convertSPINRDFToString");
		REPLACE_ALL_FUNCTION = factory.createIRI(NAMESPACE, "replaceAll");
		NAME_FUNCTION = factory.createIRI(NAMESPACE, "name");
		UN_CAMEL_CASE_FUNCTION = factory.createIRI(NAMESPACE, "unCamelCase");
		IS_VALID_URI_FUNCTION = factory.createIRI(NAMESPACE, "isValidURI");
		HAS_ALL_OBJECTS_FUNCTION = factory.createIRI(NAMESPACE, "hasAllObjects");
		INVOKE_FUNCTION = factory.createIRI(NAMESPACE, "invoke");
		CAN_INVOKE_FUNCTION = factory.createIRI(NAMESPACE, "canInvoke");
		UPPER_CASE_FUNCTION = factory.createIRI(NAMESPACE, "upperCase");
		LOWER_CASE_FUNCTION = factory.createIRI(NAMESPACE, "lowerCase");
		TITLE_CASE_FUNCTION = factory.createIRI(NAMESPACE, "titleCase");
		LOWER_TITLE_CASE_FUNCTION = factory.createIRI(NAMESPACE, "lowerTitleCase");

		FOR_EACH_PROPERTY = factory.createIRI(NAMESPACE, "foreach");
		FOR_PROPERTY = factory.createIRI(NAMESPACE, "for");
		SPLIT_PROPERTY = factory.createIRI(NAMESPACE, "split");
	}
}
