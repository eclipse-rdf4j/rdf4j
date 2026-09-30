/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.opentelemetry.repository;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.opentelemetry.RDF4JOpenTelemetryConfig;
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.Query;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.base.RepositoryConnectionWrapper;
import org.eclipse.rdf4j.repository.sparql.query.QueryStringUtil;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;

/**
 * A {@link RepositoryConnection} that wraps every prepared {@link Query}/{@link Update} so that its evaluation is
 * recorded as an OpenTelemetry span, and that also traces {@code getStatements}/{@code hasStatement} calls directly,
 * following the OpenTelemetry database client semantic conventions. See
 * {@link org.eclipse.rdf4j.opentelemetry.repository} for the full attribute list.
 * <p>
 * Instances are usually obtained via {@link TracingRepository}; this constructor is available for advanced/custom
 * wiring.
 *
 * @author Andreas Schwarte
 * @author Priyadarshan Giri
 */
public class TracingRepositoryConnection extends RepositoryConnectionWrapper {

	private final String repositoryId;
	private final RDF4JOpenTelemetryConfig config;
	private final Tracer tracer;

	/**
	 * @param repository   the owning repository, passed to {@link RepositoryConnectionWrapper}
	 * @param repositoryId recorded as the {@code db.namespace} span attribute
	 * @param config       the OpenTelemetry configuration to use
	 * @param delegate     the connection to wrap
	 */
	public TracingRepositoryConnection(Repository repository, String repositoryId, RDF4JOpenTelemetryConfig config,
			RepositoryConnection delegate) {
		super(repository, delegate);
		this.repositoryId = repositoryId;
		this.config = config;
		this.tracer = config.getTracer();
	}

	@Override
	public TupleQuery prepareTupleQuery(QueryLanguage ql, String query, String baseURI)
			throws MalformedQueryException, RepositoryException {
		return new TracingTupleQuery(super.prepareTupleQuery(ql, query, baseURI), query, repositoryId, config,
				tracer);
	}

	@Override
	public GraphQuery prepareGraphQuery(QueryLanguage ql, String query, String baseURI)
			throws MalformedQueryException, RepositoryException {
		return new TracingGraphQuery(super.prepareGraphQuery(ql, query, baseURI), query, repositoryId, config,
				tracer);
	}

	@Override
	public BooleanQuery prepareBooleanQuery(QueryLanguage ql, String query, String baseURI)
			throws MalformedQueryException, RepositoryException {
		return new TracingBooleanQuery(super.prepareBooleanQuery(ql, query, baseURI), query, repositoryId, config,
				tracer);
	}

	@Override
	public Update prepareUpdate(QueryLanguage ql, String update, String baseURI)
			throws MalformedQueryException, RepositoryException {
		return new TracingUpdate(super.prepareUpdate(ql, update, baseURI), update, repositoryId, config, tracer);
	}

	@Override
	public Query prepareQuery(QueryLanguage ql, String query, String baseURI)
			throws MalformedQueryException, RepositoryException {
		Query q = super.prepareQuery(ql, query, baseURI);
		if (q instanceof TupleQuery) {
			return new TracingTupleQuery((TupleQuery) q, query, repositoryId, config, tracer);
		}
		if (q instanceof BooleanQuery) {
			return new TracingBooleanQuery((BooleanQuery) q, query, repositoryId, config, tracer);
		}
		if (q instanceof GraphQuery) {
			return new TracingGraphQuery((GraphQuery) q, query, repositoryId, config, tracer);
		}
		throw new MalformedQueryException("Unexpected query type: " + q.getClass());
	}

	@Override
	public RepositoryResult<Statement> getStatements(Resource subj, IRI pred, Value obj, boolean includeInferred,
			Resource... contexts) throws RepositoryException {
		Span span = TracingOperation.startSpan(tracer, config, repositoryId, "GET_STATEMENTS");
		attachTriplePatternText(span, subj, pred, obj, contexts);
		try (Scope scope = span.makeCurrent()) {
			return new TracingRepositoryResult<>(super.getStatements(subj, pred, obj, includeInferred, contexts),
					span);
		} catch (RuntimeException e) {
			TracingOperation.recordException(e, span);
			span.end();
			throw e;
		}
	}

	@Override
	public boolean hasStatement(Resource subj, IRI pred, Value obj, boolean includeInferred, Resource... contexts)
			throws RepositoryException {
		Span span = TracingOperation.startSpan(tracer, config, repositoryId, "HAS_STATEMENT");
		attachTriplePatternText(span, subj, pred, obj, contexts);
		try (Scope scope = span.makeCurrent()) {
			return super.hasStatement(subj, pred, obj, includeInferred, contexts);
		} catch (RuntimeException e) {
			TracingOperation.recordException(e, span);
			throw e;
		} finally {
			span.end();
		}
	}

	@Override
	public boolean hasStatement(Statement st, boolean includeInferred, Resource... contexts)
			throws RepositoryException {
		Span span = TracingOperation.startSpan(tracer, config, repositoryId, "HAS_STATEMENT");
		attachTriplePatternText(span, st.getSubject(), st.getPredicate(), st.getObject(), contexts);
		try (Scope scope = span.makeCurrent()) {
			return super.hasStatement(st, includeInferred, contexts);
		} catch (RuntimeException e) {
			TracingOperation.recordException(e, span);
			throw e;
		} finally {
			span.end();
		}
	}

	/**
	 * Attaches the requested triple pattern as the {@code db.query.text} span attribute, e.g. {@code { ?subj
	 * <http://example.com/predicate> "object" <http://context> }}, when query text capture is enabled.
	 */
	private void attachTriplePatternText(Span span, Resource subj, IRI pred, Value obj, Resource... contexts) {
		if (!config.isCaptureQueryText()) {
			return;
		}
		String pattern = toTriplePatternText(subj, pred, obj, contexts);
		span.setAttribute(DbOtelAttributes.DB_QUERY_TEXT,
				TracingOperation.truncate(pattern, config.getMaxQueryTextLength()));
	}

	private static String toTriplePatternText(Resource subj, IRI pred, Value obj, Resource... contexts) {
		StringBuilder pattern = new StringBuilder("{ ");
		pattern.append(termOrVariable(subj, "subj")).append(' ');
		pattern.append(termOrVariable(pred, "pred")).append(' ');
		pattern.append(termOrVariable(obj, "obj"));
		for (Resource context : contexts) {
			pattern.append(' ').append(termOrVariable(context, "context"));
		}
		pattern.append(" }");
		return pattern.toString();
	}

	private static String termOrVariable(Value term, String variableName) {
		if (term == null) {
			return "?" + variableName;
		}
		if (term instanceof BNode) {
			// QueryStringUtil doesn't support BNodes (not expressible in SPARQL syntax)
			return term.toString();
		}
		return QueryStringUtil.valueToString(term);
	}
}
