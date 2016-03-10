/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j;

/**
 * Superclass of all (runtime) exceptions thrown by RDF4J.
 * 
 * @author Jeen Broekstra
 */
public abstract class RDF4JException extends RuntimeException {

	private static final long serialVersionUID = 5283957703704198489L;

	public RDF4JException() {
		super();
	}

	public RDF4JException(String msg) {
		super(msg);
	}

	public RDF4JException(Throwable t) {
		super(t);
	}

	public RDF4JException(String msg, Throwable t) {
		super(msg, t);

	}
}
