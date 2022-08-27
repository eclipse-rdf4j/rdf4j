/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php 
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.query.algebra;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * @author Jeen Broekstra
 *
 */
public class ProjectionElemListTest {

	private ProjectionElem elem_a = new ProjectionElem("a");
	private ProjectionElem elem_b = new ProjectionElem("b");
	private ProjectionElem elem_ac = new ProjectionElem("a", "c");
	private ProjectionElem elem_de = new ProjectionElem("d", "e");

	private ProjectionElemList subject = new ProjectionElemList(elem_a, elem_b, elem_ac, elem_de);

	@Test
	public void testGetProjectedNames() {
		assertThat(subject.getProjectedNames()).containsExactly("a", "b", "c", "e");
	}

	@Test
	public void testGetProjectedNamesFor() {
		Set<String> sourceNames = new HashSet<>(Arrays.asList("a", "d"));
		assertThat(subject.getProjectedNamesFor(sourceNames)).containsExactly("a", "c", "e");

		sourceNames = new HashSet<>(Arrays.asList("b"));
		assertThat(subject.getProjectedNamesFor(sourceNames)).containsExactly("b");
	}

}
