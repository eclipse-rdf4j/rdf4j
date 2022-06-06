/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.DynamicModel;
import org.eclipse.rdf4j.model.impl.DynamicModelFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDF4J;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.shacl.ast.ContextWithShapes;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class W3cComplianceTest {

	private final URL testCasePath;

	public W3cComplianceTest(URL testCasePath) {
		this.testCasePath = testCasePath;
	}

	@Parameterized.Parameters(name = "{0}")
	public static Collection<URL> data() {

		ArrayList<URL> urls = new ArrayList<>(getTestFiles());
		urls.sort(Comparator.comparing(URL::toString));
		return urls;
	}

	@Ignore
	@Test
	public void test() throws IOException, InterruptedException {
		runTest(testCasePath);
	}

	@Test
	public void parsingTest() throws IOException, InterruptedException {
		runParsingTest(testCasePath);
	}

	private void runParsingTest(URL resourceName) throws IOException, InterruptedException {
		ShaclSail shaclSail = new ShaclSail(new MemoryStore());
		SailRepository sailRepository = new SailRepository(shaclSail);

		Utils.loadShapeData(sailRepository, resourceName, RDFFormat.TURTLE, RDF4J.SHACL_SHAPE_GRAPH);

		Model statements = extractShapesModel(shaclSail);

		sailRepository.shutDown();

		statements
				.filter(null, RDF.REST, null)
				.subjects()
				.forEach(s -> {
					int size = statements.filter(s, RDF.REST, null).objects().size();
					assertEquals(s + " has more than one rdf:rest", size, 1);
				});

//		System.out.println(AbstractShaclTest.modelToString(statements));

		assert !statements.isEmpty();

	}

	private Model extractShapesModel(ShaclSail shaclSail) throws InterruptedException {
		List<ContextWithShapes> shapes = shaclSail.getCachedShapes().getDataAndRelease();

		DynamicModel model = new DynamicModelFactory().createEmptyModel();

		shapes.forEach(shape -> shape.toModel(model));

		return model;
	}

	private static Set<URL> getTestFiles() {

		Set<URL> testFiles = new HashSet<>();

		Deque<URL> manifests = new ArrayDeque<>();
		manifests.add(W3cComplianceTest.class.getClassLoader().getResource("w3c/core/manifest.ttl"));

		while (!manifests.isEmpty()) {

			URL pop = manifests.pop();
			Manifest manifest = new Manifest(pop);
			if (manifest.include.isEmpty()) {
				testFiles.add(pop);
			} else {
				manifests.addAll(manifest.include);
			}

		}

		return testFiles;

	}

	static class Manifest {

		List<URL> include;

		public Manifest(URL filename) {
			SailRepository sailRepository = new SailRepository(new MemoryStore());
			try (SailRepositoryConnection connection = sailRepository.getConnection()) {
				connection.add(filename, filename.toString(), RDFFormat.TRIG);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

			try (SailRepositoryConnection connection = sailRepository.getConnection()) {
				try (Stream<Statement> stream = connection
						.getStatements(null,
								connection.getValueFactory()
										.createIRI("http://www.w3.org/2001/sw/DataAccess/tests/test-manifest#include"),
								null)
						.stream()) {
					include = stream
							.map(Statement::getObject)
							.map(Value::stringValue)
							.map(v -> {
								try {
									return new URL(v);
								} catch (MalformedURLException e) {

									throw new RuntimeException(e);
								}
							})
							.collect(Collectors.toList());
				}
			}

		}

	}

	private void runTest(URL resourceName) throws IOException, InterruptedException {
		W3C_shaclTestValidate expected = new W3C_shaclTestValidate(resourceName);

		ShaclSail shaclSail = new ShaclSail(new MemoryStore());
		shaclSail.setParallelValidation(false);
		SailRepository sailRepository = new SailRepository(shaclSail);

		Utils.loadShapeData(sailRepository, resourceName, RDFFormat.TURTLE, RDF4J.SHACL_SHAPE_GRAPH);

		Model statements = extractShapesModel(shaclSail);

		System.out.println(AbstractShaclTest.modelToString(statements, RDFFormat.TURTLE));

		boolean actualConforms = true;
		try (SailRepositoryConnection connection = sailRepository.getConnection()) {
			connection.begin();
			connection.add(resourceName, "http://example.org/", RDFFormat.TRIG);
			connection.commit();

			connection.begin();
//			ValidationReport revalidate = ((ShaclSailConnection) connection.getSailConnection()).revalidate();
//			actualConforms = revalidate.conforms();
			connection.commit();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (RepositoryException e) {
			if (e.getCause() instanceof ShaclSailValidationException) {
				Model statements1 = ((ShaclSailValidationException) e.getCause()).validationReportAsModel();
				actualConforms = statements1.contains(null, SHACL.CONFORMS,
						SimpleValueFactory.getInstance().createLiteral(true));

				System.out.println("\n######### Report ######### \n");
				Rio.write(statements1, System.out, RDFFormat.TRIG);
				System.out.println("\n##################### \n");
			}

		} finally {
			sailRepository.shutDown();
		}

		assertEquals(expected.conforms, actualConforms);
	}

	static class W3C_shaclTestValidate {

		W3C_shaclTestValidate(URL filename) {
			this.filename = filename.getPath();
			SailRepository sailRepository = Utils.getSailRepository(filename, RDFFormat.TURTLE);
			try (SailRepositoryConnection connection = sailRepository.getConnection()) {
				try (Stream<Statement> stream = connection.getStatements(null, SHACL.CONFORMS, null).stream()) {
					conforms = stream
							.map(Statement::getObject)
							.map(o -> (Literal) o)
							.map(Literal::booleanValue)
							.findFirst()
							.orElseThrow();
				}
			}
		}

		String filename;

		boolean conforms;
	}

}
