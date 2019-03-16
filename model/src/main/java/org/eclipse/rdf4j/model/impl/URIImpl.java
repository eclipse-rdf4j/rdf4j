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
 * @deprecated since 4.0. Use {@link SimpleIRI} instead for extending, and {@link ValueFactory} for instantiation.
 */
@Deprecated
public class URIImpl extends SimpleIRI {

	/**
	 * @deprecated since 4.0. Use {@link ValueFactory#createIRI(String)} instead.
	 */
	@Deprecated
	public URIImpl(String uriString) {
		super(uriString);
	}

}
