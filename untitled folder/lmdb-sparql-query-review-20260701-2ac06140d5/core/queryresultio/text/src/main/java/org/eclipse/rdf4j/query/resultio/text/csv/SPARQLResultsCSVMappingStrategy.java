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
package org.eclipse.rdf4j.query.resultio.text.csv;

import static org.eclipse.rdf4j.rio.helpers.NTriplesUtil.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.rdf4j.model.*;
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
public class SPARQLResultsCSVMappingStrategy extends SPARQLResultsXSVMappingStrategy {

	// CSV IRI is not wrapped in <>
	private static final String HEX = "[0-9A-Fa-f]";
	private static final String UCHAR = "(?:\\\\u" + HEX + "{4}|\\\\U" + HEX + "{8})";
	private static final String IRI_CSV = "(?:[^\u0000-\u0020<>\"{}|^`\\\\]|" + UCHAR + ")*";
	private static final Pattern IRI_CSV_PATTERN = Pattern.compile(IRI_CSV);
	private static final Pattern NUMBER_PATTER = Pattern.compile("^[-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?");

	public SPARQLResultsCSVMappingStrategy(ValueFactory valueFactory) {
		super(valueFactory);
	}

	@Override
	public void captureHeader(CSVReader reader) throws IOException {
		try {
			bindingNames = Arrays.asList(reader.readNext());
		} catch (CsvValidationException ex) {
			throw new IOException(ex);
		}
	}

	@Override
	public BindingSet populateNewBean(String[] line) {
		List<Value> values = new ArrayList<>(line.length);
		for (String valueString : line) {
			Value v = null;
			if (valueString.startsWith("<<(")) {
				v = parseTripleTerm(valueString);
			} else if (valueString.startsWith("_:")) {
				v = valueFactory.createBNode(valueString.substring(2));
			} else if (!"".equals(valueString)) {
				if (numberPattern.matcher(valueString).matches()) {
					v = parseNumberPatternMatch(valueString);
				} else {
					try {
						v = valueFactory.createIRI(valueString);
					} catch (IllegalArgumentException e) {
						v = valueFactory.createLiteral(valueString);
					}
				}
			}
			values.add(v);
		}
		return new ListBindingSet(bindingNames, values.toArray(new Value[values.size()]));
	}

	Value parseTripleTerm(String valueString) {
		TripleMatch tripleMatch = parseTripleTerm(new TripleTermParseState(valueString), valueString);
		Value v = tripleMatch.getTripleTerm();
		if (tripleMatch.getLength() != valueString.length()) {
			throw new IllegalArgumentException("Not a valid triple term: " + valueString);
		}
		return v;
	}

	private TripleMatch parseTripleTerm(TripleTermParseState state, String valueStr) {
		if (state.remainingStr.startsWith("<<(")) {
			state.advanceParser(3);
			state.skipWhiteSpaces();

			Resource subject = null;
			IRI predicate = null;
			Value object = null;

			for (int i = 0; i < 3; i++) {
				Value v = null;
				if (state.remainingStr.startsWith("_:")) {
					v = parseBNode(state);
				} else if (state.remainingStr.startsWith("<<(")) {
					v = parseInnerTriple(state, valueStr);
				} else if (state.remainingStr.startsWith("\"")) {
					// Consume the opening quote
					state.advanceParser(1);
					v = parseNumber(state);
					if (v != null) {
						// Consume the closing quote
						state.advanceParser(1);
					} else {
						v = tryToParseIRI(state);
						if (v != null) {
							// Consume the closing quote
							state.advanceParser(1);
						} else {
							v = parseLiteral(state);
						}
					}
				} else if (!state.remainingStr.isEmpty()) {
					// Parse an IRI that is not quoted
					v = tryToParseIRI(state);
				}

				if (i == 0) {
					if (!(v instanceof Resource)) {
						throw new IllegalArgumentException("Not a valid triple term: " + valueStr);
					}
					subject = (Resource) v;
				} else if (i == 1) {
					if (!(v instanceof IRI)) {
						throw new IllegalArgumentException("Not a valid triple term: " + valueStr);
					}
					predicate = (org.eclipse.rdf4j.model.IRI) v;
				} else if (i == 2) {
					if (v == null) {
						throw new IllegalArgumentException("Not a valid triple term: " + valueStr);
					}
					object = v;
				}
				state.skipWhiteSpaces();
			}

			if (state.remainingStr.endsWith(")>>")) {
				state.advanceParser(3);
			}
			return new NTriplesUtil.TripleMatch(
					valueFactory.createTripleTerm(subject, predicate, object),
					state.parsedOffset);
		}
		throw new IllegalArgumentException("Not a valid triple term: " + valueStr);
	}

	private Value parseBNode(TripleTermParseState state) {
		Matcher bNodeMatcher = BNODE_PATTERN.matcher(state.remainingStr);
		if (bNodeMatcher.find() && bNodeMatcher.start() == 0) {
			String value = bNodeMatcher.group();
			Value v = valueFactory.createBNode(value.substring(2));
			state.advanceParser(bNodeMatcher.end());
			return v;
		}
		return null;
	}

	private Value parseInnerTriple(TripleTermParseState state, String original) {
		TripleMatch tm = parseTripleTerm(state, original);
		return tm.getTripleTerm();
	}

	private Value parseNumber(TripleTermParseState state) {
		Matcher numberMatcher = NUMBER_PATTER.matcher(state.remainingStr);
		if (numberMatcher.find() && numberMatcher.start() == 0) {
			String value = numberMatcher.group();
			state.advanceParser(numberMatcher.end());
			return parseNumberPatternMatch(value);
		}
		return null;
	}

	private Value tryToParseIRI(TripleTermParseState state) {
		Value v = null;
		Matcher iriMatcher = IRI_CSV_PATTERN.matcher(state.remainingStr);
		if (iriMatcher.find() && iriMatcher.start() == 0) {
			String value = iriMatcher.group();
			try {
				v = valueFactory.createIRI(value);
				state.advanceParser(iriMatcher.end());
			} catch (IllegalArgumentException e) {
				// Ignore error, we will try to parse to literal
			}
		}
		return v;
	}

	private Value parseLiteral(TripleTermParseState state) {
		int endOfLiteral = findEndOfLiteral(state.remainingStr);
		if (endOfLiteral != -1) {
			String label = state.remainingStr.substring(0, endOfLiteral - 1);
			// Unescape CSV quotes
			label = label.replace("\"\"", "\"");
			state.advanceParser(endOfLiteral);
			return valueFactory.createLiteral(label);
		}
		return null;
	}

	private int findEndOfLiteral(String str) {
		int i = 0;

		while (i < str.length()) {
			char c = str.charAt(i);
			if (c == '"') {
				// Escaped quote ("")
				if (i + 1 < str.length() && str.charAt(i + 1) == '"') {
					i += 2;
				} else {
					// Position after closing quote
					return i + 1;
				}
			} else {
				i++;
			}
		}
		return -1; // No closing quote found
	}

	private static class TripleTermParseState {
		String remainingStr;
		int parsedOffset;

		TripleTermParseState(String tripleStr) {
			remainingStr = tripleStr;
			parsedOffset = 0;
		}

		void advanceParser(int n) {
			remainingStr = remainingStr.substring(n);
			parsedOffset += n;
		}

		void skipWhiteSpaces() {
			while (!remainingStr.isEmpty() && Character.isWhitespace(remainingStr.charAt(0))) {
				advanceParser(1);
			}
		}
	}
}
