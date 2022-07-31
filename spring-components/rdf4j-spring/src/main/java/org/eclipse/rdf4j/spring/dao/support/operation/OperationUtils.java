/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.spring.dao.support.operation;

import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.Operation;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf;
import org.eclipse.rdf4j.spring.dao.exception.IncorrectResultSetSizeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 4.0.0
 * @author Florian Kleedorfer
 */
public class OperationUtils {
	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	/**
	 * Returns the object in the {@link Collection} wrapped in an {@link Optional}, an empty Optional if the collection
	 * is empty, throwing an Exception if the Collection contains more than one element.
	 */
	public static <T> Optional<T> atMostOne(Collection<T> objects) {
		int size = objects.size();
		if (size > 1) {
			throw new IncorrectResultSetSizeException(
					"Expected to find at most one entity, but found " + size, 1, size);
		}
		return objects.stream().findFirst();
	}

	/**
	 * Returns the object contained in the specified {@link Optional}, throwing an Exception if it is empty.
	 */
	public static <T> T require(Optional<T> required) {
		if (required.isEmpty()) {
			throw new IncorrectResultSetSizeException(
					"Expected to find exactly one entity but found 0", 1, 0);
		}
		return required.get();
	}

	/**
	 * Returns the element in the {@link Collection}, throwing an exception if the collection is empty or contains more
	 * than one element.
	 */
	public static <T> T exactlyOne(Collection<T> objects) {
		return require(atMostOne(objects));
	}

	/**
	 * Returns the element in the {@link java.util.stream.Stream}, throwing an exception if the stream is empty or
	 * contains more than one element.
	 */
	public static <T> Collector<T, ?, T> toSingleton() {
		return Collectors.collectingAndThen(
				Collectors.toList(),
				list -> {
					int size = list.size();
					if (size != 1) {
						throw new IncorrectResultSetSizeException(
								"Expected exactly one result, found " + size, 1, size);
					}
					return list.get(0);
				});
	}

	/**
	 * Returns the element in the {@link java.util.stream.Stream}, or null if the stream is empty, throwing an exception
	 * if the stream contains more than one element.
	 */
	public static <T> Collector<T, ?, T> toSingletonMaybe() {
		return Collectors.collectingAndThen(
				Collectors.toList(),
				list -> {
					int size = list.size();
					if (size > 1) {
						throw new IncorrectResultSetSizeException(
								"Expected zero or one result, found " + size, 1, size);
					} else if (size == 0) {
						return null;
					}
					return list.get(0);
				});
	}

	/**
	 * Returns the element in the {@link java.util.stream.Stream} wrapped in an {@link Optional} throwing an exception
	 * if the stream contains more than one element.
	 */
	public static <T> Collector<T, ?, Optional<T>> toSingletonOptional() {
		return Collectors.collectingAndThen(
				Collectors.toList(),
				list -> {
					int size = list.size();
					if (size > 1) {
						throw new IncorrectResultSetSizeException(
								"Expected zero or one result, found " + size, 1, 0);
					} else if (size == 0) {
						return Optional.empty();
					}
					return Optional.ofNullable(list.get(0));
				});
	}

	public static void setBindings(Operation operation, Map<String, Value> bindings) {
		debugLogBindings(bindings);
		operation.clearBindings();
		if (bindings != null) {
			bindings.entrySet()
					.stream()
					.forEach(entry -> operation.setBinding(entry.getKey(), entry.getValue()));
		}
	}

	private static void debugLogBindings(Map<String, Value> bindings) {
		if (logger.isDebugEnabled() && bindings != null) {
			logger.debug("bindings: {}", bindings);
			List<String> keys = bindings.keySet().stream().collect(Collectors.toList());
			logger.debug(
					"values block:\n\nVALUES ( {} ) { ( {} ) }\n",
					keys.stream().map(k -> "?" + k).collect(Collectors.joining(" ")),
					keys.stream()
							.map(k -> Rdf.object(bindings.get(k)).getQueryString())
							.collect(Collectors.joining(" ")));
		}
	}
}
