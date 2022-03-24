
package org.eclipse.rdf4j.common.iteration;

import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;

/**
 * A {@link Spliterator} implementation that wraps a {@link CloseableIteration}. It handles occurrence of checked
 * exceptions by wrapping them in RuntimeExceptions, and in addition ensures that the wrapped Iteration is closed when
 * exhausted.
 *
 * @author Håvard Ottestad
 */
public class CloseableIterationSpliterator<T, K extends CloseableIteration<T, ? extends Exception>>
		extends Spliterators.AbstractSpliterator<T> {

	private final K iteration;

	/**
	 * Creates a {@link Spliterator} implementation that wraps the supplied {@link CloseableIteration}. It handles
	 * occurrence of checked exceptions by wrapping them in RuntimeExceptions, and in addition ensures that the wrapped
	 * Iteration is closed when exhausted.
	 *
	 * @param iteration the iteration to wrap
	 */
	public CloseableIterationSpliterator(K iteration) {
		super(Long.MAX_VALUE, Spliterator.IMMUTABLE | Spliterator.NONNULL);
		this.iteration = iteration;
	}

	@Override
	public boolean tryAdvance(Consumer<? super T> action) {
		Objects.requireNonNull(action, "action may not be null");

		// we start by assuming that we need to close the iteration, in case an error occurs
		// this could be handled in the catch part, but then we would need to catch throwable...which is not recommended
		boolean needsToBeClosed = true;
		try {
			if (iteration.hasNext()) {
				action.accept(iteration.next());
				// since the iteration might have more elements we don't need to close it
				needsToBeClosed = false;
				return true;
			}
			return false;
		} catch (Exception e) {
			if (e instanceof RuntimeException) {
				throw (RuntimeException) e;
			}
			throw new RuntimeException(e);
		} finally {
			if (needsToBeClosed) {
				try {
					iteration.close();
				} catch (Exception ignored) {
				}
			}
		}
	}

	@Override
	public void forEachRemaining(final Consumer<? super T> action) {
		Objects.requireNonNull(action, "action may not be null");
		try {
			while (iteration.hasNext()) {
				action.accept(iteration.next());
			}
		} catch (Exception e) {
			if (e instanceof RuntimeException) {
				throw (RuntimeException) e;
			}
			throw new RuntimeException(e);
		} finally {
			try {
				iteration.close();
			} catch (Exception ignored) {
			}
		}
	}
}
