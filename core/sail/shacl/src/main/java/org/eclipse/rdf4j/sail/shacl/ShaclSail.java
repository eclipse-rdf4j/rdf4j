/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl;

import org.apache.commons.io.IOUtils;
import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.common.concurrent.locks.Lock;
import org.eclipse.rdf4j.common.concurrent.locks.ReadPrefReadWriteLockManager;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.RDF4J;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.NotifyingSail;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.SailConflictException;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.helpers.NotifyingSailWrapper;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.shacl.AST.NodeShape;
import org.eclipse.rdf4j.sail.shacl.config.ShaclSailConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

//@formatter:off
/**
 * A {@link Sail} implementation that adds support for the Shapes Constraint Language (SHACL).
 * <p>
 * The ShaclSail looks for SHACL shape data in a special named graph {@link RDF4J#SHACL_SHAPE_GRAPH}.
 * <h4>Working example</h4>
 * <p>
 *
 * <pre>
 * import ch.qos.logback.classic.Level;
 * import ch.qos.logback.classic.Logger;
 * import org.eclipse.rdf4j.model.Model;
 * import org.eclipse.rdf4j.model.vocabulary.RDF4J;
 * import org.eclipse.rdf4j.repository.RepositoryException;
 * import org.eclipse.rdf4j.repository.sail.SailRepository;
 * import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
 * import org.eclipse.rdf4j.rio.RDFFormat;
 * import org.eclipse.rdf4j.rio.Rio;
 * import org.eclipse.rdf4j.sail.memory.MemoryStore;
 * import org.eclipse.rdf4j.sail.shacl.ShaclSail;
 * import org.eclipse.rdf4j.sail.shacl.ShaclSailValidationException;
 * import org.eclipse.rdf4j.sail.shacl.results.ValidationReport;
 * import org.slf4j.LoggerFactory;
 *
 * import java.io.IOException;
 * import java.io.StringReader;
 *
 * public class ShaclSampleCode {
 *
 *  	public static void main(String[] args) throws IOException {
 *
 *  		ShaclSail shaclSail = new ShaclSail(new MemoryStore());
 *
 *  		// Logger root = (Logger) LoggerFactory.getLogger(ShaclSail.class.getName());
 *  		// root.setLevel(Level.INFO);
 *
 *  		// shaclSail.setLogValidationPlans(true);
 *  		// shaclSail.setGlobalLogValidationExecution(true);
 *  		// shaclSail.setLogValidationViolations(true);
 *
 *  		SailRepository sailRepository = new SailRepository(shaclSail);
 *  		sailRepository.init();
 *
 *  		try (SailRepositoryConnection connection = sailRepository.getConnection()) {
 *
 *  			connection.begin();
 *
 *  			StringReader shaclRules = new StringReader(String.join("\n", "",
 *  				"@prefix ex: <http://example.com/ns#> .",
 *  				"@prefix sh: <http://www.w3.org/ns/shacl#> .",
 *  				"@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .",
 *  				"@prefix foaf: <http://xmlns.com/foaf/0.1/>.",
 *
 *  				"ex:PersonShape",
 *  				"	a sh:NodeShape  ;",
 *  				"	sh:targetClass foaf:Person ;",
 *  				"	sh:property ex:PersonShapeProperty .",
 *
 *  				"ex:PersonShapeProperty ",
 *  				"	sh:path foaf:age ;",
 *  				"	sh:datatype xsd:int ;",
 *  				"  sh:maxCount 1 ;",
 *  				"  sh:minCount 1 ."));
 *
 *  			connection.add(shaclRules, "", RDFFormat.TURTLE, RDF4J.SHACL_SHAPE_GRAPH);
 *  			connection.commit();
 *
 *  			connection.begin();
 *
 *  			StringReader invalidSampleData = new StringReader(String.join("\n", "",
 *  				"@prefix ex: <http://example.com/ns#> .",
 *  				"@prefix foaf: <http://xmlns.com/foaf/0.1/>.",
 *  				"@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .",
 *
 *  				"ex:peter a foaf:Person ;",
 *  				"	foaf:age 20, \"30\"^^xsd:int  ."
 *
 *  			));
 *
 *  			connection.add(invalidSampleData, "", RDFFormat.TURTLE);
 *  			try {
 *  				connection.commit();
 *  			} catch (RepositoryException exception) {
 *  				Throwable cause = exception.getCause();
 *  				if (cause instanceof ShaclSailValidationException) {
 *  					ValidationReport validationReport = ((ShaclSailValidationException) cause).getValidationReport();
 *  					Model validationReportModel = ((ShaclSailValidationException) cause).validationReportAsModel();
 *  					// use validationReport or validationReportModel to understand validation violations
 *
 *  					Rio.write(validationReportModel, System.out, RDFFormat.TURTLE);
 *  				}
 *  				throw exception;
 *  			}
 *  		}
 *  	}
 * }
 * </pre>
 *
 * @author Heshan Jayasinghe
 * @author Håvard Ottestad
 * @see <a href="https://www.w3.org/TR/shacl/">SHACL W3C Recommendation</a>
 */
