/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.s3.storage;

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * Manages index permutations for quad (S, P, O, C) storage. Each QuadIndex defines a field ordering (e.g. "spoc",
 * "posc") and provides methods to encode/decode keys in that order, compute pattern scores for query optimization, and
 * construct range scan boundaries.
 *
 * <p>
 * Based on the TripleStore.TripleIndex pattern from the LMDB SAIL module.
 * </p>
 */
public class QuadIndex {

	public static final int SUBJ_IDX = 0;
	public static final int PRED_IDX = 1;
	public static final int OBJ_IDX = 2;
	public static final int CONTEXT_IDX = 3;

	static final int MAX_KEY_LENGTH = 4 * 9; // 4 varints, max 9 bytes each

	private final char[] fieldSeq;
	private final String fieldSeqString;
	private final int[] indexMap;

	/**
	 * Creates a new QuadIndex with the given field sequence.
	 *
	 * @param fieldSeq a 4-character string consisting of 's', 'p', 'o', 'c' in any order
	 * @throws IllegalArgumentException if the field sequence is invalid
	 */
	public QuadIndex(String fieldSeq) {
		if (fieldSeq == null || fieldSeq.length() != 4) {
			throw new IllegalArgumentException("Field sequence must be exactly 4 characters: " + fieldSeq);
		}
		this.fieldSeq = fieldSeq.toCharArray();
		this.fieldSeqString = fieldSeq;
		this.indexMap = buildIndexMap(this.fieldSeq);
	}

	/**
	 * Returns the field sequence for this index.
	 */
	public char[] getFieldSeq() {
		return fieldSeq;
	}

	/**
	 * Returns the field sequence as a String.
	 */
	public String getFieldSeqString() {
		return fieldSeqString;
	}

	/**
	 * Determines the 'score' of this index on the supplied pattern. The higher the score, the better the index is
	 * suited for matching the pattern. Score equals the number of leading bound components. Lowest score is 0, meaning
	 * a sequential scan.
	 *
	 * @param subj    subject ID, or -1 for wildcard
	 * @param pred    predicate ID, or -1 for wildcard
	 * @param obj     object ID, or -1 for wildcard
	 * @param context context ID, or -1 for wildcard
	 * @return pattern score (0-4)
	 */
	public int getPatternScore(long subj, long pred, long obj, long context) {
		int score = 0;
		for (char field : fieldSeq) {
			switch (field) {
			case 's':
				if (subj >= 0) {
					score++;
				} else {
					return score;
				}
				break;
			case 'p':
				if (pred >= 0) {
					score++;
				} else {
					return score;
				}
				break;
			case 'o':
				if (obj >= 0) {
					score++;
				} else {
					return score;
				}
				break;
			case 'c':
				if (context >= 0) {
					score++;
				} else {
					return score;
				}
				break;
			default:
				throw new IllegalStateException(
						"Invalid character '" + field + "' in field sequence: " + new String(fieldSeq));
			}
		}
		return score;
	}

	/**
	 * Writes a quad as varints in index order into the given buffer.
	 *
	 * @param bb      buffer for writing bytes
	 * @param subj    subject ID
	 * @param pred    predicate ID
	 * @param obj     object ID
	 * @param context context ID
	 */
	public void toKey(ByteBuffer bb, long subj, long pred, long obj, long context) {
		for (char field : fieldSeq) {
			switch (field) {
			case 's':
				Varint.writeUnsigned(bb, subj);
				break;
			case 'p':
				Varint.writeUnsigned(bb, pred);
				break;
			case 'o':
				Varint.writeUnsigned(bb, obj);
				break;
			case 'c':
				Varint.writeUnsigned(bb, context);
				break;
			}
		}
	}

