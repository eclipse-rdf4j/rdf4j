/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb.evaluation;

import static org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbNativeAggregateCompiler.RUNTIME_INTERN_BASE;
import static org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbNativeAggregateCompiler.UNKNOWN;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.TripleTerm;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.QueryEvaluationException;

/**
 * Append-only, evaluation-owned runtime values. A hash slot is one primitive word: a cached 32-bit hash and an
 * unsigned ordinal+1. The payload pages keep the original Value and one publication word containing the canonical
 * term ordinal and the unresolved-membership bit. No boxed IDs, structural keys, per-term nodes, or separate
 * canonicalization cache are allocated. Spelling and RDF-term equality are deliberately separate.
 *
 * Readers acquire a published slot (or payload word); duplicates take no monitor. A new spelling locks only its hash
 * stripe and reuses the already-probed empty slot when it is still valid. Growth rehashes cached hashes, never moves
 * payloads, and publishes a new immutable index descriptor. Pathological cheap-hash clusters switch that stripe to
 * seeded content hashing. Every match still checks exact RDF equality AND output spelling; hashes are never identity.
 */
final class NativeRuntimeValueTable implements AutoCloseable {
	static final String MAX_BYTES_PROPERTY = "rdf4j.lmdb.runtimeValues.maxBytes";
	private static final int STRIPE_BITS = 4;
	private static final int STRIPES = 1 << STRIPE_BITS;
	private static final int PAGE_SHIFT = 10;
	private static final int PAGE_SIZE = 1 << PAGE_SHIFT;
	private static final int PAGE_MASK = PAGE_SIZE - 1;
	private static final long DENSE_LIMIT = 1L << 24;
	/** A bounded local ordinal, not a narrowing conversion of a dictionary ID. */
	private static final long ORDINAL_LIMIT = 0xffff_fffeL;
	/** Count identical full hashes, not ordinary occupied buckets (which legitimately cluster at 75% load). */
	private static final int HARDEN_AFTER = 48;
	private static final int MAX_PROBES = 4096;
	private static final Page[] NO_PAGES = new Page[0];
	private static final Index EMPTY_INDEX = new Index(new long[0], false);
	private static final VarHandle LONGS = MethodHandles.arrayElementVarHandle(long[].class);
	private static final VarHandle PAGES = MethodHandles.arrayElementVarHandle(Page[].class);

	private final AtomicLong nextId;
	private final Budget budget;
	private final Stripe[] stripes = new Stripe[STRIPES];
	private final Object pageLock = new Object();
	private final AtomicInteger count = new AtomicInteger();
	private final AtomicLong bytes = new AtomicLong();
	private volatile Page[] pages = NO_PAGES;
	/** Only exceptionally sparse/high ordinals use a map; the ordinary dense path never boxes. */
	private volatile ConcurrentHashMap<Long, SparseValue> sparse;
	private volatile boolean closed;

	NativeRuntimeValueTable(AtomicLong nextId, Budget budget) {
		this.nextId = nextId;
		this.budget = budget;
		reserve(1024); // table, directory, stripes, counters and monitors; conservative JVM-independent charge
		for (int i = 0; i < STRIPES; i++) {
			stripes[i] = new Stripe();
		}
	}

	long intern(Value value, boolean unresolved) {
		checkOpen();
		if (value == null) {
			return UNKNOWN;
		}
		int cheapHash = cheapHash(value);
		Stripe stripe = stripes[cheapHash >>> (32 - STRIPE_BITS)];
		Index index = stripe.index;
		int hash = index.hardened ? contentHash(value, stripe.seed) : cheapHash;
		long[] slots = index.slots;
		if (slots.length == 0) {
			return insert(stripe, index, value, unresolved, hash, -1, 0L);
		}
		int mask = slots.length - 1;
		int slot = hash & mask;
		long canonical = 0L;
		for (int probes = 0, hashCollisions = 0;; probes++) {
			long entry = (long) LONGS.getAcquire(slots, slot);
			if (entry == 0L) {
				return insert(stripe, index, value, unresolved, hash, slot, canonical);
			}
			if ((int) (entry >>> 32) == hash) {
				hashCollisions++;
				long id = idOfEntry(entry);
				Value candidate = publishedValue(entry);
				if (sameSpelling(value, candidate)) {
					return id;
				}
				canonical = id; // possible semantic alias; inspect RDF equality only when inserting a spelling
			}
			if (probes >= MAX_PROBES) {
				throw probeLimit();
			}
			if ((hashCollisions >= HARDEN_AFTER || probes >= 512) && !index.hardened) {
				return hardenAndIntern(stripe, value, unresolved);
			}
			slot = (slot + 1) & mask;
		}
	}

