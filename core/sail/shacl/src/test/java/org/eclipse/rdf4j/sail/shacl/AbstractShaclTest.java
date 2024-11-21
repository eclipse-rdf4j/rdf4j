/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl;

import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.update.UpdateAction;
import org.eclipse.rdf4j.common.transaction.IsolationLevel;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.common.transaction.QueryEvaluationMode;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.DynamicModel;
import org.eclipse.rdf4j.model.impl.DynamicModelFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.LinkedHashModelFactory;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.DASH;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDF4J;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.RSX;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.algebra.evaluation.util.ValueComparator;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.WriterConfig;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.shacl.ShaclSail.TransactionSettings.ValidationApproach;
import org.eclipse.rdf4j.sail.shacl.ast.ContextWithShape;
import org.eclipse.rdf4j.sail.shacl.results.ValidationReport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.parallel.Isolated;
import org.junit.jupiter.params.provider.Arguments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.topbraid.jenax.util.JenaUtil;
import org.topbraid.shacl.util.ModelPrinter;
import org.topbraid.shacl.validation.ValidationUtil;
import org.topbraid.shacl.vocabulary.SH;

import com.google.common.collect.Lists;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;

/**
 * @author Håvard Ottestad
 */
@Isolated("Because we are modifying the static CONTEXTS field in the ShaclValidator class")
//@Execution(CONCURRENT)
abstract public class AbstractShaclTest {

	private static final Logger logger = LoggerFactory.getLogger(AbstractShaclTest.class);

	public static final Set<IRI> SHAPE_GRAPHS = Set.of(RDF4J.SHACL_SHAPE_GRAPH, RDF4J.NIL,
			Values.iri("http://example.com/ns#shapesGraph1"));

	public static final String INITIAL_DATA_FILE = "initialData.trig";

	private static final Set<String> ignoredTestCases = Set.of(
			"test-cases/path/oneOrMorePath",
			"test-cases/nodeKind/oneOrMorePathComplex",
			"test-cases/nodeKind/zeroOrMorePathComplex",
			"test-cases/nodeKind/oneOrMorePathSimple",
			"test-cases/minCount/oneOrMorePath",
			"test-cases/path/zeroOrMorePath",
			"test-cases/minCount/zeroOrMorePath",
			"test-cases/path/zeroOrOnePath"

	);
	public static final List<IsolationLevels> ISOLATION_LEVELS = List.of(
			IsolationLevels.NONE,
			IsolationLevels.SNAPSHOT,
			IsolationLevels.SERIALIZABLE
	);

	boolean fullLogging = false;

	private final static List<TestCase> testCases = getTestsToRun();
	private final static List<Arguments> testsToRun = getTestsToRunWithoutIsolationLevel(testCases);
	private final static List<Arguments> testsToRunWithIsolationLevel = getTestsToRunWithIsolationLevel(testCases);

	private static List<Arguments> testCases() {
		return testsToRun;
	}

	private static List<Arguments> testsToRunWithIsolationLevel() {
		return testsToRunWithIsolationLevel;
	}

	static class TestCase {

		private Model shacl;
		private final String shaclData;
		private final ExpectedResult expectedResult;
		private final List<File> queries;
		private final String initialData;
		private final String testCasePath;
		private final String parentTestCasePath;

		public TestCase(String shacl, ExpectedResult expectedResult, List<File> queries, String initialData,
				String parentTestCasePath, String testCasePath) {
			this.shaclData = shacl;
			this.expectedResult = expectedResult;
			this.queries = queries;
			this.initialData = initialData;
			this.testCasePath = testCasePath.endsWith("/") ? testCasePath : testCasePath + "/";
			this.parentTestCasePath = parentTestCasePath;
		}

		public Model getShacl() {
			if (shacl == null) {
				try {
					shacl = Rio.parse(new StringReader(shaclData), RDFFormat.TRIG).unmodifiable();
				} catch (IOException e) {
					throw new IllegalStateException(e);
				}
			}
			return shacl;
		}

		public ExpectedResult getExpectedResult() {
			return expectedResult;
		}

		public List<File> getQueries() {
			return queries;
		}

		public boolean hasInitialData() {
			return initialData != null;
		}

		public String getInitialData() {
			return testCasePath + initialData;
		}

		public String getTestCasePath() {
			return testCasePath;
		}

		public String getParentTestCasePath() {
			return parentTestCasePath;
		}

		public String getShaclData() {
			return shaclData;
		}

		@Override
		public String toString() {
			return testCasePath;
		}
	}

