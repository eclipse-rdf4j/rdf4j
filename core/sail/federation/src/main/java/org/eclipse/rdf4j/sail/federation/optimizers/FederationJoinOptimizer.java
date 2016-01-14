/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.federation.optimizers;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.algebra.AbstractQueryModelNode;
import org.eclipse.rdf4j.query.algebra.EmptySet;
import org.eclipse.rdf4j.query.algebra.LeftJoin;
import org.eclipse.rdf4j.query.algebra.QueryModelNode;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.UnaryTupleOperator;
import org.eclipse.rdf4j.query.algebra.Union;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryOptimizer;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.sail.federation.PrefixHashSet;
import org.eclipse.rdf4j.sail.federation.algebra.NaryJoin;
import org.eclipse.rdf4j.sail.federation.algebra.OwnedTupleExpr;

/**
 * Search for Join, LeftJoin, and Union arguments that can be evaluated in a
 * single member.
 * 
 * @author James Leigh
 */
public class FederationJoinOptimizer extends AbstractQueryModelVisitor<RepositoryException> implements
		QueryOptimizer
{

	private final Collection<? extends RepositoryConnection> members;

	private Map<Resource, List<RepositoryConnection>> contextToMemberMap;

	private final PrefixHashSet localSpace;

	private final boolean distinct;

	private Dataset dataset;

	public FederationJoinOptimizer(Collection<? extends RepositoryConnection> members, boolean distinct,
			PrefixHashSet localSpace)
	{
		super();
		this.members = members;
		this.localSpace = localSpace;
		this.distinct = distinct;
	}

	private static Map<Resource, List<RepositoryConnection>> createContextToMemberMap(
			Collection<? extends RepositoryConnection> members)
		throws RepositoryException
	{
		Map<Resource, List<RepositoryConnection>> contextToMemberMap = new HashMap<Resource, List<RepositoryConnection>>(
				members.size() + 1);
		for (RepositoryConnection member : members) {
			RepositoryResult<Resource> res = member.getContextIDs();
			try {
				while (res.hasNext()) {
					Resource ctx = res.next();
					List<RepositoryConnection> contextMembers = contextToMemberMap.get(ctx);
					if (contextMembers == null) {
						contextMembers = new ArrayList<RepositoryConnection>();
						contextToMemberMap.put(ctx, contextMembers);
					}
					contextMembers.add(member);
				}
			}
			finally {
				res.close();
			}
		}
		return contextToMemberMap;
	}

	@Override
	public void optimize(TupleExpr query, Dataset dataset, BindingSet bindings) {
		this.dataset = dataset;
		try {
			query.visit(this);
		}
		catch (RepositoryException e) {
			throw new UndeclaredThrowableException(e);
		}
		finally {
			this.dataset = null;
		}
	}

	@Override
	public void meetOther(QueryModelNode node)
		throws RepositoryException
	{
		if (node instanceof NaryJoin) {
			meetMultiJoin((NaryJoin)node);
		}
		else {
			super.meetOther(node);
		}
	}

	public void meetMultiJoin(NaryJoin node)
		throws RepositoryException
	{
		super.meetOther(node);
		List<Owned<NaryJoin>> ows = new ArrayList<Owned<NaryJoin>>();
		List<LocalJoin> vars = new ArrayList<LocalJoin>();
		for (TupleExpr arg : node.getArgs()) {
			RepositoryConnection member = getSingleOwner(arg);
			if ((!ows.isEmpty()) && ows.get(ows.size() - 1).getOwner() == member) {
				ows.get(ows.size() - 1).getOperation().addArg(arg.clone());
			}
			else {
				ows.add(new Owned<NaryJoin>(member, new NaryJoin(arg.clone()))); // NOPMD
			}
		}
		for (TupleExpr arg : node.getArgs()) {
			Var subj = getLocalSubject(arg);
			LocalJoin local = findLocalJoin(subj, vars);
			if (local == null) {
				vars.add(new LocalJoin(subj, new NaryJoin(arg.clone()))); // NOPMD
			}
			else {
				local.getJoin().addArg(arg.clone());
			}
		}
		addOwners(node, ows, vars);
	}

	@Override
	public void meet(LeftJoin node)
		throws RepositoryException
	{
		super.meet(node);
		Var leftSubject = getLocalSubject(node.getLeftArg());
		Var rightSubject = getLocalSubject(node.getRightArg());
		// if local then left and right can be combined
		boolean local = leftSubject != null && leftSubject.equals(rightSubject);
		RepositoryConnection leftOwner = getSingleOwner(node.getLeftArg());
		RepositoryConnection rightOwner = getSingleOwner(node.getRightArg());
		addOwners(node, leftOwner, rightOwner, local);
	}

	@Override
	public void meet(Union node)
		throws RepositoryException
	{
		super.meet(node);
		List<Owned<TupleExpr>> ows = new ArrayList<Owned<TupleExpr>>();
		for (TupleExpr arg : new TupleExpr[] { node.getLeftArg(), // NOPMD
				node.getRightArg() })
		{
			RepositoryConnection member = getSingleOwner(arg);
			int idx = ows.size() - 1;
			if ((!ows.isEmpty()) && ows.get(idx).getOwner() == member) {
				TupleExpr union = ows.get(idx).getOperation();
				union = new Union(union, arg.clone()); // NOPMD
				ows.get(idx).setOperation(union);
			}
			else {
				ows.add(new Owned<TupleExpr>(member, arg.clone())); // NOPMD
			}
		}
		addOwners(node, ows);
	}

	@Override
	protected void meetUnaryTupleOperator(UnaryTupleOperator node)
		throws RepositoryException
	{
		super.meetUnaryTupleOperator(node);
		RepositoryConnection owner = getSingleOwner(node.getArg());
		if (owner != null) {
			node.replaceWith(new OwnedTupleExpr(owner, node.clone()));
		}
	}

	private static class Owned<O> {

		private final RepositoryConnection owner;

		private O operation;

		public Owned(RepositoryConnection owner, O operation) {
			this.owner = owner;
			this.operation = operation;
		}

		public RepositoryConnection getOwner() {
			return owner;
		}

		public O getOperation() {
			return operation;
		}

		public void setOperation(O operation) {
			this.operation = operation;
		}

		@Override
		public String toString() {
			return owner + "=" + operation;
		}
	}

	private static class LocalJoin {

		private final Var var;

		private final NaryJoin join;

		public LocalJoin(Var key, NaryJoin value) {
			this.var = key;
			this.join = value;
		}

		public Var getVar() {
			return var;
		}

		public NaryJoin getJoin() {
			return join;
		}

		@Override
		public String toString() {
			return var + "=" + join;
		}
	}

	private class OwnerScanner extends AbstractQueryModelVisitor<RepositoryException> {

		private boolean shared;

		private RepositoryConnection owner;

		/**
		 * If the argument can be sent to a single member.
		 */
		public RepositoryConnection getSingleOwner(TupleExpr arg)
			throws RepositoryException
		{
			boolean pre_shared = shared;
			RepositoryConnection pre_owner = owner;
			try {
				shared = false;
				owner = null; // NOPMD
				arg.visit(this);
				return owner;
			}
			finally {
				// restore
				shared = pre_shared;
				owner = pre_owner;
			}
		}

		@Override
		public void meet(StatementPattern node)
			throws RepositoryException
		{
			super.meet(node);
			Resource subj = (Resource)node.getSubjectVar().getValue();
			IRI pred = (IRI)node.getPredicateVar().getValue();
			Value obj = node.getObjectVar().getValue();
			Resource[] ctx = getContexts(node.getContextVar());
			RepositoryConnection member = getSingleOwner(subj, pred, obj, ctx);
			if (member == null) {
				multipleOwners();
			}
			else {
				usedBy(member);
			}
		}

		@Override
		public void meetOther(QueryModelNode node)
			throws RepositoryException
		{
			if (node instanceof OwnedTupleExpr) {
				meetOwnedTupleExpr((OwnedTupleExpr)node);
			}
			else {
				super.meetOther(node);
			}
		}

		private void meetOwnedTupleExpr(OwnedTupleExpr node)
			throws RepositoryException
		{
			usedBy(node.getOwner());
		}

		private Resource[] getContexts(Var var) {
			return (var == null || !var.hasValue()) ? new Resource[0]
					: new Resource[] { (Resource)var.getValue() };
		}

		private RepositoryConnection getSingleOwner(Resource subj, IRI pred, Value obj, Resource[] ctx)
			throws RepositoryException
		{
			RepositoryConnection result = null;

			// Avoid querying repositories if given a set of explicit contexts only belonging to one federation member
			if(contextToMemberMap == null) {
				contextToMemberMap = createContextToMemberMap(members);
			}
			Set<RepositoryConnection> results = new HashSet<RepositoryConnection>();
			Collection<Resource> explicitContexts;
			if(ctx.length > 0) {
				explicitContexts = Arrays.asList(ctx);
			}
			else if(dataset != null) {
				// all graphs
				explicitContexts = new ArrayList<Resource>();
				explicitContexts.addAll(dataset.getDefaultGraphs());
				explicitContexts.addAll(dataset.getNamedGraphs());
			}
			else {
				explicitContexts = Collections.<Resource>emptyList();
			}
			for(Resource context : explicitContexts) {
				List<RepositoryConnection> contextRepos = contextToMemberMap.get(context);
				if(contextRepos != null) {
					results.addAll(contextRepos);
				}
			}
			if(results.size() == 1) {
				result = results.iterator().next();
			}
			else {
				// fallback to using hasStatement()
				// but hopefully we narrowed it down to results
				for (RepositoryConnection member : results) {
					if (member.hasStatement(subj, pred, obj, true, ctx)) {
						if (result == null) {
							result = member;
						}
						else if (result != member) {
							result = null; // NOPMD
							break;
						}
					}
				}
			}
			return result;
		}

		private void usedBy(RepositoryConnection member) {
			if (!shared && owner == null) {
				// first owner sensitive element
				owner = member;
			}
			else if (owner != member) { // NOPMD
				multipleOwners();
			}
		}

		private void multipleOwners() {
			owner = null; // NOPMD
			shared = true;
		}

	}

	private class LocalScanner extends AbstractQueryModelVisitor<RepositoryException> {

		private boolean isLocal;

		private Var relative;

		/**
		 * If the argument can be sent as a group to the members.
		 */
		public Var getLocalSubject(TupleExpr arg)
			throws RepositoryException
		{
			boolean local_stack = isLocal;
			Var relative_stack = relative;
			try {
				isLocal = true;
				relative = null; // NOPMD
				arg.visit(this);
				return relative;
			}
			finally {
				// restore
				isLocal = local_stack;
				relative = relative_stack;
			}
		}

		@Override
		public void meet(StatementPattern node)
			throws RepositoryException
		{
			super.meet(node);
			IRI pred = (IRI)node.getPredicateVar().getValue();
			if (pred != null && localSpace != null && localSpace.match(pred.stringValue())) {
				local(node.getSubjectVar());
			}
			else {
				notLocal();
			}
		}

		private void local(Var subj) {
			if (isLocal && relative == null) {
				relative = subj;
			}
			else if (!subj.equals(relative)) {
				notLocal();
			}
		}

		private void notLocal() {
			isLocal = false;
			relative = null; // NOPMD
		}

	}

	/**
	 * If two basic graph patterns have the same subject and can be run on the
	 * same member, we can change the order.
	 */
	private LocalJoin findLocalJoin(Var subj, List<LocalJoin> vars) {
		LocalJoin result = null;
		if ((!vars.isEmpty()) && vars.get(vars.size() - 1).getVar() == subj) {
			result = vars.get(vars.size() - 1);
		}
		else {
			for (LocalJoin local : vars) {
				if (subj != null && subj.equals(local.getVar())) {
					result = local;
					break;
				}
			}
		}
		return result;
	}

	/**
	 * If the argument can be sent to a single member.
	 */
	private RepositoryConnection getSingleOwner(TupleExpr arg)
		throws RepositoryException
	{
		return new OwnerScanner().getSingleOwner(arg);
	}

	/**
	 * If the argument can be sent as a group to the members.
	 */
	private Var getLocalSubject(TupleExpr arg)
		throws RepositoryException
	{
		return new LocalScanner().getLocalSubject(arg);
	}

	private void addOwners(NaryJoin node, List<Owned<NaryJoin>> ows, List<LocalJoin> vars)
		throws RepositoryException
	{
		boolean local = isLocal(vars);
		if (ows.size() == 1) {
			RepositoryConnection owner = ows.get(0).getOwner();
			if (owner == null) {
				// every element has multiple owners
				if (local) {
					performReplacementsInNode(node, vars);
				}
			}
			else {
				// every element is used by the same owner
				node.replaceWith(new OwnedTupleExpr(owner, node.clone()));
			}
		}
		else if (local) {
			AbstractQueryModelNode replacement = new NaryJoin();
			for (LocalJoin v : vars) {
				Var var = v.getVar();
				NaryJoin join = v.getJoin();
				if (var == null) {
					// each of these arguments could be anywhere
					for (TupleExpr expr : join.getArgs()) {
						((NaryJoin)replacement).addArg(expr);
					}
				}
				else {
					replacement = optimizeReplacementJoin(replacement, join);
				}
			}
			node.replaceWith(replacement);
		}
		else {
			NaryJoin replacement = generateReplacementFromOwnedJoins(ows);
			node.replaceWith(replacement);
		}
	}

	private NaryJoin generateReplacementFromOwnedJoins(List<Owned<NaryJoin>> ows) {
		NaryJoin replacement = new NaryJoin();
		for (Owned<NaryJoin> e : ows) {
			RepositoryConnection owner = e.getOwner();
			NaryJoin join = e.getOperation();
			if (owner == null) {
				// multiple owners
				for (TupleExpr arg : join.getArgs()) {
					replacement.addArg(arg);
				}
			}
			else {
				replacement.addArg(new OwnedTupleExpr(owner, join)); // NOPMD
			}
		}
		return replacement;
	}

	private AbstractQueryModelNode optimizeReplacementJoin(AbstractQueryModelNode candidate, NaryJoin join)
		throws RepositoryException
	{
		boolean multipleOwners = false;
		RepositoryConnection owner = null;
		for (TupleExpr expr : join.getArgs()) {
			RepositoryConnection connection = getSingleOwner(expr);
			if (owner == null) {
				owner = connection;
			}
			else if (connection != null && owner != connection) { // NOPMD
				multipleOwners = true;
				owner = null; // NOPMD
				break;
			}
		}
		AbstractQueryModelNode replacement = candidate;
		if (multipleOwners) {
			// no subject exists on multiple members,
			// but in the same member
			replacement = new EmptySet(); // NOPMD
		}
		else if (owner == null) {
			// these arguments might exist in any member,
			// but they will only join with the same member.
			addUnionOfMembers((NaryJoin)replacement, join);
		}
		else {
			// there is only one member that can join all these
			// arguments
			addArg((NaryJoin)replacement, new OwnedTupleExpr(owner, join)); // NOPMD
		}
		return replacement;
	}

	private void addUnionOfMembers(NaryJoin replacement, NaryJoin join) {
		TupleExpr union = null;
		for (RepositoryConnection member : members) {
			OwnedTupleExpr arg = new OwnedTupleExpr(member, join.clone()); // NOPMD
			union = union == null ? arg : new Union(union, arg); // NOPMD
		}
		if (union != null) {
			replacement.addArg(union);
		}
	}

	private void performReplacementsInNode(NaryJoin node, List<LocalJoin> vars) {
		NaryJoin replacement = new NaryJoin();
		for (LocalJoin e : vars) {
			if (distinct || e.getVar() != null) {
				TupleExpr union = null;
				for (RepositoryConnection member : members) {
					TupleExpr arg = new OwnedTupleExpr(member, e.getJoin() // NOPMD
					.clone());
					union = union == null ? arg : new Union(union, arg); // NOPMD
				}
				if (union != null) {
					replacement.addArg(union);
				}
			}
			else {
				for (TupleExpr expr : e.getJoin().getArgs()) {
					replacement.addArg(expr);
				}
			}
		}
		node.replaceWith(replacement);
	}

	private boolean isLocal(List<LocalJoin> vars) {
		boolean local = false;
		if (vars.size() > 1 || vars.size() == 1 && vars.get(0).getVar() != null) {
			for (LocalJoin e : vars) {
				if (e.getVar() != null && e.getJoin().getNumberOfArguments() > 1) {
					local = true;
					break;
				}
			}
		}
		return local;
	}

	private void addArg(NaryJoin destination, OwnedTupleExpr newArg) {
		boolean found = false;
		// if the last owner is the same then combine
		int size = destination.getNumberOfArguments();
		if (size > 0) {
			TupleExpr expr = destination.getArg(size - 1);
			if (expr instanceof OwnedTupleExpr) {
				OwnedTupleExpr existing = (OwnedTupleExpr)expr;
				boolean sameOwner = newArg.getOwner() == existing.getOwner();
				if (sameOwner && existing.getArg() instanceof NaryJoin) {
					// recently added this owner
					NaryJoin existingJoin = (NaryJoin)existing.getArg();
					NaryJoin newJoin = (NaryJoin)newArg.getArg();
					for (TupleExpr t : newJoin.getArgs()) {
						existingJoin.addArg(t);
					}
					found = true;
				}
			}
		}
		if (!found) {
			destination.addArg(newArg);
		}
	}

	private void addOwners(LeftJoin node, RepositoryConnection leftOwner, RepositoryConnection rightOwner,
			boolean local)
	{
		if (leftOwner == null && rightOwner == null) {
			if (local) {
				TupleExpr union = null;
				for (RepositoryConnection member : members) {
					OwnedTupleExpr arg = new OwnedTupleExpr(member, // NOPMD
							node.clone());
					union = union == null ? arg : new Union(union, arg); // NOPMD
				}
				node.replaceWith(union);
			}
		}
		else if (leftOwner == rightOwner) { // NOPMD
			node.replaceWith(new OwnedTupleExpr(leftOwner, node.clone()));
		}
		else {
			if (local) {
				addDistinctOwnersLocal(node, leftOwner, rightOwner);
			}
			else {
				addDistinctOwnersNonLocal(node, leftOwner, rightOwner);
			}
		}
	}

	private void addDistinctOwnersNonLocal(LeftJoin node, RepositoryConnection leftOwner,
			RepositoryConnection rightOwner)
	{
		if (leftOwner != null) {
			node.getLeftArg().replaceWith(new OwnedTupleExpr(leftOwner, node.getLeftArg().clone()));
		}
		if (rightOwner != null) {
			node.getRightArg().replaceWith(new OwnedTupleExpr(rightOwner, node.getRightArg().clone()));
		}
	}

	private void addDistinctOwnersLocal(LeftJoin node, RepositoryConnection leftOwner,
			RepositoryConnection rightOwner)
	{
		if (rightOwner == null) {
			node.replaceWith(new OwnedTupleExpr(leftOwner, node.clone()));
		}
		else if (leftOwner == null) {
			TupleExpr union = null;
			for (RepositoryConnection member : members) {
				if (rightOwner == member) {
					OwnedTupleExpr arg = new OwnedTupleExpr(member, // NOPMD
							node.clone());
					union = union == null ? arg : new Union(union, arg); // NOPMD
				}
				else {
					OwnedTupleExpr arg = new OwnedTupleExpr(member, // NOPMD
							node.getLeftArg().clone());
					union = union == null ? arg : new Union(union, arg); // NOPMD
				}
			}
			node.replaceWith(union);
		}
		else {
			node.replaceWith(new OwnedTupleExpr(leftOwner, node.getLeftArg().clone()));
		}
	}

	private void addOwners(Union node, List<Owned<TupleExpr>> ows) {
		if (ows.size() == 1) {
			RepositoryConnection owner = ows.get(0).getOwner();
			if (owner != null) {
				// every element is used by the same owner
				node.replaceWith(new OwnedTupleExpr(owner, node.clone()));
			}
		}
		else {
			TupleExpr replacement = null;
			for (Owned<TupleExpr> e : ows) {
				RepositoryConnection owner = e.getOwner();
				TupleExpr union = e.getOperation();
				if (owner == null) {
					// multiple owners
					for (TupleExpr arg : getUnionArgs(union)) {
						replacement = replacement == null ? arg.clone() : new Union(replacement, arg.clone()); // NOPMD
					}
				}
				else {
					OwnedTupleExpr arg = new OwnedTupleExpr(owner, union); // NOPMD
					replacement = replacement == null ? arg : new Union( // NOPMD
							replacement, arg);
				}
			}
			node.replaceWith(replacement);
		}
	}

	private List<TupleExpr> getUnionArgs(TupleExpr union) {
		return getUnionArgs(union, new ArrayList<TupleExpr>());
	}

	private List<TupleExpr> getUnionArgs(TupleExpr union, List<TupleExpr> list) {
		if (union instanceof Union) {
			getUnionArgs(((Union)union).getLeftArg(), list);
			getUnionArgs(((Union)union).getLeftArg(), list);
		}
		else {
			list.add(union);
		}
		return list;
	}

}
