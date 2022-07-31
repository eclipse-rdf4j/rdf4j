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
package org.eclipse.rdf4j.query.resultio.sparqlxml;

import static org.eclipse.rdf4j.query.resultio.sparqlxml.SPARQLResultsXMLConstants.BINDING_NAME_ATT;
import static org.eclipse.rdf4j.query.resultio.sparqlxml.SPARQLResultsXMLConstants.BINDING_TAG;
import static org.eclipse.rdf4j.query.resultio.sparqlxml.SPARQLResultsXMLConstants.BNODE_TAG;
import static org.eclipse.rdf4j.query.resultio.sparqlxml.SPARQLResultsXMLConstants.BOOLEAN_FALSE;
import static org.eclipse.rdf4j.query.resultio.sparqlxml.SPARQLResultsXMLConstants.BOOLEAN_TAG;
import static org.eclipse.rdf4j.query.resultio.sparqlxml.SPARQLResultsXMLConstants.BOOLEAN_TRUE;
import static org.eclipse.rdf4j.query.resultio.sparqlxml.SPARQLResultsXMLConstants.HEAD_TAG;
import static org.eclipse.rdf4j.query.resultio.sparqlxml.SPARQLResultsXMLConstants.HREF_ATT;
import static org.eclipse.rdf4j.query.resultio.sparqlxml.SPARQLResultsXMLConstants.LINK_TAG;
import static org.eclipse.rdf4j.query.resultio.sparqlxml.SPARQLResultsXMLConstants.LITERAL_DATATYPE_ATT;
import static org.eclipse.rdf4j.query.resultio.sparqlxml.SPARQLResultsXMLConstants.LITERAL_LANG_ATT;
import static org.eclipse.rdf4j.query.resultio.sparqlxml.SPARQLResultsXMLConstants.LITERAL_TAG;
import static org.eclipse.rdf4j.query.resultio.sparqlxml.SPARQLResultsXMLConstants.NAMESPACE;
import static org.eclipse.rdf4j.query.resultio.sparqlxml.SPARQLResultsXMLConstants.OBJECT_TAG;
import static org.eclipse.rdf4j.query.resultio.sparqlxml.SPARQLResultsXMLConstants.PREDICATE_TAG;
import static org.eclipse.rdf4j.query.resultio.sparqlxml.SPARQLResultsXMLConstants.QNAME;
import static org.eclipse.rdf4j.query.resultio.sparqlxml.SPARQLResultsXMLConstants.RESULT_SET_TAG;
import static org.eclipse.rdf4j.query.resultio.sparqlxml.SPARQLResultsXMLConstants.RESULT_TAG;
import static org.eclipse.rdf4j.query.resultio.sparqlxml.SPARQLResultsXMLConstants.ROOT_TAG;
import static org.eclipse.rdf4j.query.resultio.sparqlxml.SPARQLResultsXMLConstants.SUBJECT_TAG;
import static org.eclipse.rdf4j.query.resultio.sparqlxml.SPARQLResultsXMLConstants.TRIPLE_TAG;
import static org.eclipse.rdf4j.query.resultio.sparqlxml.SPARQLResultsXMLConstants.URI_TAG;
import static org.eclipse.rdf4j.query.resultio.sparqlxml.SPARQLResultsXMLConstants.VAR_NAME_ATT;
import static org.eclipse.rdf4j.query.resultio.sparqlxml.SPARQLResultsXMLConstants.VAR_TAG;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.rdf4j.common.io.CharSink;
import org.eclipse.rdf4j.common.xml.XMLWriter;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Triple;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.Literals;
import org.eclipse.rdf4j.model.vocabulary.SESAMEQNAME;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryResultHandlerException;
import org.eclipse.rdf4j.query.TupleQueryResultHandlerException;
import org.eclipse.rdf4j.query.resultio.AbstractQueryResultWriter;
import org.eclipse.rdf4j.query.resultio.BasicQueryWriterSettings;
import org.eclipse.rdf4j.query.resultio.QueryResultWriter;
import org.eclipse.rdf4j.rio.RioSetting;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.eclipse.rdf4j.rio.helpers.XMLWriterSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An abstract class to implement the base functionality for both SPARQLBooleanXMLWriter and SPARQLResultsXMLWriter.
 *
 * @author Peter Ansell
 */
