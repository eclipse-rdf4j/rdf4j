/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.model.impl;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.URI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;

/**
 * @author Jeen Broekstra
 * @deprecated since 4.0. Use {@link ContextStatement} instead for extension,
 *             and {@link ValueFactory} for instantiation.
 */
@Deprecated
public class ContextStatementImpl extends ContextStatement {

	/**
	 * @deprecated since 4.0. Use
	 *             {@link ValueFactory#createStatement(Resource, IRI, Value, Resource)}
	 *             instead.
	 */
	@Deprecated
	public ContextStatementImpl(Resource subject, URI predicate, Value object, Resource context) {
		super(subject, (IRI)predicate, object, context);
	}

}
