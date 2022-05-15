/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.testsuite.query.resultio;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Triple;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
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
import org.eclipse.rdf4j.rio.helpers.RDFStarUtil;
import org.junit.jupiter.api.Test;

/**
 * Generic tests for {@link TupleQueryResultWriter} implementations.
 *
 * @author Jeen Broekstra
 */
public abstract class AbstractTupleQueryResultWriterTest {

	protected final static ValueFactory vf = SimpleValueFactory.getInstance();

	@Test
	public void testRDFStarHandling_WithEncoding() throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		TupleQueryResultWriter writer = getWriterFactory().getWriter(baos);

		writer.getWriterConfig().set(BasicWriterSettings.ENCODE_RDF_STAR, true);

		Triple t = vf.createTriple(RDF.ALT, RDF.TYPE, RDFS.CLASS);
		MapBindingSet bs = new MapBindingSet();
		bs.addBinding("t", t);

		writer.startQueryResult(new ArrayList<>(bs.getBindingNames()));
		writer.handleSolution(bs);
		writer.endQueryResult();

		QueryResultCollector collector = new QueryResultCollector();
		TupleQueryResultParser parser = getParserFactory().getParser();
		parser.getParserConfig().set(BasicParserSettings.PROCESS_ENCODED_RDF_STAR, false);
		parser.setQueryResultHandler(collector);
		parser.parseQueryResult(new ByteArrayInputStream(baos.toByteArray()));

		BindingSet actual = collector.getBindingSets().get(0);

		if (writer.getTupleQueryResultFormat().supportsRDFStar()) {
			// natively-supporting RDF-star writers should ignore the encoding setting and just always use their
			// extended format
			assertThat(actual.getValue("t")).isInstanceOf(Triple.class);
		} else {
			assertThat(actual.getValue("t")).isInstanceOf(IRI.class);
			assertThat(actual.getValue("t")).isEqualTo(RDFStarUtil.toRDFEncodedValue((Resource) t));

		}
	}

	@Test
	public void testRDFStarHandling_NoEncoding() throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		TupleQueryResultWriter writer = getWriterFactory().getWriter(baos);

		writer.getWriterConfig().set(BasicWriterSettings.ENCODE_RDF_STAR, false);

		Triple t = vf.createTriple(RDF.ALT, RDF.TYPE, RDFS.CLASS);
		MapBindingSet bs = new MapBindingSet();
		bs.addBinding("t", t);

		writer.startQueryResult(new ArrayList<>(bs.getBindingNames()));
		writer.handleSolution(bs);
		writer.endQueryResult();

		QueryResultCollector collector = new QueryResultCollector();
		TupleQueryResultParser parser = getParserFactory().getParser();

		parser.getParserConfig().set(BasicParserSettings.PROCESS_ENCODED_RDF_STAR, false);
		parser.setQueryResultHandler(collector);
		parser.parseQueryResult(new ByteArrayInputStream(baos.toByteArray()));

		BindingSet actual = collector.getBindingSets().get(0);
		assertThat(actual.getValue("t")).isInstanceOf(Triple.class);
	}

	@Test
	public void testRDFStarHandling_DeepNesting() throws Exception {

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		TupleQueryResultWriter writer = getWriterFactory().getWriter(baos);

		writer.getWriterConfig().set(BasicWriterSettings.ENCODE_RDF_STAR, false);

		Triple t2 = vf.createTriple(RDF.ALT, RDF.TYPE, RDFS.CLASS);
		Triple t = vf.createTriple(RDF.BAG, RDFS.COMMENT, t2);
		MapBindingSet bs = new MapBindingSet();
		bs.addBinding("t", t);

		writer.startQueryResult(new ArrayList<>(bs.getBindingNames()));
		writer.handleSolution(bs);
		writer.endQueryResult();

		QueryResultCollector collector = new QueryResultCollector();
		TupleQueryResultParser parser = getParserFactory().getParser();

		parser.getParserConfig().set(BasicParserSettings.PROCESS_ENCODED_RDF_STAR, false);
		parser.setQueryResultHandler(collector);
		parser.parseQueryResult(new ByteArrayInputStream(baos.toByteArray()));

		BindingSet actual = collector.getBindingSets().get(0);
		assertThat(actual.getValue("t")).isInstanceOf(Triple.class);

		Triple actualT = (Triple) actual.getValue("t");
		assertThat(actualT.getSubject()).isEqualTo(RDF.BAG);
		assertThat(actualT.getPredicate()).isEqualTo(RDFS.COMMENT);
		assertThat(actualT.getObject()).isInstanceOf(Triple.class);

		Triple actualT2 = (Triple) actualT.getObject();
		assertThat(actualT2.getSubject()).isEqualTo(RDF.ALT);
		assertThat(actualT2.getPredicate()).isEqualTo(RDF.TYPE);
		assertThat(actualT2.getObject()).isEqualTo(RDFS.CLASS);
	}

	@Test
	public void testGetSupportedSettings() throws Exception {
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
