/******************************************************************************* 
 * Copyright (c) 2020 Eclipse RDF4J contributors. 
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Distribution License v1.0 
 * which accompanies this distribution, and is available at 
 * http://www.eclipse.org/org/documents/edl-v10.php. 
 *******************************************************************************/
package org.eclipse.rdf4j.common.io;

import org.eclipse.rdf4j.common.lang.FileFormat;

/**
 * 
 * A Sink writes data in a particular {@link FileFormat}.
 * 
 * @author Jeen Broekstra
 * @since 3.5.0
 */
public interface Sink {

	/**
	 * Get the {@link FileFormat} this sink uses.
	 * 
	 * @return a {@link FileFormat}.
	 */
	FileFormat getFileFormat();
}
