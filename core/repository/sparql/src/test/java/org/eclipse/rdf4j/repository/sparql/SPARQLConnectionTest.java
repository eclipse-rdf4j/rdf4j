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
package org.eclipse.rdf4j.repository.sparql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.ref.WeakReference;

import org.eclipse.rdf4j.http.client.SPARQLProtocolSession;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDF4J;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.query.impl.MapBindingSet;
import org.eclipse.rdf4j.query.impl.SimpleBinding;
import org.eclipse.rdf4j.query.impl.TupleQueryResultBuilder;
import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.eclipse.rdf4j.query.parser.sparql.SPARQLParser;
import org.eclipse.rdf4j.query.parser.sparql.SPARQLParserFactory;
import org.eclipse.rdf4j.rio.ParserConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;

public class SPARQLConnectionTest {

	private SPARQLConnection subject;
	private SPARQLProtocolSession client;
	private final ValueFactory vf = SimpleValueFactory.getInstance();

	@BeforeEach
	public void setUp() {
		client = mock(SPARQLProtocolSession.class);
		subject = new SPARQLConnection(null, client);
	}

	@Test
	public void setParserConfigPassesToProtocolSession() {
		ParserConfig config = new ParserConfig();

		subject.setParserConfig(config);
		verify(client, times(1)).setParserConfig(config);
	}

	@Test
	public void commitOnEmptyTxnDoesNothing() throws Exception {
		subject.begin();
		subject.commit();

		// verify both method signatures for sendUpdate never get called.
		verify(client, never()).sendUpdate(any(), any(), any(), any(), anyBoolean(), anyInt(), any());
		verify(client, never()).sendUpdate(any(), any(), any(), any(), anyBoolean(), any());
	}

	@Test
	public void testGroupingAddsInInsert() throws Exception {
		ArgumentCaptor<String> sparqlUpdateCaptor = ArgumentCaptor.forClass(String.class);

		subject.begin();
		subject.add(FOAF.PERSON, RDF.TYPE, RDFS.CLASS);
		subject.add(FOAF.AGENT, RDF.TYPE, RDFS.CLASS);
		subject.commit();

		verify(client).sendUpdate(any(), sparqlUpdateCaptor.capture(), any(), any(), anyBoolean(), anyInt(), any());

		String sparqlUpdate = sparqlUpdateCaptor.getValue();
		String expectedTriple1 = "<" + FOAF.PERSON + "> <" + RDF.TYPE + "> <" + RDFS.CLASS + ">";
		String expectedTriple2 = "<" + FOAF.AGENT + "> <" + RDF.TYPE + "> <" + RDFS.CLASS + ">";

		assertThat(sparqlUpdate).containsOnlyOnce("INSERT DATA").contains(expectedTriple1).contains(expectedTriple2);
	}

	@Test
	public void testAddSingleContextHandling() throws Exception {
		ArgumentCaptor<String> sparqlUpdateCaptor = ArgumentCaptor.forClass(String.class);

		IRI g1 = vf.createIRI("urn:g1");

		subject.begin();
		subject.add(FOAF.PERSON, RDF.TYPE, RDFS.CLASS, g1);
		subject.remove(FOAF.AGENT, RDF.TYPE, RDFS.CLASS);
		subject.commit();

		verify(client).sendUpdate(any(), sparqlUpdateCaptor.capture(), any(), any(), anyBoolean(), anyInt(), any());

		String sparqlUpdate = sparqlUpdateCaptor.getValue();
		String expectedAddPattern = "INSERT DATA[^{]*\\{[^G]*GRAPH <" + g1 + ">[^{]*\\{[^<]*<" + FOAF.PERSON + "> ";
		String expectedRemovePattern = "DELETE DATA[^{]*\\{[^<]*<" + FOAF.AGENT + "> ";

		assertThat(sparqlUpdate).containsPattern(expectedAddPattern).containsPattern(expectedRemovePattern);
	}

	@Test
	public void testSizeQuery() throws Exception {

		String sizeAsTupleQuery = subject.sizeAsTupleQuery();
		ParsedQuery query = new SPARQLParserFactory().getParser().parseQuery(sizeAsTupleQuery, "http://example.org/");
		assertNotNull(query);

		sizeAsTupleQuery = subject.sizeAsTupleQuery(vf.createIRI("urn:g1"));
		query = new SPARQLParserFactory().getParser().parseQuery(sizeAsTupleQuery, "http://example.org/");
		assertNotNull(query);

		sizeAsTupleQuery = subject.sizeAsTupleQuery(vf.createIRI("urn:g1"), vf.createIRI("urn:g2"));
		query = new SPARQLParserFactory().getParser().parseQuery(sizeAsTupleQuery, "http://example.org/");
		assertNotNull(query);

		sizeAsTupleQuery = subject.sizeAsTupleQuery(vf.createIRI("urn:g1"), vf.createBNode());
		query = new SPARQLParserFactory().getParser().parseQuery(sizeAsTupleQuery, "http://example.org/");
		assertNotNull(query);

		sizeAsTupleQuery = subject.sizeAsTupleQuery(RDF4J.NIL);
		query = new SPARQLParserFactory().getParser().parseQuery(sizeAsTupleQuery, "http://example.org/");
		assertNotNull(query);
		assertFalse(sizeAsTupleQuery.contains("nil"));

		sizeAsTupleQuery = subject.sizeAsTupleQuery(null);
		query = new SPARQLParserFactory().getParser().parseQuery(sizeAsTupleQuery, "http://example.org/");

		assertNotNull(query);
	}