	/**
	 * Probe an already-computed xsd:string label without manufacturing a Literal just to discard it on a hit.
	 * Admitted native expressions use this before materialization; the normal Value path uses the very same table.
	 */
	long internString(String label) {
		checkOpen();
		Objects.requireNonNull(label, "generated string label");
		int hash = mix(label.hashCode());
		Stripe stripe = stripes[hash >>> (32 - STRIPE_BITS)];
		Index index = stripe.index;
		if (index.hardened) {
			// Rare adversarial-hash stripe: retain the single exact general implementation.
			return intern(SimpleValueFactory.getInstance().createLiteral(label), true);
		}
		long[] slots = index.slots;
		if (slots.length == 0) {
			return insert(stripe, index, SimpleValueFactory.getInstance().createLiteral(label), true, hash, -1, 0L);
		}
		int mask = slots.length - 1;
		int slot = hash & mask;
		for (int probes = 0, hashCollisions = 0;; probes++) {
			long entry = (long) LONGS.getAcquire(slots, slot);
			if (entry == 0L) {
				return insert(stripe, index, SimpleValueFactory.getInstance().createLiteral(label), true, hash, slot, 0L);
			}
			if ((int) (entry >>> 32) == hash) {
				hashCollisions++;
				long id = idOfEntry(entry);
				Value candidate = publishedValue(entry);
				if (candidate instanceof Literal literal && label.equals(literal.getLabel())
						&& literal.getLanguage().isEmpty() && literal.getBaseDirection() == Literal.BaseDirection.NONE
						&& "http://www.w3.org/2001/XMLSchema#string".equals(literal.getDatatype().stringValue())) {
					return id;
				}
			}
			if (probes >= MAX_PROBES) {
				throw probeLimit();
			}
			if (hashCollisions >= HARDEN_AFTER || probes >= 512) {
				return hardenAndIntern(stripe, SimpleValueFactory.getInstance().createLiteral(label), true);
			}
			slot = (slot + 1) & mask;
		}
	}

