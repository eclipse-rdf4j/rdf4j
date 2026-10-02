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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.util.function.LongSupplier;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
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
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;

/**
 * A {@link RepositoryConnection} that wraps every prepared {@link Query}/{@link Update} so that its evaluation is
 * recorded as an OpenTelemetry span, and that also traces {@code getStatements}/{@code exportStatements}/
 * {@code hasStatement} calls and {@code add}/{@code remove}/{@code clear} calls directly, following the OpenTelemetry
 * database client semantic conventions. See {@link org.eclipse.rdf4j.opentelemetry.repository} for the full attribute
 * list.
 * <p>
 * {@code exportStatements} is traced as its own {@code GET_STATEMENTS} span (not by delegating to
 * {@link #getStatements}): {@link org.eclipse.rdf4j.repository.base.RepositoryConnectionWrapper} forwards it directly
 * to the delegate connection rather than routing it through {@code getStatements}, and this is the path RDF4J Server's
 * {@code GET .../statements} endpoint uses to stream matching statements into the HTTP response.
 * <p>
 * Instances are usually obtained via {@link TracingRepository}; this constructor is available for advanced/custom
 * wiring.
 *
 * @author Andreas Schwarte
 * @author Priyadarshan Giri
 */
public class TracingRepositoryConnection extends RepositoryConnectionWrapper {

