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

import java.io.Writer;

/**
 * A CharSink writes data as characters to a {@link Writer}.
 *
 * @author Jeen Broekstra
 * @since 3.5.0
 * @see ByteSink
 */
public interface CharSink extends Sink {

	/**
	 * get the {@link Writer} used by this {@link CharSink}.
	 *
	 * @return an {@link Writer}
	 * @since 3.5.0
	 */
	Writer getWriter();

}
