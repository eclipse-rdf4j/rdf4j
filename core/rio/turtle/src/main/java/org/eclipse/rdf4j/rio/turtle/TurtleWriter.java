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
import java.nio.charset.StandardCharsets;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Map;
import java.util.StringTokenizer;

import org.eclipse.rdf4j.common.io.IndentingWriter;
import org.eclipse.rdf4j.common.net.ParsedIRI;
import org.eclipse.rdf4j.common.text.StringUtil;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.datatypes.XMLDatatypeUtil;
import org.eclipse.rdf4j.model.impl.SimpleIRI;
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
 * An implementation of the RDFWriter interface that writes RDF documents in Turtle format. The Turtle format is defined
 * in <a href="http://www.dajobe.org/2004/01/turtle/">in this document</a>.
 */
public class TurtleWriter extends AbstractRDFWriter implements RDFWriter {

	private static final int LINE_WRAP = 80;

	private static final IRI FIRST = new SimpleIRI(RDF.FIRST.stringValue()) {
		private static final long serialVersionUID = -7951518099940758898L;
	};

	private static final IRI REST = new SimpleIRI(RDF.REST.stringValue()) {
		private static final long serialVersionUID = -7951518099940758898L;
	};

	/*-----------*
	 * Variables *
	 *-----------*/

	protected ParsedIRI baseIRI;
	protected IndentingWriter writer;
	protected boolean writingStarted = false;

	/**
	 * Flag indicating whether the last written statement has been closed.
	 */
	protected boolean statementClosed = true;
	protected Resource lastWrittenSubject;
	protected IRI lastWrittenPredicate;

	/**
	 * Always null
	 */
	@Deprecated
	protected Model prettyPrintModel;

	private final Deque<Resource> stack = new LinkedList<>();
	private final Deque<IRI> path = new LinkedList<>();

	private Boolean xsdStringToPlainLiteral;
	private Boolean prettyPrint;
	private boolean inlineBNodes;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new TurtleWriter that will write to the supplied OutputStream.
	 *
	 * @param out The OutputStream to write the Turtle document to.
	 */
	public TurtleWriter(OutputStream out) {
		this(out, null);
	}

	/**
	 * Creates a new TurtleWriter that will write to the supplied OutputStream.
	 *
	 * @param out     The OutputStream to write the Turtle document to.
	 * @param baseIRI
	 */
	public TurtleWriter(OutputStream out, ParsedIRI baseIRI) {
		super(out);
		this.baseIRI = baseIRI;
		this.writer = new IndentingWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
	}

	/**
	 * Creates a new TurtleWriter that will write to the supplied Writer.
	 *
	 * @param writer The Writer to write the Turtle document to.
	 */
	public TurtleWriter(Writer writer) {
		this(writer, null);
	}

