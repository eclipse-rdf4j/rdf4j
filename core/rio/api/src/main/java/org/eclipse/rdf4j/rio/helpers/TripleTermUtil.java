/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.rio.helpers;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Triple;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

/**
 * Utility methods for RDF 1.2 triples.
 *
 * @author Pavel Mihaylov
 */
public class TripleTermUtil {
	/**
	 * IRI prefix for RDF 1.2 triples encoded as IRIs.
	 */
	public static final String TRIPLE_PREFIX = "urn:rdf4j:triple:";

	private static final ValueFactory VF = SimpleValueFactory.getInstance();

	/**
	 * Converts the supplied value from RDF 1.2 to an RDF-compatible representation.
	 * <p>
	 * RDF 1.2 triples are encoded as IRIs that start with {@link #TRIPLE_PREFIX}, followed by the base64 encoding of
	 * the N-Triples serialization of the triple.
	 * <p>
	 * All other RDF 1.2 values are valid in RDF as well and remain unchanged.
	 *
	 * @param value a RDF 1.2 {@link Value} to encode.
	 * @param <T>
	 * @return the RDF-compatible encoded value, if a {@link Triple} was supplied, or the supplied value otherwise.
	 */
	public static <T extends Value> T toRDFEncodedValue(T value) {
		return value instanceof Triple
				? (T) VF.createIRI(TRIPLE_PREFIX + encode(NTriplesUtil.toNTriplesString(value)))
				: value;
	}

	/**
	 * Converts the supplied value from an RDF-compatible representation to an RDF 1.2 value.
	 * <p>
	 * See {@link #toRDFEncodedValue(Value)}.
	 *
	 * @param encodedValue an RDF {@link Value} to convert to RDF 1.2.
	 * @param <T>
	 * @return the decoded RDF 1.2 triple, if a {@link Triple} encoded as {@link IRI} was supplied, or the supplied
	 *         value otherwise.
	 * @throws IllegalArgumentException if the supplied value looked like an RDF 1.2 triple encoded as an IRI but it
	 *                                  could not be decoded successfully.
	 */
	public static <T extends Value> T fromRDFEncodedValue(T encodedValue) {
		try {
			return isEncodedTriple(encodedValue)
					? (T) NTriplesUtil.parseTriple(decode(
							encodedValue.stringValue().substring(TRIPLE_PREFIX.length())), VF)
					: encodedValue;
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Invalid RDF 1.2 encoded triple: " + encodedValue);
		}
	}

	/**
	 * Converts the supplied value from an RDF-compatible representation to an RDF 1.2 value.
	 * <p>
	 * See {@link #toRDFEncodedValue(Value)}.
	 *
	 * @param encodedValue an RDF {@link Value} to convert to RDF 1.2.
	 * @param valueFactory the {@link ValueFactory} to use for parsing the triple.
	 * @param <T>
	 * @return the decoded RDF 1.2 triple, if a {@link Triple} encoded as {@link IRI} was supplied, or the supplied
	 *         value otherwise.
	 * @throws IllegalArgumentException if the supplied value looked like an RDF 1.2 triple encoded as an IRI but it
	 *                                  could not be decoded successfully.
	 */
	public static <T extends Value> T fromRDFEncodedValue(T encodedValue, ValueFactory valueFactory) {
		try {
			return isEncodedTriple(encodedValue)
					? (T) NTriplesUtil.parseTriple(decode(
							encodedValue.stringValue().substring(TRIPLE_PREFIX.length())), valueFactory)
					: encodedValue;
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Invalid RDF 1.2 encoded triple: " + encodedValue);
		}
	}

	/**
	 * Checks if the supplied {@link Value} represents an RDF 1.2 triple encoded as an IRI.
	 *
	 * @param value the value to check.
	 * @return True if the value is an RDF 1.2 triple encoded as an IRI, false otherwise.
	 */
	public static boolean isEncodedTriple(Value value) {
		return value instanceof IRI && value.stringValue().startsWith(TRIPLE_PREFIX);
	}

	private static String encode(String s) {
		return Base64.getUrlEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8));
	}

	private static String decode(String s) {
		return new String(Base64.getUrlDecoder().decode(s), StandardCharsets.UTF_8);
	}
}
