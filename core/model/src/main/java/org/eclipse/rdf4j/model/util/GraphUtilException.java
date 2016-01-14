/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.model.util;

import org.eclipse.rdf4j.OpenRDFException;

/**
 * An exception thrown by {@link GraphUtil} when specific conditions are not
 * met.
 * 
 * @author Arjohn Kampman
 */
@Deprecated
public class GraphUtilException extends OpenRDFException {

	private static final long serialVersionUID = 3886967415616842867L;

	public GraphUtilException() {
		super();
	}

	public GraphUtilException(String message) {
		super(message);
	}

	public GraphUtilException(Throwable t) {
		super(t);
	}

	public GraphUtilException(String message, Throwable t) {
		super(message, t);
	}
}