	/**
	 * Creates a new TurtleWriter that will write to the supplied Writer.
	 *
	 * @param writer  The Writer to write the Turtle document to.
	 * @param baseIRI
	 */
	public TurtleWriter(Writer writer, ParsedIRI baseIRI) {
		this.baseIRI = baseIRI;
		this.writer = new IndentingWriter(writer);
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	public RDFFormat getRDFFormat() {
		return RDFFormat.TURTLE;
	}

	@Override
	public void startRDF() throws RDFHandlerException {
		if (writingStarted) {
			throw new RuntimeException("Document writing has already started");
		}

		writingStarted = true;

		try {
			xsdStringToPlainLiteral = getWriterConfig().get(BasicWriterSettings.XSD_STRING_TO_PLAIN_LITERAL);
			prettyPrint = getWriterConfig().get(BasicWriterSettings.PRETTY_PRINT);
			inlineBNodes = getWriterConfig().get(BasicWriterSettings.INLINE_BLANK_NODES);
			if (prettyPrint) {
				writer.setIndentationString("  ");
			} else {
				writer.setIndentationString("");
			}
			if (baseIRI != null && getWriterConfig().get(BasicWriterSettings.BASE_DIRECTIVE)) {
				writeBase(baseIRI.toString());
			}

			// Write namespace declarations
			for (Map.Entry<String, String> entry : namespaceTable.entrySet()) {
				String name = entry.getKey();
				String prefix = entry.getValue();

				writeNamespace(prefix, name);
			}

			if (!namespaceTable.isEmpty()) {
				writer.writeEOL();
			}
		} catch (IOException e) {
			throw new RDFHandlerException(e);
		}
	}

	@Override
	public void endRDF() throws RDFHandlerException {
		if (!writingStarted) {
			throw new RuntimeException("Document writing has not yet started");
		}

		try {
			closePreviousStatement();
			writer.flush();
		} catch (IOException e) {
			throw new RDFHandlerException(e);
		} finally {
			writingStarted = false;
		}
	}

	@Override
	public void handleNamespace(String prefix, String name) throws RDFHandlerException {
		try {
			if (!namespaceTable.containsKey(name)) {
				// Namespace not yet mapped to a prefix, try to give it the
				// specified prefix

				boolean isLegalPrefix = prefix.length() == 0 || TurtleUtil.isPN_PREFIX(prefix);

				if (!isLegalPrefix || namespaceTable.containsValue(prefix)) {
					// Specified prefix is not legal or the prefix is already in
					// use,
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
		} catch (IOException e) {
			throw new RDFHandlerException(e);
		}
	}

	@Override
	public void handleStatement(Statement st) throws RDFHandlerException {
		if (!writingStarted) {
			throw new RuntimeException("Document writing has not yet been started");
		}

		try {
			Resource subj = st.getSubject();
			IRI pred = st.getPredicate();
			if (inlineBNodes && (pred.equals(RDF.FIRST) || pred.equals(RDF.REST))) {
				handleList(st);
			} else if (inlineBNodes && !subj.equals(lastWrittenSubject) && stack.contains(subj)) {
				handleInlineNode(st);
			} else {
				closeHangingResource();
				handleStatementInternal(st, false, inlineBNodes, inlineBNodes);
			}
		} catch (IOException e) {
			throw new RDFHandlerException(e);
		}
	}

	/**
	 * Internal method that differentiates between the pretty-print and streaming writer cases.
	 * 
	 * @param st                     The next statement to write
	 * @param endRDFCalled           True if endRDF has been called before this method is called. This is used to buffer
	 *                               statements for pretty-printing before dumping them when all statements have been
	 *                               delivered to us.
	 * @param canShortenSubjectBNode True if, in the current context, we may be able to shorten the subject of this
	 *                               statement iff it is an instance of {@link BNode}.
	 * @param canShortenObjectBNode  True if, in the current context, we may be able to shorten the object of this
	 *                               statement iff it is an instance of {@link BNode}.
	 */
	protected void handleStatementInternal(Statement st, boolean endRDFCalled, boolean canShortenSubjectBNode,
			boolean canShortenObjectBNode) {
		Resource subj = st.getSubject();
		IRI pred = st.getPredicate();
		Value obj = st.getObject();

		try {
			if (subj.equals(lastWrittenSubject)) {
				if (pred.equals(lastWrittenPredicate)) {
					// Identical subject and predicate
					writer.write(",");
					wrapLine(prettyPrint);
				} else {
					// Identical subject, new predicate
					writer.write(";");
					writer.writeEOL();

					// Write new predicate
					writer.decreaseIndentation();
					writePredicate(pred);
					writer.increaseIndentation();
					wrapLine(true);
					path.removeLast();
					path.addLast(pred);
					lastWrittenPredicate = pred;
				}
			} else {
				// New subject
				closePreviousStatement();
				stack.addLast(subj);

				// Write new subject:
				if (prettyPrint) {
					writer.writeEOL();
				}
				writeResource(subj, canShortenSubjectBNode);
				wrapLine(true);
				writer.increaseIndentation();
				lastWrittenSubject = subj;

				// Write new predicate
				writePredicate(pred);
				wrapLine(true);
				path.addLast(pred);
				lastWrittenPredicate = pred;

				statementClosed = false;
				writer.increaseIndentation();
			}

			writeValue(obj, canShortenObjectBNode);

			// Don't close the line just yet. Maybe the next
			// statement has the same subject and/or predicate.
		} catch (IOException e) {
			throw new RDFHandlerException(e);
		}
	}

	@Override
	public void handleComment(String comment) throws RDFHandlerException {
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
			} else {
				writeCommentLine(comment);
			}
		} catch (IOException e) {
			throw new RDFHandlerException(e);
		}
	}

	protected void writeCommentLine(String line) throws IOException {
		writer.write("# ");
		writer.write(line);
		writer.writeEOL();
	}

	protected void writeBase(String baseURI) throws IOException {
		writer.write("@base <");
		StringUtil.simpleEscapeIRI(baseURI, writer, false);
		writer.write("> .");
		writer.writeEOL();
	}

	protected void writeNamespace(String prefix, String name) throws IOException {
		writer.write("@prefix ");
		writer.write(prefix);
		writer.write(": <");
		StringUtil.simpleEscapeIRI(name, writer, false);
		writer.write("> .");
		writer.writeEOL();
	}

	protected void writePredicate(IRI predicate) throws IOException {
		if (predicate.equals(RDF.TYPE)) {
			// Write short-cut for rdf:type
			writer.write("a");
		} else {
			writeURI(predicate);
		}
	}

	/**
	 * @param val The {@link Value} to write.
	 * @throws IOException
	 * @deprecated Use {@link #writeValue(Value, boolean)} instead.
	 */
	@Deprecated
	protected void writeValue(Value val) throws IOException {
		writeValue(val, false);
	}

	/**
	 * Writes a value, optionally shortening it if it is an {@link IRI} and has a namespace definition that is suitable
	 * for use in this context for shortening or a {@link BNode} that has been confirmed to be able to be shortened in
	 * this context.
	 * 
	 * @param val        The {@link Value} to write.
	 * @param canShorten True if, in the current context, we can shorten this value if it is an instance of
	 *                   {@link BNode} .
	 * @throws IOException
	 */
	protected void writeValue(Value val, boolean canShorten) throws IOException {
		if (val instanceof BNode && canShorten && !val.equals(stack.peekLast()) && !val.equals(lastWrittenSubject)) {
			stack.addLast((BNode) val);
		} else if (val instanceof Resource) {
			writeResource((Resource) val, canShorten);
		} else {
			writeLiteral((Literal) val);
		}
	}

	/**
	 * @param res The {@link Resource} to write.
	 * @throws IOException
	 * @deprecated Use {@link #writeResource(Resource, boolean)} instead.
	 */
	@Deprecated
	protected void writeResource(Resource res) throws IOException {
		writeResource(res, false);
	}

	/**
	 * Writes a {@link Resource}, optionally shortening it if it is an {@link IRI} and has a namespace definition that
	 * is suitable for use in this context for shortening or a {@link BNode} that has been confirmed to be able to be
	 * shortened in this context.
	 * 
	 * @param res        The {@link Resource} to write.
	 * @param canShorten True if, in the current context, we can shorten this value if it is an instance of
	 *                   {@link BNode} .
	 * @throws IOException
	 */
	protected void writeResource(Resource res, boolean canShorten) throws IOException {
		if (res instanceof IRI) {
			writeURI((IRI) res);
		} else {
			writeBNode((BNode) res, canShorten);
		}
	}

	protected void writeURI(IRI uri) throws IOException {
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
		} else if (baseIRI != null) {
			// Write relative URI
			writer.write("<");
			StringUtil.simpleEscapeIRI(baseIRI.relativize(uriString), writer, false);
			writer.write(">");
		} else {
			// Write full URI
			writer.write("<");
			StringUtil.simpleEscapeIRI(uriString, writer, false);
			writer.write(">");
		}
	}

