/**
 * Copyright (c) 2015 Eclipse RDF4J contributors, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.eclipse.rdf4j.http.server.repository.statements;

import java.nio.charset.StandardCharsets;

import org.eclipse.rdf4j.http.protocol.Protocol;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * @author jeen
 */
public class TestStatementsController {

	@Test
	public void shouldUseTimeoutParameterForUpdateQueries()
		throws Exception
	{
		//prepare
		StatementsController controller = new StatementsController();

		final int maxExecution = 1;
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod(HttpMethod.POST.name());
		request.setContentType(Protocol.SPARQL_UPDATE_MIME_TYPE);
		request.addParameter(Protocol.TIMEOUT_PARAM_NAME, String.valueOf(maxExecution));
		final String updateString = "delete where { <monkey:pod> ?p ?o . }";
		request.setContent(updateString.getBytes(StandardCharsets.UTF_8));

		// prepare mocks
		final RepositoryConnection connection = Mockito.mock(RepositoryConnection.class);
		final Update updateMock = Mockito.mock(Update.class);
		Mockito.when(connection.prepareUpdate(QueryLanguage.SPARQL, updateString, null)).thenReturn(
				updateMock);

		// repository interceptor uses this attribute
		request.setAttribute("repositoryConnection", connection);

		//act
		controller.handleRequest(request, new MockHttpServletResponse());

		Mockito.verify(updateMock).setMaxExecutionTime(maxExecution);
	}
}
