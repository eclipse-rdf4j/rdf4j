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

import static java.util.stream.Collectors.mapping;

import static org.eclipse.rdf4j.spring.dao.exception.mapper.ExceptionMapper.mapException;
import static org.eclipse.rdf4j.spring.dao.support.operation.OperationUtils.require;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.spring.dao.support.BindingSetMapper;
import org.eclipse.rdf4j.spring.dao.support.MappingPostProcessor;
import org.eclipse.rdf4j.spring.dao.support.TupleQueryResultMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 4.0.0
 * @author Florian Kleedorfer
 */
public class TupleQueryResultConverter {
	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private TupleQueryResult tupleQueryResult;

	public TupleQueryResultConverter(TupleQueryResult result) {
		Objects.requireNonNull(result);
		this.tupleQueryResult = result;
	}

	/**
	 * Passes the {@link TupleQueryResult} to the consumer and closes the result afterwards.
	 */
	public void consumeResult(Consumer<TupleQueryResult> consumer) {
		try {
			consumer.accept(tupleQueryResult);
		} catch (Exception e) {
			logger.debug("Caught execption while processing TupleQueryResult", e);
			throw mapException("Error processing TupleQueryResult", e);
		} finally {
			tupleQueryResult.close();
			tupleQueryResult = null;
		}
	}

	/**
	 * Applies the function to the {@link TupleQueryResult} and closes the result afterwards.
	 */
	public <T> T applyToResult(Function<TupleQueryResult, T> function) {
		try {
			return function.apply(tupleQueryResult);
		} catch (Exception e) {
			logger.warn("Caught execption while processing TupleQueryResult", e);
			throw mapException("Error processing TupleQueryResult", e);
		} finally {
			tupleQueryResult.close();
			tupleQueryResult = null;
		}
	}

	/**
	 * Obtains a stream of {@link BindingSet}s. The result is completely consumed and closed when the stream is
	 * returned.
	 */
	public Stream<BindingSet> toStream() {
		return applyToResult(r -> getBindingStream(r).collect(Collectors.toList()).stream());
	}

	private <T> Stream<T> toStreamInternal(Function<BindingSet, T> mapper) {
		return applyToResult(
				result -> getBindingStream(result)
						.map(mapper)
						.filter(Objects::nonNull)
						.collect(Collectors.toList())
						.stream());
	}

	/**
	 * Obtains a {@link Stream} of mapped query results. The result is completely consumed and closed when the stream is
	 * returned. Any null values are filterd from the resulting stream.
	 */
	public <T> Stream<T> toStream(BindingSetMapper<T> mapper) {
		return toStreamInternal(mapper);
	}

	/**
	 * Obtains a {@link Stream} of mapped query results, using the postprocessor to map it again. Any null values are
	 * filtered from the resulting stream.
	 */
	public <T, O> Stream<O> toStream(
			BindingSetMapper<T> mapper, MappingPostProcessor<T, O> postProcessor) {
		return toStreamInternal(andThenOrElseNull(mapper, postProcessor));
	}

	/** Maps the whole {@link TupleQueryResult} to one object, which may be null. */
	public <T> T toSingletonMaybeOfWholeResult(TupleQueryResultMapper<T> mapper) {
		return applyToResult(mapper);
	}

	/** Maps the whole {@link TupleQueryResult} to one {@link Optional}. */
	public <T> Optional<T> toSingletonOptionalOfWholeResult(TupleQueryResultMapper<T> mapper) {
		return Optional.ofNullable(toSingletonMaybeOfWholeResult(mapper));
	}

	/**
	 * Maps the whole {@link TupleQueryResult} to an object, throwing an exception if the mapper returns
	 * <code>null</code>.
	 *
	 * @throws org.eclipse.rdf4j.spring.dao.exception.IncorrectResultSetSizeException
	 */
	public <T> T toSingletonOfWholeResult(TupleQueryResultMapper<T> mapper) {
		return require(toSingletonOptionalOfWholeResult(mapper));
	}

	/**
	 * Maps the first {@link BindingSet} in the result if one exists, throwing an exception if there are more. Returns
	 * null if there are no results or if there is one result that is mapped to null by the specified mapper.
	 *
	 * @throws org.eclipse.rdf4j.spring.dao.exception.IncorrectResultSetSizeException
	 */
	public <T> T toSingletonMaybe(BindingSetMapper<T> mapper) {
		return mapAndCollect(mapper, OperationUtils.toSingletonMaybe());
	}

	/**
	 * Maps the first {@link BindingSet} in the result, throwing an exception if there are more than one. Returns an
	 * Optional, which is empty if there are no results or if there is one result that is mapped to null by the
	 * specified mapper.
	 */
	public <T> Optional<T> toSingletonOptional(BindingSetMapper<T> mapper) {
		return Optional.ofNullable(toSingletonMaybe(mapper));
	}

	/**
	 * Maps the first {@link BindingSet} in the result, throwing an exception if there are no results or more than one.
	 *
	 * @throws org.eclipse.rdf4j.spring.dao.exception.IncorrectResultSetSizeException
	 */
	public <T> T toSingleton(BindingSetMapper<T> mapper) {
		return require(toSingletonOptional(mapper));
	}

	/**
	 * Maps the first {@link BindingSet} in the result if one exists, throwing an exception if there are more.
	 *
	 * @throws org.eclipse.rdf4j.spring.dao.exception.IncorrectResultSetSizeException
	 */
	public <T, O> O toSingletonMaybe(
			BindingSetMapper<T> mapper, MappingPostProcessor<T, O> postProcessor) {
		return mapAndCollect(andThenOrElseNull(mapper, postProcessor), OperationUtils.toSingletonMaybe());
	}

