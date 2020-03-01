package org.eclipse.rdf4j.sail.shacl;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class W3cComplianceTest {

	private URL testCasePath;

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
	public void test() {

		runTest(testCasePath);

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

		System.out.println(Arrays.toString(testFiles.toArray()));

		return testFiles;

	}

	static class Manifest {

		List<URL> include;

		public Manifest(URL filename) {
			SailRepository sailRepository = new SailRepository(new MemoryStore());
			sailRepository.initialize();
			try (SailRepositoryConnection connection = sailRepository.getConnection()) {
				connection.add(filename, filename.toString(), RDFFormat.TURTLE);
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

	private void runTest(URL resourceName) {
		W3C_shaclTestValidate expected = new W3C_shaclTestValidate(resourceName);

		SailRepository sailRepository = Utils.getInitializedShaclRepository(resourceName);

		boolean failedShacl = false;
		try (SailRepositoryConnection connection = sailRepository.getConnection()) {
			connection.begin();
			connection.add(resourceName, "http://example.org/", RDFFormat.TURTLE);
			connection.commit();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (RepositoryException e) {
			failedShacl = e.toString().contains("Failed SHACL validation");
		}

		assertEquals(expected.conforms, !failedShacl);
	}

	class W3C_shaclTestValidate {

		W3C_shaclTestValidate(URL filename) {
			this.filename = filename.getPath();
			SailRepository sailRepository = Utils.getSailRepository(filename);
			try (SailRepositoryConnection connection = sailRepository.getConnection()) {
				try (Stream<Statement> stream = connection.getStatements(null, SHACL.CONFORMS, null).stream()) {
					conforms = stream
							.map(Statement::getObject)
							.map(o -> (Literal) o)
							.map(Literal::booleanValue)
							.findFirst()
							.get();
				}
			}
		}

		String filename;

		boolean conforms;
	}

}
