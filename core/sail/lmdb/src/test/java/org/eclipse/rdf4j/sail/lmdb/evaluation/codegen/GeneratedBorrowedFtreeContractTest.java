/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb.evaluation.codegen;

import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

import javax.tools.ToolProvider;

import org.junit.jupiter.api.Test;

/** Compiles actual generated Java using the JDK, independently of Janino availability. */
public class GeneratedBorrowedFtreeContractTest {
	public static JaninoKernel compile(String className, String source, Path output) throws Exception {
		Path file = output.resolve(className.replace('.', '/') + ".java");
		Files.createDirectories(file.getParent());
		Files.writeString(file, source);
		var compiler = ToolProvider.getSystemJavaCompiler();
		if (compiler == null)
			throw new AssertionError("full JDK required");
		int result = compiler.run(null, null, null, "-classpath", System.getProperty("java.class.path"), "-d",
				output.toString(), file.toString());
		if (result != 0)
			throw new AssertionError("generated source failed javac: " + file);
		try (var loader = new URLClassLoader(new java.net.URL[] { output.toUri().toURL() },
				GeneratedBorrowedFtreeContractTest.class.getClassLoader())) {
			return (JaninoKernel) loader.loadClass(className).getConstructor().newInstance();
		}
	}

	public static void remove(Path p) throws Exception {
		try (var paths = Files.walk(p)) {
			for (Path f : paths.sorted(Comparator.reverseOrder()).toList())
				Files.delete(f);
		}
	}

	private static void equal(long expected, long actual) {
		if (expected != actual)
			throw new AssertionError(expected + " != " + actual);
	}

	private static boolean on(long[] bits, int i) {
		return (bits[i >>> 6] & (1L << i)) != 0;
	}

	private static long[] selection(Random r, int n) {
		long[] b = new long[(n + 63) / 64];
		for (int i = 0; i < n; i++)
			if (r.nextInt(5) > 0)
				b[i >>> 6] |= 1L << i;
		return b;
	}

	private static long sum(long[] weights, long[] bits, int[] offsets, int parent) {
		long n = 0;
		for (int i = offsets[parent]; i < offsets[parent + 1]; i++)
			if (on(bits, i))
				n += weights[i];
		return n;
	}

	private static long[] weights(Random r, int n) {
		long[] a = new long[n];
		for (int i = 0; i < n; i++)
			a[i] = 1 + r.nextInt(4);
		return a;
	}

	@Test
	public void randomizedMixedBorrowedPackedCardinalitiesAndOutsideCounts() throws Exception {
		Path dir = Files.createTempDirectory("borrowed-kernel-");
		try {
			int[][] topology = { { 1, 2 }, { 3 }, {}, {} };
			JaninoKernel kernel = compile("org.eclipse.rdf4j.sail.lmdb.evaluation.codegen.TestBorrowedTopology",
					PackedFtreeKernelSource.source(topology, 0, "TestBorrowedTopology"), dir);
			Random r = new Random(5128);
			for (int trial = 0; trial < 400; trial++) {
				boolean b2 = (trial & 1) != 0, b3 = (trial & 2) != 0, outside = (trial & 4) != 0;
				int[] sizes = { 4, 12, b2 ? 0 : 16, b3 ? 0 : 24 };
				int[][] offsets = { null, { 0, 3, 6, 9, 12 }, b2 ? null : new int[] { 0, 4, 8, 12, 16 },
						b3 ? null : new int[] { 0, 2, 4, 6, 8, 10, 12, 14, 16, 18, 20, 22, 24 } };
				long[][] sel = new long[4][], wt = new long[4][], sub = new long[4][], out = new long[4][];
				for (int i = 0; i < 4; i++) {
					sel[i] = selection(r, sizes[i]);
					wt[i] = weights(r, sizes[i]);
					sub[i] = new long[sizes[i]];
					out[i] = outside ? new long[sizes[i]] : null;
				}
				PackedFtreeContext p = new PackedFtreeContext(sel, wt, sub, out, offsets, sizes, new int[4],
						sizes.clone(), new boolean[4]);
				p.needOutsideCounts = outside;
				if (b2)
					p.borrowedCounts[2] = weights(r, 4);
				if (b3)
					p.borrowedCounts[3] = weights(r, 12);
				long[] s1 = new long[12], s2 = new long[4], branch1 = new long[4];
				long expected = 0;
				for (int root = 0; root < 4; root++) {
					s2[root] = b2 ? p.borrowedCounts[2][root] : sum(wt[2], sel[2], offsets[2], root);
					for (int j = offsets[1][root]; j < offsets[1][root + 1]; j++)
						if (on(sel[1], j)) {
							s1[j] = wt[1][j] * (b3 ? p.borrowedCounts[3][j] : sum(wt[3], sel[3], offsets[3], j));
							branch1[root] += s1[j];
						}
					if (on(sel[0], root))
						expected += wt[0][root] * branch1[root] * s2[root];
				}
				kernel.bind(new KernelContext(null, new long[0], new long[0], null).withPackedFtree(p));
				kernel.fill(new long[0], 1);
				equal(expected, p.totalRows);
				for (int root = 0; root < 4; root++) {
					equal(on(sel[0], root) ? wt[0][root] * branch1[root] * s2[root] : 0, sub[0][root]);
					if (outside)
						equal(on(sel[0], root) ? 1 : 0, out[0][root]);
					for (int j = offsets[1][root]; j < offsets[1][root + 1]; j++) {
						equal(s1[j], sub[1][j]);
						long o = on(sel[0], root) && on(sel[1], j) ? wt[0][root] * s2[root] : 0;
						if (outside)
							equal(o, out[1][j]);
						if (outside && !b3)
							for (int k = offsets[3][j]; k < offsets[3][j + 1]; k++)
								equal(on(sel[3], k) ? o * wt[1][j] : 0, out[3][k]);
					}
					if (outside && !b2)
						for (int k = offsets[2][root]; k < offsets[2][root + 1]; k++)
							equal(on(sel[0], root) && on(sel[2], k) ? wt[0][root] * branch1[root] : 0, out[2][k]);
				}
			}
			kernel.close();
		} finally {
			remove(dir);
		}
	}

