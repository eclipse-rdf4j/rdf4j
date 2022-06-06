/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.model.impl;

import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.model.vocabulary.XSD;

/**
 * An extension of {@link SimpleLiteral} that stores a boolean value to avoid parsing.
 *
 * @author David Huynh
 * @author Arjohn Kampman
 */
public class BooleanLiteral extends SimpleLiteral {

	/*-----------*
	 * Constants *
	 *-----------*/

	private static final long serialVersionUID = -3610638093719366795L;

	public static final BooleanLiteral TRUE = new BooleanLiteral(true);

	public static final BooleanLiteral FALSE = new BooleanLiteral(false);

	/*-----------*
	 * Variables *
	 *-----------*/

	private final boolean value;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates an xsd:boolean typed literal with the specified value.
	 */
	protected BooleanLiteral(boolean value) {
		super(Boolean.toString(value), XSD.BOOLEAN);
		this.value = value;
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	public boolean booleanValue() {
		return value;
	}

	/**
	 * Returns a {@link BooleanLiteral} for the specified value. This method uses the constants {@link #TRUE} and
	 * {@link #FALSE} as result values, preventing the often unnecessary creation of new {@link BooleanLiteral} objects.
	 */
	public static BooleanLiteral valueOf(boolean value) {
		return value ? TRUE : FALSE;
	}

	@Override
	public CoreDatatype.XSD getCoreDatatype() {
		return CoreDatatype.XSD.BOOLEAN;
	}
}
