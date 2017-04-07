/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.rdfxml;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.rdf4j.common.xml.XMLUtil;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.Literals;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.helpers.AbstractRDFWriter;
import org.eclipse.rdf4j.rio.helpers.BasicParserSettings;
import org.eclipse.rdf4j.rio.helpers.XMLWriterSettings;

/**
 * An implementation of the RDFWriter interface that writes RDF documents in XML-serialized RDF format.
 */
public class RDFXMLWriter extends AbstractRDFWriter implements RDFWriter {

	/*-----------*
	 * Variables *
	 *-----------*/

	protected Writer writer;

	protected String defaultNamespace;

	protected boolean writingStarted;

	protected boolean headerWritten;

	protected Resource lastWrittenSubject;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new RDFXMLWriter that will write to the supplied OutputStream.
	 * 
	 * @param out
	 *        The OutputStream to write the RDF/XML document to.
	 */
	public RDFXMLWriter(OutputStream out) {
		this(new OutputStreamWriter(out, Charset.forName("UTF-8")));
	}

	/**
	 * Creates a new RDFXMLWriter that will write to the supplied Writer.
	 * 
	 * @param writer
	 *        The Writer to write the RDF/XML document to.
	 */
	public RDFXMLWriter(Writer writer) {
		this.writer = writer;
		namespaceTable = new LinkedHashMap<String, String>();
		writingStarted = false;
		headerWritten = false;
		lastWrittenSubject = null;
	}

	/*---------*
	 * Methods *
	 *---------*/

	public RDFFormat getRDFFormat() {
		return RDFFormat.RDFXML;
	}

	@Override
	public void startRDF()
		throws RDFHandlerException
	{
		if (writingStarted) {
			throw new RDFHandlerException("Document writing has already started");
		}
		writingStarted = true;
	}

	protected void writeHeader()
		throws IOException
	{
		try {
			// This export format needs the RDF namespace to be defined, add a
			// prefix for it if there isn't one yet.
			setNamespace(RDF.PREFIX, RDF.NAMESPACE);

			if (getWriterConfig().get(XMLWriterSettings.INCLUDE_XML_PI)) {
				writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
			}

			if (getWriterConfig().get(XMLWriterSettings.INCLUDE_ROOT_RDF_TAG)) {
				writeStartOfStartTag(RDF.NAMESPACE, "RDF");

				if (defaultNamespace != null) {
					writeNewLine();
					writeIndent();
					writer.write("xmlns=\"");
					writer.write(XMLUtil.escapeDoubleQuotedAttValue(defaultNamespace));
					writer.write("\"");
				}

				for (Map.Entry<String, String> entry : namespaceTable.entrySet()) {
					String name = entry.getKey();
					String prefix = entry.getValue();

					writeNewLine();
					writeIndent();
					writer.write("xmlns:");
					writer.write(prefix);
					writer.write("=\"");
					writer.write(XMLUtil.escapeDoubleQuotedAttValue(name));
					writer.write("\"");
				}

				writeEndOfStartTag();
			}

			writeNewLine();
		}
		finally {
			headerWritten = true;
		}
	}

	public void endRDF()
		throws RDFHandlerException
	{
		if (!writingStarted) {
			throw new RDFHandlerException("Document writing has not yet started");
		}

		try {
			if (!headerWritten) {
				writeHeader();
			}

			flushPendingStatements();

			writeNewLine();

			if (getWriterConfig().get(XMLWriterSettings.INCLUDE_ROOT_RDF_TAG)) {
				writeEndTag(RDF.NAMESPACE, "RDF");
			}

			writer.flush();
		}
		catch (IOException e) {
			throw new RDFHandlerException(e);
		}
		finally {
			writingStarted = false;
			headerWritten = false;
		}
	}

	public void handleNamespace(String prefix, String name) {
		setNamespace(prefix, name);
	}

	protected void setNamespace(String prefix, String name) {
		if (headerWritten) {
			// Header containing namespace declarations has already been written
			return;
		}

		if (prefix.length() == 0) {
			defaultNamespace = name;
			return;
		}

		if (namespaceTable.containsKey(name)) {
			// Namespace is already mapped to a prefix
			return;
		}

		// Try to give the namespace the specified prefix
		boolean isLegalPrefix = XMLUtil.isNCName(prefix);

		if (!isLegalPrefix || namespaceTable.containsValue(prefix)) {
			// Specified prefix is not legal or the prefix is already in use,
			// generate a legal unique prefix
			if (!isLegalPrefix) {
				prefix = "ns";
			}
			int number = 1;
			while (namespaceTable.containsValue(prefix + number)) {
				number++;
			}
			prefix += number;
		}

		namespaceTable.put(name, prefix);
	}

