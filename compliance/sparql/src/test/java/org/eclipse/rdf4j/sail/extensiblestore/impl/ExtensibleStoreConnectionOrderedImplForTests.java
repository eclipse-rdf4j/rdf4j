/*******************************************************************************
 * Copyright (c) 2023 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/

package org.eclipse.rdf4j.sail.extensiblestore.impl;

import org.eclipse.rdf4j.sail.extensiblestore.ExtensibleStoreConnection;

class ExtensibleStoreConnectionOrderedImplForTests
		extends ExtensibleStoreConnection<ExtensibleStoreOrderedImplForTests> {
	protected ExtensibleStoreConnectionOrderedImplForTests(ExtensibleStoreOrderedImplForTests sail) {
		super(sail);
	}
}
