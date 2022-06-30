/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import org.eclipse.rdf4j.common.annotation.Experimental;
import org.eclipse.rdf4j.common.annotation.InternalUseOnly;
import org.eclipse.rdf4j.common.concurrent.locks.ReadPrefReadWriteLockManager;
import org.eclipse.rdf4j.common.concurrent.locks.StampedLockManager;
import org.eclipse.rdf4j.common.concurrent.locks.diagnostics.ConcurrentCleaner;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.common.transaction.TransactionSetting;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.DASH;
import org.eclipse.rdf4j.model.vocabulary.RDF4J;
import org.eclipse.rdf4j.model.vocabulary.RSX;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.NotifyingSail;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.memory.MemoryStoreConnection;
import org.eclipse.rdf4j.sail.shacl.ast.ContextWithShapes;
import org.eclipse.rdf4j.sail.shacl.ast.Shape;
import org.eclipse.rdf4j.sail.shacl.wrapper.shape.CombinedShapeSource;
import org.eclipse.rdf4j.sail.shacl.wrapper.shape.ForwardChainingShapeSource;
import org.eclipse.rdf4j.sail.shacl.wrapper.shape.ShapeSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//@formatter:off

/**
 * A {@link Sail} implementation that adds support for the Shapes Constraint Language (SHACL).
 * <p>
 * The ShaclSail looks for SHACL shape data in a special named graph {@link RDF4J#SHACL_SHAPE_GRAPH}.
 * <h4>Working example</h4>
 * <p>
 *
 * <pre>
 * import java.io.IOException;
 * import java.io.StringReader;
 *
 * import org.eclipse.rdf4j.common.exception.ValidationException;
 * import org.eclipse.rdf4j.model.Model;
 * import org.eclipse.rdf4j.model.vocabulary.RDF4J;
 * import org.eclipse.rdf4j.repository.RepositoryException;
 * import org.eclipse.rdf4j.repository.sail.SailRepository;
 * import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
 * import org.eclipse.rdf4j.rio.RDFFormat;
 * import org.eclipse.rdf4j.rio.Rio;
 * import org.eclipse.rdf4j.rio.WriterConfig;
 * import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
 * import org.eclipse.rdf4j.sail.memory.MemoryStore;
 * import org.eclipse.rdf4j.sail.shacl.ShaclSail;
 *
 * public class ShaclSampleCode {
 *
 *     public static void main(String[] args) throws IOException {
 *
 *         ShaclSail shaclSail = new ShaclSail(new MemoryStore());
 *
 *         SailRepository sailRepository = new SailRepository(shaclSail);
 *         sailRepository.init();
 *
 *         try (SailRepositoryConnection connection = sailRepository.getConnection()) {
 *
 *             connection.begin();
 *
 *             StringReader shaclRules = new StringReader(String.join("\n", "",
 *                     "@prefix ex: &#x3C;http://example.com/ns#&#x3E; .",
 *                     "@prefix sh: &#x3C;http://www.w3.org/ns/shacl#&#x3E; .",
 *                     "@prefix xsd: &#x3C;http://www.w3.org/2001/XMLSchema#&#x3E; .",
 *                     "@prefix foaf: &#x3C;http://xmlns.com/foaf/0.1/&#x3E;.",
 *
 *                     "ex:PersonShape",
 *                     "    a sh:NodeShape  ;",
 *                     "    sh:targetClass foaf:Person ;",
 *                     "    sh:property ex:PersonShapeProperty .",
 *
 *                     "ex:PersonShapeProperty ",
 *                     "    sh:path foaf:age ;",
 *                     "    sh:datatype xsd:int ;",
 *                     "    sh:maxCount 1 ;",
 *                     "    sh:minCount 1 ."
 *             ));
 *
 *             connection.add(shaclRules, "", RDFFormat.TURTLE, RDF4J.SHACL_SHAPE_GRAPH);
 *             connection.commit();
 *
 *             connection.begin();
 *
 *             StringReader invalidSampleData = new StringReader(String.join("\n", "",
 *                     "@prefix ex: &#x3C;http://example.com/ns#&#x3E; .",
 *                     "@prefix foaf: &#x3C;http://xmlns.com/foaf/0.1/&#x3E;.",
 *                     "@prefix xsd: &#x3C;http://www.w3.org/2001/XMLSchema#&#x3E; .",
 *
 *                     "ex:peter a foaf:Person ;",
 *                     "    foaf:age 20, \"30\"^^xsd:int  ."
 *
 *             ));
 *
 *             connection.add(invalidSampleData, "", RDFFormat.TURTLE);
 *             try {
 *                 connection.commit();
 *             } catch (RepositoryException exception) {
 *                 Throwable cause = exception.getCause();
 *                 if (cause instanceof ValidationException) {
 *
 *                     // use the validationReportModel to understand validation violations
 *                     Model validationReportModel = ((ValidationException) cause).validationReportAsModel();
 *
 *                     // Pretty print the validation report
 *                     WriterConfig writerConfig = new WriterConfig()
 *                             .set(BasicWriterSettings.PRETTY_PRINT, true)
 *                             .set(BasicWriterSettings.INLINE_BLANK_NODES, true);
 *
 *                     Rio.write(validationReportModel, System.out, RDFFormat.TURTLE, writerConfig);
 *                     System.out.println();
 *                 }
 *
 *                 throw exception;
 *             }
 *         }
 *     }
 * }
 * </pre>
 *
 * @author Heshan Jayasinghe
 * @author Håvard Ottestad
 * @see <a href="https://www.w3.org/TR/shacl/">SHACL W3C Recommendation</a>
 */
