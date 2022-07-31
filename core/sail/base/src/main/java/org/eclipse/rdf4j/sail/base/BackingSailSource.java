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
 * A Backing {@link SailSource} that does not respond to {@link #close()} {@link #prepare()} or {@link #flush()}. These
 * methods have no effect.
 *
 * @author James Leigh
 */
public abstract class BackingSailSource implements SailSource {

	@Override
	public SailSource fork() {
		return new SailSourceBranch(this);
	}

	@Override
	public void close() throws SailException {
		// no-op
	}

	@Override
	public void prepare() throws SailException {
		// no-op
	}

	@Override
	public void flush() throws SailException {
		// no-op
	}

}
