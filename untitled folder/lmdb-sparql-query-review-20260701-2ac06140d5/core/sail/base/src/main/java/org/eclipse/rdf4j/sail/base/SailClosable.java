/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.base;

import org.eclipse.rdf4j.sail.SailException;

/**
 * Common interface to objects that throw {@link SailException} on close.
 *
 * @author James Leigh
 */
public interface SailClosable extends AutoCloseable {

	/**
	 * Closes this resource, relinquishing any underlying resources.
	 *
	 * @throws SailException if this resource cannot be closed
	 */
	@Override
	void close() throws SailException;
}
