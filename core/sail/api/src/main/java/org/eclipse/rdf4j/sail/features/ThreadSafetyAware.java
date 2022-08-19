/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.features;

/**
 * An interface used to signal thread safety features of a sail or its related classes.
 */
public interface ThreadSafetyAware {

	/**
	 * A class may support concurrent reads from multiple threads against the same object. This ability may change based
	 * on an object's current state.
	 *
	 * @return true if this object supports concurrent reads
	 */
	boolean supportsConcurrentReads();

}