	private static Stream<TestCase> findTestCases(String testCase, ExpectedResult baseCase) {
		String shacl = readShaclFile(testCase);

		URL resource = AbstractShaclTest.class.getClassLoader().getResource(testCase + "/" + baseCase + "/");
		if (resource == null) {
			return Stream.empty();
		}

		String[] testCases = Objects.requireNonNull(new File(resource.getFile()).list(),
				"Could not find test cases for: " + resource);

		return Arrays.stream(testCases)
				.filter(s -> !s.startsWith("."))
				.sorted()
				.map(caseName -> testCase + "/" + baseCase + "/" + caseName)
				.map(fullTestCasePath -> {
					URL fullTestCase = AbstractShaclTest.class.getClassLoader().getResource(fullTestCasePath);
					if (fullTestCase != null) {
						File[] files = new File(fullTestCase.getFile()).listFiles();
						if (files != null) {
							Optional<String> initialData = Arrays.stream(files)
									.map(File::getName)
									.filter(name -> name.equals(INITIAL_DATA_FILE))
									.findAny();
							List<File> queries = Arrays.stream(files)
									.filter(f -> f.getName().endsWith(".rq"))
									.sorted(Comparator.comparing(File::getName))
									.collect(Collectors.toList());
							return new TestCase(shacl, baseCase, queries, initialData.orElse(null), testCase,
									fullTestCasePath);
						}
					}
					return null;
				})
				.filter(Objects::nonNull);
	}

