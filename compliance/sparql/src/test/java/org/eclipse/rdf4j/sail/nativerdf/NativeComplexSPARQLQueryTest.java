/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.nativerdf;

import java.io.File;
import java.nio.file.Files;

import org.eclipse.rdf4j.common.io.FileUtil;
import org.eclipse.rdf4j.query.parser.sparql.ComplexSPARQLQueryTest;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;

/**
 * @author jeen
 */
public class NativeComplexSPARQLQueryTest extends ComplexSPARQLQueryTest {

	File dataDir = null;

	@Override
	protected Repository newRepository() throws Exception {
		dataDir = Files.createTempDirectory("nativestore").toFile();
		return new SailRepository(new NativeStore(dataDir, "spoc"));

	}

	@Override
	public void tearDown() throws Exception {
		try {
			super.tearDown();
		} finally {
			FileUtil.deleteDir(dataDir);
		}
	}

}
