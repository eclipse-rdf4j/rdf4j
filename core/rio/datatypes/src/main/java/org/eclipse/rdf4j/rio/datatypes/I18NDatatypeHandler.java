/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.rio.datatypes;

import java.util.Locale;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.util.LiteralUtilException;
import org.eclipse.rdf4j.rio.DatatypeHandler;
import org.eclipse.rdf4j.rio.LanguageHandler;
import org.eclipse.rdf4j.rio.LanguageHandlerRegistry;

/**
 *
 * Datatype handler for literals using the W3C i18n datatype namespace {@code https://www.w3.org/ns/i18n#}.
 *
 * These datatypes encode a language tag and base direction in the datatype IRI and are used when downgrading RDF 1.2
 * directional language-tagged strings to RDF 1.1.
 */
public class I18NDatatypeHandler implements DatatypeHandler {

	public static final String I18N_NAMESPACE = "https://www.w3.org/ns/i18n#";

	private static final LanguageHandler BCP47_HANDLER;

	static {
		BCP47_HANDLER = LanguageHandlerRegistry.getInstance()
				.get(LanguageHandler.BCP47)
				.orElseThrow(() -> new IllegalStateException("BCP47 language handler not available"));
	}

	@Override
	public boolean isRecognizedDatatype(IRI datatypeUri) {
		if (datatypeUri == null) {
			throw new NullPointerException("Datatype URI cannot be null");
		}
		return datatypeUri.stringValue().startsWith(I18N_NAMESPACE);
	}

	@Override
	public boolean verifyDatatype(String literalValue, IRI datatypeUri) throws LiteralUtilException {
		if (isRecognizedDatatype(datatypeUri)) {
			if (literalValue == null) {
				throw new NullPointerException("Literal value cannot be null");
			}

			String suffix = datatypeUri.stringValue().substring(I18N_NAMESPACE.length());

			int underscore = suffix.lastIndexOf('_');
			String direction;
			String language = null;

			if (underscore == -1) {
				direction = suffix;
			} else {
				language = suffix.substring(0, underscore);
				direction = suffix.substring(underscore + 1);
			}

			if (!direction.equals("ltr") && !direction.equals("rtl")) {
				return false;
			}

			if (language != null) {
				return BCP47_HANDLER.verifyLanguage(literalValue, language);
			}
			return true;
		}
		throw new LiteralUtilException("Not an i18n datatype: " + datatypeUri);
	}

	@Override
	public Literal normalizeDatatype(String literalValue, IRI datatypeUri, ValueFactory valueFactory)
			throws LiteralUtilException {
		if (isRecognizedDatatype(datatypeUri)) {
			if (literalValue == null) {
				throw new NullPointerException("Literal value cannot be null");
			}
			String iriStr = datatypeUri.stringValue();
			String suffix = iriStr.substring(I18N_NAMESPACE.length()).toLowerCase(Locale.ROOT);

			IRI normalizedDatatype = valueFactory.createIRI(I18N_NAMESPACE + suffix);
			return valueFactory.createLiteral(literalValue, normalizedDatatype);
		}
		throw new LiteralUtilException("Not an i18n datatype: " + datatypeUri);
	}

	@Override
	public String getKey() {
		return DatatypeHandler.I18N;
	}
}
