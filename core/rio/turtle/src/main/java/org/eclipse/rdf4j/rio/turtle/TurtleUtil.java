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
package org.eclipse.rdf4j.rio.turtle;

import java.util.Arrays;

import org.eclipse.rdf4j.common.text.ASCIIUtil;
import org.eclipse.rdf4j.common.text.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility methods for Turtle encoding/decoding.
 *
 * @see <a href="http://www.w3.org/TR/turtle/">Turtle: Terse RDF Triple Language</a>
 */
public class TurtleUtil {

	private static final Logger logger = LoggerFactory.getLogger(TurtleUtil.class);

	public static final char[] LOCAL_ESCAPED_CHARS = new char[] { '_', '~', '.', '-', '!', '$', '&', '\'', '(', ')',
			'*', '+', ',', ';', '=', '/', '?', '#', '@', '%' };

	static {
		// sorting array to allow simple binary search for char lookup.
		Arrays.sort(LOCAL_ESCAPED_CHARS);
	}

	/**
	 * Tries to find an index where the supplied URI can be split into a namespace and a local name that comply with the
	 * serialization constraints of the Turtle format.
	 *
	 * @param uri The URI to split.
	 * @return The index where the supplied URI can be split, or <var>-1</var> if the URI cannot be split.
	 */
	public static int findURISplitIndex(String uri) {
		int uriLength = uri.length();

		int idx = uriLength - 1;

		// Search last character that is not a name character
		for (; idx >= 0; idx--) {
			if (!TurtleUtil.isNameChar(uri.charAt(idx))) {
				// Found a non-name character
				break;
			}
		}

		idx++;

		// Local names need to start with a 'nameStartChar', skip characters
		// that are not nameStartChar's.
		for (; idx < uriLength; idx++) {
			if (TurtleUtil.isNameStartChar(uri.charAt(idx))) {
				break;
			}
		}

		// Last character cannot be a period
		if (!TurtleUtil.isNameEndChar(uri.charAt(uriLength - 1))) {
			return -1;
		}

		if (idx > 0 && idx < uriLength) {
			// A valid split index has been found
			return idx;
		}

		// No valid local name has been found
		return -1;
	}

	/**
	 * Check if the supplied code point represents a whitespace character
	 *
	 * @param codePoint a Unicode code point
	 * @return <code>true</code> if the supplied code point represents a whitespace character, <code>false</code>
	 *         otherwise.
	 */
	public static boolean isWhitespace(int codePoint) {
		// Whitespace character are space, tab, newline and carriage return:
		return codePoint == 0x20 || codePoint == 0x9 || codePoint == 0xA || codePoint == 0xD;
	}

	/**
	 * Check if the supplied code point represents a valid prefixed name base character.
	 * <p>
	 * From Turtle Spec:
	 * <p>
	 * http://www.w3.org/TR/turtle/#grammar-production-PN_CHARS_BASE
	 * <p>
	 * [163s] PN_CHARS_BASE ::= [A-Z] | [a-z] | [#x00C0-#x00D6] | [#x00D8-#x00F6] | [#x00F8-#x02FF] | [#x0370-#x037D] |
	 * [#x037F-#x1FFF] | [#x200C-#x200D] | [#x2070-#x218F] | [#x2C00-#x2FEF] | [#x3001-#xD7FF] | [#xF900-#xFDCF] |
	 * [#xFDF0-#xFFFD] | [#x10000-#xEFFFF]
	 */
	public static boolean isPN_CHARS_BASE(int codePoint) {
		return ASCIIUtil.isLetter(codePoint) || codePoint >= 0x00C0 && codePoint <= 0x00D6
				|| codePoint >= 0x00D8 && codePoint <= 0x00F6 || codePoint >= 0x00F8 && codePoint <= 0x02FF
				|| codePoint >= 0x0370 && codePoint <= 0x037D || codePoint >= 0x037F && codePoint <= 0x1FFF
				|| codePoint >= 0x200C && codePoint <= 0x200D || codePoint >= 0x2070 && codePoint <= 0x218F
				|| codePoint >= 0x2C00 && codePoint <= 0x2FEF || codePoint >= 0x3001 && codePoint <= 0xD7FF
				|| codePoint >= 0xF900 && codePoint <= 0xFDCF || codePoint >= 0xFDF0 && codePoint <= 0xFFFD
				|| codePoint >= 0x10000 && codePoint <= 0xEFFFF;
	}

