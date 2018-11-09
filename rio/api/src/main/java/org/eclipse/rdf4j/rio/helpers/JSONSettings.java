/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.helpers;

import org.eclipse.rdf4j.rio.RioSetting;

/**
 * Generic JSON settings, mostly related to Jackson Features.
 *
 * @author Peter Ansell
 */
public class JSONSettings {

	/**
	 * Boolean setting for JSON parsers to determine if any character is allowed to be backslash escaped.
	 * <p>
	 * Defaults to false.
	 */
	public static final RioSetting<Boolean> ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER = new RioSettingImpl<>(
			"org.eclipse.rdf4j.rio.json.allow_backslash_escaping_any_character",
			"Allow backslash escaping any character", Boolean.FALSE);

	/**
	 * Boolean setting for JSON parsers to determine if Java/C++ style comments are allowed.
	 * <p>
	 * Defaults to false.
	 */
	public static final RioSetting<Boolean> ALLOW_COMMENTS = new RioSettingImpl<>(
			"org.eclipse.rdf4j.rio.json.allow_comments", "Allow comments", Boolean.FALSE);

	/**
	 * Boolean setting for JSON parsers to determine if non-numeric numbers (INF/-INF/NaN) are allowed.
	 * <p>
	 * Defaults to false.
	 */
	public static final RioSetting<Boolean> ALLOW_NON_NUMERIC_NUMBERS = new RioSettingImpl<>(
			"org.eclipse.rdf4j.rio.json.allow_non_numeric_numbers", "Allow non-numeric numbers",
			Boolean.FALSE);

	/**
	 * Boolean setting for JSON parsers to determine if numeric leading zeroes are allowed.
	 * <p>
	 * Defaults to false.
	 */
	public static final RioSetting<Boolean> ALLOW_NUMERIC_LEADING_ZEROS = new RioSettingImpl<>(
			"org.eclipse.rdf4j.rio.json.allow_numeric_leading_zeros", "Allow numeric leading zeros",
			Boolean.FALSE);

	/**
	 * Boolean setting for JSON parsers to determine if single quotes are allowed.
	 * <p>
	 * Defaults to false.
	 */
	public static final RioSetting<Boolean> ALLOW_SINGLE_QUOTES = new RioSettingImpl<>(
			"org.eclipse.rdf4j.rio.json.allow_single_quotes", "Allow single quotes", Boolean.FALSE);

	/**
	 * Boolean setting for JSON parsers to determine if unquoted control characters are allowed.
	 * <p>
	 * Defaults to false.
	 */
	public static final RioSetting<Boolean> ALLOW_UNQUOTED_CONTROL_CHARS = new RioSettingImpl<>(
			"org.eclipse.rdf4j.rio.json.allow_unquoted_control_chars", "Allow unquoted control chars",
			Boolean.FALSE);

	/**
	 * Boolean setting for JSON parsers to determine if unquoted field names are allowed.
	 * <p>
	 * Defaults to false.
	 */
	public static final RioSetting<Boolean> ALLOW_UNQUOTED_FIELD_NAMES = new RioSettingImpl<>(
			"org.eclipse.rdf4j.rio.json.allow_unquoted_field_names", "Allow unquoted field names",
			Boolean.FALSE);

	/**
	 * Boolean setting for JSON parsers to determine if YAML comments (starting with '#') are allowed.
	 * <p>
	 * Defaults to false.
	 */
	public static final RioSetting<Boolean> ALLOW_YAML_COMMENTS = new RioSettingImpl<>(
			"org.eclipse.rdf4j.rio.json.allow_yaml_comments", "Allow YAML comments", Boolean.FALSE);

	/**
	 * Boolean setting for JSON parsers to determine if errors should include a reference to the source or
	 * not.
	 * <p>
	 * Defaults to true.
	 */
	public static final RioSetting<Boolean> INCLUDE_SOURCE_IN_LOCATION = new RioSettingImpl<>(
			"org.eclipse.rdf4j.rio.json.include_source_in_location", "Include Source in Location",
			Boolean.TRUE);

	/**
	 * Boolean setting for JSON parsers to determine if strict duplicate detection is allowed for JSON Object
	 * field names.
	 * <p>
	 * Defaults to false.
	 */
	public static final RioSetting<Boolean> STRICT_DUPLICATE_DETECTION = new RioSettingImpl<>(
			"org.eclipse.rdf4j.rio.json.strict_duplicate_detection", "Strict duplicate detection",
			Boolean.FALSE);

	/**
	 * Private default constructor.
	 */
	private JSONSettings() {
	}

}
