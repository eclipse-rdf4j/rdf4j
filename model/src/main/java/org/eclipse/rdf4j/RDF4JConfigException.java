/**
 * Copyright (c) 2015 Eclipse RDF4J contributors, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.eclipse.rdf4j;

/**
 * Exception indicating a configuration problem in an RDF4J component.
 * 
 * @author Jeen Broekstra
 */
public class RDF4JConfigException extends RDF4JException {

	private static final long serialVersionUID = -8121506409791182977L;

	public RDF4JConfigException() {
		super();
	}

	public RDF4JConfigException(String message) {
		super(message);
	}

	public RDF4JConfigException(Throwable t) {
		super(t);
	}

	public RDF4JConfigException(String msg, Throwable t) {
		super(msg, t);
	}
}
