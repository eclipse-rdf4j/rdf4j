/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Map;

import org.codehaus.janino.SimpleCompiler;
import org.eclipse.rdf4j.sail.lmdb.model.LmdbValue;
import org.junit.jupiter.api.Test;

/**
 * Routine C investigation: compile a generated projection iterator outside the production path.
 */
class LmdbJaninoProjectionPrototypeTest {

	private static final ProjectionShape SPO_MASK = new ProjectionShape("spoc", 0, 1, 2, -1, 7, "source-embedded");

	@Test
	void generatedProjectionMatchesGenericProjectionSemantics() throws Exception {
		PrototypeCompilation compiled = compileProjection(SPO_MASK);
		long[] binding = { LmdbValue.UNKNOWN_ID, 20L, LmdbValue.UNKNOWN_ID };
		RecordIterator generated = compiled.newIterator(
				iterator(new long[] { 10L, 20L, 30L, 0L }, new long[] { 11L, 20L, 31L, 0L }),
				binding,
				new long[binding.length]);

		assertThat(generated.next()).containsExactly(10L, 20L, 30L);
		assertThat(generated.next()).containsExactly(11L, 20L, 31L);
		assertThat(generated.next()).isNull();
		assertThat(compiled.explain.templateKey).isEqualTo(SPO_MASK.templateKey());
		assertThat(compiled.explain.branchCount).isLessThanOrEqualTo(5);
		assertThat(compiled.explain.sourceLength).isLessThan(5_000);
		assertThat(compiled.explain.bytecodeSize).isPositive();
		assertThat(compiled.explain.compileNanos).isPositive();
	}

	@Test
	void generatedProjectionRejectsConflictingBoundSlot() throws Exception {
		PrototypeCompilation compiled = compileProjection(SPO_MASK);
		long[] binding = { LmdbValue.UNKNOWN_ID, 77L, LmdbValue.UNKNOWN_ID };
		RecordIterator generated = compiled.newIterator(
				iterator(new long[] { 1L, 99L, 3L, 0L }, new long[] { 2L, 77L, 4L, 0L }),
				binding,
				new long[binding.length]);

		assertThat(generated.next()).containsExactly(2L, 77L, 4L);
		assertThat(generated.next()).isNull();
	}

	@Test
	void generatedProjectionOmitsImpossibleContextWork() throws Exception {
		PrototypeCompilation compiled = compileProjection(SPO_MASK);

		assertThat(compiled.source).doesNotContain("quad[3]");
		assertThat(compiled.source).doesNotContain("scratch[3]");
		assertThat(compiled.explain.cacheHit).isFalse();
	}

	private static PrototypeCompilation compileProjection(ProjectionShape shape) throws Exception {
		String packageName = "org.eclipse.rdf4j.sail.lmdb.codegen";
		String simpleName = "GeneratedProjection" + Integer.toUnsignedString(shape.templateKey().hashCode(), 16);
		String className = packageName + "." + simpleName;
		String source = projectionSource(packageName, simpleName, shape);

		SimpleCompiler compiler = new SimpleCompiler();
		compiler.setParentClassLoader(LmdbJaninoProjectionPrototypeTest.class.getClassLoader());
		long started = System.nanoTime();
		compiler.cook(source);
		long compileNanos = System.nanoTime() - started;

		Class<?> clazz = compiler.getClassLoader().loadClass(className);
		CodegenExplain explain = new CodegenExplain(
				shape.templateKey(),
				className,
				source.length(),
				bytecodeSize(compiler.getClassLoader()),
				countBranches(source),
				compileNanos,
				false);
		return new PrototypeCompilation(clazz, source, explain);
	}

	private static String projectionSource(String packageName, String simpleName, ProjectionShape shape) {
		StringBuilder source = new StringBuilder(2_048);
		source.append("package ").append(packageName).append(";\n");
		source.append("public final class ")
				.append(simpleName)
				.append(" implements org.eclipse.rdf4j.sail.lmdb.RecordIterator {\n");
		source.append("  private final org.eclipse.rdf4j.sail.lmdb.RecordIterator base;\n");
		source.append("  private final long[] binding;\n");
		source.append("  private final long[] scratch;\n");
		source.append("  public ")
				.append(simpleName)
				.append("(org.eclipse.rdf4j.sail.lmdb.RecordIterator base, long[] binding, long[] scratch) {\n");
		source.append("    this.base = base;\n");
		source.append("    this.binding = binding;\n");
		source.append("    this.scratch = scratch;\n");
		source.append("  }\n");
		source.append("  public final long[] next() {\n");
		source.append("    long[] quad;\n");
		source.append("    while ((quad = this.base.next()) != null) {\n");
		source.append("      java.lang.System.arraycopy(this.binding, 0, this.scratch, 0, this.binding.length);\n");
		appendSlot(source, shape.subjSlot, TripleStore.SUBJ_IDX);
		appendSlot(source, shape.predSlot, TripleStore.PRED_IDX);
		appendSlot(source, shape.objSlot, TripleStore.OBJ_IDX);
		appendSlot(source, shape.ctxSlot, TripleStore.CONTEXT_IDX);
		source.append("      return this.scratch;\n");
		source.append("    }\n");
		source.append("    return null;\n");
		source.append("  }\n");
		source.append("  public final void close() {\n");
		source.append("    this.base.close();\n");
		source.append("  }\n");
		source.append("}\n");
		return source.toString();
	}

