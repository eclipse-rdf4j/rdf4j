package org.eclipse.rdf4j.http.client;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.QueryResult;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.impl.BackgroundGraphResult;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultParser;
import org.eclipse.rdf4j.query.resultio.helpers.BackgroundTupleResult;
import org.eclipse.rdf4j.rio.RDFParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BackgroundResultExecutor implements AutoCloseable {

	private final Logger logger = LoggerFactory.getLogger(BackgroundResultExecutor.class);

	private final ExecutorService executor;

	private final HashSet<QueryResult<?>> executing = new HashSet<>();

	public BackgroundResultExecutor(ExecutorService executor) {
		this.executor = Objects.requireNonNull(executor, "Executor service was null");
	}

	public TupleQueryResult parse(TupleQueryResultParser parser, InputStream in) {
		BackgroundTupleResult result = new BackgroundTupleResult(parser, in);
		autoCloseRunnable(result, result);
		return result;
	}

	public GraphQueryResult parse(RDFParser parser, InputStream in, Charset charset, String baseURI) {
		BackgroundGraphResult result = new BackgroundGraphResult(parser, in, charset, baseURI);
		autoCloseRunnable(result, result);
		return result;
	}

	/**
	 * Force close any executing background result parsers
	 */
	@Override
	public void close() {
		synchronized (executing) {
			for (AutoCloseable onclose : executing) {
				try {
					onclose.close();
				} catch (Exception e) {
					logger.error(e.toString(), e);
				}
			}
		}
	}

	private void autoCloseRunnable(QueryResult<?> result, Runnable runner) {
		synchronized (executing) {
			executing.add(result);
		}
		executor.execute(() -> {
			try {
				runner.run();
			} finally {
				synchronized (executing) {
					executing.remove(result);
				}
			}
		});
	}
}