	/**
	 * Rendered for an explicit {@code null} element in a {@code contexts} array, i.e. "the default/null context
	 * specifically" - distinct from the {@code ?context} wildcard rendered when no context argument is given at all
	 * (meaning "any context").
	 */
	private static final String NULL_CONTEXT = "<urn:x-rdf4j:null-context>";

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
	public void exportStatements(Resource subj, IRI pred, Value obj, boolean includeInferred, RDFHandler handler,
			Resource... contexts) throws RepositoryException, RDFHandlerException {
		// RepositoryConnectionWrapper.exportStatements() delegates directly rather than going through
		// getStatements(), e.g. this is the path RDF4J Server's GET .../statements endpoint uses to stream
		// matches into the HTTP response - trace it in its own right rather than relying on getStatements().
		Span span = TracingOperation.startSpan(tracer, config, repositoryId, "GET_STATEMENTS");
		attachTriplePatternText(span, subj, pred, obj, contexts);
		CountingRDFHandler countingHandler = new CountingRDFHandler(handler);
		try (Scope scope = span.makeCurrent()) {
			super.exportStatements(subj, pred, obj, includeInferred, countingHandler, contexts);
			span.setAttribute(DbOtelAttributes.DB_RESPONSE_RETURNED_ROWS, countingHandler.getCount());
		} catch (RuntimeException e) {
			TracingOperation.recordException(e, span);
			throw e;
		} finally {
			span.end();
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
		if (contexts.length == 0) {
			// no context given means "any context"; render the same wildcard variable as an unbound argument
			pattern.append(' ').append("?context");
		} else {
			for (Resource context : contexts) {
				// an explicit null element means "the default/null context" specifically, which is a different
				// thing from the "?context" wildcard above (no context args given at all); use a distinct,
				// non-SPARQL marker so the two aren't conflated
				pattern.append(' ').append(context != null ? TracingOperation.valueToString(context) : NULL_CONTEXT);
			}
		}
		pattern.append(" }");
		return pattern.toString();
	}

	private static String termOrVariable(Value term, String variableName) {
		return term != null ? TracingOperation.valueToString(term) : "?" + variableName;
	}

	@Override
	public void add(Resource subject, IRI predicate, Value object, Resource... contexts) throws RepositoryException {
		traceWrite("ADD", () -> super.add(subject, predicate, object, contexts), () -> contextCount(contexts));
	}

	@Override
	public void add(Statement st, Resource... contexts) throws RepositoryException {
		traceWrite("ADD", () -> super.add(st, contexts), () -> contextCount(contexts));
	}

	@Override
	public void add(Iterable<? extends Statement> statements, Resource... contexts) throws RepositoryException {
		if (!config.isCaptureWriteOperations() || !config.isCaptureWriteCount()) {
			traceWrite("ADD", () -> super.add(statements, contexts), null);
			return;
		}
		CountingIterable<? extends Statement> counted = CountingIterable.wrap(statements);
		traceWrite("ADD", () -> super.add(counted, contexts), counted::getCount);
	}

	@Override
	public void add(CloseableIteration<? extends Statement> statementIter, Resource... contexts)
			throws RepositoryException {
		if (!config.isCaptureWriteOperations() || !config.isCaptureWriteCount()) {
			traceWrite("ADD", () -> super.add(statementIter, contexts), null);
			return;
		}
		CountingCloseableIteration<? extends Statement> counted = new CountingCloseableIteration<>(statementIter);
		traceWrite("ADD", () -> super.add(counted, contexts), counted::getCount);
	}

	@Override
	public void add(File file, String baseURI, RDFFormat dataFormat, Resource... contexts)
			throws IOException, RDFParseException, RepositoryException {
		traceWriteIO("ADD", () -> super.add(file, baseURI, dataFormat, contexts));
	}

	@Override
	public void add(InputStream in, String baseURI, RDFFormat dataFormat, Resource... contexts)
			throws IOException, RDFParseException, RepositoryException {
		traceWriteIO("ADD", () -> super.add(in, baseURI, dataFormat, contexts));
	}

	@Override
	public void add(Reader reader, String baseURI, RDFFormat dataFormat, Resource... contexts)
			throws IOException, RDFParseException, RepositoryException {
		traceWriteIO("ADD", () -> super.add(reader, baseURI, dataFormat, contexts));
	}

	@Override
	public void add(URL url, String baseURI, RDFFormat dataFormat, Resource... contexts)
			throws IOException, RDFParseException, RepositoryException {
		traceWriteIO("ADD", () -> super.add(url, baseURI, dataFormat, contexts));
	}

	@Override
	public void remove(Resource subject, IRI predicate, Value object, Resource... contexts)
			throws RepositoryException {
		// subject/predicate/object may be null (wildcard pattern removal), so the number of statements actually
		// removed isn't known without an extra read
		traceWrite("REMOVE", () -> super.remove(subject, predicate, object, contexts), null);
	}

	@Override
	public void remove(Statement st, Resource... contexts) throws RepositoryException {
		traceWrite("REMOVE", () -> super.remove(st, contexts), () -> contextCount(contexts));
	}

	@Override
	public void remove(Iterable<? extends Statement> statements, Resource... contexts) throws RepositoryException {
		if (!config.isCaptureWriteOperations() || !config.isCaptureWriteCount()) {
			traceWrite("REMOVE", () -> super.remove(statements, contexts), null);
			return;
		}
		CountingIterable<? extends Statement> counted = CountingIterable.wrap(statements);
		traceWrite("REMOVE", () -> super.remove(counted, contexts), counted::getCount);
	}

	@Override
	public void remove(CloseableIteration<? extends Statement> statementIter, Resource... contexts)
			throws RepositoryException {
		if (!config.isCaptureWriteOperations() || !config.isCaptureWriteCount()) {
			traceWrite("REMOVE", () -> super.remove(statementIter, contexts), null);
			return;
		}
		CountingCloseableIteration<? extends Statement> counted = new CountingCloseableIteration<>(statementIter);
		traceWrite("REMOVE", () -> super.remove(counted, contexts), counted::getCount);
	}

	@Override
	public void clear(Resource... contexts) throws RepositoryException {
		// clears every matching statement; the number removed isn't known without an extra read
		traceWrite("CLEAR", () -> super.clear(contexts), null);
	}

	/**
	 * @return the number of statements a single-statement {@code add}/{@code remove} call affects: one per context, or
	 *         one if no context is given (added/removed without a context).
	 */
	private static long contextCount(Resource... contexts) {
		return Math.max(1, contexts.length);
	}

	/**
	 * Runs a write action, traced as an {@code ADD}/{@code REMOVE}/{@code CLEAR} span when
	 * {@link RDF4JOpenTelemetryConfig#isCaptureWriteOperations()} is enabled. When disabled, {@code action} runs
	 * directly with no tracing overhead.
	 *
	 * @param lazyCount supplies the number of statements affected by the operation, recorded as
	 *                  {@code rdf4j.affected_rows} when {@link RDF4JOpenTelemetryConfig#isCaptureWriteCount()} is also
	 *                  enabled; {@code null} if not determinable for this operation. Evaluated only after
	 *                  {@code action} completes successfully.
	 */
	private void traceWrite(String operationName, WriteAction action, LongSupplier lazyCount)
			throws RepositoryException {
		if (!config.isCaptureWriteOperations()) {
			action.run();
			return;
		}
		Span span = TracingOperation.startSpan(tracer, config, repositoryId, operationName);
		try (Scope scope = span.makeCurrent()) {
			action.run();
			if (config.isCaptureWriteCount() && lazyCount != null) {
				span.setAttribute(DbOtelAttributes.RDF4J_AFFECTED_ROWS, lazyCount.getAsLong());
			}
		} catch (RuntimeException e) {
			TracingOperation.recordException(e, span);
			throw e;
		} finally {
			span.end();
		}
	}

	/**
	 * Like {@link #traceWrite(String, WriteAction, LongSupplier)}, for the RDF-document-based {@code add} overloads,
	 * which can also throw {@link IOException}. The number of statements written isn't determinable for these without
	 * intercepting the parser pipeline, so no count is ever recorded.
	 */
	private void traceWriteIO(String operationName, IOWriteAction action)
			throws IOException, RDFParseException, RepositoryException {
		if (!config.isCaptureWriteOperations()) {
			action.run();
			return;
		}
		Span span = TracingOperation.startSpan(tracer, config, repositoryId, operationName);
		try (Scope scope = span.makeCurrent()) {
			action.run();
		} catch (RuntimeException e) {
			TracingOperation.recordException(e, span);
			throw e;
		} catch (IOException e) {
			TracingOperation.recordException(e, span);
			throw e;
		} finally {
			span.end();
		}
	}

	@FunctionalInterface
	private interface WriteAction {
		void run() throws RepositoryException;
	}

	@FunctionalInterface
	private interface IOWriteAction {
		void run() throws IOException, RDFParseException, RepositoryException;
	}
}
