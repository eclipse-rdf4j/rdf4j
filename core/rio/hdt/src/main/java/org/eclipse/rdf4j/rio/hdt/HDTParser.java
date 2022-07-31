/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.rio.hdt;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.input.CountingInputStream;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RioSetting;
import org.eclipse.rdf4j.rio.helpers.AbstractRDFParser;

/**
 * RDF parser for HDT v1.0 files. This parser is not thread-safe, therefore its public methods are synchronized.
 *
 * Unfortunately the draft specification is not entirely clear and probably slightly out of date, since the open source
 * reference implementation HDT-It seems to implement a slightly different version. This parser tries to be compatible
 * with HDT-It 1.0.
 *
 * The most important parts are the Dictionaries containing the actual values (S, P, O part of a triple), and the
 * Triples containing the numeric references to construct the triples.
 *
 * Since objects in one triple are often subjects in another triple, these "shared" parts are stored in a shared
 * Dictionary, which may significantly reduce the file size.
 *
 * File structure:
 *
 * <pre>
 * +---------------------+
 * | Global              |
 * | Header              |
 * | Dictionary (Shared) |
 * | Dictionary (S)      |
 * | Dictionary (P)      |
 * | Dictionary (O)      |
 * | Triples             |
 * +---------------------+
 * </pre>
 *
 * @author Bart Hanssens
 *
 * @see <a href="http://www.rdfhdt.org/hdt-binary-format/">HDT draft (2015)</a>
 * @see <a href="https://www.w3.org/Submission/2011/03/">W3C Member Submission (2011)</a>
 */
public class HDTParser extends AbstractRDFParser {
	/**
	 * Creates a new HDTParser that will use a {@link SimpleValueFactory} to create RDF model objects.
	 */
	public HDTParser() {
		super();
	}

	/**
	 * Creates a new HDTParser that will use the supplied ValueFactory to create RDF model objects.
	 *
	 * @param valueFactory A ValueFactory.
	 */
	public HDTParser(ValueFactory valueFactory) {
		super(valueFactory);
	}

	@Override
	public RDFFormat getRDFFormat() {
		return RDFFormat.HDT;
	}

	@Override
	public Collection<RioSetting<?>> getSupportedSettings() {
		Set<RioSetting<?>> result = new HashSet<>();
		return result;
	}

	@Override
	public synchronized void parse(InputStream in, String baseURI)
			throws IOException, RDFParseException, RDFHandlerException {
		if (in == null) {
			throw new IllegalArgumentException("Input stream must not be 'null'");
		}

		if (in instanceof FileInputStream) {
			// "TODO: use more optimized way to parse the file, eg. filechannel / membuffer"
		}

		HDTDictionarySection shared = null;
		HDTDictionarySection subjects = null;
		HDTDictionarySection predicates = null;
		HDTDictionarySection objects = null;
		HDTTriplesSection section = null;

		// not using try-with-resources, since the counter is needed in the catch clause (JDK8)
		CountingInputStream bis = new CountingInputStream(in);
		try {
			reportLocation(0, -1);
			HDTGlobal global = new HDTGlobal();
			global.parse(bis);
			Map<String, String> globalProps = global.getProperties();
			String base = globalProps.getOrDefault(HDTGlobal.GLOBAL_BASEURI, "");
			if (!base.isEmpty()) {
				setBaseURI(base);
			}

			reportLocation(bis.getByteCount(), -1);
			HDTHeader header = new HDTHeader();
			header.parse(bis);

			reportLocation(bis.getByteCount(), -1);
			new HDTDictionary().parse(bis);

			long dpos = bis.getByteCount();
			reportLocation(dpos, -1);
			shared = HDTDictionarySectionFactory.parse(bis, "S+O", dpos);
			shared.parse(bis);

			dpos = bis.getByteCount();
			reportLocation(dpos, -1);
			subjects = HDTDictionarySectionFactory.parse(bis, "S", dpos);
			subjects.parse(bis);

			dpos = bis.getByteCount();
			reportLocation(dpos, -1);
			predicates = HDTDictionarySectionFactory.parse(bis, "P", dpos);
			predicates.parse(bis);

			dpos = bis.getByteCount();
			reportLocation(dpos, -1);
			objects = HDTDictionarySectionFactory.parse(bis, "O", dpos);
			objects.parse(bis);

			reportLocation(bis.getByteCount(), -1);
			HDTTriples triples = new HDTTriples();
			triples.parse(bis);

			reportLocation(bis.getByteCount(), -1);
			section = HDTTriplesSectionFactory.parse(new String(HDTTriples.FORMAT_BITMAP));
			section.parse(bis, triples.getOrder());
		} catch (IOException ioe) {
			reportFatalError(ioe.getMessage(), bis.getCount(), -1);
		} finally {
			bis.close();
		}

		if (rdfHandler != null) {
			rdfHandler.startRDF();
		}

		int cnt = 0;
		int size = shared.size();

		while (section.hasNext()) {
			int[] t = section.next();
			byte[] s = getSO(t[0], size, shared, subjects);
			byte[] p = predicates.get(t[1]);
			byte[] o = getSO(t[2], size, shared, objects);
			Statement stmt = valueFactory.createStatement(createSubject(s), createPredicate(p), createObject(o));

			if (rdfHandler != null) {
				rdfHandler.handleStatement(stmt);
			}
		}

		if (rdfHandler != null) {
			rdfHandler.endRDF();
		}
	}

