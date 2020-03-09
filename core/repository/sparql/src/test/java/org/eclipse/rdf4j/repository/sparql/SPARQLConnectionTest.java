/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.repository.sparql;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.eclipse.rdf4j.http.client.SPARQLProtocolSession;
import org.eclipse.rdf4j.rio.ParserConfig;
import org.junit.Before;
import org.junit.Test;

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
}
