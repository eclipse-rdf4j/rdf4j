package com.fluidops.fedx.repository;

/**
 * Interface for defining settings on a repository, e.g
 * {@link ConfigurableSailRepository}
 * 
 * @author Andreas Schwarte
 *
 */
public interface RepositorySettings {

	/**
	 * @param nOperations fail after nOperations, -1 to deactivate
	 */
	public void setFailAfter(int nOperations);

	/**
	 * 
	 * @param flag
	 */
	public void setWritable(boolean flag);

	public void resetOperationsCounter();

	/**
	 * 
	 * @param runnable a runnable that can be used to simulate latency, e.g. by
	 *                 letting the thread sleep
	 */
	public void setLatencySimulator(Runnable runnable);
}
