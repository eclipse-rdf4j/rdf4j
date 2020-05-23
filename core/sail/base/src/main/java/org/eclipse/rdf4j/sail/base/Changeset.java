/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.base;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.ModelFactory;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.sail.SailConflictException;
import org.eclipse.rdf4j.sail.SailException;

/**
 * Set of changes applied to an {@link SailSourceBranch} awaiting to be flushed into its backing {@link SailSource}.
 *
 * @author James Leigh
 */
abstract class Changeset implements SailSink, ModelFactory {

	/**
	 * Set of {@link SailDataset}s that are currently using this {@link Changeset} to derive the state of the
	 * {@link SailSource}.
	 */
	private Set<SailDatasetImpl> refbacks;

	/**
	 * {@link Changeset}s that have been {@link #flush()}ed to the same {@link SailSourceBranch}, since this object was
	 * {@link #flush()}ed.
	 */
	private Set<Changeset> prepend;

	/**
	 * When in {@link IsolationLevels#SERIALIZABLE} this contains all the observed {@link StatementPattern}s that were
	 * observed by {@link ObservingSailDataset}.
	 */
	private Set<StatementPattern> observations;

	/**
	 * Statements that have been added as part of a transaction, but has not yet been committed.
	 *
	 * DO NOT EXPOSE THE MODEL OUTSIDE OF THIS CLASS BECAUSE IT IS NOT THREAD-SAFE
	 */
	private Model approved;

	/**
	 * Explicit statements that have been removed as part of a transaction, but have not yet been committed.
	 *
	 * DO NOT EXPOSE THE MODEL OUTSIDE OF THIS CLASS BECAUSE IT IS NOT THREAD-SAFE
	 */
	private Model deprecated;

	/**
	 * Set of contexts of the {@link #approved} statements.
	 */
	private Set<Resource> approvedContexts;

	/**
	 * Set of contexts that were passed to {@link #clear(Resource...)}.
	 */
	private Set<Resource> deprecatedContexts;

	/**
	 * Additional namespaces added.
	 */
	private Map<String, String> addedNamespaces;

	/**
	 * Namespace prefixes that were removed.
	 */
	private Set<String> removedPrefixes;

	/**
	 * If all namespaces were removed, other than {@link #addedNamespaces}.
	 */
	private boolean namespaceCleared;

	/**
	 * If all statements were removed, other than {@link #approved}.
	 */
	private boolean statementCleared;

	@Override
	public void close() throws SailException {
		// no-op
	}

	@Override
	public void prepare() throws SailException {
		if (prepend != null && observations != null) {
			for (StatementPattern p : observations) {
				Resource subj = (Resource) p.getSubjectVar().getValue();
				IRI pred = (IRI) p.getPredicateVar().getValue();
				Value obj = p.getObjectVar().getValue();
				Var ctxVar = p.getContextVar();
				Resource[] contexts;
				if (ctxVar == null) {
					contexts = new Resource[0];
				} else {
					contexts = new Resource[] { (Resource) ctxVar.getValue() };
				}
				for (Changeset changeset : prepend) {
					if (changeset.hasApproved(subj, pred, obj, contexts)
							|| (changeset.hasDeprecated(subj, pred, obj, contexts))) {
						throw new SailConflictException("Observed State has Changed");
					}
				}
			}
		}
	}

	synchronized boolean hasApproved(Resource subj, IRI pred, Value obj, Resource[] contexts) {
		if (approved == null) {
			return false;
		}

		return approved.contains(subj, pred, obj, contexts);
	}

	synchronized boolean hasDeprecated(Resource subj, IRI pred, Value obj, Resource[] contexts) {
		if (deprecated == null) {
			return false;
		}

		return deprecated.contains(subj, pred, obj, contexts);
	}

	public synchronized void addRefback(SailDatasetImpl dataset) {
		if (refbacks == null) {
			refbacks = new HashSet<>();
		}
		refbacks.add(dataset);
	}

	public synchronized void removeRefback(SailDatasetImpl dataset) {
		if (refbacks != null) {
			refbacks.remove(dataset);
		}
	}

