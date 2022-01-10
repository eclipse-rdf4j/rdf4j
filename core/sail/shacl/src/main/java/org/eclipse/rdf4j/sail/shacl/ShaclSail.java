/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl;

import static org.eclipse.rdf4j.model.util.Values.iri;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.Cleaner;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import org.eclipse.rdf4j.common.annotation.Experimental;
import org.eclipse.rdf4j.common.annotation.InternalUseOnly;
import org.eclipse.rdf4j.common.concurrent.locks.Lock;
import org.eclipse.rdf4j.common.concurrent.locks.ReadPrefReadWriteLockManager;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.common.transaction.TransactionSetting;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.DASH;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDF4J;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.RSX;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.NotifyingSail;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.SailConflictException;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.inferencer.fc.SchemaCachingRDFSInferencer;
import org.eclipse.rdf4j.sail.inferencer.fc.SchemaCachingRDFSInferencerConnection;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.shacl.ast.Shape;
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

	private static final Logger logger = LoggerFactory.getLogger(ShaclSail.class);
	private static final Cleaner cleaner = Cleaner.create();

	private static final Model DASH_CONSTANTS = resourceAsModel("shacl-sparql-inference/dashConstants.ttl");

	// SHACL Vocabulary from W3C - https://www.w3.org/ns/shacl.ttl
	private final static IRI shaclVocabularyGraph = iri(RDF4J.NAMESPACE, "shaclVocabularyGraph");
	private final static SchemaCachingRDFSInferencer shaclVocabulary = createShaclVocabulary();

	/**
	 * an initialized {@link Repository} for storing/retrieving Shapes data
	 */
	private SailRepository shapesRepo;

	// lockManager used for read/write locks used to synchronize changes to the shapes (and caching of shapes) and used
	// to synchronize validation so that SNAPSHOT isolation is sufficient to achieve SERIALIZABLE isolation wrt.
	// validation
	final private ReadPrefReadWriteLockManager lockManager = new ReadPrefReadWriteLockManager();

	// This is used to keep track of the current connection, if the opening and closing of connections is done serially.
	// If it is done in parallel, then this will catch that and the multipleConcurrentConnections == true.
	private transient ShaclSailConnection currentConnection;
	private transient boolean multipleConcurrentConnections;

	transient private Thread threadHoldingWriteLock;

	private final AtomicBoolean initialized = new AtomicBoolean(false);

	private final ExecutorService[] executorService = new ExecutorService[1];

	static class CleanableState implements Runnable {

		private final AtomicBoolean initialized;
		private final ExecutorService[] executorService;

		CleanableState(AtomicBoolean initialized, ExecutorService[] executorService) {

			this.initialized = initialized;
			this.executorService = executorService;
		}

		public void run() {
			if (initialized.get()) {
				logger.error("ShaclSail was garbage collected without shutdown() having been called first.");
			}
			if (executorService[0] != null) {
				executorService[0].shutdownNow();
			}
		}
	}

	public ShaclSail(NotifyingSail baseSail) {
		super(baseSail);
		cleaner.register(this, new CleanableState(initialized, executorService));
	}

	public ShaclSail() {
		super();
		cleaner.register(this, new CleanableState(initialized, executorService));
	}

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

		assert executorService[0] == null;

	}

	@InternalUseOnly
	public List<Shape> getShapes(RepositoryConnection shapesRepoConnection) throws SailException {
		SailRepository shapesRepoWithReasoning = new SailRepository(
				SchemaCachingRDFSInferencer.fastInstantiateFrom(shaclVocabulary, new MemoryStore(), false));

		shapesRepoWithReasoning.init();
		List<Shape> shapes;

		try (SailRepositoryConnection shapesRepoWithReasoningConnection = shapesRepoWithReasoning.getConnection()) {
			shapesRepoWithReasoningConnection.begin(IsolationLevels.NONE);
			try (RepositoryResult<Statement> statements = shapesRepoConnection.getStatements(null, null, null,
					false)) {
				shapesRepoWithReasoningConnection.add(statements);
			}
			enrichShapes(shapesRepoWithReasoningConnection);
			shapesRepoWithReasoningConnection.commit();
			shapes = Shape.Factory.getShapes(shapesRepoWithReasoningConnection, this);
		}

		shapesRepoWithReasoning.shutDown();
		return shapes;
	}

	@Override
	public synchronized void shutDown() throws SailException {
		if (shapesRepo != null) {
			shapesRepo.shutDown();
			shapesRepo = null;
		}

		boolean terminated = shutdownExecutorService(false);

		initialized.set(false);
		super.shutDown();

		if (!terminated) {
			shutdownExecutorService(true);
		}

		executorService[0] = null;
	}

	private boolean shutdownExecutorService(boolean forced) {
		if (executorService[0] != null) {
			boolean terminated = false;

			executorService[0].shutdown();
			try {
				terminated = executorService[0].awaitTermination(200, TimeUnit.MILLISECONDS);
			} catch (InterruptedException ignored) {
			}

			if (forced && !terminated) {
				executorService[0].shutdownNow();
				logger.error("Shutdown ShaclSail while validation is still running.");
				terminated = true;
			}

			return terminated;
		}
		return true;
	}

	<T> Future<T> submitRunnableToExecutorService(Callable<T> runnable) {
		if (executorService[0] == null) {
			synchronized (this) {
				if (executorService[0] == null) {
					executorService[0] = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2,
							r -> {
								Thread t = Executors.defaultThreadFactory().newThread(r);
								// this thread pool does not need to stick around if the all other threads are done,
								// because it
								// is only used for SHACL validation and if all other threads have ended then there
								// would be no
								// thread to receive the validation results.
								t.setDaemon(true);
								return t;
							});
				}
			}
		}
		return executorService[0].submit(runnable);
	}

	@Override
	public NotifyingSailConnection getConnection() throws SailException {
		init();

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

	private void enrichShapes(SailRepositoryConnection shaclSailConnection) {

		// performance optimisation, running the queries below is time-consuming, even if the repo is empty
		if (shaclSailConnection.isEmpty()) {
			return;
		}

		shaclSailConnection.add(DASH_CONSTANTS);
		implicitTargetClass(shaclSailConnection);

	}

	private void implicitTargetClass(SailRepositoryConnection shaclSailConnection) {
		try (Stream<Statement> stream = shaclSailConnection.getStatements(null, RDF.TYPE, RDFS.CLASS, false).stream()) {
			stream
					.map(Statement::getSubject)
					.filter(s ->

					shaclSailConnection.hasStatement(s, RDF.TYPE, SHACL.NODE_SHAPE, true)
							|| shaclSailConnection.hasStatement(s, RDF.TYPE, SHACL.PROPERTY_SHAPE, true)
					)
					.forEach(s -> {
						shaclSailConnection.add(s, SHACL.TARGET_CLASS, s);
					});
		}
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
		threadHoldingWriteLock = null;
		lock.release();
		return null;
	}

	Lock acquireReadLock() {
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

	Lock releaseReadLock(Lock lock) {
		lock.release();
		return null;
	}

	@InternalUseOnly
	public List<Shape> getCurrentShapes() {
		try (SailRepositoryConnection connection = shapesRepo.getConnection()) {
			return getShapes(connection);
		}
	}

	boolean hasShapes() {
		try (SailRepositoryConnection connection = shapesRepo.getConnection()) {
			connection.begin(IsolationLevels.NONE);
			try {
				return !connection.isEmpty();
			} finally {
				connection.commit();
			}
		}
	}

	private static SchemaCachingRDFSInferencer createShaclVocabulary() {
		try (InputStream in = getResourceAsStream("shacl-sparql-inference/shaclVocabulary.ttl")) {
			SchemaCachingRDFSInferencer schemaCachingRDFSInferencer = new SchemaCachingRDFSInferencer(
					new MemoryStore());
			try (SchemaCachingRDFSInferencerConnection connection = schemaCachingRDFSInferencer.getConnection()) {
				connection.begin(IsolationLevels.NONE);
				Model model = Rio.parse(in, "", RDFFormat.TURTLE);
				model.forEach(s -> connection.addStatement(s.getSubject(), s.getPredicate(), s.getObject(),
						shaclVocabularyGraph));
				connection.commit();
			}
			return schemaCachingRDFSInferencer;
		} catch (IOException e) {
			throw new IllegalStateException("Resource could not be read: shacl-sparql-inference/shaclVocabulary.ttl",
					e);
		}
	}

	private static Model resourceAsModel(String filename) {
		try (InputStream resourceAsStream = getResourceAsStream(filename)) {
			return Rio.parse(resourceAsStream, "", RDFFormat.TURTLE);
		} catch (IOException e) {
			throw new IllegalStateException("Resource could not be read: " + filename, e);
		}
	}

	private static InputStream getResourceAsStream(String filename) {
		InputStream resourceAsStream = ShaclSail.class.getClassLoader().getResourceAsStream(filename);
		if (resourceAsStream == null) {
			throw new IllegalStateException("Resource could not be found: " + filename);
		}
		return new BufferedInputStream(resourceAsStream);
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

}
