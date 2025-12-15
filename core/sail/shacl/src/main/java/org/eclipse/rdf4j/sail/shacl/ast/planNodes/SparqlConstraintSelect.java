/*******************************************************************************
 * Copyright (c) 2023 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.ast.planNodes;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.BooleanLiteral;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.impl.MapBindingSet;
import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.eclipse.rdf4j.query.parser.QueryParserFactory;
import org.eclipse.rdf4j.query.parser.QueryParserRegistry;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.memory.MemoryStoreConnection;
import org.eclipse.rdf4j.sail.shacl.ast.ShaclSparqlConstraintFailureException;
import org.eclipse.rdf4j.sail.shacl.ast.Shape;
import org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents.ConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents.SparqlConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.results.ValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Håvard Ottestad
 */
public class SparqlConstraintSelect implements PlanNode {

	private static final Logger logger = LoggerFactory.getLogger(SparqlConstraintSelect.class);
	private static final Pattern MESSAGE_TEMPLATE_PATTERN = Pattern.compile("\\{[?$]([A-Za-z_][A-Za-z0-9_]*)\\}");

	private final SailConnection connection;

	private final PlanNode targets;
	private final String query;
	private final Resource[] dataGraph;
	private final boolean produceValidationReports;
	private final SparqlConstraintComponent constraintComponent;
	private final Shape shape;
	private final String[] variables;
	private final ConstraintComponent.Scope scope;
	private final Dataset dataset;
	private final ParsedQuery parsedQuery;
	private final boolean printed = false;
	private final Value shapesGraphBinding;
	private final Value currentShapeBinding;
	private final Value pathForMessageBinding;
	private ValidationExecutionLogger validationExecutionLogger;

	public SparqlConstraintSelect(SailConnection connection, PlanNode targets, String query,
			ConstraintComponent.Scope scope,
			Resource[] dataGraph, boolean produceValidationReports, SparqlConstraintComponent constraintComponent,
			Shape shape) {
		this.connection = connection;
		this.targets = targets;
		this.query = query;
		this.dataGraph = dataGraph;
		this.produceValidationReports = produceValidationReports;
		this.constraintComponent = constraintComponent;
		this.shape = shape;
		assert query.contains("$this") : "Query should contain $this: " + query;
		this.variables = new String[] { "$this" };
		this.scope = scope;
		this.dataset = PlanNodeHelper.asDefaultGraphDataset(dataGraph);
		this.currentShapeBinding = shape != null ? shape.getId() : null;
		this.shapesGraphBinding = determineShapesGraphBinding(shape);
		this.pathForMessageBinding = determinePathForMessageBinding(constraintComponent, scope);

		QueryParserFactory queryParserFactory = QueryParserRegistry.getInstance()
				.get(QueryLanguage.SPARQL)
				.get();

		try {
			this.parsedQuery = queryParserFactory.getParser().parseQuery(query, null);
		} catch (MalformedQueryException e) {
			logger.error("Malformed query: \n{}", query);
			throw e;
		}

	}

	private static Value determineShapesGraphBinding(Shape shape) {
		if (shape == null) {
			return null;
		}
		Resource[] contexts = shape.getContexts();
		if (contexts == null) {
			return null;
		}
		for (Resource context : contexts) {
			if (context != null && context.isIRI()) {
				return context;
			}
		}
		return null;
	}

	private static Value determinePathForMessageBinding(SparqlConstraintComponent constraintComponent,
			ConstraintComponent.Scope scope) {
		if (constraintComponent == null || scope != ConstraintComponent.Scope.propertyShape) {
			return null;
		}
		return constraintComponent.getTargetChain()
				.getPath()
				.map(p -> Values.literal(p.toSparqlPathString()))
				.orElse(null);
	}

