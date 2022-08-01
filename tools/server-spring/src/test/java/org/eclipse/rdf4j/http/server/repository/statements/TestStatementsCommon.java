/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.http.server.repository.statements;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.ParserConfig;
import org.junit.Before;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class TestStatementsCommon {
	protected final MockHttpServletRequest request = new MockHttpServletRequest();
	protected final MockHttpServletResponse response = new MockHttpServletResponse();
	protected final Repository repMock = Mockito.mock(Repository.class);
	protected final RepositoryConnection connectionMock = Mockito.mock(RepositoryConnection.class);
	private final ParserConfig parserConfigMock = Mockito.mock(ParserConfig.class);

	@Before
	public void initMocks() {
		Mockito.when(repMock.getConnection()).thenReturn(connectionMock);
		Mockito.when(connectionMock.getParserConfig()).thenReturn(parserConfigMock);
		// repository interceptor uses this attribute
		request.setAttribute("repository", repMock);
	}
}
