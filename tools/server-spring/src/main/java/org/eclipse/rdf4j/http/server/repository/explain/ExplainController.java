/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.http.server.repository.explain;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.rdf4j.http.server.ProtocolUtil;
import org.eclipse.rdf4j.http.server.repository.RepositoryInterceptor;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.explanation.Explanation;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;
import org.springframework.web.servlet.view.json.MappingJackson2JsonView;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ExplainController extends AbstractController {

	private final ObjectMapper objectMapper = new ObjectMapper();

	public ExplainController() {
		setSupportedMethods(METHOD_POST);
	}

	@Override
	protected ModelAndView handleRequestInternal(@NotNull final HttpServletRequest request,
			@NotNull final HttpServletResponse response)
			throws Exception {

		ProtocolUtil.logRequestParameters(request);
		final ModelAndView responseModel = new ModelAndView(new MappingJackson2JsonView());

		try {
			final ExplainReqBody explainReqBody = objectMapper.readValue(request.getReader(), ExplainReqBody.class);
			final String query = explainReqBody.getQuery();
			final Explanation.Level level = explainReqBody.getLevel();

			final Repository repository = RepositoryInterceptor.getRepository(request);
			if (repository == null) {
				return handleError(response, responseModel, HttpStatus.BAD_REQUEST, "Repository is null");
			}

			try (final RepositoryConnection connection = RepositoryInterceptor.getRepositoryConnection(request)) {
				final Explanation explanation = connection.prepareQuery(query).explain(level);
				response.setStatus(HttpStatus.OK.value());
				responseModel.addObject("explanation", explanation.toString());
				return responseModel;
			} catch (final MalformedQueryException e) {
				return handleError(response, responseModel, HttpStatus.BAD_REQUEST,
						"Malformed query: " + e.getMessage());
			} catch (final RepositoryException e) {
				return handleError(response, responseModel, HttpStatus.BAD_REQUEST,
						"Repository error: " + e.getMessage());
			} catch (final Exception e) {
				return handleError(response, responseModel, HttpStatus.INTERNAL_SERVER_ERROR,
						"Explain error: " + e.getMessage());
			}

		} catch (final Exception e) {
			return handleError(response, responseModel, HttpStatus.BAD_REQUEST, "Invalid request: " + e.getMessage());
		}
	}

	private ModelAndView handleError(HttpServletResponse response, ModelAndView responseModel,
			HttpStatus status, String message) {
		response.setStatus(status.value());
		responseModel.addObject("message", message);
		return responseModel;
	}

	private static class ExplainReqBody {
		private final String query;
		private final Explanation.Level level;

		ExplainReqBody(@JsonProperty("query") String query, @JsonProperty("level") Explanation.Level level) {
			this.query = query;
			this.level = level;
		}

		public String getQuery() {
			return query;
		}

		public Explanation.Level getLevel() {
			return level;
		}
	}
}
