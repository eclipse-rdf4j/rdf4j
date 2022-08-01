/**
 * Copyright (c) 2015 Eclipse RDF4J contributors, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.eclipse.rdf4j.http.server.repository.statements;

import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;

import org.eclipse.rdf4j.http.protocol.Protocol;
import org.eclipse.rdf4j.http.server.ClientHTTPException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.Update;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.mockito.Mockito;
import org.springframework.http.HttpMethod;

/**
 * @author jeen
 */
public class TestStatementsController extends TestStatementsCommon {

	private final StatementsController controller = new StatementsController();

	@Before
	public void initMocks() {
		request.setMethod(HttpMethod.POST.name());

		super.initMocks();
	}

	@Test
	public void shouldUseTimeoutParameterForUpdateQueries() throws Exception {
		final int maxExecution = 1;
		request.setContentType(Protocol.SPARQL_UPDATE_MIME_TYPE);
		request.addParameter(Protocol.TIMEOUT_PARAM_NAME, String.valueOf(maxExecution));
		final String updateString = "delete where { <monkey:pod> ?p ?o . }";
		request.setContent(updateString.getBytes(StandardCharsets.UTF_8));

		final Update updateMock = Mockito.mock(Update.class);
		Mockito.when(connectionMock.prepareUpdate(QueryLanguage.SPARQL, updateString, null)).thenReturn(updateMock);

		// act
		controller.handleRequest(request, response);

		Mockito.verify(updateMock).setMaxExecutionTime(maxExecution);
	}

	@Test
	public void shouldThrowDescriptiveErrorOnEmpryUpdateQueries_SparqlUpdateMimeType() {
		request.setContentType(Protocol.SPARQL_UPDATE_MIME_TYPE);
		request.addParameter(Protocol.UPDATE_PARAM_NAME, "");
		Exception exception = Assertions.assertThrows(ClientHTTPException.class, () -> {
			controller.handleRequest(request, response);
		});
		assertTrue(exception.getMessage().contains("Updates must be non-empty"));
	}

	@Test
	public void shouldThrowDescriptiveErrorOnEmptyUpdateQueries_FormMimeType() {
		request.setContentType(Protocol.FORM_MIME_TYPE);
		request.addParameter(Protocol.UPDATE_PARAM_NAME, "");
		Exception exception = Assertions.assertThrows(ClientHTTPException.class, () -> {
			controller.handleRequest(request, response);
		});
		assertTrue(exception.getMessage().contains("Updates must be non-empty"));
	}
}
