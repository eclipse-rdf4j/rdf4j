/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.federated.optimizer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.federated.FedXConfig;
import org.eclipse.rdf4j.federated.FedXFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.manager.LocalRepositoryManager;
import org.eclipse.rdf4j.repository.manager.RepositoryManager;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.config.SailRepositoryConfig;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.memory.config.MemoryStoreConfig;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Filter optimizer test for queries involving {@link org.eclipse.rdf4j.query.algebra.Union} &
 * {@link org.eclipse.rdf4j.query.algebra.Difference}MINUS along with a single or multiple
 * {@link org.eclipse.rdf4j.query.algebra.Filter}s
 *
 * @author Tomas Kovachev t.kovachev1996@gmail.com
 */
@RunWith(Parameterized.class)
public class FilterOptimizerTest {

	private Repository standardRepository;
	private Repository federatedRepository;

	private final String memberDataOne = "INSERT DATA \n" +
			"{ <urn:test1> <urn:what1> 7000 . "
			+ "<urn:test> <urn:what> 5000 . "
			+ "<urn:tes22> <urn:what2> 7000 . "
			+ "<urn:test7> <urn:what3> 8000 . "
			+ "<urn:tes6414> <urn:what> 1000 . "
			+ "<urn:tes5> <urn:what2> 1000 . }";

	private final String memberDataTwo = "INSERT DATA \n" +
			"{ <urn:test3> <urn:what> 2000 . "
			+ "<urn:tes2> <urn:what1> 3500 . "
			+ "<urn:test1> <urn:what> 1500 . "
			+ "<urn:tes12> <urn:what2> 3500 . "
			+ "<urn:tes6> <urn:what3> 9000 . "
			+ "<urn:tes644> <urn:what> 9000 . "
			+ "<urn:tes4> <urn:what2> 3000 . }";

	private final String query;

	public FilterOptimizerTest(String query) {
		this.query = query;
	}