	/**
	 * @param bNode The {@link BNode} to write.
	 * @throws IOException
	 * @deprecated Use {@link #writeBNode(BNode, boolean)} instead.
	 */
	@Deprecated
	protected void writeBNode(BNode bNode) throws IOException {
		writeBNode(bNode, false);
	}

	protected void writeBNode(BNode bNode, boolean canShorten) throws IOException {
		if (canShorten) {
			writer.write("[]");
			return;
		}

		writer.write("_:");
		String id = bNode.getID();

		if (id.isEmpty()) {
			if (this.getWriterConfig().get(BasicParserSettings.PRESERVE_BNODE_IDS)) {
				throw new IOException("Cannot consistently write blank nodes with empty internal identifiers");
			}
			writer.write("genid-hash-");
			writer.write(Integer.toHexString(System.identityHashCode(bNode)));
		} else {
			if (!TurtleUtil.isNameStartChar(id.charAt(0))) {
				writer.write("genid-start-");
				writer.write(Integer.toHexString(id.charAt(0)));
			} else {
				writer.write(id.charAt(0));
			}
			for (int i = 1; i < id.length() - 1; i++) {
				if (TurtleUtil.isPN_CHARS(id.charAt(i))) {
					writer.write(id.charAt(i));
				} else {
					writer.write(Integer.toHexString(id.charAt(i)));
				}
			}
			if (id.length() > 1) {
				if (!TurtleUtil.isNameEndChar(id.charAt(id.length() - 1))) {
					writer.write(Integer.toHexString(id.charAt(id.length() - 1)));
				} else {
					writer.write(id.charAt(id.length() - 1));
				}
			}
		}
	}

