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
package org.eclipse.rdf4j.rio.helpers;

import javax.xml.XMLConstants;

import org.eclipse.rdf4j.rio.RioConfig;
import org.eclipse.rdf4j.rio.RioSetting;
import org.xml.sax.XMLReader;

/**
 * ParserSettings for the XML parser features.
 * <p>
 * Several of these settings can be overridden by means of a system property, but only if specified at JVM startup time.
 *
 * @author Michael Grove
 * @author Peter Ansell
 * @see XMLConstants
 * @see <a href="http://xerces.apache.org/xerces2-j/features.html">Apache XML Project - Features</a>
 */
public final class XMLParserSettings {

	/**
	 * Parser setting for the secure processing feature of XML parsers to avoid DOS attacks
	 * <p>
	 * Defaults to true
	 * <p>
	 * Can be overridden by setting system property {@code http://javax.xml.XMLConstants/feature/secure-processing}
	 *
	 * @see <a href= "http://docs.oracle.com/javase/6/docs/api/javax/xml/XMLConstants.html#FEATURE_SECURE_PROCESSING">
	 *      XMLConstants.FEATURE_SECURE_PROCESSING</a>
	 */
	public static final RioSetting<Boolean> SECURE_PROCESSING = new BooleanRioSetting(
			XMLConstants.FEATURE_SECURE_PROCESSING, "Secure processing feature of XMLConstants", true);

	/**
	 * Parser setting specifying whether DOCTYPE declaration should be disallowed.
	 * <p>
	 * Defaults to false. Can be overridden by setting system property
	 * {@code http://apache.org/xml/features/disallow-doctype-decl}
	 * <p>
	 *
	 * @see <a href="http://xerces.apache.org/xerces2-j/features.html">Apache XML Project - Features</a>
	 * @see <a href="https://www.owasp.org/index.php/XML_External_Entity_(XXE)_Prevention_Cheat_Sheet">XXE Prevention
	 *      Cheat Sheet</a>
	 */
	public static final RioSetting<Boolean> DISALLOW_DOCTYPE_DECL = new BooleanRioSetting(
			"http://apache.org/xml/features/disallow-doctype-decl", "Disallow DOCTYPE declaration in document", false);

	/**
	 * Parser setting specifying whether external DTDs should be loaded.
	 * <p>
	 * Defaults to false.
	 * <p>
	 * Can be overridden by setting system property
	 * {@code http://apache.org/xml/features/nonvalidating/load-external-dtd}
	 *
	 * @see <a href="http://xerces.apache.org/xerces2-j/features.html">Apache XML Project - Features</a>
	 */
	public static final RioSetting<Boolean> LOAD_EXTERNAL_DTD = new BooleanRioSetting(
			"http://apache.org/xml/features/nonvalidating/load-external-dtd", "Load External DTD", false);

	/**
	 * Parser setting specifying whether external text entities should be included.
	 * <p>
	 * Defaults to false.
	 * <p>
	 * Can be overridden by setting system property {@code http://xml.org/sax/features/external-general-entities}
	 *
	 * @see <a href="http://xerces.apache.org/xerces2-j/features.html">Apache XML Project - Features</a>
	 * @see <a href="https://www.owasp.org/index.php/XML_External_Entity_(XXE)_Prevention_Cheat_Sheet">XXE Prevention
	 *      Cheat Sheet</a>
	 */
	public static final RioSetting<Boolean> EXTERNAL_GENERAL_ENTITIES = new BooleanRioSetting(
			"http://xml.org/sax/features/external-general-entities", "Include external general entities", false);

	/**
	 * Parser setting specifying whether external parameter entities should be included.
	 * <p>
	 * Defaults to false.
	 * <p>
	 * Can be overridden by setting system property {@code http://xml.org/sax/features/external-parameter-entities}
	 *
	 * @see <a href="http://xerces.apache.org/xerces2-j/features.html">Apache XML Project - Features</a>
	 * @see <a href="https://www.owasp.org/index.php/XML_External_Entity_(XXE)_Prevention_Cheat_Sheet">XXE Prevention
	 *      Cheat Sheet</a>
	 */
	public static final RioSetting<Boolean> EXTERNAL_PARAMETER_ENTITIES = new BooleanRioSetting(
			"http://xml.org/sax/features/external-parameter-entities", "Include external parameter entities", false);

