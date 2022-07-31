/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.nativerdf;

import java.io.File;
import java.io.IOException;

import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.SailException;

/**
 * @author Jerven Bolleman, SIB Swiss Institute of Bioinformatics
 */
public class LimitedSizeNativeStore extends NativeStore {

	/**
	 * @param dataDir
	 * @param string
	 */
	public LimitedSizeNativeStore(File dataDir, String string) {
		super(dataDir, string);
	}

	public LimitedSizeNativeStore() {
		super();
	}

	public LimitedSizeNativeStore(File dataDir) {
		super(dataDir);
	}

	@Override
	protected NotifyingSailConnection getConnectionInternal() throws SailException {
		try {
			return new LimitedSizeNativeStoreConnection(this);
		} catch (IOException e) {
			throw new SailException(e);
		}
	}
}
