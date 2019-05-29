/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl;

import org.eclipse.rdf4j.IsolationLevel;
import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.common.io.IOUtil;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.shacl.results.ValidationReport;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;

/**
 * @author Håvard Ottestad
 */
@RunWith(Parameterized.class)
abstract public class AbstractShaclTest {
	// @formatter:off
	// formatter doesn't understand that the trailing ) needs to be on a new line.

	private static final List<String> testCasePaths = Stream.of(
		"test-cases/complex/dcat",
		"test-cases/complex/foaf",
		"test-cases/datatype/simple",
		"test-cases/datatype/targetNode",
		"test-cases/datatype/targetSubjectsOf",
		"test-cases/datatype/targetSubjectsOfSingle",
		"test-cases/datatype/targetObjectsOf",
		"test-cases/datatype/validateTarget",
		"test-cases/minLength/simple",
		"test-cases/maxLength/simple",
		"test-cases/pattern/simple",
		"test-cases/pattern/multiple",
		"test-cases/languageIn/simple",
		"test-cases/nodeKind/simple",
		"test-cases/nodeKind/validateTarget",
		"test-cases/minCount/simple",
		"test-cases/minCount/targetNode",
		"test-cases/maxCount/simple",
		"test-cases/maxCount/targetNode",
		"test-cases/or/multiple",
		"test-cases/or/inheritance",
		"test-cases/or/inheritance-deep",
		"test-cases/or/inheritance-deep-minCountMaxCount",
		"test-cases/or/inheritanceNodeShape",
		"test-cases/or/datatype",
		"test-cases/or/datatypeTargetNode",
		"test-cases/or/minCountMaxCount",
		"test-cases/or/maxCount",
		"test-cases/or/minCount",
		"test-cases/or/nodeKindMinLength",
		"test-cases/or/implicitAnd",
		"test-cases/or/datatypeDifferentPaths",
		"test-cases/or/class",
		"test-cases/or/classValidateTarget",
		"test-cases/or/datatype2",
		"test-cases/or/minCountDifferentPath",
		"test-cases/or/nodeKindValidateTarget",
		"test-cases/or/datatypeNodeShape",
		"test-cases/minExclusive/simple",
		"test-cases/minExclusive/dateVsTime",
		"test-cases/maxExclusive/simple",
		"test-cases/minInclusive/simple",
		"test-cases/maxInclusive/simple",
		"test-cases/implicitTargetClass/simple",
		"test-cases/class/simple",
		"test-cases/class/and",
		"test-cases/class/subclass",
		"test-cases/class/targetNode",
		"test-cases/class/multipleClass",
		"test-cases/class/validateTarget",
		"test-cases/deactivated/nodeshape",
		"test-cases/deactivated/or",
		"test-cases/deactivated/propertyshape",
		"test-cases/in/simple",
		"test-cases/uniqueLang/simple",
		"test-cases/propertyShapeWithTarget/simple",
		"test-cases/and-or/datatypeNodeShape"
	)
		.distinct()
		.sorted()
		.collect(Collectors.toList());

	// @formatter:on

	final String testCasePath;
	final String path;
	final ExpectedResult expectedResult;
	final IsolationLevel isolationLevel;

	public AbstractShaclTest(String testCasePath, String path, ExpectedResult expectedResult,
			IsolationLevel isolationLevel) {
		this.testCasePath = testCasePath;
		this.path = path;
		this.expectedResult = expectedResult;
		this.isolationLevel = isolationLevel;
	}

	@Parameterized.Parameters(name = "{2} - {1} - {3}")
	public static Collection<Object[]> data() {

		return getTestsToRun();
	}

