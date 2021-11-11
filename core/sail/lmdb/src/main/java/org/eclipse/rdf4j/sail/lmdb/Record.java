/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb;

import java.nio.ByteBuffer;

/**
 * A record in the triple store.
 *
 * @author Ken Wenzel
 */
public class Record {
	final ByteBuffer key;
	final ByteBuffer val;

	public Record(ByteBuffer key, ByteBuffer val) {
		this.key = key;
		this.val = val;
	}
}