	public <T, O> Optional<O> toSingletonOptional(
			BindingSetMapper<T> mapper, MappingPostProcessor<T, O> postProcessor) {
		return Optional.ofNullable(toSingletonMaybe(mapper, postProcessor));
	}

	/**
	 * Maps the first {@link BindingSet} in the result, throwing an exception if there are no results or more than one.
	 *
	 * @throws org.eclipse.rdf4j.spring.dao.exception.IncorrectResultSetSizeException
	 */
	public <T, O> O toSingleton(
			BindingSetMapper<T> mapper, MappingPostProcessor<T, O> postProcessor) {
		return require(toSingletonOptional(mapper, postProcessor));
	}

	public <T, A, R> R mapAndCollect(Function<BindingSet, T> mapper, Collector<T, A, R> collector) {
		return applyToResult(
				result -> getBindingStream(result)
						.map(mapper)
						.filter(Objects::nonNull)
						.collect(collector));
	}

	/** Maps the query result to a {@link List}. */
	public <T> List<T> toList(BindingSetMapper<T> mapper) {
		return mapAndCollect(mapper, Collectors.toList());
	}

	/** Maps the query result to a {@link List}. */
	public <T, O> List<O> toList(
			BindingSetMapper<T> mapper, MappingPostProcessor<T, O> postProcessor) {
		return mapAndCollect(andThenOrElseNull(mapper, postProcessor), Collectors.toList());
	}

	/** Maps the query result to a {@link Set}. */
	public <T> Set<T> toSet(BindingSetMapper<T> mapper) {
		return mapAndCollect(mapper, Collectors.toSet());
	}

	/** Maps the query result to a {@link Set}. */
	public <T, O> Set<O> toSet(
			BindingSetMapper<T> mapper, MappingPostProcessor<T, O> postProcessor) {
		return mapAndCollect(andThenOrElseNull(mapper, postProcessor), Collectors.toSet());
	}

	/**
	 * Maps the query result to a {@link Map}, throwing an Exception if there are multiple values for one key.
	 */
	public <K, V> Map<K, V> toMap(
			Function<BindingSet, K> keyMapper, Function<BindingSet, V> valueMapper) {
		return mapAndCollect(Function.identity(), Collectors.toMap(keyMapper, valueMapper));
	}

	/** Maps the query result to a {@link Map} of {@link Set}s. */
	public <K, V> Map<K, Set<V>> toMapOfSet(
			Function<BindingSet, K> keyMapper, Function<BindingSet, V> valueMapper) {
		return mapAndCollect(
				Function.identity(),
				Collectors.groupingBy(
						keyMapper, Collectors.mapping(valueMapper, Collectors.toSet())));
	}

	/** Maps the query result to a {@link Map} of {@link List}s. */
	public <K, V> Map<K, List<V>> toMapOfList(
			Function<BindingSet, K> keyMapper, Function<BindingSet, V> valueMapper) {
		return mapAndCollect(
				Function.identity(),
				Collectors.groupingBy(
						keyMapper, Collectors.mapping(valueMapper, Collectors.toList())));
	}

	/**
	 * Maps the query result to a {@link Map}, throwing an Exception if there are multiple values for one key.
	 */
	public <T, K, V> Map<K, V> toMap(
			BindingSetMapper<T> mapper, Function<T, K> keyMapper, Function<T, V> valueMapper) {
		return mapAndCollect(mapper, Collectors.toMap(keyMapper, valueMapper));
	}

	/**
	 * Maps the query result to a {@link Map}, throwing an Exception if there are multiple values for one key.
	 */
	public <K, V> Map<K, V> toMap(Function<BindingSet, Map.Entry<K, V>> entryMapper) {
		return mapAndCollect(
				Function.identity(),
				Collectors.toMap(
						bs -> entryMapper.apply(bs).getKey(),
						bs -> entryMapper.apply(bs).getValue()));
	}

	/** Maps the query result to a {@link Map} of {@link Set}s. */
	public <T, K, V> Map<K, Set<V>> toMapOfSet(
			BindingSetMapper<T> mapper, Function<T, K> keyMapper, Function<T, V> valueMapper) {
		return mapAndCollect(
				mapper, Collectors.groupingBy(keyMapper, mapping(valueMapper, Collectors.toSet())));
	}

	/** Maps the query result to a {@link Map} of {@link List}s. */
	public <T, K, V> Map<K, List<V>> toMapOfList(
			BindingSetMapper<T> mapper, Function<T, K> keyMapper, Function<T, V> valueMapper) {
		return mapAndCollect(
				mapper,
				Collectors.groupingBy(keyMapper, mapping(valueMapper, Collectors.toList())));
	}

	/**
	 * If the result has only one empty binding set, this method returns an empty stream, otherwise the stream of
	 * BindingSets
	 */
	public Stream<BindingSet> getBindingStream(TupleQueryResult result) {
		if (!result.hasNext()) {
			return Stream.empty();
		}
		BindingSet first = result.next();
		if (!result.hasNext() && first.size() == 0) {
			return Stream.empty();
		}
		return Stream.concat(Stream.of(first), result.stream());
	}

	/**
	 * Executes <code>mapper.andThen(postProcessor)</code> unless the result of <code>mapper</code> is null, in which
	 * case the result is <code>null</code>.
	 */
	private <T, O> Function<BindingSet, O> andThenOrElseNull(
			BindingSetMapper<T> mapper, MappingPostProcessor<T, O> postProcessor) {
		return bindingSet -> Optional.ofNullable(mapper.apply(bindingSet))
				.map(postProcessor)
				.orElse(null);
	}

}
