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

package org.eclipse.rdf4j.common.xml;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * A utility class offering convenience methods for writing XML. This class takes care of character escaping,
 * identation, etc. This class does not verify that the written data is legal XML. It is the callers responsibility to
 * make sure that elements are properly nested, etc.
 * <h3>Example:</h3>
 * <p>
 * To write the following XML:
 *
 * <pre>
 * &lt;?xml version='1.0' encoding='UTF-8'?&gt;
 * &lt;xml-doc&gt;
 * &lt;foo a="1" b="2&amp;amp;3"/&gt;
 * &lt;bar&gt;Hello World!&lt;/bar&gt;
 * &lt;/xml-doc&gt;
 * </pre>
 * <p>
 * One can use the following code:
 *
 * <pre>
 * XMLWriter xmlWriter = new XMLWriter(myWriter);
 * xmlWriter.setPrettyPrint(true);
 *
 * xmlWriter.startDocument();
 * xmlWriter.startTag(&quot;xml-doc&quot;);
 *
 * xmlWriter.setAttribute(&quot;a&quot;, 1);
 * xmlWriter.setAttribute(&quot;b&quot;, &quot;2&amp;3&quot;);
 * xmlWriter.simpleTag(&quot;foo&quot;);
 *
 * xmlWriter.textTag(&quot;bar&quot;, &quot;Hello World!&quot;);
 *
 * xmlWriter.endTag(&quot;xml-doc&quot;);
 * xmlWriter.endDocument();
 * </pre>
 */
public class XMLWriter {

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
	 * The writer to write the XML to.
	 */
	private final Writer _writer;

	/**
	 * The required character encoding of the written data.
	 */
	private String _charEncoding;

	/**
	 * Flag indicating whether the output should be printed pretty, i.e. adding newlines and indentation.
	 */
	private boolean _prettyPrint = false;

	/**
	 * The current indentation level, i.e. the number of tabs to indent a start or end tag.
	 */
	protected int _indentLevel = 0;

	/**
	 * The string to use for indentation, e.g. a tab or a number of spaces.
	 */
	private String _indentString = "\t";

	/**
	 * A mapping from attribute names to values for the next start tag.
	 */
	private final Map<String, String> _attributes = new LinkedHashMap<>();

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new XMLWriter that will write its data to the supplied Writer. Character encoding issues are left to
	 * the supplier of the Writer.
	 *
	 * @param writer The Writer to write the XML to.
	 */
	public XMLWriter(Writer writer) {
		_writer = writer;
	}

	/**
	 * Creates a new XMLWriter that will write its data to the supplied OutputStream in the default UTF-8 character
	 * encoding.
	 *
	 * @param outputStream The OutputStream to write the XML to.
	 */
	public XMLWriter(OutputStream outputStream) {
		_charEncoding = StandardCharsets.UTF_8.name();
		_writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
	}

	/**
	 * Creates a new XMLWriter that will write its data to the supplied OutputStream in specified character encoding.
	 *
	 * @param outputStream The OutputStream to write the XML to.
	 */
	public XMLWriter(OutputStream outputStream, String charEncoding) throws UnsupportedEncodingException {
		_charEncoding = charEncoding;
		_writer = new OutputStreamWriter(outputStream, _charEncoding);
	}

	/*---------*
	 * Methods *
	 *---------*/

	/**
	 * Enables or disables pretty-printing. If pretty-printing is enabled, the XMLWriter will add newlines and
	 * indentation to the written data. Pretty-printing is disabled by default.
	 *
	 * @param prettyPrint Flag indicating whether pretty-printing should be enabled.
	 */
	public void setPrettyPrint(boolean prettyPrint) {
		_prettyPrint = prettyPrint;
	}

	/**
	 * Checks whether pretty-printing is enabled.
	 *
	 * @return <var>true</var> if pretty-printing is enabled, <var>false</var> otherwise.
	 */
	public boolean prettyPrintEnabled() {
		return _prettyPrint;
	}

	/**
	 * @return the writer
	 */
	public Writer getWriter() {
		return _writer;
	}

	/**
	 * Sets the string that should be used for indentation when pretty-printing is enabled. The default indentation
	 * string is a tab character.
	 *
	 * @param indentString The indentation string, e.g. a tab or a number of spaces.
	 */
	public void setIndentString(String indentString) {
		_indentString = indentString;
	}

	/**
	 * Gets the string used for indentation.
	 *
	 * @return the indentation string.
	 */
	public String getIndentString() {
		return _indentString;
	}

	/**
	 * Writes the XML header for the XML file.
	 *
	 * @throws IOException If an I/O error occurs.
	 */
	public void startDocument() throws IOException {
		_write("<?xml version='1.0'");
		if (_charEncoding != null) {
			_write(" encoding='" + _charEncoding + "'");
		}
		_writeLn("?>");
	}

	/**
	 * Finishes writing and flushes the OutputStream or Writer that this XMLWriter is writing to.
	 */
	public void endDocument() throws IOException {
		getWriter().flush();
	}

	/**
	 * Sets an attribute for the next start tag.
	 *
	 * @param name  The name of the attribute.
	 * @param value The value of the attribute.
	 */
	public void setAttribute(String name, String value) {
		_attributes.put(name, value);
	}

	/**
	 * Sets an attribute for the next start element.
	 *
	 * @param name  The name of the attribute.
	 * @param value The value of the attribute. The integer value will be transformed to a string using the method
	 *              <var>String.valueOf(int)</var>.
	 * @see java.lang.String#valueOf(int)
	 */
	public void setAttribute(String name, int value) {
		setAttribute(name, String.valueOf(value));
	}

