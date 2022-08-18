/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.common.io;

import java.io.OutputStream;

/**
 * A ByteSink writes data as raw bytes directly to an {@link OutputStream}.
 *
 * @author Jeen Broekstra
 * @since 3.5.0
 */
public interface ByteSink extends Sink {

	/**
	 * get the {@link OutputStream} used by this {@link ByteSink}.
	 *
	 * @return an {@link OutputStream}
	 * @since 3.5.0
	 */
	OutputStream getOutputStream();

}
