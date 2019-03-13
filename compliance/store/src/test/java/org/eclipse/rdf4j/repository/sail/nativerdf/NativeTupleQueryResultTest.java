/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.repository.sail.nativerdf;

import java.io.File;
import java.io.IOException;

import org.eclipse.rdf4j.common.io.FileUtil;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.TupleQueryResultTest;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;

public class NativeTupleQueryResultTest extends TupleQueryResultTest {

	private File dataDir;

	@Override
	protected Repository newRepository()
		throws IOException
	{
		dataDir = FileUtil.createTempDir("nativestore");
		return new SailRepository(new NativeStore(dataDir, "spoc"));
	}

	@Override
	public void tearDown()
		throws Exception
	{
		try {
			super.tearDown();
		}
		finally {
			FileUtil.deleteDir(dataDir);
		}
	}
}
