package org.eclipse.rdf4j.sail.shacl;

import org.assertj.core.util.Files;
import org.eclipse.rdf4j.sail.NotifyingSail;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;
import org.junit.After;

import java.io.File;

public class MultithreadedNativeStoreTest extends MultithreadedTest {

	File file = Files.temporaryFolder();

	@After
	public void after() {

	}

	@Override
	NotifyingSail getBaseSail() {
		return new NativeStore(file);
	}
}
