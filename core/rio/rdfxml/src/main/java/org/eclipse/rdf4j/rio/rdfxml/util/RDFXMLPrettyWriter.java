/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.rdfxml.util;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Collection;
import java.util.HashSet;
import java.util.Stack;

import org.eclipse.rdf4j.common.net.ParsedIRI;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Literals;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RioSetting;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.eclipse.rdf4j.rio.rdfxml.RDFXMLWriter;

/**
 * An extension of RDFXMLWriter that outputs a more concise form of RDF/XML. The resulting output is semantically
 * equivalent to the output of an RDFXMLWriter (it produces the same set of statements), but it is usually easier to
 * read for humans.
 * <p>
 * This is a quasi-streaming RDFWriter. Statements are cached as long as the striped syntax is followed (i.e. the
 * subject of the next statement is the object of the previous statement) and written to the output when the stripe is
 * broken.
 * <p>
 * The abbreviations used are <a href="http://www.w3.org/TR/rdf-syntax-grammar/#section-Syntax-typed-nodes" >typed node
 * elements</a>, <a href= "http://www.w3.org/TR/rdf-syntax-grammar/#section-Syntax-empty-property-elements" >empty
 * property elements</a> and
 * <a href= "http://www.w3.org/TR/rdf-syntax-grammar/#section-Syntax-node-property-elements" >striped syntax</a>. Note
 * that these abbreviations require that statements are written in the appropriate order.
 * <p>
 * Striped syntax means that when the object of a statement is the subject of the next statement we can nest the
 * descriptions in each other.
 * <p>
 * Example:
 *
 * <pre>
 * &lt;rdf:Seq&gt;
 *    &lt;rdf:li&gt;
 *       &lt;foaf:Person&gt;
 *          &lt;foaf:knows&gt;
 *             &lt;foaf:Person&gt;
 *               &lt;foaf:mbox rdf:resource=&quot;...&quot;/&gt;
 *             &lt;/foaf:Person&gt;
 *          &lt;/foaf:knows&gt;
 *       &lt;/foaf:Person&gt;
 *    &lt;/rdf:li&gt;
 * &lt;/rdf:Seq&gt;
 * </pre>
 *
 * Typed node elements means that we write out type information in the short form of
 *
 * <pre>
 * &lt;foaf:Person rdf:about=&quot;...&quot;&gt;
 *     ...
 *  &lt;/foaf:Person&gt;
 * </pre>
 *
 * instead of
 *
 * <pre>
 * &lt;rdf:Description rdf:about=&quot;...&quot;&gt;
 *    &lt;rdf:type rdf:resource=&quot;http://xmlns.com/foaf/0.1/Person&quot;/&gt;
 *     ...
 *  &lt;/rdf:Description&gt;
 * </pre>
 *
 * Empty property elements are of the form
 *
 * <pre>
 * &lt;foaf:Person&gt;
 *    &lt;foaf:homepage rdf:resource=&quot;http://www.cs.vu.nl/&tilde;marta&quot;/&gt;
 * &lt;/foaf:Person&gt;
 * </pre>
 *
 * instead of
 *
 * <pre>
 * &lt;foaf:Person&gt;
 *    &lt;foaf:homepage&gt;
 *       &lt;rdf:Description rdf:about=&quot;http://www.cs.vu.nl/&tilde;marta&quot;/&gt;
 *    &lt;foaf:homepage&gt;
 * &lt;/foaf:Person&gt;
 * </pre>
 *
 * @author Peter Mika (pmika@cs.vu.nl)
 */
public class RDFXMLPrettyWriter extends RDFXMLWriter implements Closeable, Flushable {

	/*-----------*
	 * Variables *
	 *-----------*/

	/*
	 * We implement striped syntax by using two stacks, one for predicates and one for subjects/objects.
	 */

	/**
	 * Stack for remembering the nodes (subjects/objects) of statements at each level.
	 */
	private final Stack<Node> nodeStack = new Stack<>();

	/**
	 * Stack for remembering the predicate of statements at each level.
	 */
	private final Stack<IRI> predicateStack = new Stack<>();

