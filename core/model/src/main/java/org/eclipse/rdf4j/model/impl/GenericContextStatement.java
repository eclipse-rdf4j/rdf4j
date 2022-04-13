/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.model.impl;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;

/**
 * An extension of {@link SimpleStatement} that adds a context field.
 */
public class GenericContextStatement<R extends Resource, I extends IRI, V extends Value>
		extends GenericSimpleStatement<R, I, V> {

	/*-----------*
	 * Constants *
	 *-----------*/

	private static final long serialVersionUID = -4747275587477906748L;

	/**
	 * The statement's context, if applicable.
	 */
	private final R context;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new Statement with the supplied subject, predicate and object for the specified associated context.
	 *
	 * @param subject   The statement's subject, must not be <var>null</var>.
	 * @param predicate The statement's predicate, must not be <var>null</var>.
	 * @param object    The statement's object, must not be <var>null</var>.
	 * @param context   The statement's context, <var>null</var> to indicate no context is associated.
	 */
	protected GenericContextStatement(R subject, I predicate, V object, R context) {
		super(subject, predicate, object);
		this.context = context;
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	public R getContext() {
		return context;
	}

	public boolean exactSameContext(R context) {
		return context == this.context;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(256);

		sb.append(super.toString());
		sb.append(" [").append(getContext()).append("]");

		return sb.toString();
	}
}
