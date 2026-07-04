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
package org.eclipse.rdf4j.model.util;

import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.model.Value;

/**
 * An exception thrown by {@link org.eclipse.rdf4j.model.Model Model} and {@link org.eclipse.rdf4j.model.util.Models
 * Models} when specific conditions are not met.
 *
 * @author Arjohn Kampman
 */
public class ModelException extends RDF4JException {

	private static final long serialVersionUID = 3886967415616842867L;

	public ModelException(Value value) {
		this("Unexpected object term: " + value);
	}

	public ModelException(Value v1, Value v2) {
		this(buildMessage(v1, v2));
	}

	public ModelException(String message, Throwable cause) {
		super(message, cause);
	}

	public ModelException(String message) {
		super(message);
	}

	public ModelException(Throwable cause) {
		super(cause);
	}

	private static String buildMessage(Value v1, Value v2) {
		StringBuilder sb = new StringBuilder();
		if (!v1.toString().equals(v2.toString())) {
			sb.append("Object is both ");
			sb.append(v1.toString());
			sb.append(" and ");
			sb.append(v2.toString());
		} else if (!v1.getClass().getName().equals(v2.getClass().getName())) {
			sb.append("Object is both ");
			sb.append("a ");
			sb.append(v1.getClass().getName());
			sb.append(" and a ");
			sb.append(v2.getClass().getName());
		} else {
			sb.append("Object is ");
			sb.append(v1);
			sb.append(" twice!? (store maybe corrupt)");
		}
		return sb.toString();
	}
}
