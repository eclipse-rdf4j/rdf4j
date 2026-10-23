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

import static jakarta.servlet.http.HttpServletResponse.SC_OK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.rdf4j.http.server.ServerHTTPException;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.RDFWriterFactory;
import org.eclipse.rdf4j.rio.rdfjson.RDFJSONWriter;
import org.eclipse.rdf4j.rio.rdfjson.RDFJSONWriterSettings;
import org.eclipse.rdf4j.rio.turtle.TurtleWriterFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpMethod;
import org.springframework.ui.ModelMap;

public class TestExportStatementsView extends TestStatementsCommon {
	public static final String REPOSITORY_ERROR_MSG = "Unable to get statements from Sail";
	private final ExportStatementsView exportStatementsView = ExportStatementsView.getInstance();
	private final Map<String, Object> model = new ModelMap();

	@BeforeEach
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

		assertEquals(SC_OK, response.getStatus());
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

		assertNotNull(exception);
		assertTrue(exception.getMessage().contains(REPOSITORY_ERROR_MSG));
	}

	@Test
	public void failedRDFJSONExportReleasesTemporaryBudget() throws Exception {
		ValueFactory vf = SimpleValueFactory.getInstance();
		Statement statement = vf.createStatement(vf.createIRI("urn:test:s"), vf.createIRI("urn:test:p"),
				vf.createLiteral("x".repeat(80)));
		RDFWriterFactory factory = Mockito.mock(RDFWriterFactory.class);
		Mockito.when(factory.getRDFFormat()).thenReturn(RDFFormat.RDFJSON);
		Mockito.when(factory.getWriter(Mockito.any(OutputStream.class))).thenAnswer(invocation -> {
			RDFJSONWriter writer = new RDFJSONWriter((OutputStream) invocation.getArgument(0), RDFFormat.RDFJSON);
			writer.getWriterConfig().set(RDFJSONWriterSettings.MAX_IN_MEMORY_BYTES, 128L);
			writer.getWriterConfig().set(RDFJSONWriterSettings.MAX_SPILL_BYTES, 400L);
			return writer;
		});
		model.put(ExportStatementsView.FACTORY_KEY, factory);
		AtomicInteger calls = new AtomicInteger();
		Mockito.doAnswer(invocation -> {
			RDFHandler handler = invocation.getArgument(4);
			handler.startRDF();
			if (calls.incrementAndGet() == 2) {
				handler.handleStatement(statement);
				throw new RepositoryException("test upstream export failure");
			}
			handler.endRDF();
			return null;
		})
				.when(connectionMock)
				.exportStatements(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyBoolean(),
						Mockito.notNull(), Mockito.any());

		assertThrows(ServerHTTPException.class, () -> exportStatementsView.render(model, request, response));
		RDFJSONWriter later = new RDFJSONWriter(new ByteArrayOutputStream(), RDFFormat.RDFJSON);
		later.getWriterConfig().set(RDFJSONWriterSettings.MAX_IN_MEMORY_BYTES, 128L);
		later.getWriterConfig().set(RDFJSONWriterSettings.MAX_SPILL_BYTES, 400L);
		later.startRDF();
		later.handleStatement(statement);
		later.endRDF();
	}

	@Test
	public void failedRDFJSONPreflightReleasesTemporaryBudget() throws Exception {
		ValueFactory vf = SimpleValueFactory.getInstance();
		Statement statement = vf.createStatement(vf.createIRI("urn:test:s"), vf.createIRI("urn:test:p"),
				vf.createLiteral("x".repeat(80)));
		RDFWriterFactory factory = Mockito.mock(RDFWriterFactory.class);
		Mockito.when(factory.getRDFFormat()).thenReturn(RDFFormat.RDFJSON);
		Mockito.when(factory.getWriter(Mockito.any(OutputStream.class))).thenAnswer(invocation -> {
			RDFJSONWriter writer = new RDFJSONWriter((OutputStream) invocation.getArgument(0), RDFFormat.RDFJSON);
			writer.getWriterConfig().set(RDFJSONWriterSettings.MAX_IN_MEMORY_BYTES, 128L);
			writer.getWriterConfig().set(RDFJSONWriterSettings.MAX_SPILL_BYTES, 400L);
			return writer;
		});
		model.put(ExportStatementsView.FACTORY_KEY, factory);
		Mockito.doAnswer(invocation -> {
			RDFHandler handler = invocation.getArgument(4);
			handler.startRDF();
			handler.handleStatement(statement);
			throw new RepositoryException("test preflight failure");
		})
				.when(connectionMock)
				.exportStatements(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyBoolean(),
						Mockito.notNull(), Mockito.any());

		assertThrows(ServerHTTPException.class, () -> exportStatementsView.render(model, request, response));
		RDFJSONWriter later = new RDFJSONWriter(new ByteArrayOutputStream(), RDFFormat.RDFJSON);
		later.getWriterConfig().set(RDFJSONWriterSettings.MAX_IN_MEMORY_BYTES, 128L);
		later.getWriterConfig().set(RDFJSONWriterSettings.MAX_SPILL_BYTES, 400L);
		later.startRDF();
		later.handleStatement(statement);
		later.endRDF();
	}
}
