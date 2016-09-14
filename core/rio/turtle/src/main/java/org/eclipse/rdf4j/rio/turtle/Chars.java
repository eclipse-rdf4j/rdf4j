package org.eclipse.rdf4j.rio.turtle;

/**
 * Utilities that were taken from the Character but changed to be better suited for parsing
 * without additional garbage.
 * @author Nikola Petrov nikola.petrov@ontotext.com
 */
class Chars {
	/**
	 * Appends the characters from codepoint into the string builder. This is the same
	 * as Character#toChars but prevents the additional char array garbage.
	 * @param dst the destination in which to append the characters
	 * @param codePoint the codepoint to be appended
	 */
	public static void append(StringBuilder dst, int codePoint) {
		/*
		 * This code is copy pasted from Character#toChars but doesn't allocate new arrays.
		 */
		if (Character.isBmpCodePoint(codePoint)) {
			dst.append((char)codePoint);
		} else if (Character.isValidCodePoint(codePoint)) {
			char[] result = new char[2];
			// NB: Code copied Character#toSurrogates
			dst.append(Character.highSurrogate(codePoint));
			dst.append(Character.lowSurrogate(codePoint));
		} else {
			throw new IllegalArgumentException();
		}
	}
}
