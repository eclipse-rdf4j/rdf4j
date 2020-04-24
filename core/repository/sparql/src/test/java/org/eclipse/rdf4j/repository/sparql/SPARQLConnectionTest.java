/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.repository.sparql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.eclipse.rdf4j.http.client.SPARQLProtocolSession;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.rio.ParserConfig;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class SPARQLConnectionTest {

	private SPARQLConnection subject;
	private SPARQLProtocolSession client;

	@Before
	public void setUp() throws Exception {
		client = mock(SPARQLProtocolSession.class);
		subject = new SPARQLConnection(null, client);
	}

	@Test
	public void setParserConfigPassesToProtocolSession() throws Exception {
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
}