	@Test
	public void testAddMultipleContextHandling() throws Exception {
		ArgumentCaptor<String> sparqlUpdateCaptor = ArgumentCaptor.forClass(String.class);

		IRI g1 = vf.createIRI("urn:g1");
		IRI g2 = vf.createIRI("urn:g2");

		subject.begin();
		subject.add(FOAF.PERSON, RDF.TYPE, RDFS.CLASS, g1, g2);
		subject.remove(FOAF.AGENT, RDF.TYPE, RDFS.CLASS);
		subject.commit();

		verify(client).sendUpdate(any(), sparqlUpdateCaptor.capture(), any(), any(), anyBoolean(), anyInt(), any());

		String sparqlUpdate = sparqlUpdateCaptor.getValue();
		String expectedAddPattern1 = "INSERT DATA[^{]*\\{[^G]*GRAPH <" + g1 + ">[^{]*\\{[^<]*<" + FOAF.PERSON + "> ";
		String expectedAddPattern2 = "INSERT DATA[^{]*\\{[^G]*GRAPH <" + g2 + ">[^{]*\\{[^<]*<" + FOAF.PERSON + "> ";
		String expectedRemovePattern = "DELETE DATA[^{]*\\{[^<]*<" + FOAF.AGENT + "> ";

		assertThat(sparqlUpdate).containsPattern(expectedAddPattern1)
				.containsPattern(expectedAddPattern2)
				.containsPattern(expectedRemovePattern);
	}

	@Test
	public void testHandlingAddsRemoves() throws Exception {
		ArgumentCaptor<String> sparqlUpdateCaptor = ArgumentCaptor.forClass(String.class);

		subject.begin();
		subject.add(FOAF.PERSON, RDF.TYPE, RDFS.CLASS);
		subject.add(FOAF.AGENT, RDF.TYPE, RDFS.CLASS);
		subject.remove(FOAF.BIRTHDAY, RDF.TYPE, RDF.PROPERTY);
		subject.add(FOAF.AGE, RDF.TYPE, RDF.PROPERTY);
		subject.commit();

		verify(client).sendUpdate(any(), sparqlUpdateCaptor.capture(), any(), any(), anyBoolean(), anyInt(), any());

		String sparqlUpdate = sparqlUpdateCaptor.getValue();

		String expectedAddedTriple1 = "<" + FOAF.PERSON + "> <" + RDF.TYPE + "> <" + RDFS.CLASS + "> .";
		String expectedAddedTriple2 = "<" + FOAF.AGENT + "> <" + RDF.TYPE + "> <" + RDFS.CLASS + "> .";
		String expectedAddedTriple3 = "<" + FOAF.AGE + "> <" + RDF.TYPE + "> <" + RDF.PROPERTY + "> ";
		String expectedRemovedTriple1 = "<" + FOAF.BIRTHDAY + "> <" + RDF.TYPE + "> <" + RDF.PROPERTY + "> .";

		String expectedSequence = "INSERT DATA[^{]*\\{[^}]*\\}[^D]+DELETE DATA[^{]*\\{[^}]*\\}[^I]+INSERT DATA.*";

		assertThat(sparqlUpdate).containsPattern(expectedSequence);
		assertThat(sparqlUpdate).contains(expectedAddedTriple1)
				.contains(expectedAddedTriple2)
				.contains(expectedAddedTriple3)
				.contains(expectedRemovedTriple1);

	}

	@Test
	public void testSilentClear() throws Exception {
		subject.setSilentClear(true);
		assertThat(subject.isSilentClear());

		ArgumentCaptor<String> sparqlUpdateCaptor = ArgumentCaptor.forClass(String.class);
		subject.begin();
		subject.clear();
		subject.commit();

		verify(client).sendUpdate(any(), sparqlUpdateCaptor.capture(), any(), any(), anyBoolean(), anyInt(), any());

		String sparqlUpdate = sparqlUpdateCaptor.getValue();
		assertThat(sparqlUpdate).containsOnlyOnce("CLEAR SILENT");
	}

	@Test
	public void testSilentClear_NamedGraph() throws Exception {
		subject.setSilentClear(true);
		assertThat(subject.isSilentClear());

		ArgumentCaptor<String> sparqlUpdateCaptor = ArgumentCaptor.forClass(String.class);
		subject.begin();
		subject.clear(iri("http://example.org/"));
		subject.commit();

		verify(client).sendUpdate(any(), sparqlUpdateCaptor.capture(), any(), any(), anyBoolean(), anyInt(), any());

		String sparqlUpdate = sparqlUpdateCaptor.getValue();
		assertThat(sparqlUpdate).containsOnlyOnce("CLEAR SILENT GRAPH <http://example.org/>");
	}
}