//@formatter:on
public class ShaclSail extends ShaclSailBaseConfiguration {

	private static final int AVAILABLE_PROCESSORS = Runtime.getRuntime().availableProcessors();

	private static final Logger logger = LoggerFactory.getLogger(ShaclSail.class);
	private static final ConcurrentCleaner cleaner = new ConcurrentCleaner();

	/**
	 * an initialized {@link Repository} for storing/retrieving Shapes data
	 */
	private SailRepository shapesRepo;

	// lockManager used for read/write locks used to synchronize validation so that SNAPSHOT isolation is sufficient to
	// achieve SERIALIZABLE isolation wrt. validation
	final ReadPrefReadWriteLockManager serializableValidationLock = new ReadPrefReadWriteLockManager();

	// shapesCacheLockManager used to keep track of changes to the cache
	private StampedLockManager.Cache<List<ContextWithShapes>> cachedShapes;

	// true if the base sail supports IsolationLevels.SNAPSHOT
	private boolean supportsSnapshotIsolation;

	// This is used to keep track of the current connection, if the opening and closing of connections is done serially.
	// If it is done in parallel, then this will catch that and the multipleConcurrentConnections == true.
	private final transient AtomicLong singleConnectionCounter = new AtomicLong();
	final Object singleConnectionMonitor = new Object();

	private final AtomicBoolean initialized = new AtomicBoolean(false);

	private final RevivableExecutorService executorService;

	@InternalUseOnly
	StampedLockManager.Cache<List<ContextWithShapes>>.WritableState getCachedShapesForWriting()
			throws InterruptedException {
		return cachedShapes.getWriteState();
	}

	@InternalUseOnly
	public StampedLockManager.Cache<List<ContextWithShapes>>.ReadableState getCachedShapes()
			throws InterruptedException {
		return cachedShapes.getReadState();
	}

	static class CleanableState implements Runnable {

		private final AtomicBoolean initialized;
		private final ExecutorService executorService;

		CleanableState(AtomicBoolean initialized, ExecutorService executorService) {
			this.initialized = initialized;
			this.executorService = executorService;
		}

		public void run() {
			if (initialized.get()) {
				logger.error("ShaclSail was garbage collected without shutdown() having been called first.");
			}
			executorService.shutdownNow();
		}
	}

	public ShaclSail(NotifyingSail baseSail) {
		super(baseSail);
		executorService = getExecutorService();
		cleaner.register(this, new CleanableState(initialized, executorService));
		this.supportsSnapshotIsolation = baseSail.getSupportedIsolationLevels().contains(IsolationLevels.SNAPSHOT);
	}

	public ShaclSail() {
		super();
		executorService = getExecutorService();
		cleaner.register(this, new CleanableState(initialized, executorService));
	}

	@Override
	public void setBaseSail(Sail baseSail) {
		super.setBaseSail(baseSail);
		this.supportsSnapshotIsolation = baseSail.getSupportedIsolationLevels().contains(IsolationLevels.SNAPSHOT);
	}

	/**
	 * @return
	 * @implNote This is an extension point for configuring a different executor service for parallel validation. The
	 *           code is marked as experimental because it may change from one minor release to another.
	 */
	@Experimental
	protected RevivableExecutorService getExecutorService() {
		return new RevivableExecutorService(
				() -> Executors.newFixedThreadPool(AVAILABLE_PROCESSORS,
						r -> {
							Thread t = Executors.defaultThreadFactory().newThread(r);
							// this thread pool does not need to stick around if the all other threads are done, because
							// it is only used for SHACL validation and if all other threads have ended then there would
							// be no thread to receive the validation results.
							t.setDaemon(true);
							t.setName("ShaclSail validation thread " + t.getId());
							return t;
						}));
	}