	private long insert(Stripe stripe, Index seen, Value value, boolean unresolved, int hash,
			int emptySlot, long canonical) {
		synchronized (stripe) {
			checkOpen();
			Index index = stripe.index;
			// With append-only slots, an unchanged empty terminator proves no equal key was inserted since probing.
			// Recheck the descriptor too: resize/hardening may change both the position and the hashing policy.
			if (index != seen || emptySlot < 0 || index.slots[emptySlot] != 0L) {
				hash = index.hardened ? contentHash(value, stripe.seed) : cheapHash(value);
				canonical = 0L;
				if (index.slots.length == 0) {
					index = resize(stripe, 32, index.hardened);
				}
				int mask = index.slots.length - 1;
				emptySlot = hash & mask;
				for (int probes = 0, hashCollisions = 0; index.slots[emptySlot] != 0L; probes++) {
					long entry = index.slots[emptySlot];
					if ((int) (entry >>> 32) == hash) {
						hashCollisions++;
						long id = idOfEntry(entry);
						Value candidate = publishedValue(entry);
						if (sameSpelling(value, candidate)) {
							return id;
						}
						canonical = id;
					}
					if (probes >= MAX_PROBES) {
						throw probeLimit();
					}
					if ((hashCollisions >= HARDEN_AFTER || probes >= 512) && !index.hardened) {
						return hardenAndIntern(stripe, value, unresolved);
					}
					emptySlot = (emptySlot + 1) & mask;
				}
			}
			if (stripe.size >= index.slots.length - (index.slots.length >>> 2)) {
				if (index.slots.length == 1 << 30) {
					throw new QueryEvaluationException("Runtime-value hash stripe reached its capacity limit");
				}
				index = resize(stripe, index.slots.length << 1, index.hardened);
				int mask = index.slots.length - 1;
				emptySlot = hash & mask;
				while (index.slots[emptySlot] != 0L) {
					emptySlot = (emptySlot + 1) & mask;
				}
			}
			long payloadBytes = retainedValueBytes(value, 0);
			reserve(payloadBytes);
			boolean published = false;
			try {
				long id = nextId.getAndIncrement();
				long ordinal = id - RUNTIME_INTERN_BASE;
				if (Long.compareUnsigned(ordinal, ORDINAL_LIMIT) >= 0) {
					throw new QueryEvaluationException("Runtime-value ordinal capacity exceeded; ID was not published");
				}
				if (canonical == 0L) {
					canonical = id;
				} else {
					// A full-hash candidate need not be RDF-equal. Only an inserted spelling needs semantic coalescing.
					canonical = canonicalForNewSpelling(index, value, hash, id);
				}
				long metadata = ((canonical - RUNTIME_INTERN_BASE + 1L) << 1) | (unresolved ? 1L : 0L);
				publish(ordinal, value, metadata);
				LONGS.setRelease(index.slots, emptySlot, ((long) hash << 32) | (ordinal + 1L));
				stripe.size++;
				count.incrementAndGet();
				published = true;
				return id;
			} finally {
				if (!published) {
					release(payloadBytes);
				}
			}
		}
	}

	private long hardenAndIntern(Stripe stripe, Value value, boolean unresolved) {
		synchronized (stripe) {
			checkOpen();
			if (!stripe.index.hardened) {
				resize(stripe, stripe.index.slots.length, true);
			}
			return intern(value, unresolved);
		}
	}

	/** Caller owns the stripe. Stored full hashes make ordinary growth payload-free. */
	private Index resize(Stripe stripe, int capacity, boolean hardened) {
		Index old = stripe.index;
		long allocated = 32L + 8L * capacity;
		reserve(allocated);
		boolean published = false;
		try {
			long[] replacement = new long[capacity];
			int mask = capacity - 1;
			for (long entry : old.slots) {
				if (entry == 0L) {
					continue;
				}
				int hash = (int) (entry >>> 32);
				if (hardened != old.hardened) {
					hash = contentHash(publishedValue(entry), stripe.seed);
					entry = ((long) hash << 32) | (entry & 0xffff_ffffL);
				}
				int slot = hash & mask;
				for (int probes = 0; replacement[slot] != 0L; probes++) {
					if (probes >= MAX_PROBES) {
						throw probeLimit();
					}
					slot = (slot + 1) & mask;
				}
				replacement[slot] = entry;
			}
			Index index = new Index(replacement, hardened);
			stripe.index = index;
			published = true;
			if (old.slots.length != 0) {
				release(32L + 8L * old.slots.length);
			}
			return index;
		} finally {
			if (!published) {
				release(allocated);
			}
		}
	}


	/** An acquired hash entry already publishes this payload. Do not repeat the metadata ownership probe here. */
	private Value publishedValue(long entry) {
		long ordinal = (entry & 0xffff_ffffL) - 1L;
		if (ordinal < DENSE_LIMIT) {
			Page[] directory = pages;
			int pageIndex = (int) (ordinal >>> PAGE_SHIFT);
			if (pageIndex < directory.length) {
				Page page = directory[pageIndex];
				if (page != null) {
					return page.values[(int) ordinal & PAGE_MASK];
				}
			}
			return null; // a concurrent close detached the pages; insertion's closed check fails the query
		}
		ConcurrentHashMap<Long, SparseValue> map = sparse;
		SparseValue value = map == null ? null : map.get(RUNTIME_INTERN_BASE + ordinal);
		return value == null ? null : value.value;
	}

	long key(long id) {
		long metadata = metadata(id);
		return metadata == 0L ? 0L : RUNTIME_INTERN_BASE + (metadata >>> 1) - 1L;
	}