	private boolean writingEnded;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new RDFXMLPrintWriter that will write to the supplied OutputStream.
	 *
	 * @param out The OutputStream to write the RDF/XML document to.
	 */
	public RDFXMLPrettyWriter(OutputStream out) {
		super(out);
	}

	/**
	 * Creates a new RDFXMLPrintWriter that will write to the supplied OutputStream.
	 *
	 * @param out The OutputStream to write the RDF/XML document to.
	 */
	public RDFXMLPrettyWriter(OutputStream out, ParsedIRI baseIRI) {
		super(out, baseIRI);
	}

	/**
	 * Creates a new RDFXMLPrintWriter that will write to the supplied Writer.
	 *
	 * @param out The Writer to write the RDF/XML document to.
	 */
	public RDFXMLPrettyWriter(Writer out) {
		super(out);
	}

	/**
	 * Creates a new RDFXMLPrintWriter that will write to the supplied Writer.
	 *
	 * @param writer  the Writer to write the RDF/XML document to
	 * @param baseIRI base IRI
	 */
	public RDFXMLPrettyWriter(Writer writer, ParsedIRI baseIRI) {
		super(writer, baseIRI);
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	public void endRDF() throws RDFHandlerException {
		if (!writingEnded) {
			super.endRDF();
			writingEnded = true;
		}
	}

	@Override
	protected void writeHeader() throws IOException {
		// This export format needs the RDF Schema namespace to be defined:
		setNamespace(RDFS.PREFIX, RDFS.NAMESPACE);

		super.writeHeader();
	}

	@Override
	public void flush() throws IOException {
		if (isWritingStarted()) {
			if (!headerWritten) {
				writeHeader();
			}

			try {
				flushPendingStatements();
			} catch (RDFHandlerException e) {
				if (e.getCause() != null && e.getCause() instanceof IOException) {
					throw (IOException) e.getCause();
				} else {
					throw new IOException(e);
				}
			}

			writer.flush();
		}
	}

	@Override
	public Collection<RioSetting<?>> getSupportedSettings() {
		final Collection<RioSetting<?>> settings = new HashSet<>(super.getSupportedSettings());
		settings.add(BasicWriterSettings.INLINE_BLANK_NODES);
		return settings;
	}

	@Override
	public void close() throws IOException {
		try {
			if (isWritingStarted() && !writingEnded) {
				endRDF();
			}
		} catch (RDFHandlerException e) {
			if (e.getCause() != null && e.getCause() instanceof IOException) {
				throw (IOException) e.getCause();
			} else {
				throw new IOException(e);
			}
		} finally {
			nodeStack.clear();
			predicateStack.clear();
			writer.close();
		}
	}

	@Override
	protected void flushPendingStatements() throws IOException, RDFHandlerException {
		if (!nodeStack.isEmpty()) {
			popStacks(null);
		}
	}

	/**
	 * Write out the stacks until we find subject. If subject == null, write out the entire stack
	 *
	 * @param newSubject
	 */
	private void popStacks(Resource newSubject) throws IOException, RDFHandlerException {
		// Write start tags for the part of the stacks that are not yet
		// written
		for (int i = 0; i < nodeStack.size() - 1; i++) {
			Node node = nodeStack.get(i);

			if (!node.isWritten()) {
				if (i > 0) {
					writeIndents(i * 2 - 1);

					IRI predicate = predicateStack.get(i - 1);

					writeStartTag(predicate.getNamespace(), predicate.getLocalName());
					writeNewLine();
				}

				writeIndents(i * 2);
				writeNodeStartTag(node);
				node.setIsWritten(true);
			}
		}

		// Write tags for the top subject
		Node topNode = nodeStack.pop();

		if (predicateStack.isEmpty()) {
			// write out an empty subject
			writeIndents(nodeStack.size() * 2);
			writeNodeEmptyTag(topNode);
			writeNewLine();
		} else {
			IRI topPredicate = predicateStack.pop();

			if (!topNode.hasType()) {
				// we can use an abbreviated predicate
				writeIndents(nodeStack.size() * 2 - 1);
				writeAbbreviatedPredicate(topPredicate, topNode.getValue());
			} else {
				// we cannot use an abbreviated predicate because the type needs to
				// written out as well

				writeIndents(nodeStack.size() * 2 - 1);
				writeStartTag(topPredicate.getNamespace(), topPredicate.getLocalName());
				writeNewLine();

				// write out an empty subject
				writeIndents(nodeStack.size() * 2);
				writeNodeEmptyTag(topNode);
				writeNewLine();

				writeIndents(nodeStack.size() * 2 - 1);
				writeEndTag(topPredicate.getNamespace(), topPredicate.getLocalName());
				writeNewLine();
			}
		}

		// Write out the end tags until we find the subject
		while (!nodeStack.isEmpty()) {
			Node nextElement = nodeStack.peek();

			if (nextElement.getValue().equals(newSubject)) {
				break;
			} else {
				nodeStack.pop();

				// We have already written out the subject/object,
				// but we still need to close the tag
				writeIndents(predicateStack.size() + nodeStack.size());

				writeNodeEndTag(nextElement);

				if (predicateStack.size() > 0) {
					IRI nextPredicate = predicateStack.pop();

					writeIndents(predicateStack.size() + nodeStack.size());

					writeEndTag(nextPredicate.getNamespace(), nextPredicate.getLocalName());

					writeNewLine();
				}
			}
		}
	}

	@Override
	public void consumeStatement(Statement st) throws RDFHandlerException {
		Resource subj = st.getSubject();
		IRI pred = st.getPredicate();
		Value obj = st.getObject();

		try {
			if (!headerWritten) {
				writeHeader();
			}

			if (!nodeStack.isEmpty() && !subj.equals(nodeStack.peek().getValue())) {
				// Different subject than we had before, empty the stack
				// until we find it
				popStacks(subj);
			}

			// Stack is either empty or contains the same subject at top

			if (nodeStack.isEmpty()) {
				// Push subject
				nodeStack.push(new Node(subj));
			}

			// Stack now contains at least one element
			Node topSubject = nodeStack.peek();

			// Check if current statement is a type statement and use a typed node
			// element is possible
			// FIXME: verify that an XML namespace-qualified name can be created
			// for the type URI
			if (pred.equals(RDF.TYPE) && obj instanceof IRI && !topSubject.hasType() && !topSubject.isWritten()) {
				// Use typed node element
				topSubject.setType((IRI) obj);
			} else {
				if (!nodeStack.isEmpty() && pred.equals(nodeStack.peek().nextLi())) {
					pred = RDF.LI;
					nodeStack.peek().incrementNextLi();
				}

				// Push predicate and object
				predicateStack.push(pred);
				nodeStack.push(new Node(obj));
			}
		} catch (IOException e) {
			throw new RDFHandlerException(e);
		}
	}

	/**
	 * Write out the opening tag of the subject or object of a statement up to (but not including) the end of the tag.
	 * Used both in writeStartSubject and writeEmptySubject.
	 */
	private void writeNodeStartOfStartTag(Node node) throws IOException, RDFHandlerException {
		Boolean inlineBlankNodes = getWriterConfig().get(BasicWriterSettings.INLINE_BLANK_NODES);
		Value value = node.getValue();

		if (node.hasType()) {
			// We can use abbreviated syntax
			writeStartOfStartTag(node.getType().getNamespace(), node.getType().getLocalName());
		} else {
			// We cannot use abbreviated syntax
			writeStartOfStartTag(RDF.NAMESPACE, "Description");
		}

		if (value instanceof IRI) {
			IRI uri = (IRI) value;
			writeAttribute(RDF.NAMESPACE, "about", uri.toString());
		} else {
			BNode bNode = (BNode) value;
			if (!inlineBlankNodes) {
				writeAttribute(RDF.NAMESPACE, "nodeID", getValidNodeId(bNode));
			}
		}
	}

	/**
	 * Write out the opening tag of the subject or object of a statement.
	 */
	private void writeNodeStartTag(Node node) throws IOException, RDFHandlerException {
		writeNodeStartOfStartTag(node);
		writeEndOfStartTag();
		writeNewLine();
	}

	/**
	 * Write out the closing tag for the subject or object of a statement.
	 */
	private void writeNodeEndTag(Node node) throws IOException {
		if (node.getType() != null) {
			writeEndTag(node.getType().getNamespace(), node.getType().getLocalName());
		} else {
			writeEndTag(RDF.NAMESPACE, "Description");
		}
		writeNewLine();
	}

	/**
	 * Write out an empty tag for the subject or object of a statement.
	 */
	private void writeNodeEmptyTag(Node node) throws IOException, RDFHandlerException {
		writeNodeStartOfStartTag(node);
		writeEndOfEmptyTag();
	}

	/**
	 * Write out an empty property element.
	 */
	private void writeAbbreviatedPredicate(IRI pred, Value obj) throws IOException, RDFHandlerException {
		writeStartOfStartTag(pred.getNamespace(), pred.getLocalName());

		if (obj instanceof Resource) {
			Resource objRes = (Resource) obj;

			if (objRes instanceof IRI) {
				IRI uri = (IRI) objRes;
				writeAttribute(RDF.NAMESPACE, "resource", uri.toString());
			} else {
				BNode bNode = (BNode) objRes;
				writeAttribute(RDF.NAMESPACE, "nodeID", getValidNodeId(bNode));
			}

			writeEndOfEmptyTag();
		} else if (obj instanceof Literal) {
			Literal objLit = (Literal) obj;
			// datatype attribute
			CoreDatatype datatype = objLit.getCoreDatatype();
			// Check if datatype is rdf:XMLLiteral
			boolean isXmlLiteral = datatype == CoreDatatype.RDF.XMLLITERAL;

			// language attribute
			if (Literals.isLanguageLiteral(objLit)) {
				writeAttribute("xml:lang", objLit.getLanguage().get());
			} else {
				if (isXmlLiteral) {
					writeAttribute(RDF.NAMESPACE, "parseType", "Literal");
				} else {
					writeAttribute(RDF.NAMESPACE, "datatype", objLit.getDatatype().toString());
				}
			}

			writeEndOfStartTag();

			// label
			if (isXmlLiteral) {
				// Write XML literal as plain XML
				writer.write(objLit.getLabel());
			} else {
				writeCharacterData(objLit.getLabel());
			}

			writeEndTag(pred.getNamespace(), pred.getLocalName());
		}

		writeNewLine();
	}

	protected void writeStartTag(String namespace, String localName) throws IOException {
		writeStartOfStartTag(namespace, localName);
		writeEndOfStartTag();
	}

	/**
	 * Writes <var>n</var> indents.
	 */
	protected void writeIndents(int n) throws IOException {
		for (int i = 0; i < n; i++) {
			writeIndent();
		}
	}

	/*------------------*
	 * Inner class Node *
	 *------------------*/

	private static class Node {

		private int nextLiIndex = 1;

		private Resource nextLi;

		private final Value value;

		// type == null means that we use <rdf:Description>
		private IRI type = null;

		private boolean isWritten = false;

		/**
		 * Creates a new Node for the supplied Value.
		 */
		public Node(Value value) {
			this.value = value;
		}

		Resource nextLi() {
			if (nextLi == null) {
				nextLi = SimpleValueFactory.getInstance().createIRI(RDF.NAMESPACE + "_" + nextLiIndex);
			}

			return nextLi;
		}

		public void incrementNextLi() {
			nextLiIndex++;
			nextLi = null;
		}

		public Value getValue() {
			return value;
		}

		public void setType(IRI type) {
			this.type = type;
		}

		public IRI getType() {
			return type;
		}

		public boolean hasType() {
			return type != null;
		}

		public void setIsWritten(boolean isWritten) {
			this.isWritten = isWritten;
		}

		public boolean isWritten() {
			return isWritten;
		}
	}
}
