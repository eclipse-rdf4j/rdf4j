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
package org.eclipse.rdf4j.sail.memory.model;

import org.eclipse.rdf4j.model.vocabulary.XSD;

/**
 * An extension of MemLiteral that stores a boolean value to avoid parsing.
 *
 * @author David Huynh
 * @author Arjohn Kampman
 */
public class BooleanMemLiteral extends MemLiteral {

	private static final long serialVersionUID = 8061173551677475700L;

	/*-----------*
	 * Variables *
	 *-----------*/

	private final boolean b;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public BooleanMemLiteral(Object creator, boolean b) {
		this(creator, Boolean.toString(b), b);
	}

	public BooleanMemLiteral(Object creator, String label, boolean b) {
		super(creator, label, XSD.BOOLEAN);
		this.b = b;
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	public boolean booleanValue() {
		return b;
	}
}
