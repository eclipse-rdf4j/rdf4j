package org.eclipse.rdf4j.sail.shacl;

import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.shacl.results.ValidationReport;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Stream;

import static junit.framework.TestCase.assertTrue;

public class SerializableTest {

	@Test
	public void testMaxCount() throws IOException, InterruptedException {

		SailRepository repo = Utils.getInitializedShaclRepository("shaclMax.ttl", false);

		multithreadedMaxCountViolation(IsolationLevels.SNAPSHOT, repo);


		try (SailRepositoryConnection connection = repo.getConnection()) {
			connection.begin();

			ValidationReport revalidate = ((ShaclSailConnection) connection.getSailConnection()).revalidate();
			Rio.write(revalidate.asModel(), System.out, RDFFormat.TURTLE);

			assertTrue(revalidate.conforms());

			connection.commit();
		}


	}

	@Test
	public void testMaxCountSerializable() throws IOException, InterruptedException {

		SailRepository repo = Utils.getInitializedShaclRepository("shaclMax.ttl", false);

		multithreadedMaxCountViolation(IsolationLevels.SERIALIZABLE, repo);


		try (SailRepositoryConnection connection = repo.getConnection()) {
			connection.begin();

			ValidationReport revalidate = ((ShaclSailConnection) connection.getSailConnection()).revalidate();
			Rio.write(revalidate.asModel(), System.out, RDFFormat.TURTLE);

			assertTrue(revalidate.conforms());

			connection.commit();
		}


	}

	private void multithreadedMaxCountViolation(IsolationLevels isolationLevel, SailRepository repo) throws InterruptedException {
		CountDownLatch countDownLatch = new CountDownLatch(2);

		ValueFactory vf = SimpleValueFactory.getInstance();
		IRI iri = vf.createIRI("http://example.com/resouce1");


		Runnable runnable1 = () -> {

			try (SailRepositoryConnection connection = repo.getConnection()) {
				connection.begin(isolationLevel);
				connection.add(iri, RDF.TYPE, RDFS.RESOURCE);
				connection.add(iri, RDFS.LABEL, vf.createLiteral("a"));
				connection.add(iri, RDFS.LABEL, vf.createLiteral("b"));
				countDownLatch.countDown();
				try {
					countDownLatch.await();
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}

				try {
					connection.commit();
				}catch (Exception e){
					e.printStackTrace();
				}
			}


		};


		Runnable runnable2 = () -> {

			try (SailRepositoryConnection connection = repo.getConnection()) {
				connection.begin(isolationLevel);
				connection.add(iri, RDF.TYPE, RDFS.RESOURCE);
				connection.add(iri, RDFS.LABEL, vf.createLiteral("c"));
				connection.add(iri, RDFS.LABEL, vf.createLiteral("d"));
				countDownLatch.countDown();
				try {
					countDownLatch.await();
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}

				try {
					connection.commit();
				}catch (Exception e){
					e.printStackTrace();
				}			}


		};

		Thread thread1 = new Thread(runnable1);
		Thread thread2 = new Thread(runnable2);


		thread1.start();
		thread2.start();

		thread1.join();
		thread2.join();
	}


}
