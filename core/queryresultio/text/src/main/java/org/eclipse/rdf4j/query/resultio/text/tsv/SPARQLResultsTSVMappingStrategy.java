/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.resultio.text.tsv;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.impl.ListBindingSet;
import org.eclipse.rdf4j.query.resultio.text.SPARQLResultsXSVMappingStrategy;
import org.eclipse.rdf4j.rio.helpers.NTriplesUtil;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

/**
 * Implements a {@link com.opencsv.bean.MappingStrategy} to allow opencsv to work in parallel. This is where the input
 * is converted into {@link org.eclipse.rdf4j.query.BindingSet}s.
 *
 * @author Andrew Rucker Jones
 */
public class SPARQLResultsTSVMappingStrategy extends SPARQLResultsXSVMappingStrategy {

	public SPARQLResultsTSVMappingStrategy(ValueFactory valueFactory) {
		super(valueFactory);
	}

	@Override
	public void captureHeader(CSVReader reader) throws IOException {
		try {
			// header is mandatory in SPARQL TSV
			bindingNames = Stream.of(reader.readNext())
					.map(s -> StringUtils.removeStart(s, "?"))
					.collect(Collectors.toList());
		} catch (CsvValidationException ex) {
			throw new IOException(ex);
		}
	}

	@Override
	public BindingSet populateNewBean(String[] line) {
		// process solution
		List<Value> values = new ArrayList<>(line.length);
		for (String valueString : line) {
			values.add(parseValue(valueString));
		}
		return new ListBindingSet(bindingNames, values.toArray(new Value[values.size()]));
	}

	protected Value parseValue(String valueString) {
		Value v = null;
		if (valueString.startsWith("<<")) {
			v = NTriplesUtil.parseTriple(valueString, valueFactory);
		} else if (valueString.startsWith("_:")) {
			v = valueFactory.createBNode(valueString.substring(2));
		} else if (valueString.startsWith("<") && valueString.endsWith(">")) {
			try {
				v = valueFactory.createIRI(valueString.substring(1, valueString.length() - 1));
			} catch (IllegalArgumentException e) {
				v = valueFactory.createLiteral(valueString);
			}
		} else if (valueString.startsWith("\"")) {
			v = parseLiteral(valueString);
		} else if (!"".equals(valueString)) {
			if (numberPattern.matcher(valueString).matches()) {
				v = parseNumberPatternMatch(valueString);
			} else {
				v = valueFactory.createLiteral(valueString);
			}
		}
		return v;
	}

	/**
	 * Parses a literal, creates an object for it and returns this object.
	 *
	 * @param literal The literal to parse.
	 * @return An object representing the parsed literal.
	 * @throws IllegalArgumentException If the supplied literal could not be parsed correctly.
	 */
	protected Literal parseLiteral(String literal) throws IllegalArgumentException {
		if (literal.startsWith("\"")) {
			// Find string separation points
			int endLabelIdx = findEndOfLabel(literal);

			if (endLabelIdx != -1) {
				int startLangIdx = literal.indexOf('@', endLabelIdx);
				int startDtIdx = literal.indexOf("^^", endLabelIdx);

				if (startLangIdx != -1 && startDtIdx != -1) {
					throw new IllegalArgumentException("Literals can not have both a language and a datatype");
				}

				// Get label
				String label = literal.substring(1, endLabelIdx);
				label = decodeString(label);

				if (startLangIdx != -1) {
					// Get language
					String language = literal.substring(startLangIdx + 1);
					return valueFactory.createLiteral(label, language);
				} else if (startDtIdx != -1) {
					// Get datatype
					String datatype = literal.substring(startDtIdx + 2);
					datatype = datatype.substring(1, datatype.length() - 1);
					IRI dtURI = valueFactory.createIRI(datatype);
					return valueFactory.createLiteral(label, dtURI);
				} else {
					return valueFactory.createLiteral(label);
				}
			}
		}

		throw new IllegalArgumentException("Not a legal literal: " + literal);
	}

	/**
	 * Finds the end of the label in a literal string.
	 *
	 * @return The index of the double quote ending the label.
	 */
	private int findEndOfLabel(String literal) {
		// we just look for the last occurrence of a double quote
		return literal.lastIndexOf('"');
	}

	/**
	 * Decodes an encoded Turtle string. Any \-escape sequences are substituted with their decoded value.
	 *
	 * @param s An encoded Turtle string.
	 * @return The unencoded string.
	 * @exception IllegalArgumentException If the supplied string is not a correctly encoded Turtle string.
	 **/
	protected static String decodeString(String s) {
		int backSlashIdx = s.indexOf('\\');

		if (backSlashIdx == -1) {
			// No escaped characters found
			return s;
		}

		int startIdx = 0;
		int sLength = s.length();
		StringBuilder sb = new StringBuilder(sLength);

		while (backSlashIdx != -1) {
			sb.append(s.substring(startIdx, backSlashIdx));

			if (backSlashIdx + 1 >= sLength) {
				throw new IllegalArgumentException("Unescaped backslash in: " + s);
			}

			char c = s.charAt(backSlashIdx + 1);

			switch (c) {
			case 't':
				sb.append('\t');
				startIdx = backSlashIdx + 2;
				break;
			case 'r':
				sb.append('\r');
				startIdx = backSlashIdx + 2;
				break;
			case 'n':
				sb.append('\n');
				startIdx = backSlashIdx + 2;
				break;
			case '"':
				sb.append('"');
				startIdx = backSlashIdx + 2;
				break;
			case '>':
				sb.append('>');
				startIdx = backSlashIdx + 2;
				break;
			case '\\':
				sb.append('\\');
				startIdx = backSlashIdx + 2;
				break;
			case 'u': {
				// \\uxxxx
				if (backSlashIdx + 5 >= sLength) {
					throw new IllegalArgumentException("Incomplete Unicode escape sequence in: " + s);
				}
				String xx = s.substring(backSlashIdx + 2, backSlashIdx + 6);
				try {
					c = (char) Integer.parseInt(xx, 16);
					sb.append(c);

					startIdx = backSlashIdx + 6;
				} catch (NumberFormatException e) {
					throw new IllegalArgumentException("Illegal Unicode escape sequence '\\u" + xx + "' in: " + s);
				}
				break;
			}
			case 'U': {
				// \\Uxxxxxxxx
				if (backSlashIdx + 9 >= sLength) {
					throw new IllegalArgumentException("Incomplete Unicode escape sequence in: " + s);
				}
				String xx = s.substring(backSlashIdx + 2, backSlashIdx + 10);
				try {
					c = (char) Integer.parseInt(xx, 16);
					sb.append(c);

					startIdx = backSlashIdx + 10;
				} catch (NumberFormatException e) {
					throw new IllegalArgumentException("Illegal Unicode escape sequence '\\U" + xx + "' in: " + s);
				}
				break;
			}
			default:
				throw new IllegalArgumentException("Unescaped backslash in: " + s);
			}

			backSlashIdx = s.indexOf('\\', startIdx);
		}

		sb.append(s.substring(startIdx));

		return sb.toString();
	}
}