	@Parameterized.Parameters(name = "{0}")
	public static Collection<Object> data() {
		return Arrays.asList(
				new Object[] {
						"SELECT * WHERE {\n"
								+ "        ?s <urn:what1> ?value .\n"
								+ "        ?x <urn:what> ?value .\n"
								+ "        filter (?value > 4000) \n"
								+ "}",
						"SELECT * WHERE {\n"
								+ "        ?s <urn:what1> ?value .\n"
								+ "        OPTIONAL { ?x <urn:what2> ?valu1 } .\n"
								+ "        filter (?value > 4000) \n"
								+ "        filter (?valu1 > 4000) \n"
								+ "}",
						"SELECT * WHERE {\n"
								+ "    {\n"
								+ "        ?s <urn:what1> ?value .\n"
								+ "    } UNION {\n"
								+ "        ?s <urn:what> ?value .\n"
								+ "    } \n"
								+ "        filter (?value > 4000) \n"
								+ "}",
						"SELECT * WHERE {\n"
								+ "    bind (<urn:test1> as ?s ) {\n"
								+ "        ?s <urn:what1> ?value .\n"
								+ "    } UNION {\n"
								+ "        ?s <urn:what> ?value .\n"
								+ "    } \n"
								+ "        filter (isIRI(?s)) \n"
								+ "}",
						"SELECT * WHERE {\n"
								+ "    values ?s {<urn:test1>} {\n"
								+ "        ?s <urn:what1> ?value .\n"
								+ "    } UNION {\n"
								+ "        ?s <urn:what> ?value .\n"
								+ "    } \n"
								+ "        filter (isIRI(?s)) \n"
								+ "}",
						"SELECT * WHERE {\n"
								+ "    bind (<urn:test1> as ?s ) {\n"
								+ "        ?s <urn:what1> ?value .\n"
								+ "    } UNION {\n"
								+ "        ?s <urn:what> ?value .\n"
								+ "    } \n"
								+ "        filter (isblank(?s)) \n"
								+ "        filter (?value > 4000) \n"
								+ "}",
						"SELECT * WHERE {\n"
								+ "    {\n"
								+ "        ?s <urn:what1> ?value .\n"
								+ "        {\n    "
								+ "            ?y <urn:what2> ?value1 .\n"
								+ "        } UNION {\n"
								+ "            ?x <urn:what3> ?value1 .\n"
								+ "        } \n"
								+ "    } UNION {\n"
								+ "        ?s <urn:what> ?value .\n"
								+ "    } \n"
								+ "        filter (?value > 4000) \n"
								+ "        filter (?value1 > 4000) \n"
								+ "}",
						"SELECT * WHERE {\n"
								+ "    {\n"
								+ "        ?s <urn:what1> ?value .\n"
								+ "        {\n    "
								+ "            ?y <urn:what2> ?value1 .\n"
								+ "        } MINUS {\n"
								+ "            ?x <urn:what3> ?value1 .\n"
								+ "        } \n"
								+ "    } UNION {\n"
								+ "        ?s <urn:what> ?value .\n"
								+ "    } \n"
								+ "        filter (?value > 4000) \n"
								+ "        filter (?value1 > 4000) \n"
								+ "}",
						"SELECT * WHERE {\n"
								+ "    {\n"
								+ "        ?s <urn:what1> ?value .\n"
								+ "    } UNION {\n"
								+ "        ?s <urn:what> ?value .\n"
								+ "    } \n"
								+ "    {\n    "
								+ "        ?y <urn:what2> ?value1 .\n"
								+ "    } UNION {\n"
								+ "        ?x <urn:what3> ?value1 .\n"
								+ "    } \n"
								+ "        filter (?value > 4000) \n"
								+ "        filter (?value1 > 4000) \n"
								+ "}",
						"SELECT * WHERE {\n"
								+ "    {\n"
								+ "        ?s <urn:what1> ?value .\n"
								+ "    } UNION {\n"
								+ "        ?s <urn:what> ?value .\n"
								+ "    } \n"
								+ "        filter (?value > 4000) \n"
								+ "        filter (?value < 7000) \n"
								+ "}",
						"SELECT * WHERE {\n"
								+ "    {\n"
								+ "         {?s <urn:what1> ?value .}\n"
								+ "       UNION \n"
								+ "         { ?s <urn:what2> ?value . } \n"
								+ "    } UNION {\n"
								+ "        ?s <urn:what> ?value .\n"
								+ "    } \n"
								+ "        filter (?value > 4000) \n"
								+ "}",
						"SELECT * WHERE {\n"
								+ "    {\n"
								+ "        ?s <urn:what> ?value .\n"
								+ "    } UNION {\n"
								+ "         ?x <urn:what1> ?value .\n"
								+ "         ?x <urn:what3> ?value .  \n"
								+ "    } \n"
								+ "        filter (?value > 4000) \n"
								+ "}",
						"SELECT * WHERE {\n"
								+ "    {\n"
								+ "        ?s <urn:what> ?value .\n"
								+ "        OPTIONAL {\n"
								+ "           ?x <urn:what3> ?value1 .\n"
								+ "        } \n"
								+ "    } UNION {\n"
								+ "         ?x <urn:what1> ?value .\n"
								+ "    } \n"
								+ "        filter (?value > 4000) \n"
								+ "        filter (?value1 > 4000) \n"
								+ "}",
						"SELECT * WHERE {\n"
								+ "    {\n"
								+ "        ?s <urn:what> ?value .\n"
								+ "        ?s <urn:what2> ?value2 .\n"
								+ "        OPTIONAL {\n"
								+ "           ?x <urn:what3> ?value1 .\n"
								+ "        } \n"
								+ "    } UNION {\n"
								+ "         ?x <urn:what1> ?value .\n"
								+ "    } \n"
								+ "        filter (?value > 4000) \n"
								+ "        filter (?value1 > 4000) \n"
								+ "}",
						"SELECT * WHERE {\n"
								+ "    {\n"
								+ "        OPTIONAL {\n"
								+ "           ?x <urn:what3> ?value1 .\n"
								+ "        } \n"
								+ "    } UNION {\n"
								+ "         ?x <urn:what1> ?value .\n"
								+ "    } \n"
								+ "        filter (?value > 4000) \n"
								+ "        filter (?value1 > 4000) \n"
								+ "}",
						"SELECT * WHERE {\n"
								+ "    {\n"
								+ "        OPTIONAL {\n"
								+ "           ?x <urn:what3> ?value1 .\n"
								+ "        } \n"
								+ "    } UNION {\n"
								+ "        OPTIONAL {\n"
								+ "           ?x <urn:what1> ?value .\n"
								+ "        } \n"
								+ "    } \n"
								+ "        filter (?value > 4000) \n"
								+ "        filter (?value1 > 4000) \n"
								+ "}",
						"SELECT * WHERE {\n"
								+ "    {\n"
								+ "        ?s <urn:what> ?value .\n"
								+ "    } UNION {\n"
								+ "         ?x <urn:what1> ?value .\n"
								+ "        OPTIONAL {\n"
								+ "           ?s <urn:what2> ?value1 .\n"
								+ "        } \n"
								+ "    } \n"
								+ "        filter (?value > 4000) \n"
								+ "        filter (?value1 > 4000) \n"
								+ "}",
						"SELECT * WHERE {\n"
								+ "    {\n"
								+ "        ?s <urn:what> ?value .\n"
								+ "        OPTIONAL {\n"
								+ "           ?x <urn:what3> ?value1 .\n"
								+ "        } \n"
								+ "    } UNION {\n"
								+ "         ?x <urn:what1> ?value .\n"
								+ "    } \n"
								+ "        filter (?value > 4000) \n"
								+ "        filter (?value1 > 4000) \n"
								+ "}",
						"SELECT * WHERE {\n"
								+ "    {\n"
								+ "        ?s <urn:what> ?value .\n"
								+ "        filter (?value > 3000) \n"
								+ "    } UNION {\n"
								+ "         ?x <urn:what1> ?value .\n"
								+ "    } \n"
								+ "        filter (?value > 4000) \n"
								+ "}",
						"SELECT * WHERE {\n"
								+ "    {\n"
								+ "        ?s <urn:what1> ?value .\n"
								+ "    } UNION {\n"
								+ "         ?x <urn:what> ?value .\n"
								+ "        filter (?value > 3000) \n"
								+ "    } \n"
								+ "        filter (?value > 4000) \n"
								+ "}",
						"SELECT * WHERE {\n"
								+ "    {\n"
								+ "        ?s <urn:what> ?value .\n"
								+ "        filter (?value > 3000) \n"
								+ "    } UNION {\n"
								+ "         ?x <urn:what1> ?value .\n"
								+ "        filter (?value > 3000) \n"
								+ "    } \n"
								+ "        filter (?value > 4000) \n"
								+ "}"
				});
	}

