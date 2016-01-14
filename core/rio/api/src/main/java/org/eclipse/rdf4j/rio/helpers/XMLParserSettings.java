/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.helpers;

import javax.xml.XMLConstants;

import org.eclipse.rdf4j.rio.RioConfig;
import org.eclipse.rdf4j.rio.RioSetting;
import org.xml.sax.XMLReader;

/**
 * ParserSettings for the XML parser features.
 * 
 * @author Michael Grove
 * @author Peter Ansell
 * @see XMLConstants
 * @see <a href="http://xerces.apache.org/xerces-j/features.html">Apache XML
 *      Project - Features</a>
 * @since 2.7.0
 */
public final class XMLParserSettings {

	/**
	 * Parser setting for the secure processing feature of XML parsers to avoid
	 * DOS attacks
	 * <p>
	 * Defaults to true
	 * 
	 * @see <a
	 *      href="http://docs.oracle.com/javase/6/docs/api/javax/xml/XMLConstants.html#FEATURE_SECURE_PROCESSING">
	 *      XMLConstants.FEATURE_SECURE_PROCESSING</a>
	 * @since 2.7.0
	 */
	public static final RioSetting<Boolean> SECURE_PROCESSING = new RioSettingImpl<Boolean>(
			XMLConstants.FEATURE_SECURE_PROCESSING, "Secure processing feature of XMLConstants", true);

	/**
	 * Parser setting specifying whether external DTDs should be loaded.
	 * <p>
	 * Defaults to true.
	 * 
	 * @see <a href="http://xerces.apache.org/xerces-j/features.html">Apache XML
	 *      Project - Features</a>
	 * @since 2.7.0
	 */
	public static final RioSetting<Boolean> LOAD_EXTERNAL_DTD = new RioSettingImpl<Boolean>(
			"http://apache.org/xml/features/nonvalidating/load-external-dtd", "Load External DTD", true);

	/**
	 * Parser setting to customise the XMLReader that is used by an XML based Rio
	 * parser.
	 * <p>
	 * IMPORTANT: The XMLReader must not be shared across different readers, so
	 * this setting must be reset for each parse operation.
	 * <p>
	 * Defaults to null, This settings is only useful if
	 * {@link RioConfig#isSet(RioSetting)} returns true.
	 * 
	 * @since 2.7.0
	 */
	public static final RioSetting<XMLReader> CUSTOM_XML_READER = new RioSettingImpl<XMLReader>(
			"org.eclipse.rdf4j.rio.xmlreader", "Custom XML Reader", null);

	/**
	 * Parser setting to determine whether to ignore non-fatal errors that come
	 * from SAX parsers.
	 * <p>
	 * Defaults to true
	 * 
	 * @since 2.7.0
	 */
	public static final RioSetting<Boolean> FAIL_ON_SAX_NON_FATAL_ERRORS = new RioSettingImpl<Boolean>(
			"org.eclipse.rdf4j.rio.failonsaxnonfatalerrors", "Fail on SAX non-fatal errors", true);

	/**
	 * Parser setting to determine whether to ignore non-standard attributes that
	 * are found in an XML document.
	 * <p>
	 * Defaults to true
	 * 
	 * @since 2.7.0
	 */
	public static final RioSetting<Boolean> FAIL_ON_NON_STANDARD_ATTRIBUTES = new RioSettingImpl<Boolean>(
			"org.eclipse.rdf4j.rio.failonnonstandardattributes", "Fail on non-standard attributes", true);

	/**
	 * Parser setting to determine whether to ignore XML documents containing
	 * invalid NCNAMEs.
	 * <p>
	 * Defaults to true
	 * 
	 * @since 2.7.0
	 */
	public static final RioSetting<Boolean> FAIL_ON_INVALID_NCNAME = new RioSettingImpl<Boolean>(
			"org.eclipse.rdf4j.rio.failoninvalidncname", "Fail on invalid NCName", true);

	/**
	 * Parser setting to determine whether to throw an error for duplicate uses
	 * of rdf:ID in a single document.
	 * <p>
	 * Defaults to true
	 * 
	 * @since 2.7.0
	 */
	public static final RioSetting<Boolean> FAIL_ON_DUPLICATE_RDF_ID = new RioSettingImpl<Boolean>(
			"org.eclipse.rdf4j.rio.failonduplicaterdfid", "Fail on duplicate RDF ID", true);

	/**
	 * Parser setting to determine whether to ignore XML documents containing
	 * invalid QNAMEs.
	 * <p>
	 * Defaults to true
	 * 
	 * @since 2.7.0
	 */
	public static final RioSetting<Boolean> FAIL_ON_INVALID_QNAME = new RioSettingImpl<Boolean>(
			"org.eclipse.rdf4j.rio.failoninvalidqname", "Fail on invalid QName", true);

	/**
	 * Parser setting to determine whether to throw an error for XML documents
	 * containing mismatched tags
	 * <p>
	 * Defaults to true
	 * 
	 * @since 2.7.1
	 */
	public static final RioSetting<Boolean> FAIL_ON_MISMATCHED_TAGS = new RioSettingImpl<Boolean>(
			"org.eclipse.rdf4j.rio.failonmismatchedtags", "Fail on mismatched tags", true);

	/**
	 * Flag indicating whether the parser parses stand-alone RDF documents. In
	 * stand-alone documents, the rdf:RDF element is optional if it contains just
	 * one element.
	 * <p>
	 * Defaults to true
	 * 
	 * @since 2.7.8
	 */
	public static final RioSetting<Boolean> PARSE_STANDALONE_DOCUMENTS = new RioSettingImpl<Boolean>(
			"org.eclipse.rdf4j.rio.parsestandalonedocuments", "Parse standalone documents", true);

	/**
	 * Private constructor
	 */
	private XMLParserSettings() {
	}

}
