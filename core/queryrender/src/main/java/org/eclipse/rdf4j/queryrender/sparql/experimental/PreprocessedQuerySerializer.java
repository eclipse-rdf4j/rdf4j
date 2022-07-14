/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.queryrender.sparql.experimental;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.algebra.Add;
import org.eclipse.rdf4j.query.algebra.And;
import org.eclipse.rdf4j.query.algebra.ArbitraryLengthPath;
import org.eclipse.rdf4j.query.algebra.Avg;
import org.eclipse.rdf4j.query.algebra.BNodeGenerator;
import org.eclipse.rdf4j.query.algebra.BindingSetAssignment;
import org.eclipse.rdf4j.query.algebra.Bound;
import org.eclipse.rdf4j.query.algebra.Clear;
import org.eclipse.rdf4j.query.algebra.Coalesce;
import org.eclipse.rdf4j.query.algebra.Compare;
import org.eclipse.rdf4j.query.algebra.CompareAll;
import org.eclipse.rdf4j.query.algebra.CompareAny;
import org.eclipse.rdf4j.query.algebra.Copy;
import org.eclipse.rdf4j.query.algebra.Count;
import org.eclipse.rdf4j.query.algebra.Create;
import org.eclipse.rdf4j.query.algebra.Datatype;
import org.eclipse.rdf4j.query.algebra.DeleteData;
import org.eclipse.rdf4j.query.algebra.Difference;
import org.eclipse.rdf4j.query.algebra.Distinct;
import org.eclipse.rdf4j.query.algebra.EmptySet;
import org.eclipse.rdf4j.query.algebra.Exists;
import org.eclipse.rdf4j.query.algebra.Extension;
import org.eclipse.rdf4j.query.algebra.ExtensionElem;
import org.eclipse.rdf4j.query.algebra.Filter;
import org.eclipse.rdf4j.query.algebra.FunctionCall;
import org.eclipse.rdf4j.query.algebra.Group;
import org.eclipse.rdf4j.query.algebra.GroupConcat;
import org.eclipse.rdf4j.query.algebra.GroupElem;
import org.eclipse.rdf4j.query.algebra.IRIFunction;
import org.eclipse.rdf4j.query.algebra.If;
import org.eclipse.rdf4j.query.algebra.In;
import org.eclipse.rdf4j.query.algebra.InsertData;
import org.eclipse.rdf4j.query.algebra.Intersection;
import org.eclipse.rdf4j.query.algebra.IsBNode;
import org.eclipse.rdf4j.query.algebra.IsLiteral;
import org.eclipse.rdf4j.query.algebra.IsNumeric;
import org.eclipse.rdf4j.query.algebra.IsResource;
import org.eclipse.rdf4j.query.algebra.IsURI;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.Label;
import org.eclipse.rdf4j.query.algebra.Lang;
import org.eclipse.rdf4j.query.algebra.LangMatches;
import org.eclipse.rdf4j.query.algebra.LeftJoin;
import org.eclipse.rdf4j.query.algebra.ListMemberOperator;
import org.eclipse.rdf4j.query.algebra.Load;
import org.eclipse.rdf4j.query.algebra.LocalName;
import org.eclipse.rdf4j.query.algebra.MathExpr;
import org.eclipse.rdf4j.query.algebra.Max;
import org.eclipse.rdf4j.query.algebra.Min;
import org.eclipse.rdf4j.query.algebra.Modify;
import org.eclipse.rdf4j.query.algebra.Move;
import org.eclipse.rdf4j.query.algebra.MultiProjection;
import org.eclipse.rdf4j.query.algebra.Namespace;
import org.eclipse.rdf4j.query.algebra.Not;
import org.eclipse.rdf4j.query.algebra.Or;
import org.eclipse.rdf4j.query.algebra.Order;
import org.eclipse.rdf4j.query.algebra.OrderElem;
import org.eclipse.rdf4j.query.algebra.Projection;
import org.eclipse.rdf4j.query.algebra.ProjectionElem;
import org.eclipse.rdf4j.query.algebra.ProjectionElemList;
import org.eclipse.rdf4j.query.algebra.QueryRoot;
import org.eclipse.rdf4j.query.algebra.Reduced;
import org.eclipse.rdf4j.query.algebra.Regex;
import org.eclipse.rdf4j.query.algebra.SameTerm;
import org.eclipse.rdf4j.query.algebra.Sample;
import org.eclipse.rdf4j.query.algebra.Service;
import org.eclipse.rdf4j.query.algebra.SingletonSet;
import org.eclipse.rdf4j.query.algebra.Slice;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.Str;
import org.eclipse.rdf4j.query.algebra.Sum;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.Union;
import org.eclipse.rdf4j.query.algebra.ValueConstant;
import org.eclipse.rdf4j.query.algebra.ValueExpr;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.ZeroLengthPath;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;
import org.eclipse.rdf4j.queryrender.RenderUtils;
import org.eclipse.rdf4j.queryrender.sparql.experimental.SerializableParsedTupleQuery.QueryModifier;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.AbstractRDFHandler;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * This class processes a {@link SerializableParsedTupleQuery} and renders it as a SPARQL string.
 *
 * @author Andriy Nikolov
 * @author Jeen Broekstra
 * @author Andreas Schwarte
 *
 */
