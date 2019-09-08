/*
 * Copyright (C) 2018 Veritas Technologies LLC.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
