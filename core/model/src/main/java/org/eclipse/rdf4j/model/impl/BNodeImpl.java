/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.model.impl;

import org.eclipse.rdf4j.model.ValueFactory;

/**
 * @author Jeen Broekstra
 * @deprecated since 4.0. Use {@link SimpleBNode} instead for extending, and
 *             {@link ValueFactory} for instantiation.
 */
@Deprecated
public class BNodeImpl extends SimpleBNode {

	/**
	 * @deprecated since 4.0. Use {@link ValueFactory#createBNode()} instead.
	 */
	@Deprecated
	public BNodeImpl() {
		super();
	}

	/**
	 * @deprecated since 4.0. Use {@link ValueFactory#createBNode(String)}
	 *             instead.
	 */
	@Deprecated
	public BNodeImpl(String nodeID) {
		super(nodeID);
	}
}