	protected void writeLiteral(Literal lit) throws IOException {
		String label = lit.getLabel();
		IRI datatype = lit.getDatatype();

		if (prettyPrint) {
			if (XMLSchema.INTEGER.equals(datatype) || XMLSchema.DECIMAL.equals(datatype)
					|| XMLSchema.DOUBLE.equals(datatype) || XMLSchema.BOOLEAN.equals(datatype)) {
				try {
					String normalized = XMLDatatypeUtil.normalize(label, datatype);
					if (!normalized.equals(XMLDatatypeUtil.POSITIVE_INFINITY)
							&& !normalized.equals(XMLDatatypeUtil.NEGATIVE_INFINITY)
							&& !normalized.equals(XMLDatatypeUtil.NaN)) {
						writer.write(normalized);
						return; // done
					}
				} catch (IllegalArgumentException e) {
					// not a valid numeric typed literal. ignore error and write
					// as
					// quoted string instead.
				}
			}
		}

		if (label.indexOf('\n') != -1 || label.indexOf('\r') != -1 || label.indexOf('\t') != -1) {
			// Write label as long string
			writer.write("\"\"\"");
			writer.write(TurtleUtil.encodeLongString(label));
			writer.write("\"\"\"");
		} else {
			// Write label as normal string
			writer.write("\"");
			writer.write(TurtleUtil.encodeString(label));
			writer.write("\"");
		}

		if (Literals.isLanguageLiteral(lit)) {
			// Append the literal's language
			writer.write("@");
			writer.write(lit.getLanguage().get());
		} else if (!xsdStringToPlainLiteral || !XMLSchema.STRING.equals(datatype)) {
			// Append the literal's datatype (possibly written as an abbreviated
			// URI)
			writer.write("^^");
			writeURI(datatype);
		}
	}

	protected void closePreviousStatement() throws IOException {
		closeNestedResources(null);
		if (!statementClosed) {
			// The previous statement still needs to be closed:
			writer.write(" .");
			writer.writeEOL();
			writer.decreaseIndentation();
			writer.decreaseIndentation();

			stack.pollLast();
			path.pollLast();
			assert stack.isEmpty();
			assert path.isEmpty();
			statementClosed = true;
			lastWrittenSubject = null;
			lastWrittenPredicate = null;
		}
	}