	void closeConnection() {
		// closing a connection will set the to zero if there are currently no other connections open
		singleConnectionCounter.compareAndSet(1, 0);
	}

	boolean usesSingleConnection() {
//		return false; // if this method returns false, then the connection will always use the new serializable validation
		assert singleConnectionCounter.get() != 0;
		return singleConnectionCounter.get() == 1;
	}

	/**
	 * Lists the predicates that have been implemented in the ShaclSail. All of these, and all combinations,
	 * <i>should</i> work, please report any bugs. For sh:path, only single predicate paths, or single predicate inverse
	 * paths are supported. DASH and RSX features may need to be enabled.
	 *
	 * @return List of IRIs (SHACL predicates)
	 */
	public static List<IRI> getSupportedShaclPredicates() {
		return Arrays.asList(
				SHACL.TARGET_CLASS,
				SHACL.PATH,
				SHACL.PROPERTY,
				SHACL.OR,
				SHACL.AND,
				SHACL.MIN_COUNT,
				SHACL.MAX_COUNT,
				SHACL.MIN_LENGTH,
				SHACL.MAX_LENGTH,
				SHACL.PATTERN,
				SHACL.FLAGS,
				SHACL.NODE_KIND_PROP,
				SHACL.LANGUAGE_IN,
				SHACL.DATATYPE,
				SHACL.MIN_EXCLUSIVE,
				SHACL.MIN_INCLUSIVE,
				SHACL.MAX_EXCLUSIVE,
				SHACL.MAX_INCLUSIVE,
				SHACL.CLASS,
				SHACL.TARGET_NODE,
				SHACL.DEACTIVATED,
				SHACL.TARGET_SUBJECTS_OF,
				SHACL.IN,
				SHACL.UNIQUE_LANG,
				SHACL.NOT,
				SHACL.TARGET_OBJECTS_OF,
				SHACL.HAS_VALUE,
				SHACL.TARGET_PROP,
				SHACL.INVERSE_PATH,
				SHACL.NODE,
				SHACL.QUALIFIED_MAX_COUNT,
				SHACL.QUALIFIED_MIN_COUNT,
				SHACL.QUALIFIED_VALUE_SHAPE,
				SHACL.SHAPES_GRAPH,
				DASH.hasValueIn,
				RSX.targetShape
		);
	}

	@Override
	public void init() throws SailException {
		if (!initialized.compareAndSet(false, true)) {
			// already initialized
			return;
		}

		super.init();

		executorService.init();

		if (shapesRepo != null) {
			shapesRepo.shutDown();
			shapesRepo = null;
		}

		if (super.getBaseSail().getDataDir() != null) {
			String path = super.getBaseSail().getDataDir().getPath();
			if (path.endsWith("/")) {
				path = path.substring(0, path.length() - 1);
			}
			path = path + "/shapes-graph/";

			logger.info("Shapes will be persisted in: " + path);

			shapesRepo = new SailRepository(new MemoryStore(new File(path)));
		} else {
			shapesRepo = new SailRepository(new MemoryStore());
		}

		shapesRepo.init();

		try (SailRepositoryConnection shapesRepoConnection = shapesRepo.getConnection()) {
			shapesRepoConnection.begin(IsolationLevels.NONE);
			shapesRepoConnection.commit();
		}

		cachedShapes = new StampedLockManager.Cache<>(new StampedLockManager(), () -> {
			IRI[] shapesGraphs = getShapesGraphs().stream()
					.map(g -> {
						if (g.equals(RDF4J.NIL)) {
							return null;
						}
						return g;
					})
					.toArray(IRI[]::new);

			boolean onlyRdf4jShaclShapeGraph = shapesGraphs.length == 1
					&& RDF4J.SHACL_SHAPE_GRAPH.equals(shapesGraphs[0]);

			return getShapes(shapesGraphs, onlyRdf4jShaclShapeGraph);
		});

		try {
			cachedShapes.warmUp();
		} catch (InterruptedException e) {
			throw convertToSailException(e);

		}

	}

