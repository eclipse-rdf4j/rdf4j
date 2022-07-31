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
package org.eclipse.rdf4j.testsuite.rio;

public enum FailureMode {
	/**
	 * Helper constant to indicate that test failures for negative tests are being ignored.
	 */
	IGNORE_FAILURE(true),

	/**
	 * Helper constant to indicate that test failures for negative tests are being recognised.
	 */
	DO_NOT_IGNORE_FAILURE(false);

	private final boolean ignoreFailure;

	FailureMode(boolean ignoreFailure) {
		this.ignoreFailure = ignoreFailure;
	}

	public boolean ignoreFailure() {
		return ignoreFailure;
	}
}
