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
package org.eclipse.rdf4j.workbench.commands;

import static org.eclipse.rdf4j.workbench.base.TransformationServlet.CONTEXT;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.QueryResultHandlerException;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.workbench.exceptions.BadRequestException;
import org.eclipse.rdf4j.workbench.util.WorkbenchRequest;
import org.junit.Test;

/**
 * Unit and regression tests for {@link RemoteServlet}.
 *
 * @author Dale Visser
 */
public class TestRemoveServlet {

	private final RemoveServlet servlet = new RemoveServlet();

	@Test
	public void testSES1958regression()
			throws RepositoryException, QueryResultHandlerException, IOException, BadRequestException {
		WorkbenchRequest request = mock(WorkbenchRequest.class);
		when(request.isParameterPresent(CONTEXT)).thenReturn(true);
		IRI context = SimpleValueFactory.getInstance().createIRI("<http://foo.org/bar>");
		when(request.getResource(CONTEXT)).thenReturn(context);
		Repository repository = mock(Repository.class);
		servlet.setRepository(repository);
		RepositoryConnection connection = mock(RepositoryConnection.class);
		when(repository.getConnection()).thenReturn(connection);
		servlet.doPost(request, mock(HttpServletResponse.class), "");
		verify(connection).clear(eq(context));
	}
}
