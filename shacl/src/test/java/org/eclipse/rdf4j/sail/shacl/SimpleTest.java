package org.eclipse.rdf4j.sail.shacl;

import org.eclipse.rdf4j.common.io.IOUtil;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.shacl.validation.ShaclSail;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;

public class SimpleTest {


	@Test
	public void minCountSimple() {

		String caseName = "minCount/simple";
		runTests(caseName);

	}

	private void runTests(String caseName) {

		String path = "test-cases/" + caseName + "/";



		runTestCases(path, false);
		runTestCases(path, true);

	}

	private void runTestCases(String path, boolean valid) {

		String dataPath = valid ? path + "valid/" : path + "invalid/";

		for (int i = 0; i < 100; i++) {

			SailRepository shaclSail = new SailRepository(new ShaclSail(new MemoryStore(), getSailRepository(path + "shacl.ttl")));
			shaclSail.initialize();
			boolean exception = false;
			boolean ran = false;
			for (int j = 0; j < 100; j++) {

				String name = dataPath + "" + "case" + i + "/query" + j + ".rq";
				InputStream resourceAsStream = SimpleTest.class.getClassLoader().getResourceAsStream(name);
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
				if (valid) {
					assertFalse(exception);
				} else {
					assertTrue(exception);
				}
			}
		}
	}


	private SailRepository getSailRepository(String resourceName) {
		SailRepository sailRepository = new SailRepository(new MemoryStore());
		sailRepository.initialize();
		try (SailRepositoryConnection connection = sailRepository.getConnection()) {
			connection.add(SimpleTest.class.getClassLoader().getResourceAsStream(resourceName), "", RDFFormat.TURTLE);
		} catch (IOException | NullPointerException e) {
			System.out.println("Error reading: " + resourceName);
			throw new RuntimeException(e);
		}
		return sailRepository;
	}

}
