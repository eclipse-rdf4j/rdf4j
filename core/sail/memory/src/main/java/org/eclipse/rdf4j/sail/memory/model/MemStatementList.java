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
package org.eclipse.rdf4j.sail.memory.model;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

/**
 * A dedicated data structure for storing MemStatement objects, offering operations optimized for their use in the
 * memory Sail.
 */
public class MemStatementList {
	private static final MemStatement[] EMPTY_ARRAY = {};

	// statements will be null when the array is growing
	private volatile MemStatement[] statements = EMPTY_ARRAY;
	private static final VarHandle STATEMENTS;
	static final VarHandle STATEMENTS_ARRAY;

	private volatile int size;
	private static final VarHandle SIZE;

	// When inserting a new statement into the statements array we need to iterate through the array looking for a free
	// spot. We keep track of where we last inserted a statement by storting that index in previouslyInsertedIndex. This
	// doesn't guarantee that previouslyInsertedIndex+1 wil be free, but it gives us a decent hint as to where to start
	// looking. When multiple threads are inserting at the same time the previouslyInsertedIndex should be considered
	// "best effort".
	private volatile int previouslyInsertedIndex = -1;
	private static final VarHandle PREVIOUSLY_INSERTED_INDEX;

	private volatile int guaranteedLastIndexInUse = -1;
	private static final VarHandle GUARANTEED_LAST_INDEX_IN_USE;

	private volatile boolean prioritiseCleanup;
	private static final VarHandle PRIORITISE_CLEANUP;

	private final AtomicReference<Thread> prioritisedThread = new AtomicReference<>();

	public MemStatementList() {
	}

	public MemStatementList(int capacity) {
		statements = new MemStatement[capacity];
	}

	public int size() {
		return ((int) SIZE.getAcquire(this));
	}

	public boolean isEmpty() {
		return ((int) SIZE.getAcquire(this)) == 0;
	}

	public void add(MemStatement st) throws InterruptedException {

		if (((boolean) PRIORITISE_CLEANUP.getOpaque(this))) {
			long start = System.currentTimeMillis();
			long stop = start + TimeUnit.SECONDS.toMillis(30);
			while (stop > System.currentTimeMillis() && ((boolean) PRIORITISE_CLEANUP.getVolatile(this))) {
				LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(10));
			}
		}

