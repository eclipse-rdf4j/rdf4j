/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl;

import org.apache.commons.io.IOUtils;
import org.eclipse.rdf4j.IsolationLevel;
import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.common.io.IOUtil;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.WriterConfig;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.shacl.results.ValidationReport;
import org.junit.AfterClass;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;

/**
 * @author Håvard Ottestad
 */
@RunWith(Parameterized.class)
abstract public class AbstractShaclTest {

	private static final Logger logger = LoggerFactory.getLogger(AbstractShaclTest.class);

	// @formatter:off
	// formatter doesn't understand that the trailing ) needs to be on a new line.

	private static final List<String> testCasePaths = Stream.of(
		"test-cases/and-or/datatypeNodeShape",
		"test-cases/class/and",
		"test-cases/class/and2",
		"test-cases/class/multipleClass",
		"test-cases/class/not",
		"test-cases/class/not2",
		"test-cases/class/notAnd",
		"test-cases/class/notNotSimple",
		"test-cases/class/simple",
		"test-cases/class/subclass",
		"test-cases/class/targetNode",
		"test-cases/class/validateTarget",
		"test-cases/class/validateTargetNot",
		"test-cases/complex/dcat",
		"test-cases/complex/foaf",
		"test-cases/datatype/not",
		"test-cases/datatype/notNodeShape",
		"test-cases/datatype/notNot",
		"test-cases/datatype/notTargetNode",
		"test-cases/datatype/simple",
		"test-cases/datatype/targetNode",
		"test-cases/datatype/targetNode2",
		"test-cases/datatype/targetObjectsOf",
		"test-cases/datatype/targetSubjectsOf",
		"test-cases/datatype/targetSubjectsOfSingle",
		"test-cases/datatype/validateTarget",
		"test-cases/deactivated/nodeshape",
		"test-cases/deactivated/or",
		"test-cases/deactivated/propertyshape",
		"test-cases/implicitTargetClass/simple",
		"test-cases/in/notAnd",
		"test-cases/in/notOr",
		"test-cases/in/simple",
		"test-cases/languageIn/simple",
		"test-cases/maxCount/not",
		"test-cases/maxCount/not2",
		"test-cases/maxCount/notNot",
		"test-cases/maxCount/simple",
		"test-cases/maxCount/targetNode",
		"test-cases/maxExclusive/simple",
		"test-cases/maxExclusiveMinLength/not",
		"test-cases/maxExclusiveMinLength/simple",
		"test-cases/maxInclusive/simple",
		"test-cases/maxLength/simple",
		"test-cases/minCount/not",
		"test-cases/minCount/simple",
		"test-cases/minCount/targetNode",
		"test-cases/minExclusive/dateVsTime",
		"test-cases/minExclusive/simple",
		"test-cases/minInclusive/simple",
		"test-cases/minLength/simple",
		"test-cases/nodeKind/not",
		"test-cases/nodeKind/simple",
		"test-cases/nodeKind/validateTarget",
		"test-cases/or/class",
		"test-cases/or/class2",
		"test-cases/or/classValidateTarget",
		"test-cases/or/datatype",
		"test-cases/or/datatype2",
		"test-cases/or/datatypeDifferentPaths",
		"test-cases/or/datatypeNodeShape",
		"test-cases/or/datatypeTargetNode",
		"test-cases/or/implicitAnd",
		"test-cases/or/inheritance",
		"test-cases/or/inheritance-deep",
		"test-cases/or/inheritanceNodeShape",
		"test-cases/or/maxCount",
		"test-cases/or/minCount",
		"test-cases/or/minCountDifferentPath",
		"test-cases/or/minCountMaxCount",
		"test-cases/or/multiple",
		"test-cases/or/nodeKindMinLength",
		"test-cases/or/nodeKindValidateTarget",
		"test-cases/pattern/multiple",
		"test-cases/pattern/simple",
		"test-cases/propertyShapeWithTarget/simple",
		"test-cases/uniqueLang/not",
		"test-cases/uniqueLang/simple"
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

		try {
			URL resource = AbstractShaclTest.class.getClassLoader().getResource(testCase + "/shacl.ttl");
			assert Objects.nonNull(resource) : "Could not find: " + testCase + "/shacl.ttl";
			URI uri = resource.toURI();
			assert Files.exists(Paths.get(uri)) : uri;

		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}

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

	@AfterClass
	public static void afterClass() {
		GlobalValidationExecutionLogging.loggingEnabled = false;
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
			IsolationLevel isolationLevel, boolean preloadWithDummyData) {

		if (!dataPath.endsWith("/")) {
			dataPath = dataPath + "/";
		}

		if (!shaclPath.endsWith("/")) {
			shaclPath = shaclPath + "/";
		}

		String shaclFile = shaclPath + "shacl.ttl";
		logger.debug(shaclFile);

		printFile(shaclFile);

		SailRepository shaclRepository = getShaclSail();

		try {
			Utils.loadShapeData(shaclRepository, shaclFile);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

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
				printCurrentState(shaclRepository);

				ran = true;
				printFile(name);

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
					logger.debug(sailException.getMessage());

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

	private static void printCurrentState(SailRepository shaclRepository) {
		if (!fullLogging) {
			return;
		}

		try (SailRepositoryConnection connection = shaclRepository.getConnection()) {

			if (connection.isEmpty()) {
				System.out.println("### CURRENT REPOSITORY STATE ###");
				System.out.println("   EMPTY!");
				System.out.println("################################################\n");
			} else {

				try (Stream<Statement> stream = Iterations.stream(connection.getStatements(null, null, null, false))) {
					LinkedHashModel model = stream.collect(Collectors.toCollection(LinkedHashModel::new));
					model.setNamespace("ex", "http://example.com/ns#");
					model.setNamespace(FOAF.PREFIX, FOAF.NAMESPACE);
					model.setNamespace(XMLSchema.PREFIX, XMLSchema.NAMESPACE);
					model.setNamespace(RDF.PREFIX, RDF.NAMESPACE);
					model.setNamespace(RDFS.PREFIX, RDFS.NAMESPACE);

					WriterConfig writerConfig = new WriterConfig();
					writerConfig.set(BasicWriterSettings.PRETTY_PRINT, true);
					writerConfig.set(BasicWriterSettings.INLINE_BLANK_NODES, true);
					System.out.println("### CURRENT REPOSITORY STATE ###");
					Rio.write(model, System.out, RDFFormat.TURTLE, writerConfig);
					System.out.println("################################################\n");

				}
			}

		}
	}

	private static void printFile(String filename) {
		if (!fullLogging) {
			return;
		}

		try {
			System.out.println("### " + filename + " ###");
			String s = IOUtils.toString(AbstractShaclTest.class.getClassLoader().getResourceAsStream(filename),
					"utf-8");
			s = Arrays
					.stream(s.split("\n"))
					.filter(a -> !a.trim().toLowerCase().startsWith("@prefix"))
					.filter(a -> !a.trim().toLowerCase().startsWith("@base"))
					.filter(a -> !a.trim().toUpperCase().startsWith("PREFIX "))
					.reduce((a, b) -> a + "\n" + b)
					.orElse("");
			System.out.println(s);
			System.out.println("################################################\n");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	static void runTestCaseSingleTransaction(String shaclPath, String dataPath, ExpectedResult expectedResult,
			IsolationLevel isolationLevel) {

		if (!dataPath.endsWith("/")) {
			dataPath = dataPath + "/";
		}

		if (!shaclPath.endsWith("/")) {
			shaclPath = shaclPath + "/";
		}

		SailRepository shaclRepository = getShaclSail();
		try {
			Utils.loadShapeData(shaclRepository, shaclPath + "shacl.ttl");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

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
				logger.debug(name);

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
				logger.debug(sailException.getMessage());

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
			IsolationLevel isolationLevel) {

		if (!dataPath.endsWith("/")) {
			dataPath = dataPath + "/";
		}

		if (!shaclPath.endsWith("/")) {
			shaclPath = shaclPath + "/";
		}

		SailRepository shaclRepository = getShaclSail();
		try {
			Utils.loadShapeData(shaclRepository, shaclPath + "shacl.ttl");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

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

		printResults(report);

		if (expectedResult == ExpectedResult.valid) {
			assertTrue(report.conforms());
		} else {
			assertFalse(report.conforms());
		}

	}

	private static void printResults(ValidationReport report) {
		if (!fullLogging) {
			return;
		}
		System.out.println("\n############################################");
		System.out.println("\tValidation Report\n");
		Model validationReport = report.asModel();

		WriterConfig writerConfig = new WriterConfig();
		writerConfig.set(BasicWriterSettings.PRETTY_PRINT, true);
		writerConfig.set(BasicWriterSettings.INLINE_BLANK_NODES, true);

		Rio.write(validationReport, System.out, RDFFormat.TURTLE, writerConfig);
		System.out.println("\n############################################");
	}

	private static void printResults(RepositoryException sailException) {
		ValidationReport validationReport = ((ShaclSailValidationException) sailException.getCause())
				.getValidationReport();
		printResults(validationReport);
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

	static boolean fullLogging = false;

	private static SailRepository getShaclSail() {

		ShaclSail shaclSail = new ShaclSail(new MemoryStore());
		SailRepository shaclRepository = new SailRepository(shaclSail);

		shaclSail.setLogValidationPlans(fullLogging);
		shaclSail.setCacheSelectNodes(true);
		shaclSail.setParallelValidation(true);
		shaclSail.setLogValidationViolations(fullLogging);
		shaclSail.setGlobalLogValidationExecution(fullLogging);

		shaclRepository.init();

		return shaclRepository;
	}

}