	@InternalUseOnly
	public List<ContextWithShapes> getShapes(RepositoryConnection shapesRepoConnection, SailConnection sailConnection,
			IRI[] shapesGraphs) throws SailException {

		try (ShapeSource shapeSource = new CombinedShapeSource(shapesRepoConnection, sailConnection)
				.withContext(shapesGraphs)) {
			return Shape.Factory.getShapes(shapeSource, this);
		}

	}

	@InternalUseOnly
	public List<ContextWithShapes> getShapes(RepositoryConnection shapesRepoConnection, IRI[] shapesGraphs)
			throws SailException {

		try (ShapeSource shapeSource = new ForwardChainingShapeSource(shapesRepoConnection).withContext(shapesGraphs)) {
			return Shape.Factory.getShapes(shapeSource, this);
		}

	}

	@Override
	public synchronized void shutDown() throws SailException {
		if (shapesRepo != null) {
			shapesRepo.shutDown();
			shapesRepo = null;
		}

		cachedShapes = null;

		boolean terminated = shutdownExecutorService(false);

		initialized.set(false);
		super.shutDown();

		if (!terminated) {
			shutdownExecutorService(true);
		}
	}

	private boolean shutdownExecutorService(boolean forced) {
		boolean terminated = false;

		executorService.shutdown();
		try {
			terminated = executorService.awaitTermination(200, TimeUnit.MILLISECONDS);
		} catch (InterruptedException ignored) {
			Thread.currentThread().interrupt();
		}

		if (forced && !terminated) {
			executorService.shutdownNow();
			logger.error("Shutdown ShaclSail while validation is still running.");
			terminated = true;
		}

		return terminated;

	}

	<T> Future<T> submitToExecutorService(Callable<T> runnable) {
		return executorService.submit(runnable);
	}

	@Override
	public NotifyingSailConnection getConnection() throws SailException {
		init();

		synchronized (singleConnectionMonitor) {
			singleConnectionCounter.incrementAndGet();
		}

		try {
			NotifyingSailConnection connection = super.getConnection();
			if (connection instanceof MemoryStoreConnection) {
				if (isSerializableValidation()) {
					return new ShaclSailConnection(this, connection, super.getConnection(), shapesRepo.getConnection(),
							super.getConnection());
				} else {
					return new ShaclSailConnection(this, connection, super.getConnection(), shapesRepo.getConnection());
				}
			} else if (isSerializableValidation()) {
				return new ShaclSailConnection(this, connection, shapesRepo.getConnection(), super.getConnection());
			} else {
				return new ShaclSailConnection(this, connection, shapesRepo.getConnection());
			}
		} catch (Throwable t) {
			singleConnectionCounter.decrementAndGet();
			throw t;
		}

	}

	@InternalUseOnly
	public List<ContextWithShapes> getShapes(IRI[] shapesGraphs, boolean onlyRdf4jShaclShapeGraph) {

		try (SailRepositoryConnection shapesRepoConnection = shapesRepo.getConnection()) {
			shapesRepoConnection.begin(IsolationLevels.READ_COMMITTED);
			try {
				if (onlyRdf4jShaclShapeGraph) {
					return getShapes(shapesRepoConnection, shapesGraphs);
				} else {
					try (NotifyingSailConnection sailConnection = getBaseSail().getConnection()) {
						sailConnection.begin(IsolationLevels.READ_COMMITTED);
						try {
							return getShapes(shapesRepoConnection, sailConnection, shapesGraphs);
						} finally {
							sailConnection.rollback();
						}
					}
				}
			} finally {
				shapesRepoConnection.rollback();
			}

		}
	}

	public void setShapesGraphs(Set<IRI> shapesGraphs) {
		if (initialized.get()) {
			try {
				try (StampedLockManager.Cache<List<ContextWithShapes>>.WritableState writeState = cachedShapes
						.getWriteState()) {
					super.setShapesGraphs(shapesGraphs);
					writeState.purge();
				}
			} catch (InterruptedException e) {
				throw convertToSailException(e);
			}
		} else {
			super.setShapesGraphs(shapesGraphs);
		}
	}

	@Override
	public boolean isSerializableValidation() {
		if (!supportsSnapshotIsolation && super.isSerializableValidation()) {
			if (logger.isDebugEnabled()) {
				logger.debug(
						"Serializable validation is enabled but can not be used because the base sail does not support IsolationLevels.SNAPSHOT!");
			}
		}

		return supportsSnapshotIsolation && super.isSerializableValidation();
	}

	static SailException convertToSailException(InterruptedException e) {
		Thread.currentThread().interrupt();
		return new SailException(e);
	}

	public static class TransactionSettings {

