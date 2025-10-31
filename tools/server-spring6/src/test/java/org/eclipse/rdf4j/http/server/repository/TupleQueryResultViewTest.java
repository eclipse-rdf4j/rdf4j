/*******************************************************************************
 * Copyright (c) 2023 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.http.server.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.resultio.sparqljson.SPARQLResultsJSONWriterFactory;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class TupleQueryResultViewTest {

	private static TupleQueryResultView view = TupleQueryResultView.getInstance();

	@Test
	public void testRender_QueryEvaluationError1() throws Exception {
		var request = new MockHttpServletRequest();
		var response = new MockHttpServletResponse();

		TupleQueryResult queryResult = mock(TupleQueryResult.class);
		when(queryResult.hasNext()).thenThrow(QueryEvaluationException.class);

		Map<String, Object> model = new HashMap<>();
		model.put(TupleQueryResultView.FACTORY_KEY, new SPARQLResultsJSONWriterFactory());
		model.put(TupleQueryResultView.QUERY_RESULT_KEY, queryResult);

		view.render(model, request, response);

		assertThat(response.getStatus()).isEqualTo(500);
	}
}
