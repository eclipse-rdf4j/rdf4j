package org.eclipse.rdf4j.spring.resultcache;

import java.util.function.Supplier;

public interface ThrowableRecorder {
	void recordThrowable(Throwable t);

	static <T> T recordingThrowable(Supplier<T> supplier, ThrowableRecorder recorder) {
		try {
			return supplier.get();
		} catch (Throwable t) {
			recorder.recordThrowable(t);
			throw t;
		}
	}

	static void recordingThrowable(Runnable runnable, ThrowableRecorder recorder) {
		try {
			runnable.run();
		} catch (Throwable t) {
			recorder.recordThrowable(t);
			throw t;
		}
	}
}
