/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.common.io;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * A wrapper for an output stream to avoid allowing libraries to close output streams unexpectedly using the
 * {@link #close()} method. Instead, they must be closed by the creator using {@link #doClose()}.
 *
 * @author Bart Hanssens
 */
public class UncloseableOutputStream extends FilterOutputStream {

	/**
	 * Constructor
	 *
	 * @param parent output stream
	 */
	public UncloseableOutputStream(OutputStream parent) {
		super(parent);
	}

	@Override
	public void close() throws IOException {
		// do nothing
	}

	/**
	 * Invoke close on FilterOutputStream parent class.
	 *
	 * @throws IOException
	 */
	public void doClose() throws IOException {
		super.close();
	}
}
