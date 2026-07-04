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
 * Indicates that the current write operation did not succeed because the SAIL cannot be written to, it can only be read
 * from.
 *
 * @author James Leigh
 */
public class SailReadOnlyException extends SailException {

	private static final long serialVersionUID = 2439801771913652923L;

	public SailReadOnlyException(String msg) {
		super(msg);
	}

}
