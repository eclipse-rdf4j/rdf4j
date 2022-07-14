/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/

package org.eclipse.rdf4j.sail.nativerdf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.sail.nativerdf.btree.RecordIterator;

/**
 * An simplified implementation of the RecordCache that keeps everything in memory.
 */
public class InMemRecordCache implements RecordCache {

	private List<byte[]> records = new ArrayList<>(1);

	@Override
	public void setMaxRecords(long maxRecords) {
		throw new UnsupportedOperationException();
	}

	@Override
	public long getRecordCount() {
		return records.size();
	}

	@Override
	public void storeRecord(byte[] data) throws IOException {
		records.add(data);
	}

	@Override
	public void storeRecords(RecordCache otherCache) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear() throws IOException {
		records = new ArrayList<>(1);
	}

	@Override
	public RecordIterator getRecords() throws IOException {

		return new RecordIterator() {

			private int index = 0;

			@Override
			public byte[] next() throws IOException {
				if (index < records.size()) {
					return records.get(index++);
				}
				return null;
			}

			@Override
			public void set(byte[] record) throws IOException {
				records.set(index - 1, record);
			}

			@Override
			public void close() throws IOException {

			}
		};
	}

	@Override
	public boolean isValid() {
		return true;
	}

	@Override
	public void discard() throws IOException {

	}
}
