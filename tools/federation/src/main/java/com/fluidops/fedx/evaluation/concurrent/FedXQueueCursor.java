package com.fluidops.fedx.evaluation.concurrent;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.impl.QueueCursor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Specialized variants of {@link QueueCursor} which avoids converting any
 * exception if it is already of type{@link QueryEvaluationException}.
 * 
 * 
 * @author Andreas Schwarte
 *
 * @param <T>
 */
public class FedXQueueCursor<T> extends QueueCursor<CloseableIteration<T, QueryEvaluationException>> {

	private static final Logger log = LoggerFactory.getLogger(FedXQueueCursor.class);

	public static <T> FedXQueueCursor<T> create(int capacity) {
		BlockingQueue<CloseableIteration<T, QueryEvaluationException>> queue = new ArrayBlockingQueue<>(capacity,
				false);
		return new FedXQueueCursor<>(queue);
	}

	/**
	 * Reference to the queue such that we can access it in {@link #handleClose()}.
	 * This is required to make sure that we can close the non-consumed iterations
	 * from the queue. Note that the private queue of the super class is not
	 * accessible.
	 */
	private final BlockingQueue<CloseableIteration<T, QueryEvaluationException>> queueRef;

	private FedXQueueCursor(BlockingQueue<CloseableIteration<T, QueryEvaluationException>> queue) {
		super(queue);
		this.queueRef = queue;
	}

	@Override
	protected QueryEvaluationException convert(Exception e) {
		if (e instanceof QueryEvaluationException) {
			return (QueryEvaluationException) e;
		}
		return super.convert(e);
	}

	@Override
	public void handleClose() throws QueryEvaluationException {

		try {
			// consume all remaining elements from the queue and make sure to close them
			// Note: unfortunately we cannot access "afterLast" of the super class
			// => thus have to make a check whether the polled object is actually a
			// closable iteration
			Object take = queueRef.poll();
			while (take != null) {
				if (take instanceof CloseableIteration) {
					@SuppressWarnings("unchecked")
					CloseableIteration<T, QueryEvaluationException> closable = (CloseableIteration<T, QueryEvaluationException>) take;
					try {
						log.trace("Attempting to close non consumed inner iteration.");
						closable.close();
					} catch (Throwable t) {
						log.trace("Failed to close inner iteration: ", t);
					}
				}
				take = queueRef.poll();
			}
			done(); // re-add after-last
		} finally {
			super.handleClose();
		}
	}

}
