/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.common.text;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

public class StringUtil {
	private static final char[] IRI_DONT_ESCAPE = new char[] { '_', '~', '.', '-', '!', '$', '&', '\'', '(',
			')', '*', '+', ',', ';', '=', ':', '/', '?', '#', '@', '%', '[', ']' };

	static {
		// sorting array to allow simple binary search for char lookup.
		Arrays.sort(IRI_DONT_ESCAPE);
	}

	/**
	 * The minimum length of initial text.
	 */
	private static final int MIN_INITIAL_TEXT_LENGTH = 3;

	/**
	 * The maximum length of derived initial text.
	 */
	private static final int MAX_INITIAL_TEXT_LENGTH = 250;

	/**
	 * Substitute String "old" by String "new" in String "text" everywhere. This is a static util function that I could
	 * not place anywhere more appropriate. The name of this function is from the good-old awk time.
	 * 
	 * @param olds The String to be substituted.
	 * @param news The String is the new content.
	 * @param text The String in which the substitution is done.
	 * @return The result String containing the substitutions; if no substitutions were made, the result is 'text'.
	 */
	public static String gsub(String olds, String news, String text) {
		if (olds == null || olds.length() == 0) {
			// Nothing to substitute.
			return text;
		}
		if (text == null) {
			return null;
		}

		// Search for any occurences of 'olds'.
		int oldsIndex = text.indexOf(olds);
		if (oldsIndex == -1) {
			// Nothing to substitute.
			return text;
		}

		// We're going to do some substitutions.
		StringBuilder buf = new StringBuilder(text.length());
		int prevIndex = 0;

		while (oldsIndex >= 0) {
			// First, add the text between the previous and the current
			// occurence.
			buf.append(text.substring(prevIndex, oldsIndex));

			// Then add the substition pattern
			buf.append(news);

			// Remember the index for the next loop.
			prevIndex = oldsIndex + olds.length();

			// Search for the next occurence.
			oldsIndex = text.indexOf(olds, prevIndex);
		}

		// Add the part after the last occurence.
		buf.append(text.substring(prevIndex));

		return buf.toString();
	}

	private static String hex(int c) {
		return Integer.toHexString(c).toUpperCase(Locale.US);
	}

	/**
	 * Escapes a string to a (mostly) conforming IRI value and append it to the appendable.
	 * 
	 * Non-ASCII (valid) values can optionally be numerically encoded by setting escapeUnicode to true. Most characters
	 * that are invalid in an IRI - like a white space or control character - are percent-encoded.
	 * 
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
	 * Returns all text occurring after the specified separator character, or the entire string when the separator char
	 * does not occur.
	 * 
	 * @param string        The string of which the substring needs to be determined.
	 * @param separatorChar The character to look for.
	 * @return All text occurring after the separator character, or the entire string when the character does not occur.
	 */
	public static String getAllAfter(String string, char separatorChar) {
		int index = string.indexOf(separatorChar);
		if (index < 0 || index == string.length() - 1) {
			return string;
		} else {
			return string.substring(index + 1);
		}
	}

	/**
	 * Returns all text occurring before the specified separator character, or the entire string when the separator char
	 * does not occur.
	 * 
	 * @param string        The string of which the substring needs to be determined.
	 * @param separatorChar The character to look for.
	 * @return All text occurring before the separator character, or the entire string when the character does not
	 *         occur.
	 */
	public static String getAllBefore(String string, char separatorChar) {
		int index = string.indexOf(separatorChar);
		return index <= 0 ? string : string.substring(0, index - 1);
	}

	/**
	 * Encodes an array of Strings into a single String than can be decoded to the original array using the
	 * corresponding decode method.Useful for e.g.storing an array of Strings as a single entry in a Preferences node.
	 * 
	 * @param array array of strings
	 * @return single string
	 */
	public static String encodeArray(String[] array) {
		StringBuilder buffer = new StringBuilder();
		int nrItems = array.length;

		for (int i = 0; i < nrItems; i++) {
			String item = array[i];
			item = StringUtil.gsub("_", "__", item);
			buffer.append(item);

			if (i < nrItems - 1) {
				buffer.append("_.");
			}
		}

		return buffer.toString();
	}

	/**
	 * Decodes a String generated by encodeArray.
	 * 
	 * @param encodedArray
	 * @return array of strings
	 */
	public static String[] decodeArray(String encodedArray) {
		String[] items = encodedArray.split("_\\.");
		ArrayList<String> list = new ArrayList<>();

		for (int i = 0; i < items.length; i++) {
			String item = items[i];
			item = gsub("__", "_", item);
			if (!item.isEmpty()) {
				list.add(item);
			}
		}

		return list.toArray(new String[list.size()]);
	}

