/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j;

/**
 * General superclass of all unchecked exceptions that parts of RDF4J can throw.
 * 
 * @author Jeen Broekstra
 */
@SuppressWarnings("deprecation")
public abstract class RDF4JException extends OpenRDFException {

	private static final long serialVersionUID = 8913366826930181397L;

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
