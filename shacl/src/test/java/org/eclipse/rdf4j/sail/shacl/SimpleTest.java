package org.eclipse.rdf4j.sail.shacl;

import org.eclipse.rdf4j.common.io.IOUtil;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
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

		runTestCasesSingleTransaction(path, false);
		runTestCasesSingleTransaction(path, true);

	}

	private void runTestCases(String path, boolean valid) {

		String dataPath = valid ? path + "valid/" : path + "invalid/";

		for (int i = 0; i < 100; i++) {

			SailRepository shaclSail = new SailRepository(new ShaclSail(new MemoryStore(), Utils.getSailRepository(path + "shacl.ttl")));
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

	private void runTestCasesSingleTransaction(String path, boolean valid) {

		String dataPath = valid ? path + "valid/" : path + "invalid/";

		for (int i = 0; i < 100; i++) {

			SailRepository shaclSailSingleTransaction = new SailRepository(new ShaclSail(new MemoryStore(), Utils.getSailRepository(path + "shacl.ttl")));
			shaclSailSingleTransaction.initialize();

			boolean exception = false;
			boolean ran = false;

			try (SailRepositoryConnection shaclSailSingleTransactionConnection = shaclSailSingleTransaction.getConnection()) {
				shaclSailSingleTransactionConnection.begin();

				for (int j = 0; j < 100; j++) {

					String name = dataPath + "" + "case" + i + "/query" + j + ".rq";
					InputStream resourceAsStream = SimpleTest.class.getClassLoader().getResourceAsStream(name);
					if (resourceAsStream == null) {
						continue;
					}

					ran = true;
					System.out.println(name);

					try {
						String query = IOUtil.readString(resourceAsStream);
						shaclSailSingleTransactionConnection.prepareUpdate(query).execute();

					} catch (IOException e) {
						e.printStackTrace();
					}
				}

				try {
					shaclSailSingleTransactionConnection.commit();

				} catch (RepositoryException sailException) {
					exception = true;
					System.out.println(sailException.getMessage());
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


}
