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

/**
 * Utility methods for ASCII character checking.
 */
public class ASCIIUtil {
	/**
	 * Checks whether the supplied character is a letter or number.
	 *
	 * @param c character
	 * @return true if the character is a letter or a number
	 * @see #isLetter
	 * @see #isNumber
	 */
	public static boolean isLetterOrNumber(int c) {
		return isLetter(c) || isNumber(c);
	}

	/**
	 * Checks whether the supplied character is a letter.
	 *
	 * @param c character
	 * @return true if the character is in the range [a-z] or [A-Z]
	 */
	public static boolean isLetter(int c) {
		return isUpperCaseLetter(c) || isLowerCaseLetter(c);
	}

	/**
	 * Checks whether the supplied character is an upper-case letter.
	 *
	 * @param c character
	 * @return true if the character is in the range [A-Z]
	 */
	public static boolean isUpperCaseLetter(int c) {
		return (c >= 65 && c <= 90); // A - Z
	}

	/**
	 * Checks whether the supplied character is an lower-case letter.
	 *
	 * @param c character
	 * @return true if the character is in the range [a-z]
	 */
	public static boolean isLowerCaseLetter(int c) {
		return (c >= 97 && c <= 122); // a - z
	}

	/**
	 * Checks whether the supplied character is a number.
	 *
	 * @param c character
	 * @return true if the character is in the range [0-9]
	 */
	public static boolean isNumber(int c) {
		return (c >= 48 && c <= 57); // 0 - 9
	}

	/**
	 * Check whether the supplied character is a Hexadecimal character.
	 *
	 * @param c character
	 * @return <code>true</code> if c is a hexadecimal character, <code>false</code> otherwise.
	 */
	public static boolean isHex(int c) {
		return isNumber(c) || isUpperCaseHexLetter(c) || isLowerCaseHexLetter(c);
	}

	private static boolean isUpperCaseHexLetter(int c) {
		return (c >= 65 && c <= 70);
	}

	private static boolean isLowerCaseHexLetter(int c) {
		return (c >= 97 && c <= 102);
	}
}
