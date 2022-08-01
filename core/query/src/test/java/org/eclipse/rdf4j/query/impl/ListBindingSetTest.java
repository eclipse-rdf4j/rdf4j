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

import java.util.Arrays;
import java.util.List;

import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.BindingSetTest;

/**
 * Unit tests for {@link ListBindingSet}
 *
 * @author Jeen Broekstra
 */
public class ListBindingSetTest extends BindingSetTest<ListBindingSet> {

	@Override
	protected ListBindingSet[] createTwoEqualReorderedBindingSets() {

		List<String> names1 = Arrays.asList(new String[] { "x", "y", "z" });
		List<String> names2 = Arrays.asList(new String[] { "y", "z", "x" });

		ListBindingSet bindingSet1 = new ListBindingSet(names1, RDF.ALT, RDF.BAG, RDF.FIRST);
		ListBindingSet bindingSet2 = new ListBindingSet(names2, RDF.BAG, RDF.FIRST, RDF.ALT);
		return new ListBindingSet[] { bindingSet1, bindingSet2 };
	}

}
