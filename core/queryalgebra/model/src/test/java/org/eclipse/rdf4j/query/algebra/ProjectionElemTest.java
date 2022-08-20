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

import org.junit.jupiter.api.Test;

/**
 * @author Jeen Broekstra
 *
 */
public class ProjectionElemTest {

	private ProjectionElem elem_a = new ProjectionElem("a");
	private ProjectionElem elem_b = new ProjectionElem("b");
	private ProjectionElem elem_a2 = new ProjectionElem("a");

	private ProjectionElem elem_ab = new ProjectionElem("a", "b");
	private ProjectionElem elem_aa = new ProjectionElem("a", "a");
	private ProjectionElem elem_ba = new ProjectionElem("b", "a");
	private ProjectionElem elem_ab2 = new ProjectionElem("a", "b");

	@Test
	public void testEquals() {
		assertThat(elem_a).isEqualTo(elem_a2);
		assertThat(elem_a).isNotEqualTo(elem_aa).isNotEqualTo(elem_b).isNotEqualTo(elem_ab).isNotEqualTo(elem_ba);
		assertThat(elem_ab).isNotEqualTo(elem_aa).isNotEqualTo(elem_ba);
		assertThat(elem_ab).isEqualTo(elem_ab2);
	}

	@Test
	public void testSignature() {
		assertThat(elem_a.getSignature()).isEqualTo("ProjectionElem \"a\"");
		assertThat(elem_aa.getSignature()).isEqualTo("ProjectionElem \"a\" AS \"a\"");
		assertThat(elem_ab.getSignature()).isEqualTo("ProjectionElem \"a\" AS \"b\"");
	}
}
