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
package org.eclipse.rdf4j.federated.endpoint.provider;

import java.io.File;

import org.eclipse.rdf4j.sail.nativerdf.NativeStore;

public class NativeStoreProvideTest {

	/**
	 * Create a NativeStore using {@link NativeStoreProvider}.
	 *
	 * Makes the method accessible for test infrastructure
	 *
	 * @param store
	 * @return
	 */
	public static NativeStore createNativeStore(File store) {
		return new NativeStoreProvider(null).createNativeStore(store);
	}
}
