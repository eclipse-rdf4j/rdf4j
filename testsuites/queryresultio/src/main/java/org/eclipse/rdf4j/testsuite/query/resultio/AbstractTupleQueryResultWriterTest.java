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
package org.eclipse.rdf4j.testsuite.query.resultio;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.TripleTerm;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.impl.MapBindingSet;
import org.eclipse.rdf4j.query.resultio.QueryResultWriter;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultParser;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultParserFactory;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultWriter;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultWriterFactory;
import org.eclipse.rdf4j.query.resultio.helpers.QueryResultCollector;
import org.eclipse.rdf4j.rio.RioSetting;
import org.eclipse.rdf4j.rio.helpers.BasicParserSettings;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.eclipse.rdf4j.rio.helpers.TripleTermUtil;
import org.junit.jupiter.api.Test;

/**
 * Generic tests for {@link TupleQueryResultWriter} implementations.
 *
 * @author Jeen Broekstra
 */
public abstract class AbstractTupleQueryResultWriterTest {

	protected final static ValueFactory vf = SimpleValueFactory.getInstance();

	@Test
	public void testRDFTripleTermHandling_WithEncoding() throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		TupleQueryResultWriter writer = getWriterFactory().getWriter(baos);

		writer.getWriterConfig().set(BasicWriterSettings.ENCODE_TRIPLE_TERMS, true);

		TripleTerm t = vf.createTripleTerm(RDF.ALT, RDF.TYPE, RDFS.CLASS);
		MapBindingSet bs = new MapBindingSet();
		bs.addBinding("t", t);

		writer.startQueryResult(new ArrayList<>(bs.getBindingNames()));
		writer.handleSolution(bs);
		writer.endQueryResult();

		QueryResultCollector collector = new QueryResultCollector();
		TupleQueryResultParser parser = getParserFactory().getParser();
		parser.getParserConfig().set(BasicParserSettings.PROCESS_ENCODED_TRIPLE_TERMS, false);
		parser.setQueryResultHandler(collector);
		parser.parseQueryResult(new ByteArrayInputStream(baos.toByteArray()));

		BindingSet actual = collector.getBindingSets().get(0);