abstract class AbstractSPARQLXMLWriter extends AbstractQueryResultWriter implements CharSink {

	/*-----------*
	 * Variables *
	 *-----------*/

	/**
	 * XMLWriter to write XML to.
	 */
	protected XMLWriter xmlWriter;

	protected boolean documentOpen = false;

	protected boolean headerOpen = false;

	protected boolean headerComplete = false;

	protected boolean tupleVariablesFound = false;

	/**
	 * Map with keys as namespace URI strings and the values as the shortened prefixes.
	 */
	private final Map<String, String> namespaceTable = new HashMap<>();

	private final Logger log = LoggerFactory.getLogger(this.getClass());

	/*--------------*
	 * Constructors *
	 *--------------*/

	protected AbstractSPARQLXMLWriter(OutputStream out) {
		this.xmlWriter = new XMLWriter(out);
		this.xmlWriter.setPrettyPrint(true);
	}

	protected AbstractSPARQLXMLWriter(Writer writer) {
		this(new XMLWriter(writer));
	}

	protected AbstractSPARQLXMLWriter(XMLWriter xmlWriter) {
		this.xmlWriter = xmlWriter;
		this.xmlWriter.setPrettyPrint(true);
	}

	@Override
	public Writer getWriter() {
		return xmlWriter.getWriter();
	}

	/**
	 * Enables/disables addition of indentation characters and newlines in the XML document. By default, pretty-printing
	 * is set to <var>true</var>. If set to <var>false</var>, no indentation and newlines are added to the XML document.
	 * This method has to be used before writing starts (that is, before {@link #startDocument} is called).
	 *
	 * @deprecated Use {@link #getWriterConfig()} .set(BasicWriterSettings.PRETTY_PRINT, prettyPrint) instead.
	 */
	@Deprecated
	public void setPrettyPrint(boolean prettyPrint) {
		getWriterConfig().set(BasicWriterSettings.PRETTY_PRINT, prettyPrint);
		xmlWriter.setPrettyPrint(prettyPrint);
	}

	protected void endDocument() throws IOException {
		xmlWriter.endTag(ROOT_TAG);

		xmlWriter.endDocument();

		tupleVariablesFound = false;
		headerOpen = false;
		headerComplete = false;
		documentOpen = false;
	}

	@Override
	public void handleBoolean(boolean value) throws QueryResultHandlerException {
		if (!documentOpen) {
			startDocument();
		}

		if (!headerOpen) {
			startHeader();
		}

		if (!headerComplete) {
			endHeader();
		}

		if (tupleVariablesFound) {
			throw new QueryResultHandlerException("Cannot call handleBoolean after startQueryResults");
		}

		try {
			if (value) {
				xmlWriter.textElement(BOOLEAN_TAG, BOOLEAN_TRUE);
			} else {
				xmlWriter.textElement(BOOLEAN_TAG, BOOLEAN_FALSE);
			}

			endDocument();
		} catch (IOException e) {
			throw new QueryResultHandlerException(e);
		}
	}

	@Override
	public void startDocument() throws QueryResultHandlerException {
		if (!documentOpen) {
			documentOpen = true;
			headerOpen = false;
			headerComplete = false;
			tupleVariablesFound = false;

			try {
				xmlWriter.setPrettyPrint(getWriterConfig().get(BasicWriterSettings.PRETTY_PRINT));

				if (getWriterConfig().get(XMLWriterSettings.INCLUDE_XML_PI)) {
					xmlWriter.startDocument();
				}

				xmlWriter.setAttribute("xmlns", NAMESPACE);

				if (getWriterConfig().get(BasicQueryWriterSettings.ADD_SESAME_QNAME)) {
					xmlWriter.setAttribute("xmlns:q", SESAMEQNAME.NAMESPACE);
				}

				for (String nextPrefix : namespaceTable.keySet()) {
					this.log.debug("Adding custom prefix for <{}> to map to <{}>", nextPrefix,
							namespaceTable.get(nextPrefix));
					xmlWriter.setAttribute("xmlns:" + namespaceTable.get(nextPrefix), nextPrefix);
				}
			} catch (IOException e) {
				throw new QueryResultHandlerException(e);
			}
		}
	}

