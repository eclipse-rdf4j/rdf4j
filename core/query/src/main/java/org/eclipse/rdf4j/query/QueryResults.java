/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.datatype.XMLGregorianCalendar;

import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.common.iteration.AbstractCloseableIteration;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.DistinctIteration;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.common.iteration.LimitIteration;
import org.eclipse.rdf4j.common.iteration.OffsetIteration;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.ModelFactory;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.datatypes.XMLDatatypeUtil;
import org.eclipse.rdf4j.model.impl.DynamicModelFactory;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.query.impl.BackgroundGraphResult;
import org.eclipse.rdf4j.query.impl.IteratingGraphQueryResult;
import org.eclipse.rdf4j.query.impl.IteratingTupleQueryResult;
import org.eclipse.rdf4j.query.impl.QueueCursor;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.UnsupportedRDFormatException;

/**
 * Utility methods related to query results.
 *
 * @author Jeen Broekstra
 */
public class QueryResults extends Iterations {

	/**
	 * Get a {@link Model} containing all elements obtained from the specified query result.
	 *
	 * @param iteration the source iteration to get the statements from.
	 * @return a {@link Model} containing all statements obtained from the specified source iteration.
	 */
	public static Model asModel(CloseableIteration<? extends Statement, ? extends RDF4JException> iteration)
			throws QueryEvaluationException {
		return asModel(iteration, new DynamicModelFactory());
	}

	/**
	 * Get a {@link Model} containing all elements obtained from the specified query result.
	 *
	 * @param iteration    the source iteration to get the statements from.
	 * @param modelFactory the ModelFactory used to instantiate the model that gets returned.
	 * @return a {@link Model} containing all statements obtained from the specified source iteration.
	 */
	public static Model asModel(CloseableIteration<? extends Statement, ? extends RDF4JException> iteration,
			ModelFactory modelFactory)
			throws QueryEvaluationException {
		Model model = modelFactory.createEmptyModel();
		addAll(iteration, model);
		return model;
	}

	/**
	 * Returns a list of values of a particular variable out of the QueryResult.
	 *
	 * @param result
	 * @param var    variable for which list of values needs to be returned
	 * @return a list of Values of var
	 * @throws QueryEvaluationException
	 */
	public static List<Value> getAllValues(TupleQueryResult result, String var) throws QueryEvaluationException {
		try (Stream<BindingSet> stream = result.stream()) {
			return result.getBindingNames().contains(var)
					? stream.map(bs -> bs.getValue(var)).collect(Collectors.toList())
					: Collections.emptyList();
		}
	}

	/**
	 * Returns a single element from the query result.The QueryResult is automatically closed by this method.
	 *
	 * @param result
	 * @return a single query result element or null
	 * @throws QueryEvaluationException
	 */
	public static Statement singleResult(GraphQueryResult result) throws QueryEvaluationException {
		try (Stream<Statement> stream = result.stream()) {
			return stream.findFirst().orElse(null);
		}
	}

	/**
	 * Returns a single element from the query result.The QueryResult is automatically closed by this method.
	 *
	 * @param result
	 * @return a single query result element or null
	 * @throws QueryEvaluationException
	 */
	public static BindingSet singleResult(TupleQueryResult result) throws QueryEvaluationException {
		try (Stream<BindingSet> stream = result.stream()) {
			return stream.findFirst().orElse(null);
		}
	}

	/**
	 * Returns a {@link GraphQueryResult} that filters out any duplicate solutions from the supplied queryResult.
	 *
	 * @param queryResult a queryResult containing possible duplicate statements.
	 * @return a {@link GraphQueryResult} with any duplicates filtered out.
	 */
	public static GraphQueryResult distinctResults(GraphQueryResult queryResult) {
		return new GraphQueryResultFilter(queryResult);
	}

	/**
	 * Returns a {@link TupleQueryResult} that filters out any duplicate solutions from the supplied queryResult.
	 *
	 * @param queryResult a queryResult containing possible duplicate solutions.
	 * @return a {@link TupleQueryResult} with any duplicates filtered out.
	 */
	public static TupleQueryResult distinctResults(TupleQueryResult queryResult) {
		return new TupleQueryResultFilter(queryResult);
	}

