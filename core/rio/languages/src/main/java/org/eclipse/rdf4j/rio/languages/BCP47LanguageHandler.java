/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.rio.languages;

import java.util.IllformedLocaleException;
import java.util.Objects;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.util.LiteralUtilException;
import org.eclipse.rdf4j.model.util.Literals;
import org.eclipse.rdf4j.rio.LanguageHandler;

/**
 * A language handler that can verify BCP47 formatted language tags.
 * <p>
 * This language handler normalises language tags to lower-case if
 * {@link #normalizeLanguage(String, String, ValueFactory)} is used.
 *
 * @see <a href="https://tools.ietf.org/html/bcp47">BCP47</a>
 * @author Peter Ansell
 */
public class BCP47LanguageHandler implements LanguageHandler {

	/**
	 * Default constructor.
	 */
	public BCP47LanguageHandler() {
	}

	@Override
	public boolean isRecognizedLanguage(String languageTag) {
		Objects.requireNonNull(languageTag, "Language tag cannot be null");

		try {
			Literals.normalizeLanguageTag(languageTag);
		} catch (IllformedLocaleException e) {
			return false;
		}

		return true;
	}

	@Override
	public boolean verifyLanguage(String literalValue, String languageTag) throws LiteralUtilException {
		Objects.requireNonNull(languageTag, "Language tag cannot be null");
		Objects.requireNonNull(literalValue, "Literal value cannot be null");

		if (isRecognizedLanguage(languageTag)) {
			// Language tag syntax already checked in isRecognizedLanguage
			return true;
		}

		throw new LiteralUtilException("Could not verify BCP47 language tag");
	}

	@Override
	public Literal normalizeLanguage(String literalValue, String languageTag, ValueFactory valueFactory)
			throws LiteralUtilException {
		Objects.requireNonNull(languageTag, "Language tag cannot be null");
		Objects.requireNonNull(literalValue, "Literal value cannot be null");

		try {
			return valueFactory.createLiteral(literalValue, Literals.normalizeLanguageTag(languageTag));
		} catch (IllformedLocaleException e) {
			throw new LiteralUtilException("Could not normalize BCP47 language tag", e);
		}
	}

	@Override
	public String getKey() {
		return LanguageHandler.BCP47;
	}
}
