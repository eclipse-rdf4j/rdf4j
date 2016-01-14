/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.turtle;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.eclipse.rdf4j.common.io.IndentingWriter;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.datatypes.XMLDatatypeUtil;
import org.eclipse.rdf4j.model.util.Literals;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.helpers.AbstractRDFWriter;
import org.eclipse.rdf4j.rio.helpers.BasicParserSettings;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;

/**
 * An implementation of the RDFWriter interface that writes RDF documents in
 * Turtle format. The Turtle format is defined in <a
 * href="http://www.dajobe.org/2004/01/turtle/">in this document</a>.
 */
public class TurtleWriter extends AbstractRDFWriter implements RDFWriter {

	/*-----------*
	 * Variables *
	 *-----------*/

	protected IndentingWriter writer;

	protected boolean writingStarted;

	/**
	 * Flag indicating whether the last written statement has been closed.
	 */
	protected boolean statementClosed;

	protected Resource lastWrittenSubject;

	protected IRI lastWrittenPredicate;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new TurtleWriter that will write to the supplied OutputStream.
	 * 
	 * @param out
	 *        The OutputStream to write the Turtle document to.
	 */
	public TurtleWriter(OutputStream out) {
		this(new OutputStreamWriter(out, Charset.forName("UTF-8")));
	}

	/**
	 * Creates a new TurtleWriter that will write to the supplied Writer.
	 * 
	 * @param writer
	 *        The Writer to write the Turtle document to.
	 */
	public TurtleWriter(Writer writer) {
		this.writer = new IndentingWriter(writer);
		namespaceTable = new LinkedHashMap<String, String>();
		writingStarted = false;
		statementClosed = true;
		lastWrittenSubject = null;
		lastWrittenPredicate = null;
	}

	/*---------*
	 * Methods *
	 *---------*/

	public RDFFormat getRDFFormat() {
		return RDFFormat.TURTLE;
	}

	public void startRDF()
		throws RDFHandlerException
	{
		if (writingStarted) {
			throw new RuntimeException("Document writing has already started");
		}

		writingStarted = true;

		try {
			// Write namespace declarations
			for (Map.Entry<String, String> entry : namespaceTable.entrySet()) {
				String name = entry.getKey();
				String prefix = entry.getValue();

				writeNamespace(prefix, name);
			}

			if (!namespaceTable.isEmpty()) {
				writer.writeEOL();
			}
		}
		catch (IOException e) {
			throw new RDFHandlerException(e);
		}
	}

	public void endRDF()
		throws RDFHandlerException
	{
		if (!writingStarted) {
			throw new RuntimeException("Document writing has not yet started");
		}

		try {
			closePreviousStatement();
			writer.flush();
		}
		catch (IOException e) {
			throw new RDFHandlerException(e);
		}
		finally {
			writingStarted = false;
		}
	}

	public void handleNamespace(String prefix, String name)
		throws RDFHandlerException
	{
		try {
			if (!namespaceTable.containsKey(name)) {
				// Namespace not yet mapped to a prefix, try to give it the
				// specified prefix

				boolean isLegalPrefix = prefix.length() == 0 || TurtleUtil.isPN_PREFIX(prefix);

				if (!isLegalPrefix || namespaceTable.containsValue(prefix)) {
					// Specified prefix is not legal or the prefix is already in use,
					// generate a legal unique prefix

					if (prefix.length() == 0 || !isLegalPrefix) {
						prefix = "ns";
					}

					int number = 1;

					while (namespaceTable.containsValue(prefix + number)) {
						number++;
					}

					prefix += number;
				}

				namespaceTable.put(name, prefix);

				if (writingStarted) {
					closePreviousStatement();

					writeNamespace(prefix, name);
				}
			}
		}
		catch (IOException e) {
			throw new RDFHandlerException(e);
		}
	}

	public void handleStatement(Statement st)
		throws RDFHandlerException
	{
		if (!writingStarted) {
			throw new RuntimeException("Document writing has not yet been started");
		}

		Resource subj = st.getSubject();
		IRI pred = st.getPredicate();
		Value obj = st.getObject();

		try {
			if (subj.equals(lastWrittenSubject)) {
				if (pred.equals(lastWrittenPredicate)) {
					// Identical subject and predicate
					writer.write(" , ");
				}
				else {
					// Identical subject, new predicate
					writer.write(" ;");
					writer.writeEOL();

					// Write new predicate
					writePredicate(pred);
					writer.write(" ");
					lastWrittenPredicate = pred;
				}
			}
			else {
				// New subject
				closePreviousStatement();

				// Write new subject:
				writer.writeEOL();
				writeResource(subj);
				writer.write(" ");
				lastWrittenSubject = subj;

				// Write new predicate
				writePredicate(pred);
				writer.write(" ");
				lastWrittenPredicate = pred;

				statementClosed = false;
				writer.increaseIndentation();
			}

			writeValue(obj);

			// Don't close the line just yet. Maybe the next
			// statement has the same subject and/or predicate.
		}
		catch (IOException e) {
			throw new RDFHandlerException(e);
		}
	}

	public void handleComment(String comment)
		throws RDFHandlerException
	{
		try {
			closePreviousStatement();

			if (comment.indexOf('\r') != -1 || comment.indexOf('\n') != -1) {
				// Comment is not allowed to contain newlines or line feeds.
				// Split comment in individual lines and write comment lines
				// for each of them.
				StringTokenizer st = new StringTokenizer(comment, "\r\n");
				while (st.hasMoreTokens()) {
					writeCommentLine(st.nextToken());
				}
			}
			else {
				writeCommentLine(comment);
			}
		}
		catch (IOException e) {
			throw new RDFHandlerException(e);
		}
	}