	boolean unresolved(long id) {
		return (metadata(id) & 1L) != 0L;
	}

	boolean contains(long id) {
		return metadata(id) != 0L;
	}

	Value valueOf(long id) {
		long ordinal = id - RUNTIME_INTERN_BASE;
		if (Long.compareUnsigned(ordinal, DENSE_LIMIT) < 0) {
			Page[] directory = pages;
			int pageIndex = (int) (ordinal >>> PAGE_SHIFT);
			if (pageIndex < directory.length) {
				Page page = (Page) PAGES.getAcquire(directory, pageIndex);
				int offset = (int) ordinal & PAGE_MASK;
				if (page != null && (long) LONGS.getAcquire(page.metadata, offset) != 0L) {
					return page.values[offset];
				}
			}
			return null;
		}
		ConcurrentHashMap<Long, SparseValue> map = sparse;
		SparseValue value = map == null ? null : map.get(id);
		return value == null ? null : value.value;
	}

	private long metadata(long id) {
		long ordinal = id - RUNTIME_INTERN_BASE;
		if (Long.compareUnsigned(ordinal, DENSE_LIMIT) < 0) {
			Page[] directory = pages;
			int pageIndex = (int) (ordinal >>> PAGE_SHIFT);
			if (pageIndex < directory.length) {
				Page page = (Page) PAGES.getAcquire(directory, pageIndex);
				if (page != null) {
					return (long) LONGS.getAcquire(page.metadata, (int) ordinal & PAGE_MASK);
				}
			}
			return 0L;
		}
		ConcurrentHashMap<Long, SparseValue> map = sparse;
		SparseValue value = map == null ? null : map.get(id);
		return value == null ? 0L : value.metadata;
	}

	private void publish(long ordinal, Value value, long metadata) {
		if (ordinal >= DENSE_LIMIT) {
			synchronized (pageLock) {
				if (sparse == null) {
					reserve(128L);
					sparse = new ConcurrentHashMap<>();
				}
			}
			reserve(128L); // boxed ID, sparse record, map node, and amortized table growth
			sparse.put(RUNTIME_INTERN_BASE + ordinal, new SparseValue(value, metadata));
			return;
		}
		int pageIndex = (int) (ordinal >>> PAGE_SHIFT);
		Page[] directory = pages;
		Page page = pageIndex < directory.length ? (Page) PAGES.getAcquire(directory, pageIndex) : null;
		if (page == null) {
			page = createPage(pageIndex);
		}
		int offset = (int) ordinal & PAGE_MASK;
		page.values[offset] = value;
		// This single release publishes Value, exact ownership, key, and membership state together.
		LONGS.setRelease(page.metadata, offset, metadata);
	}

	private Page createPage(int pageIndex) {
		synchronized (pageLock) {
			Page[] directory = pages;
			if (pageIndex >= directory.length) {
				int length = Integer.highestOneBit(pageIndex + 1) << 1;
				long allocation = 32L + 8L * length;
				reserve(allocation);
				Page[] grown;
				try {
					grown = Arrays.copyOf(directory, length);
				} catch (Throwable failure) {
					release(allocation);
					throw failure;
				}
				pages = grown;
				if (directory.length != 0) {
					release(32L + 8L * directory.length);
				}
				directory = grown;
			}
			Page page = (Page) PAGES.getAcquire(directory, pageIndex);
			if (page == null) {
				long allocation = 96L + 16L * PAGE_SIZE; // charge 8-byte references even with compressed oops
				reserve(allocation);
				try {
					page = new Page();
				} catch (Throwable failure) {
					release(allocation);
					throw failure;
				}
				PAGES.setRelease(directory, pageIndex, page);
			}
			return page;
		}
	}

	int size() {
		return count.get();
	}

	long accountedBytes() {
		return bytes.get();
	}

	int hardenedStripes() {
		int n = 0;
		for (Stripe stripe : stripes) {
			if (stripe.index.hardened) {
				n++;
			}
		}
		return n;
	}

	private void reserve(long amount) {
		budget.reserve(amount);
		bytes.addAndGet(amount);
	}

	private void release(long amount) {
		bytes.addAndGet(-amount);
		budget.release(amount);
	}

