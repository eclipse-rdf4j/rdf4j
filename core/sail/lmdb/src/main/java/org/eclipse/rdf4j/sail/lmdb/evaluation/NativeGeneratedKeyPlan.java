/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb.evaluation;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.rdf4j.query.BindingSet;

/**
 * A proof about one terminal's producer, not a guess from the first batch. Only assignments whose result has no
 * consumer other than keying/output (and simple aliases) are admitted. It is deliberately not an optimizer strategy:
 * the same proof and assignments survive ordinary cursors, interpreted IR and emitted IR.
 *
 * An unresolved local value must never be confused with a value proved absent from the dictionary. Joins, OPTIONAL,
 * opaque filters, mixed-provenance UNION arms and entry-bound assignment targets keep store-first identity.
 */
final class NativeGeneratedKeyPlan {
	/** One increment per activated evaluation, never per key or row. */
	static final AtomicLong ACTIVATIONS = new AtomicLong();
	static final String PROPERTY = "rdf4j.lmdb.generatedKeys.enabled";
	static final NativeGeneratedKeyPlan NONE = new NativeGeneratedKeyPlan(Set.of(), 0L);
	private final Set<CopyBinding> assignments;
	private final long targetMask;

	private NativeGeneratedKeyPlan(Set<CopyBinding> assignments, long targetMask) {
		this.assignments = assignments;
		this.targetMask = targetMask;
	}

	static NativeGeneratedKeyPlan distinct(SlotPlan arg, int[] keys) {
		return prove(arg, mask(keys));
	}

	static NativeGeneratedKeyPlan group(SlotPlan arg, int[] keys, AggregateSpec[] aggregates) {
		long needed = mask(keys);
		// Every DISTINCT channel is a key consumer too, including hidden expression slots. Do not activate an
		// all-generated proof while a stored DISTINCT input would require a different key policy.
		for (AggregateSpec aggregate : aggregates) {
			if (aggregate.distinct) {
				if (aggregate.rowSlots != null) {
					needed |= mask(aggregate.rowSlots);
				} else if (aggregate.slot >= 0) {
					needed |= 1L << aggregate.slot;
				} else {
					return NONE;
				}
			}
		}
		return prove(arg, needed);
	}

	private static long mask(int[] slots) {
		long result = 0L;
		for (int slot : slots) {
			if (slot < 0 || slot >= LmdbNativeAggregateCompiler.MAX_NATIVE_SLOTS) {
				throw new IllegalArgumentException("Invalid generated-key slot: " + slot);
			}
			result |= 1L << slot;
		}
		return result;
	}

	private static NativeGeneratedKeyPlan prove(SlotPlan arg, long needed) {
		if (needed == 0L) {
			return NONE;
		}
		Builder proof = new Builder();
		if (!proof.visit(arg, needed, 0L) || proof.assignments.isEmpty()) {
			return NONE;
		}
		return new NativeGeneratedKeyPlan(Collections.unmodifiableSet(proof.assignments), proof.targetMask);
	}

	boolean isEmpty() {
		return assignments.isEmpty();
	}

	boolean accepts(CopyBinding copy) {
		return (targetMask & (1L << copy.targetSlot)) != 0L && assignments.contains(copy);
	}

	boolean enabled(NativeSlotLayout layout, BindingSet entry) {
		if (isEmpty() || "false".equalsIgnoreCase(System.getProperty(PROPERTY))) {
			return false;
		}
		for (long pending = targetMask; pending != 0L; pending &= pending - 1L) {
			if (entry.hasBinding(layout.slotName(Long.numberOfTrailingZeros(pending)))) {
				return false;
			}
		}
		return true;
	}

	int assignmentCount() {
		return assignments.size();
	}

	private static final class Builder {
		final Set<CopyBinding> assignments = Collections.newSetFromMap(new IdentityHashMap<>());
		long targetMask;

		boolean visit(SlotPlan plan, long needed, long observed) {
			if (needed == 0L) {
				return true;
			}
			if (plan instanceof FilterPlan filter) {
				return filter.filterMask >= 0L && (filter.filterMask & needed) == 0L
						&& visit(filter.arg, needed, observed | filter.filterMask);
			}
			if (plan instanceof UnionPlan union) {
				return visit(union.left, needed, observed) && visit(union.right, needed, observed);
			}
			if (!(plan instanceof ExtensionPlan extension)) {
				return false;
			}
			long allTargets = 0L;
			for (CopyBinding copy : extension.copies) {
				long target = 1L << copy.targetSlot;
				if ((allTargets & target) != 0L) {
					return false;
				}
				allTargets |= target;
			}
			long laterReads = observed;
			long written = 0L;
			for (int i = extension.copies.length - 1; i >= 0; i--) {
				CopyBinding copy = extension.copies[i];
				long target = 1L << copy.targetSlot;
				if ((needed & target) != 0L) {
					// The ordinary extension performs a binding compatibility check. Never replace an existing
					// store ID, a repeated assignment, or a value observed by another expression with a local ID.
					if ((extension.arg.producedMask() & target) != 0L || (written & target) != 0L
							|| (laterReads & target) != 0L || copy.termChecked) {
						return false;
					}
					targetMask |= target;
					needed &= ~target;
					if (copy.computedValue != null || copy.semanticValue != null) {
						assignments.add(copy);
					} else if (copy.sourceSlot >= 0) {
						// An alias transports the same local identity; the source assignment still needs proof.
						needed |= 1L << copy.sourceSlot;
						written |= target;
						continue;
					} else {
						// Inline/constant/stored keys keep their existing primitive path. This optimization is for
						// generated values that would otherwise require dictionary membership resolution.
						return false;
					}
				}
				written |= target;
				laterReads |= copy.keyReadMask();
			}
			// All assignments before this extension execute before the remaining locals are produced. Readers
			// inside this extension must not observe those locals (an alias is the sole exception above).
			return (laterReads & needed) == 0L && visit(extension.arg, needed, laterReads);
		}
	}
}