	/**
	 * Returns a {@link TupleQueryResult} that returns at most the specified maximum number of solutions, starting at
	 * the supplied offset.
	 *
	 * @param queryResult a query result possibly containing more solutions than the specified maximum.
	 * @param limit       the maximum number of solutions to return. If set to 0 or lower, no limit will be applied.
	 * @param offset      the number of solutions to skip at the beginning. If set to 0 or lower, no offset will be
	 *                    applied.
	 * @return A {@link TupleQueryResult} that will at return at most the specified maximum number of solutions. If
	 *         neither {@code limit} nor {@code offset} are applied, this returns the original {@code queryResult}.
	 */
	public static TupleQueryResult limitResults(TupleQueryResult queryResult, long limit, long offset) {
		CloseableIteration<BindingSet, QueryEvaluationException> iter = queryResult;
		if (offset > 0) {
			iter = new OffsetIteration<>(iter, offset);
		}
		if (limit > 0) {
			iter = new LimitIteration<>(iter, limit);
		}

		if (!(iter instanceof TupleQueryResult)) {
			return new IteratingTupleQueryResult(queryResult.getBindingNames(), iter);
		}
		return (TupleQueryResult) iter;
	}

	/**
	 * Returns a {@link GraphQueryResult} that returns at most the specified maximum number of solutions, starting at
	 * the supplied offset.
	 *
	 * @param queryResult a query result possibly containing more solutions than the specified maximum.
	 * @param limit       the maximum number of solutions to return. If set to 0 or lower, no limit will be applied.
	 * @param offset      the number of solutions to skip at the beginning. If set to 0 or lower, no offset will be
	 *                    applied.
	 * @return A {@link GraphQueryResult} that will at return at most the specified maximum number of solutions. If
	 *         neither {@code limit} nor {@code offset} are applied, this returns the original {@code queryResult}.
	 */
	public static GraphQueryResult limitResults(GraphQueryResult queryResult, long limit, long offset) {
		CloseableIteration<Statement, QueryEvaluationException> iter = queryResult;
		if (offset > 0) {
			iter = new OffsetIteration<>(iter, offset);
		}
		if (limit > 0) {
			iter = new LimitIteration<>(iter, limit);
		}

		if (!(iter instanceof GraphQueryResult)) {
			return new IteratingGraphQueryResult(queryResult.getNamespaces(), iter);
		}
		return (GraphQueryResult) iter;
	}

	/**
	 * Parses an RDF document and returns it as a GraphQueryResult object, with parsing done on a separate thread in the
	 * background.<br>
	 * IMPORTANT: As this method will spawn a new thread in the background, it is vitally important that the resulting
	 * GraphQueryResult be closed consistently when it is no longer required, to prevent resource leaks.
	 *
	 * @param in      The {@link InputStream} containing the RDF document.
	 * @param baseURI The base URI for the RDF document.
	 * @param format  The {@link RDFFormat} of the RDF document.
	 * @return A {@link GraphQueryResult} that parses in the background, and must be closed to prevent resource leaks.
	 */
	public static GraphQueryResult parseGraphBackground(InputStream in, String baseURI, RDFFormat format,
			WeakReference<?> callerReference)
			throws UnsupportedRDFormatException {
		return parseGraphBackground(in, baseURI, Rio.createParser(format), callerReference);
	}

	/**
	 * Parses an RDF document and returns it as a GraphQueryResult object, with parsing done on a separate thread in the
	 * background.<br>
	 * IMPORTANT: As this method will spawn a new thread in the background, it is vitally important that the resulting
	 * GraphQueryResult be closed consistently when it is no longer required, to prevent resource leaks.
	 *
	 * @param in      The {@link InputStream} containing the RDF document.
	 * @param baseURI The base URI for the RDF document.
	 * @param parser  The {@link RDFParser}.
	 * @return A {@link GraphQueryResult} that parses in the background, and must be closed to prevent resource leaks.
	 */
	public static GraphQueryResult parseGraphBackground(InputStream in, String baseURI, RDFParser parser,
			WeakReference<?> callerReference) {
		RDFFormat format = parser.getRDFFormat();
		BackgroundGraphResult result = new BackgroundGraphResult(
				new QueueCursor<>(new LinkedBlockingQueue<>(1), callerReference),
				parser, in, format.getCharset(), baseURI);
		boolean allGood = false;
		try {
			ForkJoinPool.commonPool().submit(result);
			allGood = true;
		} finally {
			if (!allGood) {
				result.close();
			}
		}
		return result;
	}

