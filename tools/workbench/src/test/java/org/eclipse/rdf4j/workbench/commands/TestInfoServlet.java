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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.rdf4j.model.vocabulary.RDF4J;
import org.eclipse.rdf4j.repository.manager.RepositoryInfo;
import org.eclipse.rdf4j.repository.manager.RepositoryManager;
import org.eclipse.rdf4j.workbench.util.WorkbenchRequest;
import org.junit.Before;
import org.junit.Test;

/**
 * @author dale
 */
public class TestInfoServlet {

	private final InfoServlet servlet = new InfoServlet();

	private RepositoryManager manager;

	private final RepositoryInfo info = new RepositoryInfo();

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		servlet.setRepositoryInfo(info);
		manager = mock(RepositoryManager.class);
		servlet.setRepositoryManager(manager);
	}

	/**
	 * Throwing exceptions for invalid repository ID's results in a 500 response code to the client. As seen in the bug
	 * report, some versions of Internet Explorer don't gracefully handle error responses during XSLT parsing.
	 *
	 * @see <a href="https://openrdf.atlassian.net/browse/SES-1770">SES-1770</a>
	 */
	@Test
	public final void testSES1770regression() throws Exception {
		when(manager.hasRepositoryConfig(null)).thenThrow(new NullPointerException());
		WorkbenchRequest req = mock(WorkbenchRequest.class);
		when(req.getParameter(anyString())).thenReturn(RDF4J.NIL.toString());
		HttpServletResponse resp = mock(HttpServletResponse.class);
		when(resp.getOutputStream()).thenReturn(mock(ServletOutputStream.class));
		servlet.service(req, resp, "");
	}

}
