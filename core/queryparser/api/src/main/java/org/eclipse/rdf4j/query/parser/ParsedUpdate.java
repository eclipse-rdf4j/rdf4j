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
package org.eclipse.rdf4j.query.parser;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.algebra.UpdateExpr;

/**
 * A parsed update sequence formulated in the OpenRDF query algebra.
 *
 * @author Jeen Broekstra
 */
public class ParsedUpdate extends ParsedOperation {

	/*-----------*
	 * Variables *
	 *-----------*/

	private final Map<String, String> namespaces;

	private final List<UpdateExpr> updateExprs = new ArrayList<>();

	private final Map<UpdateExpr, Dataset> datasetMapping = new IdentityHashMap<>();

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new update sequence. To complete this update sequence, one or more update expressions need to be
	 * supplied to it using {@link #addUpdateExpr(UpdateExpr)}.
	 */
	public ParsedUpdate() {
		super();
		this.namespaces = Map.of();
	}

	public ParsedUpdate(String sourceString) {
		super(sourceString);
		this.namespaces = Map.of();
	}

	public ParsedUpdate(String sourceString, Map<String, String> namespaces) {
		super(sourceString);
		this.namespaces = Objects.requireNonNull(namespaces);
	}

	/**
	 * Creates a new update sequence. To complete this update sequence, one or update expressions need to be supplied to
	 * it using {@link #addUpdateExpr(UpdateExpr)}.
	 *
	 * @param namespaces A mapping of namespace prefixes to namespace names representing the namespaces that are used in
	 *                   the update.
	 */
	public ParsedUpdate(Map<String, String> namespaces) {
		super();
		this.namespaces = Objects.requireNonNull(namespaces);
	}

	/*---------*
	 * Methods *
	 *---------*/

	public Map<String, String> getNamespaces() {
		return namespaces;
	}

	public void addUpdateExpr(UpdateExpr updateExpr) {
		updateExprs.add(updateExpr);
	}

	public List<UpdateExpr> getUpdateExprs() {
		return updateExprs;
	}

	/**
	 * @param updateExpr The updateExpr to map to a dataset.
	 * @param dataset    the dataset that applies to the updateExpr. May be null.
	 */
	public void map(UpdateExpr updateExpr, Dataset dataset) {
		datasetMapping.put(updateExpr, dataset);
	}

	/**
	 * @return Returns the map of update expressions and associated datasets.
	 */
	public Map<UpdateExpr, Dataset> getDatasetMapping() {
		return datasetMapping;
	}

	@Override
	public String toString() {
		StringBuilder stringBuilder = new StringBuilder();
		// TODO visualize dataset in toString()?
		for (UpdateExpr updateExpr : updateExprs) {
			stringBuilder.append(updateExpr.toString());
			stringBuilder.append("; ");
		}
		return stringBuilder.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		ParsedUpdate that = (ParsedUpdate) o;

		if (!namespaces.equals(that.namespaces)) {
			return false;
		}
		if (!updateExprs.equals(that.updateExprs)) {
			return false;
		}
		return datasetMapping.equals(that.datasetMapping);
	}

	@Override
	public int hashCode() {
		assert updateExprs.stream().noneMatch(expr -> expr.hashCode() == System.identityHashCode(expr));

		int result = namespaces.hashCode();
		result = 31 * result + updateExprs.hashCode();
		result = 31 * result + datasetMapping.hashCode();
		return result;
	}
}
