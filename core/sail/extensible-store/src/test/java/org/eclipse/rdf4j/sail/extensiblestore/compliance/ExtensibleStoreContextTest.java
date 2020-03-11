/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.extensiblestore.compliance;

import org.eclipse.rdf4j.sail.NotifyingSail;
import org.eclipse.rdf4j.sail.RDFNotifyingStoreTest;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.extensiblestore.ExtensibleStoreImplForTests;

public class ExtensibleStoreContextTest extends RDFNotifyingStoreTest {

	@Override
	protected NotifyingSail createSail() throws SailException {
		return new ExtensibleStoreImplForTests();
	}
}
