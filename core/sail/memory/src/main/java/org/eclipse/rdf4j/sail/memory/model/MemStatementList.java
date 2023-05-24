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
import java.util.concurrent.locks.StampedLock;

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

	private final StampedLock addRemoveLock = new StampedLock();

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

		long readLock = addRemoveLock.readLock();
		try {
			do {

				MemStatement[] statements = getStatements();
				int length = statements.length;

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

								// check if the statements array has been swapped out (because it was grown) while we
								// were
								// inserting into it
								MemStatement[] statementsAfterInsert = getStatements();
								if (statementsAfterInsert != statements
										&& STATEMENTS_ARRAY.getAcquire(statements, i) != st) {
									// we wrote into an array while it was growing and our write was lost
									break;
								}

								PREVIOUSLY_INSERTED_INDEX.setRelease(this, i);
								SIZE.getAndAdd(this, 1);

								updateGuaranteedLastIndexInUse(i);

								return;
							}
						}

					}

				}

				// statements array is probably full

				if (STATEMENTS.compareAndSet(this, statements, null)) {// Grow array
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
		} finally {
			addRemoveLock.unlockRead(readLock);
		}
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

	public void remove(MemStatement st) throws InterruptedException {

		do {
			MemStatement[] statements = getStatements();

			boolean success = true;
			for (int i = 0; i < statements.length; i++) {
				if (statements[i] == st) {

					success = innerRemove(st, statements, i);

					break;
				}
			}
			if (success) {
				break;
			}
			if (Thread.interrupted()) {
				throw new InterruptedException();
			}
		} while (true);
	}

	public void remove(MemStatement st, int index) throws InterruptedException {

		do {
			MemStatement[] statements = getStatements();

			if (statements[index] == st && innerRemove(st, statements, index)) {
				return;
			} else {
				remove(st);
			}
			if (Thread.interrupted()) {
				throw new InterruptedException();
			}
		} while (true);
	}

	private boolean innerRemove(MemStatement st, MemStatement[] statements, int i) throws InterruptedException {
		long writeLock = addRemoveLock.writeLock();
		try {
			if (getStatements() != statements) {
				return false;
			}

			boolean success = STATEMENTS_ARRAY.compareAndSet(statements, i, st, null);
			if (success) {
				while (true) {
					int size = size();
					boolean decrementedSize = SIZE.compareAndSet(this, size, size - 1);
					if (decrementedSize) {
						return true;
					}
				}
			} else {
				return false;
			}
		} finally {
			addRemoveLock.unlockWrite(writeLock);
		}

	}

	public void clear() {
		long writeLock = addRemoveLock.writeLock();
		try {
			statements = EMPTY_ARRAY;
			size = 0;
			previouslyInsertedIndex = -1;
			guaranteedLastIndexInUse = -10;
			prioritiseCleanup = false;
		} finally {
			addRemoveLock.unlockWrite(writeLock);
		}
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
					boolean success = innerRemove(statement, statements, i);
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

}
