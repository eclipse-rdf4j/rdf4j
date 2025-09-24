/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/

package org.eclipse.rdf4j.sail;

public class InterruptedSailException extends SailException {

	public InterruptedSailException(String s) {
		super(s);
		assert Thread.currentThread().isInterrupted();
	}

	public InterruptedSailException() {
		super();
		assert Thread.currentThread().isInterrupted();
	}

	public InterruptedSailException(Throwable t) {
		super(t);
		assert Thread.currentThread().isInterrupted();
	}

	public InterruptedSailException(String msg, Throwable t) {
		super(msg, t);
		assert Thread.currentThread().isInterrupted();
	}
}
