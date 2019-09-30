/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.federated.repository;

/**
 * Interface for defining settings on a repository, e.g {@link ConfigurableSailRepository}
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
	 * @param runnable a runnable that can be used to simulate latency, e.g. by letting the thread sleep
	 */
	public void setLatencySimulator(Runnable runnable);
}