		@Experimental
		public enum PerformanceHint implements TransactionSetting {

			/**
			 * Run validation is parallel (multithreaded).
			 */
			ParallelValidation("ParallelValidation"),
			/**
			 * Run validation serially (single threaded)
			 */
			SerialValidation("SerialValidation"),
			/**
			 * Cache intermediate results. Uses more memory but can reduce validation time.
			 */
			CacheEnabled("CacheEnabled"),
			/**
			 * Do not cache intermediate results.
			 */
			CacheDisabled("CacheDisabled");

			private final String value;

			PerformanceHint(String value) {
				this.value = value;
			}

			@Override
			public String getName() {
				return PerformanceHint.class.getCanonicalName();
			}

			@Override
			public String getValue() {
				return value;
			}

		}

		public enum ValidationApproach implements TransactionSetting {

			/**
			 * Do not run any validation. This could potentially lead to your database becoming invalid.
			 */
			Disabled("Disabled", 0),

			/**
			 * Use a validation approach that is optimized for bulk operations such as adding or removing large amounts
			 * of data. This will automatically disable parallel validation and turn off caching. Add performance hints
			 * to enable parallel validation or caching if you have enough resources (RAM).
			 */
			Bulk("Bulk", 1),

			/**
			 * Let the SHACL engine decide on the best approach for validating. This typically means that it will use
			 * transactional validation except when changing the SHACL Shape.
			 */
			Auto("Auto", 2);

			private final String value;

			// lowest priority wins
			private final int priority;

			ValidationApproach(String value, int priority) {
				this.value = value;
				this.priority = priority;
			}

			@Override
			public String getName() {
				return ValidationApproach.class.getCanonicalName();
			}

			@Override
			public String getValue() {
				return value;
			}

			public static ValidationApproach getHighestPriority(ValidationApproach v1, ValidationApproach v2) {
				assert v1 != null || v2 != null;
				if (v1 == null) {
					return v2;
				}
				if (v2 == null) {
					return v1;
				}

				if (v1.priority < v2.priority) {
					return v1;
				}

				return v2;
			}

		}

		private final String value;

		TransactionSettings(String value) {
			this.value = value;
		}

		public String getValue() {
			return value;
		}
	}

	/**
	 * @implNote This is an extension point for configuring a different executor service for parallel validation. The
	 *           code is marked as experimental because it may change from one minor release to another.
	 * @return
	 */
	@Experimental
	@SuppressWarnings("NullableProblems")
	protected static class RevivableExecutorService implements ExecutorService {
		private final Supplier<ExecutorService> supplier;
		ExecutorService delegate;
		boolean initialized = false;

		public RevivableExecutorService(Supplier<ExecutorService> supplier) {
			this.supplier = supplier;
		}

		public void init() {
			assert delegate == null || delegate.isTerminated();
			delegate = supplier.get();
			initialized = true;
		}

		@Override
		public void shutdown() {
			if (initialized) {
				delegate.shutdown();
			}
		}

		@Override
		public List<Runnable> shutdownNow() {
			if (initialized) {
				return delegate.shutdownNow();
			} else {
				return Collections.emptyList();
			}
		}

		@Override
		public boolean isShutdown() {
			return !initialized || delegate.isShutdown();
		}

		@Override
		public boolean isTerminated() {
			return !initialized || delegate.isTerminated();
		}

		@Override
		public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
			return !initialized || delegate.awaitTermination(timeout, unit);
		}

		@Override
		public <T> Future<T> submit(Callable<T> task) {
			assert initialized;
			return delegate.submit(task);
		}

		@Override
		public <T> Future<T> submit(Runnable task, T result) {
			assert initialized;
			return delegate.submit(task, result);
		}

		@Override
		public Future<?> submit(Runnable task) {
			assert initialized;
			return delegate.submit(task);
		}

		@Override
		public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
			assert initialized;
			return delegate.invokeAll(tasks);
		}

		@Override
		public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
				throws InterruptedException {
			assert initialized;
			return delegate.invokeAll(tasks, timeout, unit);
		}

		@Override
		public <T> T invokeAny(Collection<? extends Callable<T>> tasks)
				throws InterruptedException, ExecutionException {
			assert initialized;
			return delegate.invokeAny(tasks);
		}

		@Override
		public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
				throws InterruptedException, ExecutionException, TimeoutException {
			assert initialized;
			return delegate.invokeAny(tasks, timeout, unit);
		}

		@Override
		public void execute(Runnable command) {
			assert initialized;
			delegate.execute(command);
		}
	}

}
