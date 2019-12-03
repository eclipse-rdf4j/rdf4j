package org.eclipse.rdf4j.sail.extensiblestoreimpl;

import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.extensiblestore.ExtensibleStore;
import org.eclipse.rdf4j.sail.extensiblestore.SimpleMemoryNamespaceStore;

public class ExtensibleStoreImplForTests
		extends ExtensibleStore<NaiveHashSetDataStructure, SimpleMemoryNamespaceStore> {

	public ExtensibleStoreImplForTests() {
		namespaceStore = new SimpleMemoryNamespaceStore();
		dataStructure = new NaiveHashSetDataStructure();
		dataStructureInferred = new NaiveHashSetDataStructure();
	}

	@Override
	protected NotifyingSailConnection getConnectionInternal() throws SailException {
		return new ExtensibleStoreConnectionImplForTests(this);
	}

	@Override
	public boolean isWritable() throws SailException {
		return true;
	}

}