class PreprocessedQuerySerializer extends AbstractQueryModelVisitor<RuntimeException> {

	/**
	 * Enumeration of standard SPARQL 1.1 functions that are neither recognized by RDF4J as special value expressions
	 * nor defined as IRI functions in the <i>fn:</i> namespace (see {@link FNFunction}).
	 *
	 */
	protected enum NonIriFunctions {
		STRLANG,
		STRDT,
		UUID,
		STRUUID,
		RAND,
		NOW,
		TZ,
		MD5,
		SHA1,
		SHA256,
		SHA384,
		SHA512;

		public static boolean contains(String token) {
			try {
				valueOf(token.toUpperCase());
				return true;
			} catch (IllegalArgumentException e) {
				return false;
			}
		}
	}

	private final Map<Projection, SerializableParsedTupleQuery> queriesByProjection = new HashMap<>();

	private AbstractSerializableParsedQuery currentQueryProfile = null;

	private SerializableParsedUpdate currentUpdate = null;

	protected StringBuilder builder;

	private final Map<AbstractSerializableParsedQuery, Set<String>> renderedExtensionElements = Maps.newHashMap();

	private boolean insideFunction = false;

	public PreprocessedQuerySerializer() {
		this.builder = new StringBuilder();
	}

	/**
	 * Serializes a {@link SerializableParsedTupleQuery} passed as an input.
	 *
	 * @param query a parsed tuple query previously produced by {@link ParsedQueryPreprocessor}
	 * @return string SPARQL serialization of the query
	 */
	public String serialize(SerializableParsedTupleQuery query) {

		this.builder = new StringBuilder();

		this.queriesByProjection.putAll(query.subQueriesByProjection);

		processTupleQuery(query);

		return builder.toString().trim();
	}

	/**
	 * Serializes a {@link SerializableParsedBooleanQuery} passed as an input.
	 *
	 * @param query a parsed tuple query previously produced by {@link ParsedQueryPreprocessor}
	 * @return string SPARQL serialization of the query
	 */
	public String serialize(SerializableParsedBooleanQuery query) {

		this.builder = new StringBuilder();

		this.queriesByProjection.putAll(query.subQueriesByProjection);

		processBooleanQuery(query);

		return builder.toString().trim();
	}

	public String serialize(SerializableParsedConstructQuery query) {
		this.builder = new StringBuilder();
		this.queriesByProjection.putAll(query.subQueriesByProjection);
		if (query.describe) {
			processDescribeQuery(query);
		} else {
			processConstructQuery(query);
		}

		return builder.toString().trim();
	}

	public String serialize(SerializableParsedUpdate update) {
		this.builder = new StringBuilder();
		this.queriesByProjection.putAll(update.subQueriesByProjection);

		processUpdate(update);

		return builder.toString().trim();
	}

	private void processDatasetClause(Dataset dataset) {
		if (dataset != null) {
			for (IRI defaultGraph : dataset.getDefaultGraphs()) {
				builder.append("FROM ");
				this.meet(defaultGraph);
				builder.append(" \n");
			}
			for (IRI namedGraph : dataset.getNamedGraphs()) {
				builder.append("FROM NAMED ");
				this.meet(namedGraph);
				builder.append(" \n");
			}
		}
	}

	private void processBooleanQuery(SerializableParsedBooleanQuery query) {
		renderedExtensionElements.put(query, Sets.newHashSet());
		this.currentQueryProfile = query;

		builder.append("ASK ");
		processDatasetClause(query.dataset);
		builder.append("WHERE ");
		builder.append("{ \n");

		this.meetWhereClause(query.whereClause);

		builder.append(" }\n ");

		if (query.bindings != null) {
			this.meet(query.bindings);
			builder.append("\n");
		}
	}

	private void processDescribeQuery(SerializableParsedConstructQuery query) {
		renderedExtensionElements.put(query, Sets.newHashSet());
		this.currentQueryProfile = query;
		builder.append("DESCRIBE ");
		for (ProjectionElemList pr : query.projection.getProjections()) {
			pr.visit(this);
		}
		processDatasetClause(query.dataset);

		if ((query.whereClause != null) && !(query.whereClause instanceof SingletonSet)) {

			builder.append("WHERE { \n");

			this.meetWhereClause(query.whereClause);

			builder.append(" }\n ");
		}

		if (query.limit != null) {
			this.writeLimit(query.limit);
			builder.append("\n");
		}

		if (query.bindings != null) {
			this.meet(query.bindings);
			builder.append("\n");
		}
	}