		do {

			MemStatement[] statements = getStatements();
			int length = statements.length;

			boolean shouldGrowArray = true;

			if (length > (int) SIZE.getAcquire(this)) {

				int previouslyInsertedIndex = (int) PREVIOUSLY_INSERTED_INDEX.getOpaque(this);
				if (previouslyInsertedIndex >= length) {
					continue;
				}

				int i = previouslyInsertedIndex + 1 >= length ? 0 : previouslyInsertedIndex + 1;

				for (; i != previouslyInsertedIndex; i = (i + 1 >= length ? 0 : i + 1)) {

					if (statements[i] == null) {

						boolean success = STATEMENTS_ARRAY.compareAndSet(statements, i, null, st);

						if (success) {
							shouldGrowArray = false;

							// check if the statements array has been swapped out (because it has grown) while we were
							// inserting into it
							MemStatement[] statementsAfterInsert = getStatementsWithoutInterrupt();
							if (statementsAfterInsert != statements
									&& STATEMENTS_ARRAY.getAcquire(statementsAfterInsert, i) != st) {
								// We wrote into an array while it was growing and our write was lost.
								break;
							}

							PREVIOUSLY_INSERTED_INDEX.setRelease(this, i);
							SIZE.getAndAdd(this, 1);

							updateGuaranteedLastIndexInUse(i);

							return;
						}
					} else if (previouslyInsertedIndex < 0 && i == length - 1) {
						// The array is full but no threads have made it to the code line where the
						// PREVIOUSLY_INSERTED_INDEX is updated. Don't grow the array just yet, it is better to wait
						// until PREVIOUSLY_INSERTED_INDEX is updated.
						shouldGrowArray = false;
						break;
					}
				}
			}

			if (shouldGrowArray && STATEMENTS.compareAndSet(this, statements, null)) {
				// Grow array
				MemStatement[] newArray = new MemStatement[Math.max(4, length * 2)];
				if (statements != EMPTY_ARRAY) {
					System.arraycopy(statements, 0, newArray, 0, length);
				}

				STATEMENTS.setRelease(this, newArray);
			}

			if (Thread.interrupted()) {
				throw new InterruptedException();
			}

		} while (true);

	}

	private void updateGuaranteedLastIndexInUse(int newValue) {
		int guaranteedLastIndexInUse = (int) GUARANTEED_LAST_INDEX_IN_USE.getAcquire(this);
		if (guaranteedLastIndexInUse < newValue) {
			while (guaranteedLastIndexInUse < newValue
					&& !GUARANTEED_LAST_INDEX_IN_USE.compareAndSet(this, guaranteedLastIndexInUse, newValue)) {
				guaranteedLastIndexInUse = (int) GUARANTEED_LAST_INDEX_IN_USE.getAcquire(this);
			}

		}
	}

	public boolean optimisticRemove(MemStatement st) throws InterruptedException {

		MemStatement[] statements = getStatements();

		for (int i = 0; i < statements.length; i++) {
			if (statements[i] == st) {
				return optimisticInnerRemove(st, statements, i);
			}
		}
		if (Thread.interrupted()) {
			throw new InterruptedException();
		}
		return false;
	}

	public boolean optimisticRemove(MemStatement st, int index) throws InterruptedException {
		MemStatement[] statements = getStatements();

		if (statements[index] == st && optimisticInnerRemove(st, statements, index)) {
			return true;
		} else {
			return optimisticRemove(st);
		}
	}

	private boolean optimisticInnerRemove(MemStatement toRemove, MemStatement[] statements, int i) {

		boolean success = STATEMENTS_ARRAY.weakCompareAndSet(statements, i, toRemove, null);
		if (success) {

			MemStatement[] statementsAfterRemoval = getStatementsWithoutInterrupt();
			if (statementsAfterRemoval != statements) {
				// We don't know if the statement was removed because the STATEMENTS_ARRAY has changed (because it
				// grew). Since it can never shrink we know that if we managed to remove the statement then the index
				// should either be null or a different statement
				if (STATEMENTS_ARRAY.getAcquire(statementsAfterRemoval, i) == toRemove) {
					return false;
				}
			}
			SIZE.getAndAdd(this, -1);

			return true;
		} else {
			return false;
		}

	}

	public void clear() {
		statements = EMPTY_ARRAY;
		size = 0;
		previouslyInsertedIndex = -1;
		guaranteedLastIndexInUse = -10;
		prioritiseCleanup = false;
	}

	public void cleanSnapshots(int currentSnapshot) throws InterruptedException {
		boolean error;
		do {
			MemStatement[] statements = getStatements();

			// reset the error flag
			error = false;

			for (int i = 0; i < statements.length; i++) {

				if (Thread.interrupted()) {
					throw new InterruptedException();
				}

				MemStatement statement = statements[i];
				if (statement != null && statement.getTillSnapshot() <= currentSnapshot) {
					boolean success = optimisticInnerRemove(statement, statements, i);
					if (!success) {
						error = true;
						break;
					}
				}

			}

			if (!error) {
				// make sure that the statements list didn't grow while we were cleaning it
				error = !STATEMENTS.compareAndSet(this, statements, statements);
			}
		} while (error);

	}

	/**
	 * Iterates through this list and returns the statement that exactly matches the provided arguments. The subject,
	 * predicate and object should not be null. If the context is null it will match statements with null as their
	 * context.
	 *
	 * @param subject
	 * @param predicate
	 * @param object
	 * @param context
	 * @param snapshot
	 * @return
	 */
	public MemStatement getExact(MemResource subject, MemIRI predicate, MemValue object, MemResource context,
			int snapshot) throws InterruptedException {

		MemStatement[] statements = getStatements();
		int lastIndexToCheck = getGuaranteedLastIndexInUse();

		for (int i = 0; i <= lastIndexToCheck; i++) {
			MemStatement memStatement = statements[i];
			if (memStatement != null && memStatement.exactMatch(subject, predicate, object, context)
					&& memStatement.isInSnapshot(snapshot)) {
				return memStatement;
			}
		}
		return null;
	}

	/**
	 * An internal method to retrieve the inner array that stores the statements. Useful to reduce the number of
	 * volatile reads.
	 *
	 * @return the underlying array og MemStatements
	 */
	public MemStatement[] getStatements() throws InterruptedException {
		MemStatement[] statements = (MemStatement[]) STATEMENTS.getAcquire(this);
		while (statements == null) {
			if (Thread.interrupted()) {
				throw new InterruptedException();
			}
			Thread.onSpinWait();
			statements = (MemStatement[]) STATEMENTS.getAcquire(this);
		}
		return statements;
	}

	private MemStatement[] getStatementsWithoutInterrupt() {
		MemStatement[] statements = (MemStatement[]) STATEMENTS.getAcquire(this);
		while (statements == null) {
			Thread.onSpinWait();
			statements = (MemStatement[]) STATEMENTS.getAcquire(this);
		}
		return statements;
	}

	public int getGuaranteedLastIndexInUse() {
		return ((int) GUARANTEED_LAST_INDEX_IN_USE.getAcquire(this));
	}

	public void setPrioritiseCleanup(boolean prioritiseCleanup) {
		if (!prioritiseCleanup) {
			if (prioritisedThread.compareAndSet(Thread.currentThread(), null)) {
				PRIORITISE_CLEANUP.setVolatile(this, false);
			} else {
				assert !((boolean) PRIORITISE_CLEANUP.getVolatile(this));
			}
		} else {
			if (prioritisedThread.compareAndSet(null, Thread.currentThread())) {
				Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
				PRIORITISE_CLEANUP.setVolatile(this, true);
			} else {
				throw new IllegalStateException("A cleanup thread is already prioritised: " + prioritisedThread.get());
			}
		}
	}

	static {
		try {
			SIZE = MethodHandles.lookup()
					.in(MemStatementList.class)
					.findVarHandle(MemStatementList.class, "size", int.class);
		} catch (ReflectiveOperationException e) {
			throw new Error(e);
		}
	}

	static {
		try {
			PREVIOUSLY_INSERTED_INDEX = MethodHandles.lookup()
					.in(MemStatementList.class)
					.findVarHandle(MemStatementList.class, "previouslyInsertedIndex", int.class);
		} catch (ReflectiveOperationException e) {
			throw new Error(e);
		}
	}

	static {
		try {
			GUARANTEED_LAST_INDEX_IN_USE = MethodHandles.lookup()
					.in(MemStatementList.class)
					.findVarHandle(MemStatementList.class, "guaranteedLastIndexInUse", int.class);
		} catch (ReflectiveOperationException e) {
			throw new Error(e);
		}
	}

	static {
		try {
			PRIORITISE_CLEANUP = MethodHandles.lookup()
					.in(MemStatementList.class)
					.findVarHandle(MemStatementList.class, "prioritiseCleanup", boolean.class);
		} catch (ReflectiveOperationException e) {
			throw new Error(e);
		}
	}

	static {
		try {
			STATEMENTS = MethodHandles.lookup()
					.in(MemStatementList.class)
					.findVarHandle(MemStatementList.class, "statements", MemStatement[].class);
		} catch (ReflectiveOperationException e) {
			throw new Error(e);
		}
	}

	static {
		STATEMENTS_ARRAY = MethodHandles.arrayElementVarHandle(MemStatement[].class);
	}

	boolean verifySizeForTesting() {
		MemStatement[] statements1 = getStatementsWithoutInterrupt();
		int size = 0;
		for (int i = 0; i < statements1.length; i++) {
			if (statements1[i] != null) {
				size++;
			}
		}
		return size == size();

	}

	int getRealSizeForTesting() {
		MemStatement[] statements1 = getStatementsWithoutInterrupt();
		int size = 0;
		for (int i = 0; i < statements1.length; i++) {
			if (statements1[i] != null) {
				size++;
			}
		}
		return size;

	}
}
