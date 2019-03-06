package org.eclipse.rdf4j.sail.shacl;

import org.assertj.core.util.Files;
import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.common.io.IOUtil;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class DeadlockTest {

	@Test
	public void test() throws IOException {

		for (int i = 0; i < 10; i++) {

			String shaclPath = "complexBenchmark/";

			File dataDir = Files.newTemporaryFolder();
			dataDir.deleteOnExit();

			ShaclSail shaclSail = new ShaclSail(new NativeStore(dataDir));
			SailRepository shaclRepository = new SailRepository(shaclSail);
			shaclRepository.init();

			shaclSail.setParallelValidation(true);

			Utils.loadShapeData(shaclRepository, shaclPath + "shacl.ttl");

			try (SailRepositoryConnection connection = shaclRepository.getConnection()) {

				connection.begin(IsolationLevels.SNAPSHOT);
				connection.prepareUpdate(IOUtil.readString(DeadlockTest.class.getClassLoader().getResourceAsStream(shaclPath + "transaction1.qr"))).execute();
				connection.commit();

				connection.begin(IsolationLevels.SNAPSHOT);
				connection.prepareUpdate(IOUtil.readString(DeadlockTest.class.getClassLoader().getResourceAsStream(shaclPath + "transaction2.qr"))).execute();
				connection.commit();
			}
		}
	}

}
