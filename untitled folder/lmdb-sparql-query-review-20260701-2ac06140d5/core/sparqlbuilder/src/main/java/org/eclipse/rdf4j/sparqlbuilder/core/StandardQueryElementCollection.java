/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.sparqlbuilder.core;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;

/**
 * A {@link QueryElementCollection} that follows a more standard way of converting to a query string
 *
 * @param <T> the type of {@link QueryElement}s in the collection
 */
public abstract class StandardQueryElementCollection<T extends QueryElement> extends QueryElementCollection<T> {
	private Optional<String> operatorName = Optional.empty();
	private Function<String, String> wrapperMethod = Function.identity();

	private boolean printBodyIfEmpty = false;
	private boolean printNameIfEmpty = true;

	protected StandardQueryElementCollection() {
	}

	protected StandardQueryElementCollection(String delimeter) {
		super(delimeter);
	}

	protected StandardQueryElementCollection(String operatorName, String delimeter) {
		super(delimeter);
		setOperatorName(operatorName);
	}

	protected StandardQueryElementCollection(String delimeter, Collection<T> collection) {
		super(delimeter, collection);
	}

	protected StandardQueryElementCollection(String operatorName, Function<String, String> wrapperMethod) {
		super();
		setOperatorName(operatorName);
		setWrapperMethod(wrapperMethod);
	}

	protected StandardQueryElementCollection(String operatorName, String delimiter, Collection<T> collection) {
		super(delimiter, collection);
		setOperatorName(operatorName);
	}

	protected StandardQueryElementCollection(String operatorName, String delimiter,
			Function<String, String> wrapperMethod, Collection<T> collection) {
		super(delimiter, collection);
		setOperatorName(operatorName);
		setWrapperMethod(wrapperMethod);
	}

	protected void setOperatorName(String operatorName) {
		setOperatorName(operatorName, true);
	}

	protected void setOperatorName(String operatorName, boolean pad) {
		this.operatorName = Optional.of(operatorName + (pad ? " " : ""));
	}

	protected void setWrapperMethod(Function<String, String> wrapperMethod) {
		this.wrapperMethod = wrapperMethod;
	}

	protected void resetWrapperMethod() {
		this.wrapperMethod = Function.identity();
	}

	protected void printBodyIfEmpty(boolean printBodyIfEmpty) {
		this.printBodyIfEmpty = printBodyIfEmpty;
	}

	protected void printNameIfEmpty(boolean printNameIfEmpty) {
		this.printNameIfEmpty = printNameIfEmpty;
	}

	@Override
	public String getQueryString() {
		StringBuilder queryString = new StringBuilder();

		if (printNameIfEmpty || !isEmpty()) {
			operatorName.ifPresent(queryString::append);
		}
		if (printBodyIfEmpty || !isEmpty()) {
			queryString.append(wrapperMethod.apply(super.getQueryString()));
		}

		return queryString.toString();
	}
}