//@formatter:on
public class ShaclSail extends NotifyingSailWrapper {

	private static final Logger logger = LoggerFactory.getLogger(ShaclSail.class);

	private List<NodeShape> nodeShapes = Collections.emptyList();

	private static String IMPLICIT_TARGET_CLASS_NODE_SHAPE;
	private static String IMPLICIT_TARGET_CLASS_PROPERTY_SHAPE;
	private static String PROPERTY_SHAPE_WITH_TARGET;

	/**
	 * an initialized {@link Repository} for storing/retrieving Shapes data
	 */
	private SailRepository shapesRepo;

	// lockManager used for read/write locks used to synchronize changes to the shapes (and caching of shapes) and used
	// to synchronize validation so that SNAPSHOT isolation is sufficient to achieve SERIALIZABLE isolation wrt.
	// validation
	final private ReadPrefReadWriteLockManager lockManager = new ReadPrefReadWriteLockManager();

	transient private Thread threadHoldingWriteLock;

	private boolean parallelValidation = ShaclSailConfig.PARALLEL_VALIDATION_DEFAULT;
	private boolean undefinedTargetValidatesAllSubjects = ShaclSailConfig.UNDEFINED_TARGET_VALIDATES_ALL_SUBJECTS_DEFAULT;
	private boolean logValidationPlans = ShaclSailConfig.LOG_VALIDATION_PLANS_DEFAULT;
	private boolean logValidationViolations = ShaclSailConfig.LOG_VALIDATION_VIOLATIONS_DEFAULT;
	private boolean ignoreNoShapesLoadedException = ShaclSailConfig.IGNORE_NO_SHAPES_LOADED_EXCEPTION_DEFAULT;
	private boolean validationEnabled = ShaclSailConfig.VALIDATION_ENABLED_DEFAULT;
	private boolean cacheSelectNodes = ShaclSailConfig.CACHE_SELECT_NODES_DEFAULT;
	private boolean rdfsSubClassReasoning = ShaclSailConfig.RDFS_SUB_CLASS_REASONING_DEFAULT;
	private boolean serializableValidation = ShaclSailConfig.SERIALIZABLE_VALIDATION_DEFAULT;
	private boolean performanceLogging = ShaclSailConfig.PERFORMANCE_LOGGING_DEFAULT;

	static {
		try {
			IMPLICIT_TARGET_CLASS_NODE_SHAPE = resourceAsString(
					"shacl-sparql-inference/implicitTargetClassNodeShape.rq");
			IMPLICIT_TARGET_CLASS_PROPERTY_SHAPE = resourceAsString(
					"shacl-sparql-inference/implicitTargetClassPropertyShape.rq");
			PROPERTY_SHAPE_WITH_TARGET = resourceAsString(
					"shacl-sparql-inference/propertyShapeWithTarget.rq");
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}

	}

	private final ExecutorService[] executorService = new ExecutorService[1];

