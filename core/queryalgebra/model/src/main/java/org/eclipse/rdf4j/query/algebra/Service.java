/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;

/**
 * The SERVICE keyword as defined in <a href="http://www.w3.org/TR/sparql11-federated- query/#defn_service" >SERVICE
 * definition</a>. The service expression is evaluated at the specified service URI. If the service reference is a
 * variable, a value for this variable must be available at evaluation time (e.g. from earlier computations).
 * 
 * @author Andreas Schwarte
 */
public class Service extends UnaryTupleOperator {

	/*-----------*
	 * Variables *
	 *-----------*/

	private Var serviceRef;

	/* a string representation of the inner expression (e.g. extracted during parsing) */
	private String serviceExpressionString;

	private Set<String> serviceVars;

	/* the prefix declarations, potentially null */
	private Map<String, String> prefixDeclarations;

	private String baseURI;

	/*
	 * prepared queries, including prefix. Contains %PROJECTION_VARS% to be replaced at evaluation time. see
	 */
	private String preparedSelectQueryString;

	private String preparedAskQueryString;

	private boolean silent;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public Service(Var serviceRef, TupleExpr serviceExpr, String serviceExpressionString,
			Map<String, String> prefixDeclarations, String baseURI, boolean silent) {
		super(serviceExpr);
		setServiceRef(serviceRef);
		setExpressionString(serviceExpressionString);
		this.serviceVars = computeServiceVars(serviceExpr);
		setPrefixDeclarations(prefixDeclarations);
		setBaseURI(baseURI);
		initPreparedQueryString();
		this.silent = silent;
	}

	/*---------*
	 * Methods *
	 *---------*/

	public Var getServiceRef() {
		return this.serviceRef;
	}

	public TupleExpr getServiceExpr() {
		return this.arg;
	}

	public void setServiceRef(Var serviceRef) {
		this.serviceRef = serviceRef;
	}

	/**
	 * @return Returns the silent.
	 */
	public boolean isSilent() {
		return silent;
	}

	/**
	 * @return Returns the prefixDeclarations.
	 */
	public Map<String, String> getPrefixDeclarations() {
		return prefixDeclarations;
	}

	/**
	 * @param prefixDeclarations The prefixDeclarations to set.
	 */
	public void setPrefixDeclarations(Map<String, String> prefixDeclarations) {
		this.prefixDeclarations = prefixDeclarations;
	}

	/**
	 * The SERVICE expression, either complete or just the expression e.g. "SERVICE <url> { ... }" becomes " ... "
	 * 
	 * @param serviceExpressionString the inner expression as SPARQL String representation
	 */
	public void setExpressionString(String serviceExpressionString) {
		this.serviceExpressionString = parseServiceExpression(serviceExpressionString);
	}

	/**
	 * @return Returns the serviceExpressionString.
	 */
	public String getServiceExpressionString() {
		return serviceExpressionString;
	}

	/**
	 * Returns an ASK query string using no projection vars.
	 * 
	 * @return an ASK query string
	 */
	public String getAskQueryString() {
		return preparedAskQueryString;
	}

	/**
	 * Returns a SELECT query string using the provided projection vars. The variables are inserted into the
	 * preparedSelectQueryString in the SELECT clause.
	 * 
	 * @param projectionVars
	 * @return SELECT query string, utilizing the given projection variables
	 */
	public String getSelectQueryString(Set<String> projectionVars) {
		if (projectionVars.isEmpty())
			return preparedSelectQueryString.replace("%PROJECTION_VARS%", "*");
		StringBuilder sb = new StringBuilder();
		for (String var : projectionVars)
			sb.append(" ?").append(var);
		return preparedSelectQueryString.replace("%PROJECTION_VARS%", sb.toString());
	}

	/**
	 * @return Returns the serviceVars.
	 */
	public Set<String> getServiceVars() {
		return serviceVars;
	}

	@Override
	public <X extends Exception> void visit(QueryModelVisitor<X> visitor) throws X {
		visitor.meet(this);
	}

