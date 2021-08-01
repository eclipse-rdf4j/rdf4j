package org.eclipse.rdf4j.spring.util;

import java.lang.invoke.MethodHandles;
import java.util.Optional;
import java.util.function.Function;

import org.eclipse.rdf4j.repository.DelegatingRepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RepositoryConnectionWrappingUtils {
	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	public static <T> RepositoryConnection wrapOnce(
			RepositoryConnection con,
			Function<RepositoryConnection, RepositoryConnection> wrapper,
			Class<T> wrapperClass) {
		if (!isWrapped(con, wrapperClass)) {
			logger.debug(
					"connection is not wrapped in {}, wrapping it", wrapperClass.getSimpleName());
			return wrapper.apply(con);
		} else {
			logger.debug(
					"connection is already wrapped in {}, not wrapping it",
					wrapperClass.getSimpleName());
		}
		return con;
	}

	public static <T> boolean isWrapped(RepositoryConnection con, Class<T> wrapperClass) {
		return findWrapper(con, wrapperClass).isPresent();
	}

	public static <T> Optional<T> findWrapper(RepositoryConnection con, Class<T> wrapperClass) {
		if (wrapperClass.isInstance(con)) {
			return Optional.of((T) con);
		}
		if (con instanceof DelegatingRepositoryConnection) {
			return findWrapper(((DelegatingRepositoryConnection) con).getDelegate(), wrapperClass);
		}
		return Optional.empty();
	}

	public static RepositoryConnection findRoot(RepositoryConnection con) {
		if (con instanceof DelegatingRepositoryConnection) {
			return findRoot(((DelegatingRepositoryConnection) con).getDelegate());
		} else {
			return con;
		}
	}
}