		if (writer.getTupleQueryResultFormat().supportsTripleTerms()) {
			// natively-supporting RDF triple terms writers should ignore the encoding setting and just always use their
			// format
			assertThat(actual.getValue("t")).isInstanceOf(TripleTerm.class);
		} else {
			assertThat(actual.getValue("t")).isInstanceOf(IRI.class);
			assertThat(actual.getValue("t")).isEqualTo(TripleTermUtil.toRDFEncodedValue(t));

		}
	}

	@Test
	public void testRDFTripleTermsHandling_NoEncoding() throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		TupleQueryResultWriter writer = getWriterFactory().getWriter(baos);

		writer.getWriterConfig().set(BasicWriterSettings.ENCODE_TRIPLE_TERMS, false);

		TripleTerm t = vf.createTripleTerm(RDF.ALT, RDF.TYPE, RDFS.CLASS);
		MapBindingSet bs = new MapBindingSet();
		bs.addBinding("t", t);

		writer.startQueryResult(new ArrayList<>(bs.getBindingNames()));
		writer.handleSolution(bs);
		writer.endQueryResult();

		QueryResultCollector collector = new QueryResultCollector();
		TupleQueryResultParser parser = getParserFactory().getParser();

		parser.getParserConfig().set(BasicParserSettings.PROCESS_ENCODED_TRIPLE_TERMS, false);
		parser.setQueryResultHandler(collector);
		parser.parseQueryResult(new ByteArrayInputStream(baos.toByteArray()));

		BindingSet actual = collector.getBindingSets().get(0);
		assertThat(actual.getValue("t")).isInstanceOf(TripleTerm.class);
	}

	@Test
	public void testRDFTripleTermHandling_DeepNesting() throws Exception {

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		TupleQueryResultWriter writer = getWriterFactory().getWriter(baos);

		writer.getWriterConfig().set(BasicWriterSettings.ENCODE_TRIPLE_TERMS, false);

		TripleTerm t2 = vf.createTripleTerm(RDF.ALT, RDF.TYPE, RDFS.CLASS);
		TripleTerm t = vf.createTripleTerm(RDF.BAG, RDFS.COMMENT, t2);

		MapBindingSet bs = new MapBindingSet();
		bs.addBinding("t", t);

		writer.startQueryResult(new ArrayList<>(bs.getBindingNames()));
		writer.handleSolution(bs);
		writer.endQueryResult();

		QueryResultCollector collector = new QueryResultCollector();
		TupleQueryResultParser parser = getParserFactory().getParser();

		parser.getParserConfig().set(BasicParserSettings.PROCESS_ENCODED_TRIPLE_TERMS, false);
		parser.setQueryResultHandler(collector);
		parser.parseQueryResult(new ByteArrayInputStream(baos.toByteArray()));

		BindingSet actual = collector.getBindingSets().get(0);
		assertThat(actual.getValue("t")).isInstanceOf(TripleTerm.class);

		TripleTerm actualT = (TripleTerm) actual.getValue("t");
		assertThat(actualT.getSubject()).isEqualTo(RDF.BAG);
		assertThat(actualT.getPredicate()).isEqualTo(RDFS.COMMENT);
		assertThat(actualT.getObject()).isInstanceOf(TripleTerm.class);

		TripleTerm actualT2 = (TripleTerm) actualT.getObject();
		assertThat(actualT2.getSubject()).isEqualTo(RDF.ALT);
		assertThat(actualT2.getPredicate()).isEqualTo(RDF.TYPE);
		assertThat(actualT2.getObject()).isEqualTo(RDFS.CLASS);
	}

	@Test
	public void testRDFTripleTermHandling_DeepNestingLiteralObject() throws Exception {

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		TupleQueryResultWriter writer = getWriterFactory().getWriter(baos);

		writer.getWriterConfig().set(BasicWriterSettings.ENCODE_TRIPLE_TERMS, false);

		IRI carol = vf.createIRI("http://example/carol");
		IRI says = vf.createIRI("http://example/says");
		Literal alice = vf.createLiteral("Hello world, my name is \"Alice\".");

		TripleTerm t2 = vf.createTripleTerm(carol, says, alice);

		IRI iriWithComma = vf.createIRI("http://example/iri,with,comma");
		IRI exampleSubject = vf.createIRI("http://example/subject");

		TripleTerm t = vf.createTripleTerm(exampleSubject, iriWithComma, t2);

		MapBindingSet bs = new MapBindingSet();
		bs.addBinding("t", t);

		writer.startQueryResult(new ArrayList<>(bs.getBindingNames()));
		writer.handleSolution(bs);
		writer.endQueryResult();

		QueryResultCollector collector = new QueryResultCollector();
		TupleQueryResultParser parser = getParserFactory().getParser();

		parser.getParserConfig().set(BasicParserSettings.PROCESS_ENCODED_TRIPLE_TERMS, false);
		parser.setQueryResultHandler(collector);
		parser.parseQueryResult(new ByteArrayInputStream(baos.toByteArray()));

		BindingSet actual = collector.getBindingSets().get(0);
		assertThat(actual.getValue("t")).isInstanceOf(TripleTerm.class);

		TripleTerm actualT = (TripleTerm) actual.getValue("t");
		assertThat(actualT.getSubject()).isEqualTo(exampleSubject);
		assertThat(actualT.getPredicate()).isEqualTo(iriWithComma);
		assertThat(actualT.getObject()).isInstanceOf(TripleTerm.class);

		TripleTerm actualT2 = (TripleTerm) actualT.getObject();
		assertThat(actualT2.getSubject()).isEqualTo(carol);
		assertThat(actualT2.getPredicate()).isEqualTo(says);
		assertThat(actualT2.getObject()).isEqualTo(alice);
	}

	@Test
	public void testRDFTripleTermHandling_ObjectAsLiteralNumber() throws Exception {

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		TupleQueryResultWriter writer = getWriterFactory().getWriter(baos);

		writer.getWriterConfig().set(BasicWriterSettings.ENCODE_TRIPLE_TERMS, false);

		IRI carol = vf.createIRI("http://example/carol");
		IRI pays = vf.createIRI("http://example/pays");
		Literal price = vf.createLiteral("89", XSD.INTEGER);

		TripleTerm t2 = vf.createTripleTerm(carol, pays, price);

		MapBindingSet bs = new MapBindingSet();
		bs.addBinding("t", t2);

		writer.startQueryResult(new ArrayList<>(bs.getBindingNames()));
		writer.handleSolution(bs);
		writer.endQueryResult();

		QueryResultCollector collector = new QueryResultCollector();
		TupleQueryResultParser parser = getParserFactory().getParser();

		parser.getParserConfig().set(BasicParserSettings.NORMALIZE_DATATYPE_VALUES, true);
		parser.setQueryResultHandler(collector);
		parser.parseQueryResult(new ByteArrayInputStream(baos.toByteArray()));

		BindingSet actual = collector.getBindingSets().get(0);
		assertThat(actual.getValue("t")).isInstanceOf(TripleTerm.class);

		TripleTerm actualT = (TripleTerm) actual.getValue("t");
		assertThat(actualT.getSubject()).isEqualTo(carol);
		assertThat(actualT.getPredicate()).isEqualTo(pays);
		assertThat(actualT.getObject()).isEqualTo(price);
	}

	@Test
	public void testGetSupportedSettings() {
		TupleQueryResultWriter writer = getWriterFactory().getWriter(System.out);

		Collection<RioSetting<?>> supportedSettings = writer.getSupportedSettings();
		assertThat(supportedSettings).containsExactlyInAnyOrder(getExpectedSupportedSettings());
	}

	/**
	 * Get the {@link RioSetting}s expected to be returned by {@link QueryResultWriter#getSupportedSettings()}. Used by
	 * {@link #testGetSupportedSettings()} to determine if the output of
	 * {@link QueryResultWriter#getSupportedSettings()} is as expected for the concrete writer implementation.
	 *
	 * @return an array of {@link RioSetting}s.
	 */
	protected abstract RioSetting<?>[] getExpectedSupportedSettings();

	protected abstract TupleQueryResultParserFactory getParserFactory();

	protected abstract TupleQueryResultWriterFactory getWriterFactory();
}
