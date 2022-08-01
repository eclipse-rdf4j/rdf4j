/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.testimp;

import org.eclipse.rdf4j.sail.NotifyingSail;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.helpers.NotifyingSailWrapper;

/**
 * Used for testing performance. Gives a good indication of the overhead of the notifying sail code.
 */
public class TestNotifyingSail extends NotifyingSailWrapper {

	public TestNotifyingSail(NotifyingSail baseSail) {
		super(baseSail);
	}

	@Override
	public NotifyingSailConnection getConnection() throws SailException {
		return new TestNotifyingSailConnection(super.getConnection());
	}
}