	private void processConstructQuery(SerializableParsedConstructQuery query) {
		renderedExtensionElements.put(query, Sets.newHashSet());
		this.currentQueryProfile = query;
		builder.append("CONSTRUCT { \n");

		this.meet(query.projection);
		builder.append("} ");

		processDatasetClause(query.dataset);

		builder.append("WHERE { \n");

		this.meetWhereClause(query.whereClause);

		builder.append(" }\n ");

		if (query.orderBy != null) {
			this.meet(query.orderBy);
			builder.append("\n");
		}

		if (query.limit != null) {
			this.writeLimit(query.limit);
			builder.append("\n");
		}

		if (query.bindings != null) {
			this.meet(query.bindings);
			builder.append("\n");
		}

	}

	private void processUpdate(SerializableParsedUpdate update) {
		this.currentUpdate = update;
		renderedExtensionElements.put(update, Sets.newHashSet());
		update.updateExpr.visit(this);
	}

	private void processTupleQuery(SerializableParsedTupleQuery query) {
		renderedExtensionElements.put(query, Sets.newHashSet());

		final AbstractSerializableParsedQuery prevQuery = this.currentQueryProfile;
		this.currentQueryProfile = query;

		if (query.projection != null) {
			builder.append("SELECT ");

			if (query.modifier != null) {
				if (query.modifier.equals(QueryModifier.DISTINCT)) {
					builder.append("DISTINCT ");
				} else if (query.modifier.equals(QueryModifier.REDUCED)) {
					builder.append("REDUCED ");
				}
			}
			this.meet(query.projection.getProjectionElemList());
			builder.append("\n");
			processDatasetClause(query.dataset);
			builder.append("WHERE ");
		}
		builder.append("{ \n");

		this.meetWhereClause(query.whereClause);

		builder.append(" }\n ");

		if (query.groupBy != null) {
			this.meet(query.groupBy);
			if (query.having != null) {
				builder.append("HAVING (");
				query.having.getCondition().visit(this);
				builder.append(") ");
			}
			builder.append("\n");
		}

		if (query.orderBy != null) {
			this.meet(query.orderBy);
			builder.append("\n");
		}

		if (query.limit != null) {
			this.writeLimit(query.limit);
			builder.append("\n");
		}

		if (query.bindings != null) {
			this.meet(query.bindings);
			builder.append("\n");
		}

		this.currentQueryProfile = prevQuery;
	}

	/**
	 * Serializes the TupleExpr serving as a WHERE clause of the query.
	 *
	 * @param whereClause a TupleExpr representing a WHERE clause
	 */
	public void meetWhereClause(TupleExpr whereClause) {
		// The whereClause cannot be null for full queries,
		// but can be null when we only render a TupleExpr,
		// e.g., a VALUES clause without any graph patterns.
		if (whereClause != null) {
			whereClause.visit(this);
		}
	}

	@Override
	public void meet(QueryRoot node) throws RuntimeException {
		super.meet(node);
	}

	@Override
	public void meet(Add node) throws RuntimeException {
		builder.append("ADD ");
		if (node.isSilent()) {
			builder.append("SILENT ");
		}
		if (node.getSourceGraph() != null) {
			builder.append("GRAPH ");
			meet(node.getSourceGraph());
		} else {
			builder.append("DEFAULT ");
		}
		builder.append("TO ");
		if (node.getDestinationGraph() != null) {
			builder.append("GRAPH ");
			meet(node.getDestinationGraph());
		} else {
			builder.append("DEFAULT ");
		}
	}

	@Override
	public void meet(And node) throws RuntimeException {
		node.getLeftArg().visit(this);
		builder.append(" && ");
		node.getRightArg().visit(this);
	}

	@Override
	public void meet(ArbitraryLengthPath node) throws RuntimeException {
		PropertyPathSerializer serializer = new PropertyPathSerializer();
		builder.append("\t");
		builder.append(serializer.serialize(node, currentQueryProfile));
		builder.append("\n");
	}

	@Override
	public void meet(Avg node) throws RuntimeException {
		writeAsAggregationFunction("AVG", node.getArg(), node.isDistinct());
	}

	public void meet(Value node) {
		builder.append(RenderUtils.toSPARQL(node));
	}

