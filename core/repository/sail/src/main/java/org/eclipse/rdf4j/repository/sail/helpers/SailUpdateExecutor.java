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
package org.eclipse.rdf4j.repository.sail.helpers;

import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.ConvertingIteration;
import org.eclipse.rdf4j.common.iteration.TimeLimitIteration;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF4J;
import org.eclipse.rdf4j.model.vocabulary.SESAME;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryInterruptedException;
import org.eclipse.rdf4j.query.algebra.Add;
import org.eclipse.rdf4j.query.algebra.Clear;
import org.eclipse.rdf4j.query.algebra.Copy;
import org.eclipse.rdf4j.query.algebra.Create;
import org.eclipse.rdf4j.query.algebra.DeleteData;
import org.eclipse.rdf4j.query.algebra.InsertData;
import org.eclipse.rdf4j.query.algebra.Load;
import org.eclipse.rdf4j.query.algebra.Modify;
import org.eclipse.rdf4j.query.algebra.Move;
import org.eclipse.rdf4j.query.algebra.QueryRoot;
import org.eclipse.rdf4j.query.algebra.SingletonSet;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.StatementPattern.Scope;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.UpdateExpr;
import org.eclipse.rdf4j.query.algebra.ValueConstant;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.helpers.collectors.StatementPatternCollector;
import org.eclipse.rdf4j.query.impl.EmptyBindingSet;
import org.eclipse.rdf4j.query.impl.MapBindingSet;
import org.eclipse.rdf4j.query.parser.sparql.SPARQLUpdateDataBlockParser;
import org.eclipse.rdf4j.repository.sail.SailUpdate;
import org.eclipse.rdf4j.repository.util.RDFLoader;
import org.eclipse.rdf4j.rio.ParserConfig;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.helpers.BasicParserSettings;
import org.eclipse.rdf4j.rio.helpers.TimeLimitRDFHandler;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.UpdateContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link SailUpdate#execute()} using
 * {@link SailConnection#evaluate(TupleExpr, Dataset, BindingSet, boolean)} and other {@link SailConnection} methods.
 * LOAD is handled at the Repository API level because it requires access to the Rio parser.
 *
 * @author jeen
 * @author James Leigh
 * @see SailConnection#startUpdate(UpdateContext)
 * @see SailConnection#endUpdate(UpdateContext)
 * @see SailConnection#addStatement(UpdateContext, Resource, IRI, Value, Resource...)
 * @see SailConnection#removeStatement(UpdateContext, Resource, IRI, Value, Resource...)
 * @see SailConnection#clear(Resource...)
 * @see SailConnection#getContextIDs()
 * @see SailConnection#getStatements(Resource, IRI, Value, boolean, Resource...)
 * @see SailConnection#evaluate(TupleExpr, Dataset, BindingSet, boolean)
 */
public class SailUpdateExecutor {

	private final Logger logger = LoggerFactory.getLogger(SailUpdateExecutor.class);

	private final SailConnection con;

	private final ValueFactory vf;

	private final RDFLoader loader;

	/**
	 * Implementation of {@link SailUpdate#execute()} using
	 * {@link SailConnection#evaluate(TupleExpr, Dataset, BindingSet, boolean)} and other {@link SailConnection}
	 * methods.
	 *
	 * @param con        Used to read data from and write data to.
	 * @param vf         Used to create {@link BNode}s
	 * @param loadConfig
	 */
	public SailUpdateExecutor(SailConnection con, ValueFactory vf, ParserConfig loadConfig) {
		this.con = con;
		this.vf = vf;
		this.loader = new RDFLoader(loadConfig, vf);
	}

	/**
	 * @param maxExecutionTime in seconds.
	 */
	public void executeUpdate(UpdateExpr updateExpr, Dataset dataset, BindingSet bindings, boolean includeInferred,
			int maxExecutionTime) throws SailException, RDFParseException, IOException {
		UpdateContext uc = new UpdateContext(updateExpr, dataset, bindings, includeInferred);
		logger.trace("Incoming update expression:\n{}", uc);

		con.startUpdate(uc);
		try {
			if (updateExpr instanceof Load) {
				executeLoad((Load) updateExpr, uc);
			} else if (updateExpr instanceof Modify) {
				executeModify((Modify) updateExpr, uc, maxExecutionTime);
			} else if (updateExpr instanceof InsertData) {
				executeInsertData((InsertData) updateExpr, uc, maxExecutionTime);
			} else if (updateExpr instanceof DeleteData) {
				executeDeleteData((DeleteData) updateExpr, uc, maxExecutionTime);
			} else if (updateExpr instanceof Clear) {
				executeClear((Clear) updateExpr, uc, maxExecutionTime);
			} else if (updateExpr instanceof Create) {
				executeCreate((Create) updateExpr, uc);
			} else if (updateExpr instanceof Copy) {
				executeCopy((Copy) updateExpr, uc, maxExecutionTime);
			} else if (updateExpr instanceof Add) {
				executeAdd((Add) updateExpr, uc, maxExecutionTime);
			} else if (updateExpr instanceof Move) {
				executeMove((Move) updateExpr, uc, maxExecutionTime);
			} else if (updateExpr instanceof Load) {
				throw new SailException("load operations can not be handled directly by the SAIL");
			}
		} finally {
			con.endUpdate(uc);
		}
	}

	protected void executeLoad(Load load, UpdateContext uc) throws IOException, RDFParseException, SailException {
		Value source = load.getSource().getValue();
		Value graph = load.getGraph() != null ? load.getGraph().getValue() : null;

		URL sourceURL = new URL(source.stringValue());

		RDFSailInserter rdfInserter = new RDFSailInserter(con, vf, uc);
		if (graph != null) {
			rdfInserter.enforceContext((Resource) graph);
		}
		try {
			loader.load(sourceURL, source.stringValue(), null, rdfInserter);
		} catch (RDFHandlerException e) {
			// RDFSailInserter only throws wrapped SailExceptions
			throw (SailException) e.getCause();
		}
	}

	protected void executeCreate(Create create, UpdateContext uc) throws SailException {
		// check if named graph exists, if so, we have to return an error.
		// Otherwise, we simply do nothing.
		Value graphValue = create.getGraph().getValue();

		if (graphValue instanceof Resource) {
			Resource namedGraph = (Resource) graphValue;

			try (CloseableIteration<? extends Resource, SailException> contextIDs = con.getContextIDs()) {
				while (contextIDs.hasNext()) {
					Resource contextID = contextIDs.next();

					if (namedGraph.equals(contextID)) {
						throw new SailException("Named graph " + namedGraph + " already exists. ");
					}
				}
			}
		}
	}

	/**
	 * @param copy
	 * @param uc
	 * @throws SailException
	 */
	protected void executeCopy(Copy copy, UpdateContext uc, int maxExecutionTime) throws SailException {
		ValueConstant sourceGraph = copy.getSourceGraph();
		ValueConstant destinationGraph = copy.getDestinationGraph();

		Resource source = sourceGraph != null ? (Resource) sourceGraph.getValue() : null;
		Resource destination = destinationGraph != null ? (Resource) destinationGraph.getValue() : null;

		if (source == null && destination == null || (source != null && source.equals(destination))) {
			// source and destination are the same, copy is a null-operation.
			return;
		}

		// clear destination
		final long start = System.currentTimeMillis();
		con.clear((Resource) destination);
		final long clearTime = (System.currentTimeMillis() - start) / 1000;

		if (maxExecutionTime > 0) {
			if (clearTime > maxExecutionTime) {
				throw new SailException("execution took too long");
			}
		}

		// get all statements from source and add them to destination
		CloseableIteration<? extends Statement, SailException> statements = null;
		try {
			statements = con.getStatements(null, null, null, uc.isIncludeInferred(), (Resource) source);

			if (maxExecutionTime > 0) {
				statements = new TimeLimitIteration<Statement, SailException>(statements,
						1000L * (maxExecutionTime - clearTime)) {

					@Override
					protected void throwInterruptedException() throws SailException {
						throw new SailException("execution took too long");
					}
				};
			}

			while (statements.hasNext()) {
				Statement st = statements.next();
				con.addStatement(uc, st.getSubject(), st.getPredicate(), st.getObject(), (Resource) destination);
			}
		} finally {
			if (statements != null) {
				statements.close();
			}
		}
	}

	/**
	 * @param add
	 * @param uc
	 * @throws SailException
	 */
	protected void executeAdd(Add add, UpdateContext uc, int maxExecTime) throws SailException {
		ValueConstant sourceGraph = add.getSourceGraph();
		ValueConstant destinationGraph = add.getDestinationGraph();

		Resource source = sourceGraph != null ? (Resource) sourceGraph.getValue() : null;
		Resource destination = destinationGraph != null ? (Resource) destinationGraph.getValue() : null;

		if (source == null && destination == null || (source != null && source.equals(destination))) {
			// source and destination are the same, copy is a null-operation.
			return;
		}

		// get all statements from source and add them to destination
		CloseableIteration<? extends Statement, SailException> statements = null;
		try {
			statements = con.getStatements(null, null, null, uc.isIncludeInferred(), (Resource) source);

			if (maxExecTime > 0) {
				statements = new TimeLimitIteration<Statement, SailException>(statements, 1000L * maxExecTime) {

					@Override
					protected void throwInterruptedException() throws SailException {
						throw new SailException("execution took too long");
					}
				};
			}

			while (statements.hasNext()) {
				Statement st = statements.next();
				con.addStatement(uc, st.getSubject(), st.getPredicate(), st.getObject(), (Resource) destination);
			}
		} finally {
			if (statements != null) {
				statements.close();
			}
		}
	}

	/**
	 * @param move
	 * @param uc
	 * @throws SailException
	 */
	protected void executeMove(Move move, UpdateContext uc, int maxExecutionTime) throws SailException {
		ValueConstant sourceGraph = move.getSourceGraph();
		ValueConstant destinationGraph = move.getDestinationGraph();

		Resource source = sourceGraph != null ? (Resource) sourceGraph.getValue() : null;
		Resource destination = destinationGraph != null ? (Resource) destinationGraph.getValue() : null;

		if (source == null && destination == null || (source != null && source.equals(destination))) {
			// source and destination are the same, move is a null-operation.
			return;
		}

		// clear destination
		final long start = System.currentTimeMillis();
		con.clear((Resource) destination);
		final long clearTime = (System.currentTimeMillis() - start) / 1000;

		if (maxExecutionTime > 0 && clearTime > maxExecutionTime) {
			throw new SailException("execution took too long");
		}

		// remove all statements from source and add them to destination
		CloseableIteration<? extends Statement, SailException> statements = null;

		try {
			statements = con.getStatements(null, null, null, uc.isIncludeInferred(), (Resource) source);
			if (maxExecutionTime > 0) {
				statements = new TimeLimitIteration<Statement, SailException>(statements,
						1000L * (maxExecutionTime - clearTime)) {

					@Override
					protected void throwInterruptedException() throws SailException {
						throw new SailException("execution took too long");
					}
				};
			}

			while (statements.hasNext()) {
				Statement st = statements.next();
				con.addStatement(uc, st.getSubject(), st.getPredicate(), st.getObject(), (Resource) destination);
				con.removeStatement(uc, st.getSubject(), st.getPredicate(), st.getObject(), (Resource) source);
			}
		} finally {
			if (statements != null) {
				statements.close();
			}
		}
	}

	/**
	 * @param clearExpr
	 * @param uc
	 * @throws SailException
	 */
	protected void executeClear(Clear clearExpr, UpdateContext uc, int maxExecutionTime) throws SailException {
		try {
			ValueConstant graph = clearExpr.getGraph();

			if (graph != null) {
				Resource context = (Resource) graph.getValue();
				con.clear(context);
			} else {
				Scope scope = clearExpr.getScope();
				if (Scope.NAMED_CONTEXTS.equals(scope)) {
					CloseableIteration<? extends Resource, SailException> contextIDs = null;
					try {
						contextIDs = con.getContextIDs();
						if (maxExecutionTime > 0) {
							contextIDs = new TimeLimitIteration<Resource, SailException>(contextIDs,
									1000L * maxExecutionTime) {

								@Override
								protected void throwInterruptedException() throws SailException {
									throw new SailException("execution took too long");
								}
							};
						}
						while (contextIDs.hasNext()) {
							con.clear(contextIDs.next());
						}
					} finally {
						if (contextIDs != null) {
							contextIDs.close();
						}
					}
				} else if (Scope.DEFAULT_CONTEXTS.equals(scope)) {
					con.clear((Resource) null);
				} else {
					con.clear();
				}
			}
		} catch (SailException e) {
			if (!clearExpr.isSilent()) {
				throw e;
			}
		}
	}

	/**
	 * @param insertDataExpr
	 * @param uc
	 * @throws SailException
	 */
	protected void executeInsertData(InsertData insertDataExpr, UpdateContext uc, int maxExecutionTime)
			throws SailException {

		SPARQLUpdateDataBlockParser parser = new SPARQLUpdateDataBlockParser(vf);
		RDFHandler handler = new RDFSailInserter(con, vf, uc);
		if (maxExecutionTime > 0) {
			handler = new TimeLimitRDFHandler(handler, 1000L * maxExecutionTime);
		}
		parser.setRDFHandler(handler);
		parser.setLineNumberOffset(insertDataExpr.getLineNumberOffset());
		parser.getParserConfig().addNonFatalError(BasicParserSettings.VERIFY_DATATYPE_VALUES);
		parser.getParserConfig().addNonFatalError(BasicParserSettings.FAIL_ON_UNKNOWN_DATATYPES);
		parser.getParserConfig().set(BasicParserSettings.SKOLEMIZE_ORIGIN, null);
		try {
			// TODO process update context somehow? dataset, base URI, etc.
			parser.parse(new StringReader(insertDataExpr.getDataBlock()), "");
		} catch (RDFParseException | RDFHandlerException | IOException e) {
			throw new SailException(e);
		}
	}

	/**
	 * @param deleteDataExpr
	 * @param uc
	 * @throws SailException
	 */
	protected void executeDeleteData(DeleteData deleteDataExpr, UpdateContext uc, int maxExecutionTime)
			throws SailException {

		SPARQLUpdateDataBlockParser parser = new SPARQLUpdateDataBlockParser(vf);
		parser.setLineNumberOffset(deleteDataExpr.getLineNumberOffset());
		parser.setAllowBlankNodes(false); // no blank nodes allowed in DELETE DATA.
		RDFHandler handler = new RDFSailRemover(con, vf, uc);
		if (maxExecutionTime > 0) {
			handler = new TimeLimitRDFHandler(handler, 1000L * maxExecutionTime);
		}
		parser.setRDFHandler(handler);

		try {
			// TODO process update context somehow? dataset, base URI, etc.
			parser.parse(new StringReader(deleteDataExpr.getDataBlock()), "");
		} catch (RDFParseException | RDFHandlerException | IOException e) {
			throw new SailException(e);
		}
	}

	protected void executeModify(Modify modify, UpdateContext uc, int maxExecutionTime) throws SailException {
		try {
			TupleExpr whereClause = modify.getWhereExpr();

			if (!(whereClause instanceof QueryRoot)) {
				whereClause = new QueryRoot(whereClause);
			}

			try (CloseableIteration<? extends BindingSet, QueryEvaluationException> sourceBindings = evaluateWhereClause(
					whereClause, uc, maxExecutionTime)) {
				while (sourceBindings.hasNext()) {
					BindingSet sourceBinding = sourceBindings.next();
					deleteBoundTriples(sourceBinding, modify.getDeleteExpr(), uc);

					insertBoundTriples(sourceBinding, modify.getInsertExpr(), uc);
				}
			}
		} catch (QueryEvaluationException e) {
			throw new SailException(e);
		}
	}

	private IRI[] getDefaultRemoveGraphs(Dataset dataset) {
		if (dataset == null) {
			return new IRI[0];
		}
		Set<IRI> set = new HashSet<>(dataset.getDefaultRemoveGraphs());
		if (set.isEmpty()) {
			return new IRI[0];
		}

		if (set.remove(SESAME.NIL) | set.remove(RDF4J.NIL)) {
			set.add(null);
		}

		return set.toArray(new IRI[set.size()]);
	}

	private CloseableIteration<? extends BindingSet, QueryEvaluationException> evaluateWhereClause(
			final TupleExpr whereClause, final UpdateContext uc, final int maxExecutionTime)
			throws SailException, QueryEvaluationException {
		CloseableIteration<? extends BindingSet, QueryEvaluationException> sourceBindings1 = null;
		CloseableIteration<? extends BindingSet, QueryEvaluationException> sourceBindings2 = null;
		ConvertingIteration<BindingSet, BindingSet, QueryEvaluationException> result = null;
		boolean allGood = false;
		try {
			sourceBindings1 = con.evaluate(whereClause, uc.getDataset(), uc.getBindingSet(), uc.isIncludeInferred());

			if (maxExecutionTime > 0) {
				sourceBindings2 = new TimeLimitIteration<BindingSet, QueryEvaluationException>(sourceBindings1,
						1000L * maxExecutionTime) {

					@Override
					protected void throwInterruptedException() throws QueryEvaluationException {
						throw new QueryInterruptedException("execution took too long");
					}
				};
			} else {
				sourceBindings2 = sourceBindings1;
			}

			result = new ConvertingIteration<BindingSet, BindingSet, QueryEvaluationException>(sourceBindings2) {

				@Override
				protected BindingSet convert(BindingSet sourceBinding) throws QueryEvaluationException {
					if (whereClause instanceof SingletonSet && sourceBinding instanceof EmptyBindingSet
							&& uc.getBindingSet() != null) {
						// in the case of an empty WHERE clause, we use the supplied bindings to produce triples to
						// DELETE/INSERT
						return uc.getBindingSet();
					} else {
						// check if any supplied bindings do not occur in the bindingset produced by the WHERE clause.
						// If so, merge.
						Set<String> uniqueBindings = new HashSet<>(uc.getBindingSet().getBindingNames());
						uniqueBindings.removeAll(sourceBinding.getBindingNames());
						if (uniqueBindings.size() > 0) {
							MapBindingSet mergedSet = new MapBindingSet();
							for (String bindingName : sourceBinding.getBindingNames()) {
								final Binding binding = sourceBinding.getBinding(bindingName);
								if (binding != null) {
									mergedSet.addBinding(binding);
								}
							}
							for (String bindingName : uniqueBindings) {
								final Binding binding = uc.getBindingSet().getBinding(bindingName);
								if (binding != null) {
									mergedSet.addBinding(binding);
								}
							}
							return mergedSet;
						}
						return sourceBinding;
					}
				}
			};
			allGood = true;
			return result;
		} finally {
			if (!allGood) {
				try {
					if (result != null) {
						result.close();
					}
				} finally {
					try {
						if (sourceBindings2 != null) {
							sourceBindings2.close();
						}
					} finally {
						if (sourceBindings1 != null) {
							sourceBindings1.close();
						}
					}
				}
			}
		}
	}

	/**
	 * @param whereBinding
	 * @param deleteClause
	 * @throws SailException
	 */
	private void deleteBoundTriples(BindingSet whereBinding, TupleExpr deleteClause, UpdateContext uc)
			throws SailException {
		if (deleteClause != null) {
			List<StatementPattern> deletePatterns = StatementPatternCollector.process(deleteClause);

			Value patternValue;
			for (StatementPattern deletePattern : deletePatterns) {

				patternValue = getValueForVar(deletePattern.getSubjectVar(), whereBinding);
				Resource subject = patternValue instanceof Resource ? (Resource) patternValue : null;

				patternValue = getValueForVar(deletePattern.getPredicateVar(), whereBinding);
				IRI predicate = patternValue instanceof IRI ? (IRI) patternValue : null;

				Value object = getValueForVar(deletePattern.getObjectVar(), whereBinding);

				Resource context = null;
				if (deletePattern.getContextVar() != null) {
					patternValue = getValueForVar(deletePattern.getContextVar(), whereBinding);
					context = patternValue instanceof Resource ? (Resource) patternValue : null;
				}

				if (subject == null || predicate == null || object == null) {
					/*
					 * skip removal of triple if any variable is unbound (may happen with optional patterns or if triple
					 * pattern forms illegal triple). See SES-1047 and #610.
					 */
					continue;
				}

				if (context != null) {
					if (RDF4J.NIL.equals(context) || SESAME.NIL.equals(context)) {
						con.removeStatement(uc, subject, predicate, object, (Resource) null);
					} else {
						con.removeStatement(uc, subject, predicate, object, context);
					}
				} else {
					IRI[] remove = getDefaultRemoveGraphs(uc.getDataset());
					con.removeStatement(uc, subject, predicate, object, remove);
				}
			}
		}
	}

	/**
	 * @param whereBinding
	 * @param insertClause
	 * @throws SailException
	 */
	private void insertBoundTriples(BindingSet whereBinding, TupleExpr insertClause, UpdateContext uc)
			throws SailException {
		if (insertClause != null) {
			List<StatementPattern> insertPatterns = StatementPatternCollector.process(insertClause);

			// bnodes in the insert pattern are locally scoped for each
			// individual source binding.
			MapBindingSet bnodeMapping = new MapBindingSet();
			for (StatementPattern insertPattern : insertPatterns) {
				Statement toBeInserted = createStatementFromPattern(insertPattern, whereBinding, bnodeMapping);

				if (toBeInserted != null) {
					IRI with = uc.getDataset().getDefaultInsertGraph();
					if (with == null && toBeInserted.getContext() == null) {
						con.addStatement(uc, toBeInserted.getSubject(), toBeInserted.getPredicate(),
								toBeInserted.getObject());
					} else if (toBeInserted.getContext() == null) {
						con.addStatement(uc, toBeInserted.getSubject(), toBeInserted.getPredicate(),
								toBeInserted.getObject(), with);
					} else {
						con.addStatement(uc, toBeInserted.getSubject(), toBeInserted.getPredicate(),
								toBeInserted.getObject(), toBeInserted.getContext());
					}
				}
			}
		}
	}

	private Statement createStatementFromPattern(StatementPattern pattern, BindingSet sourceBinding,
			MapBindingSet bnodeMapping) throws SailException {

		Resource subject = null;
		IRI predicate = null;
		Value object;
		Resource context = null;

		Value patternValue;
		if (pattern.getSubjectVar().hasValue()) {
			patternValue = pattern.getSubjectVar().getValue();
			if (patternValue instanceof Resource) {
				subject = (Resource) patternValue;
			}
		} else {
			patternValue = sourceBinding.getValue(pattern.getSubjectVar().getName());
			if (patternValue instanceof Resource) {
				subject = (Resource) patternValue;
			}

			if (subject == null && pattern.getSubjectVar().isAnonymous()) {
				Binding mappedSubject = bnodeMapping.getBinding(pattern.getSubjectVar().getName());

				if (mappedSubject != null) {
					patternValue = mappedSubject.getValue();
					if (patternValue instanceof Resource) {
						subject = (Resource) patternValue;
					}
				} else {
					subject = vf.createBNode();
					bnodeMapping.addBinding(pattern.getSubjectVar().getName(), subject);
				}
			}
		}

		if (subject == null) {
			return null;
		}

		if (pattern.getPredicateVar().hasValue()) {
			patternValue = pattern.getPredicateVar().getValue();
			if (patternValue instanceof IRI) {
				predicate = (IRI) patternValue;
			}
		} else {
			patternValue = sourceBinding.getValue(pattern.getPredicateVar().getName());
			if (patternValue instanceof IRI) {
				predicate = (IRI) patternValue;
			}
		}

		if (predicate == null) {
			return null;
		}

		if (pattern.getObjectVar().hasValue()) {
			object = pattern.getObjectVar().getValue();
		} else {
			object = sourceBinding.getValue(pattern.getObjectVar().getName());

			if (object == null && pattern.getObjectVar().isAnonymous()) {
				Binding mappedObject = bnodeMapping.getBinding(pattern.getObjectVar().getName());

				if (mappedObject != null) {
					patternValue = mappedObject.getValue();
					if (patternValue instanceof Resource) {
						object = (Resource) patternValue;
					}
				} else {
					object = vf.createBNode();
					bnodeMapping.addBinding(pattern.getObjectVar().getName(), object);
				}
			}
		}

		if (object == null) {
			return null;
		}

		if (pattern.getContextVar() != null) {
			if (pattern.getContextVar().hasValue()) {
				patternValue = pattern.getContextVar().getValue();
				if (patternValue instanceof Resource) {
					context = (Resource) patternValue;
				}
			} else {
				patternValue = sourceBinding.getValue(pattern.getContextVar().getName());
				if (patternValue instanceof Resource) {
					context = (Resource) patternValue;
				}
			}
		}

		Statement st = null;
		if (subject != null && predicate != null && object != null) {
			if (context != null) {
				st = vf.createStatement(subject, predicate, object, context);
			} else {
				st = vf.createStatement(subject, predicate, object);
			}
		}
		return st;
	}

	private Value getValueForVar(Var var, BindingSet bindings) throws SailException {
		Value value;
		if (var.hasValue()) {
			value = var.getValue();
		} else {
			value = bindings.getValue(var.getName());
		}
		return value;
	}
}