	private static void appendSlot(StringBuilder source, int slot, int quadIndex) {
		if (slot < 0) {
			return;
		}
		source.append("      long bound").append(slot).append(" = this.binding[").append(slot).append("];\n");
		source.append("      long value").append(slot).append(" = quad[").append(quadIndex).append("];\n");
		source.append("      if (bound")
				.append(slot)
				.append(" != org.eclipse.rdf4j.sail.lmdb.model.LmdbValue.UNKNOWN_ID && bound")
				.append(slot)
				.append(" != value")
				.append(slot)
				.append(") {\n");
		source.append("        continue;\n");
		source.append("      }\n");
		source.append("      this.scratch[")
				.append(slot)
				.append("] = bound")
				.append(slot)
				.append(" != org.eclipse.rdf4j.sail.lmdb.model.LmdbValue.UNKNOWN_ID ? bound")
				.append(slot)
				.append(" : value")
				.append(slot)
				.append(";\n");
	}

	private static int countBranches(String source) {
		int branches = 0;
		branches += countOccurrences(source, "if (");
		branches += countOccurrences(source, "while (");
		return branches;
	}

	private static int countOccurrences(String source, String needle) {
		int count = 0;
		int offset = 0;
		while ((offset = source.indexOf(needle, offset)) >= 0) {
			count++;
			offset += needle.length();
		}
		return count;
	}

	@SuppressWarnings("unchecked")
	private static int bytecodeSize(ClassLoader classLoader) throws ReflectiveOperationException {
		Field classes = classLoader.getClass().getDeclaredField("classes");
		classes.setAccessible(true);
		int total = 0;
		for (byte[] bytecode : ((Map<String, byte[]>) classes.get(classLoader)).values()) {
			total += bytecode.length;
		}
		return total;
	}

	private static RecordIterator iterator(long[]... rows) {
		return new RecordIterator() {
			private int offset;

			@Override
			public long[] next() {
				if (offset >= rows.length) {
					return null;
				}
				return rows[offset++];
			}

			@Override
			public void close() {
			}
		};
	}

	private static final class PrototypeCompilation {

		private final Class<?> clazz;
		private final String source;
		private final CodegenExplain explain;

		private PrototypeCompilation(Class<?> clazz, String source, CodegenExplain explain) {
			this.clazz = clazz;
			this.source = source;
			this.explain = explain;
		}

		private RecordIterator newIterator(RecordIterator base, long[] binding, long[] scratch) throws Exception {
			Constructor<?> constructor = clazz.getConstructor(RecordIterator.class, long[].class, long[].class);
			return (RecordIterator) constructor.newInstance(base, binding, scratch);
		}
	}

	private static final class CodegenExplain {

		private final String templateKey;
		private final String className;
		private final int sourceLength;
		private final int bytecodeSize;
		private final int branchCount;
		private final long compileNanos;
		private final boolean cacheHit;

		private CodegenExplain(String templateKey, String className, int sourceLength, int bytecodeSize,
				int branchCount, long compileNanos, boolean cacheHit) {
			this.templateKey = templateKey;
			this.className = className;
			this.sourceLength = sourceLength;
			this.bytecodeSize = bytecodeSize;
			this.branchCount = branchCount;
			this.compileNanos = compileNanos;
			this.cacheHit = cacheHit;
		}
	}

	private static final class ProjectionShape {

		private final String indexOrder;
		private final int subjSlot;
		private final int predSlot;
		private final int objSlot;
		private final int ctxSlot;
		private final int boundMask;
		private final String constantPolicy;

		private ProjectionShape(String indexOrder, int subjSlot, int predSlot, int objSlot, int ctxSlot, int boundMask,
				String constantPolicy) {
			this.indexOrder = indexOrder;
			this.subjSlot = subjSlot;
			this.predSlot = predSlot;
			this.objSlot = objSlot;
			this.ctxSlot = ctxSlot;
			this.boundMask = boundMask;
			this.constantPolicy = constantPolicy;
		}

		private String templateKey() {
			return "projection:index=" + indexOrder + ";mask=" + boundMask + ";s=" + subjSlot + ";p=" + predSlot
					+ ";o=" + objSlot + ";c=" + ctxSlot + ";constants=" + constantPolicy;
		}
	}
}
