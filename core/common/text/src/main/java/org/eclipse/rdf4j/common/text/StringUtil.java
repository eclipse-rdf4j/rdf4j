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

package org.eclipse.rdf4j.common.text;

import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;

public class StringUtil {
	private static final char[] IRI_DONT_ESCAPE = new char[] { '_', '~', '.', '-', '!', '$', '&', '\'', '(',
			')', '*', '+', ',', ';', '=', ':', '/', '?', '#', '@', '%', '[', ']' };

	static {
		// sorting array to allow simple binary search for char lookup.
		Arrays.sort(IRI_DONT_ESCAPE);
	}

	private static String hex(int c) {
		return Integer.toHexString(c).toUpperCase(Locale.US);
	}

	/**
	 * Escapes a string to a (mostly) conforming IRI value and append it to the appendable.
	 * <p>
	 * Non-ASCII (valid) values can optionally be numerically encoded by setting escapeUnicode to true. Most characters
	 * that are invalid in an IRI - like a white space or control character - are percent-encoded.
	 * <p>
	 * This is slightly faster than {@link org.eclipse.rdf4j.common.net.ParsedIRI#create(String)} for valid IRI (without
	 * percents) and much faster for IRI with invalid (percent-encoded) characters, though it is less accurate.
	 *
	 * @param str
	 * @param appendable
	 * @param escapeUnicode escape non-ASCII values numerically
	 * @throws IOException
	 */
	public static void simpleEscapeIRI(String str, Appendable appendable, boolean escapeUnicode) throws IOException {
		int strlen = str.length();

		for (int i = 0; i < strlen; i++) {
			char c = str.charAt(i);

			if (ASCIIUtil.isLetterOrNumber(c)) {
				appendable.append(c);
			} else if (c < 0xA0) {
				if (Arrays.binarySearch(IRI_DONT_ESCAPE, c) > -1) {
					appendable.append(c);
				} else {
					appendable.append('%').append(hex(c));
				}
			} else {
				if (escapeUnicode) {
					if (c <= 0xFF) {
						appendable.append("\\u00").append(hex(c));
					} else if (c <= 0x0FFF) {
						appendable.append("\\u0").append(hex(c));
					} else {
						if (Character.isSurrogate(c) && (i < strlen - 1)) {
							// U+10000 - U+10FFFF
							int code = str.codePointAt(i);
							i++;
							appendable.append("\\U000").append(hex(code));
						} else {
							appendable.append("\\u").append(hex(c));
						}
					}
				} else {
					appendable.append(c);
				}
			}
		}
	}

	/**
	 * Appends the specified character <var>n</var> times to the supplied StringBuilder.
	 *
	 * @param c  The character to append.
	 * @param n  The number of times the character should be appended.
	 * @param sb The StringBuilder to append the character(s) to.
	 */
	public static void appendN(char c, int n, StringBuilder sb) {
		for (int i = n; i > 0; i--) {
			sb.append(c);
		}
	}

	/**
	 * Removes the double quote from the start and end of the supplied string if it starts and ends with this character.
	 * This method does not create a new string if <var>text</var> doesn't start and end with double quotes, the
	 * <var>text</var> object itself is returned in that case.
	 *
	 * @param text The string to remove the double quotes from.
	 * @return The trimmed string, or a reference to <var>text</var> if it did not start and end with double quotes.
	 */
	public static String trimDoubleQuotes(String text) {
		int textLength = text.length();

		if (textLength >= 2 && text.charAt(0) == '"' && text.charAt(textLength - 1) == '"') {
			return text.substring(1, textLength - 1);
		}

		return text;
	}
}
