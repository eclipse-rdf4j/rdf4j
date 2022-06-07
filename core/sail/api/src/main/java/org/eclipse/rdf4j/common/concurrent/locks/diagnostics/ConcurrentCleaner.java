/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Distribution License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/

package org.eclipse.rdf4j.common.concurrent.locks.diagnostics;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.ref.Cleaner;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;

import org.eclipse.rdf4j.common.annotation.InternalUseOnly;

/**
 * Optimized for multithreaded use of the Java 9+ Cleaner
 *
 * @author Håvard M. Ottestad
 */
@InternalUseOnly
public class ConcurrentCleaner {

	private final static int MAX = 128;
	private final static int mask;
	private final static int concurrency;

	static {
		concurrency = powerOfTwoSize(Runtime.getRuntime().availableProcessors() * 2);
		mask = concurrency - 1;
	}

	private final static AtomicBoolean initialized = new AtomicBoolean();
	private volatile static Cleaner cleaner = Cleaner.create();
	private volatile static ConcurrentLinkedQueue<LazyCleanable>[] queues;
	private volatile static LongAdder counter = new LongAdder();

	private static ScheduledExecutorService scheduledExecutorService;

	private static long lastFlush = 0;

	private volatile static ConcurrentCleaner concurrentCleaner;

	private ConcurrentCleaner() {
	}

	public static ConcurrentCleaner create() {
		if (initialized.compareAndSet(false, true)) {
			cleaner = Cleaner.create();
			queues = new ConcurrentLinkedQueue[concurrency];
			for (int i = 0; i < queues.length; i++) {
				queues[i] = new ConcurrentLinkedQueue<>();
			}
			counter = new LongAdder();

			scheduledExecutorService = Executors.newScheduledThreadPool(1, r -> {
				Thread t = Executors.defaultThreadFactory().newThread(r);
				t.setDaemon(true);
				t.setName("ConcurrentCleaner-" + t.getId());
				return t;
			});

			scheduledExecutorService.scheduleWithFixedDelay(() -> {
				if (System.currentTimeMillis() > lastFlush + 1000 || counter.sum() > 10000) {
					flush();
				}

			}, 1000, 10, TimeUnit.SECONDS);

			concurrentCleaner = new ConcurrentCleaner();
		}
		return concurrentCleaner;
	}

	private static int powerOfTwoSize(int initialSize) {
		int n = -1 >>> Integer.numberOfLeadingZeros(initialSize - 1);
		return (n < 0) ? 1 : (n >= MAX) ? MAX : n + 1;
	}

	static int getIndex(Thread key) {
		if (key == null) {
			return 0;
		}
		return mask & ((int) key.getId());
	}

	public Cleaner.Cleanable register(Object obj, Runnable action) {
		var queue = queues[getIndex(Thread.currentThread())];
		var lazyCleanable = new LazyCleanable(cleaner, obj, action);
		queue.add(lazyCleanable);
		counter.increment();
		return lazyCleanable;
	}

	/**
	 * Flush all buffers and register all objects with the {@link Cleaner}.
	 */
	public static void flush() {
		long sum = counter.sumThenReset();

		for (var queue : queues) {
			if (queue == null) {
				break;
			}

			LazyCleanable poll = queue.poll();
			while (poll != null) {
				sum--;
				poll.register();
				if (Thread.currentThread().isInterrupted()) {
					break;
				}
				if (sum <= 0) {
					lastFlush = System.currentTimeMillis();
					return;
				}
				poll = queue.poll();
			}
		}

		lastFlush = System.currentTimeMillis();
	}

	@SuppressWarnings("FieldMayBeFinal")
	private static class LazyCleanable implements Cleaner.Cleanable {

		private final Cleaner cleaner;
		private final Runnable action;
		private volatile Object object;
		private volatile Cleaner.Cleanable delegate;

		private final static VarHandle DELEGATE;
		private final static VarHandle OBJECT;

		static {
			try {
				OBJECT = MethodHandles.lookup()
						.in(LazyCleanable.class)
						.findVarHandle(LazyCleanable.class, "object", Object.class);
			} catch (ReflectiveOperationException e) {
				throw new Error(e);
			}
		}

		static {
			try {
				DELEGATE = MethodHandles.lookup()
						.in(LazyCleanable.class)
						.findVarHandle(LazyCleanable.class, "delegate", Cleaner.Cleanable.class);
			} catch (ReflectiveOperationException e) {
				throw new Error(e);
			}
		}

		public LazyCleanable(Cleaner cleaner, Object object, Runnable action) {
			this.cleaner = cleaner;
			this.action = action;
			this.object = object;
		}

		public void register() {
			Object object = OBJECT.getAndSet(this, null);
			if (object != null) {
				DELEGATE.setRelease(this, cleaner.register(object, action));
			}
		}

		@Override
		public void clean() {
			OBJECT.setRelease(this, null);
			Cleaner.Cleanable delegate = (Cleaner.Cleanable) DELEGATE.getAcquire(this);

			if (delegate != null) {
				delegate.clean();
				DELEGATE.setRelease(this, null);
			}
		}
	}

}