	/**
	 * Reports a tuple query result to a {@link TupleQueryResultHandler}. <br>
	 * The {@link TupleQueryResult#close()} method will always be called before this method returns. <br>
	 * If there is an exception generated by the TupleQueryResult, {@link QueryResultHandler#endQueryResult()} will not
	 * be called.
	 *
	 * @param tqr     The query result to report.
	 * @param handler The handler to report the query result to.
	 * @throws TupleQueryResultHandlerException If such an exception is thrown by the used query result writer.
	 */
	public static void report(TupleQueryResult tqr, QueryResultHandler handler)
			throws TupleQueryResultHandlerException, QueryEvaluationException {

		try (tqr) {
			handler.startQueryResult(tqr.getBindingNames());

			while (tqr.hasNext()) {
				BindingSet bindingSet = tqr.next();
				handler.handleSolution(bindingSet);
			}
		}
		handler.endQueryResult();
	}

	/**
	 * Reports a graph query result to an {@link RDFHandler}. <br>
	 * The {@link GraphQueryResult#close()} method will always be called before this method returns.<br>
	 * If there is an exception generated by the GraphQueryResult, {@link RDFHandler#endRDF()} will not be called.
	 *
	 * @param graphQueryResult The query result to report.
	 * @param rdfHandler       The handler to report the query result to.
	 * @throws RDFHandlerException      If such an exception is thrown by the used RDF writer.
	 * @throws QueryEvaluationException
	 */
	public static void report(GraphQueryResult graphQueryResult, RDFHandler rdfHandler)
			throws RDFHandlerException, QueryEvaluationException {
		try (graphQueryResult) {
			rdfHandler.startRDF();

			for (Map.Entry<String, String> entry : graphQueryResult.getNamespaces().entrySet()) {
				String prefix = entry.getKey();
				String namespace = entry.getValue();
				rdfHandler.handleNamespace(prefix, namespace);
			}

			while (graphQueryResult.hasNext()) {
				Statement st = graphQueryResult.next();
				rdfHandler.handleStatement(st);
			}
		}
		rdfHandler.endRDF();
	}

	/**
	 * Compares two tuple query results and returns {@code true} if they are equal.Tuple query results are equal if they
	 * contain the same set of {@link BindingSet}s and have the same headers. Blank nodes identifiers are not relevant
	 * for equality, they are matched by trying to find compatible mappings between BindingSets. Note that the method
	 * consumes both query results fully.
	 *
	 * @param tqr1 the first {@link TupleQueryResult} to compare.
	 * @param tqr2 the second {@link TupleQueryResult} to compare.
	 * @return true if equal
	 * @throws QueryEvaluationException
	 */
	public static boolean equals(TupleQueryResult tqr1, TupleQueryResult tqr2) throws QueryEvaluationException {
		List<BindingSet> list1 = Iterations.asList(tqr1);
		List<BindingSet> list2 = Iterations.asList(tqr2);

		// Compare the number of statements in both sets
		if (list1.size() != list2.size()) {
			return false;
		}

		return matchBindingSets(list1, list2);
	}

	public static boolean isSubset(TupleQueryResult tqr1, TupleQueryResult tqr2) throws QueryEvaluationException {
		List<BindingSet> list1 = Iterations.asList(tqr1);
		List<BindingSet> list2 = Iterations.asList(tqr2);

		// Compare the number of statements in both sets
		if (list1.size() > list2.size()) {
			return false;
		}

		return matchBindingSets(list1, list2);
	}

	/**
	 * Compares two graph query results and returns {@code true} if they are equal. Two graph query results are
	 * considered equal if they are isomorphic graphs. Note that the method consumes both query results fully.
	 *
	 * @param result1 the first query result to compare
	 * @param result2 the second query result to compare.
	 * @return {@code true} if the supplied graph query results are isomorphic graphs, {@code false} otherwise.
	 * @throws QueryEvaluationException
	 * @see Models#isomorphic(Iterable, Iterable)
	 */
	public static boolean equals(GraphQueryResult result1, GraphQueryResult result2) throws QueryEvaluationException {
		Set<? extends Statement> graph1 = Iterations.asSet(result1);
		Set<? extends Statement> graph2 = Iterations.asSet(result2);

		return Models.isomorphic(graph1, graph2);
	}

	private static boolean matchBindingSets(List<? extends BindingSet> queryResult1,
			Iterable<? extends BindingSet> queryResult2) {
		return matchBindingSets(queryResult1, queryResult2, new HashMap<>(), 0);
	}

