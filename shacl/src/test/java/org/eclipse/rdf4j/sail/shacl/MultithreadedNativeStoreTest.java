package org.eclipse.rdf4j.sail.shacl;

import org.apache.commons.io.FileUtils;
import org.assertj.core.util.Files;
import org.eclipse.rdf4j.sail.NotifyingSail;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;
import org.junit.After;

import java.io.File;
import java.io.IOException;

public class MultithreadedNativeStoreTest extends MultithreadedTest {

	File file = Files.newTemporaryFolder();

	@After
	public void after() {
		try {
			FileUtils.deleteDirectory(file);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	NotifyingSail getBaseSail() {
		return new NativeStore(file);
	}
}
