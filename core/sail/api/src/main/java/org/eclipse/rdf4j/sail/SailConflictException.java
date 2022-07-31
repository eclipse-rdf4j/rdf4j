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
package org.eclipse.rdf4j.sail;

import org.eclipse.rdf4j.common.transaction.IsolationLevels;

/**
 * An exception thrown by some methods in Sail to indicate that a requested isolation level could not be fulfilled. Such
 * as when multiple conflicting {@link IsolationLevels#SERIALIZABLE} transactions overlap in time.
 */
public class SailConflictException extends SailException {

	private static final long serialVersionUID = 8616609700552763681L;

	public SailConflictException() {
		super();
	}

	public SailConflictException(String msg) {
		super(msg);
	}

	public SailConflictException(String msg, Throwable t) {
		super(msg, t);
	}

	public SailConflictException(Throwable t) {
		super(t);
	}

}