	/**
	 * Check if the supplied code point represents either a valid prefixed name base character or an underscore.
	 * <p>
	 * From Turtle Spec:
	 * <p>
	 * http://www.w3.org/TR/turtle/#grammar-production-PN_CHARS_U
	 * <p>
	 * [164s] PN_CHARS_U ::= PN_CHARS_BASE | '_'
	 */
	public static boolean isPN_CHARS_U(int codePoint) {
		return isPN_CHARS_BASE(codePoint) || codePoint == '_';
	}

	/**
	 * Check if the supplied code point represents a valid prefixed name character.
	 * <p>
	 * From Turtle Spec:
	 * <p>
	 * http://www.w3.org/TR/turtle/#grammar-production-PN_CHARS
	 * <p>
	 * [166s] PN_CHARS ::= PN_CHARS_U | '-' | [0-9] | #x00B7 | [#x0300-#x036F] | [#x203F-#x2040]
	 */
	public static boolean isPN_CHARS(int codePoint) {
		return isPN_CHARS_U(codePoint) || ASCIIUtil.isNumber(codePoint) || codePoint == '-' || codePoint == 0x00B7
				|| codePoint >= 0x0300 && codePoint <= 0x036F || codePoint >= 0x203F && codePoint <= 0x2040;
	}

	/**
	 * Check if the supplied code point represents a valid prefixed name start character.
	 *
	 * @param codePoint a Unicode code point.
	 * @return <code>true</code> if the supplied code point represents a valid prefixed name start char, false
	 *         otherwise.
	 */
	public static boolean isPrefixStartChar(int codePoint) {
		return isPN_CHARS_BASE(codePoint);
	}

	/**
	 * Check if the supplied code point represents a valid start character for a blank node label.
	 *
	 * @param codePoint a Unicode code point.
	 * @return <code>true</code> if the supplied code point represents a valid blank node label start char,
	 *         <code>false</code> otherwise.
	 */
	public static boolean isBLANK_NODE_LABEL_StartChar(int codePoint) {
		return isPN_CHARS_U(codePoint) || ASCIIUtil.isNumber(codePoint);
	}

	/**
	 * Check if the supplied code point represents a valid blank node label character.
	 *
	 * @param codePoint a Unicode code point.
	 * @return <code>true</code> if the supplied code point represents a valid blank node label char, <code>false</code>
	 *         otherwise.
	 */
	public static boolean isBLANK_NODE_LABEL_Char(int codePoint) {
		return isPN_CHARS(codePoint) || codePoint == '.';
	}

	/**
	 * Check if the supplied code point represents a valid blank node label end character.
	 *
	 * @param codePoint a Unicode code point.
	 * @return <code>true</code> if the supplied code point represents a valid blank node label end char,
	 *         <code>false</code> otherwise.
	 */
	public static boolean isBLANK_NODE_LABEL_EndChar(int codePoint) {
		return isPN_CHARS(codePoint);
	}

	/**
	 * Check if the supplied code point represents a valid name start character.
	 *
	 * @param codePoint a Unicode code point.
	 * @return <code>true</code> if the supplied code point represents a valid name start char, <code>false</code>
	 *         otherwise.
	 */
	public static boolean isNameStartChar(int codePoint) {
		return isPN_CHARS_U(codePoint) || codePoint == ':' || ASCIIUtil.isNumber(codePoint) || codePoint == '\\'
				|| codePoint == '%';
	}

