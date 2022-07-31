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
						if ((c == 'u' || c == 'U') && ((backSlashCnt & 1) == 1)) {
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

			// Here, we have seen an odd number of backslash's followed by a 'u'
			try {
				if (c == 'u') {
					buffer[bufpos] = c = (char) (hexval(ReadByte()) << 12 | hexval(ReadByte()) << 8
							| hexval(ReadByte()) << 4 | hexval(ReadByte()));
					column += 4;
				} else if (c == 'U') {
					String hex = new String(new char[] { ReadByte(), ReadByte(), ReadByte(), ReadByte(), ReadByte(),
							ReadByte(), ReadByte(), ReadByte() });
					int cp = Integer.parseInt(hex, 16);
					char[] chrs = Character.toChars(cp); // length of 1 or 2
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
}