	/**
	 * Sets an attribute for the next start element.
	 *
	 * @param name  The name of the attribute.
	 * @param value The value of the attribute. The boolean value will be transformed to a string using the method
	 *              <var>String.valueOf(boolean)</var>.
	 * @see java.lang.String#valueOf(boolean)
	 */
	public void setAttribute(String name, boolean value) {
		setAttribute(name, String.valueOf(value));
	}

	/**
	 * Writes a start tag containing the previously set attributes.
	 *
	 * @param elName The element name.
	 * @see #setAttribute(java.lang.String, java.lang.String)
	 */
	public void startTag(String elName) throws IOException {
		_writeIndent();
		_write("<" + elName);
		_writeAtts();
		_writeLn(">");
		_indentLevel++;
	}

	/**
	 * Writes an end tag.
	 *
	 * @param elName The element name.
	 */
	public void endTag(String elName) throws IOException {
		_indentLevel--;
		_writeIndent();
		_writeLn("</" + elName + ">");
	}

	/**
	 * Writes an 'empty' element, e.g. <var>&lt;foo/&gt;</var>. The tag will contain any previously set attributes.
	 *
	 * @param elName The element name.
	 * @see #setAttribute(java.lang.String, java.lang.String)
	 */
	public void emptyElement(String elName) throws IOException {
		_writeIndent();
		_write("<" + elName);
		_writeAtts();
		_writeLn("/>");
	}

	/**
	 * Writes a link to an XSL stylesheet, using <var>&lt;?xml-stylesheet type='text/xsl' href='url'?&gt;</var>.
	 *
	 * @param url The URL of the stylesheet.
	 */
	public void writeStylesheet(String url) throws IOException {
		_write("<?xml-stylesheet type='text/xsl' href='");
		text(url);
		_writeLn("'?>");
	}

	/**
	 * Writes a start and end tag with the supplied text between them. The start tag will contain any previously set
	 * attributes.
	 *
	 * @param elName The element name.
	 * @param text   The text.
	 * @see #setAttribute(java.lang.String, java.lang.String)
	 */
	public void textElement(String elName, String text) throws IOException {
		_writeIndent();
		_write("<" + elName);
		_writeAtts();
		_write(">");
		text(text);
		_writeLn("</" + elName + ">");
	}

	/**
	 * Writes a start and end tag with the supplied text between them, without the usual escape rules. The start tag
	 * will contain any previously set attributes.
	 *
	 * @param elName The element name.
	 * @param text   The text.
	 * @see #setAttribute(java.lang.String, java.lang.String)
	 */
	public void unescapedTextElement(String elName, String text) throws IOException {
		_writeIndent();
		_write("<" + elName);
		_writeAtts();
		_write(">");
		_write(text);
		_writeLn("</" + elName + ">");
	}

	/**
	 * Writes a start and end tag with the supplied value between them. The start tag will contain any previously set
	 * attributes.
	 *
	 * @param elName The element name.
	 * @param value  The value. The integer value will be transformed to a string using the method
	 *               <var>String.valueOf(int)</var>.
	 * @see java.lang.String#valueOf(int)
	 */
	public void textElement(String elName, int value) throws IOException {
		textElement(elName, String.valueOf(value));
	}

	/**
	 * Writes a start and end tag with the supplied boolean value between them. The start tag will contain any
	 * previously set attributes.
	 *
	 * @param elName The element name.
	 * @param value  The boolean value. The integer value will be transformed to a string using the method
	 *               <var>String.valueOf(boolean)</var>.
	 * @see java.lang.String#valueOf(boolean)
	 */
	public void textElement(String elName, boolean value) throws IOException {
		textElement(elName, String.valueOf(value));
	}

	/**
	 * Writes a piece of text.
	 *
	 * @param text The text.
	 */
	public void text(String text) throws IOException {
		_write(XMLUtil.escapeCharacterData(text));
	}

	/**
	 * Writes a comment.
	 *
	 * @param comment The comment.
	 */
	public void comment(String comment) throws IOException {
		_writeIndent();
		_writeLn("<!-- " + comment + " -->");
	}

	/**
	 * Writes an empty line. A call to this method will be ignored when pretty-printing is disabled.
	 *
	 * @see #setPrettyPrint
	 */
	public void emptyLine() throws IOException {
		_writeLn("");
	}

	/**
	 * Writes any set attributes and clears them afterwards.
	 */
	private void _writeAtts() throws IOException {
		for (Entry<String, String> entry : _attributes.entrySet()) {
			String name = entry.getKey();
			String value = entry.getValue();

			_write(" " + name + "='");
			if (value != null) {
				_write(XMLUtil.escapeSingleQuotedAttValue(value));
			}
			_write("'");
		}

		_attributes.clear();
	}

	/**
	 * Writes a string.
	 */
	protected void _write(String s) throws IOException {
		getWriter().write(s);
	}

	/**
	 * Writes a string followed by a line-separator. The line-separator is not written when pretty-printing is disabled.
	 */
	protected void _writeLn(String s) throws IOException {
		_write(s);
		if (_prettyPrint) {
			_write(LINE_SEPARATOR);
		}
	}

	/**
	 * Writes as much indentation strings as appropriate for the current indentation level. A call to this method is
	 * ignored when pretty-printing is disabled.
	 */
	protected void _writeIndent() throws IOException {
		if (_prettyPrint) {
			for (int i = 0; i < _indentLevel; i++) {
				_write(_indentString);
			}
		}
	}
}