	/**
	 * A recursive method for finding a complete mapping between blank nodes in queryResult1 and blank nodes in
	 * queryResult2. The algorithm does a depth-first search trying to establish a mapping for each blank node occurring
	 * in queryResult1.
	 *
	 * @return true if a complete mapping has been found, false otherwise.
	 */
	private static boolean matchBindingSets(List<? extends BindingSet> queryResult1,
			Iterable<? extends BindingSet> queryResult2, Map<BNode, BNode> bNodeMapping, int idx) {

		if (idx < queryResult1.size()) {
			BindingSet bs1 = queryResult1.get(idx);

			List<BindingSet> matchingBindingSets = findMatchingBindingSets(bs1, queryResult2, bNodeMapping);

			for (BindingSet bs2 : matchingBindingSets) {
				// Map bNodes in bs1 to bNodes in bs2
				Map<BNode, BNode> newBNodeMapping = new HashMap<>(bNodeMapping);

				for (Binding binding : bs1) {
					if (binding.getValue() instanceof BNode) {
						newBNodeMapping.put((BNode) binding.getValue(), (BNode) bs2.getValue(binding.getName()));
					}
				}

				// FIXME: this recursive implementation has a high risk of
				// triggering a stack overflow

				// Enter recursion
				if (matchBindingSets(queryResult1, queryResult2, newBNodeMapping, idx + 1)) {
					// models match, look no further
					return true;
				}
			}
		} else {
			// All statements have been mapped successfully
			return true;
		}

		return false;

	}

	private static List<BindingSet> findMatchingBindingSets(BindingSet st, Iterable<? extends BindingSet> model,
			Map<BNode, BNode> bNodeMapping) {
		List<BindingSet> result = new ArrayList<>();

		for (BindingSet modelSt : model) {
			if (bindingSetsMatch(st, modelSt, bNodeMapping)) {
				// All components possibly match
				result.add(modelSt);
			}
		}

		return result;
	}

	private static boolean bindingSetsMatch(BindingSet bs1, BindingSet bs2, Map<BNode, BNode> bNodeMapping) {

		if (bs1.size() != bs2.size()) {
			return false;
		}

		for (Binding binding1 : bs1) {
			Value value1 = binding1.getValue();
			Value value2 = bs2.getValue(binding1.getName());

			if (value1 instanceof BNode && value2 instanceof BNode) {
				BNode mappedBNode = bNodeMapping.get(value1);

				if (mappedBNode != null) {
					// bNode 'value1' was already mapped to some other bNode
					if (!value2.equals(mappedBNode)) {
						// 'value1' and 'value2' do not match
						return false;
					}
				} else {
					// 'value1' was not yet mapped, we need to check if 'value2' is a
					// possible mapping candidate
					if (bNodeMapping.containsValue(value2)) {
						// 'value2' is already mapped to some other value.
						return false;
					}
				}
			} else {
				// values are not (both) bNodes
				if (value1 instanceof Literal && value2 instanceof Literal) {
					// do literal value-based comparison for supported datatypes
					Literal leftLit = (Literal) value1;
					Literal rightLit = (Literal) value2;

					IRI dt1 = leftLit.getDatatype();
					IRI dt2 = rightLit.getDatatype();

					if (dt1 != null && dt1.equals(dt2)
							&& XMLDatatypeUtil.isValidValue(leftLit.getLabel(), dt1)
							&& XMLDatatypeUtil.isValidValue(rightLit.getLabel(), dt2)) {
						Integer compareResult = null;
						if (dt1.equals(XSD.DOUBLE)) {
							compareResult = Double.compare(leftLit.doubleValue(), rightLit.doubleValue());
						} else if (dt1.equals(XSD.FLOAT)) {
							compareResult = Float.compare(leftLit.floatValue(), rightLit.floatValue());
						} else if (dt1.equals(XSD.DECIMAL)) {
							compareResult = leftLit.decimalValue().compareTo(rightLit.decimalValue());
						} else if (XMLDatatypeUtil.isIntegerDatatype(dt1)) {
							compareResult = leftLit.integerValue().compareTo(rightLit.integerValue());
						} else if (dt1.equals(XSD.BOOLEAN)) {
							Boolean leftBool = leftLit.booleanValue();
							Boolean rightBool = rightLit.booleanValue();
							compareResult = leftBool.compareTo(rightBool);
						} else if (XMLDatatypeUtil.isCalendarDatatype(dt1)) {
							XMLGregorianCalendar left = leftLit.calendarValue();
							XMLGregorianCalendar right = rightLit.calendarValue();

							compareResult = left.compare(right);
						}

						if (compareResult != null) {
							if (compareResult != 0) {
								return false;
							}
						} else if (!value1.equals(value2)) {
							return false;
						}
					} else if (!value1.equals(value2)) {
						return false;
					}
				} else if (!value1.equals(value2)) {
					return false;
				}
			}
		}

		return true;
	}