	@Override
	public CloseableIteration<? extends ValidationTuple> iterator() {
		return new LoggingCloseableIteration(this, validationExecutionLogger) {

			CloseableIteration<? extends BindingSet> results;

			CloseableIteration<? extends ValidationTuple> targetIterator;

			ValidationTuple next;
			ValidationTuple nextTarget;

			@Override
			protected void init() {
				assert targetIterator == null;
				targetIterator = targets.iterator();
			}

			private void calculateNext() {
				while (next == null && (targetIterator.hasNext() || (results != null && results.hasNext()))) {

					if (results == null && targetIterator.hasNext()) {
						nextTarget = targetIterator.next();
						MapBindingSet bindings = new MapBindingSet(3);
						bindings.setBinding("this", nextTarget.getActiveTarget());
						if (currentShapeBinding != null) {
							bindings.setBinding("currentShape", currentShapeBinding);
						}
						if (shapesGraphBinding != null) {
							bindings.setBinding("shapesGraph", shapesGraphBinding);
						}
						results = connection.evaluate(parsedQuery.getTupleExpr(), dataset, bindings, true);
					}

					if (results.hasNext()) {
						BindingSet bindingSet = results.next();
						if (bindingSet.hasBinding("failure")) {
							if (bindingSet.getValue("failure").equals(BooleanLiteral.TRUE)) {
								throw new ShaclSparqlConstraintFailureException(shape, query, bindingSet,
										nextTarget.getActiveTarget(), dataGraph);
							}
						}

						Value value1 = bindingSet.getValue("value");
						if (value1 == null) {
							value1 = nextTarget.getValue();
						}
						Value currentValue = value1;

						Value path = bindingSet.getValue("path");
						List<Literal> resultMessages = null;

						Value messageValue = bindingSet.getValue("message");
						if (messageValue != null) {
							if (messageValue.isLiteral()) {
								resultMessages = List.of((Literal) messageValue);
							} else {
								resultMessages = List.of(Values.literal(messageValue.stringValue()));
							}
						} else if (produceValidationReports) {
							List<Literal> templates = constraintComponent.getDefaultMessage();
							if (!templates.isEmpty()) {
								if (constraintComponent.hasMessageTemplateVariables()) {
									Value focusNode = nextTarget.getActiveTarget();
									resultMessages = templates.stream()
											.map(t -> substituteMessageTemplate(t, bindingSet, focusNode,
													shapesGraphBinding, currentShapeBinding,
													pathForMessageBinding))
											.collect(Collectors.toList());
								} else {
									resultMessages = List.copyOf(templates);
								}
							}
						}

						final List<Literal> finalResultMessages = resultMessages;

						if (scope == ConstraintComponent.Scope.nodeShape) {
							next = nextTarget.addValidationResult(t -> {
								ValidationResult validationResult = new ValidationResult(t.getActiveTarget(),
										currentValue,
										shape,
										constraintComponent, shape.getSeverity(),
										ConstraintComponent.Scope.nodeShape, t.getContexts(),
										shape.getContexts());
								if (finalResultMessages != null) {
									validationResult.setMessagesOverride(finalResultMessages);
								}
								if (path != null && path.isIRI()) {
									validationResult.setPathIri(path);
								}
								return validationResult;
							});
						} else {

							ValidationTuple validationTuple = new ValidationTuple(nextTarget.getActiveTarget(),
									currentValue,
									scope, currentValue != null, nextTarget.getContexts());
							next = ValidationTupleHelper.join(nextTarget, validationTuple).addValidationResult(t -> {
								ValidationResult validationResult = new ValidationResult(t.getActiveTarget(),
										currentValue,
										shape,
										constraintComponent, shape.getSeverity(),
										ConstraintComponent.Scope.propertyShape, t.getContexts(),
										shape.getContexts());
								if (finalResultMessages != null) {
									validationResult.setMessagesOverride(finalResultMessages);
								}
								if (path != null && path.isIRI()) {
									validationResult.setPathIri(path);
								}
								return validationResult;
							});
						}
					} else {
						results.close();
						results = null;
					}

				}

			}

			@Override
			protected boolean localHasNext() {
				calculateNext();
				return next != null;
			}

			@Override
			protected ValidationTuple loggingNext() {
				calculateNext();
				ValidationTuple temp = next;
				next = null;
				return temp;
			}

			@Override
			public void localClose() {
				try {
					if (targetIterator != null) {
						targetIterator.close();
					}
				} finally {
					if (results != null) {
						results.close();
					}
				}
			}

		};
	}

	@Override
	public int depth() {
		return targets.depth() + 1;
	}

