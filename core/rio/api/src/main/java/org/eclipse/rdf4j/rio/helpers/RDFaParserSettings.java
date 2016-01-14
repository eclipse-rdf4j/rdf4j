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
 * A selection of parser settings specific to RDFa parsers.
 * 
 * @author Peter Ansell
 * @since 2.7.0
 */
public class RDFaParserSettings {

	/**
	 * Boolean setting for parser to determine the RDFa version to use when
	 * processing the document.
	 * <p>
	 * Defaults to {@link RDFaVersion#RDFA_1_0}.
	 * 
	 * @since 2.7.0
	 */
	public static final RioSetting<RDFaVersion> RDFA_COMPATIBILITY = new RioSettingImpl<RDFaVersion>(
			"org.eclipse.rdf4j.rio.rdfa.version", "RDFa Version Compatibility", RDFaVersion.RDFA_1_0);

	/**
	 * Enables or disables <a href=
	 * "http://www.w3.org/TR/2012/REC-rdfa-core-20120607/#s_vocab_expansion"
	 * >vocabulary expansion</a> feature.
	 * <p>
	 * Defaults to false
	 * 
	 * @see <a
	 *      href="http://www.w3.org/TR/2012/REC-rdfa-core-20120607/#s_vocab_expansion">RDFa
	 *      Vocabulary Expansion</a>
	 * @since 2.7.0
	 */
	public static final RioSetting<Boolean> VOCAB_EXPANSION_ENABLED = new RioSettingImpl<Boolean>(
			"http://www.w3.org/TR/2012/REC-rdfa-core-20120607/#s_vocab_expansion", "Vocabulary Expansion",
			Boolean.FALSE);
	/**
	 * Boolean setting for parser to determine whether the published RDFa
	 * prefixes are used to substitute for undefined prefixes.
	 * <p>
	 * Defaults to false.
	 *
	 * @since 2.7.0
	 * @deprecated Use {@link BasicParserSettings#NAMESPACES}
	 */
	@Deprecated
	public static final RioSetting<Boolean> FAIL_ON_RDFA_UNDEFINED_PREFIXES = new RioSettingImpl<Boolean>(
		    "org.eclipse.rdf4j.rio.allowrdfaundefinedprefixes", "Allow RDFa Undefined Prefixes", Boolean.FALSE);

	/**
	 * Private default constructor.
	 */
	private RDFaParserSettings() {
	}

}
