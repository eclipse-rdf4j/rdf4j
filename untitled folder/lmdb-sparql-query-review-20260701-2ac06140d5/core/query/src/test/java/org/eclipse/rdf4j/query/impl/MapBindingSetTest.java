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
package org.eclipse.rdf4j.query.impl;

import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.BindingSetTest;

/**
 * Unit tests for {@link MapBindingSet}
 *
 * @author Jeen Broekstra
 */
public class MapBindingSetTest extends BindingSetTest<MapBindingSet> {

	@Override
	protected MapBindingSet[] createTwoEqualReorderedBindingSets() {

		MapBindingSet bindingSet1 = new MapBindingSet();
		MapBindingSet bindingSet2 = new MapBindingSet();

		bindingSet1.addBinding("x", RDF.ALT);
		bindingSet1.addBinding("y", RDF.BAG);
		bindingSet1.addBinding("z", RDF.FIRST);

		bindingSet2.addBinding("y", RDF.BAG);
		bindingSet2.addBinding("x", RDF.ALT);
		bindingSet2.addBinding("z", RDF.FIRST);

		return new MapBindingSet[] { bindingSet1, bindingSet2 };
	}

}
