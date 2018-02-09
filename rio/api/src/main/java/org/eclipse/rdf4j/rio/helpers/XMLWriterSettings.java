/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.helpers;

import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.RioSetting;

/**
 * A class encapsulating writer settings that XML writers may support.
 * 
 * @author Peter Ansell
 */
public class XMLWriterSettings {

	/**
	 * Boolean setting for XML Writer to determine whether the XML PI (Processing Instruction) should be
	 * printed. If this setting is disabled the user must have previously printed the XML PI before calling
	 * {@link RDFWriter#startRDF()} for the document to be valid XML.
	 * <p>
	 * Defaults to true.
	 * 
	 * @see <a href="http://www.w3.org/TR/rdf-syntax-grammar/#section-Syntax-complete-document">RDF/XML
	 *      specification</a>
	 */
	public static final RioSetting<Boolean> INCLUDE_XML_PI = new RioSettingImpl<Boolean>(
			"org.eclipse.rdf4j.rio.includexmlpi", "Include XML Processing Instruction", Boolean.TRUE);

	/**
	 * Boolean setting for RDF/XML Writer to determine whether the rdf:RDF root tag is to be written. The tag
	 * is optional in the RDF/XML specification, but a standalone RDF/XML document typically includes it.
	 * <p>
	 * Defaults to true.
	 * 
	 * @see <a href="http://www.w3.org/TR/rdf-syntax-grammar/#section-Syntax-complete-document">RDF/XML
	 *      specification</a>
	 */
	public static final RioSetting<Boolean> INCLUDE_ROOT_RDF_TAG = new RioSettingImpl<Boolean>(
			"org.eclipse.rdf4j.rio.includerootrdftag", "Include Root RDF Tag", Boolean.TRUE);

	/**
	 * Private default constructor.
	 */
	private XMLWriterSettings() {
	}

}
