/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.model.impl;

/**
 * @deprecated since 4.0. Use {@link BooleanLiteral} instead.
 * @author Jeen Broekstra
 */
@Deprecated
public class BooleanLiteralImpl extends BooleanLiteral {

	/**
	 * @deprecated since 4.0. Use {@link BooleanLiteral#TRUE} instead.
	 */
	@Deprecated
	public static final BooleanLiteralImpl TRUE = new BooleanLiteralImpl(true);

	/**
	 * @deprecated since 4.0. Use {@link BooleanLiteral#FALSE} instead.
	 */
	@Deprecated
	public static final BooleanLiteralImpl FALSE = new BooleanLiteralImpl(false);

	@Deprecated
	protected BooleanLiteralImpl(boolean b) {
		super(b);
	}

}
