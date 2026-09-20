/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb.evaluation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongUnaryOperator;

/** Runs unchanged production concurrency/range code without Maven or dependency substitutes. */
final class LmdbNativeMorselChecks {
	private static final AtomicLong assertions = new AtomicLong();

	public static void main(String[] args) throws Exception {
		int repetitions = args.length == 0 ? 200 : Integer.parseInt(args[0]);
		long started = System.nanoTime();
		ranges(repetitions);
		dedupBoundaries(repetitions);
		try (ExecutorService pool = Executors.newFixedThreadPool(6)) {
			concurrentRanges(pool, repetitions);
			exchange(pool, repetitions);
			for (int i = 0; i < repetitions; i++) {
				earlyClose(pool);
				workerFailure(pool);
				partialSubmission(pool);
			}
			concurrentFailures(pool);
			interruptedClose(pool);
			parentCancellation(pool);
		}
		System.out.printf("PASS %,d assertions; %,d stress repetitions; %.3f s; %s%n", assertions.get(),
				repetitions, (System.nanoTime() - started) / 1e9, System.getProperty("java.runtime.version"));
		System.out.println("Compiled unchanged production range dispatcher, exchange/lifetime and cancellation token. No integration seams.");
	}

	static void check(boolean condition, String message) {
		assertions.incrementAndGet();
		if (!condition)
			throw new AssertionError(message);
	}

	static void ranges(int repetitions) {
		Random random = new Random(0x593769A5L);
		for (int test = 0; test < repetitions * 20; test++) {
			long size = random.nextInt(100_000);
			long rows = 1 + random.nextInt(8000);
			LmdbNativeMorselRange source = new LmdbNativeMorselRange(size, rows);
			long at = 0L, count = 0L;
			for (LmdbNativeMorselRange.Range range; (range = source.claim(null)) != null;) {
				check(range.from() == at && range.to() > at && range.to() - at <= rows, "range tiling");
				at = range.to();
				count++;
			}
			check(at == size && source.exhausted(), "complete root coverage");
			check(count == source.estimatedMorsels(), "morsel estimate");
		}
		LmdbNativeMorselRange wide = new LmdbNativeMorselRange(Long.MAX_VALUE, Long.MAX_VALUE - 1L);
		check(wide.claim(null).to() == Long.MAX_VALUE - 1L, "wide first range");
		check(wide.claim(null).to() == Long.MAX_VALUE && wide.exhausted(), "wide last range");
		LmdbNativeMorselRange invalid = new LmdbNativeMorselRange(5L, 1L);
		for (long boundary : new long[] { -1L, 0L, 6L, Long.MIN_VALUE }) {
			try {
				invalid.claim(ignored -> boundary);
				throw new AssertionError("invalid boundary accepted");
			} catch (IllegalStateException expected) {
				check(!invalid.exhausted(), "rejected cut must not consume work");
			}
		}
		check(invalid.claim(null).from() == 0L, "rejected boundary leaves head untouched");
		System.out.println("PASS empty/tail/wide/overflow-safe ranges and rejected cuts");
	}

	static void dedupBoundaries(int repetitions) {
		Random random = new Random(935912L);
		for (int test = 0; test < repetitions * 5; test++) {
			long[] values = new long[random.nextInt(2048) + 1];
			long value = Long.MAX_VALUE - 20;
			for (int i = 0; i < values.length; i++) {
				if (random.nextInt(13) == 0)
					value++;
				values[i] = value;
			}
			for (int cut = 0; cut <= values.length; cut++) {
				int expected = cut;
				if (cut != 0)
					while (expected < values.length && values[expected] == values[cut - 1])
						expected++;
				long actual = LmdbNativeMorselRange.afterDuplicates(i -> values[(int) i], cut, values.length);
				check(actual == expected, "unsigned duplicate boundary mismatch");
			}
		}
		long size = Long.MAX_VALUE;
		AtomicInteger reads = new AtomicInteger();
		LongUnaryOperator same = index -> {
			check(index >= 0L && index < size, "duplicate lookup out of range");
			reads.incrementAndGet();
			return -1L;
		};
		check(LmdbNativeMorselRange.afterDuplicates(same, 1L, size) == size, "huge duplicate run");
		check(reads.get() <= 130, "duplicate alignment must remain logarithmic");
		System.out.println("PASS random duplicate alignment, unsigned ID boundary and logarithmic huge-run cuts");
	}

