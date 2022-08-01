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

import static javax.servlet.http.HttpServletResponse.SC_OK;

import java.util.Map;

import org.eclipse.rdf4j.http.server.ServerHTTPException;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.turtle.TurtleWriterFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpMethod;
import org.springframework.ui.ModelMap;

public class TestExportStatementsView extends TestStatementsCommon {
	public static final String REPOSITORY_ERROR_MSG = "Unable to get statements from Sail";
	private final ExportStatementsView exportStatementsView = ExportStatementsView.getInstance();
	private final Map<String, Object> model = new ModelMap();

	@Before
	public void initMocks() {
		request.setMethod(HttpMethod.GET.name());
		model.put(ExportStatementsView.FACTORY_KEY, new TurtleWriterFactory());
		model.put(ExportStatementsView.USE_INFERENCING_KEY, false);
		model.put(ExportStatementsView.HEADERS_ONLY, false);

		super.initMocks();
	}

	@Test
	public void shouldReturnSC_OKIfNoExceptionIsThrown() throws Exception {
		// act
		exportStatementsView.render(model, request, response);

		Assert.assertEquals(SC_OK, response.getStatus());
	}

	@Test
	public void shouldReturnSC_INTERNAL_SERVER_ERRORIfExceptionIsThrown() throws Exception {
		Exception exception = null;
		Mockito.doThrow(new RepositoryException(REPOSITORY_ERROR_MSG))
				.when(connectionMock)
				.exportStatements(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(Boolean.class),
						Mockito.notNull(), Mockito.any());

		try {
			// act
			exportStatementsView.render(model, request, response);
		} catch (ServerHTTPException ex) {
			exception = ex;
		}

		Assert.assertNotNull(exception);
		Assert.assertTrue(exception.getMessage().contains(REPOSITORY_ERROR_MSG));
	}
}