	private static QueryEvaluationException probeLimit() {
		return new QueryEvaluationException("Runtime-value hash chain exceeded " + MAX_PROBES
				+ " probes even after content-hash protection; refusing pathological key work without dropping rows");
	}

	private void checkOpen() {
		if (closed) {
			throw new IllegalStateException("runtime values belong to a closed evaluation");
		}
	}

	@Override
	public void close() {
		synchronized (this) {
			if (closed) {
				return;
			}
			closed = true;
			// Closing never holds pageLock or budget while waiting for a writer's stripe.
			for (Stripe stripe : stripes) {
				synchronized (stripe) {
					stripe.index = EMPTY_INDEX;
				}
			}
			pages = NO_PAGES;
			sparse = null;
			count.set(0);
			budget.release(bytes.getAndSet(0L));
		}
	}

	private static long idOfEntry(long entry) {
		return RUNTIME_INTERN_BASE + (entry & 0xffff_ffffL) - 1L;
	}

	/** Cheap cached lexical hashes; semantic equality, not an implementation-specific Value.hashCode, is the contract. */
	private static int cheapHash(Value value) {
		if (value instanceof Literal literal) {
			return mix(literal.getLabel().hashCode());
		}
		if (value instanceof TripleTerm triple) {
			return tripleHash(triple, 0);
		}
		return mix(31 * value.getType().ordinal() + value.stringValue().hashCode());
	}

	private static int nestedHash(Value value, int depth) {
		return value instanceof TripleTerm triple ? tripleHash(triple, depth) : cheapHash(value);
	}

	private static int tripleHash(TripleTerm triple, int depth) {
		if (depth >= 256) {
			throw new QueryEvaluationException("Runtime triple-term nesting exceeds 256 levels");
		}
		return mix(31 * (31 * nestedHash(triple.getSubject(), depth + 1)
				+ nestedHash(triple.getPredicate(), depth + 1)) + nestedHash(triple.getObject(), depth + 1));
	}

	private static int mix(int hash) {
		hash ^= hash >>> 16;
		hash *= 0x7feb352d;
		hash ^= hash >>> 15;
		hash *= 0x846ca68b;
		return hash ^ (hash >>> 16);
	}

	/** Exact spelling first: duplicate aliases need neither RDF case-folding nor a canonical-payload read. */
	private static boolean sameSpelling(Value left, Value right) {
		if (left == right) {
			return true;
		}
		if (left instanceof Literal literal && right instanceof Literal other) {
			return literal.getLabel().equals(other.getLabel())
					&& Objects.equals(literal.getLanguage().orElse(null), other.getLanguage().orElse(null))
					&& literal.getBaseDirection() == other.getBaseDirection()
					&& literal.getDatatype().equals(other.getDatatype());
		}
		if (left instanceof TripleTerm triple && right instanceof TripleTerm other) {
			return sameSpelling(triple.getSubject(), other.getSubject())
					&& sameSpelling(triple.getPredicate(), other.getPredicate())
					&& sameSpelling(triple.getObject(), other.getObject());
		}
		return left.equals(right);
	}

	/** Caller owns the stripe. The exact-spelling miss is established; this scan runs only on an actual insertion. */
	private long canonicalForNewSpelling(Index index, Value value, int hash, long ownId) {
		int mask = index.slots.length - 1;
		int slot = hash & mask;
		for (long entry; (entry = index.slots[slot]) != 0L; slot = (slot + 1) & mask) {
			if ((int) (entry >>> 32) == hash && value.equals(publishedValue(entry))) {
				return key(idOfEntry(entry));
			}
		}
		return ownId;
	}

