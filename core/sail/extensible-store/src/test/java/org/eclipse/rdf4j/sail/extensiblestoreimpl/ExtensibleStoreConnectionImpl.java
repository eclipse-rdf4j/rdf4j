package org.eclipse.rdf4j.sail.extensiblestoreimpl;

import org.eclipse.rdf4j.sail.extensiblestore.ExtensibleStoreConnection;

public class ExtensibleStoreConnectionImpl extends ExtensibleStoreConnection<ExtensibleStoreImpl> {
	protected ExtensibleStoreConnectionImpl(ExtensibleStoreImpl sail) {
		super(sail);
	}
}