	protected void writeCommentLine(String line)
		throws IOException
	{
		writer.write("# ");
		writer.write(line);
		writer.writeEOL();
	}

	protected void writeNamespace(String prefix, String name)
		throws IOException
	{
		writer.write("@prefix ");
		writer.write(prefix);
		writer.write(": <");
		writer.write(TurtleUtil.encodeURIString(name));
		writer.write("> .");
		writer.writeEOL();
	}

	protected void writePredicate(IRI predicate)
		throws IOException
	{
		if (predicate.equals(RDF.TYPE)) {
			// Write short-cut for rdf:type
			writer.write("a");
		}
		else {
			writeURI(predicate);
		}
	}

	protected void writeValue(Value val)
		throws IOException
	{
		if (val instanceof Resource) {
			writeResource((Resource)val);
		}
		else {
			writeLiteral((Literal)val);
		}
	}

	protected void writeResource(Resource res)
		throws IOException
	{
		if (res instanceof IRI) {
			writeURI((IRI)res);
		}
		else {
			writeBNode((BNode)res);
		}
	}

	protected void writeURI(IRI uri)
		throws IOException
	{
		String uriString = uri.toString();

		// Try to find a prefix for the URI's namespace
		String prefix = null;

		int splitIdx = TurtleUtil.findURISplitIndex(uriString);
		if (splitIdx > 0) {
			String namespace = uriString.substring(0, splitIdx);
			prefix = namespaceTable.get(namespace);
		}

		if (prefix != null) {
			// Namespace is mapped to a prefix; write abbreviated URI
			writer.write(prefix);
			writer.write(":");
			writer.write(uriString.substring(splitIdx));
		}
		else {
			// Write full URI
			writer.write("<");
			writer.write(TurtleUtil.encodeURIString(uriString));
			writer.write(">");
		}
	}

	protected void writeBNode(BNode bNode)
		throws IOException
	{
		writer.write("_:");
		String id = bNode.getID();

		if (id.isEmpty()) {
			if (this.getWriterConfig().get(BasicParserSettings.PRESERVE_BNODE_IDS)) {
				throw new IOException("Cannot consistently write blank nodes with empty internal identifiers");
			}
			writer.write("genid-hash-");
			writer.write(Integer.toHexString(System.identityHashCode(bNode)));
		}
		else {
			if (!TurtleUtil.isNameStartChar(id.charAt(0))) {
				writer.write("genid-start-");
				writer.write(Integer.toHexString(id.charAt(0)));
			}
			else {
				writer.write(id.charAt(0));
			}
			for (int i = 1; i < id.length() - 1; i++) {
				if (TurtleUtil.isPN_CHARS(id.charAt(i))) {
					writer.write(id.charAt(i));
				}
				else {
					writer.write(Integer.toHexString(id.charAt(i)));
				}
			}
			if (id.length() > 1) {
				if (!TurtleUtil.isNameEndChar(id.charAt(id.length() - 1))) {
					writer.write(Integer.toHexString(id.charAt(id.length() - 1)));
				}
				else {
					writer.write(id.charAt(id.length() - 1));
				}
			}
		}
	}

	protected void writeLiteral(Literal lit)
		throws IOException
	{
		String label = lit.getLabel();
		IRI datatype = lit.getDatatype();

		if (getWriterConfig().get(BasicWriterSettings.PRETTY_PRINT)) {
			if (XMLSchema.INTEGER.equals(datatype) || XMLSchema.DECIMAL.equals(datatype)
					|| XMLSchema.DOUBLE.equals(datatype) || XMLSchema.BOOLEAN.equals(datatype))
			{
				try {
					writer.write(XMLDatatypeUtil.normalize(label, datatype));
					return; // done
				}
				catch (IllegalArgumentException e) {
					// not a valid numeric typed literal. ignore error and write as
					// quoted string instead.
				}
			}
		}

		if (label.indexOf('\n') != -1 || label.indexOf('\r') != -1 || label.indexOf('\t') != -1) {
			// Write label as long string
			writer.write("\"\"\"");
			writer.write(TurtleUtil.encodeLongString(label));
			writer.write("\"\"\"");
		}
		else {
			// Write label as normal string
			writer.write("\"");
			writer.write(TurtleUtil.encodeString(label));
			writer.write("\"");
		}

		if (Literals.isLanguageLiteral(lit)) {
			// Append the literal's language
			writer.write("@");
			writer.write(lit.getLanguage().get());
		}
		else if (!XMLSchema.STRING.equals(datatype) || !xsdStringToPlainLiteral()) {
			// Append the literal's datatype (possibly written as an abbreviated
			// URI)
			writer.write("^^");
			writeURI(datatype);
		}
	}

	protected void closePreviousStatement()
		throws IOException
	{
		if (!statementClosed) {
			// The previous statement still needs to be closed:
			writer.write(" .");
			writer.writeEOL();
			writer.decreaseIndentation();

			statementClosed = true;
			lastWrittenSubject = null;
			lastWrittenPredicate = null;
		}
	}

	private boolean xsdStringToPlainLiteral() {
		return getWriterConfig().get(BasicWriterSettings.XSD_STRING_TO_PLAIN_LITERAL);
	}
}
