package org.eclipse.rdf4j.sail.shacl;

import org.eclipse.rdf4j.sail.NotifyingSail;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

public class MultithreadedMemoryStoreTest extends MultithreadedTest {
	@Override
	NotifyingSail getBaseSail() {
		return new MemoryStore();
	}
}
