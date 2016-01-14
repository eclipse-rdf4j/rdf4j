/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.federation.optimizers;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.Literals;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.algebra.Distinct;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.LeftJoin;
import org.eclipse.rdf4j.query.algebra.Projection;
import org.eclipse.rdf4j.query.algebra.ProjectionElem;
import org.eclipse.rdf4j.query.algebra.ProjectionElemList;
import org.eclipse.rdf4j.query.algebra.QueryModelNode;
import org.eclipse.rdf4j.query.algebra.Reduced;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.StatementPattern.Scope;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryOptimizer;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.turtle.TurtleUtil;
import org.eclipse.rdf4j.sail.federation.algebra.NaryJoin;
import org.eclipse.rdf4j.sail.federation.algebra.OwnedTupleExpr;

/**
 * Remove redundant {@link OwnedTupleExpr}.
 * 
 * @author James Leigh
 */
public class PrepareOwnedTupleExpr extends AbstractQueryModelVisitor<RepositoryException> implements
		QueryOptimizer
{

	private static final String END_BLOCK = "}\n";

	private OwnedTupleExpr owner;

	private String pattern;

	private TupleExpr patternNode;

	/** local name to sparql name */
	private Map<String, String> variables = new HashMap<String, String>();

	private boolean reduce;

	private boolean reduced;

	private boolean distinct;

	public void optimize(TupleExpr query, Dataset dataset, BindingSet bindings) {
		try {
			query.visit(this);
		}
		catch (RepositoryException e) {
			throw new UndeclaredThrowableException(e);
		}
	}

	@Override
	public void meetOther(QueryModelNode node)
		throws RepositoryException
	{
		if (node instanceof OwnedTupleExpr) {
			meetOwnedTupleExpr((OwnedTupleExpr)node);
		}
		else if (node instanceof NaryJoin) {
			meetMultiJoin((NaryJoin)node);
		}
		else {
			super.meetOther(node);
		}
	}

	private void meetOwnedTupleExpr(OwnedTupleExpr node)
		throws RepositoryException
	{
		OwnedTupleExpr before = this.owner;
		try {
			this.owner = node;
			meetNode(node);
			this.owner = null; // NOPMD
		}
		finally {
			this.owner = before;
		}
	}

	@Override
	protected void meetNode(QueryModelNode node)
		throws RepositoryException
	{
		super.meetNode(node);
		if (owner != null && patternNode != null && !(patternNode instanceof StatementPattern)) {
			StringBuilder builder = new StringBuilder();
			builder.append("SELECT");
			if (distinct) {
				builder.append(" DISTINCT");
			}
			else if (reduced || reduce) {
				builder.append(" REDUCED");
			}
			boolean mapping = false;
			Map<String, String> bindings = new HashMap<String, String>();
			ProjectionElemList list = new ProjectionElemList();
			for (String name : patternNode.getBindingNames()) {
				mapping = addBindingNames(builder, mapping, bindings, list, name);
			}
			builder.append("\nWHERE {\n").append(pattern).append("}");
			meetNodeLocal(builder, mapping, bindings, list);
		}
		reduced = false;
		distinct = false;
		pattern = null; // NOPMD
		patternNode = null; // NOPMD
	}

	private void meetNodeLocal(StringBuilder builder, boolean mapping, Map<String, String> bindings,
			ProjectionElemList list)
		throws RepositoryException, AssertionError
	{
		try {
			QueryModelNode parent = patternNode.getParentNode();
			if (parent instanceof OwnedTupleExpr) {
				OwnedTupleExpr owned = (OwnedTupleExpr)parent;
				meetNodeLocalParentOwned(builder, mapping, bindings, list, owned);
			}
			else {
				meetNodeLocalParentNotOwned(builder, mapping, bindings, list);
			}
		}
		catch (MalformedQueryException e) {
			throw new AssertionError(e);
		}
	}

	private void meetNodeLocalParentOwned(StringBuilder builder, boolean mapping,
			Map<String, String> bindings, ProjectionElemList list, OwnedTupleExpr owned)
		throws RepositoryException, MalformedQueryException
	{
		owned.prepare(QueryLanguage.SPARQL, builder.toString(), bindings);
		if (mapping) {
			Projection proj = new Projection(owned.clone(), list);
			owned.replaceWith(proj);
		}
	}

	private void meetNodeLocalParentNotOwned(StringBuilder builder, boolean mapping,
			Map<String, String> bindings, ProjectionElemList list)
		throws RepositoryException, MalformedQueryException
	{
		OwnedTupleExpr owned = new OwnedTupleExpr(owner.getOwner(), patternNode.clone());
		owned.prepare(QueryLanguage.SPARQL, builder.toString(), bindings);
		if (mapping) {
			Projection proj = new Projection(owned, list);
			patternNode.replaceWith(proj);
		}
		else {
			patternNode.replaceWith(owned);
		}
	}

	private boolean addBindingNames(StringBuilder builder, boolean alreadyMapping,
			Map<String, String> bindings, ProjectionElemList list, String name)
	{
		boolean mapping = alreadyMapping;
		if (variables.containsKey(name)) {
			String var = variables.get(name);
			builder.append(" ?").append(var);
			bindings.put(name, var);
			list.addElement(new ProjectionElem(var, name));
			if (!name.equals(var)) {
				mapping = true;
			}
		}
		return mapping;
	}

	@Override
	public void meet(Distinct node)
		throws RepositoryException
	{
		boolean before = reduce;
		try {
			reduce = true;
			node.getArg().visit(this);
		}
		finally {
			reduce = before;
		}
		if (patternNode == null) {
			return;
		}
		this.distinct = true;
		this.patternNode = node;
	}

	@Override
	public void meet(Reduced node)
		throws RepositoryException
	{
		boolean before = reduce;
		try {
			reduce = true;
			node.getArg().visit(this);
		}
		finally {
			reduce = before;
		}
		if (patternNode == null) {
			return;
		}
		this.reduced = true;
		this.patternNode = node;
	}

	@Override
	public void meet(Projection node)
		throws RepositoryException
	{
		TupleExpr arg = node.getArg();
		if (arg instanceof StatementPattern && arg.getBindingNames().equals(node.getBindingNames())) {
			meetNode(node);
		}
		else {
			arg.visit(this);
			if (patternNode == null) {
				return;
			}
			Map<String, String> map = new HashMap<String, String>();
			for (ProjectionElem e : node.getProjectionElemList().getElements()) {
				String source = variables.get(e.getSourceName());
				if (source == null) {
					source = safe(e.getSourceName());
				}
				map.put(e.getTargetName(), source);
			}
			this.variables = map;
			this.patternNode = node;
		}
	}

	@Override
	public void meet(LeftJoin node)
		throws RepositoryException
	{
		if (node.getCondition() == null) {
			Map<String, String> vars = new HashMap<String, String>();
			StringBuilder builder = new StringBuilder();
			node.getLeftArg().visit(this);
			if (patternNode != null) {
				builder.append(pattern);
				vars.putAll(variables);
				node.getRightArg().visit(this);
				if (patternNode != null) {
					builder.append("OPTIONAL {").append(pattern).append(END_BLOCK);
					vars.putAll(variables);
					this.variables = vars;
					this.pattern = builder.toString();
					this.patternNode = node;
				}
			}
		}
		else {
			super.meet(node);
		}
	}

	public void meetMultiJoin(NaryJoin node)
		throws RepositoryException
	{
		Map<String, String> vars = new HashMap<String, String>();
		StringBuilder builder = new StringBuilder();
		for (TupleExpr arg : node.getArgs()) {
			arg.visit(this);
			if (patternNode == null && owner != null) {
				return; // unsupported operation
			}
			else if (patternNode == null) {
				// no owner
				builder = null; // NOPMD
			}
			else if (builder != null) {
				builder.append("{").append(pattern).append(END_BLOCK);
				vars.putAll(variables);
			}
		}
		if (builder != null) {
			this.variables = vars;
			this.pattern = builder.toString();
			this.patternNode = node;
		}
	}

	@Override
	public void meet(Join node)
		throws RepositoryException
	{
		Map<String, String> vars = new HashMap<String, String>();
		StringBuilder builder = new StringBuilder();
		node.getLeftArg().visit(this);
		if (patternNode != null) {
			builder.append("{").append(pattern).append(END_BLOCK);
			vars.putAll(variables);
			node.getRightArg().visit(this);
			if (patternNode != null) {
				builder.append("{").append(pattern).append(END_BLOCK);
				vars.putAll(variables);
				this.variables = vars;
				this.pattern = builder.toString();
				this.patternNode = node;
			}
		}
	}

	@Override
	public void meet(StatementPattern node)
		throws RepositoryException
	{
		StringBuilder builder = new StringBuilder();
		Scope scope = node.getScope();
		Var subj = node.getSubjectVar();
		Var pred = node.getPredicateVar();
		Var obj = node.getObjectVar();
		Var ctx = node.getContextVar();
		boolean cokay = ctx == null && scope.equals(Scope.DEFAULT_CONTEXTS) || ctx != null
				&& scope.equals(Scope.NAMED_CONTEXTS);
		boolean sokay = !subj.hasValue() || subj.isAnonymous() || subj.getValue() instanceof IRI;
		boolean ookay = !obj.hasValue() || obj.isAnonymous() || obj.getValue() instanceof IRI
				|| obj.getValue() instanceof Literal;
		if (cokay && sokay && ookay) {
			variables.clear();
			if (ctx != null) {
				builder.append("GRAPH ");
				appendVar(builder, ctx.getName());
				builder.append(" {\n");
			}
			appendVar(builder, subj);
			appendVar(builder, pred);
			appendVar(builder, obj);
			builder.append(" .");
			appendFilter(builder, subj);
			appendFilter(builder, pred);
			appendFilter(builder, obj);
			if (ctx != null) {
				if (ctx.hasValue()) {
					builder.append("\nFILTER sameTerm(");
					appendVar(builder, ctx.getName());
					builder.append(", ");
					writeValue(builder, ctx.getValue());
					builder.append(")\n");
				}
				builder.append("}");
			}
			this.pattern = builder.toString();
			this.patternNode = node;
		}
		else {
			this.patternNode = null; // NOPMD
		}
	}

	private void appendVar(StringBuilder builder, Var var) {
		if (var.hasValue() && var.isAnonymous()) {
			Value value = var.getValue();
			writeValue(builder, value);
		}
		else {
			String varName = var.getName();
			appendVar(builder, varName);
		}
		builder.append(" ");
	}

	private void appendVar(StringBuilder builder, String varName) {
		builder.append("?");
		String name = safe(varName);
		builder.append(name);
		variables.put(varName, name);
	}

	private String safe(String name) {
		return name.replace('-', '_');
	}

	private void appendFilter(StringBuilder builder, Var var) {
		if (var.hasValue() && !var.isAnonymous()) {
			builder.append("\nFILTER sameTerm(");
			appendVar(builder, var.getName());
			builder.append(", ");
			writeValue(builder, var.getValue());
			builder.append(")");
		}
	}

	private void writeValue(StringBuilder builder, Value val) {
		if (val instanceof Resource) {
			writeResource(builder, (Resource)val);
		}
		else {
			writeLiteral(builder, (Literal)val);
		}
	}

	private void writeResource(StringBuilder builder, Resource res) {
		if (res instanceof IRI) {
			writeURI(builder, (IRI)res);
		}
		else {
			writeBNode(builder, (BNode)res);
		}
	}

	private void writeURI(StringBuilder builder, IRI uri) {
		builder.append("<");
		builder.append(TurtleUtil.encodeURIString(uri.stringValue()));
		builder.append(">");
	}

	private void writeBNode(StringBuilder builder, BNode bNode) {
		builder.append("_:");
		builder.append(bNode.stringValue());
	}

	private void writeLiteral(StringBuilder builder, Literal lit) {
		String label = lit.getLabel();

		if (label.indexOf('\n') > 0 || label.indexOf('\r') > 0 || label.indexOf('\t') > 0) {
			// Write label as long string
			builder.append("\"\"\"");
			builder.append(TurtleUtil.encodeLongString(label));
			builder.append("\"\"\"");
		}
		else {
			// Write label as normal string
			builder.append("\"");
			builder.append(TurtleUtil.encodeString(label));
			builder.append("\"");
		}

		IRI datatype = lit.getDatatype();
		if (Literals.isLanguageLiteral(lit)) {
			// Append the literal's language
			builder.append("@");
			builder.append(lit.getLanguage().get());
		}
		else {
			// Append the literal's data type (possibly written as an
			// abbreviated URI)
			builder.append("^^");
			writeURI(builder, datatype);
		}
	}

}
