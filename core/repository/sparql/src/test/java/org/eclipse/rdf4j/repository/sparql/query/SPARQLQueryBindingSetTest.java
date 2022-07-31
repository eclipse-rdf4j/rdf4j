/*******************************************************************************
 * Copyright (c) 2016 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.repository.sparql.query;

import static org.junit.Assert.assertEquals;

import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.BindingSet;
import org.junit.Test;

/**
 * @author jeen
 *
 */
public class SPARQLQueryBindingSetTest {

	/**
	 * Verifies that the BindingSet implementation honors the API spec for {@link BindingSet#equals(Object)} and
	 * {@link BindingSet#hashCode()}.
	 */
	@Test
	public void testEqualsHashcode() {
		SPARQLQueryBindingSet bs1 = new SPARQLQueryBindingSet();
		SPARQLQueryBindingSet bs2 = new SPARQLQueryBindingSet();

		bs1.addBinding("x", RDF.ALT);
		bs1.addBinding("y", RDF.BAG);
		bs1.addBinding("z", RDF.FIRST);

		bs2.addBinding("y", RDF.BAG);
		bs2.addBinding("x", RDF.ALT);
		bs2.addBinding("z", RDF.FIRST);

		assertEquals(bs1, bs2);
		assertEquals(bs1.hashCode(), bs2.hashCode());
	}

}