	static void concurrentRanges(ExecutorService pool, int repetitions) throws Exception {
		Random random = new Random(5098543L);
		for (int test = 0; test < repetitions; test++) {
			int count = 4096 + random.nextInt(4096);
			int rows = 1 + random.nextInt(80);
			LmdbNativeMorselRange range = new LmdbNativeMorselRange(count, rows);
			AtomicIntegerArray visits = new AtomicIntegerArray(count);
			AtomicInteger started = new AtomicInteger();
			CountDownLatch ready = new CountDownLatch(4);
			List<java.util.concurrent.Future<?>> futures = new ArrayList<>();
			for (int worker = 0; worker < 4; worker++)
				futures.add(pool.submit(() -> {
					started.incrementAndGet();
					ready.countDown();
					try {
						if (!ready.await(5L, TimeUnit.SECONDS))
							throw new AssertionError("worker start barrier");
					} catch (InterruptedException e) {
						throw new AssertionError(e);
					}
					for (LmdbNativeMorselRange.Range r; (r = range.claim(null)) != null;)
						for (long i = r.from(); i < r.to(); i++)
							visits.incrementAndGet((int) i);
				}));
			for (var future : futures)
				future.get(10L, TimeUnit.SECONDS);
			check(started.get() == 4, "real concurrent workers");
			for (int i = 0; i < count; i++)
				check(visits.get(i) == 1, "duplicate or missing claimed root");
		}
		System.out.println("PASS four-worker exactly-once dynamic claims");
	}

	static void exchange(ExecutorService pool, int repetitions) throws Exception {
		for (int test = 0; test < repetitions; test++) {
			NativeCancellationToken parent = new NativeCancellationToken();
			AtomicInteger exited = new AtomicInteger();
			AtomicIntegerArray seen = new AtomicIntegerArray(2048);
			LmdbNativeMorselRange range = new LmdbNativeMorselRange(seen.length(), 17);
			try (LmdbNativeMorselExecutor<LmdbNativeMorselRange.Range> execution =
					new LmdbNativeMorselExecutor<>(4, 1 + test % 7, parent)) {
				execution.start(pool, (worker, e) -> {
					try {
						for (LmdbNativeMorselRange.Range r; (r = range.claim(null)) != null;)
							if (!e.emit(r))
								return;
					} finally {
						exited.incrementAndGet();
					}
				});
				for (LmdbNativeMorselRange.Range r; (r = execution.take()) != null;)
					for (long i = r.from(); i < r.to(); i++)
						seen.incrementAndGet((int) i);
				execution.finish();
			}
			check(exited.get() == 4 && !parent.isCancellationRequested(), "normal lifecycle and parent isolation");
			for (int i = 0; i < seen.length(); i++)
				check(seen.get(i) == 1, "exchange lost or duplicated output");
		}
		System.out.println("PASS bounded streaming exchange, full drainage and parent-token isolation");
	}

	static void earlyClose(ExecutorService pool) throws Exception {
		NativeCancellationToken parent = new NativeCancellationToken();
		CountDownLatch running = new CountDownLatch(2);
		AtomicInteger exited = new AtomicInteger();
		AtomicInteger produced = new AtomicInteger();
		LmdbNativeMorselExecutor<Integer> execution = new LmdbNativeMorselExecutor<>(2, 1, parent);
		execution.start(pool, (index, e) -> {
			running.countDown();
			try {
				while (!e.stopped()) {
					produced.incrementAndGet();
					if (!e.emit(index))
						return;
				}
			} finally {
				exited.incrementAndGet();
			}
		});
		check(running.await(5L, TimeUnit.SECONDS), "workers running before close");
		execution.close();
		execution.close();
		check(exited.get() == 2, "close returned before worker finally");
		check(produced.get() <= 3, "bounded queue failed backpressure");
		check(!parent.isCancellationRequested(), "early close cancelled parent query");
	}

	static void workerFailure(ExecutorService pool) throws Exception {
		IOException injected = new IOException("injected worker read failure");
		CountDownLatch started = new CountDownLatch(2);
		AtomicInteger exited = new AtomicInteger();
		LmdbNativeMorselExecutor<Integer> execution = new LmdbNativeMorselExecutor<>(2, 1,
				new NativeCancellationToken());
		execution.start(pool, (index, e) -> {
			started.countDown();
			try {
				if (!started.await(5L, TimeUnit.SECONDS))
					throw new AssertionError("failure test worker barrier");
				if (index == 0)
					throw injected;
				while (!e.stopped())
					e.emit(1);
			} finally {
				exited.incrementAndGet();
			}
		});
		try {
			while (execution.take() != null) { }
			execution.finish();
			throw new AssertionError("worker failure was hidden");
		} catch (IOException observed) {
			check(observed == injected, "checked failure identity");
			check(exited.get() == 2, "failure published before worker cleanup");
		} finally {
			execution.close(); // Do not rethrow an already-observed exception or self-suppress it.
		}
	}

