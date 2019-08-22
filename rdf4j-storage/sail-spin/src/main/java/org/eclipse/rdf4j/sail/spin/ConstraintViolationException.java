/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.spin;

import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.spin.ConstraintViolation;

/**
 * Exception thrown when a SPIN constraint is violated.
 */
public class ConstraintViolationException extends SailException {

	private static final long serialVersionUID = 2208275585538203176L;

	private final ConstraintViolation violation;

	public ConstraintViolationException(ConstraintViolation violation) {
		super(violation.getMessage());
		this.violation = violation;
	}

	public ConstraintViolation getConstraintViolation() {
		return violation;
	}
}
