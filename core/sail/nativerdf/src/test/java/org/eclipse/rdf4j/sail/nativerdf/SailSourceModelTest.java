/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.nativerdf;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;

import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.sail.SailException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class SailSourceModelTest {

	static SailSourceModel sailSourceModel;

	@TempDir
	static Path datadir;

	@BeforeAll
	public static void setUp() throws SailException, IOException {
		NativeSailStore store = new NativeSailStore(datadir.toFile(), "spoc");
		sailSourceModel = new SailSourceModel(store);
	}

	@Test
	public void testSimpleAdd() {
		sailSourceModel.add(RDF.TYPE, RDF.TYPE, RDF.TYPE);
		assertThat(sailSourceModel.contains(RDF.TYPE, RDF.TYPE, RDF.TYPE));
	}

	@Test
	public void testSimpleRemove() {
		sailSourceModel.remove(RDF.TYPE, RDF.TYPE, RDF.TYPE);
		assertThat(sailSourceModel.contains(RDF.TYPE, RDF.TYPE, RDF.TYPE)).isFalse();
	}

}
