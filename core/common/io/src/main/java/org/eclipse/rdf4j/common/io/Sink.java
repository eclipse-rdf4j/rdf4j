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

import org.eclipse.rdf4j.common.lang.FileFormat;

/**
 *
 * A Sink writes a data stream in a particular {@link FileFormat}.
 *
 * @author Jeen Broekstra
 * @since 3.5.0
 */
public interface Sink {

	/**
	 * Get the {@link FileFormat} this sink uses.
	 *
	 * @return a {@link FileFormat}. May not be <code>null</code>.
	 */
	FileFormat getFileFormat();

	/**
	 * Check if this Sink accepts the supplied {@link FileFormat}.
	 *
	 * @implNote the default implementation of this method only returns {@code true} if the supplied format is equal to
	 *           the format supplied by {@link #getFileFormat()}. Specific Sink implementations can choose to override
	 *           this behavior if they wish to indicate they can also accept other formats.
	 *
	 * @param format the {@link FileFormat} to check.
	 * @return {@code true} if the sink accepts the supplied format, {@code false} otherwise.
	 */
	default boolean acceptsFileFormat(FileFormat format) {
		return getFileFormat().equals(format);
	}

}