	/** Slow-path content hash is independent of Java's polynomial String hash, including its Aa/BB collision family. */
	private static int contentHash(Value value, long seed) {
		long hash;
		if (value instanceof Literal literal) {
			hash = textHash(literal.getLabel(), seed);
			hash = textHash(literal.getDatatype().stringValue(), hash);
			String language = literal.getLanguage().orElse(null);
			if (language != null) {
				for (int i = 0; i < language.length();) {
					int cp = language.codePointAt(i);
					i += Character.charCount(cp);
					// Matches Java's locale-independent equalsIgnoreCase, including non-ASCII external terms.
					hash = hash * 0x9e3779b185ebca87L + Character.toLowerCase(Character.toUpperCase(cp));
				}
			}
			Literal.BaseDirection direction = literal.getBaseDirection();
			hash ^= (direction == null ? 0L : direction.ordinal() + 1L) * 0xc2b2ae3d27d4eb4fL;
		} else if (value instanceof TripleTerm triple) {
			hash = contentHash(triple.getSubject(), seed);
			hash = contentHash(triple.getPredicate(), hash);
			hash = contentHash(triple.getObject(), hash);
		} else {
			hash = textHash(value.stringValue(), seed ^ value.getType().ordinal());
		}
		hash ^= hash >>> 33;
		hash *= 0xff51afd7ed558ccdL;
		hash ^= hash >>> 33;
		return mix((int) (hash ^ (hash >>> 32)));
	}

	private static long textHash(String text, long seed) {
		long hash = seed ^ text.length();
		for (int i = 0; i < text.length(); i++) {
			hash = hash * 0x9e3779b185ebca87L + text.charAt(i);
		}
		return hash;
	}

	/** Conservative charge for standard RDF Values. This is not a heap-graph/RSS measurement of arbitrary subclasses. */
	private static long retainedValueBytes(Value value, int depth) {
		if (depth > 256) {
			throw new QueryEvaluationException("Runtime triple-term nesting exceeds 256 levels");
		}
		if (value instanceof Literal literal) {
			long bytes = 96L + stringBytes(literal.getLabel()) + 32L + stringBytes(literal.getDatatype().stringValue());
			String language = literal.getLanguage().orElse(null);
			return language == null ? bytes : bytes + 24L + stringBytes(language);
		}
		if (value instanceof TripleTerm triple) {
			return 64L + retainedValueBytes(triple.getSubject(), depth + 1)
					+ retainedValueBytes(triple.getPredicate(), depth + 1)
					+ retainedValueBytes(triple.getObject(), depth + 1);
		}
		return 64L + stringBytes(value.stringValue());
	}

	private static long stringBytes(String value) {
		return 48L + 2L * value.length();
	}

	/** Shared by nested contexts of one query. Limits admission; it does not evict live IDs or pretend to spill Values. */
	static final class Budget {
		private final long limit;
		private long used;
		private long peak;

		Budget(long limit) {
			if (limit <= 0L) {
				throw new IllegalArgumentException(MAX_BYTES_PROPERTY + " must be positive");
			}
			this.limit = limit;
		}

		static Budget configured() {
			String property = System.getProperty(MAX_BYTES_PROPERTY);
			long defaultLimit = Math.max(16L << 20, Math.min(512L << 20, Runtime.getRuntime().maxMemory() / 8L));
			return new Budget(property == null ? defaultLimit : Long.parseLong(property));
		}

		synchronized void reserve(long amount) {
			if (amount < 0L || amount > limit - used) {
				throw new QueryEvaluationException("Runtime-value memory admission refused: requested=" + amount
						+ ", accounted=" + used + ", limit=" + limit + "; configure " + MAX_BYTES_PROPERTY
						+ " or reduce generated-key cardinality. Runtime payload spilling is not implemented.");
			}
			used += amount;
			peak = Math.max(peak, used);
		}

		synchronized void release(long amount) {
			if (amount < 0L || amount > used) {
				throw new IllegalStateException("Unbalanced runtime-value accounting");
			}
			used -= amount;
		}

		synchronized long usedBytes() {
			return used;
		}

		synchronized long peakBytes() {
			return peak;
		}
	}

	private static final class Stripe {
		final long seed = ThreadLocalRandom.current().nextLong();
		volatile Index index = EMPTY_INDEX;
		int size;
	}

	private record Index(long[] slots, boolean hardened) {
	}

	private static final class Page {
		final Value[] values = new Value[PAGE_SIZE];
		final long[] metadata = new long[PAGE_SIZE];
	}

	private record SparseValue(Value value, long metadata) {
	}
}
