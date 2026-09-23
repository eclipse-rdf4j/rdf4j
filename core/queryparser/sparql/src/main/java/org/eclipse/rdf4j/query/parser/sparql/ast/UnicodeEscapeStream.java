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

public class UnicodeEscapeStream extends JavaCharStream {

	/**
	 * The quote character ('\'' or '"') of the string literal we are currently inside, or 0 if we are not inside one.
	 */
	private char stringQuote = 0;

	/**
	 * Whether we are currently inside an IRIREF ("&lt;...&gt;").
	 */
	private boolean inIriRef = false;

	public UnicodeEscapeStream(String string, int tabSize) {
		super(new StringReader(string), 1, 1, string.length());
		setTabSize(tabSize);
	}

	public UnicodeEscapeStream(Reader dstream, int tabSize) {
		super(dstream);
		setTabSize(tabSize);
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

			for (;;) // Read all the backslashes
			{
				if (++bufpos == available) {
					AdjustBuffSize();
				}

				try {
					if ((buffer[bufpos] = c = ReadByte()) != '\\') {
						UpdateLineColumn(c);
						// found a non-backslash char.
						if ((c == 'u' || c == 'U') && ((backSlashCnt & 1) == 1) && inEscapableContext()) {
							if (--bufpos < 0) {
								bufpos = bufsize - 1;
							}

							break;
						}

						// This trailing character will be replayed later via the inBuf fast path (see the top of
						// this method), which never calls trackContext itself. If it closes the string literal or
						// IRIREF we're currently in, that must be tracked here or the stream loses track of it (e.g.
						// a string closed right after a run of backslashes, as in "\\\\"). Only handle the closing
						// case: at top level (e.g. inside a comment, which trackContext knows nothing about) a
						// trailing quote must not be mistaken for the start of a string.
						if (stringQuote != 0 || inIriRef) {
							trackContext(c);
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

			// Here, we have seen an odd number of backslash's followed by a 'u'/'U', while inside a string literal
			// or IRIREF.
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
			char echarLetter = stringQuote != 0 ? echarLetterFor(c) : 0;
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
			trackContext(c);
			return c;
		}
	}

	private void trackContext(char c) {
		if (inIriRef) {
			if (c == '>') {
				inIriRef = false;
			}
			return;
		}

		if (stringQuote != 0) {
			if (c == stringQuote) {
				stringQuote = 0;
			}
			return;
		}

		if (c == '\'' || c == '"') {
			stringQuote = c;
			return;
		}

		if (c == '<') {
			// Look ahead one character: a real IRIREF starts immediately with its content, with no whitespace or
			// operator character following, unlike the '<' relational operator (e.g. "?x < ?y", "?x <= ?y").
			if (++bufpos == available) {
				AdjustBuffSize();
			}
			try {
				char next = buffer[bufpos] = ReadByte();
				// This peeked character will be replayed verbatim by the inBuf fast path above, which never calls
				// UpdateLineColumn itself, so it must be stamped here or all line/column bookkeeping after it drifts.
				UpdateLineColumn(next);
				backup(1);
				if (!Character.isWhitespace(next) && next != '=' && next != '<' && next != '>') {
					inIriRef = true;
				}
			} catch (java.io.IOException e) {
				// EOF right after '<': nothing to track.
			}
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
	 * Codepoint escapes are only resolved while {@link #stringQuote} is set or {@link #inIriRef} is true: outside a
	 * string literal or IRIREF (e.g. in a PN_LOCAL, a variable name, or a prefix) a codepoint escape is not part of the
	 * SPARQL grammar and must be left alone so the lexer rejects it.
	 */
	private boolean inEscapableContext() {
		return stringQuote != 0 || inIriRef;
	}
}
