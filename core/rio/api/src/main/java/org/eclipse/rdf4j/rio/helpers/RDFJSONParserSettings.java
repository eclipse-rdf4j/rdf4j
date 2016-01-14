/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.helpers;

import org.eclipse.rdf4j.rio.RioSetting;

/**
 * A selection of parser settings specific to RDF/JSON parsers.
 * 
 * @author Peter Ansell
 * @since 2.7.1
 */
public class RDFJSONParserSettings {

	/**
	 * Boolean setting for parser to determine whether an RDF/JSON parser should
	 * fail if it finds multiple values for a single object in a single
	 * statement.
	 * <p>
	 * Defaults to true.
	 * 
	 * @since 2.7.1
	 */
	public static final RioSetting<Boolean> FAIL_ON_MULTIPLE_OBJECT_VALUES = new RioSettingImpl<Boolean>(
			"org.eclipse.rdf4j.rio.failonmultipleobjectvalues", "Fail on multiple object values", Boolean.TRUE);

	/**
	 * Boolean setting for parser to determine whether an RDF/JSON parser should
	 * fail if it finds multiple types for a single object in a single statement.
	 * <p>
	 * Defaults to true.
	 * 
	 * @since 2.7.1
	 */
	public static final RioSetting<Boolean> FAIL_ON_MULTIPLE_OBJECT_TYPES = new RioSettingImpl<Boolean>(
			"org.eclipse.rdf4j.rio.failonmultipleobjecttypes", "Fail on multiple object types", Boolean.TRUE);

	/**
	 * Boolean setting for parser to determine whether an RDF/JSON parser should
	 * fail if it finds multiple languages for a single object in a single
	 * statement.
	 * <p>
	 * Defaults to true.
	 * 
	 * @since 2.7.1
	 */
	public static final RioSetting<Boolean> FAIL_ON_MULTIPLE_OBJECT_LANGUAGES = new RioSettingImpl<Boolean>(
			"org.eclipse.rdf4j.rio.failonmultipleobjectlanguages", "Fail on multiple object languages", Boolean.TRUE);

	/**
	 * Boolean setting for parser to determine whether an RDF/JSON parser should
	 * fail if it finds multiple datatypes for a single object in a single
	 * statement.
	 * <p>
	 * Defaults to true.
	 * 
	 * @since 2.7.1
	 */
	public static final RioSetting<Boolean> FAIL_ON_MULTIPLE_OBJECT_DATATYPES = new RioSettingImpl<Boolean>(
			"org.eclipse.rdf4j.rio.failonmultipleobjectdatatypes", "Fail on multiple object datatypes", Boolean.TRUE);

	/**
	 * Boolean setting for parser to determine whether an RDF/JSON parser should
	 * fail if it finds multiple properties that it does not recognise in the
	 * JSON document.
	 * <p>
	 * Defaults to true.
	 * 
	 * @since 2.7.1
	 */
	public static final RioSetting<Boolean> FAIL_ON_UNKNOWN_PROPERTY = new RioSettingImpl<Boolean>(
			"org.eclipse.rdf4j.rio.failonunknownproperty", "Fail on unknown property", Boolean.TRUE);

	/**
	 * Boolean setting for parser to determine whether an RDF/JSON parser should
	 * support the graphs extension to make it a quads format.
	 * <p>
	 * Defaults to true.
	 * 
	 * @since 2.7.1
	 */
	public static final RioSetting<Boolean> SUPPORT_GRAPHS_EXTENSION = new RioSettingImpl<Boolean>(
			"org.eclipse.rdf4j.rio.supportgraphsextension", "SUPPORT_GRAPHS_EXTENSION", Boolean.TRUE);

	/**
	 * Private default constructor.
	 */
	private RDFJSONParserSettings() {
	}

}
