/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Distribution License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/

package org.eclipse.rdf4j.sail.helpers.impl;

import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.helpers.AbstractSail;

public class SailImpl extends AbstractSail {
	@Override
	public boolean isWritable() throws SailException {
		return true;
	}

	@Override
	public ValueFactory getValueFactory() {
		return SimpleValueFactory.getInstance();
	}

	@Override
	protected void shutDownInternal() throws SailException {

	}

	@Override
	protected SailConnection getConnectionInternal() throws SailException {
		return new SailConnectionImpl(this);
	}
}