	/**
	 * Not supported, since HDT is a binary format.
	 */
	@Override
	public synchronized void parse(Reader reader, String baseURI)
			throws IOException, RDFParseException, RDFHandlerException {
		throw new UnsupportedOperationException("HDT is binary, text readers not supported.");
	}

	/**
	 * Get part of triple from shared HDT Dictionary or (if not found) from specific HDT Dictionary
	 *
	 * @param pos    position
	 * @param size   size of shared Dictionary
	 * @param shared shared Dictionary
	 * @param other  specific Dictionary
	 * @return subject or object
	 */
	private byte[] getSO(int pos, int size, HDTDictionarySection shared, HDTDictionarySection other)
			throws IOException {
		return (pos <= size) ? shared.get(pos) : other.get(pos - size);
	}

	private boolean isBNodeID(byte[] b) {
		// HDT-It generates "genid" for blank nodes in RDF/XML
		return (b[0] == '_' || (b.length > 5 && b[0] == 'g' && b[1] == 'e'));
	}

	/**
	 * Create subject IRI or blank node
	 *
	 * @param b byte buffer
	 * @return IRI or blank node
	 */
	private Resource createSubject(byte[] b) {
		String str = new String(b, StandardCharsets.UTF_8);
		return isBNodeID(b) ? valueFactory.createBNode(str) : valueFactory.createIRI(str);
	}

	/**
	 * Create predicate IRI
	 *
	 * @param b byte buffer
	 * @return IRI
	 */
	private IRI createPredicate(byte[] b) {
		return valueFactory.createIRI(new String(b, StandardCharsets.UTF_8));
	}

	/**
	 * Create object (typed) literal, IRI or blank node
	 *
	 * @param b byte buffer
	 * @return literal, IRI or blank node
	 */
	private Value createObject(byte[] b) {
		if (b[0] == '"') {
			int i = b.length - 1;
			for (; i > 1 && b[i] != '"'; i--) {
				if (b[i] == '@') {
					String lang = new String(b, i + 1, b.length - i - 1, StandardCharsets.US_ASCII);
					return valueFactory.createLiteral(new String(b, 1, i - 2, StandardCharsets.UTF_8), lang);
				} else if (b[i] == '^') {
					IRI datatype = valueFactory
							.createIRI(new String(b, i + 2, b.length - i - 3, StandardCharsets.US_ASCII));
					return valueFactory.createLiteral(new String(b, 1, i - 3, StandardCharsets.UTF_8), datatype);
				}
			}
			return valueFactory.createLiteral(new String(b, 1, i - 1, StandardCharsets.UTF_8));
		}
		String str = new String(b, StandardCharsets.UTF_8);
		return isBNodeID(b) ? valueFactory.createBNode(str) : valueFactory.createIRI(str);
	}
}