	/**
	 * Encodes a quad as a byte array key in index order.
	 *
	 * @param subj    subject ID
	 * @param pred    predicate ID
	 * @param obj     object ID
	 * @param context context ID
	 * @return encoded byte array key
	 */
	public byte[] toKeyBytes(long subj, long pred, long obj, long context) {
		int length = Varint.calcListLengthUnsigned(
				getValueForField(fieldSeq[0], subj, pred, obj, context),
				getValueForField(fieldSeq[1], subj, pred, obj, context),
				getValueForField(fieldSeq[2], subj, pred, obj, context),
				getValueForField(fieldSeq[3], subj, pred, obj, context));
		ByteBuffer bb = ByteBuffer.allocate(length);
		toKey(bb, subj, pred, obj, context);
		return bb.array();
	}

	/**
	 * Reads a key back to quad values in SPOC order.
	 *
	 * @param key  buffer positioned at the start of the key
	 * @param quad array of length 4 to receive [subj, pred, obj, context]
	 */
	public void keyToQuad(ByteBuffer key, long[] quad) {
		Varint.readQuadUnsigned(key, indexMap, quad);
	}

	/**
	 * Reads a key from a byte array back to quad values in SPOC order.
	 *
	 * @param key  byte array containing the encoded key
	 * @param quad array of length 4 to receive [subj, pred, obj, context]
	 */
	public void keyToQuad(byte[] key, long[] quad) {
		ByteBuffer bb = ByteBuffer.wrap(key);
		Varint.readQuadUnsigned(bb, indexMap, quad);
	}

	/**
	 * Constructs the minimum key for a range scan. Unbound components (-1 or 0) become 0.
	 *
	 * @param bb      buffer for writing bytes
	 * @param subj    subject ID, or -1 for wildcard
	 * @param pred    predicate ID, or -1 for wildcard
	 * @param obj     object ID, or -1 for wildcard
	 * @param context context ID, or -1 for wildcard
	 */
	public void getMinKey(ByteBuffer bb, long subj, long pred, long obj, long context) {
		toKey(bb,
				subj <= 0 ? 0 : subj,
				pred <= 0 ? 0 : pred,
				obj <= 0 ? 0 : obj,
				context <= 0 ? 0 : context);
	}

	/**
	 * Constructs the minimum key as a byte array for a range scan.
	 */
	public byte[] getMinKeyBytes(long subj, long pred, long obj, long context) {
		return toKeyBytes(
				subj <= 0 ? 0 : subj,
				pred <= 0 ? 0 : pred,
				obj <= 0 ? 0 : obj,
				context <= 0 ? 0 : context);
	}

	/**
	 * Constructs the maximum key for a range scan. Unbound components become Long.MAX_VALUE.
	 *
	 * @param bb      buffer for writing bytes
	 * @param subj    subject ID, or -1 for wildcard
	 * @param pred    predicate ID, or -1 for wildcard
	 * @param obj     object ID, or -1 for wildcard
	 * @param context context ID, or -1 for wildcard
	 */
	public void getMaxKey(ByteBuffer bb, long subj, long pred, long obj, long context) {
		toKey(bb,
				subj <= 0 ? Long.MAX_VALUE : subj,
				pred <= 0 ? Long.MAX_VALUE : pred,
				obj <= 0 ? Long.MAX_VALUE : obj,
				context <= 0 ? Long.MAX_VALUE : context);
	}

	/**
	 * Constructs the maximum key as a byte array for a range scan.
	 */
	public byte[] getMaxKeyBytes(long subj, long pred, long obj, long context) {
		return toKeyBytes(
				subj <= 0 ? Long.MAX_VALUE : subj,
				pred <= 0 ? Long.MAX_VALUE : pred,
				obj <= 0 ? Long.MAX_VALUE : obj,
				context <= 0 ? Long.MAX_VALUE : context);
	}