	@Rule
	public TemporaryFolder tmpFolder = new TemporaryFolder();

	@Before
	public void setup() throws IOException {
		RepositoryManager manager = new LocalRepositoryManager(new File(tmpFolder.getRoot().getAbsolutePath()));
		federatedRepository = createFedXRepo(manager);
		addRepoConfig(manager, "standard");
		createStandardRepository();
		importData(memberDataOne, standardRepository);
		importData(memberDataTwo, standardRepository);
	}

	public Repository createFedXRepo(RepositoryManager repoManager) {
		FedXConfig config = new FedXConfig().withEnableMonitoring(true)
				.withLogQueryPlan(true)
				.withIncludeInferredDefault(true);
		String repo1ID = "one";
		String repo2ID = "two";
		addRepoConfig(repoManager, repo1ID);
		importData(memberDataOne, repoManager.getRepository(repo1ID));
		addRepoConfig(repoManager, repo2ID);
		importData(memberDataTwo, repoManager.getRepository(repo2ID));
		return FedXFactory.newFederation()
				.withRepositoryResolver(repoManager)
				.withResolvableEndpoint(repo1ID, true)
				.withResolvableEndpoint(repo2ID)
				.withConfig(config)
				.create();
	}

	private void addRepoConfig(RepositoryManager repoManager, String repoID) {
		RepositoryConfig repoConfig = new RepositoryConfig();
		repoConfig.setID(repoID);
		SailRepositoryConfig sailConfig = new SailRepositoryConfig();
		sailConfig.setSailImplConfig(new MemoryStoreConfig());
		repoConfig.setRepositoryImplConfig(sailConfig);
		repoManager.addRepositoryConfig(repoConfig);
	}

	private void createStandardRepository() {
		this.standardRepository = new SailRepository(new MemoryStore());
	}

	private void importData(String data, Repository repository) {
		repository.init();
		try (RepositoryConnection connection = repository.getConnection()) {
			connection.prepareUpdate(data).execute();
		}
	}

	@Test
	public void testFedXQuery() {
		List<BindingSet> actual;
		List<BindingSet> expected;
		try (RepositoryConnection connection = federatedRepository.getConnection();
				RepositoryConnection standardConn = standardRepository.getConnection()) {
			actual = connection.prepareTupleQuery(query).evaluate().stream().collect(Collectors.toList());
			expected = standardConn.prepareTupleQuery(query).evaluate().stream().collect(Collectors.toList());
		}
		assertEquals(expected.size(), actual.size());
		assertTrue(actual.containsAll(expected));
		assertTrue(expected.containsAll(actual));
	}
}
