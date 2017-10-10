/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.impl;

import org.eclipse.rdf4j.model.Value;

/**
 * @deprecated since 4.0. Use {@link SimpleBinding} instead.
 * @author Jeen Broekstra
 */
@Deprecated
public class BindingImpl extends SimpleBinding {

	private static final long serialVersionUID = 1L;

	public BindingImpl(String name, Value value) {
		super(name, value);
	}

}
