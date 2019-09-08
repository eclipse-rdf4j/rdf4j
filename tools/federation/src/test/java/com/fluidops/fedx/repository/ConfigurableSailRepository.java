package com.fluidops.fedx.repository;

import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.SailException;

/**
 * Specialized {@link SailRepository} that allows configuration of various
 * behaviors, e.g. fail after N operations.
 * 
 * @author Andreas Schwarte
 *
 */
public class ConfigurableSailRepository extends SailRepository implements RepositorySettings {
	int failAfter = -1; // fail after x operations, -1 means inactive
	boolean writable = true;
	
	/**
	 * A runnable that can be used to simulate latency
	 */
	Runnable latencySimulator = null;

	/**
	 * Counter for operations, only active if {@link #failAfter} is set
	 */
	AtomicInteger operationsCount = new AtomicInteger(0);

	public ConfigurableSailRepository(Sail sail, boolean writable) {
		super(sail);
		this.writable = writable;
	}

	/**
	 * @param nOperations fail after nOperations, -1 to deactivate
	 */
	@Override
	public void setFailAfter(int nOperations) {
		this.failAfter = nOperations;
	}
	
	@Override
	public void setWritable(boolean flag) {
		this.writable = flag;
	}

	@Override
	public void resetOperationsCounter() {
		this.operationsCount.set(0);
	}
				
	@Override
	public boolean isWritable() throws RepositoryException 	{
		return writable && super.isWritable();
	}

	@Override
	public SailRepositoryConnection getConnection()
			throws RepositoryException {
		try {
			return new ConfigurableSailRepositoryConnection(this, getSail().getConnection());
		} catch (SailException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void setLatencySimulator(Runnable runnable) {
		this.latencySimulator = runnable;
	}
}