	@Override
	public void handleStylesheet(String url) throws QueryResultHandlerException {
		if (!documentOpen) {
			startDocument();
		}

		try {
			xmlWriter.writeStylesheet(url);
		} catch (IOException e) {
			throw new QueryResultHandlerException(e);
		}
	}

	@Override
	public void startHeader() throws QueryResultHandlerException {
		if (!documentOpen) {
			startDocument();
		}

		if (!headerOpen) {
			try {
				xmlWriter.startTag(ROOT_TAG);

				xmlWriter.startTag(HEAD_TAG);

				headerOpen = true;
			} catch (IOException e) {
				throw new QueryResultHandlerException(e);
			}
		}
	}

	@Override
	public void handleLinks(List<String> linkUrls) throws QueryResultHandlerException {
		if (!documentOpen) {
			startDocument();
		}

		if (!headerOpen) {
			startHeader();
		}

		try {
			// Write link URLs
			for (String name : linkUrls) {
				xmlWriter.setAttribute(HREF_ATT, name);
				xmlWriter.emptyElement(LINK_TAG);
			}
		} catch (IOException e) {
			throw new QueryResultHandlerException(e);
		}
	}

	@Override
	public void endHeader() throws QueryResultHandlerException {
		if (!documentOpen) {
			startDocument();
		}

		if (!headerOpen) {
			startHeader();
		}

		if (!headerComplete) {
			try {
				xmlWriter.endTag(HEAD_TAG);

				if (tupleVariablesFound) {
					// Write start of results, which must always exist, even if there
					// are no result bindings
					xmlWriter.startTag(RESULT_SET_TAG);
				}

				headerComplete = true;
			} catch (IOException e) {
				throw new QueryResultHandlerException(e);
			}
		}
	}

	@Override
	public void startQueryResult(List<String> bindingNames) throws TupleQueryResultHandlerException {
		super.startQueryResult(bindingNames);

		try {
			if (!documentOpen) {
				startDocument();
			}
			if (!headerOpen) {
				startHeader();
			}

			tupleVariablesFound = true;
			// Write binding names
			for (String name : bindingNames) {
				xmlWriter.setAttribute(VAR_NAME_ATT, name);
				xmlWriter.emptyElement(VAR_TAG);
			}
		} catch (IOException | QueryResultHandlerException e) {
			throw new TupleQueryResultHandlerException(e);
		}
	}

	@Override
	public void endQueryResult() throws TupleQueryResultHandlerException {
		try {
			if (!documentOpen) {
				startDocument();
			}

			if (!headerOpen) {
				startHeader();
			}

			if (!headerComplete) {
				endHeader();
			}

			if (!tupleVariablesFound) {
				throw new IllegalStateException("Could not end query result as startQueryResult was not called first.");
			}

			xmlWriter.endTag(RESULT_SET_TAG);
			endDocument();
		} catch (IOException | QueryResultHandlerException e) {
			throw new TupleQueryResultHandlerException(e);
		}
	}

	@Override
	protected void handleSolutionImpl(BindingSet bindingSet) throws TupleQueryResultHandlerException {
		try {
			if (!documentOpen) {
				startDocument();
			}

			if (!headerOpen) {
				startHeader();
			}

			if (!headerComplete) {
				endHeader();
			}

			if (!tupleVariablesFound) {
				throw new IllegalStateException("Must call startQueryResult before handleSolution");
			}

			xmlWriter.startTag(RESULT_TAG);

			for (Binding binding : bindingSet) {
				xmlWriter.setAttribute(BINDING_NAME_ATT, binding.getName());
				xmlWriter.startTag(BINDING_TAG);

				writeValue(binding.getValue());

				xmlWriter.endTag(BINDING_TAG);
			}

			xmlWriter.endTag(RESULT_TAG);
		} catch (IOException | QueryResultHandlerException e) {
			throw new TupleQueryResultHandlerException(e);
		}
	}

