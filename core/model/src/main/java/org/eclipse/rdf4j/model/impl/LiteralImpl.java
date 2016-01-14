/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.model.impl;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.URI;
import org.eclipse.rdf4j.model.ValueFactory;

/**
 * @author Jeen Broekstra
 * @deprecated since 4.0. Use {@link SimpleLiteral} for extending, and
 *             instantiate using a {@link ValueFactory}.
 */
@Deprecated
public class LiteralImpl extends SimpleLiteral {

	/**
	 * @deprecated since 4.0. Use {@link ValueFactory#createLiteral(String)}
	 *             instead.
	 */
	@Deprecated
	public LiteralImpl(String label) {
		super(label);
	}

	/**
	 * @deprecated since 4.0. Use
	 *             {@link ValueFactory#createLiteral(String, String)} instead.
	 */
	@Deprecated
	public LiteralImpl(String label, String language) {
		super(label, language);
	}

	/**
	 * @deprecated since 4.0. Use {@link ValueFactory#createLiteral(String, IRI)}
	 *             instead.
	 */
	@Deprecated
	public LiteralImpl(String label, URI datatype) {
		super(label, (IRI)datatype);
	}
}
