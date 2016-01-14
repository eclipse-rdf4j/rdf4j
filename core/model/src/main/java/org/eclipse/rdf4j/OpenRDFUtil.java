/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j;

import org.eclipse.rdf4j.model.Resource;

/**
 * General utility methods for OpenRDF/Sesame modules.
 * 
 * @author Arjohn Kampman
 */
public class OpenRDFUtil {

	/**
	 * Verifies that the supplied contexts parameter is not <tt>null</tt>,
	 * throwing an {@link IllegalArgumentException} if it is.
	 * <p>
	 * The semantics of supplying <tt>null</tt> as the value of the
	 * <tt>contexts</tt> vararg is not completely clear; it can either be
	 * equivalent to supplying an empty array (i.e.: matching all statements
	 * disregarding context), or to supplying a <tt>null</tt>-Resource value
	 * (e.g.: matching all statements with no associated context). As we so far
	 * haven't been able to prefer one over the other, methods operating on
	 * contexts currently throw {@link IllegalArgumentException}s.
	 * 
	 * @param contexts
	 *        The parameter to check.
	 * @throws IllegalArgumentException
	 *         If the supplied contexts parameter is <tt>null</tt>.
	 */
	public static void verifyContextNotNull(Resource... contexts) {
		if (contexts == null) {
			throw new IllegalArgumentException(
					"Illegal value null array for contexts argument; either the value should be cast to Resource or an empty array should be supplied");
		}
	}
}