	/**
	 * Check whether two {@link BindingSet}s are compatible. Two binding sets are compatible if they have equal values
	 * for each variable that is bound in both binding sets.
	 *
	 * @param bs1
	 * @param bs2
	 * @return true if compatible
	 */
	public static boolean bindingSetsCompatible(BindingSet bs1, BindingSet bs2) {
		Set<String> bs1BindingNames = bs1.getBindingNames();
		if (bs1BindingNames.isEmpty())
			return true;

		Set<String> bs2BindingNames = bs2.getBindingNames();

		for (String bindingName : bs1BindingNames) {

			if (bs2BindingNames.contains(bindingName)) {
				Value value1 = bs1.getValue(bindingName);

				// if a variable is unbound in one set it is compatible
				if (value1 != null) {
					Value value2 = bs2.getValue(bindingName);

					// if a variable is unbound in one set it is compatible
					if (value2 != null && !value1.equals(value2)) {
						return false;
					}
				}

			}
		}

		return true;
	}

	private static class GraphQueryResultFilter extends AbstractCloseableIteration<Statement, QueryEvaluationException>
			implements GraphQueryResult {

		private final DistinctIteration<Statement, QueryEvaluationException> filter;

		private final GraphQueryResult unfiltered;

		public GraphQueryResultFilter(GraphQueryResult wrappedResult) {
			this.filter = new DistinctIteration<>(wrappedResult);
			this.unfiltered = wrappedResult;
		}

		@Override
		public boolean hasNext() throws QueryEvaluationException {
			if (isClosed()) {
				return false;
			}

			boolean result = filter.hasNext();
			if (!result) {
				close();
			}
			return result;
		}

		@Override
		public Statement next() throws QueryEvaluationException {
			if (isClosed()) {
				throw new NoSuchElementException("The iteration has been closed.");
			}

			try {
				return filter.next();
			} catch (NoSuchElementException e) {
				close();
				throw e;
			}
		}

		@Override
		public void remove() throws QueryEvaluationException {
			if (isClosed()) {
				throw new IllegalStateException("The iteration has been closed.");
			}

			try {
				filter.remove();
			} catch (IllegalStateException e) {
				close();
				throw e;
			}
		}

		@Override
		public void handleClose() throws QueryEvaluationException {
			try {
				super.handleClose();
			} finally {
				filter.close();
			}
		}

		@Override
		public Map<String, String> getNamespaces() throws QueryEvaluationException {
			return unfiltered.getNamespaces();
		}
	}

	private static class TupleQueryResultFilter extends AbstractCloseableIteration<BindingSet, QueryEvaluationException>
			implements TupleQueryResult {

		private final DistinctIteration<BindingSet, QueryEvaluationException> filter;

		private final TupleQueryResult unfiltered;

		public TupleQueryResultFilter(TupleQueryResult wrappedResult) {
			this.filter = new DistinctIteration<>(wrappedResult);
			this.unfiltered = wrappedResult;
		}

		@Override
		public boolean hasNext() throws QueryEvaluationException {
			if (isClosed()) {
				return false;
			}

			boolean result = filter.hasNext();
			if (!result) {
				close();
			}
			return result;
		}

		@Override
		public BindingSet next() throws QueryEvaluationException {
			if (isClosed()) {
				throw new NoSuchElementException("The iteration has been closed.");
			}

			try {
				return filter.next();
			} catch (NoSuchElementException e) {
				close();
				throw e;
			}
		}

		@Override
		public void remove() throws QueryEvaluationException {
			if (isClosed()) {
				throw new IllegalStateException("The iteration has been closed.");
			}

			try {
				filter.remove();
			} catch (IllegalStateException e) {
				close();
				throw e;
			}
		}

		@Override
		public void handleClose() throws QueryEvaluationException {
			try {
				super.handleClose();
			} finally {
				filter.close();
			}
		}

		@Override
		public List<String> getBindingNames() throws QueryEvaluationException {
			return unfiltered.getBindingNames();
		}

	}
}
