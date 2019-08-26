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
public class ContextStatement extends SimpleStatement {

	/*-----------*
	 * Constants *
	 *-----------*/

	private static final long serialVersionUID = -4747275587477906748L;

	/**
	 * The statement's context, if applicable.
	 */
	private final Resource context;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new Statement with the supplied subject, predicate and object for the specified associated context.
	 * 
	 * @param subject   The statement's subject, must not be <tt>null</tt>.
	 * @param predicate The statement's predicate, must not be <tt>null</tt>.
	 * @param object    The statement's object, must not be <tt>null</tt>.
	 * @param context   The statement's context, <tt>null</tt> to indicate no context is associated.
	 */
	protected ContextStatement(Resource subject, IRI predicate, Value object, Resource context) {
		super(subject, predicate, object);
		this.context = context;
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	public Resource getContext() {
		return context;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(256);

		sb.append(super.toString());
		sb.append(" [").append(getContext()).append("]");

		return sb.toString();
	}
}
