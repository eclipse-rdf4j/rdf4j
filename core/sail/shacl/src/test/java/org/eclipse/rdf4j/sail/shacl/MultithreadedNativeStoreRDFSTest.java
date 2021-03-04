/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.shacl;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.assertj.core.util.Files;
import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.sail.NotifyingSail;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.inferencer.fc.SchemaCachingRDFSInferencer;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;
import org.junit.After;
import org.junit.Before;

public class MultithreadedNativeStoreRDFSTest extends MultithreadedTest {

	File file;

	@After
	public void after() {
		try {
			FileUtils.deleteDirectory(file);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Before
	public void before() {
		file = Files.newTemporaryFolder();
	}

	@Override
	NotifyingSail getBaseSail() {
		NativeStore nativeStore = new NativeStore(file);
		SchemaCachingRDFSInferencer schemaCachingRDFSInferencer = new SchemaCachingRDFSInferencer(nativeStore);
		try (NotifyingSailConnection connection = schemaCachingRDFSInferencer.getConnection()) {
			connection.begin(IsolationLevels.NONE);
			connection.clear();
			connection.commit();
		}
		return schemaCachingRDFSInferencer;
	}

}
