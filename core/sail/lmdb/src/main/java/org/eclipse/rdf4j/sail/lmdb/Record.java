/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb;

import java.nio.ByteBuffer;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A record in the triple store.
 *
 */
public class Record {
	final ByteBuffer key;
	final ByteBuffer val;
	private final BiConsumer<ByteBuffer, long[]> toQuad;

	public Record(ByteBuffer key, ByteBuffer val, BiConsumer<ByteBuffer, long[]> toQuad) {
		this.key = key;
		this.val = val;
		this.toQuad = toQuad;
	}

	void toQuad(long[] quad) {
		toQuad.accept(key, quad);
	}
}