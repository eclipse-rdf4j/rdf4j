package org.eclipse.rdf4j.sail.shacl;

import org.apache.commons.io.FileUtils;
import org.assertj.core.util.Files;
import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.sail.NotifyingSail;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;
import org.junit.After;
import org.junit.Before;

import java.io.File;
import java.io.IOException;

public class MultithreadedNativeStoreTest extends MultithreadedTest {

	File file;

	@After
	public void after() {
		try {
			FileUtils.deleteDirectory(file);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Before
	public void before() {
		file = Files.newTemporaryFolder();
	}

	@Override
	NotifyingSail getBaseSail() {
		NativeStore nativeStore = new NativeStore(file);
		try (NotifyingSailConnection connection = nativeStore.getConnection()) {
			connection.begin(IsolationLevels.NONE);
			connection.clear();
			connection.commit();
		}
		return nativeStore;
	}
}