	/**
	 * Check if the supplied code point represents a valid name character.
	 *
	 * @param codePoint a Unicode code point.
	 * @return <code>true</code> if the supplied code point represents a valid name char, <code>false</code> otherwise.
	 */
	public static boolean isNameChar(int codePoint) {
		return isPN_CHARS(codePoint) || codePoint == '.' || codePoint == ':' | codePoint == '\\' || codePoint == '%';
	}

	/**
	 * Check if the supplied code point represents a valid name end character.
	 *
	 * @param codePoint a Unicode code point.
	 * @return <code>true</code> if the supplied code point represents a valid name end char, <code>false</code>
	 *         otherwise.
	 */
	public static boolean isNameEndChar(int codePoint) {
		return isPN_CHARS(codePoint) || codePoint == ':';
	}

	/**
	 * Check if the supplied code point represents a valid local escaped character.
	 *
	 * @param codePoint a Unicode code point.
	 * @return <code>true</code> if the supplied code point represents a valid local escaped char, <code>false</code>
	 *         otherwise.
	 */
	public static boolean isLocalEscapedChar(int codePoint) {
		return Arrays.binarySearch(LOCAL_ESCAPED_CHARS, (char) codePoint) > -1;
	}

	/**
	 * Check if the supplied code point represents a valid prefix character.
	 *
	 * @param codePoint a Unicode code point.
	 * @return <code>true</code> if the supplied code point represents a valid prefix char, <code>false</code>
	 *         otherwise.
	 */
	public static boolean isPrefixChar(int codePoint) {
		return isPN_CHARS_BASE(codePoint) || isPN_CHARS(codePoint) || codePoint == '.';
	}

	/**
	 * Check if the supplied code point represents a valid language tag start character.
	 *
	 * @param codePoint a Unicode code point.
	 * @return <code>true</code> if the supplied code point represents a valid language tag start char,
	 *         <code>false</code> otherwise.
	 */
	public static boolean isLanguageStartChar(int codePoint) {
		return ASCIIUtil.isLetter(codePoint);
	}

	/**
	 * Check if the supplied code point represents a valid language tag character.
	 *
	 * @param codePoint a Unicode code point.
	 * @return <code>true</code> if the supplied code point represents a valid language tag char, <code>false</code>
	 *         otherwise.
	 */
	public static boolean isLanguageChar(int codePoint) {
		return ASCIIUtil.isLetter(codePoint) || ASCIIUtil.isNumber(codePoint) || codePoint == '-';
	}

	/**
	 * Checks if the supplied prefix string is a valid Turtle namespace prefix. From Turtle Spec:
	 * <p>
	 * http://www.w3.org/TR/turtle/#grammar-production-PN_PREFIX
	 * <p>
	 * [167s] PN_PREFIX ::= PN_CHARS_BASE ((PN_CHARS | '.')* PN_CHARS)?
	 *
	 * @param prefix a prefix string.
	 * @return true if the supplied prefix conforms to Turtle grammar rules
	 */
	public static boolean isPN_PREFIX(String prefix) {
		// Empty prefixes are not legal, they should always have a colon
		if (prefix.length() == 0) {
			logger.debug("PN_PREFIX was not valid (empty)");
			return false;
		}

		if (!isPN_CHARS_BASE(prefix.charAt(0))) {
			logger.debug("PN_PREFIX was not valid (start character invalid) i=0 nextchar={} prefix=", prefix.charAt(0),
					prefix);
			return false;
		}

		final int numberOfCodePoints = prefix.codePointCount(0, prefix.length());
		for (int i = 1; i < numberOfCodePoints; i++) {
			final int codePoint = prefix.codePointAt(i);

			if (!isPN_CHARS(codePoint) || (codePoint == '.' && i < (numberOfCodePoints - 1))) {
				logger.debug(
						"PN_PREFIX was not valid (intermediate character invalid) i=" + i + " nextchar={} prefix={}",
						Character.toChars(codePoint), prefix);
				return false;
			}

			// Check if the percent encoding was less than two characters from the
			// end of the prefix, in which case it is invalid
			if (codePoint == '%' && (prefix.length() - i) < 2) {
				logger.debug("PN_PREFIX was not valid (percent encoding) i=" + i + " nextchar={} prefix={}",
						Character.toChars(codePoint), prefix);
				return false;
			}

			if (Character.isHighSurrogate((char) codePoint)) {
				// surrogate pair, skip second member in char sequence.
				i++;
			}
		}

		return true;
	}

