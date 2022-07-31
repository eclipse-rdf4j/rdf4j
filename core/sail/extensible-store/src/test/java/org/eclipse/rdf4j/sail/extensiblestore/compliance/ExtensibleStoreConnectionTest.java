/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.extensiblestore.compliance;

import org.eclipse.rdf4j.common.transaction.IsolationLevel;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.extensiblestore.ExtensibleStoreImplForTests;
import org.eclipse.rdf4j.testsuite.repository.RepositoryConnectionTest;
import org.junit.runners.Parameterized;

public class ExtensibleStoreConnectionTest extends RepositoryConnectionTest {

	public ExtensibleStoreConnectionTest(IsolationLevel level) {
		super(level);
	}

	@Parameterized.Parameters(name = "{0}")
	public static IsolationLevel[] parameters() {
		return new IsolationLevel[] {
				IsolationLevels.NONE,
				IsolationLevels.READ_UNCOMMITTED,
				IsolationLevels.READ_COMMITTED
		};
	}

	@Override
	protected Repository createRepository() {
		return new SailRepository(new ExtensibleStoreImplForTests());
	}

}
