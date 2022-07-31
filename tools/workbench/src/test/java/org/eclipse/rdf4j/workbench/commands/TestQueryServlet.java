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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;

import javax.servlet.ServletException;

import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.common.io.ResourceUtil;
import org.eclipse.rdf4j.workbench.exceptions.BadRequestException;
import org.eclipse.rdf4j.workbench.util.QueryStorage;
import org.eclipse.rdf4j.workbench.util.WorkbenchRequest;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Dale Visser
 */
public class TestQueryServlet {

	private static final String SHORT_QUERY = "select * {?s ?p ?o .}";

	private final QueryServlet servlet = new QueryServlet();

	private String longQuery;

	@Before
	public void setUp() throws IOException {
		longQuery = ResourceUtil.getString("long.rq");
	}

	@Test
	public final void testLongQuery() throws ServletException, IOException {
		assertThat(servlet.shouldWriteQueryCookie(longQuery)).isFalse();
	}

	@Test
	public final void testShortQuery() throws ServletException, IOException {
		assertThat(servlet.shouldWriteQueryCookie(SHORT_QUERY)).isTrue();
	}

	@Test
	public final void testNoQuery() throws ServletException, IOException {
		assertThat(servlet.shouldWriteQueryCookie(null)).isTrue();
	}

	@Test
	public void testGetQueryTextRefText() throws BadRequestException, RDF4JException {
		WorkbenchRequest request = mock(WorkbenchRequest.class);
		when(request.isParameterPresent(QueryServlet.QUERY)).thenReturn(true);
		when(request.getParameter(QueryServlet.QUERY)).thenReturn(SHORT_QUERY);
		when(request.isParameterPresent(QueryServlet.REF)).thenReturn(true);
		when(request.getParameter(QueryServlet.REF)).thenReturn("text");
		assertThat(servlet.getQueryText(request)).isEqualTo(SHORT_QUERY);
	}

	@Test
	public void testGetQueryNoQuery() throws BadRequestException, RDF4JException {
		WorkbenchRequest request = mock(WorkbenchRequest.class);
		when(request.isParameterPresent(QueryServlet.QUERY)).thenReturn(false);
		assertThat(servlet.getQueryText(request)).isEmpty();
	}

	@Test
	public void testGetQueryTextUnrecognizedRef() throws BadRequestException, RDF4JException {
		WorkbenchRequest request = mock(WorkbenchRequest.class);
		when(request.isParameterPresent(QueryServlet.QUERY)).thenReturn(true);
		when(request.getParameter(QueryServlet.QUERY)).thenReturn(SHORT_QUERY);
		when(request.isParameterPresent(QueryServlet.REF)).thenReturn(true);
		when(request.getParameter(QueryServlet.REF)).thenReturn("junk");
		assertThat(servlet.getQueryText(request)).isEqualTo(SHORT_QUERY);
	}

	@Test
	public void testGetQueryTextNoRef() throws BadRequestException, RDF4JException {
		WorkbenchRequest request = mock(WorkbenchRequest.class);
		when(request.isParameterPresent(QueryServlet.QUERY)).thenReturn(true);
		when(request.getParameter(QueryServlet.QUERY)).thenReturn(SHORT_QUERY);
		when(request.isParameterPresent(QueryServlet.REF)).thenReturn(false);
		assertThat(servlet.getQueryText(request)).isEqualTo(SHORT_QUERY);
	}

	@Test
	public void testGetQueryTextRefHash() throws BadRequestException, RDF4JException {
		WorkbenchRequest request = mock(WorkbenchRequest.class);
		when(request.isParameterPresent(QueryServlet.QUERY)).thenReturn(true);
		String hash = String.valueOf(longQuery.hashCode());
		when(request.getParameter(QueryServlet.QUERY)).thenReturn(hash);
		when(request.isParameterPresent(QueryServlet.REF)).thenReturn(true);
		when(request.getParameter(QueryServlet.REF)).thenReturn("hash");
		QueryServlet.substituteQueryCache(Collections.singletonMap(hash, longQuery));
		assertThat(servlet.getQueryText(request)).isEqualTo(longQuery);
	}

	@Test
	public void testGetQueryTextRefHashNoEntry() throws BadRequestException, RDF4JException {
		WorkbenchRequest request = mock(WorkbenchRequest.class);
		when(request.isParameterPresent(QueryServlet.QUERY)).thenReturn(true);
		String hash = String.valueOf(longQuery.hashCode());
		when(request.getParameter(QueryServlet.QUERY)).thenReturn(hash);
		when(request.isParameterPresent(QueryServlet.REF)).thenReturn(true);
		when(request.getParameter(QueryServlet.REF)).thenReturn("hash");
		QueryServlet.substituteQueryCache(Collections.<String, String>emptyMap());
		assertThat(servlet.getQueryText(request)).isEmpty();
	}

	@Test
	public void testGetQueryTextRefId() throws BadRequestException, RDF4JException {
		WorkbenchRequest request = mock(WorkbenchRequest.class);
		when(request.isParameterPresent(QueryServlet.QUERY)).thenReturn(true);
		when(request.getParameter(QueryServlet.QUERY)).thenReturn("test save name");
		when(request.isParameterPresent(QueryServlet.REF)).thenReturn(true);
		when(request.getParameter(QueryServlet.REF)).thenReturn("id");
		QueryStorage storage = mock(QueryStorage.class);
		when(storage.getQueryText(any(), anyString(), eq("test save name"))).thenReturn(longQuery);
		servlet.substituteQueryStorage(storage);
		assertThat(servlet.getQueryText(request)).isEqualTo(longQuery);
	}
}
