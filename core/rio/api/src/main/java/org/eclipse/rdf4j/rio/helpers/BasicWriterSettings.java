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

import org.eclipse.rdf4j.rio.RioSetting;

/**
 * A class encapsulating the basic writer settings that most writers may support.
 *
 * @author Peter Ansell
 */
public class BasicWriterSettings {

	/**
	 * Boolean setting for writer to determine whether pretty printing is preferred.
	 * <p>
	 * Defaults to true
	 * <p>
	 * Can be overridden by setting system property {@code org.eclipse.rdf4j.rio.pretty_print}.
	 */
	public static final RioSetting<Boolean> PRETTY_PRINT = new BooleanRioSetting("org.eclipse.rdf4j.rio.pretty_print",
			"Pretty print", Boolean.TRUE);

	/**
	 * Inline blanks nodes by their value and don't write any blank node labels when this setting is true. This setting
	 * should only be used when blank nodes never appear in the context and there are no blank node cycles.
	 * <p>
	 * WARNING: This setting requires all triples to be processed before being written and could use a lot of memory in
	 * the process and should be set to false for large RDF files.
	 * <p>
	 * Defaults to false.
	 * <p>
	 * Can be overridden by setting system property {@code org.eclipse.rdf4j.rio.inline_blank_nodes}.
	 *
	 * @since 2.3
	 */
	public static final RioSetting<Boolean> INLINE_BLANK_NODES = new BooleanRioSetting(
			"org.eclipse.rdf4j.rio.inline_blank_nodes",
			"Use blank node property lists, collections, and anonymous nodes instead of blank node labels",
			Boolean.FALSE);

	/**
	 * Boolean setting for writer to determine whether it should remove the xsd:string datatype from literals and
	 * represent them as RDF-1.0 Plain Literals.
	 * <p>
	 * In RDF-1.1, all literals that would have been Plain Literals in RDF-1.0 will be typed as xsd:string internally.
	 * <p>
	 * Defaults to true to allow for backwards compatibility without enforcing it.
	 * <p>
	 * Can be overridden by setting system property {@code org.eclipse.rdf4j.rio.rdf10_plain_literals}.
	 */
	public static final RioSetting<Boolean> XSD_STRING_TO_PLAIN_LITERAL = new BooleanRioSetting(
			"org.eclipse.rdf4j.rio.rdf10_plain_literals", "RDF-1.0 compatible Plain Literals", Boolean.TRUE);

	/**
	 * Boolean setting for writer to determine whether it should omit the rdf:langString datatype from language literals
	 * when serialising them.
	 * <p>
	 * In RDF-1.1, all RDF-1.0 Language Literals are typed using rdf:langString in the abstract model, but this datatype
	 * is not necessary for concrete syntaxes.
	 * <p>
	 * In most concrete syntaxes it is either syntactically invalid or semantically ambiguous to have a language tagged
	 * literal with an explicit datatype. In those cases this setting will not be used, and the rdf:langString datatype
	 * will not be attached to language tagged literals.
	 * <p>
	 * In particular, in RDF/XML, if rdf:langString is serialised, the language tag may not be retained when the
	 * document is parsed due to the precedence rule in RDF/XML for datatype over language.
	 * <p>
	 * Defaults to true as rdf:langString was not previously used, and should not be commonly required.
	 * <p>
	 * Can be overridden by setting system property {@code org.eclipse.rdf4j.rio.rdf10_language_literals}.
	 */
	public static final RioSetting<Boolean> RDF_LANGSTRING_TO_LANG_LITERAL = new BooleanRioSetting(
			"org.eclipse.rdf4j.rio.rdf10_language_literals", "RDF-1.0 compatible Language Literals", Boolean.TRUE);

	/**
	 * Boolean setting for writer to determine whether it should include a base directive.
	 * <p>
	 * Defaults to true
	 * <p>
	 * Can be overridden by setting system property {@code org.eclipse.rdf4j.rio.base_directive}.
	 */
	public static final RioSetting<Boolean> BASE_DIRECTIVE = new BooleanRioSetting(
			"org.eclipse.rdf4j.rio.base_directive", "Serialize base directive", Boolean.TRUE);

	/**
	 * Boolean setting for writer to determine whether it should convert RDF-star statements to standard RDF
	 * reification.
	 * <p>
	 * Defaults to false
	 * <p>
	 * Can be overridden by setting system property {@code org.eclipse.rdf4j.rio.convert_rdf_star}.
	 */
	public static final RioSetting<Boolean> CONVERT_RDF_STAR_TO_REIFICATION = new BooleanRioSetting(
			"org.eclipse.rdf4j.rio.convert_rdf_star", "Convert RDF-star statements to RDF reification", Boolean.FALSE);

	/**
	 * Boolean setting for writer to determine whether it should encode RDF-star triple values to RDF-compatible special
	 * IRIs. These IRIs start with urn:rdf4j:triple: followed by the base64-encoding of the N-Triples serialization of
	 * the RDF-star triple value.
	 * <p>
	 * Writers that support RDF-star natively will ignore this setting and always serialize RDF-star triples.
	 * <p>
	 * Defaults to true.
	 * <p>
	 * Can be overridden by setting system property {@code org.eclipse.rdf4j.rio.encode_rdf_star}.
	 */
	public static final RioSetting<Boolean> ENCODE_RDF_STAR = new BooleanRioSetting(
			"org.eclipse.rdf4j.rio.encode_rdf_star",
			"Encodes RDF-star triples to special IRIs for compatibility with RDF", Boolean.TRUE);

	/**
	 * Private default constructor.
	 */
	private BasicWriterSettings() {
	}

}