	@Override
	public void meet(BindingSetAssignment node) throws RuntimeException {

		List<String> bindingNames = new ArrayList<>(node.getBindingNames());

		builder.append("VALUES (");
		for (String var : bindingNames) {
			builder.append("?");
			builder.append(var);
			builder.append(" ");
		}

		builder.append(") { ");
		for (BindingSet bs : node.getBindingSets()) {
			builder.append("(");
			for (String s : bindingNames) {
				if (bs.getValue(s) != null) {
					this.meet(bs.getValue(s));
				} else {
					builder.append("UNDEF ");
				}
			}
			builder.append(") ");
		}
		builder.append(" } ");
	}

	@Override
	public void meet(BNodeGenerator node) throws RuntimeException {
		writeAsFunction("BNODE", Lists.newArrayList());
	}

	@Override
	public void meet(Bound node) throws RuntimeException {
		writeAsFunction("BOUND", node.getArg());
	}

	@Override
	public void meet(Clear clear) throws RuntimeException {
		builder.append("CLEAR ");
		if (clear.isSilent()) {
			builder.append("SILENT ");
		}
		if (clear.getGraph() != null) {
			builder.append("GRAPH ");
			meet(clear.getGraph());
		} else if (clear.getScope() != null) {
			switch (clear.getScope()) {
			case DEFAULT_CONTEXTS:
				builder.append("DEFAULT");
				break;
			case NAMED_CONTEXTS:
				builder.append("NAMED");
				break;
			default:

				break;
			}
		} else {
			builder.append("ALL");
		}
	}

	@Override
	public void meet(Coalesce node) throws RuntimeException {
		writeAsFunction("COALESCE", node.getArguments());
	}

	@Override
	public void meet(Compare node) throws RuntimeException {
		node.getLeftArg().visit(this);
		builder.append(" ");
		builder.append(node.getOperator().getSymbol());
		builder.append(" ");
		node.getRightArg().visit(this);
	}

	@Override
	public void meet(CompareAll node) throws RuntimeException {
		super.meet(node);

	}

	@Override
	public void meet(CompareAny node) throws RuntimeException {
		super.meet(node);

	}

	@Override
	public void meet(Copy node) throws RuntimeException {
		builder.append("COPY ");
		if (node.isSilent()) {
			builder.append("SILENT ");
		}
		if (node.getSourceGraph() != null) {
			builder.append("GRAPH ");
			meet(node.getSourceGraph());
		} else {
			builder.append("DEFAULT ");
		}
		builder.append("TO ");
		if (node.getDestinationGraph() != null) {
			builder.append("GRAPH ");
			meet(node.getDestinationGraph());
		} else {
			builder.append("DEFAULT ");
		}
	}

	@Override
	public void meet(Count node) throws RuntimeException {
		writeAsAggregationFunction("COUNT", node.getArg(), node.isDistinct());
	}

	@Override
	public void meet(Create create) throws RuntimeException {
		builder.append("CREATE ");
		if (create.isSilent()) {
			builder.append("SILENT ");
		}
		if (create.getGraph() != null) {
			builder.append("GRAPH ");
			meet(create.getGraph());
		}
	}

	@Override
	public void meet(Datatype node) throws RuntimeException {
		writeAsFunction("DATATYPE", node.getArg());
	}

	@Override
	public void meet(DeleteData deleteData) throws RuntimeException {
		builder.append("DELETE DATA { \n");
		meetUpdateDataBlock(deleteData.getDataBlock());
		builder.append("} ");
	}

	@Override
	public void meet(Difference node) throws RuntimeException {
		builder.append("{\n");

		node.getLeftArg().visit(this);

		builder.append("}\n MINUS\n{\n");

		node.getRightArg().visit(this);

		builder.append("}\n");
	}

	@Override
	public void meet(Distinct node) throws RuntimeException {
		super.meet(node);

	}

	@Override
	public void meet(EmptySet node) throws RuntimeException {
		super.meet(node);
	}

	@Override
	public void meet(Exists node) throws RuntimeException {
		builder.append("EXISTS {");
		node.getSubQuery().visit(this);
		builder.append("} ");
	}

	@Override
	public void meet(Extension node) throws RuntimeException {
		node.getArg().visit(this);
		for (ExtensionElem element : node.getElements()) {
			if (!isTautologicalExtensionElem(element)
					&& !isExtensionElemAlreadyRendered(element)) {
				builder.append("\tBIND (");
				element.visit(this);
				builder.append(") . \n");
			}

		}
	}

	protected boolean isExtensionElemAlreadyRendered(ExtensionElem element) {
		Set<String> alreadyRenderedList = this.renderedExtensionElements.get(this.currentQueryProfile);
		if (alreadyRenderedList != null) {
			return alreadyRenderedList.contains(element.getName());
		}
		return false;
	}

