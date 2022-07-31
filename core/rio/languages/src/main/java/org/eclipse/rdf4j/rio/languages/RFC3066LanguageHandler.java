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
package org.eclipse.rdf4j.rio.languages;

import java.util.Objects;
import java.util.regex.Pattern;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.util.LiteralUtilException;
import org.eclipse.rdf4j.rio.LanguageHandler;

/**
 * A language handler that can verify RFC3066 formatted language tags.
 * <p>
 * This language handler normalises language tags to lower-case if
 * {@link #normalizeLanguage(String, String, ValueFactory)} is used.
 *
 * @see <a href="http://www.ietf.org/rfc/rfc3066.txt">RFC 3066</a>
 * @author Peter Ansell
 */
public class RFC3066LanguageHandler implements LanguageHandler {

	/**
	 * Language tag is RFC3066-conformant if it matches this regex: [a-zA-Z]{1,8}(-[a-zA-Z0-9]{1,8})*
	 */
	protected final Pattern matcher = Pattern.compile("[a-zA-Z]{1,8}(-[a-zA-Z0-9]{1,8})*");

	/**
	 * Default constructor.
	 */
	public RFC3066LanguageHandler() {
	}

	@Override
	public boolean isRecognizedLanguage(String languageTag) {
		Objects.requireNonNull(languageTag, "Language tag cannot be null");

		// language tag is RFC3066-conformant if it matches this regex:
		// [a-zA-Z]{1,8}(-[a-zA-Z0-9]{1,8})*
		boolean result = matcher.matcher(languageTag).matches();

		return result;
	}

	@Override
	public boolean verifyLanguage(String literalValue, String languageTag) throws LiteralUtilException {
		Objects.requireNonNull(languageTag, "Language tag cannot be null");
		Objects.requireNonNull(literalValue, "Literal value cannot be null");

		if (isRecognizedLanguage(languageTag)) {
			// Language tag syntax already checked in isRecognizedLanguage
			return true;
		}

		throw new LiteralUtilException("Could not verify RFC3066 language tag");
	}

	@Override
	public Literal normalizeLanguage(String literalValue, String languageTag, ValueFactory valueFactory)
			throws LiteralUtilException {
		Objects.requireNonNull(languageTag, "Language tag cannot be null");
		Objects.requireNonNull(literalValue, "Literal value cannot be null");

		if (isRecognizedLanguage(languageTag)) {
			return valueFactory.createLiteral(literalValue, languageTag.toLowerCase().intern());
		}

		throw new LiteralUtilException("Could not normalize RFC3066 language tag");
	}

	@Override
	public String getKey() {
		return LanguageHandler.RFC3066;
	}

}