	/**
	 * Parser setting to customise the XMLReader that is used by an XML based Rio parser.
	 * <p>
	 * IMPORTANT: The XMLReader must not be shared across different readers, so this setting must be reset for each
	 * parse operation.
	 * <p>
	 * Defaults to null, This settings is only useful if {@link RioConfig#isSet(RioSetting)} returns true.
	 */
	public static final RioSetting<XMLReader> CUSTOM_XML_READER = new RioSettingImpl<>(
			"org.eclipse.rdf4j.rio.xmlreader", "Custom XML Reader", null);

	/**
	 * Parser setting to determine whether to ignore non-fatal errors that come from SAX parsers.
	 * <p>
	 * Defaults to true
	 * <p>
	 * Can be overridden by setting system property {@code org.eclipse.rdf4j.rio.fail_on_sax_non_fatal_errors}
	 */
	public static final RioSetting<Boolean> FAIL_ON_SAX_NON_FATAL_ERRORS = new BooleanRioSetting(
			"org.eclipse.rdf4j.rio.fail_on_sax_non_fatal_errors", "Fail on SAX non-fatal errors", true);

	/**
	 * Parser setting to determine whether to ignore non-standard attributes that are found in an XML document.
	 * <p>
	 * Defaults to true
	 * <p>
	 * Can be overridden by setting system property {@code org.eclipse.rdf4j.rio.fail_on_non_standard_attributes}
	 */
	public static final RioSetting<Boolean> FAIL_ON_NON_STANDARD_ATTRIBUTES = new BooleanRioSetting(
			"org.eclipse.rdf4j.rio.fail_on_non_standard_attributes", "Fail on non-standard attributes", true);

	/**
	 * Parser setting to determine whether to ignore XML documents containing invalid NCNAMEs.
	 * <p>
	 * Defaults to true
	 * <p>
	 * Can be overridden by setting system property {@code org.eclipse.rdf4j.rio.fail_on_invalid_ncname}
	 */
	public static final RioSetting<Boolean> FAIL_ON_INVALID_NCNAME = new BooleanRioSetting(
			"org.eclipse.rdf4j.rio.fail_on_invalid_ncname", "Fail on invalid NCName", true);

	/**
	 * Parser setting to determine whether to throw an error for duplicate uses of rdf:ID in a single document.
	 * <p>
	 * Defaults to true
	 * <p>
	 * Can be overridden by setting system property {@code org.eclipse.rdf4j.rio.fail_on_duplicate_rdf_id}
	 */
	public static final RioSetting<Boolean> FAIL_ON_DUPLICATE_RDF_ID = new BooleanRioSetting(
			"org.eclipse.rdf4j.rio.fail_on_duplicate_rdf_id", "Fail on duplicate RDF ID", true);

	/**
	 * Parser setting to determine whether to ignore XML documents containing invalid QNAMEs.
	 * <p>
	 * Defaults to true
	 * <p>
	 * Can be overridden by setting system property {@code org.eclipse.rdf4j.rio.fail_on_invalid_qname}
	 */
	public static final RioSetting<Boolean> FAIL_ON_INVALID_QNAME = new BooleanRioSetting(
			"org.eclipse.rdf4j.rio.fail_on_invalid_qname", "Fail on invalid QName", true);

	/**
	 * Parser setting to determine whether to throw an error for XML documents containing mismatched tags
	 * <p>
	 * Defaults to true
	 * <p>
	 * Can be overridden by setting system property {@code org.eclipse.rdf4j.rio.fail_on_mismatched_tags}
	 */
	public static final RioSetting<Boolean> FAIL_ON_MISMATCHED_TAGS = new BooleanRioSetting(
			"org.eclipse.rdf4j.rio.fail_on_mismatched_tags", "Fail on mismatched tags", true);

	/**
	 * Flag indicating whether the parser parses stand-alone RDF documents. In stand-alone documents, the rdf:RDF
	 * element is optional if it contains just one element.
	 * <p>
	 * Defaults to true
	 * <p>
	 * Can be overridden by setting system property {@code org.eclipse.rdf4j.rio.parse_standalone_documents}
	 */
	public static final RioSetting<Boolean> PARSE_STANDALONE_DOCUMENTS = new BooleanRioSetting(
			"org.eclipse.rdf4j.rio.parse_standalone_documents", "Parse standalone documents", true);

	/**
	 * Private constructor
	 */
	private XMLParserSettings() {
	}

}
