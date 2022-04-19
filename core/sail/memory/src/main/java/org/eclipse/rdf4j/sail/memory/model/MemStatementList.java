/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.memory.model;

import java.util.Arrays;

/**
 * A dedicated data structure for storing MemStatement objects, offering operations optimized for their use in the
 * memory Sail.
 */
public class MemStatementList {
	private static final MemStatement[] EMPTY_ARRAY = new MemStatement[0];

	/*-----------*
	 * Variables *
	 *-----------*/

	private volatile MemStatement[] statements;

	private volatile int size;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new MemStatementList.
	 */
	public MemStatementList() {
		statements = EMPTY_ARRAY;
		size = 0;
	}

	public MemStatementList(int capacity) {
		statements = new MemStatement[capacity];
		size = 0;
	}

	public MemStatementList(MemStatementList other) {
		statements = Arrays.copyOf(other.statements, other.statements.length);
		size = other.size;
	}

	/*---------*
	 * Methods *
	 *---------*/

	public int size() {
		return size;
	}

	public boolean isEmpty() {
		return size == 0;
	}

	public MemStatement get(int index) {
		assert index >= 0 : "index < 0";
		assert index < size : "index >= size";

		return statements[index];
	}

	public MemStatement getIfExists(int index) {
		if (index < size) {
			return statements[index];
		} else {
			return null;
		}
	}

	public void add(MemStatement st) {
		if (size == statements.length) {
			// Grow array
			growArray((size == 0) ? 4 : 2 * size);
		}

		statements[size] = st;
		++size;
	}

	public void addAll(MemStatementList other) {
		if (size + other.size >= statements.length) {
			// Grow array
			growArray(size + other.size);
		}

		System.arraycopy(other.statements, 0, statements, size, other.size);
		size += other.size;
	}

	public void remove(int index) {
		assert index >= 0 : "index < 0";
		assert index < size : "index >= size";

		if (index == size - 1) {
			// Last statement in array
			statements[index] = null;
			--size;
		} else {
			// Not last statement in array, move last
			// statement over the one at [index]
			--size;
			statements[index] = statements[size];
			statements[size] = null;
		}
	}

	public void remove(MemStatement st) {
		for (int i = 0; i < size; ++i) {
			if (statements[i] == st) {
				remove(i);
				return;
			}
		}
	}

	public void clear() {
		statements = EMPTY_ARRAY;
		size = 0;
	}

	public void cleanSnapshots(int currentSnapshot) {
		int i = size - 1;

		// remove all deprecated statements from the end of the list
		for (; i >= 0; i--) {
			if (Thread.currentThread().isInterrupted()) {
				return;
			}
			if (statements[i].getTillSnapshot() <= currentSnapshot) {
				--size;
				statements[i] = null;
			} else {
				i--;
				break;
			}
		}

		// remove all deprecated statements that are not at the end of the list
		for (; i >= 0; i--) {
			if (Thread.currentThread().isInterrupted()) {
				return;
			}

			if (statements[i].getTillSnapshot() <= currentSnapshot) {
				// replace statement with last statement in the list
				--size;
				statements[i] = statements[size];
				statements[size] = null;
			}
		}
	}

	private void growArray(int newSize) {
		MemStatement[] newArray = new MemStatement[newSize];
		if (statements != EMPTY_ARRAY) {
			System.arraycopy(statements, 0, newArray, 0, size);
		}
		statements = newArray;

	}
}
