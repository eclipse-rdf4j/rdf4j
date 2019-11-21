package org.eclipse.rdf4j.sail.extensiblestoreimpl;

import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.extensiblestore.ExtensibleStore;

public class ExtensibleStoreImpl extends ExtensibleStore<DataStructure, NamespaceStore> {

	public ExtensibleStoreImpl() {
		namespaceStore = new NamespaceStore();
		dataStructure = new DataStructure();
		dataStructureInferred = new DataStructure();
	}

	@Override
	protected NotifyingSailConnection getConnectionInternal() throws SailException {
		return new ExtensibleStoreConnectionImpl(this);
	}

	@Override
	public boolean isWritable() throws SailException {
		return true;
	}
}
