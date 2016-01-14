/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.util.LiteralUtilException;

/**
 * An interface defining methods related to verification and normalization of
 * language tags.
 * <p>
 * The language handler may optionally provide normalization and verification
 * services for string literals based on the language tags, including
 * translation, grammar and spelling checks. However, this behavior is entirely
 * driven by the user.
 * 
 * @author Peter Ansell
 * @since 2.7.0
 */
public interface LanguageHandler {

	/**
	 * Identifier for the language tag format defined by RFC3066, which is
	 * referenced by the RDF specification.
	 */
	public static final String RFC3066 = "org.eclipse.rdf4j.rio.languages.RFC3066";

	/**
	 * Identifier for the language tag format defined by RFC4646, which obsoletes
	 * RFC3066, but which is not referenced by the RDF specification.
	 */
	public static final String RFC4646 = "org.eclipse.rdf4j.rio.languages.RFC4646";

	/**
	 * Checks if the given language tag is recognized by this language handler,
	 * including cases where the language tag is recognized, but is not yet
	 * normalized.
	 * 
	 * @param languageTag
	 *        The language tag to check.
	 * @return True if the language tag is syntactically valid and could be used
	 *         with {@link #verifyLanguage(String, String)} and
	 *         {@link #normalizeLanguage(String, String, ValueFactory)}.
	 * @since 2.7.0
	 */
	public boolean isRecognizedLanguage(String languageTag);

	/**
	 * Verifies that the language tag is valid, optionally including an automated
	 * check on the literal value.
	 * <p>
	 * This method must only be called after verifying that
	 * {@link #isRecognizedLanguage(String)} returns true for the given language
	 * tag.
	 * 
	 * @param literalValue
	 *        Literal value matching the given language tag.
	 * @param languageTag
	 *        A language tag that matched with
	 *        {@link #isRecognizedLanguage(String)}.
	 * @return True if the language tag is recognized by this language handler,
	 *         and it is verified to be syntactically valid.
	 * @since 2.7.0
	 * @throws LiteralUtilException
	 *         If the language tag was not recognized.
	 */
	public boolean verifyLanguage(String literalValue, String languageTag)
		throws LiteralUtilException;

	/**
	 * Normalize both the language tag and the language if appropriate, and use
	 * the given value factory to generate a literal matching the literal value
	 * and language tag.
	 * <p>
	 * This method must only be called after verifying that
	 * {@link #isRecognizedLanguage(String)} returns true for the given language
	 * tag, and {@link #verifyLanguage(String, String)} also returns true for the
	 * given language and literal value.
	 * 
	 * @param literalValue
	 *        Required literal value to use in the normalization process and to
	 *        provide the value for the resulting literal.
	 * @param languageTag
	 *        The language tag which is to be normalized. This tag is available
	 *        in normalized form from the result using
	 *        {@link Literal#getLanguage()}.
	 * @param valueFactory
	 *        The {@link ValueFactory} to use to create the result literal.
	 * @return A {@link Literal} containing the normalized literal value and
	 *         language tag.
	 * @since 2.7.0
	 * @throws LiteralUtilException
	 *         If the language tag was not recognized or verified, or the literal
	 *         value could not be normalized due to an error.
	 */
	public Literal normalizeLanguage(String literalValue, String languageTag, ValueFactory valueFactory)
		throws LiteralUtilException;

	/**
	 * A unique key for this language handler to identify it in the
	 * LanguageHandlerRegistry.
	 * 
	 * @return A unique string key.
	 * @since 2.7.0
	 */
	public String getKey();

}