	protected void setExtensionElemAlreadyRendered(ExtensionElem element) {
		Set<String> alreadyRenderedList = this.renderedExtensionElements.get(this.currentQueryProfile);
		if (alreadyRenderedList == null) {
			alreadyRenderedList = Sets.newHashSet();
			this.renderedExtensionElements.put(this.currentQueryProfile, alreadyRenderedList);
		}
		alreadyRenderedList.add(element.getName());
	}

	@Override
	public void meet(ExtensionElem node) throws RuntimeException {
		node.getExpr().visit(this);
		builder.append(" AS ?");
		builder.append(node.getName());
		setExtensionElemAlreadyRendered(node);
	}

	@Override
	public void meet(Filter node) throws RuntimeException {
		boolean isHaving = false;
		if (currentQueryProfile instanceof SerializableParsedTupleQuery) {
			isHaving = node.equals(((SerializableParsedTupleQuery) currentQueryProfile).having);
		}

		if (currentQueryProfile == null || !isHaving) {
			node.getArg().visit(this);
			builder.append(" FILTER ");
			builder.append("(");
			node.getCondition().visit(this);
			builder.append(") ");
		}
	}

	@Override
	public void meet(FunctionCall node) throws RuntimeException {
		// RDF4J doesn't recognize CONCAT as a built-in function,
		// but assumes that it has the default URI namespace.
		// This leads to failures when sending the query to other triple stores
		// like Blazegraph
		writeAsFunction(getFunctionNameAsString(node), node.getArgs());
	}

	@Override
	public void meet(Group node) throws RuntimeException {
		if (!node.getGroupBindingNames().isEmpty()) {
			builder.append("GROUP BY ");
			for (String name : node.getGroupBindingNames()) {
				builder.append("?");
				builder.append(name);
				builder.append(" ");
			}
		}
	}

	@Override
	public void meet(GroupConcat node) throws RuntimeException {
		builder.append("GROUP_CONCAT(");
		if (node.isDistinct()) {
			builder.append("DISTINCT ");
		}
		node.getArg().visit(this);
		if (node.getSeparator() != null) {
			builder.append(";separator=");
			node.getSeparator().visit(this);
		}
		builder.append(") ");
	}

	@Override
	public void meet(GroupElem node) throws RuntimeException {
		super.meet(node);
	}

	@Override
	public void meet(If node) throws RuntimeException {
		writeAsFunction("IF",
				Lists.newArrayList(node.getCondition(), node.getResult(), node.getAlternative()));
	}

	@Override
	public void meet(In node) throws RuntimeException {
		super.meet(node);
	}

	@Override
	public void meet(InsertData insertData) throws RuntimeException {
		builder.append("INSERT DATA { \n");
		meetUpdateDataBlock(insertData.getDataBlock());
		builder.append("} ");
	}

	protected void meetUpdateDataBlock(String dataBlock) throws RuntimeException {
		RDFParser parser = Rio.createParser(RDFFormat.TRIG);

		parser.setRDFHandler(new AbstractRDFHandler() {

			@Override
			public void handleStatement(Statement st) throws RDFHandlerException {
				PreprocessedQuerySerializer.this.meet(st.getSubject());
				builder.append(" ");
				PreprocessedQuerySerializer.this.meet(st.getPredicate());
				builder.append(" ");
				PreprocessedQuerySerializer.this.meet(st.getObject());
				builder.append(" . \n");
			}
		});

		if (!StringUtils.isEmpty(dataBlock)) {

			try {
				parser.parse(new StringReader(dataBlock), "");
			} catch (IOException e) {
				// No-op
			}
		}
	}

	@Override
	public void meet(Intersection node) throws RuntimeException {
		throw new UnsupportedOperationException("Unsupported operator: Intersection");
	}

	@Override
	public void meet(IRIFunction node) throws RuntimeException {
		writeAsFunction("IRI", node.getArg());
	}

	@Override
	public void meet(IsBNode node) throws RuntimeException {
		writeAsFunction("isBlank", node.getArg());
	}

	@Override
	public void meet(IsLiteral node) throws RuntimeException {
		writeAsFunction("isLITERAL", node.getArg());
	}

	@Override
	public void meet(IsNumeric node) throws RuntimeException {
		writeAsFunction("isNUMERIC", node.getArg());

	}

	@Override
	public void meet(IsResource node) throws RuntimeException {
		writeAsFunction("isRESOURCE", node.getArg());

	}

	@Override
	public void meet(IsURI node) throws RuntimeException {
		writeAsFunction("isURI", node.getArg());

	}

	@Override
	public void meet(Join node) throws RuntimeException {
		node.getLeftArg().visit(this);
		node.getRightArg().visit(this);
	}