	public synchronized boolean isRefback() {
		return refbacks != null && !refbacks.isEmpty();
	}

	public synchronized void prepend(Changeset changeset) {
		if (prepend == null) {
			prepend = new HashSet<>();
		}
		prepend.add(changeset);
	}

	@Override
	public synchronized void setNamespace(String prefix, String name) {
		if (removedPrefixes == null) {
			removedPrefixes = new HashSet<>();
		}
		removedPrefixes.add(prefix);
		if (addedNamespaces == null) {
			addedNamespaces = new HashMap<>();
		}
		addedNamespaces.put(prefix, name);
	}

	@Override
	public synchronized void removeNamespace(String prefix) {
		if (addedNamespaces != null) {
			addedNamespaces.remove(prefix);
		}
		if (removedPrefixes == null) {
			removedPrefixes = new HashSet<>();
		}
		removedPrefixes.add(prefix);
	}

	@Override
	public synchronized void clearNamespaces() {
		if (removedPrefixes != null) {
			removedPrefixes.clear();
		}
		if (addedNamespaces != null) {
			addedNamespaces.clear();
		}
		namespaceCleared = true;
	}

	@Override
	public synchronized void observe(Resource subj, IRI pred, Value obj, Resource... contexts)
			throws SailConflictException {
		if (observations == null) {
			observations = new HashSet<>();
		}
		if (contexts == null) {
			observations.add(new StatementPattern(new Var("s", subj), new Var("p", pred), new Var("o", obj),
					new Var("g", null)));
		} else if (contexts.length == 0) {
			observations.add(new StatementPattern(new Var("s", subj), new Var("p", pred), new Var("o", obj)));
		} else {
			for (Resource ctx : contexts) {
				observations.add(new StatementPattern(new Var("s", subj), new Var("p", pred), new Var("o", obj),
						new Var("g", ctx)));
			}
		}
	}

	@Override
	public synchronized void clear(Resource... contexts) {
		if (contexts != null && contexts.length == 0) {
			if (approved != null) {
				approved.clear();
			}
			if (approvedContexts != null) {
				approvedContexts.clear();
			}
			statementCleared = true;
		} else {
			if (approved != null) {
				approved.remove(null, null, null, contexts);
			}
			if (approvedContexts != null) {
				approvedContexts.removeAll(Arrays.asList(contexts));
			}
			if (deprecatedContexts == null) {
				deprecatedContexts = new HashSet<>();
			}
			deprecatedContexts.addAll(Arrays.asList(contexts));
		}
	}

	@Override
	public synchronized void approve(Resource subj, IRI pred, Value obj, Resource ctx) {
		if (deprecated != null) {
			deprecated.remove(subj, pred, obj, ctx);
		}
		if (approved == null) {
			approved = createEmptyModel();
		}
		approved.add(subj, pred, obj, ctx);
		if (ctx != null) {
			if (approvedContexts == null) {
				approvedContexts = new HashSet<>();
			}
			approvedContexts.add(ctx);
		}
	}

	@Override
	public synchronized void approve(Statement statement) {
		if (deprecated != null) {
			deprecated.remove(statement);
		}
		if (approved == null) {
			approved = createEmptyModel();
		}
		approved.add(statement);
		if (statement.getContext() != null) {
			if (approvedContexts == null) {
				approvedContexts = new HashSet<>();
			}
			approvedContexts.add(statement.getContext());
		}
	}