	@Test
	public void metadataOnlyCountAndOverflowAreExact() throws Exception {
		Path dir = Files.createTempDirectory("borrowed-count-");
		try {
			JaninoKernel kernel = compile("org.eclipse.rdf4j.sail.lmdb.evaluation.codegen.TestBorrowedCount",
					PackedFtreeKernelSource.source(new int[][] { { 1, 2 }, {}, {} }, 0, "TestBorrowedCount"), dir);
			PackedFtreeContext p = new PackedFtreeContext(new long[][] { { 1 }, {}, {} }, new long[3][],
					new long[][] { new long[1], {}, {} }, new long[3][], new int[3][], new int[] { 1, 0, 0 },
					new int[3], new int[] { 1, 0, 0 }, new boolean[3]);
			p.needOutsideCounts = false;
			p.borrowedCounts[1] = new long[] { 3_000_000_000L };
			p.borrowedCounts[2] = new long[] { 2 };
			kernel.bind(new KernelContext(null, new long[0], new long[0], null).withPackedFtree(p));
			kernel.fill(new long[0], 1);
			equal(6_000_000_000L, p.totalRows);
			p.borrowedCounts[1][0] = Long.MAX_VALUE;
			kernel.bind(new KernelContext(null, new long[0], new long[0], null).withPackedFtree(p));
			try {
				kernel.fill(new long[0], 1);
				throw new AssertionError("overflow accepted");
			} catch (org.eclipse.rdf4j.query.QueryEvaluationException expected) {
			}
		} finally {
			remove(dir);
		}
	}

	@Test
	public void invalidShapeRejectedAndSourceContainsNoData() {
		String a = PackedFtreeKernelSource.source(new int[][] { { 1 }, {} }, 0, "Shape");
		if (!a.equals(PackedFtreeKernelSource.source(new int[][] { { 1 }, {} }, 0, "Shape")))
			throw new AssertionError("nondeterministic source");
		for (int[][] bad : new int[][][] { { { 1, 1 }, {} }, { {}, {} } }) {
			try {
				PackedFtreeKernelSource.source(bad, 0, "Bad");
				throw new AssertionError("invalid tree admitted");
			} catch (IllegalArgumentException expected) {
			}
		}
	}

	@Test
	public void topologyKernelObservesCancellationInsideDenseAndSparseTraversal() throws Exception {
		Path dir = Files.createTempDirectory("ftree-cancel-");
		try {
			JaninoKernel kernel = compile("org.eclipse.rdf4j.sail.lmdb.evaluation.codegen.TestTopologyCancel",
					PackedFtreeKernelSource.source(new int[][] { {} }, 0, "TestTopologyCancel"), dir);
			for (boolean dense : new boolean[] { true, false }) {
				int n = dense ? 8192 : 1 << 19;
				long[] bits = new long[(n + 63) / 64];
				if (dense)
					Arrays.fill(bits, -1L);
				PackedFtreeContext p = new PackedFtreeContext(new long[][] { bits }, new long[1][],
						new long[][] { new long[n] },
						new long[1][], new int[1][], new int[] { n }, new int[1], new int[] { n }, new boolean[1]);
				p.needOutsideCounts = false;
				int[] polls = { 0 };
				int threshold = dense ? 3 : 35;
				KernelCancellation cancellation = new KernelCancellation(System.nanoTime() + 1_000_000_000_000L, null,
						() -> ++polls[0] >= threshold);
				KernelContext context = new KernelContext(null, new long[0], new long[0], null).withPackedFtree(p);
				context.cancellation = cancellation;
				kernel.bind(context);
				try {
					kernel.fill(new long[0], 0);
					throw new AssertionError("cancelled topology completed");
				} catch (KernelQueryCancelledException expected) {
				}
				equal(threshold, polls[0]);
				equal(0, p.totalRows);
			}
			kernel.close();
			var field = kernel.getClass().getDeclaredField("p");
			field.setAccessible(true);
			if (field.get(kernel) != null)
				throw new AssertionError("retained closed chunk");
		} finally {
			remove(dir);
		}
	}

	@Test
	public void topologyKernelChecksCancellationBeforeAnyWork() throws Exception {
		Path dir = Files.createTempDirectory("ftree-cancel-first-");
		try {
			JaninoKernel kernel = compile("org.eclipse.rdf4j.sail.lmdb.evaluation.codegen.TestTopologyAlreadyCancelled",
					PackedFtreeKernelSource.source(new int[][] { {} }, 0, "TestTopologyAlreadyCancelled"), dir);
			KernelCancellation cancellation = new KernelCancellation(System.nanoTime() + 1_000_000_000_000L);
			cancellation.cancel();
			// Null packed data is intentional: cancellation must precede reading it.
			KernelContext context = new KernelContext(null, new long[0], new long[0], null);
			context.cancellation = cancellation;
			kernel.bind(context);
			try {
				kernel.fill(new long[0], 0);
				throw new AssertionError("cancelled topology ran");
			} catch (KernelCancelledException expected) {
			}
			kernel.close();
		} finally {
			remove(dir);
		}
	}

}
