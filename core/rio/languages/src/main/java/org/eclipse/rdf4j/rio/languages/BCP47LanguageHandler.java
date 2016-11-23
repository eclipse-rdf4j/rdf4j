/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.languages;

import java.util.IllformedLocaleException;
import java.util.Locale;
import java.util.regex.Pattern;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.util.LiteralUtilException;
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
		if (languageTag == null) {
			throw new NullPointerException("Language tag cannot be null");
		}

		try {
			parseAsBCP47(languageTag);
		}
		catch (IllformedLocaleException e) {
			return false;
		}

		return true;
	}

	@Override
	public boolean verifyLanguage(String literalValue, String languageTag)
		throws LiteralUtilException
	{
		if (isRecognizedLanguage(languageTag)) {
			if (literalValue == null) {
				throw new NullPointerException("Literal value cannot be null");
			}

			// Language tag syntax already checked in isRecognizedLanguage
			return true;
		}

		throw new LiteralUtilException("Could not verify BCP47 language tag");
	}

	@Override
	public Literal normalizeLanguage(String literalValue, String languageTag, ValueFactory valueFactory)
		throws LiteralUtilException
	{
		if (languageTag == null) {
			throw new NullPointerException("Language tag cannot be null");
		}

		try {
			Locale asBCP47 = parseAsBCP47(languageTag);
			
			if (literalValue == null) {
				throw new NullPointerException("Literal value cannot be null");
			}

			return valueFactory.createLiteral(literalValue, asBCP47.toLanguageTag().intern());
		}
		catch (IllformedLocaleException e) {
			throw new LiteralUtilException("Could not normalize BCP47 language tag", e);
		}
	}

	@Override
	public String getKey() {
		return LanguageHandler.BCP47;
	}

	private Locale parseAsBCP47(String languageTag)
		throws IllformedLocaleException
	{
		return new Locale.Builder().setLanguageTag(languageTag).build();
	}

}
