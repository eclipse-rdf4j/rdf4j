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

/**
 * Indicates that a SAIL's transaction state (active or inactive) cannot be determined.
 *
 * @author Jeen Broekstra
 */
public class UnknownSailTransactionStateException extends SailException {

	private static final long serialVersionUID = 8616609700552763681L;

	public UnknownSailTransactionStateException(String msg) {
		super(msg);
	}

}