	@Override
	public void meet(Label node) throws RuntimeException {
		writeAsFunction("LABEL", node.getArg());

	}

	@Override
	public void meet(Lang node) throws RuntimeException {
		writeAsFunction("LANG", node.getArg());

	}

	@Override
	public void meet(LangMatches node) throws RuntimeException {
		writeAsFunction("langMatches", Lists.newArrayList(node.getLeftArg(), node.getRightArg()));
	}

	@Override
	public void meet(LeftJoin node) throws RuntimeException {
		node.getLeftArg().visit(this);
		builder.append(" OPTIONAL { ");
		node.getRightArg().visit(this);
		if (node.hasCondition()) {
			builder.append(" FILTER (");
			node.getCondition().visit(this);
			builder.append(")");
		}
		builder.append("} ");

	}

	@Override
	public void meet(ListMemberOperator node) throws RuntimeException {
		Iterator<ValueExpr> argIter = node.getArguments().iterator();
		ValueExpr operand = argIter.next();
		operand.visit(this);
		builder.append(" IN (");
		if (argIter.hasNext()) {
			argIter.next().visit(this);
		}
		while (argIter.hasNext()) {
			builder.append(", ");
			argIter.next().visit(this);
		}
		builder.append(") ");
	}

	@Override
	public void meet(Load load) throws RuntimeException {
		builder.append("LOAD ");
		if (load.isSilent()) {
			builder.append("SILENT ");
		}
		meet(load.getSource());
		if (load.getGraph() != null) {
			builder.append(" INTO GRAPH ");
			meet(load.getGraph());
		}
	}

	@Override
	public void meet(LocalName node) throws RuntimeException {
		super.meet(node);

	}

	@Override
	public void meet(MathExpr node) throws RuntimeException {
		builder.append("(");
		node.getLeftArg().visit(this);
		builder.append(node.getOperator().getSymbol());
		node.getRightArg().visit(this);
		builder.append(") ");

	}

	@Override
	public void meet(Max node) throws RuntimeException {
		writeAsAggregationFunction("MAX", node.getArg(), node.isDistinct());
	}

	@Override
	public void meet(Min node) throws RuntimeException {
		writeAsAggregationFunction("MIN", node.getArg(), node.isDistinct());

	}

	@Override
	public void meet(Modify modify) throws RuntimeException {
		renderedExtensionElements.put(this.currentUpdate, Sets.newHashSet());
		if (modify.getDeleteExpr() != null) {
			builder.append("DELETE { \n");
			modify.getDeleteExpr().visit(this);
			builder.append(" } \n");
		}
		if (modify.getInsertExpr() != null) {
			builder.append("INSERT { \n");
			modify.getInsertExpr().visit(this);
			builder.append(" } \n");
		}
		if (modify.getWhereExpr() != null) {
			builder.append(" WHERE { \n");

			this.meetWhereClause(modify.getWhereExpr());

			builder.append(" }\n ");

			if (this.currentUpdate.limit != null) {
				this.writeLimit(this.currentUpdate.limit);
				builder.append("\n");
			}

			if (this.currentUpdate.bindings != null) {
				this.meet(this.currentUpdate.bindings);
				builder.append("\n");
			}
		}
	}

	@Override
	public void meet(Move node) throws RuntimeException {
		builder.append("MOVE ");
		if (node.isSilent()) {
			builder.append("SILENT ");
		}
		if (node.getSourceGraph() != null) {
			builder.append("GRAPH ");
			meet(node.getSourceGraph());
		} else {
			builder.append("DEFAULT ");
		}
		builder.append("TO ");
		if (node.getDestinationGraph() != null) {
			builder.append("GRAPH ");
			meet(node.getDestinationGraph());
		} else {
			builder.append("DEFAULT ");
		}
	}

	@Override
	public void meet(MultiProjection node) throws RuntimeException {
		Map<String, ValueExpr> valueMap = Maps.newHashMap();
		if (node.getArg() instanceof Extension) {
			Extension ext = (Extension) node.getArg();
			ext.getElements()
					.stream()
					.filter(elem -> (elem.getExpr() instanceof ValueExpr))
					.forEach(elem -> valueMap.put(elem.getName(),
							(ValueExpr) elem.getExpr()));
		}

		for (ProjectionElemList proj : node.getProjections()) {
			for (ProjectionElem elem : proj.getElements()) {
				if (valueMap.containsKey(elem.getSourceName())) {
					ValueExpr expr = valueMap.get(elem.getSourceName());
					if (expr instanceof BNodeGenerator) {
						builder.append("_:" + elem.getSourceName());
					} else {
						valueMap.get(elem.getSourceName()).visit(this);
					}
				} else {
					builder.append("?" + elem.getSourceName());
				}
				builder.append(" ");
				// elem.getSourceExpression().getExpr().visit(this);
			}
			builder.append(" . \n");
		}

		// throw new UnsupportedOperationException("Only SELECT queries are supported");
	}