	public void handleStatement(Statement st)
		throws RDFHandlerException
	{
		if (!writingStarted) {
			throw new RDFHandlerException("Document writing has not yet been started");
		}

		Resource subj = st.getSubject();
		IRI pred = st.getPredicate();
		Value obj = st.getObject();

		// Verify that an XML namespace-qualified name can be created for the
		// predicate
		String predString = pred.toString();
		int predSplitIdx = XMLUtil.findURISplitIndex(predString);
		if (predSplitIdx == -1) {
			throw new RDFHandlerException(
					"Unable to create XML namespace-qualified name for predicate: " + predString);
		}

		String predNamespace = predString.substring(0, predSplitIdx);
		String predLocalName = predString.substring(predSplitIdx);

		try {
			if (!headerWritten) {
				writeHeader();
			}

			// SUBJECT
			if (!subj.equals(lastWrittenSubject)) {
				flushPendingStatements();

				// Write new subject:
				writeNewLine();
				writeStartOfStartTag(RDF.NAMESPACE, "Description");
				if (subj instanceof BNode) {
					BNode bNode = (BNode)subj;
					writeAttribute(RDF.NAMESPACE, "nodeID", getValidNodeId(bNode));
				}
				else {
					IRI uri = (IRI)subj;
					writeAttribute(RDF.NAMESPACE, "about", uri.toString());
				}
				writeEndOfStartTag();
				writeNewLine();

				lastWrittenSubject = subj;
			}

			// PREDICATE
			writeIndent();
			writeStartOfStartTag(predNamespace, predLocalName);

			// OBJECT
			if (obj instanceof Resource) {
				Resource objRes = (Resource)obj;

				if (objRes instanceof BNode) {
					BNode bNode = (BNode)objRes;
					writeAttribute(RDF.NAMESPACE, "nodeID", getValidNodeId(bNode));
				}
				else {
					IRI uri = (IRI)objRes;
					writeAttribute(RDF.NAMESPACE, "resource", uri.toString());
				}

				writeEndOfEmptyTag();
			}
			else if (obj instanceof Literal) {
				Literal objLit = (Literal)obj;
				// datatype attribute
				boolean isXMLLiteral = false;

				// language attribute
				if (Literals.isLanguageLiteral(objLit)) {
					writeAttribute("xml:lang", objLit.getLanguage().get());
				}
				else {
					IRI datatype = objLit.getDatatype();
					// Check if datatype is rdf:XMLLiteral
					isXMLLiteral = datatype.equals(RDF.XMLLITERAL);

					if (isXMLLiteral) {
						writeAttribute(RDF.NAMESPACE, "parseType", "Literal");
					}
					else if (!datatype.equals(XMLSchema.STRING)) {
						writeAttribute(RDF.NAMESPACE, "datatype", datatype.toString());
					}
				}

				writeEndOfStartTag();

				// label
				if (isXMLLiteral) {
					// Write XML literal as plain XML
					writer.write(objLit.getLabel());
				}
				else {
					writeCharacterData(objLit.getLabel());
				}

				writeEndTag(predNamespace, predLocalName);
			}

			writeNewLine();

			// Don't write </rdf:Description> yet, maybe the next statement
			// has the same subject.
		}
		catch (IOException e) {
			throw new RDFHandlerException(e);
		}
	}

	public void handleComment(String comment)
		throws RDFHandlerException
	{
		try {
			if (!headerWritten) {
				writeHeader();
			}

			flushPendingStatements();

			writer.write("<!-- ");
			writer.write(comment);
			writer.write(" -->");
			writeNewLine();
		}
		catch (IOException e) {
			throw new RDFHandlerException(e);
		}
	}

	protected void flushPendingStatements()
		throws IOException, RDFHandlerException
	{
		if (lastWrittenSubject != null) {
			// The last statement still has to be closed:
			writeEndTag(RDF.NAMESPACE, "Description");
			writeNewLine();

			lastWrittenSubject = null;
		}
	}