	private static String readShaclFile(String testCase) {
		try (InputStream resourceAsStream = AbstractShaclTest.class.getClassLoader()
				.getResourceAsStream(testCase + "/shacl.trig")) {
			assert Objects.nonNull(resourceAsStream) : "Could not find: " + testCase + "/shacl.trig";
			return IOUtils.toString(resourceAsStream, StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	private static List<Arguments> getTestsToRunWithIsolationLevel(List<TestCase> testCases) {

		return testCases.stream()
				.flatMap(testCase -> ISOLATION_LEVELS
						.stream()
						.map(isolationLevel -> arguments(testCase, isolationLevel))
				)
				.collect(Collectors.toList());
	}

	private static List<Arguments> getTestsToRunWithoutIsolationLevel(List<TestCase> testCases) {

		return testCases.stream()
				.map(Arguments::arguments)
				.collect(Collectors.toList());
	}

	private static List<TestCase> getTestsToRun() {
		URL testCasesUrl = AbstractShaclTest.class.getClassLoader().getResource("test-cases");
		File testCases = new File(Objects.requireNonNull(testCasesUrl).getFile());
		String baseTestCasesPath = testCases.getPath();

		List<File> mainTestCases = Arrays.stream(Objects.requireNonNull(testCases.listFiles()))
				.filter(s -> !s.getName().startsWith("."))
				.collect(Collectors.toList());

		List<String> innerTestCases = mainTestCases.stream()
				.flatMap(testCase -> Arrays.stream(Objects.requireNonNull(testCase.listFiles()))
						.filter(s -> !s.getName().startsWith("."))
						.map(File::getPath))
				.map(testCasePath -> testCasePath.replace(baseTestCasesPath, "test-cases"))
				.filter(testCasePath -> !ignoredTestCases.contains(testCasePath))
				.sorted()
				.collect(Collectors.toList());

		List<TestCase> individualTestCases = innerTestCases.stream()
				.flatMap(testCasePath -> Arrays.stream(ExpectedResult.values())
						.flatMap(expectedResult -> findTestCases(testCasePath, expectedResult))
				)
				.collect(Collectors.toList());

		return individualTestCases;
	}

	@BeforeAll
	static void beforeAll() throws IllegalAccessException {
		IRI[] shapesGraphs = SHAPE_GRAPHS.stream()
				.map(g -> {
					if (g.equals(RDF4J.NIL)) {
						return null;
					}
					return g;
				})
				.toArray(IRI[]::new);

		FieldUtils.writeDeclaredStaticField(ShaclValidator.class, "SHAPE_CONTEXTS", shapesGraphs, true);
	}

	@AfterAll
	static void afterAll() throws IllegalAccessException {
		FieldUtils.writeDeclaredStaticField(ShaclValidator.class, "SHAPE_CONTEXTS", new Resource[] {}, true);
	}

	@AfterEach
	void afterEach() {
		fullLogging = false;
	}

	void runTestCase(TestCase testCase, IsolationLevel isolationLevel, boolean preloadWithDummyData) {

		printTestCase(testCase);

		SailRepository shaclRepository = getShaclSail(testCase);

		boolean containsShapesGraphStatements = testCase.getShacl().contains(null, SHACL.SHAPES_GRAPH, null)
				|| testCase.getShacl().contains(null, RSX.shapesGraph, null);
		boolean onlyContainsRdf4jShapesGraph = testCase.getShacl().contexts().equals(Set.of(RDF4J.SHACL_SHAPE_GRAPH));

		if (!containsShapesGraphStatements) {
			Assertions.assertTrue(onlyContainsRdf4jShapesGraph);
			((ShaclSail) shaclRepository.getSail()).setShapesGraphs(Set.of(RDF4J.SHACL_SHAPE_GRAPH));
		}

		try {

			boolean exception = false;
			boolean ran = false;

			if (preloadWithDummyData) {
				try (SailRepositoryConnection connection = shaclRepository.getConnection()) {
					connection.begin(isolationLevel, ValidationApproach.Disabled);
					ValueFactory vf = connection.getValueFactory();
					connection.add(vf.createIRI("http://example.com/fewfkj9832ur8fh8whiu32hu"),
							vf.createIRI("http://example.com/jkhsdfiu3r2y9fjr3u0"),
							vf.createLiteral("123", XSD.INTEGER), vf.createBNode());
					try {
						connection.commit();
					} catch (RepositoryException sailException) {
						if (!(sailException.getCause() instanceof ShaclSailValidationException)) {
							throw sailException;
						}

					}
				}

			}

			List<File> testCaseQueries = testCase.getQueries();
			for (File queryFile : testCaseQueries) {
				try {
					String query = FileUtils.readFileToString(queryFile, StandardCharsets.UTF_8);

					printCurrentState(shaclRepository);

					ran = true;
					printFile(testCase.getTestCasePath() + queryFile.getName());

					try (SailRepositoryConnection connection = shaclRepository.getConnection()) {
						connection.begin(isolationLevel);
						connection.prepareUpdate(query).execute();
						printCurrentState(connection);
						connection.commit();
					} catch (RepositoryException sailException) {
						if (!(sailException.getCause() instanceof ShaclSailValidationException)) {
							throw sailException;
						}

						Assertions.assertEquals(testCaseQueries.get(testCaseQueries.size() - 1), queryFile,
								"Validation should only fail on the very last query");
						exception = true;
						logger.debug(sailException.getMessage());
						printResults(sailException);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}

			}

			if (ran) {

				if (testCase.expectedResult == ExpectedResult.valid) {
					Assertions.assertFalse(exception, "Expected transaction to succeed");
				} else {
					Assertions.assertTrue(exception, "Expected transaction to fail");
				}
			}
		} finally {
			shaclRepository.shutDown();
		}

	}

	private void printTestCase(TestCase testCase) {
		if (!fullLogging) {
			return;
		}

		System.out.println("################################################");
		System.out.println("## " + testCase.testCasePath + " ##");
		System.out.println("################################################\n");
		System.out.println("### shacl.ttl ###");
		System.out.println(removeLeadingPrefixStatements(testCase.getShaclData()));
		System.out.println("#####################\n\n");

	}

	void runWithShaclValidator(TestCase testCase) {

		SailRepository shapesRepo = new SailRepository(new MemoryStore());
		SailRepository dataRepo = new SailRepository(new MemoryStore());

		try {

			Utils.loadShapeData(shapesRepo, testCase.getShacl());
			if (testCase.hasInitialData()) {
				Utils.loadInitialData(dataRepo, testCase.getInitialData());
			}

			for (File queryFile : testCase.getQueries()) {
				try {
					String query = FileUtils.readFileToString(queryFile, StandardCharsets.UTF_8);

					logger.debug(queryFile.getName());

					try (SailRepositoryConnection connection = dataRepo.getConnection()) {
						connection.prepareUpdate(query).execute();
					} catch (MalformedQueryException e) {
						System.err.println(query + "\n");
						throw e;
					}

				} catch (IOException e) {
					e.printStackTrace();
				}

			}

			printTestCase(testCase);

			printCurrentState(dataRepo);

			ValidationReport validationReport1 = ShaclValidator.validate(dataRepo.getSail(), shapesRepo.getSail());

			Assertions.assertEquals(testCase.expectedResult == ExpectedResult.valid, validationReport1.conforms(),
					"Validation result does not match expected result");

			ValidationReport validationReport2 = ShaclValidator.validate(dataRepo.getSail(), shapesRepo.getSail());

			Assertions.assertEquals(testCase.expectedResult == ExpectedResult.valid, validationReport2.conforms(),
					"Validation result does not match expected result");

//			writeActualModelToExpectedModelForDevPurposes(testCase.testCasePath, validationReport1.asModel());

			testValidationReport(testCase.testCasePath, validationReport1.asModel());
			testValidationReport(testCase.testCasePath, validationReport2.asModel());

		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			try {
				shapesRepo.shutDown();
			} finally {
				dataRepo.shutDown();
			}
		}

	}

	private static void testValidationReport(String dataPath, Model validationReportActual) {
		try {
			InputStream resourceAsStream = getResourceAsStream(dataPath + "report.ttl");
			if (resourceAsStream == null) {
				logger.warn(dataPath + "report.ttl did not exist, attempting to create an empty file!");

				String file = Objects.requireNonNull(AbstractShaclTest.class.getClassLoader()
						.getResource(dataPath))
						.getFile()
						.replace("/target/test-classes/", "/src/test/resources/");
				boolean newFile = new File(file + "report.ttl").createNewFile();
				if (!newFile) {
					logger.error(dataPath + "report.ttl did not exist and could not create an empty file!");
				}
			}
			Model validationReportExpected = getModel(resourceAsStream);

			if (!Models.isomorphic(validationReportActual, validationReportExpected)) {
//				writeActualModelToExpectedModelForDevPurposes(dataPath, validationReportActual);

				String validationReportExpectedString = modelToString(validationReportExpected, RDFFormat.TURTLE);
				String validationReportActualString = modelToString(validationReportActual, RDFFormat.TURTLE);
				Assertions.assertEquals(validationReportExpectedString, validationReportActualString);
			}

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static InputStream getResourceAsStream(String dataPath) {
		return AbstractShaclTest.class.getClassLoader().getResourceAsStream(dataPath);
	}

	private static void writeActualModelToExpectedModelForDevPurposes(String dataPath, Model report)
			throws IOException {
		String file = Objects.requireNonNull(AbstractShaclTest.class.getClassLoader()
				.getResource(dataPath))
				.getFile()
				.replace("/target/test-classes/", "/src/test/resources/");
		File file1 = new File(file + "report.ttl");
		try (FileOutputStream fileOutputStream = new FileOutputStream(file1)) {
			IOUtils.write(modelToString(report, RDFFormat.TURTLE), fileOutputStream, StandardCharsets.UTF_8);
		}

	}

	void referenceImplementationTestCaseValidation(TestCase testCase) {

		// ignored test cases for shacl extensions
		if (testCase.testCasePath.startsWith("test-cases/class/complexTargetShape/")) {
			return;
		}
		if (testCase.testCasePath.startsWith("test-cases/class/complexTargetShape2/")) {
			return;
		}
		if (testCase.testCasePath.startsWith("test-cases/class/simpleTargetShape/")) {
			return;
		}
		if (testCase.testCasePath.startsWith("test-cases/datatype/notNodeShapeTargetShape/")) {
			return;
		}
		if (testCase.testCasePath.startsWith("test-cases/hasValue/targetShape")) {
			return;
		}
		if (testCase.testCasePath.startsWith("test-cases/datatype/notTargetShape/")) {
			return;
		}
		if (testCase.testCasePath.startsWith("test-cases/hasValueIn/")) {
			return;
		}

		// we support more variations for RDFS than the reference engine
		if (testCase.testCasePath.contains("subclass")) {
			return;
		}

		// uses rsx:targetShape
		if (testCase.testCasePath.startsWith("test-cases/qualifiedShape/complex/")) {
			return;
		}

		// uses rsx:targetShape
		if (testCase.testCasePath.startsWith("test-cases/complex/targetShapeAndQualifiedShape/")) {
			return;
		}

		// uses rsx:targetShape
		if (testCase.testCasePath.startsWith("test-cases/path/sequencePathTargetShape")) {
			return;
		}

		// sh:shapesGraph
		if (testCase.testCasePath.startsWith("test-cases/datatype/simpleNamedGraph/")) {
			return;
		}

		// rsx:DataAndShapesGraphLink
		if (testCase.testCasePath.startsWith("test-cases/minCount/unionDataset/")) {
			return;
		}

		// uses multiple named graphs
		if (testCase.testCasePath.startsWith("test-cases/minCount/simple/valid/case6/")) {
			return;
		}

		if (testCase.testCasePath.startsWith("test-cases/minCount/simple/invalid/case4/")) {
			return;
		}

		// the TopBraid SHACL API doesn't agree with other implementations on how sh:closed should work in a property
		// shape
		if (testCase.testCasePath.startsWith("test-cases/closed/propertyShape/")) {
			return;
		}

		// the TopBraid SHACL API doesn't agree with other implementations on how sh:closed should work in a property
		// shape
		if (testCase.testCasePath.startsWith("test-cases/closed/notPropertyShape/")) {
			return;
		}

		// the TopBraid SHACL API doesn't agree with other implementations on how multiple paths to the same target
		// should work
		if (testCase.testCasePath.startsWith("test-cases/nodeKind/simpleCompress/")) {
			return;
		}

		// the TopBraid SHACL API doesn't support multiple data graphs
		if (testCase.testCasePath.startsWith("test-cases/maxCount/simple/invalid/case4/")) {
			return;
		}

		printTestCase(testCase);

		Dataset shaclDataset = DatasetFactory.create();

		RDFDataMgr.read(shaclDataset, new StringReader(testCase.getShaclData()), "", RDFLanguages.TRIG);

		org.apache.jena.rdf.model.Model shacl = JenaUtil.createMemoryModel();

		Iterator<String> stringIterator = shaclDataset.listNames();
		while (stringIterator.hasNext()) {
			String namedGraph = stringIterator.next();
			shacl.add(shaclDataset.getNamedModel(namedGraph));
		}

		shacl.add(shaclDataset.getDefaultModel());

		checkShapesConformToW3cShaclRecommendation(shacl);

		org.apache.jena.rdf.model.Model data = JenaUtil.createMemoryModel();

		if (testCase.hasInitialData()) {
			try (InputStream resourceAsStream = getResourceAsStream(testCase.getInitialData())) {
				data.read(resourceAsStream, "", org.apache.jena.util.FileUtils.langTurtle);
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
		}

		for (File queryFile : testCase.getQueries()) {
			try {
				logger.debug(queryFile.getCanonicalPath());
				String query = FileUtils.readFileToString(queryFile, StandardCharsets.UTF_8);
				logger.debug(query);
				UpdateAction.parseExecute(query, data);
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}

		}

		org.apache.jena.rdf.model.Resource report = ValidationUtil.validateModel(data, shacl, false);

		org.apache.jena.rdf.model.Model model = report.getModel();
		model.setNsPrefix("sh", "http://www.w3.org/ns/shacl#");

		boolean conforms = report.getProperty(SH.conforms).getBoolean();

		try {
			InputStream resourceAsStream = getResourceAsStream(testCase.getTestCasePath() + "report.ttl");
			Model validationReportActual = extractValidationReport(getModel(resourceAsStream));

			Model validationReportExpected = Rio.parse(new StringReader(ModelPrinter.get().print(model)),
					RDFFormat.TRIG);

			validationReportExpected = extractValidationReport(validationReportExpected);

			if (testCase.expectedResult == ExpectedResult.valid) {
				Assertions.assertTrue(conforms,
						"Expected test case to conform\n" + modelToString(validationReportExpected, RDFFormat.TURTLE));
			} else {
				Assertions.assertFalse(conforms, "Expected test case to not conform\n"
						+ modelToString(validationReportExpected, RDFFormat.TURTLE));
			}

			for (Model validationReport : Arrays.asList(validationReportActual, validationReportExpected)) {
				validationReport.remove(null, RDF4J.TRUNCATED, null);
				validationReport.remove(null, RSX.dataGraph, null);
				validationReport.remove(null, RSX.shapesGraph, null);
				validationReport.remove(null, RSX.actualPairwisePath, null);

				// We don't have any default values for sh:resultMessage
				validationReport.remove(null, SHACL.RESULT_MESSAGE, null);

				// Remove the contents fo the SPARQL constraint since the reference implementation only seems to
				// add the Resource of the SPARQL constraint.
				ArrayList<Statement> sparqlConstraints = Lists
						.newArrayList(validationReport.getStatements(null, RDF.TYPE, SHACL.SPARQL_CONSTRAINT));
				for (Statement sparqlConstraint : sparqlConstraints) {
					validationReport.remove(sparqlConstraint.getSubject(), null, null);
				}

			}

			validationReportActual = new ValidationReportBnodeDuplicator(validationReportActual).getModel();
			validationReportExpected = new ValidationReportBnodeDuplicator(validationReportExpected).getModel();

			if (!Models.isomorphic(validationReportActual, validationReportExpected)) {

				String validationReportExpectedString = modelToString(validationReportExpected,
						RDFFormat.TURTLE);
				String validationReportActualString = modelToString(validationReportActual, RDFFormat.TURTLE);
				Assertions.assertEquals(validationReportExpectedString, validationReportActualString);
			}

		} catch (IOException e) {
			throw new IllegalStateException();
		}

	}

	private static Model getModel(InputStream resourceAsStream) throws IOException {
		try (resourceAsStream) {
			Model validationReportActual;

			if (resourceAsStream == null) {
				validationReportActual = new LinkedHashModel();
			} else {
				validationReportActual = Rio.parse(resourceAsStream, RDFFormat.TRIG);
			}
			return validationReportActual;
		}
	}

	private static void checkShapesConformToW3cShaclRecommendation(org.apache.jena.rdf.model.Model shacl) {
		org.apache.jena.rdf.model.Model w3cShacl = JenaUtil.createMemoryModel();
		try (InputStream resourceAsStream = getResourceAsStream("w3cshacl.ttl")) {
			w3cShacl.read(resourceAsStream, "", org.apache.jena.util.FileUtils.langTurtle);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}

		org.apache.jena.rdf.model.Resource report = ValidationUtil.validateModel(shacl, w3cShacl, false);

		boolean conforms = report.getProperty(SH.conforms).getBoolean();

		if (!conforms) {
			org.apache.jena.rdf.model.Model model = report.getModel();
			model.setNsPrefix("sh", "http://www.w3.org/ns/shacl#");

			System.out.println(ModelPrinter.get().print(model));

			Assertions.fail("SHACL does not conform to the W3C SHACL Recommendation");
		}
	}

	private void printCurrentState(SailRepository repository) {
		try (SailRepositoryConnection connection = repository.getConnection()) {
			printCurrentState(connection);
		}

	}

	private void printCurrentState(SailRepositoryConnection connection) {
		if (!fullLogging) {
			return;
		}

		if (connection.isEmpty()) {
			System.out.println("########### CURRENT REPOSITORY STATE ###########");
			System.out.println("\nEMPTY!\n");
			System.out.println("################################################\n\n");
		} else {

			try (Stream<Statement> stream = connection.getStatements(null, null, null, false).stream()) {
				LinkedHashModel model = stream.collect(Collectors.toCollection(LinkedHashModel::new));

				String prettyPrintedModel = modelToString(model, RDFFormat.TRIG);

				System.out.println("########### CURRENT REPOSITORY STATE ###########");
				System.out.println(prettyPrintedModel);
				System.out.println("################################################\n\n");

			}
		}

	}

	static String modelToString(Model model, RDFFormat format) {

		ArrayList<Statement> statements = new ArrayList<>(model);
		ValueComparator valueComparator = new ValueComparator();
		statements.sort(
				Comparator
						.comparing(Statement::getPredicate, valueComparator)
						.thenComparing(Statement::getSubject, valueComparator)
						.thenComparing(Statement::getObject, valueComparator)
		);

		model = new LinkedHashModel(statements);

		model.setNamespace("ex", "http://example.com/ns#");
		model.setNamespace(FOAF.NS);
		model.setNamespace(XSD.NS);
		model.setNamespace(RDF.NS);
		model.setNamespace(RDFS.NS);
		model.setNamespace(SHACL.NS);
		model.setNamespace(RDF.NS);
		model.setNamespace(RDFS.NS);
		model.setNamespace(RSX.NS);
		model.setNamespace(RDF4J.NS);

		WriterConfig writerConfig = new WriterConfig();
		writerConfig.set(BasicWriterSettings.PRETTY_PRINT, true);
		writerConfig.set(BasicWriterSettings.INLINE_BLANK_NODES, true);
		writerConfig.set(BasicWriterSettings.XSD_STRING_TO_PLAIN_LITERAL, true);

		StringWriter stringWriter = new StringWriter();

		Rio.write(model, stringWriter, format, writerConfig);

		return stringWriter.toString();
	}

	private static Model extractValidationReport(Model model) {

		Optional<Resource> subject = Models.subject(model.filter(null, RDF.TYPE, SHACL.VALIDATION_REPORT));
		if (subject.isPresent()) {
			return ModelExtractor.extract(model, subject.get(), s -> {
				if (s.getPredicate().equals(SHACL.SOURCE_SHAPE)) {
					return ModelExtractor.Decision.includeDontFollow;
				}

				return ModelExtractor.Decision.includeAndFollow;
			});
		} else {
			return model;
		}
	}

	static class ModelExtractor {

		enum Decision {
			includeAndFollow,
			includeDontFollow,
			exclude
		}

		static Model extract(Model model, Resource start, Function<Statement, Decision> decisionFunction) {
			DynamicModel emptyModel = new DynamicModelFactory().createEmptyModel();

			Set<Statement> breadthFirstSearchBuffer = new HashSet<>();
			model.getStatements(start, null, null).forEach(breadthFirstSearchBuffer::add);

			while (!breadthFirstSearchBuffer.isEmpty()) {
				Set<Statement> tempBuffer = new HashSet<>();
				for (Statement statement : breadthFirstSearchBuffer) {
					Decision decision = decisionFunction.apply(statement);

					switch (decision) {

					case includeAndFollow:
						boolean add = emptyModel.add(statement);
						if (add && statement.getObject() instanceof Resource) {
							model.getStatements((Resource) statement.getObject(), null, null).forEach(tempBuffer::add);
						}
						break;
					case includeDontFollow:
						emptyModel.add(statement);
						break;
					case exclude:
						break;
					}
				}

				breadthFirstSearchBuffer = tempBuffer;

			}

			return emptyModel;
		}

	}

	static class ValidationReportBnodeDuplicator {

		private final Model inputModel;
		private final Set<BNode> toRemove = new HashSet<>();
		private final Model toReturn = new DynamicModel(new LinkedHashModelFactory());

		public ValidationReportBnodeDuplicator(Model inputModel) {
			this.inputModel = inputModel;
		}

		Model getModel() {
			Resource subject = Models.subject(inputModel.filter(null, RDF.TYPE, SHACL.VALIDATION_REPORT)).get();
			traverse(subject, subject);
			for (BNode bNode : toRemove) {
				toReturn.remove(bNode, null, null);
				toReturn.remove(null, null, bNode);
			}
			return toReturn;
		}

		private void traverse(Resource subject, Resource override) {
			for (Statement statement : inputModel.getStatements(subject, null, null)) {
				Value object = statement.getObject();
				if (object.isResource()) {
					if (statement.getObject().isBNode()) {
						toRemove.add(((BNode) object));
						object = Values.bnode();
					}
					traverse(((Resource) statement.getObject()), (Resource) object);
				}

				toReturn.add(override, statement.getPredicate(), object);

			}

		}

	}

	private void printFile(String filename) {
		if (!fullLogging) {
			return;
		}

		try {
			System.out.println("### " + filename + " ###");
			String s = IOUtils.toString(
					Objects.requireNonNull(getResourceAsStream(filename)),
					StandardCharsets.UTF_8);

			s = removeLeadingPrefixStatements(s);

			System.out.println(s);
			System.out.println("################################################\n\n");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static String removeLeadingPrefixStatements(String s) {
		String[] splitByNewLine = s.split("\n");

		boolean skippingPrefixes = true;

		StringBuilder stringBuilder = new StringBuilder();
		for (String line : splitByNewLine) {
			if (skippingPrefixes) {
				if (!(line.trim().equals("") ||
						line.trim().toLowerCase().startsWith("@prefix") ||
						line.trim().toLowerCase().startsWith("@base") ||
						line.trim().toLowerCase().startsWith("prefix"))) {
					skippingPrefixes = false;
				}
			}

			if (!skippingPrefixes) {
				stringBuilder.append(line).append("\n");
			}

		}
		return stringBuilder.toString();
	}

	void runTestCaseSingleTransaction(TestCase testCase) {

		SailRepository shaclRepository = getShaclSail(testCase);

		try {
			boolean exception = false;
			boolean ran = false;
			Model validationReportActual = new LinkedHashModel();

			try (SailRepositoryConnection shaclSailConnection = shaclRepository.getConnection()) {
				shaclSailConnection.begin(IsolationLevels.NONE);

				for (File queryFile : testCase.getQueries()) {
					try {
						String query = FileUtils.readFileToString(queryFile, StandardCharsets.UTF_8);

						ran = true;
						logger.debug(queryFile.getName());

						try {
							shaclSailConnection.prepareUpdate(query).execute();
						} catch (MalformedQueryException e) {
							System.err.println(query + "\n");
							throw e;
						}

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

					validationReportActual = ((ShaclSailValidationException) sailException.getCause())
							.validationReportAsModel();
					printResults(sailException);
				}
			}

			if (ran) {
				if (testCase.expectedResult == ExpectedResult.valid) {
					Assertions.assertFalse(exception,
							"Expected validation to succeed for " + testCase.getTestCasePath());
				} else {
					Assertions.assertTrue(exception, "Expected validation to fail for " + testCase.getTestCasePath());
					testValidationReport(testCase.testCasePath, validationReportActual);

				}

			}
		} finally {
			shaclRepository.shutDown();
		}

	}

	void runTestCaseRevalidate(TestCase testCase, IsolationLevel isolationLevel) {

		SailRepository shaclRepository = getShaclSail(testCase);
		try {

			ValidationReport report = new ValidationReport(true);

			try (SailRepositoryConnection shaclSailConnection = shaclRepository.getConnection()) {
				shaclSailConnection.begin(isolationLevel, ValidationApproach.Disabled);

				for (File queryFile : testCase.getQueries()) {
					try {
						String query = FileUtils.readFileToString(queryFile, StandardCharsets.UTF_8);
						shaclSailConnection.prepareUpdate(query).execute();

					} catch (IOException e) {
						e.printStackTrace();
					}
				}

				// testing that bulk validation always validates all the data by committing the transaction with
				// validation disabled and then running an empty transaction with bulk validation
				shaclSailConnection.commit();

				shaclSailConnection.begin(ValidationApproach.Bulk);
//				shaclSailConnection.begin(ValidationApproach.Bulk, QueryEvaluationMode.MINIMAL_COMPLIANT);

				try {
					shaclSailConnection.commit();
				} catch (RepositoryException e) {
					if (e.getCause() instanceof ShaclSailValidationException) {
						report = ((ShaclSailValidationException) e.getCause()).getValidationReport();
					}
				}
			}

			printResults(report);

			if (testCase.getExpectedResult() == ExpectedResult.valid) {
				Assertions.assertTrue(report.conforms());
			} else {
				Assertions.assertFalse(report.conforms());
				testValidationReport(testCase.getTestCasePath(), report.asModel());
			}
		} finally {
			shaclRepository.shutDown();
		}

	}

	void runParsingTest(TestCase testCase) {

		// skip test case with shapes split between multiple graphs
		if (testCase.testCasePath.startsWith("test-cases/qualifiedShape/complex/")) {
			return;
		}

		SailRepository shaclRepository;
		try {
			shaclRepository = getShaclSail(testCase);
		} catch (Exception e) {
			System.err.println(testCase.getTestCasePath() + "shacl.trig");
			throw e;
		}
		try {

			List<ContextWithShape> shapes = ((ShaclSail) shaclRepository.getSail()).getCachedShapes()
					.getDataAndRelease();

			HashSet<Resource> cycleDetection = new HashSet<>();

			Model actual = new DynamicModelFactory().createEmptyModel();
			shapes.forEach(shape -> shape.toModel(actual, cycleDetection));

			Model expected = new LinkedHashModel(testCase.getShacl());

			// handle implicit targets in SHACL
			expected.filter(null, RDF.TYPE, RDFS.CLASS).forEach(s -> {
				if (expected.contains(s.getSubject(), RDF.TYPE, SHACL.PROPERTY_SHAPE)
						|| expected.contains(s.getSubject(), RDF.TYPE, SHACL.NODE_SHAPE)) {
					expected.add(s.getSubject(), SHACL.TARGET_CLASS, s.getSubject(), s.getContext());
				}
			});
			expected.remove(null, RDF.TYPE, RDFS.CLASS);

			// this helps with one test where the schema is in the shacl file
			expected.remove(null, RDFS.SUBCLASSOF, null);

			expected.remove(null, SHACL.SHAPES_GRAPH, null);
			expected.filter(null, RDF.TYPE, RSX.DataAndShapesGraphLink).forEach(s -> {
				expected.remove(s.getSubject(), null, null);
			});

			// we add inferred NodeShape and PropertyShape, easier to remove when comparing
			expected.remove(null, RDF.TYPE, SHACL.NODE_SHAPE);
			expected.remove(null, RDF.TYPE, SHACL.SHAPE);
			expected.remove(null, RDF.TYPE, SHACL.PROPERTY_SHAPE);
			actual.remove(null, RDF.TYPE, SHACL.NODE_SHAPE);
			actual.remove(null, RDF.TYPE, SHACL.SHAPE);
			actual.remove(null, RDF.TYPE, SHACL.PROPERTY_SHAPE);

			expected.remove(null, RDF.TYPE, DASH.AllObjectsTarget);
			expected.remove(null, RDF.TYPE, DASH.AllSubjectsTarget);
			actual.remove(null, RDF.TYPE, DASH.AllObjectsTarget);
			actual.remove(null, RDF.TYPE, DASH.AllSubjectsTarget);

			if (!Models.isomorphic(expected, actual)) {
				Assertions.assertEquals(modelToString(expected, RDFFormat.TRIG), modelToString(actual, RDFFormat.TRIG));
			}

		} catch (InterruptedException e) {
			throw new IllegalStateException(e);
		} finally {
			shaclRepository.shutDown();
		}

	}

	private void printResults(ValidationReport report) {
		if (!fullLogging) {
			return;
		}
		System.out.println("\n############################################");
		System.out.println("\tValidation Report\n");
		Model validationReport = report.asModel();

		validationReport.setNamespace(SHACL.NS);
		validationReport.setNamespace(XSD.NS);
		validationReport.setNamespace(RDF4J.NS);

		WriterConfig writerConfig = new WriterConfig();
		writerConfig.set(BasicWriterSettings.PRETTY_PRINT, true);
		writerConfig.set(BasicWriterSettings.INLINE_BLANK_NODES, true);

		Rio.write(validationReport, System.out, RDFFormat.TRIG, writerConfig);
		System.out.println("\n############################################");
	}

	private void printResults(RepositoryException sailException) {
		var shaclSailValidationException = (ShaclSailValidationException) sailException.getCause();
		ValidationReport validationReport = shaclSailValidationException.getValidationReport();
		printResults(validationReport);
	}

	SailRepository getShaclSail(TestCase testCase) {
		MemoryStore memoryStore = new MemoryStore();
		// Use strict evaluation for SHACL test suite
		// FIXME we should be able to set this directly on the ShaclSail (and let it delegate to its base sail), but no
		// decision has been made yet on where the setter for this sits (I'm not sure we want it at the level of the
		// Sail interface).
		memoryStore.setDefaultQueryEvaluationMode(QueryEvaluationMode.STRICT);

		ShaclSail shaclSail = new ShaclSail(memoryStore);
		SailRepository repository = new SailRepository(shaclSail);

		shaclSail.setLogValidationPlans(fullLogging);
		shaclSail.setCacheSelectNodes(true);
		shaclSail.setParallelValidation(false);
		shaclSail.setLogValidationViolations(fullLogging);
		shaclSail.setGlobalLogValidationExecution(fullLogging);
		shaclSail.setEclipseRdf4jShaclExtensions(true);
		shaclSail.setDashDataShapes(true);
		shaclSail.setPerformanceLogging(false);

		shaclSail.setShapesGraphs(SHAPE_GRAPHS);

		repository.init();

		try {
			Utils.loadShapeData(repository, testCase.getShacl());
			if (testCase.hasInitialData()) {
				Utils.loadInitialData(repository, testCase.getInitialData());
			}
		} catch (Exception e) {
			repository.shutDown();
			if (e instanceof RuntimeException) {
				throw ((RuntimeException) e);
			}
			throw new RuntimeException(e);
		}

		return repository;
	}

	void runWithAutomaticLogging(Runnable r) {
		LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
		ch.qos.logback.classic.Logger shaclPackageLogger = loggerContext.getLogger("org.eclipse.rdf4j.sail.shacl");

		Level originalLogLevel = shaclPackageLogger.getLevel();

		try {
			r.run();
		} catch (Throwable t) {
			fullLogging = true;

			shaclPackageLogger.setLevel(Level.DEBUG);

			System.out.println("\n##############################################");
			System.out.println("###### Re-running test with full logging #####");
			System.out.println("##############################################\n");

			r.run();
			throw t;
		} finally {
			fullLogging = false;
			shaclPackageLogger.setLevel(originalLogLevel);

		}
	}

	enum ExpectedResult {
		valid,
		invalid
	}

}
