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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;

import org.eclipse.rdf4j.common.io.FileUtil;
import org.eclipse.rdf4j.http.protocol.Protocol;
import org.eclipse.rdf4j.http.server.ClientHTTPException;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Test which checks whether the correct exception is thrown when some query is processed through
 * {@link TransactionController TransactionController}
 *
 * @author Denitsa Stoyanova
 */
public class TestTransactionControllerErrorHandling {
	private MockHttpServletRequest request;
	private MockHttpServletResponse response;
	private final String repositoryID = "test-repo";
	private File dataDir;
	private Repository repository;

	@BeforeEach
	public void setUp() throws IOException {
		dataDir = Files.createTempDirectory(repositoryID).toFile();

		repository = new SailRepository(new NativeStore(dataDir));
		repository.init();

		request = new MockHttpServletRequest();
		response = new MockHttpServletResponse();
	}

	@AfterEach
	public void tearDown() throws Exception {
		repository.shutDown();
		FileUtil.deleteDir(dataDir);
	}

	@Test
	public void shouldThrowMalformedQueryExceptionForQuerySyntaxErrors() throws Exception {
		String testQuery = "#PREFIX ex: <http://example.com#>\n" +
				"select * where { \n" +
				"\t?s ex:data ?o .\n" +
				"}";

		Transaction txn = new Transaction(repository);
		ActiveTransactionRegistry.INSTANCE.register(txn);

		final UUID transactionId = txn.getID();

		request.setRequestURI("/repositories/" + repositoryID + "/transactions/" + transactionId);
		request.setPathInfo(repositoryID + "/transactions/" + transactionId);
		request.setMethod(HttpMethod.PUT.name());
		request.setParameter(Protocol.ACTION_PARAM_NAME, "QUERY");
		request.setContentType("application/sparql-query; charset=utf-8");
		request.setContent(testQuery.getBytes(StandardCharsets.UTF_8));

		TransactionController transactionController = new TransactionController();

		response = new MockHttpServletResponse();

		try {
			transactionController.handleRequestInternal(request, response);
			Assertions.fail("Exception must be thrown.");
		} catch (ClientHTTPException e) {
			Assertions.assertNotNull(e);
			Assertions.assertEquals("MALFORMED QUERY: org.eclipse.rdf4j.query.parser.sparql.ast.VisitorException: " +
					"QName 'ex:data' uses an undefined prefix", e.getMessage());
		} finally {
			txn.close();
			ActiveTransactionRegistry.INSTANCE.deregister(txn);
		}
	}
}