	/**
	 * Finds the best index from the given list for a query pattern by choosing the index with the highest pattern
	 * score.
	 *
	 * @param indexes list of available indexes
	 * @param subj    subject ID, or -1 for wildcard
	 * @param pred    predicate ID, or -1 for wildcard
	 * @param obj     object ID, or -1 for wildcard
	 * @param context context ID, or -1 for wildcard
	 * @return the best matching index
	 */
	public static QuadIndex getBestIndex(List<QuadIndex> indexes, long subj, long pred, long obj, long context) {
		int bestScore = -1;
		QuadIndex bestIndex = null;

		for (QuadIndex index : indexes) {
			int score = index.getPatternScore(subj, pred, obj, context);
			if (score > bestScore) {
				bestScore = score;
				bestIndex = index;
			}
		}

		return bestIndex;
	}

	/**
	 * Parses a comma/whitespace-separated list of index specifications. Each spec must consist of 4 characters: 's',
	 * 'p', 'o' and 'c'.
	 *
	 * @param indexSpecStr a string like "spoc, posc, cosp"
	 * @return a set of parsed index specifications
	 * @throws IllegalArgumentException if any spec is invalid
	 */
	public static Set<String> parseIndexSpecList(String indexSpecStr) {
		Set<String> indexes = new HashSet<>();

		if (indexSpecStr != null) {
			StringTokenizer tok = new StringTokenizer(indexSpecStr, ", \t");
			while (tok.hasMoreTokens()) {
				String index = tok.nextToken().toLowerCase();

				if (index.length() != 4 || index.indexOf('s') == -1 || index.indexOf('p') == -1
						|| index.indexOf('o') == -1 || index.indexOf('c') == -1) {
					throw new IllegalArgumentException(
							"Invalid value '" + index + "' in index specification: " + indexSpecStr);
				}

				indexes.add(index);
			}
		}

		return indexes;
	}

	/**
	 * Tests whether a decoded quad matches the given pattern. Unbound components (< 0) are treated as wildcards.
	 *
	 * @param quad a long[4] array in SPOC order
	 * @param s    subject pattern, or -1 for wildcard
	 * @param p    predicate pattern, or -1 for wildcard
	 * @param o    object pattern, or -1 for wildcard
	 * @param c    context pattern, or -1 for wildcard
	 * @return true if all bound components match
	 */
	public static boolean matches(long[] quad, long s, long p, long o, long c) {
		return (s < 0 || quad[SUBJ_IDX] == s)
				&& (p < 0 || quad[PRED_IDX] == p)
				&& (o < 0 || quad[OBJ_IDX] == o)
				&& (c < 0 || quad[CONTEXT_IDX] == c);
	}

	/**
	 * Maps a field character ('s', 'p', 'o', 'c') to the corresponding array index (0-3).
	 */
	public static int fieldCharToIdx(char c) {
		switch (c) {
		case 's':
			return SUBJ_IDX;
		case 'p':
			return PRED_IDX;
		case 'o':
			return OBJ_IDX;
		case 'c':
			return CONTEXT_IDX;
		default:
			throw new IllegalArgumentException("Invalid field: " + c);
		}
	}

	@Override
	public String toString() {
		return fieldSeqString;
	}

	private static int[] buildIndexMap(char[] fieldSeq) {
		int[] indexes = new int[fieldSeq.length];
		for (int i = 0; i < fieldSeq.length; i++) {
			switch (fieldSeq[i]) {
			case 's':
				indexes[i] = SUBJ_IDX;
				break;
			case 'p':
				indexes[i] = PRED_IDX;
				break;
			case 'o':
				indexes[i] = OBJ_IDX;
				break;
			case 'c':
				indexes[i] = CONTEXT_IDX;
				break;
			default:
				throw new IllegalArgumentException(
						"Invalid character '" + fieldSeq[i] + "' in field sequence: " + new String(fieldSeq));
			}
		}
		return indexes;
	}

	private static long getValueForField(char field, long subj, long pred, long obj, long context) {
		switch (field) {
		case 's':
			return subj;
		case 'p':
			return pred;
		case 'o':
			return obj;
		case 'c':
			return context;
		default:
			throw new IllegalArgumentException("Invalid field: " + field);
		}
	}
}
