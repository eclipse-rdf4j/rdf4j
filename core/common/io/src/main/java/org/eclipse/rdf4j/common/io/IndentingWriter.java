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
package org.eclipse.rdf4j.common.io;

import java.io.IOException;
import java.io.Writer;

/**
 * A writer that adds indentation to written text.
 *
 * @author Arjohn Kampman
 */
public class IndentingWriter extends Writer {

	/*-----------*
	 * Constants *
	 *-----------*/

	/**
	 * The (platform-dependent) line separator.
	 */
	private static final String LINE_SEPARATOR = System.getProperty("line.separator");

	/*-----------*
	 * Variables *
	 *-----------*/

	/**
	 * The underlying writer.
	 */
	protected Writer out;

	/**
	 * The current indentation level, i.e. the number of tabs to indent a start or end tag.
	 */
	protected int indentationLevel = 0;

	/**
	 * The string to use for indentation, e.g. a tab or a number of spaces.
	 */
	private String indentationString = "\t";

	/**
	 * Flag indicating whether indentation has been written for the current line.
	 */
	private boolean indentationWritten = false;

	/**
	 * Number of characters written since the last call to {@link #writeEOL()}
	 */
	private int charactersSinceEOL;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public IndentingWriter(Writer out) {
		this.out = out;
	}

	/*---------*
	 * Methods *
	 *---------*/

	/**
	 * Sets the string that should be used for indentation. The default indentation string is a tab character.
	 *
	 * @param indentString The indentation string, e.g. a tab or a number of spaces.
	 */
	public void setIndentationString(String indentString) {
		this.indentationString = indentString;
	}

	/**
	 * Gets the string used for indentation.
	 *
	 * @return the indentation string.
	 */
	public String getIndentationString() {
		return indentationString;
	}

	/**
	 * Get the indentation level (number of tabs or indentation string).
	 *
	 * @return level as an integer
	 */
	public int getIndentationLevel() {
		return indentationLevel;
	}

	/**
	 * Set indentation level (number of tabs or indentation string).
	 *
	 * @param indentationLevel level as an integer
	 */
	public void setIndentationLevel(int indentationLevel) {
		this.indentationLevel = indentationLevel;
	}

	/**
	 * Get the number of characters read since end-of-line.
	 *
	 * @return number of characters
	 */
	public int getCharactersSinceEOL() {
		return charactersSinceEOL;
	}

	/**
	 * Increase indentation level by 1.
	 */
	public void increaseIndentation() {
		indentationLevel++;
	}

	/**
	 * Decrease indentation level by 1.
	 */
	public void decreaseIndentation() {
		indentationLevel--;
	}

	/**
	 * Writes an end-of-line character sequence and triggers the indentation for the text written on the next line.
	 *
	 * @throws IOException
	 */
	public void writeEOL() throws IOException {
		write(LINE_SEPARATOR);
		indentationWritten = false;
		charactersSinceEOL = 0;
	}

	@Override
	public void close() throws IOException {
		out.close();
	}

	@Override
	public void flush() throws IOException {
		out.flush();
	}

	@Override
	public void write(String str, int off, int len)
			throws IOException {
		if (!indentationWritten) {
			for (int i = 0; i < indentationLevel; i++) {
				out.write(indentationString);
			}

			indentationWritten = true;
		}
		charactersSinceEOL += len;
		out.write(str, off, len);
	}

	@Override
	public void write(char cbuf[], int off, int len) throws IOException {
		if (!indentationWritten) {
			for (int i = 0; i < indentationLevel; i++) {
				out.write(indentationString);
			}

			indentationWritten = true;
		}
		charactersSinceEOL += len;
		out.write(cbuf, off, len);
	}
}
