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
package org.eclipse.rdf4j.common.webapp;

/**
 * @author Herko ter Horst
 */
public class Message {

	public static final String ATTRIBUTE_KEY = "message";

	public enum Type {
		ERROR,
		WARN,
		INFO
	}

	private final Type type;

	private final String i18n;

	public Message(Type type, String i18n) {
		this.type = type;
		this.i18n = i18n;
	}

	/**
	 * @return Returns the type.
	 */
	public Type getType() {
		return type;
	}

	/**
	 * @return Returns the i18n.
	 */
	public String getI18n() {
		return i18n;
	}

}
