/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.nativerdf;

import java.io.File;
import java.io.IOException;

import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;


/**
 * Native Store extension which introduces a hook with a specialized connection (cf 
 * {@link NativeStoreConnectionExt}), which allows for efficient evaluation of
 * prepared queries without prior optimization.<p>
 * 
 * Whenever a native store is to be used as a repository within FedX, use this extension.
 * 
 * @author Andreas Schwarte
 * @see NativeStoreConnectionExt
 *
 */
public class NativeStoreExt extends NativeStore {

	public NativeStoreExt() {
		super();
	}

	public NativeStoreExt(File dataDir, String tripleIndexes) {
		super(dataDir, tripleIndexes);
	}

	public NativeStoreExt(File dataDir) {
		super(dataDir);
	}
	
	
	@Override
	protected NotifyingSailConnection getConnectionInternal() throws SailException {
		try {
			return new NativeStoreConnectionExt(this);
		} catch (IOException e) {
			throw new SailException(e);
		}
	}

}