	/**
	 * Derives the initial text from the supplied text.The returned text excludes whitespace and other special
	 * characters and is useful for display purposes (e.g.previews).
	 * 
	 * @param text
	 * @return
	 */
	public static String deriveInitialText(String text) {
		String result = null;

		int startIdx = 0; // index of the first text character
		int endIdx = 0; // index of the first char after the end of the text
		int textLength = text.length();

		while (startIdx < textLength && result == null) {
			startIdx = endIdx;

			// skip until first/next text character
			while (startIdx < textLength && !isInitialTextStartChar(text.charAt(startIdx))) {
				startIdx++;
			}

			// try to find an initial text of a sufficient length
			endIdx = startIdx + 1;
			while (endIdx < textLength && ((endIdx - startIdx) < MAX_INITIAL_TEXT_LENGTH)
					&& isInitialTextChar(text.charAt(endIdx))) {
				endIdx++;
			}

			if (endIdx - startIdx >= MIN_INITIAL_TEXT_LENGTH) {
				// get candidate text. The text is trimmed to remove any spaces
				// at the end. This will prevent texts like "A " to be accepted.
				String candidateText = text.substring(startIdx, endIdx).trim();
				if (!isGarbageText(candidateText)) {
					result = candidateText;
				}
			}
		}

		return result;
	}

	/**
	 * Titles shorter than MIN_TITLE_LENGTH and long titles that don't contain a single space character are considered
	 * to be garbage.
	 * 
	 * @param text
	 * @return true if considered garbage
	 */
	public static boolean isGarbageText(String text) {
		boolean result = false;

		if (text.trim().length() < MIN_INITIAL_TEXT_LENGTH) {
			result = true;
		} else if (text.length() > 30) {
			result = true;

			for (int i = 0; i < text.length(); i++) {
				if (Character.getType(text.charAt(i)) == Character.SPACE_SEPARATOR) {
					result = false;
					break;
				}
			}
		}

		return result;
	}

	/**
	 * Appends the specified character <tt>n</tt> times to the supplied StringBuilder.
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
	 * This method does not create a new string if <tt>text</tt> doesn't start and end with double quotes, the
	 * <tt>text</tt> object itself is returned in that case.
	 * 
	 * @param text The string to remove the double quotes from.
	 * @return The trimmed string, or a reference to <tt>text</tt> if it did not start and end with double quotes.
	 */
	public static String trimDoubleQuotes(String text) {
		int textLength = text.length();

		if (textLength >= 2 && text.charAt(0) == '"' && text.charAt(textLength - 1) == '"') {
			return text.substring(1, textLength - 1);
		}

		return text;
	}

	// A nice overview of Unicode character categories can be found at:
	// http://oss.software.ibm.com/cgi-bin/icu/ub

	private static boolean isInitialTextStartChar(char c) {
		int charType = Character.getType(c);

		return charType == Character.UPPERCASE_LETTER || charType == Character.LOWERCASE_LETTER
				|| charType == Character.TITLECASE_LETTER || charType == Character.MODIFIER_LETTER
				|| charType == Character.OTHER_LETTER || charType == Character.DECIMAL_DIGIT_NUMBER
				|| charType == Character.START_PUNCTUATION || charType == Character.INITIAL_QUOTE_PUNCTUATION;
	}

	private static boolean isInitialTextChar(char c) {
		int charType = Character.getType(c);

		return charType == Character.UPPERCASE_LETTER || charType == Character.LOWERCASE_LETTER
				|| charType == Character.TITLECASE_LETTER || charType == Character.MODIFIER_LETTER
				|| charType == Character.OTHER_LETTER || charType == Character.DECIMAL_DIGIT_NUMBER
				|| charType == Character.SPACE_SEPARATOR || charType == Character.CONNECTOR_PUNCTUATION
				|| charType == Character.DASH_PUNCTUATION || charType == Character.START_PUNCTUATION
				|| charType == Character.END_PUNCTUATION || charType == Character.INITIAL_QUOTE_PUNCTUATION
				|| charType == Character.FINAL_QUOTE_PUNCTUATION || charType == Character.OTHER_PUNCTUATION;
	}

	/**
	 * Concatenate a number of Strings. This implementation uses a StringBuilder.
	 * 
	 * @param strings the String to concatenate
	 * @return a String that is the results of concatenating the input strings.
	 */
	public static String concat(String... strings) {
		// Determine total length of concatenated string to prevent expensive char
		// array copies for growing StringBuilder's internal array
		int totalLength = 0;
		for (String s : strings) {
			totalLength += s.length();
		}

		StringBuilder result = new StringBuilder(totalLength);
		for (String string : strings) {
			result.append(string);
		}

		return result.toString();
	}
}
