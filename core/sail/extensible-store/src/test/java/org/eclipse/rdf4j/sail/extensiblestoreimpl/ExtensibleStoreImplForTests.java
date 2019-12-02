package org.eclipse.rdf4j.sail.extensiblestoreimpl;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.extensiblestore.ExtensibleStore;
import org.eclipse.rdf4j.sail.extensiblestore.SimpleMemoryNamespaceStore;
import org.eclipse.rdf4j.sail.extensiblestore.WriteAheadLoggingInterface;

import java.util.concurrent.atomic.AtomicBoolean;

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

	@Override
	public WriteAheadLoggingInterface<NaiveHashSetDataStructure> writeAheadLoggingHandler() {
		return new WriteAheadLoggingInterface<NaiveHashSetDataStructure>() {

			AtomicBoolean begin = new AtomicBoolean(false);
			AtomicBoolean commit = new AtomicBoolean(false);

			@Override
			public void init(NaiveHashSetDataStructure dataStructure) {

			}

			@Override
			public void begin() {
				if (!begin.compareAndSet(false, true)) {
					throw new IllegalStateException("Begin was called more than once");
				}
				System.out.println("begin");
			}

			@Override
			public void commit() {
				if (!commit.compareAndSet(false, true)) {
					throw new IllegalStateException("Commit was called more than once");
				}
				System.out.println("commit");

			}

			@Override
			public void statementToAdd(Statement statement) {

			}

			@Override
			public void statementToRemove(Statement statement) {

			}
		};
	}

}