	@Override
	public synchronized void deprecate(Statement statement) {
		if (approved != null) {
			approved.remove(statement);
		}
		if (deprecated == null) {
			deprecated = createEmptyModel();
		}
		deprecated.add(statement);
		Resource ctx = statement.getContext();
		if (approvedContexts != null && approvedContexts.contains(ctx) && !approved.contains(null, null, null, ctx)) {
			approvedContexts.remove(ctx);
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if (observations != null) {
			sb.append(observations.size());
			sb.append(" observations, ");
		}
		if (namespaceCleared) {
			sb.append("namespaceCleared, ");
		}
		if (removedPrefixes != null) {
			sb.append(removedPrefixes.size());
			sb.append(" removedPrefixes, ");
		}
		if (addedNamespaces != null) {
			sb.append(addedNamespaces.size());
			sb.append(" addedNamespaces, ");
		}
		if (statementCleared) {
			sb.append("statementCleared, ");
		}
		if (deprecatedContexts != null && !deprecatedContexts.isEmpty()) {
			sb.append(deprecatedContexts.size());
			sb.append(" deprecatedContexts, ");
		}
		if (deprecated != null) {
			sb.append(deprecated.size());
			sb.append(" deprecated, ");
		}
		if (approved != null) {
			sb.append(approved.size());
			sb.append(" approved, ");
		}
		if (sb.length() > 0) {
			return sb.substring(0, sb.length() - 2);
		} else {
			return super.toString();
		}
	}

	protected void setChangeset(Changeset from) {
		this.observations = from.observations;
		this.approved = from.approved;
		this.deprecated = from.deprecated;
		this.approvedContexts = from.approvedContexts;
		this.deprecatedContexts = from.deprecatedContexts;
		this.addedNamespaces = from.addedNamespaces;
		this.removedPrefixes = from.removedPrefixes;
		this.namespaceCleared = from.namespaceCleared;
		this.statementCleared = from.statementCleared;
	}

	public synchronized Set<StatementPattern> getObservations() {

		return cloneSet(observations);

	}

	public synchronized Set<Resource> getApprovedContexts() {
		return cloneSet(approvedContexts);
	}

	public synchronized Set<Resource> getDeprecatedContexts() {
		return cloneSet(deprecatedContexts);
	}

	public synchronized boolean isStatementCleared() {
		return statementCleared;
	}

	public synchronized Map<String, String> getAddedNamespaces() {
		return addedNamespaces;
	}

	public synchronized Set<String> getRemovedPrefixes() {
		return cloneSet(removedPrefixes);
	}

	public synchronized boolean isNamespaceCleared() {
		return namespaceCleared;
	}

	public synchronized boolean hasDeprecated() {
		return deprecated != null && !deprecated.isEmpty();
	}

	boolean isChanged() {
		return approved != null || deprecated != null || approvedContexts != null
				|| deprecatedContexts != null || addedNamespaces != null
				|| removedPrefixes != null || statementCleared || namespaceCleared
				|| observations != null;
	}

	synchronized List<Statement> getDeprecatedStatements() {
		if (deprecated == null) {
			return Collections.emptyList();
		}
		return new ArrayList<>(deprecated);
	}

	synchronized List<Statement> getApprovedStatements() {
		if (approved == null) {
			return Collections.emptyList();
		}
		return new ArrayList<>(approved);
	}

	synchronized boolean hasDeprecated(Statement statement) {
		if (deprecated == null) {
			return false;
		}
		return deprecated.contains(statement);
	}

	synchronized boolean hasApproved() {
		return approved != null && !approved.isEmpty();
	}

	synchronized Iterable<Statement> getApprovedStatements(Resource subj, IRI pred, Value obj,
			Resource[] contexts) {

		if (approved == null) {
			return Collections.emptyList();
		}

		Iterable<Statement> statements = approved.getStatements(subj, pred, obj, contexts);

		// This is a synchronized context, users of this method will be allowed to use the results at their leisure. We
		// provide a copy of the data so that there will be no concurrent modification exceptions!
		if (statements instanceof Collection) {
			return new ArrayList<>((Collection<? extends Statement>) statements);
		} else {
			return StreamSupport
					.stream(statements.spliterator(), false)
					.collect(Collectors.toList());
		}
	}

	synchronized void removeApproved(Statement next) {
		if (approved != null) {
			approved.remove(next);
		}
	}

	private <T> Set<T> cloneSet(Set<T> deprecatedContexts) {
		if (deprecatedContexts == null) {
			return null;
		}
		return new HashSet<>(deprecatedContexts);
	}
}
