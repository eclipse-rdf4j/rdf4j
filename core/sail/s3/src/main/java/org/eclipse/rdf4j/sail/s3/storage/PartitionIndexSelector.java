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

/**
 * Selects the best sort order for queries within a predicate partition.
 *
 * <p>
 * Within a predicate partition the predicate component is implicit, so query optimization selects among
 * three-dimensional sort orders over the remaining components: subject, object, and context.
 *
 * <ul>
 * <li><b>soc</b> - sorted by (subject, object, context)</li>
 * <li><b>osc</b> - sorted by (object, subject, context)</li>
 * <li><b>cso</b> - sorted by (context, subject, object)</li>
 * </ul>
 *
 * <p>
 * The selection strategy counts leading bound components for each sort order and picks the order with the highest
 * score. On ties, {@code soc} is preferred as the default.
 */
public class PartitionIndexSelector {

	private PartitionIndexSelector() {
		// utility class
	}

	/**
	 * Selects the best sort order for a within-partition query.
	 *
	 * <p>
	 * Within a partition, predicate is fixed, so we pick from:
	 * <ul>
	 * <li><b>soc</b>: sort by (subject, object, context)</li>
	 * <li><b>osc</b>: sort by (object, subject, context)</li>
	 * <li><b>cso</b>: sort by (context, subject, object)</li>
	 * </ul>
	 *
	 * Each sort order is scored by counting its leading bound components. The order with the highest score wins. Ties
	 * are broken in favor of {@code soc}.
	 *
	 * @param subjectBound true if the subject is bound in the query
	 * @param objectBound  true if the object is bound in the query
	 * @param contextBound true if the context is bound in the query
	 * @return the sort order suffix string: "soc", "osc", or "cso"
	 */
	public static String selectSortOrder(boolean subjectBound, boolean objectBound, boolean contextBound) {
		// Score each sort order by counting leading bound components

		// soc: subject -> object -> context
		int socScore = 0;
		if (subjectBound) {
			socScore++;
			if (objectBound) {
				socScore++;
				if (contextBound) {
					socScore++;
				}
			}
		}

		// osc: object -> subject -> context
		int oscScore = 0;
		if (objectBound) {
			oscScore++;
			if (subjectBound) {
				oscScore++;
				if (contextBound) {
					oscScore++;
				}
			}
		}

		// cso: context -> subject -> object
		int csoScore = 0;
		if (contextBound) {
			csoScore++;
			if (subjectBound) {
				csoScore++;
				if (objectBound) {
					csoScore++;
				}
			}
		}

		// Pick highest score; ties prefer soc (default)
		if (oscScore > socScore && oscScore > csoScore) {
			return "osc";
		}
		if (csoScore > socScore && csoScore > oscScore) {
			return "cso";
		}
		return "soc";
	}

	/**
	 * Returns the column order for a given sort order suffix. Used when sorting entries before writing to Parquet.
	 *
	 * @param sortOrder the sort order suffix: "soc", "osc", or "cso"
	 * @return array of column names in sort priority order
	 */
	public static String[] getColumnOrder(String sortOrder) {
		switch (sortOrder) {
		case "soc":
			return new String[] { "subject", "object", "context" };
		case "osc":
			return new String[] { "object", "subject", "context" };
		case "cso":
			return new String[] { "context", "subject", "object" };
		default:
			return new String[] { "subject", "object", "context" };
		}
	}
}