	public ShaclSail(NotifyingSail baseSail) {
		super(baseSail);
		ReferenceQueue<ShaclSail> objectReferenceQueue = new ReferenceQueue<>();
		startMonitoring(objectReferenceQueue, new PhantomReference<>(this, objectReferenceQueue), initialized,
				executorService);
	}

	public ShaclSail() {
		super();
		ReferenceQueue<ShaclSail> objectReferenceQueue = new ReferenceQueue<>();
		startMonitoring(objectReferenceQueue, new PhantomReference<>(this, objectReferenceQueue), initialized,
				executorService);
	}

	// This is used to keep track of the current connection, if the opening and closing of connections is done serially.
	// If it is done in parallel, then this will catch that and the multipleConcurrentConnections == true.
	private transient ShaclSailConnection currentConnection;
	private transient boolean multipleConcurrentConnections;

	synchronized void closeConnection(ShaclSailConnection connection) {
		if (connection == currentConnection) {
			currentConnection = null;
		}
	}

	synchronized boolean usesSingleConnection() {
//		return false; // if this method returns false, then the connection will always use the new serializable validation
		return !multipleConcurrentConnections;
	}

	/**
	 * Lists the predicates that have been implemented in the ShaclSail. All of these, and all combinations,
	 * <i>should</i> work, please report any bugs. For sh:path, only single predicate paths are supported.
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
				SHACL.TARGET_OBJECTS_OF);
	}

	private final AtomicBoolean initialized = new AtomicBoolean(false);

	@Override
	public void initialize() throws SailException {
		if (!initialized.compareAndSet(false, true)) {
			// already initialized
			return;
		}

		super.initialize();

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
			nodeShapes = refreshShapes(shapesRepoConnection);
			shapesRepoConnection.commit();
		}

		assert executorService[0] == null;

	}

	List<NodeShape> refreshShapes(SailRepositoryConnection shapesRepoConnection) throws SailException {

		SailRepository shapesRepoCache = new SailRepository(new MemoryStore());
		shapesRepoCache.init();
		List<NodeShape> shapes;

		try (SailRepositoryConnection shapesRepoCacheConnection = shapesRepoCache.getConnection()) {
			shapesRepoCacheConnection.begin(IsolationLevels.NONE);
			try (RepositoryResult<Statement> statements = shapesRepoConnection.getStatements(null, null, null, false)) {
				shapesRepoCacheConnection.add(statements);
			}

			runInferencingSparqlQueries(shapesRepoCacheConnection);
			shapesRepoCacheConnection.commit();

			shapes = NodeShape.Factory.getShapes(shapesRepoCacheConnection, this);
		}

		shapesRepoCache.shutDown();
		return shapes;
	}

	@Override
	public synchronized void shutDown() throws SailException {
		if (shapesRepo != null) {
			shapesRepo.shutDown();
			shapesRepo = null;
		}

		if (executorService[0] != null) {
			executorService[0].shutdown();
			boolean terminated = false;
			try {
				terminated = executorService[0].awaitTermination(200, TimeUnit.MILLISECONDS);
			} catch (InterruptedException ignored) {
			}

			if (!terminated) {
				executorService[0].shutdownNow();
				logger.error("Shutdown ShaclSail while validation is still running.");
			}

		}

		initialized.set(false);
		executorService[0] = null;
		nodeShapes = Collections.emptyList();
		super.shutDown();
	}

	synchronized <T> Future<T> submitRunnableToExecutorService(Callable<T> runnable) {
		if (executorService[0] == null) {
			executorService[0] = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2,
					r -> {
						Thread t = Executors.defaultThreadFactory().newThread(r);
						// this thread pool does not need to stick around if the all other threads are done, because it
						// is only used for SHACL validation and if all other threads have ended then there would be no
						// thread to receive the validation results.
						t.setDaemon(true);
						return t;
					});
		}
		return executorService[0].submit(runnable);
	}

	@Override
	public NotifyingSailConnection getConnection() throws SailException {

		ShaclSailConnection shaclSailConnection = new ShaclSailConnection(this, super.getConnection(),
				super.getConnection(), super.getConnection(), super.getConnection(),
				shapesRepo.getConnection());

		// don't synchronize the entire method, because this can cause a deadlock when trying to get a new connection
		// while at the same time closing another connection
		synchronized (this) {
			if (currentConnection == null) {
				currentConnection = shaclSailConnection;
			} else {
				multipleConcurrentConnections = true;
			}
		}

		return shaclSailConnection;
	}

	List<NodeShape> getNodeShapes() {
		return nodeShapes;
	}

	private void runInferencingSparqlQueries(SailRepositoryConnection shaclSailConnection) {

		// performance optimisation, running the queries below is time-consuming, even if the repo is empty
		if (shaclSailConnection.isEmpty()) {
			return;
		}

		long prevSize;
		long currentSize = shaclSailConnection.size();
		do {
			prevSize = currentSize;
			shaclSailConnection.prepareUpdate(IMPLICIT_TARGET_CLASS_PROPERTY_SHAPE).execute();
			shaclSailConnection.prepareUpdate(IMPLICIT_TARGET_CLASS_NODE_SHAPE).execute();
			shaclSailConnection.prepareUpdate(PROPERTY_SHAPE_WITH_TARGET).execute();
			currentSize = shaclSailConnection.size();
		} while (prevSize != currentSize);

	}

	/**
	 * Tries to obtain an exclusive write lock on this store. This method will block until either the lock is obtained
	 * or an interrupt signal is received.
	 *
	 * @throws SailException if the thread is interrupted while waiting to obtain the lock.
	 */
	Lock acquireExclusiveWriteLock(Lock lock) {

		if (lock != null && lock.isActive()) {
			return lock;
		}

		assert lock == null;

		if (threadHoldingWriteLock == Thread.currentThread()) {
			throw new SailConflictException(
					"Deadlock detected when a single thread uses multiple connections " +
							"interleaved and one connection has modified the shapes without calling commit() " +
							"while another connection also tries to modify the shapes!");
		}

		try {
			Lock writeLock = lockManager.getWriteLock();
			threadHoldingWriteLock = Thread.currentThread();
			return writeLock;
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Releases the exclusive write lock.
	 *
	 * @return
	 */
	Lock releaseExclusiveWriteLock(Lock lock) {
		assert lock != null;
		threadHoldingWriteLock = null;
		lock.release();
		return null;
	}

	Lock acquireReadlock() {
		if (threadHoldingWriteLock == Thread.currentThread()) {
			throw new SailConflictException(
					"Deadlock detected when a single thread uses multiple connections " +
							"interleaved and one connection has modified the shapes without calling commit() " +
							"while another connection calls commit()!");
		}
		try {
			return lockManager.getReadLock();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}

	}

	Lock releaseReadlock(Lock lock) {
		assert lock != null;

		lock.release();

		return null;
	}

//	Lock convertToReadLock(Lock writeLockStamp) {
//		assert writeLockStamp != null;
//
//		writeLockStamp.release();
//
//		Lock readLock = null;
//		while (readLock == null) {
//			try {
//				readLock = lockManager.getReadLock();
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//		}
//
//		System.out.println("convertToReadLock");
//		return readLock;
//
//
//	}

	void setNodeShapes(List<NodeShape> nodeShapes) {
		this.nodeShapes = nodeShapes;
	}

	/**
	 * Log (INFO) every execution step of the SHACL validation. This is fairly costly and should not be used in
	 * production. Recommended to disable parallel validation with setParallelValidation(false)
	 *
	 * @param loggingEnabled
	 */
	public void setGlobalLogValidationExecution(boolean loggingEnabled) {
		GlobalValidationExecutionLogging.loggingEnabled = loggingEnabled;
	}

	/**
	 * Check if logging of every execution steps is enabled.
	 *
	 * @return <code>true</code> if enabled, <code>false</code> otherwise.
	 * @see #setGlobalLogValidationExecution(boolean)
	 */
	public boolean isGlobalLogValidationExecution() {
		return GlobalValidationExecutionLogging.loggingEnabled;
	}

	/**
	 * Check if logging a list of violations and the triples that caused the violations is enabled. It is recommended to
	 * disable parallel validation with {@link #setParallelValidation(boolean)}
	 *
	 * @see #setLogValidationViolations(boolean)
	 */
	public boolean isLogValidationViolations() {
		return this.logValidationViolations;
	}

	/**
	 * Log (INFO) a list of violations and the triples that caused the violations (BETA). Recommended to disable
	 * parallel validation with setParallelValidation(false)
	 *
	 * @param logValidationViolations
	 */
	public void setLogValidationViolations(boolean logValidationViolations) {
		this.logValidationViolations = logValidationViolations;
	}

	/**
	 * If no target is defined for a NodeShape, that NodeShape will be ignored. Calling this method with "true" will
	 * make such NodeShapes wildcard shapes and validate all subjects. Equivalent to setting sh:targetClass to owl:Thing
	 * or rdfs:Resource in an environment with a reasoner.
	 *
	 * @param undefinedTargetValidatesAllSubjects default false
	 */
	public void setUndefinedTargetValidatesAllSubjects(boolean undefinedTargetValidatesAllSubjects) {
		this.undefinedTargetValidatesAllSubjects = undefinedTargetValidatesAllSubjects;
	}

	/**
	 * Check if {@link NodeShape}s without a defined target are considered wildcards.
	 *
	 * @return <code>true</code> if enabled, <code>false</code> otherwise
	 * @see #setUndefinedTargetValidatesAllSubjects(boolean)
	 */
	public boolean isUndefinedTargetValidatesAllSubjects() {
		return this.undefinedTargetValidatesAllSubjects;
	}

	/**
	 * Check if SHACL validation is run in parellel.
	 *
	 * @return <code>true</code> if enabled, <code>false</code> otherwise.
	 */
	public boolean isParallelValidation() {
		return this.parallelValidation;
	}

	/**
	 * EXPERIMENTAL! Run SHACL validation in parallel. Default: false
	 * <p>
	 * May cause deadlock, especially when using NativeStore.
	 *
	 * @param parallelValidation default true
	 */
	public void setParallelValidation(boolean parallelValidation) {
		this.parallelValidation = parallelValidation;
	}

	/**
	 * Check if selected nodes caches is enabled.
	 *
	 * @return <code>true</code> if enabled, <code>false</code> otherwise.
	 * @see #setCacheSelectNodes(boolean)
	 */
	public boolean isCacheSelectNodes() {
		return this.cacheSelectNodes;
	}

	/**
	 * The ShaclSail retries a lot of its relevant data through running SPARQL Select queries against the underlying
	 * sail and against the changes in the transaction. This is usually good for performance, but while validating large
	 * amounts of data disabling this cache will use less memory. Default: true
	 *
	 * @param cacheSelectNodes default true
	 */
	public void setCacheSelectNodes(boolean cacheSelectNodes) {
		this.cacheSelectNodes = cacheSelectNodes;
	}

	public boolean isRdfsSubClassReasoning() {
		return rdfsSubClassReasoning;
	}

	public void setRdfsSubClassReasoning(boolean rdfsSubClassReasoning) {
		this.rdfsSubClassReasoning = rdfsSubClassReasoning;
	}

	/**
	 * Disable the SHACL validation on commit()
	 */
	public void disableValidation() {
		this.validationEnabled = false;
	}

	/**
	 * Enabled the SHACL validation on commit()
	 */
	public void enableValidation() {
		this.validationEnabled = true;
	}

	/**
	 * Check if SHACL validation on commit() is enabled.
	 *
	 * @return <code>true</code> if validation is enabled, <code>false</code> otherwise.
	 */
	public boolean isValidationEnabled() {
		return validationEnabled;
	}

	/**
	 * Check if logging of validation plans is enabled.
	 *
	 * @return <code>true</code> if validation plan logging is enabled, <code>false</code> otherwise.
	 */
	public boolean isLogValidationPlans() {
		return this.logValidationPlans;
	}

	public boolean isIgnoreNoShapesLoadedException() {
		return this.ignoreNoShapesLoadedException;
	}

	/**
	 * Check if shapes have been loaded into the shapes graph before other data is added
	 *
	 * @param ignoreNoShapesLoadedException
	 */
	public void setIgnoreNoShapesLoadedException(boolean ignoreNoShapesLoadedException) {
		this.ignoreNoShapesLoadedException = ignoreNoShapesLoadedException;
	}

	/**
	 * Log (INFO) the executed validation plans as GraphViz DOT Recommended to disable parallel validation with
	 * setParallelValidation(false)
	 *
	 * @param logValidationPlans
	 */
	public void setLogValidationPlans(boolean logValidationPlans) {
		this.logValidationPlans = logValidationPlans;
	}

	public boolean isPerformanceLogging() {
		return performanceLogging;
	}

	// @formatter:off

	/**
	 * Log (INFO) the execution time per shape. Recommended to disable the following:
	 * <ul>
	 * 		<li>setParallelValidation(false)</li>
	 * 		<li>setCacheSelectNodes(false)</li>
	 * </ul>
	 *
	 * @param performanceLogging default false
	 */
	// @formatter:on
	public void setPerformanceLogging(boolean performanceLogging) {
		this.performanceLogging = performanceLogging;
	}

	/**
	 * On transactions using SNAPSHOT isolation the ShaclSail can run the validation serializably. This stops the sail
	 * from becoming inconsistent due to race conditions between two transactions. Serializable validation limits TPS
	 * (transactions per second), it is however considerably faster than actually using SERIALIZABLE isolation.
	 *
	 * @return <code>true</code> if serializable validation is enabled, <code>false</code> otherwise.
	 */
	public boolean isSerializableValidation() {
		return serializableValidation;
	}

	/**
	 * Enable or disable serializable validation.On transactions using SNAPSHOT isolation the ShaclSail can run the
	 * validation serializably. This stops the sail from becoming inconsistent due to race conditions between two
	 * transactions. Serializable validation limits TPS (transactions per second), it is however considerably faster
	 * than actually using SERIALIZABLE isolation.
	 *
	 * <p>
	 * To increase TPS, serializable validation can be disabled. Validation will then be limited to the semantics of the
	 * SNAPSHOT isolation level (or whichever is specified). If you use any other isolation level than SNAPSHOT,
	 * disabling serializable validation will make no difference on performance.
	 * </p>
	 *
	 * @param serializableValidation default true
	 */
	public void setSerializableValidation(boolean serializableValidation) {
		this.serializableValidation = serializableValidation;
	}

	private static String resourceAsString(String s) throws IOException {
		return IOUtils.toString(Objects.requireNonNull(ShaclSail.class.getClassLoader().getResourceAsStream(s)),
				StandardCharsets.UTF_8);
	}

	private void startMonitoring(ReferenceQueue<ShaclSail> referenceQueue, Reference<ShaclSail> ref,
			AtomicBoolean initialized, ExecutorService[] executorService) {

		ExecutorService ex = Executors.newSingleThreadExecutor(r -> {
			Thread t = Executors.defaultThreadFactory().newThread(r);
			// this thread pool does not need to stick around if the all other threads are done
			t.setDaemon(true);
			return t;
		});

		ex.execute(() -> {
			while (referenceQueue.poll() != ref) {
				// don't hang forever
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					// should never be interrupted
					break;
				}
			}

			if (ref.get() != null) {
				// we were apparently interrupted before the object was set to be finalized
				return;
			}

			if (initialized.get()) {
				logger.error("ShaclSail was garbage collected without shutdown() having been called first.");
			}
			if (executorService[0] != null) {
				executorService[0].shutdownNow();
			}

		});
		// this is a soft operation, the thread pool will actually wait until the task above has completed
		ex.shutdown();
	}
}