	@Override
	public <X extends Exception> void visitChildren(QueryModelVisitor<X> visitor) throws X {
		serviceRef.visit(visitor);
		super.visitChildren(visitor);
	}

	@Override
	public void replaceChildNode(QueryModelNode current, QueryModelNode replacement) {
		if (serviceRef == current) {
			setServiceRef((Var) replacement);
		} else {
			super.replaceChildNode(current, replacement);
		}
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof Service && super.equals(other)) {
			Service o = (Service) other;
			return serviceRef.equals(o.getServiceRef());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ serviceRef.hashCode();
	}

	@Override
	public Service clone() {
		Service clone = (Service) super.clone();
		clone.setServiceRef(serviceRef.clone());
		return clone;
	}

	/**
	 * Compute the variable names occurring in the service expression using tree traversal, since these are necessary
	 * for building the SPARQL query.
	 * 
	 * @return the set of variable names in the given service expression
	 */
	private Set<String> computeServiceVars(TupleExpr serviceExpression) {
		final Set<String> res = new HashSet<>();
		serviceExpression.visit(new AbstractQueryModelVisitor<RuntimeException>() {

			@Override
			public void meet(Var node) throws RuntimeException {
				// take only real vars, i.e. ignore blank nodes
				if (!node.hasValue() && !node.isAnonymous()) {
					res.add(node.getName());
				}
			}

			@Override
			public void meet(BindingSetAssignment bsa) {
				res.addAll(bsa.getAssuredBindingNames());
			}

			@Override
			public void meet(Extension e) {
				super.meet(e);
				for (ExtensionElem elem : e.getElements()) {
					res.add(elem.getName());
				}
			}
			// TODO maybe stop tree traversal in nested SERVICE?
		});
		return res;
	}

	private static Pattern subselectPattern = Pattern.compile("SELECT.*", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

	private void initPreparedQueryString() {

		serviceExpressionString = serviceExpressionString.trim();
		String prefixString = computePrefixString(prefixDeclarations);

		// build the raw SELECT query string
		StringBuilder sb = new StringBuilder();
		sb.append(prefixString);
		if (subselectPattern.matcher(serviceExpressionString).matches()) {
			sb.append(serviceExpressionString);
		} else {
			sb.append("SELECT %PROJECTION_VARS% WHERE { ");
			sb.append(serviceExpressionString);
			sb.append("\n}");
		}
		preparedSelectQueryString = sb.toString();

		// build the raw ASK query string
		sb = new StringBuilder();
		sb.append(prefixString);
		sb.append("ASK {");
		sb.append(serviceExpressionString);
		sb.append(" }");
		preparedAskQueryString = sb.toString();
	}

	/**
	 * Compute the prefix string only once to avoid computation overhead during evaluation.
	 * 
	 * @param prefixDeclarations
	 * @return a Prefix String or an empty string if there are no prefixes
	 */
	private String computePrefixString(Map<String, String> prefixDeclarations) {
		if (prefixDeclarations == null)
			return "";

		StringBuilder sb = new StringBuilder();
		for (String prefix : prefixDeclarations.keySet()) {
			String uri = prefixDeclarations.get(prefix);
			sb.append("PREFIX ").append(prefix).append(":").append(" <").append(uri).append("> ");
		}
		return sb.toString();
	}

	/**
	 * Parses a service expression to just have the inner expression, e.g. from something like "SERVICE &lt;url&gt; {
	 * ... }" becomes " ... ", also applies {@link String#trim()} to remove leading/tailing space
	 * 
	 * @param serviceExpression
	 * @return the inner expression of the given service expression
	 */
	private String parseServiceExpression(String serviceExpression) {

		if (serviceExpression.toLowerCase().startsWith("service")) {
			return serviceExpression.substring(serviceExpression.indexOf('{') + 1, serviceExpression.lastIndexOf('}'))
					.trim();
		}
		return serviceExpression;
	}

	/**
	 * @param baseURI The baseURI to set.
	 */
	public void setBaseURI(String baseURI) {
		this.baseURI = baseURI;
	}

	/**
	 * @return Returns the baseURI.
	 */
	public String getBaseURI() {
		return baseURI;
	}
}
