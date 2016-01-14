/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.common.io;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * A wrapper for an input stream to avoid allowing libraries to close input
 * streams unexpectedly using the {@link #close()} method. Instead, they must be
 * closed by the creator using {@link #doClose()}.
 * 
 * @author Peter Ansell
 */
public class UncloseableInputStream extends FilterInputStream {

	public UncloseableInputStream(InputStream parent) {
		super(parent);
	}

	public void close()
		throws IOException
	{
		// do nothing
	}

	public void doClose()
		throws IOException
	{
		super.close();
	}
}