	@Override
	public void meet(Namespace node) throws RuntimeException {
		super.meet(node);

	}

	@Override
	public void meet(Not node) throws RuntimeException {
		builder.append("!");
		super.meet(node);
	}

	@Override
	public void meet(Or node) throws RuntimeException {
		node.getLeftArg().visit(this);
		builder.append(" || ");
		node.getRightArg().visit(this);
	}

	@Override
	public void meet(Order node) throws RuntimeException {

		if (!node.getElements().isEmpty()) {
			builder.append("ORDER BY ");
			for (OrderElem elem : node.getElements()) {
				elem.visit(this);
			}
		}
	}

	@Override
	public void meet(OrderElem node) throws RuntimeException {
		if (!node.isAscending()) {
			builder.append("DESC(");
		}
		node.getExpr().visit(this);
		if (!node.isAscending()) {
			builder.append(")");
		}
		builder.append(" ");
	}

	@Override
	public void meet(Projection node) throws RuntimeException {
		boolean isCurrentMainProjection = false;
		if (this.currentQueryProfile instanceof SerializableParsedTupleQuery) {
			isCurrentMainProjection = node.equals(((SerializableParsedTupleQuery) this.currentQueryProfile).projection);
		} else if (this.currentQueryProfile instanceof SerializableParsedBooleanQuery) {
			isCurrentMainProjection = node
					.equals(((SerializableParsedBooleanQuery) this.currentQueryProfile).projection);
		}

		if (currentQueryProfile == null || !isCurrentMainProjection) {
			builder.append("{ ");
			this.processTupleQuery(this.queriesByProjection.get(node));
			builder.append(" } ");
		}
	}

	@Override
	public void meet(ProjectionElem node) throws RuntimeException {

		if (node.getSourceExpression() == null) {
			boolean isDescribe = false;
			if ((this.currentQueryProfile instanceof SerializableParsedConstructQuery)) {
				isDescribe = ((SerializableParsedConstructQuery) this.currentQueryProfile).describe;
			}

			if (node.getSourceName() == null || node.getTargetName().equals(node.getSourceName())) {
				if (isDescribe && this.currentQueryProfile.extensionElements.containsKey(node.getTargetName())) {
					ExtensionElem elem = this.currentQueryProfile.extensionElements.get(node.getTargetName());
					elem.getExpr().visit(this);
					builder.append(" ");
				} else {
					builder.append("?");
					builder.append(node.getTargetName());
					builder.append(" ");
				}
			} else {
				builder.append("(");
				builder.append("?");
				builder.append(node.getSourceName());
				builder.append(" ");
				builder.append("AS ");
				builder.append("?");
				builder.append(node.getTargetName());
				builder.append(" ");
				builder.append(") ");
			}
		} else {
			if (!isTautologicalExtensionElem(node.getSourceExpression())) {
				builder.append("(");
				node.getSourceExpression().visit(this);
				builder.append(") ");
			} else {
				builder.append("?");
				builder.append(node.getTargetName());
				builder.append(" ");
			}
		}
	}

	@Override
	public void meet(ProjectionElemList node) throws RuntimeException {
		super.meet(node);

	}

	@Override
	public void meet(Reduced node) throws RuntimeException {
		builder.append("REDUCED ");
		super.meet(node);

	}

	@Override
	public void meet(Regex node) throws RuntimeException {
		writeAsFunction("REGEX",
				Lists.newArrayList(node.getLeftArg(), node.getRightArg(), node.getFlagsArg()));
	}

	@Override
	public void meet(SameTerm node) throws RuntimeException {
		writeAsFunction("sameTerm", Lists.newArrayList(node.getLeftArg(), node.getRightArg()));

	}

	@Override
	public void meet(Sample node) throws RuntimeException {
		writeAsAggregationFunction("SAMPLE", node.getArg(), node.isDistinct());
	}

	@Override
	public void meet(Service node) throws RuntimeException {
		builder.append("SERVICE ");
		node.getServiceRef().visit(this);
		builder.append(" { \n");
		node.getServiceExpr().visit(this);
		builder.append("} \n");
	}

	@Override
	public void meet(SingletonSet node) throws RuntimeException {
		builder.append("{ } \n");

	}

	@Override
	public void meet(Slice node) throws RuntimeException {
		node.getArg().visit(this);
	}

