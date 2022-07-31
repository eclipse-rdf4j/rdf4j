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
package org.eclipse.rdf4j.spin;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.FN;
import org.eclipse.rdf4j.model.vocabulary.SP;
import org.eclipse.rdf4j.query.algebra.evaluation.function.FunctionRegistry;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

final class SpinWellKnownFunctions {

	private static final ValueFactory valueFactory = SimpleValueFactory.getInstance();

	private static final FunctionRegistry functionRegistry = FunctionRegistry.getInstance();

	static final SpinWellKnownFunctions INSTANCE = new SpinWellKnownFunctions();

	private final BiMap<String, IRI> stringToUri = HashBiMap.create(64);

	private final BiMap<IRI, String> uriToString = stringToUri.inverse();

	public SpinWellKnownFunctions() {
		stringToUri.put(FN.SUBSTRING.stringValue(), valueFactory.createIRI(SP.NAMESPACE, "substr"));
		stringToUri.put(FN.SUBSTRING_BEFORE.stringValue(), valueFactory.createIRI(SP.NAMESPACE, "strbefore"));
		stringToUri.put(FN.SUBSTRING_AFTER.stringValue(), valueFactory.createIRI(SP.NAMESPACE, "strafter"));
		stringToUri.put(FN.STARTS_WITH.stringValue(), valueFactory.createIRI(SP.NAMESPACE, "strstarts"));
		stringToUri.put(FN.ENDS_WITH.stringValue(), valueFactory.createIRI(SP.NAMESPACE, "strends"));
		stringToUri.put(FN.STRING_LENGTH.stringValue(), valueFactory.createIRI(SP.NAMESPACE, "strlen"));
		stringToUri.put(FN.CONCAT.stringValue(), valueFactory.createIRI(SP.NAMESPACE, "concat"));
		stringToUri.put(FN.CONTAINS.stringValue(), valueFactory.createIRI(SP.NAMESPACE, "contains"));
		stringToUri.put(FN.LOWER_CASE.stringValue(), valueFactory.createIRI(SP.NAMESPACE, "lcase"));
		stringToUri.put(FN.UPPER_CASE.stringValue(), valueFactory.createIRI(SP.NAMESPACE, "ucase"));
		stringToUri.put(FN.REPLACE.stringValue(), valueFactory.createIRI(SP.NAMESPACE, "replace"));
		stringToUri.put(FN.NUMERIC_ABS.stringValue(), valueFactory.createIRI(SP.NAMESPACE, "abs"));
		stringToUri.put(FN.NUMERIC_CEIL.stringValue(), valueFactory.createIRI(SP.NAMESPACE, "ceil"));
		stringToUri.put(FN.NUMERIC_FLOOR.stringValue(), valueFactory.createIRI(SP.NAMESPACE, "floor"));
		stringToUri.put(FN.NUMERIC_ROUND.stringValue(), valueFactory.createIRI(SP.NAMESPACE, "round"));
		stringToUri.put(FN.YEAR_FROM_DATETIME.stringValue(), valueFactory.createIRI(SP.NAMESPACE, "year"));
		stringToUri.put(FN.MONTH_FROM_DATETIME.stringValue(), valueFactory.createIRI(SP.NAMESPACE, "month"));
		stringToUri.put(FN.DAY_FROM_DATETIME.stringValue(), valueFactory.createIRI(SP.NAMESPACE, "day"));
		stringToUri.put(FN.HOURS_FROM_DATETIME.stringValue(), valueFactory.createIRI(SP.NAMESPACE, "hours"));
		stringToUri.put(FN.MINUTES_FROM_DATETIME.stringValue(), valueFactory.createIRI(SP.NAMESPACE, "minutes"));
		stringToUri.put(FN.SECONDS_FROM_DATETIME.stringValue(), valueFactory.createIRI(SP.NAMESPACE, "seconds"));
		stringToUri.put(FN.TIMEZONE_FROM_DATETIME.stringValue(), valueFactory.createIRI(SP.NAMESPACE, "timezone"));
		stringToUri.put(FN.ENCODE_FOR_URI.stringValue(), valueFactory.createIRI(SP.NAMESPACE, "encode_for_uri"));
		stringToUri.put("NOW", valueFactory.createIRI(SP.NAMESPACE, "now"));
		stringToUri.put("RAND", valueFactory.createIRI(SP.NAMESPACE, "rand"));
		stringToUri.put("STRDT", valueFactory.createIRI(SP.NAMESPACE, "strdt"));
		stringToUri.put("STRLANG", valueFactory.createIRI(SP.NAMESPACE, "strlang"));
		stringToUri.put("TZ", valueFactory.createIRI(SP.NAMESPACE, "tz"));
		stringToUri.put("UUID", valueFactory.createIRI(SP.NAMESPACE, "uuid"));
		stringToUri.put("STRUUID", valueFactory.createIRI(SP.NAMESPACE, "struuid"));
		stringToUri.put("MD5", valueFactory.createIRI(SP.NAMESPACE, "md5"));
		stringToUri.put("SHA1", valueFactory.createIRI(SP.NAMESPACE, "sha1"));
		stringToUri.put("SHA256", valueFactory.createIRI(SP.NAMESPACE, "sha256"));
		stringToUri.put("SHA384", valueFactory.createIRI(SP.NAMESPACE, "sha384"));
		stringToUri.put("SHA512", valueFactory.createIRI(SP.NAMESPACE, "sha512"));
	}

	public IRI getURI(String name) {
		IRI iri = stringToUri.get(name);
		if (iri == null && functionRegistry.has(name)) {
			iri = valueFactory.createIRI(name);
		}
		return iri;
	}

	public String getName(IRI IRI) {
		String name = uriToString.get(IRI);
		if (name == null && functionRegistry.has(IRI.stringValue())) {
			name = IRI.stringValue();
		}
		return name;
	}
}
