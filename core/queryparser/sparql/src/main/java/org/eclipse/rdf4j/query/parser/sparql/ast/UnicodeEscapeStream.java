/*******************************************************************************
 * Copyright (c) 2017 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.parser.sparql.ast;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.BitSet;

public class UnicodeEscapeStream extends JavaCharStream {

	/**
	 * Raw input offsets of the backslashes that start a codepoint escape inside a string literal.
	 */
	private final BitSet stringEscapes = new BitSet();

	/**
	 * Raw input offsets of the backslashes that start a codepoint escape inside an IRIREF.
	 */
	private final BitSet iriEscapes = new BitSet();

	/**
	 * Raw input offset of the character most recently obtained from {@link #ReadByte()}.
	 */
	private int rawPos = -1;

	public UnicodeEscapeStream(String string, int tabSize) {
		super(new StringReader(string), 1, 1, string.length());
		setTabSize(tabSize);
		scan(string);
	}

	public UnicodeEscapeStream(Reader dstream, int tabSize) throws IOException {
		this(readFully(dstream), tabSize);
	}

	private static String readFully(Reader reader) throws IOException {
		StringBuilder sb = new StringBuilder();
		char[] buf = new char[4096];
		int n;
		while ((n = reader.read(buf)) != -1) {
			sb.append(buf, 0, n);
		}
		return sb.toString();
	}

	@Override
	protected char ReadByte() throws IOException {
		char c = super.ReadByte();
		rawPos++;
		return c;
	}

	@Override
	public char readChar() throws IOException {
		if (inBuf > 0) {
			--inBuf;

			if (++bufpos == bufsize) {
				bufpos = 0;
			}

			return buffer[bufpos];
		}

		char c;

		if (++bufpos == available) {
			AdjustBuffSize();
		}

		if ((buffer[bufpos] = c = ReadByte()) == '\\') {
			UpdateLineColumn(c);

			int backSlashCnt = 1;
			boolean inString;

			for (;;) // Read all the backslashes
			{
				if (++bufpos == available) {
					AdjustBuffSize();
				}

				try {
					if ((buffer[bufpos] = c = ReadByte()) != '\\') {
						UpdateLineColumn(c);
						// found a non-backslash char.
						// rawPos - 1 is the raw offset of the last backslash of the run, the one directly before c.
						if ((c == 'u' || c == 'U') && isCodepointEscape(rawPos - 1)) {
							inString = stringEscapes.get(rawPos - 1);
							if (--bufpos < 0) {
								bufpos = bufsize - 1;
							}

							break;
						}

						backup(backSlashCnt);
						return '\\';
					}
				} catch (java.io.IOException e) {
					// We are returning one backslash so we should only backup (count-1)
					if (backSlashCnt > 1) {
						backup(backSlashCnt - 1);
					}

					return '\\';
				}

				UpdateLineColumn(c);
				backSlashCnt++;
			}

			// Here, we have seen a backslash followed by a 'u'/'U' that the scan identified as a codepoint escape
			// inside a
			// string literal or IRIREF.
			try {
				if (c == 'u') {
					buffer[bufpos] = c = (char) (hexval(ReadByte()) << 12 | hexval(ReadByte()) << 8
							| hexval(ReadByte()) << 4 | hexval(ReadByte()));
					column += 4;
					if (Character.isSurrogate(c)) {
						// A bare 4-hex-digit escape represents a single BMP character; a lone or misordered
						// surrogate code unit isn't a valid character on its own (an 8-hex-digit escape is required
						// for characters outside the BMP).
						throw new TokenMgrError(
								"Invalid surrogate codepoint escape at line " + line + " column " + column + ".",
								TokenMgrError.LEXICAL_ERROR);
					}
				} else if (c == 'U') {
					String hex = new String(new char[] { ReadByte(), ReadByte(), ReadByte(), ReadByte(), ReadByte(),
							ReadByte(), ReadByte(), ReadByte() });
					int cp = Integer.parseInt(hex, 16);
					char[] chrs = Character.toChars(cp); // length of 1 or 2
					if (chrs.length == 1 && Character.isSurrogate(chrs[0])) {
						throw new TokenMgrError(
								"Invalid surrogate codepoint escape at line " + line + " column " + column + ".",
								TokenMgrError.LEXICAL_ERROR);
					}
					buffer[bufpos] = c = chrs[0];
					if (chrs.length > 1) {
						if (++bufpos == available) {
							AdjustBuffSize();
						}
						buffer[bufpos] = chrs[1];
						UpdateLineColumn(c);
						backup(1);
					}
					column += hex.length();
				}
			} catch (java.io.IOException | IllegalArgumentException e) {
				throw new Error("Invalid escape character at line " + line + " column " + column + ".", e);
			}

			// A character resolved from a codepoint escape is data, not a fresh escape introducer: it must never be
			// re-interpreted by the ECHAR grammar (or by later backslash-unescaping), and it must never accidentally
			// end the enclosing literal (e.g. a resolved '"' closing the string it's inside). Inside a string
			// literal (not an IRIREF, which has no escape mechanism of its own) we guarantee this by re-encoding the
			// handful of characters that are unsafe to emit raw as the equivalent ECHAR pair instead.
			char echarLetter = inString ? echarLetterFor(c) : 0;
			if (echarLetter != 0) {
				buffer[bufpos] = '\\';
				if (++bufpos == available) {
					AdjustBuffSize();
				}
				buffer[bufpos] = echarLetter;
				backup(backSlashCnt);
				return '\\';
			}

			if (backSlashCnt == 1) {
				return c;
			} else {
				backup(backSlashCnt - 1);
				return '\\';
			}
		} else {
			UpdateLineColumn(c);
			return c;
		}
	}

	private static char echarLetterFor(char c) {
		return switch (c) {
		case '\n' -> 'n';
		case '\r' -> 'r';
		case '"' -> '"';
		case '\'' -> '\'';
		case '\\' -> '\\';
		default -> 0;
		};
	}

	/**
	 * Whether the backslash at the given raw input offset starts a codepoint escape that must be resolved, i.e. one
	 * inside a string literal or an IRIREF. Anywhere else (a PN_LOCAL, a variable name, a prefix, a comment) a
	 * codepoint escape is not part of the SPARQL grammar and is left alone, so the lexer rejects it or skips it with
	 * the comment.
	 */
	private boolean isCodepointEscape(int rawOffset) {
		return rawOffset >= 0 && (stringEscapes.get(rawOffset) || iriEscapes.get(rawOffset));
	}

	/**
	 * Scans the raw query text the way the SPARQL lexer tokenizes it (comments, the four string literal forms with
	 * their ECHARs, and IRIREFs) and records which backslashes start a codepoint escape inside a string literal or an
	 * IRIREF. Doing this up front, on the raw text, means every escape is classified exactly once and independently of
	 * any lookahead or replay done by the char stream.
	 */
	private void scan(String s) {
		int n = s.length();
		int i = 0;
		while (i < n) {
			char c = s.charAt(i);
			switch (c) {
			case '#':
				// comment, up to the end of the line
				while (i < n && s.charAt(i) != '\n' && s.charAt(i) != '\r') {
					i++;
				}
				break;
			case '"':
			case '\'':
				i = scanString(s, i);
				break;
			case '<':
				i = scanIriRef(s, i);
				break;
			case '\\':
				// outside string literals and IRIREFs a backslash can only be a PN_LOCAL_ESC, whose escaped character
				// (e.g. a quote or '#') must not be mistaken for a delimiter
				i += 2;
				break;
			default:
				i++;
			}
		}
	}

	/**
	 * Scans the string literal starting at the given quote and returns the offset just past it.
	 */
	private int scanString(String s, int start) {
		int n = s.length();
		char quote = s.charAt(start);
		boolean isLong = start + 2 < n && s.charAt(start + 1) == quote && s.charAt(start + 2) == quote;
		int i = start + (isLong ? 3 : 1);
		while (i < n) {
			char c = s.charAt(i);
			if (c == '\\') {
				if (i + 1 < n && (s.charAt(i + 1) == 'u' || s.charAt(i + 1) == 'U')) {
					stringEscapes.set(i);
				}
				// a codepoint escape or an ECHAR: either way the escaped character is content, not a delimiter
				i += 2;
			} else if (c == quote) {
				if (!isLong) {
					return i + 1;
				}
				// a long string ends at the first run of three quotes; one or two quotes are content
				if (i + 2 < n && s.charAt(i + 1) == quote && s.charAt(i + 2) == quote) {
					return i + 3;
				}
				i++;
			} else if (!isLong && (c == '\n' || c == '\r')) {
				// unterminated short string: leave it to the lexer to report
				return i;
			} else {
				i++;
			}
		}
		return n;
	}

	/**
	 * If an IRIREF starts at the given '&lt;', records its codepoint escapes and returns the offset just past its
	 * closing '&gt;'. Otherwise the '&lt;' is an operator ("&lt;", "&lt;=", "&lt;&lt;") and the offset just past it is
	 * returned. Like the lexer's longest match, an IRIREF wins whenever one can be completed.
	 */
	private int scanIriRef(String s, int start) {
		int n = s.length();
		int i = start + 1;
		while (i < n) {
			char c = s.charAt(i);
			if (c == '>') {
				return i + 1;
			}
			if (c == '\\') {
				int length = codepointEscapeLength(s, i);
				if (length == 0) {
					break;
				}
				iriEscapes.set(i);
				i += length;
			} else if (c <= 0x20 || "<>\"{}|^`".indexOf(c) >= 0) {
				break;
			} else {
				i++;
			}
		}
		iriEscapes.clear(start, i);
		return start + 1;
	}

	/**
	 * The length of the well-formed codepoint escape (backslash, 'u' and 4 hex digits, or backslash, 'U' and 8 hex
	 * digits) at the given offset, or 0 if there is none.
	 */
	private static int codepointEscapeLength(String s, int i) {
		if (i + 1 >= s.length()) {
			return 0;
		}
		int length = switch (s.charAt(i + 1)) {
		case 'u' -> 6;
		case 'U' -> 10;
		default -> 0;
		};
		if (length == 0 || i + length > s.length()) {
			return 0;
		}
		for (int j = i + 2; j < i + length; j++) {
			if (Character.digit(s.charAt(j), 16) < 0) {
				return 0;
			}
		}
		return length;
	}
}
