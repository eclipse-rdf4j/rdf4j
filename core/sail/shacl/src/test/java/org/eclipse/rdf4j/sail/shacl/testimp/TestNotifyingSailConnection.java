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

import java.util.HashSet;
import java.util.Set;

import org.eclipse.rdf4j.common.transaction.IsolationLevel;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.SailConnectionListener;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.helpers.NotifyingSailConnectionWrapper;

public class TestNotifyingSailConnection extends NotifyingSailConnectionWrapper implements SailConnectionListener {

	Set<Statement> added;
	Set<Statement> removed;

	public TestNotifyingSailConnection(NotifyingSailConnection wrappedCon) {
		super(wrappedCon);
		addConnectionListener(this);
	}

	@Override
	public void statementAdded(Statement statement) {
		boolean add = added.add(statement);
		if (!add) {
			removed.remove(statement);
		}
	}

	@Override
	public void statementRemoved(Statement statement) {
		boolean add = removed.add(statement);
		if (!add) {
			added.remove(statement);
		}
	}

	@Override
	public void begin() throws SailException {
		super.begin();
		added = new HashSet<>();
		removed = new HashSet<>();
	}

	@Override
	public void begin(IsolationLevel level) throws SailException {
		super.begin(level);
		added = new HashSet<>();
		removed = new HashSet<>();
	}
}