	public static boolean isPLX_START(String name) {
		if (name.length() >= 3 && isPERCENT(name.substring(0, 3))) {
			return true;
		}

		if (name.length() >= 2 && isPN_LOCAL_ESC(name.substring(0, 2))) {
			return true;
		}

		return false;
	}

	public static boolean isPERCENT(String name) {
		if (name.length() != 3) {
			return false;
		}

		if (name.charAt(0) != '%') {
			return false;
		}

		if (!ASCIIUtil.isHex(name.charAt(1)) || !ASCIIUtil.isHex(name.charAt(2))) {
			return false;
		}

		return true;
	}

	public static boolean isPLX_INTERNAL(String name) {
		if (name.length() == 3 && isPERCENT(name)) {
			return true;
		}

		if (name.length() == 2 && isPN_LOCAL_ESC(name)) {
			return true;
		}

		return false;
	}

	public static boolean isPN_LOCAL_ESC(String name) {
		if (name.length() != 2) {
			return false;
		}

		if (!name.startsWith("\\")) {
			return false;
		}

		if (!(Arrays.binarySearch(LOCAL_ESCAPED_CHARS, name.charAt(1)) > -1)) {
			return false;
		}

		return true;
	}

	public static boolean isPN_LOCAL(String name) {
		// Empty names are legal
		if (name.length() == 0) {
			return true;
		}

		if (!isPN_CHARS_U(name.charAt(0)) && name.charAt(0) != ':' && !ASCIIUtil.isNumber(name.charAt(0))
				&& !isPLX_START(name)) {
			logger.debug("PN_LOCAL was not valid (start characters invalid) i=" + 0 + " nextchar="
					+ name.charAt(0) + " name=" + name);
			return false;
		}

		if (!isNameStartChar(name.charAt(0))) {
			logger.debug("name was not valid (start character invalid) i=" + 0 + " nextchar=" + name.charAt(0)
					+ " name=" + name);
			return false;
		}

		for (int i = 1; i < name.length(); i++) {
			if (!isNameChar(name.charAt(i))) {
				logger.debug("name was not valid (intermediate character invalid) i=" + i + " nextchar="
						+ name.charAt(i) + " name=" + name);
				return false;
			}

			// Check if the percent encoding was less than two characters from the
			// end of the prefix, in which case it is invalid
			if (name.charAt(i) == '%' && (name.length() - i) < 3) {
				logger.debug("name was not valid (short percent escape) i=" + i + " nextchar=" + name.charAt(i)
						+ " name=" + name);
				return false;
			}
		}

		return true;
	}

	// public static boolean isLegalName(String name) {
	// return isPN_LOCAL(name);
	// }

	/**
	 * Encodes the supplied string for inclusion as a 'normal' string in a Turtle document.
	 *
	 * @param s
	 * @return encoded string
	 */
	public static String encodeString(String s) {
		s = StringUtil.gsub("\\", "\\\\", s);
		s = StringUtil.gsub("\t", "\\t", s);
		s = StringUtil.gsub("\n", "\\n", s);
		s = StringUtil.gsub("\r", "\\r", s);
		s = StringUtil.gsub("\"", "\\\"", s);
		return s;
	}

