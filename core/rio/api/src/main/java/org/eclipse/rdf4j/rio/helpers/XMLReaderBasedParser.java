/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.rio.helpers;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.rdf4j.common.xml.XMLReaderFactory;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.rio.RioSetting;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;

/**
 * Base class for Rio parsers that are based on a SAX {@link XMLReader}.
 *
 * @author Jeen Broekstra
 */
public abstract class XMLReaderBasedParser extends AbstractRDFParser {

	private final static Set<RioSetting<Boolean>> compulsoryXmlFeatureSettings = new HashSet<>(
			Arrays.asList(XMLParserSettings.SECURE_PROCESSING, XMLParserSettings.DISALLOW_DOCTYPE_DECL,
					XMLParserSettings.EXTERNAL_GENERAL_ENTITIES, XMLParserSettings.EXTERNAL_PARAMETER_ENTITIES,
					XMLParserSettings.LOAD_EXTERNAL_DTD));

	protected XMLReaderBasedParser(ValueFactory f) {
		super(f);
	}

	/**
	 * Returns a collection of settings that will always be set as XML parser properties using
	 * {@link XMLReader#setProperty(String, Object)}
	 * <p>
	 * Subclasses can override this to specify more supported settings.
	 *
	 * @return A collection of {@link RioSetting}s that indicate which properties will always be setup using
	 *         {@link XMLReader#setProperty(String, Object)}.
	 */
	public Collection<RioSetting<?>> getCompulsoryXmlPropertySettings() {
		return Collections.<RioSetting<?>>emptyList();
	}

	/**
	 * Returns a collection of settings that will always be set as XML parser features using
	 * {@link XMLReader#setFeature(String, boolean)}.
	 * <p>
	 * Subclasses can override this to specify more supported settings.
	 *
	 * @return A collection of {@link RioSetting}s that indicate which boolean settings will always be setup using
	 *         {@link XMLReader#setFeature(String, boolean)}.
	 */
	public Collection<RioSetting<Boolean>> getCompulsoryXmlFeatureSettings() {
		return Collections.unmodifiableSet(compulsoryXmlFeatureSettings);
	}

	/**
	 * Returns a collection of settings that will be used, if set in {@link #getParserConfig()}, as XML parser
	 * properties using {@link XMLReader#setProperty(String, Object)}
	 * <p>
	 * Subclasses can override this to specify more supported settings.
	 *
	 * @return A collection of {@link RioSetting}s that indicate which properties can be setup using
	 *         {@link XMLReader#setProperty(String, Object)}.
	 */
	public Collection<RioSetting<?>> getOptionalXmlPropertySettings() {
		return Collections.<RioSetting<?>>emptyList();
	}

	/**
	 * Returns a collection of settings that will be used, if set in {@link #getParserConfig()}, as XML parser features
	 * using {@link XMLReader#setFeature(String, boolean)}.
	 * <p>
	 * Subclasses can override this to specify more supported settings.
	 *
	 * @return A collection of {@link RioSetting}s that indicate which boolean settings can be setup using
	 *         {@link XMLReader#setFeature(String, boolean)}.
	 */
	public Collection<RioSetting<Boolean>> getOptionalXmlFeatureSettings() {
		return Collections.<RioSetting<Boolean>>emptyList();
	}

	/**
	 * Creates an XML Reader configured using the current parser settings.
	 *
	 * @return a configured {@link XMLReader}
	 * @throws SAXException if an error occurs during configuration.
	 */
	protected XMLReader getXMLReader() throws SAXException {

		XMLReader xmlReader;

		if (getParserConfig().isSet(XMLParserSettings.CUSTOM_XML_READER)) {
			xmlReader = getParserConfig().get(XMLParserSettings.CUSTOM_XML_READER);
		} else {
			xmlReader = XMLReaderFactory.createXMLReader();
		}

		// Set all compulsory feature settings, using the defaults if they are
		// not explicitly set
		for (RioSetting<Boolean> aSetting : getCompulsoryXmlFeatureSettings()) {
			try {
				xmlReader.setFeature(aSetting.getKey(), getParserConfig().get(aSetting));
			} catch (SAXNotRecognizedException e) {
				reportWarning(String.format("%s is not a recognized SAX feature.", aSetting.getKey()));
			} catch (SAXNotSupportedException e) {
				reportWarning(String.format("%s is not a supported SAX feature.", aSetting.getKey()));
			}
		}

		// Set all compulsory property settings, using the defaults if they are
		// not explicitly set
		for (RioSetting<?> aSetting : getCompulsoryXmlPropertySettings()) {
			try {
				xmlReader.setProperty(aSetting.getKey(), getParserConfig().get(aSetting));
			} catch (SAXNotRecognizedException e) {
				reportWarning(String.format("%s is not a recognized SAX property.", aSetting.getKey()));
			} catch (SAXNotSupportedException e) {
				reportWarning(String.format("%s is not a supported SAX property.", aSetting.getKey()));
			}
		}

		// Check for any optional feature settings that are explicitly set in
		// the parser config
		for (RioSetting<Boolean> aSetting : getOptionalXmlFeatureSettings()) {
			try {
				if (getParserConfig().isSet(aSetting)) {
					xmlReader.setFeature(aSetting.getKey(), getParserConfig().get(aSetting));
				}
			} catch (SAXNotRecognizedException e) {
				reportWarning(String.format("%s is not a recognized SAX feature.", aSetting.getKey()));
			} catch (SAXNotSupportedException e) {
				reportWarning(String.format("%s is not a supported SAX feature.", aSetting.getKey()));
			}
		}

		// Check for any optional property settings that are explicitly set in
		// the parser config
		for (RioSetting<?> aSetting : getOptionalXmlPropertySettings()) {
			try {
				if (getParserConfig().isSet(aSetting)) {
					xmlReader.setProperty(aSetting.getKey(), getParserConfig().get(aSetting));
				}
			} catch (SAXNotRecognizedException e) {
				reportWarning(String.format("%s is not a recognized SAX property.", aSetting.getKey()));
			} catch (SAXNotSupportedException e) {
				reportWarning(String.format("%s is not a supported SAX property.", aSetting.getKey()));
			}
		}

		return xmlReader;
	}
}