	@Override
	public void meet(StatementPattern node) throws RuntimeException {
		boolean isInContext = node.getContextVar() != null;

		if (isInContext) {
			builder.append("GRAPH ");
			node.getContextVar().visit(this);
			builder.append(" { ");
		}
		builder.append("\t");
		node.getSubjectVar().visit(this);
		builder.append(" ");
		node.getPredicateVar().visit(this);
		builder.append(" ");
		node.getObjectVar().visit(this);
		builder.append(" . \n");
		if (isInContext) {
			builder.append(" } \n");
		}
	}

	@Override
	public void meet(Str node) throws RuntimeException {
		writeAsFunction("STR", node.getArg());

	}

	@Override
	public void meet(Sum node) throws RuntimeException {
		writeAsAggregationFunction("SUM", node.getArg(), node.isDistinct());
	}

	@Override
	public void meet(Union node) throws RuntimeException {
		builder.append("{\n");
		node.getLeftArg().visit(this);
		builder.append("}\n UNION\n{\n");
		node.getRightArg().visit(this);
		builder.append("}\n");
	}

	@Override
	public void meet(ValueConstant node) throws RuntimeException {
		this.meet(node.getValue());
	}

	@Override
	public void meet(Var node) throws RuntimeException {

		if (node.hasValue()) {
			this.meet(node.getValue());
		} else {
			if (node.isAnonymous()) {
				if (currentQueryProfile.extensionElements.containsKey(node.getName())) {
					ExtensionElem elem = currentQueryProfile.extensionElements.get(node.getName());
					elem.getExpr().visit(this);
				} else if (currentQueryProfile.nonAnonymousVars.containsKey(node.getName())) {
					builder.append("?");
					builder.append(node.getName());
				} else {
					builder.append("_:");
					builder.append(node.getName());
				}
			} else {
				builder.append("?");
				builder.append(node.getName());
			}
		}

		super.meet(node);

	}

	@Override
	public void meet(ZeroLengthPath node) throws RuntimeException {
		super.meet(node);

	}

//    @Override
//    public void meetOther(QueryModelNode node) throws RuntimeException {
//        if (node instanceof NaryJoin) {
//            NaryJoin joinNode = (NaryJoin)node;
//            for (TupleExpr arg : joinNode.getArgs()) {
//                arg.visit(this);
//            }
//
//        } else {
//            super.meetOther(node);
//        }
//    }

	/**
	 * A special case check: we project a variable from a subquery that has the same name We must avoid writing SELECT
	 * (?x as ?x) WHERE { { SELECT ?x WHERE { ... } } }
	 *
	 */
	private boolean isTautologicalExtensionElem(ExtensionElem val) {
		String varName = val.getName();
		if (val.getExpr() instanceof Var) {
			return (((Var) val.getExpr()).getName().equals(varName));
		}
		return false;
	}

	private void writeAsFunction(String name, ValueExpr arg) {
		builder.append(name);
		builder.append("(");
		boolean prevInsideFunction = insideFunction;
		insideFunction = true;
		arg.visit(this);
		insideFunction = prevInsideFunction;
		builder.append(") ");
	}

	private void writeAsFunction(String name, List<ValueExpr> args) {
		builder.append(name);
		builder.append("(");
		boolean prevInsideFunction = insideFunction;
		insideFunction = true;
		if (!args.isEmpty()) {
			args.get(0).visit(this);
			for (int i = 1; i < args.size(); i++) {
				if (args.get(i) != null) {
					builder.append(",");
					args.get(i).visit(this);
				}
			}
		}
		insideFunction = prevInsideFunction;
		builder.append(") ");
	}

	private void writeLimit(Slice node) throws RuntimeException {
		if (node.getLimit() > -1) {
			builder.append("LIMIT ");
			builder.append(node.getLimit());
			builder.append(" ");
		}
		if (node.getOffset() > 0) {
			builder.append("OFFSET ");
			builder.append(node.getOffset());
			builder.append(" ");
		}
	}

	private void writeAsAggregationFunction(String name, ValueExpr arg, boolean distinct) {
		builder.append(name);
		builder.append("(");
		if (distinct) {
			builder.append("DISTINCT ");
		}
		boolean prevInsideFunction = insideFunction;
		insideFunction = true;
		if (arg != null) {
			arg.visit(this);
		} else {
			builder.append("*");
		}
		insideFunction = prevInsideFunction;
		builder.append(") ");
	}

	protected String getFunctionNameAsString(FunctionCall expr) {
		String uri = expr.getURI();
		if (StringUtils.isEmpty(uri)) {
			return uri;
		}

		Optional<FNFunction> fnfunc = FNFunction.byUri(uri);
		if (fnfunc.isPresent()) {
			return fnfunc.get().getName();
		} else if (NonIriFunctions.contains(uri)) {
			return uri;
		} else {
			return "<" + uri + ">";
		}
	}

}
