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
package org.eclipse.rdf4j.workbench.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.eclipse.rdf4j.common.io.ResourceUtil;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.junit.Test;

/**
 * Regression test suite for {@link org.eclipse.rdf4j.workbench.util.PagedQuery PagedQuery}.
 *
 * @author Dale Visser
 */
public class TestPagedQuery {

	@Test
	public final void testSES1895regression() {
		PagedQuery pagedQuery = new PagedQuery("select * {?s ?p ?o } LIMIT 10", QueryLanguage.SPARQL, 100, 0);
		assertThat(pagedQuery.toString().toLowerCase()).isEqualTo("select * {?s ?p ?o } limit 10");
	}

	/**
	 * Check that inner query limits do not affect the paging parameters.
	 *
	 * @throws IOException
	 */
	@Test
	public final void testSES2307regression() throws IOException {
		PagedQuery pagedQuery = new PagedQuery(ResourceUtil.getString("ses2307.rq"), QueryLanguage.SPARQL, 100, 0);
		assertThat(pagedQuery.getLimit()).isEqualTo(100);
	}
}
