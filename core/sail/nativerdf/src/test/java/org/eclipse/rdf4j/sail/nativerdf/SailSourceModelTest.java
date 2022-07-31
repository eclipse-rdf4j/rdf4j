/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.nativerdf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.nio.file.Files;
import java.util.Iterator;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.testsuite.model.ModelTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class SailSourceModelTest extends ModelTest {

	@Test
	public void testRemove() {
		SailSourceModel sailSourceModel = getNewModel();
		sailSourceModel.add(RDF.TYPE, RDF.TYPE, RDF.TYPE);
		sailSourceModel.remove(RDF.TYPE, RDF.TYPE, RDF.TYPE);
		assertThat(sailSourceModel.contains(RDF.TYPE, RDF.TYPE, RDF.TYPE)).isFalse();
	}

	@Test
	public void testRemoveTermIteration() {
		SailSourceModel sailSourceModel = getNewModel();
		sailSourceModel.add(RDF.TYPE, RDF.TYPE, RDF.TYPE);
		sailSourceModel.removeTermIteration((Iterator<Statement>) mock(Iterator.class), RDF.TYPE, RDF.TYPE, RDF.TYPE);
		assertThat(sailSourceModel.contains(RDF.TYPE, RDF.TYPE, RDF.TYPE)).isFalse();
	}

	@Test
	@Override
	@Disabled
	public void testGetStatements_ConcurrentModificationOfModel() {
	}

	@Override
	protected SailSourceModel getNewModel() {
		try {
			NativeSailStore store = new NativeSailStore(Files.createTempDirectory("SailSourceModelTest-").toFile(),
					"spoc");
			return new SailSourceModel(store);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
