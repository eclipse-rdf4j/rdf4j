/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.config;

import org.eclipse.rdf4j.OpenRDFException;

/**
 * Exception indicating a sail configuration problem.
 * 
 * @author Arjohn Kampman
 */
public class SailConfigException extends OpenRDFException {

	private static final long serialVersionUID = 185213210952981723L;

	public SailConfigException() {
		super();
	}

	public SailConfigException(String message) {
		super(message);
	}

	public SailConfigException(Throwable t) {
		super(t);
	}

	public SailConfigException(String message, Throwable t) {
		super(message, t);
	}
}