	private boolean isHanging() {
		return !stack.isEmpty() && lastWrittenSubject != null && !lastWrittenSubject.equals(stack.peekLast());
	}

	private void closeHangingResource() throws IOException {
		if (isHanging()) {
			Value val = stack.pollLast();
			if (val instanceof Resource) {
				writeResource((Resource) val, inlineBNodes);
			} else {
				writeLiteral((Literal) val);
			}
			assert lastWrittenSubject.equals(stack.peekLast());
		}
	}

	private void closeNestedResources(Resource subj) throws IOException {
		closeHangingResource();
		while (stack.size() > 1 && !stack.peekLast().equals(subj)) {
			if (prettyPrint) {
				writer.writeEOL();
			}
			writer.decreaseIndentation();
			writer.decreaseIndentation();
			writer.write("]");

			stack.pollLast();
			path.pollLast();
			lastWrittenSubject = stack.peekLast();
			lastWrittenPredicate = path.peekLast();
		}
	}

	private void handleInlineNode(Statement st) throws IOException {
		Resource subj = st.getSubject();
		IRI pred = st.getPredicate();
		if (isHanging() && subj.equals(stack.peekLast())) {
			// blank subject
			lastWrittenSubject = subj;
			writer.write("[");
			if (prettyPrint && !RDF.TYPE.equals(pred)) {
				writer.writeEOL();
			} else {
				wrapLine(prettyPrint);
			}
			writer.increaseIndentation();

			// Write new predicate
			writePredicate(pred);
			writer.increaseIndentation();
			wrapLine(true);
			path.addLast(pred);
			lastWrittenPredicate = pred;
			writeValue(st.getObject(), inlineBNodes);
		} else if (!subj.equals(lastWrittenSubject) && stack.contains(subj)) {
			closeNestedResources(subj);
			handleStatementInternal(st, false, inlineBNodes, inlineBNodes);
		} else {
			assert false;
		}
	}

	private void handleList(Statement st) throws IOException {
		Resource subj = st.getSubject();
		boolean first = RDF.FIRST.equals(st.getPredicate());
		boolean rest = RDF.REST.equals(st.getPredicate()) && !RDF.NIL.equals(st.getObject());
		boolean nil = RDF.REST.equals(st.getPredicate()) && RDF.NIL.equals(st.getObject());
		if (first && REST != lastWrittenPredicate && isHanging()) {
			// new collection
			writer.write("(");
			writer.increaseIndentation();
			wrapLine(false);
			lastWrittenSubject = subj;
			path.addLast(FIRST);
			lastWrittenPredicate = FIRST;
			writeValue(st.getObject(), inlineBNodes);
		} else if (first && REST == lastWrittenPredicate) {
			// item in existing collection
			lastWrittenSubject = subj;
			path.addLast(FIRST);
			lastWrittenPredicate = FIRST;
			writeValue(st.getObject(), inlineBNodes);
		} else {
			closeNestedResources(subj);
			if (rest && FIRST == lastWrittenPredicate) {
				// next item
				wrapLine(true);
				path.removeLast(); // RDF.FIRST
				path.addLast(REST);
				lastWrittenPredicate = REST;
				writeValue(st.getObject(), inlineBNodes);
			} else if (nil && FIRST == lastWrittenPredicate) {
				writer.decreaseIndentation();
				writer.write(")");
				path.removeLast(); // RDF.FIRST
				path.addLast(REST);
				while (REST == path.peekLast()) {
					stack.pollLast();
					path.pollLast();
					lastWrittenSubject = stack.peekLast();
					lastWrittenPredicate = path.peekLast();
				}
			} else {
				handleStatementInternal(st, false, inlineBNodes, inlineBNodes);
			}
		}
	}

	private void wrapLine(boolean space) throws IOException {
		if (prettyPrint && writer.getCharactersSinceEOL() > LINE_WRAP) {
			writer.writeEOL();
		} else if (space) {
			writer.write(" ");
		}
	}
}
