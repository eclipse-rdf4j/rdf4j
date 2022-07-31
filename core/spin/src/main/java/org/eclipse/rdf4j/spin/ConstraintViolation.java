/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.spin;

import java.io.Serializable;

/**
 * Class to store SPIN constraint violation RDF statements.
 */
public class ConstraintViolation implements Serializable {

	private static final long serialVersionUID = 3699022598761641221L;

	private final String message;

	private final String root;

	private final String path;

	private final String value;

	private final ConstraintViolationLevel level;

	public ConstraintViolation(String message, String root, String path, String value, ConstraintViolationLevel level) {
		this.message = message;
		this.root = root;
		this.path = path;
		this.value = value;
		this.level = level;
	}

	public String getMessage() {
		return message;
	}

	public String getRoot() {
		return root;
	}

	public String getPath() {
		return path;
	}

	public String getValue() {
		return value;
	}

	public ConstraintViolationLevel getLevel() {
		return level;
	}
}
