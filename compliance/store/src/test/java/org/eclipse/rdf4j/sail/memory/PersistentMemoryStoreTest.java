/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.memory;

import java.io.File;
import java.io.IOException;

import org.eclipse.rdf4j.common.io.FileUtil;
import org.eclipse.rdf4j.sail.NotifyingSail;
import org.eclipse.rdf4j.sail.RDFNotifyingStoreTest;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

/**
 * An extension of RDFStoreTest for testing the class
 * <tt>org.eclipse.rdf4j.sesame.sail.memory.MemoryStore</tt>.
 */
public class PersistentMemoryStoreTest extends RDFNotifyingStoreTest {

	private volatile File dataDir;

	@Override
	protected NotifyingSail createSail()
		throws SailException
	{
		try {
			dataDir = FileUtil.createTempDir(PersistentMemoryStoreTest.class.getSimpleName());
			NotifyingSail sail = new MemoryStore(dataDir);
			sail.initialize();
			return sail;
		}
		catch (IOException e) {
			throw new SailException(e);
		}
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