	protected void writeStartOfStartTag(String namespace, String localName)
		throws IOException
	{
		if (namespace.equals(defaultNamespace)) {
			writer.write("<");
			writer.write(localName);
		}
		else {
			String prefix = namespaceTable.get(namespace);

			// If the prefix was not defined, or the root rdf:RDF tag was not
			// written, then use xmlns=...
			if (prefix == null || !getWriterConfig().get(XMLWriterSettings.INCLUDE_ROOT_RDF_TAG)) {
				writer.write("<");
				writer.write(localName);
				writer.write(" xmlns=\"");
				writer.write(XMLUtil.escapeDoubleQuotedAttValue(namespace));
				writer.write("\"");
			}
			else {
				writer.write("<");
				writer.write(prefix);
				writer.write(":");
				writer.write(localName);
			}
		}
	}

	protected void writeAttribute(String attName, String value)
		throws IOException
	{
		writer.write(" ");
		writer.write(attName);
		writer.write("=\"");
		writer.write(XMLUtil.escapeDoubleQuotedAttValue(value));
		writer.write("\"");
	}

	protected void writeAttribute(String namespace, String attName, String value)
		throws IOException, RDFHandlerException
	{
		// Note: attribute cannot use the default namespace
		String prefix = namespaceTable.get(namespace);

		if (prefix == null) {
			throw new RDFHandlerException(
					"No prefix has been declared for the namespace used in this attribute: " + namespace);
		}

		writer.write(" ");
		writer.write(prefix);
		writer.write(":");
		writer.write(attName);
		writer.write("=\"");
		writer.write(XMLUtil.escapeDoubleQuotedAttValue(value));
		writer.write("\"");
	}

	protected void writeEndOfStartTag()
		throws IOException
	{
		writer.write(">");
	}

	protected void writeEndOfEmptyTag()
		throws IOException
	{
		writer.write("/>");
	}

	protected void writeEndTag(String namespace, String localName)
		throws IOException
	{
		if (namespace.equals(defaultNamespace)) {
			writer.write("</");
			writer.write(localName);
			writer.write(">");
		}
		else {
			writer.write("</");
			String prefix = namespaceTable.get(namespace);
			if (prefix != null) {
				writer.write(prefix);
				writer.write(":");
			}
			writer.write(localName);
			writer.write(">");
		}
	}

	protected void writeCharacterData(String chars)
		throws IOException
	{
		writer.write(XMLUtil.escapeCharacterData(chars));
	}

	protected void writeIndent()
		throws IOException
	{
		writer.write("\t");
	}

	protected void writeNewLine()
		throws IOException
	{
		writer.write("\n");
	}

	/**
	 * Create a syntactically valid node id from the supplied blank node id. This is necessary because RDF/XML
	 * syntax enforces the blank node id is a valid NCName.
	 * 
	 * @param bNode
	 *        a blank node identifier
	 * @return the blank node identifier converted to a form that is a valid NCName.
	 * @see <a href="http://www.w3.org/TR/REC-rdf-syntax/#rdf-id">section 7.2.34 of the RDF/XML Syntax
	 *      specification</a>
	 */
	protected String getValidNodeId(BNode bNode)
		throws IOException
	{
		String validNodeId = bNode.getID();
		if (!XMLUtil.isNCName(validNodeId)) {
			StringBuilder builder = new StringBuilder();
			if (validNodeId.isEmpty()) {
				if (this.getWriterConfig().get(BasicParserSettings.PRESERVE_BNODE_IDS)) {
					throw new IOException(
							"Cannot consistently write blank nodes with empty internal identifiers");
				}
				builder.append("genid-hash-");
				builder.append(Integer.toHexString(System.identityHashCode(bNode)));
			}
			else {
				if (!XMLUtil.isNCNameStartChar(validNodeId.charAt(0))) {
					// prepend legal start char
					builder.append("genid-start-");
					builder.append(Integer.toHexString(validNodeId.charAt(0)));
				}
				else {
					builder.append(validNodeId.charAt(0));
				}

				for (int i = 1; i < validNodeId.length(); i++) {
					// do char-by-char scan and replace illegal chars where
					// necessary.
					if (XMLUtil.isNCNameChar(validNodeId.charAt(i))) {
						builder.append(validNodeId.charAt(i));
					}
					else {
						// replace incompatible char with encoded hex value that will
						// always be alphanumeric.
						builder.append(Integer.toHexString(validNodeId.charAt(i)));
					}
				}
			}
			validNodeId = builder.toString();
		}
		return validNodeId;
	}
}