	static void partialSubmission(ExecutorService pool) throws Exception {
		CountDownLatch running = new CountDownLatch(1);
		AtomicInteger submitted = new AtomicInteger();
		AtomicInteger exited = new AtomicInteger();
		RejectedExecutionException rejected = new RejectedExecutionException("injected partial submission");
		Executor rejecting = task -> {
			if (submitted.getAndIncrement() != 0) {
				try {
					check(running.await(5L, TimeUnit.SECONDS), "first worker started");
				} catch (InterruptedException e) {
					throw new AssertionError(e);
				}
				throw rejected;
			}
			pool.execute(task);
		};
		try (LmdbNativeMorselExecutor<Integer> execution = new LmdbNativeMorselExecutor<>(4, 1,
				new NativeCancellationToken())) {
			try {
				execution.start(rejecting, (index, e) -> {
					running.countDown();
					try {
						while (e.emit(1)) { }
					} finally {
						exited.incrementAndGet();
					}
				});
				throw new AssertionError("submission rejection hidden");
			} catch (RejectedExecutionException actual) {
				check(actual == rejected, "rejection identity");
				check(exited.get() == 1, "submitted worker not joined after rejection");
			}
		}
	}

	static void concurrentFailures(ExecutorService pool) throws Exception {
		CountDownLatch running = new CountDownLatch(2);
		IOException one = new IOException("one"), two = new IOException("two");
		try (LmdbNativeMorselExecutor<Integer> execution = new LmdbNativeMorselExecutor<>(2, 1,
				new NativeCancellationToken())) {
			execution.start(pool, (index, e) -> {
				running.countDown();
				if (!running.await(5L, TimeUnit.SECONDS))
					throw new AssertionError("concurrent failure barrier");
				throw index == 0 ? one : two;
			});
			try {
				execution.finish();
				throw new AssertionError("concurrent errors disappeared");
			} catch (IOException failure) {
				check(failure == one || failure == two, "first error preserved");
				check(failure.getSuppressed().length == 1, "secondary worker failure retained");
			}
		}
		System.out.println("PASS early close, checked worker failures, concurrent failures and partial submission cleanup");
	}

	static void interruptedClose(ExecutorService pool) throws Exception {
		CountDownLatch running = new CountDownLatch(1);
		AtomicInteger exited = new AtomicInteger();
		LmdbNativeMorselExecutor<Integer> execution = new LmdbNativeMorselExecutor<>(1, 1,
				new NativeCancellationToken());
		execution.start(pool, (index, e) -> {
			running.countDown();
			try {
				while (e.emit(1)) { }
			} finally {
				exited.incrementAndGet();
			}
		});
		check(running.await(5L, TimeUnit.SECONDS), "interrupted-close worker started");
		Thread.currentThread().interrupt();
		try {
			execution.close();
			check(Thread.currentThread().isInterrupted(), "close lost interruption");
			check(exited.get() == 1, "interrupted close returned before worker exit");
		} finally {
			Thread.interrupted();
		}
	}

	static void parentCancellation(ExecutorService pool) throws Exception {
		NativeCancellationToken parent = new NativeCancellationToken();
		NativeCancellationToken child = new NativeCancellationToken(parent);
		child.requestCancellation();
		check(child.isCancellationRequested() && !parent.isCancellationRequested(), "child cancellation leaked upward");
		NativeCancellationToken sibling = new NativeCancellationToken(parent);
		parent.requestCancellation();
		check(sibling.isCancellationRequested(), "parent cancellation was not inherited");
		AtomicInteger ran = new AtomicInteger();
		try (LmdbNativeMorselExecutor<Integer> execution = new LmdbNativeMorselExecutor<>(3, 1, parent)) {
			execution.start(pool, (index, e) -> ran.incrementAndGet());
			execution.finish();
			check(execution.take() == null && ran.get() == 0, "cancelled parent launched query work");
		}
		System.out.println("PASS interrupted join, linked cancellation and pre-cancelled task admission");
	}
}
