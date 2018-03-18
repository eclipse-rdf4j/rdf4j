/*******************************************************************************
 * Copyright (c) 2016 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl;

import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.common.io.IOUtil;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.shacl.planNodes.LoggingNode;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;

/**
 * @author Håvard Ottestad
 */
@RunWith(Parameterized.class)
public class ShaclTest {

	static final List<String> testCasePaths = Arrays.asList(
		"test-cases/datatype/simple",
		"test-cases/minCount/simple"
	);

	private final String testCasePath;
	private final String path;
	private final ExpectedResult expectedResult;

	public ShaclTest(String testCasePath, String path, ExpectedResult expectedResult) {
		this.testCasePath = testCasePath;
		this.path = path;
		this.expectedResult = expectedResult;
	}

	{
		LoggingNode.loggingEnabled = true;
	}



	@Parameterized.Parameters(name = "{2} - {1}")
	public static Collection<Object[]> data() {

		return getTestsToRun();
	}

	@Test
	public void test() {

		runTestCase(testCasePath, path, expectedResult);

	}

	@Test
	public void testSingleTransaction() {

		runTestCaseSingleTransaction(testCasePath, path, expectedResult);

	}

	static List<String> findTestCases(String testCase, String baseCase) {

		List<String> ret = new ArrayList<>();

		for (int i = 0; i < 1000; i++) {
			String path = testCase + "/" + baseCase + "/case" + i;
			InputStream resourceAsStream = ShaclTest.class.getClassLoader().getResourceAsStream(path);
			if (resourceAsStream != null) {
				ret.add(path);
				try {
					resourceAsStream.close();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}

		return ret;

	}


	static Collection<Object[]> getTestsToRun() {
		List<Object[]> ret = new ArrayList<>();


		for (String testCasePath : testCasePaths) {
			for (ExpectedResult baseCase : ExpectedResult.values()) {
				findTestCases(testCasePath, baseCase.name()).forEach(path -> {
					Object[] temp = {testCasePath, path, baseCase};
					ret.add(temp);

				});
			}
		}


		return ret;
	}





	void runTestCase(String shaclPath, String dataPath, ExpectedResult expectedResult) {

		if (!dataPath.endsWith("/")) {
			dataPath = dataPath + "/";
		}

		if (!shaclPath.endsWith("/")) {
			shaclPath = shaclPath + "/";
		}


		String shaclFile = shaclPath + "shacl.ttl";
		System.out.println(shaclFile);
		SailRepository shaclSail = new SailRepository(new ShaclSail(new MemoryStore(), Utils.getSailRepository(shaclFile)));
		shaclSail.initialize();

		boolean exception = false;
		boolean ran = false;

		for (int j = 0; j < 100; j++) {

			String name = dataPath + "query" + j + ".rq";
			InputStream resourceAsStream = ShaclTest.class.getClassLoader().getResourceAsStream(name);
			if (resourceAsStream == null) {
				continue;
			}

			ran = true;
			System.out.println(name);

			try (SailRepositoryConnection connection = shaclSail.getConnection()) {
				String query = IOUtil.readString(resourceAsStream);
				connection.prepareUpdate(query).execute();

			} catch (RepositoryException sailException) {
				exception = true;
				System.out.println(sailException.getMessage());

			} catch (IOException e) {
				e.printStackTrace();
			}

		}
		if (ran) {
			if (expectedResult == ExpectedResult.valid) {
				assertFalse(exception);
			} else {
				assertTrue(exception);
			}
		}

	}

	void runTestCaseSingleTransaction(String shaclPath, String dataPath, ExpectedResult expectedResult) {

		if (!dataPath.endsWith("/")) {
			dataPath = dataPath + "/";
		}

		if (!shaclPath.endsWith("/")) {
			shaclPath = shaclPath + "/";
		}

		SailRepository shaclSail = new SailRepository(new ShaclSail(new MemoryStore(), Utils.getSailRepository(shaclPath + "shacl.ttl")));
		shaclSail.initialize();

		boolean exception = false;
		boolean ran = false;

		try (SailRepositoryConnection shaclSailConnection = shaclSail.getConnection()) {
			shaclSailConnection.begin(IsolationLevels.SNAPSHOT);

			for (int j = 0; j < 100; j++) {

				String name = dataPath + "query" + j + ".rq";
				InputStream resourceAsStream = ShaclTest.class.getClassLoader().getResourceAsStream(name);
				if (resourceAsStream == null) {
					continue;
				}

				ran = true;
				System.out.println(name);

				try {
					String query = IOUtil.readString(resourceAsStream);
					shaclSailConnection.prepareUpdate(query).execute();

				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			try {
				shaclSailConnection.commit();

			} catch (RepositoryException sailException) {
				exception = true;
				System.out.println(sailException.getMessage());
			}
		}
		if (ran) {
			if (expectedResult == ExpectedResult.valid) {
				assertFalse(exception);
			} else {
				assertTrue(exception);
			}
		}

	}

	enum ExpectedResult {
		valid, invalid
	}


}
