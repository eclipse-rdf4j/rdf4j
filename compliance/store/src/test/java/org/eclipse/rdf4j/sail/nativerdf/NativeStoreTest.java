/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.nativerdf;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.sail.NotifyingSail;
import org.eclipse.rdf4j.sail.RDFNotifyingStoreTest;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * An extension of RDFStoreTest for testing the class {@link NativeStore}.
 */
public class NativeStoreTest extends RDFNotifyingStoreTest {

	/*-----------*
	 * Variables *
	 *-----------*/

	@Rule
	public TemporaryFolder tempDir = new TemporaryFolder();

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	protected NotifyingSail createSail()
		throws SailException
	{
		try {
			NotifyingSail sail = new NativeStore(tempDir.newFolder("nativestore"), "spoc,posc");
			sail.initialize();
			return sail;
		}
		catch (IOException e) {
			throw new AssertionError(e);
		}
	}

	// Test for SES-542
	@Test()
	public void testGetNamespacePersistence()
		throws Exception
	{
		con.begin();
		con.setNamespace("rdf", RDF.NAMESPACE);
		con.commit();
		assertEquals(RDF.NAMESPACE, con.getNamespace("rdf"));

		con.close();
		sail.shutDown();
		sail.initialize();
		con = sail.getConnection();

		assertEquals(RDF.NAMESPACE, con.getNamespace("rdf"));
	}

}
