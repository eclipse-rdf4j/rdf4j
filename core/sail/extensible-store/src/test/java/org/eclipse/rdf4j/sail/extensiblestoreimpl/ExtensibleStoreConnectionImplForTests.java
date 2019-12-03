package org.eclipse.rdf4j.sail.extensiblestoreimpl;

import org.eclipse.rdf4j.sail.extensiblestore.ExtensibleStoreConnection;

public class ExtensibleStoreConnectionImplForTests extends ExtensibleStoreConnection<ExtensibleStoreImplForTests> {
	protected ExtensibleStoreConnectionImplForTests(ExtensibleStoreImplForTests sail) {
		super(sail);
	}
}