	@Override
	public void getPlanAsGraphvizDot(StringBuilder stringBuilder) {
//		if (printed) {
//			return;
//		}
//		printed = true;
//		stringBuilder.append(getId() + " [label=\"" + StringEscapeUtils.escapeJava(this.toString()) + "\"];")
//			.append("\n");
//
//		// added/removed connections are always newly minted per plan node, so we instead need to compare the underlying
//		// sail
//		if (connection instanceof MemoryStoreConnection) {
//			stringBuilder
//				.append(System.identityHashCode(((MemoryStoreConnection) connection).getSail()) + " -> " + getId())
//				.append("\n");
//		} else {
//			stringBuilder.append(System.identityHashCode(connection) + " -> " + getId()).append("\n");
//		}

	}

	private static Literal substituteMessageTemplate(Literal template, BindingSet bindingSet, Value focusNode,
			Value shapesGraphBinding, Value currentShapeBinding, Value pathForMessageBinding) {
		String label = template.getLabel();
		Matcher matcher = MESSAGE_TEMPLATE_PATTERN.matcher(label);
		if (!matcher.find()) {
			return template;
		}

		matcher.reset();
		StringBuffer sb = new StringBuffer();
		while (matcher.find()) {
			String varName = matcher.group(1);
			Value value = bindingSet.getValue(varName);
			if (value == null) {
				if ("this".equals(varName)) {
					value = focusNode;
				} else if ("shapesGraph".equals(varName)) {
					value = shapesGraphBinding;
				} else if ("currentShape".equals(varName)) {
					value = currentShapeBinding;
				} else if ("PATH".equals(varName)) {
					value = pathForMessageBinding;
				}
			}
			String replacement = value != null ? value.stringValue() : matcher.group(0);
			matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
		}
		matcher.appendTail(sb);

		if (template.getLanguage().isPresent()) {
			return Values.literal(sb.toString(), template.getLanguage().get());
		}
		return Values.literal(sb.toString(), template.getDatatype());
	}

	@Override
	public String getId() {
		return System.identityHashCode(this) + "";
	}

	@Override
	public String toString() {
		return "SparqlConstraintSelect{" +
				"targets=" + targets +
				", query='" + query.replace("\n", "  ") + '\'' +
				", dataGraph=" + Arrays.toString(dataGraph) +
				", scope=" + scope +
				'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		SparqlConstraintSelect that = (SparqlConstraintSelect) o;

		if (scope != that.scope) {
			return false;
		}
		if (!targets.equals(that.targets)) {
			return false;
		}
		if (!query.equals(that.query)) {
			return false;
		}
		// Probably incorrect - comparing Object[] arrays with Arrays.equals
		if (!Arrays.equals(dataGraph, that.dataGraph)) {
			return false;
		}
		// Probably incorrect - comparing Object[] arrays with Arrays.equals
		if (!Arrays.equals(variables, that.variables)) {
			return false;
		}

		return Objects.equals(
				connection instanceof MemoryStoreConnection ? ((MemoryStoreConnection) connection).getSail()
						: connection,
				that.connection instanceof MemoryStoreConnection
						? ((MemoryStoreConnection) that.connection).getSail()
						: that.connection);
	}

	@Override
	public int hashCode() {
		if (connection instanceof MemoryStoreConnection) {
			int result = ((MemoryStoreConnection) connection).getSail().hashCode();
			result = 31 * result + targets.hashCode();
			result = 31 * result + query.hashCode();
			result = 31 * result + Arrays.hashCode(dataGraph);
			result = 31 * result + Arrays.hashCode(variables);
			result = 31 * result + scope.hashCode();
			return result;
		} else {
			int result = connection.hashCode();
			result = 31 * result + targets.hashCode();
			result = 31 * result + query.hashCode();
			result = 31 * result + Arrays.hashCode(dataGraph);
			result = 31 * result + Arrays.hashCode(variables);
			result = 31 * result + scope.hashCode();
			return result;
		}
	}

	@Override
	public void receiveLogger(ValidationExecutionLogger validationExecutionLogger) {
		this.validationExecutionLogger = validationExecutionLogger;
		targets.receiveLogger(validationExecutionLogger);
	}

	@Override
	public boolean producesSorted() {
		return false;
	}

	@Override
	public boolean requiresSorted() {
		return false;
	}
}