	/**
	 * Encodes the supplied string for inclusion as a long string in a Turtle document.
	 *
	 * @param s
	 * @return encoded long string
	 */
	public static String encodeLongString(String s) {
		// TODO: not all double quotes need to be escaped. It suffices to encode
		// the ones that form sequences of 3 or more double quotes, and the ones
		// at the end of a string.
		s = StringUtil.gsub("\\", "\\\\", s);
		s = StringUtil.gsub("\"", "\\\"", s);
		return s;
	}

	/**
	 * Encodes the supplied string for inclusion as a (relative) URI in a Turtle document.
	 *
	 * @param s
	 */
	@Deprecated
	public static String encodeURIString(String s) {
		s = StringUtil.gsub("\\", "\\u005C", s);
		s = StringUtil.gsub("\t", "\\u0009", s);
		s = StringUtil.gsub("\n", "\\u000A", s);
		s = StringUtil.gsub("\r", "\\u000D", s);
		s = StringUtil.gsub("\"", "\\u0022", s);
		s = StringUtil.gsub("`", "\\u0060", s);
		s = StringUtil.gsub("^", "\\u005E", s);
		s = StringUtil.gsub("|", "\\u007C", s);
		s = StringUtil.gsub("<", "\\u003C", s);
		s = StringUtil.gsub(">", "\\u003E", s);
		s = StringUtil.gsub(" ", "\\u0020", s);
		return s;
	}

	/**
	 * Decodes an encoded Turtle string. Any \-escape sequences are substituted with their decoded value.
	 *
	 * @param s An encoded Turtle string.
	 * @return The unencoded string.
	 * @exception IllegalArgumentException If the supplied string is not a correctly encoded Turtle string.
	 **/
	public static String decodeString(String s) {
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

			if (c == 't') {
				sb.append('\t');
				startIdx = backSlashIdx + 2;
			} else if (c == 'b') {
				sb.append('\b');
				startIdx = backSlashIdx + 2;
			} else if (c == 'n') {
				sb.append('\n');
				startIdx = backSlashIdx + 2;
			} else if (c == 'r') {
				sb.append('\r');
				startIdx = backSlashIdx + 2;
			} else if (c == 'f') {
				sb.append('\f');
				startIdx = backSlashIdx + 2;
			} else if (c == '"') {
				sb.append('"');
				startIdx = backSlashIdx + 2;
			} else if (c == '\'') {
				sb.append('\'');
				startIdx = backSlashIdx + 2;
			} else if (c == '>') {
				sb.append('>');
				startIdx = backSlashIdx + 2;
			} else if (c == '\\') {
				sb.append('\\');
				startIdx = backSlashIdx + 2;
			} else if (c == 'u') {
				// \\uxxxx
				if (backSlashIdx + 5 >= sLength) {
					throw new IllegalArgumentException("Incomplete Unicode escape sequence in: " + s);
				}
				String xx = s.substring(backSlashIdx + 2, backSlashIdx + 6);

				try {
					final int codePoint = Integer.parseInt(xx, 16);
					sb.append(Character.toChars(codePoint));

					startIdx = backSlashIdx + 6;
				} catch (NumberFormatException e) {
					throw new IllegalArgumentException("Illegal Unicode escape sequence '\\u" + xx + "' in: " + s);
				}
			} else if (c == 'U') {
				// \\Uxxxxxxxx
				if (backSlashIdx + 9 >= sLength) {
					throw new IllegalArgumentException("Incomplete Unicode escape sequence in: " + s);
				}
				String xx = s.substring(backSlashIdx + 2, backSlashIdx + 10);

				try {
					final int codePoint = Integer.parseInt(xx, 16);
					sb.append(Character.toChars(codePoint));

					startIdx = backSlashIdx + 10;
				} catch (NumberFormatException e) {
					throw new IllegalArgumentException("Illegal Unicode escape sequence '\\U" + xx + "' in: " + s);
				}
			} else {
				throw new IllegalArgumentException("Unescaped backslash in: " + s);
			}

			backSlashIdx = s.indexOf('\\', startIdx);
		}

		sb.append(s.substring(startIdx));

		return sb.toString();
	}
}