	@Override
	public final Collection<RioSetting<?>> getSupportedSettings() {
		Set<RioSetting<?>> result = new HashSet<>(super.getSupportedSettings());

		result.add(BasicWriterSettings.PRETTY_PRINT);
		result.add(BasicWriterSettings.XSD_STRING_TO_PLAIN_LITERAL);
		result.add(BasicWriterSettings.ENCODE_RDF_STAR);
		result.add(BasicQueryWriterSettings.ADD_SESAME_QNAME);
		result.add(XMLWriterSettings.INCLUDE_XML_PI);

		return result;
	}

	@Override
	public void handleNamespace(String prefix, String uri) throws QueryResultHandlerException {
		// we only support the addition of prefixes before the document is open
		// fail silently if namespaces are added after this point
		if (!documentOpen) {
			// SES-1751 : Do not allow overriding of the fixed sparql or
			// sesameqname prefixes
			if (!prefix.trim().isEmpty() && !prefix.trim().equals(SESAMEQNAME.PREFIX)) {
				this.log.debug("Handle namespace: Will map <{}> to <{}>", uri, prefix);
				// NOTE: The keys in the namespace table are the URIs and the values
				// are the prefixes
				this.namespaceTable.put(uri, prefix);
			} else {
				this.log.debug(
						"handleNamespace was ignored for either the empty prefix or the sesame qname prefix (q). Attempted to map: <{}> to <{}>",
						uri, prefix);
			}
		} else {
			this.log.warn("handleNamespace was ignored after startDocument: <{}> to <{}>", uri, prefix);
		}
	}

	private void writeValue(Value value) throws IOException {
		if (value instanceof Triple) {
			writeTriple((Triple) value);
		} else if (value instanceof IRI) {
			writeURI((IRI) value);
		} else if (value instanceof BNode) {
			writeBNode((BNode) value);
		} else if (value instanceof Literal) {
			writeLiteral((Literal) value);
		}
	}

	private boolean isQName(IRI nextUri) {
		return namespaceTable.containsKey(nextUri.getNamespace());
	}

	private void writeTriple(Triple triple) throws IOException {
		xmlWriter.startTag(TRIPLE_TAG);
		xmlWriter.startTag(SUBJECT_TAG);
		writeValue(triple.getSubject());
		xmlWriter.endTag(SUBJECT_TAG);
		xmlWriter.startTag(PREDICATE_TAG);
		writeValue(triple.getPredicate());
		xmlWriter.endTag(PREDICATE_TAG);
		xmlWriter.startTag(OBJECT_TAG);
		writeValue(triple.getObject());
		xmlWriter.endTag(OBJECT_TAG);
		xmlWriter.endTag(TRIPLE_TAG);
	}

	/**
	 * Write a QName for the given URI if and only if the {@link BasicQueryWriterSettings#ADD_SESAME_QNAME} setting has
	 * been set to true. By default it is false, to ensure that this implementation stays within the specification by
	 * default.
	 *
	 * @param nextUri The prefixed URI to be written as a sesame qname attribute.
	 */
	private void writeQName(IRI nextUri) {
		if (getWriterConfig().get(BasicQueryWriterSettings.ADD_SESAME_QNAME)) {
			xmlWriter.setAttribute(QNAME, namespaceTable.get(nextUri.getNamespace()) + ":" + nextUri.getLocalName());
		}
	}

	private void writeURI(IRI uri) throws IOException {
		if (isQName(uri)) {
			writeQName(uri);
		}
		xmlWriter.textElement(URI_TAG, uri.toString());
	}

	private void writeBNode(BNode bNode) throws IOException {
		xmlWriter.textElement(BNODE_TAG, bNode.getID());
	}

	private void writeLiteral(Literal literal) throws IOException {
		if (Literals.isLanguageLiteral(literal)) {
			xmlWriter.setAttribute(LITERAL_LANG_ATT, literal.getLanguage().get());
		}
		// Only enter this section for non-language literals now, as the
		// rdf:langString datatype is handled implicitly above
		else {
			IRI datatype = literal.getDatatype();
			boolean ignoreDatatype = datatype.equals(XSD.STRING) && xsdStringToPlainLiteral();
			if (!ignoreDatatype) {
				if (isQName(datatype)) {
					writeQName(datatype);
				}
				xmlWriter.setAttribute(LITERAL_DATATYPE_ATT, datatype.stringValue());
			}
		}

		xmlWriter.textElement(LITERAL_TAG, literal.getLabel());
	}
}
