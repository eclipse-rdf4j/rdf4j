/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.federation.optimizers;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.Collection;
import java.util.function.Function;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.algebra.EmptySet;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryOptimizer;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.filters.AccurateRepositoryBloomFilter;
import org.eclipse.rdf4j.repository.filters.RepositoryBloomFilter;

import com.google.common.base.MoreObjects;

/**
 * Remove StatementPatterns that have no statements.
 *
 * @author James Leigh
 */
public class EmptyPatternOptimizer extends AbstractQueryModelVisitor<RepositoryException> implements QueryOptimizer {
	private final Collection<? extends RepositoryConnection> members;

	private final Function<? super Repository, ? extends RepositoryBloomFilter> bloomFilters;

	public EmptyPatternOptimizer(Collection<? extends RepositoryConnection> members) {
		this(members, c -> AccurateRepositoryBloomFilter.INCLUDE_INFERRED_INSTANCE);
	}

	public EmptyPatternOptimizer(Collection<? extends RepositoryConnection> members,
			Function<? super Repository, ? extends RepositoryBloomFilter> bloomFilters) {
		this.members = members;
		this.bloomFilters = bloomFilters;
	}

	@Override
	public void optimize(TupleExpr query, Dataset dataset, BindingSet bindings) {
		try {
			query.visit(this);
		} catch (RepositoryException e) {
			throw new UndeclaredThrowableException(e);
		}
	}

	@Override
	public void meet(StatementPattern node) throws RepositoryException {
		Resource subj = (Resource) node.getSubjectVar().getValue();
		IRI pred = (IRI) node.getPredicateVar().getValue();
		Value obj = node.getObjectVar().getValue();
		Resource[] ctx = getContexts(node.getContextVar());
		for (RepositoryConnection member : members) {
			RepositoryBloomFilter bloomFilter = MoreObjects.firstNonNull(bloomFilters.apply(member.getRepository()),
					AccurateRepositoryBloomFilter.INCLUDE_INFERRED_INSTANCE);
			if (bloomFilter.mayHaveStatement(member, subj, pred, obj, ctx)) {
				return;
			}
		}
		node.replaceWith(new EmptySet());
	}

	private Resource[] getContexts(Var var) {
		return (var == null || !var.hasValue()) ? new Resource[0] : new Resource[] { (Resource) var.getValue() };
	}

}