	private static List<String> findTestCases(String testCase, String baseCase) {

		List<String> ret = new ArrayList<>();

		for (int i = 0; i < 100; i++) {
			String path = testCase + "/" + baseCase + "/case" + i;
			InputStream resourceAsStream = AbstractShaclTest.class.getClassLoader().getResourceAsStream(path);
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

	private static Collection<Object[]> getTestsToRun() {
		List<Object[]> ret = new ArrayList<>();

		for (String testCasePath : testCasePaths) {
			for (ExpectedResult baseCase : ExpectedResult.values()) {
				findTestCases(testCasePath, baseCase.name()).forEach(path -> {
					for (IsolationLevel isolationLevel : Arrays.asList(IsolationLevels.NONE, IsolationLevels.SNAPSHOT,
							IsolationLevels.SERIALIZABLE)) {
						Object[] temp = { testCasePath, path, baseCase, isolationLevel };
						ret.add(temp);
					}

				});
			}
		}

		return ret;
	}

	static void runTestCase(String shaclPath, String dataPath, ExpectedResult expectedResult,
			IsolationLevel isolationLevel, boolean preloadWithDummyData) throws Exception {

		if (!dataPath.endsWith("/")) {
			dataPath = dataPath + "/";
		}

		if (!shaclPath.endsWith("/")) {
			shaclPath = shaclPath + "/";
		}

		String shaclFile = shaclPath + "shacl.ttl";
		System.out.println(shaclFile);

		SailRepository shaclRepository = getShaclSail();

		Utils.loadShapeData(shaclRepository, shaclFile);

		boolean exception = false;
		boolean ran = false;

		if (preloadWithDummyData) {
			try (SailRepositoryConnection connection = shaclRepository.getConnection()) {
				connection.begin(isolationLevel);
				ValueFactory vf = connection.getValueFactory();
				connection.add(vf.createBNode(), vf.createIRI("http://example.com/jkhsdfiu3r2y9fjr3u0"),
						vf.createLiteral("auto-generated!"), vf.createBNode());
				connection.commit();
			}

		}

		for (int j = 0; j < 100; j++) {

			String name = dataPath + "query" + j + ".rq";
			try (InputStream resourceAsStream = AbstractShaclTest.class.getClassLoader().getResourceAsStream(name)) {
				if (resourceAsStream == null) {
					continue;
				}

				ran = true;
				System.out.println(name);

				try (SailRepositoryConnection connection = shaclRepository.getConnection()) {
					connection.begin(isolationLevel);
					String query = IOUtil.readString(resourceAsStream);
					connection.prepareUpdate(query).execute();
					connection.commit();
				} catch (RepositoryException sailException) {
					if (!(sailException.getCause() instanceof ShaclSailValidationException)) {
						throw sailException;
					}
					exception = true;
					System.out.println(sailException.getMessage());

					printResults(sailException);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

		}

		shaclRepository.shutDown();

		if (ran) {
			if (expectedResult == ExpectedResult.valid) {
				assertFalse("Expected transaction to succeed", exception);
			} else {
				assertTrue("Expected transaction to fail", exception);
			}
		}

	}

	static void runTestCaseSingleTransaction(String shaclPath, String dataPath, ExpectedResult expectedResult,
			IsolationLevel isolationLevel) throws Exception {

		if (!dataPath.endsWith("/")) {
			dataPath = dataPath + "/";
		}

		if (!shaclPath.endsWith("/")) {
			shaclPath = shaclPath + "/";
		}

		SailRepository shaclRepository = getShaclSail();
		Utils.loadShapeData(shaclRepository, shaclPath + "shacl.ttl");

		boolean exception = false;
		boolean ran = false;

		try (SailRepositoryConnection shaclSailConnection = shaclRepository.getConnection()) {
			shaclSailConnection.begin(isolationLevel);

			for (int j = 0; j < 100; j++) {

				String name = dataPath + "query" + j + ".rq";
				InputStream resourceAsStream = AbstractShaclTest.class.getClassLoader().getResourceAsStream(name);
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
				if (!(sailException.getCause() instanceof ShaclSailValidationException)) {
					throw sailException;
				}
				exception = true;
				System.out.println(sailException.getMessage());

				printResults(sailException);
			}
		}

		shaclRepository.shutDown();

		if (ran) {
			if (expectedResult == ExpectedResult.valid) {
				assertFalse(exception);
			} else {
				assertTrue(exception);
			}
		}

	}

	static void runTestCaseRevalidate(String shaclPath, String dataPath, ExpectedResult expectedResult,
			IsolationLevel isolationLevel) throws Exception {

		if (!dataPath.endsWith("/")) {
			dataPath = dataPath + "/";
		}

		if (!shaclPath.endsWith("/")) {
			shaclPath = shaclPath + "/";
		}

		SailRepository shaclRepository = getShaclSail();
		Utils.loadShapeData(shaclRepository, shaclPath + "shacl.ttl");

		ValidationReport report;

		try (SailRepositoryConnection shaclSailConnection = shaclRepository.getConnection()) {
			((ShaclSail) shaclRepository.getSail()).disableValidation();
			shaclSailConnection.begin(isolationLevel);

			for (int j = 0; j < 100; j++) {

				String name = dataPath + "query" + j + ".rq";
				InputStream resourceAsStream = AbstractShaclTest.class.getClassLoader().getResourceAsStream(name);
				if (resourceAsStream == null) {
					continue;
				}

				try {
					String query = IOUtil.readString(resourceAsStream);
					shaclSailConnection.prepareUpdate(query).execute();

				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			shaclSailConnection.commit();

			((ShaclSail) shaclRepository.getSail()).enableValidation();

			shaclSailConnection.begin();
			report = ((ShaclSailConnection) shaclSailConnection.getSailConnection()).revalidate();

			shaclSailConnection.commit();
		}

		shaclRepository.shutDown();

		if (expectedResult == ExpectedResult.valid) {
			assertTrue(report.conforms());
		} else {
			assertFalse(report.conforms());
		}

	}

	private static void printResults(RepositoryException sailException) {
		System.out.println("\n############################################");
		System.out.println("\tValidation Report\n");
		ShaclSailValidationException cause = (ShaclSailValidationException) sailException.getCause();
		Model validationReport = cause.validationReportAsModel();
		Rio.write(validationReport, System.out, RDFFormat.TURTLE);
		System.out.println("\n############################################");
	}

	String getShaclPath() {
		String shaclPath = testCasePath;

		if (!shaclPath.endsWith("/")) {
			shaclPath = shaclPath + "/";
		}

		return shaclPath + "shacl.ttl";
	}

	enum ExpectedResult {
		valid,
		invalid
	}

	private static SailRepository getShaclSail() {

		ShaclSail shaclSail = new ShaclSail(new MemoryStore());
		SailRepository shaclRepository = new SailRepository(shaclSail);

		shaclSail.setLogValidationPlans(true);
		shaclSail.setCacheSelectNodes(true);
		shaclSail.setParallelValidation(true);
		shaclSail.setLogValidationViolations(true);
		shaclSail.setGlobalLogValidationExecution(true);

		shaclRepository.init();

		return shaclRepository;
	}

}
