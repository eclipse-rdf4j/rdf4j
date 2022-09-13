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
package org.eclipse.rdf4j.http.server.repository.transaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.fail;
import static org.eclipse.rdf4j.common.transaction.IsolationLevels.SNAPSHOT;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.UUID;

import org.eclipse.rdf4j.common.transaction.IsolationLevel;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.http.server.repository.RepositoryInterceptor;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.ModelAndView;

class TransactionStartControllerTest {
	private final static String REPO_ID = "test-repo";
	private final TransactionStartController controller = new TransactionStartController();

	private MockHttpServletRequest request;
	private MockHttpServletResponse response;

	@BeforeEach
	public void setUp() throws Exception {
		request = new MockHttpServletRequest();
		request.setRequestURI("/repositories/" + REPO_ID + "/transactions");
		request.setAttribute("repositoryID", REPO_ID);
		request.setAttribute("repository", new SailRepository(new MemoryStore()));
		request.setMethod(HttpMethod.POST.name());
		response = new MockHttpServletResponse();
	}

	@Test
	void createTransactionLocation_default() throws Exception {
		// Arrange
		controller.setExternalUrl(null);

		// Act
		final ModelAndView result = controller.handleRequest(request, response);

		// Assert
		assertThat(getHeaders(result).get("Location"))
				.startsWith("http://localhost/repositories/test-repo/transactions/");
	}

	@Test
	void createTransactionLocation_overrideExternalUrl() throws Exception {
		// Arrange
		controller.setExternalUrl("https://external-url.com/subpath/");

		// Act
		final ModelAndView result = controller.handleRequest(request, response);

		// Assert
		assertThat(getHeaders(result).get("Location")).startsWith(
				"https://external-url.com/subpath/repositories/test-repo/transactions/");
	}

	@Test
	void createTransactionLocation_overrideExternalUrl_withoutEndingSlash() throws Exception {
		// Arrange
		controller.setExternalUrl("https://external-url.com/subpath");

		// Act
		final ModelAndView result = controller.handleRequest(request, response);

		// Assert
		assertThat(getHeaders(result).get("Location")).startsWith(
				"https://external-url.com/subpath/repositories/test-repo/transactions/");
	}

	private HashMap<String, String> getHeaders(final ModelAndView result) {
		if (result == null) {
			return fail("Result is null");
		}

		return (HashMap<String, String>) result.getModel().get("headers");
	}

	@Test
	void positiveIsolationEnumsOldPath() {
		for (IsolationLevel level : IsolationLevels.values()) {
			request.addParameter("isolation-level", level.toString());

			assertThat(controller.getIsolationLevel(request).size() == 1);
		}

	}

	@Test
	void negativeIsolationEnumsOldPath() {
		request.addParameter("isolation-level", "GARBAGE");
		assertThatIllegalArgumentException().isThrownBy(() -> controller.getIsolationLevel(request));
	}

	@Test
	void createTransactionLocation_withPositiveIsolationOldPath() throws Exception {
		TransactionStartController controller = spy(TransactionStartController.class);
		Transaction tx = mock(Transaction.class);
		// Arrange
		controller.setExternalUrl(null);

		request.addParameter("isolation-level", "SNAPSHOT");
		Repository repository = RepositoryInterceptor.getRepository(request);

		when(controller.createTransaction(repository)).thenReturn(tx);
		when(tx.getID()).thenReturn(UUID.randomUUID());
		// Act
		controller.handleRequest(request, response);
		verify(tx).begin(SNAPSHOT);
	}

	@Test
	void createTransactionLocation_withNegativeIsolationOldPath() throws Exception {

		TransactionStartController controller = spy(TransactionStartController.class);
		Transaction tx = mock(Transaction.class);
		controller.setExternalUrl(null);

		request.addParameter("isolation-level", "GARBAGE");
		Repository repository = RepositoryInterceptor.getRepository(request);

		when(controller.createTransaction(repository)).thenReturn(tx);
		when(tx.getID()).thenReturn(UUID.randomUUID());

		assertThatIllegalArgumentException().isThrownBy(() -> controller.handleRequest(request, response));
	}

}
