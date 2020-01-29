package org.eclipse.rdf4j.sail.memory.benchmark;

import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@State(Scope.Benchmark)
@Warmup(iterations = 20)
@BenchmarkMode({ Mode.AverageTime })
//@Fork(value = 1, jvmArgs = {"-Xms4G", "-Xmx4G", "-XX:+UseSerialGC", "-XX:+UnlockCommercialFeatures", "-XX:StartFlightRecording=delay=5s,duration=60s,filename=recording.jfr,settings=profile", "-XX:FlightRecorderOptions=samplethreads=true,stackdepth=1024", "-XX:+UnlockDiagnosticVMOptions", "-XX:+DebugNonSafepoints"})
@Fork(value = 1, jvmArgs = { "-Xms4G", "-Xmx4G", "-XX:+UseSerialGC" })
@Measurement(iterations = 10)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class MemoryBenchmark {

	@Param({ "NONE", "READ_UNCOMMITTED", "READ_COMMITTED", "SNAPSHOT_READ", "SNAPSHOT", "SERIALIZABLE" })
	public String isolationLevel;

	private List<Statement> statementList = getStatements();

	@Setup(Level.Iteration)
	public void setUp() {
		System.gc();
	}

	@Benchmark
	public void load() {

		MemoryStore memoryStore = new MemoryStore();
		memoryStore.initialize();

		try (NotifyingSailConnection connection = memoryStore.getConnection()) {
			connection.begin(IsolationLevels.valueOf(isolationLevel));
			statementList.forEach(
					st -> connection.addStatement(st.getSubject(), st.getPredicate(), st.getObject(), st.getContext()));

			connection.commit();
		}

	}

	@Benchmark
	public long size() {

		MemoryStore memoryStore = new MemoryStore();
		memoryStore.initialize();

		try (NotifyingSailConnection connection = memoryStore.getConnection()) {
			connection.begin(IsolationLevels.valueOf(isolationLevel));
			statementList.forEach(
					st -> connection.addStatement(st.getSubject(), st.getPredicate(), st.getObject(), st.getContext()));

			connection.commit();

			return connection.size();
		}

	}

	@Benchmark
	public long duplicates() {

		MemoryStore memoryStore = new MemoryStore();
		memoryStore.initialize();

		try (NotifyingSailConnection connection = memoryStore.getConnection()) {
			connection.begin(IsolationLevels.valueOf(isolationLevel));

			statementList.forEach(
					st -> connection.addStatement(st.getSubject(), st.getPredicate(), st.getObject(), st.getContext()));

			connection.commit();

			connection.begin(IsolationLevels.valueOf(isolationLevel));

			statementList.forEach(
					st -> connection.addStatement(st.getSubject(), st.getPredicate(), st.getObject(), st.getContext()));

			long count = 0;
			for (int i = 0; i < 10; i++) {
				count += getCount(connection);
			}

			connection.commit();

			return count;
		}

	}

	@Benchmark
	public long duplicatesFlush() {

		MemoryStore memoryStore = new MemoryStore();
		memoryStore.initialize();

		try (NotifyingSailConnection connection = memoryStore.getConnection()) {
			connection.begin(IsolationLevels.valueOf(isolationLevel));

			statementList.forEach(
					st -> connection.addStatement(st.getSubject(), st.getPredicate(), st.getObject(), st.getContext()));

			connection.commit();

			connection.begin(IsolationLevels.valueOf(isolationLevel));

			statementList.forEach(
					st -> connection.addStatement(st.getSubject(), st.getPredicate(), st.getObject(), st.getContext()));

			connection.flush();

			long count = 0;
			for (int i = 0; i < 10; i++) {
				count += getCount(connection);
			}

			connection.commit();

			return count;
		}

	}

	@Benchmark
	public long duplicatesAndNewStatements() {

		MemoryStore memoryStore = new MemoryStore();
		memoryStore.initialize();

		try (NotifyingSailConnection connection = memoryStore.getConnection()) {
			connection.begin(IsolationLevels.valueOf(isolationLevel));

			statementList.forEach(
					st -> connection.addStatement(st.getSubject(), st.getPredicate(), st.getObject(), st.getContext()));

			connection.commit();

			connection.begin(IsolationLevels.valueOf(isolationLevel));

			statementList.forEach(
					st -> connection.addStatement(st.getSubject(), st.getPredicate(), st.getObject(), st.getContext()));

			ValueFactory vf = memoryStore.getValueFactory();
			connection.addStatement(vf.createBNode(), RDFS.LABEL, vf.createLiteral("label"));

			long count = 0;
			for (int i = 0; i < 10; i++) {
				count += getCount(connection);
			}

			connection.commit();

			return count;
		}

	}

	@Benchmark
	public long duplicatesAndNewStatementsGetFirst() {

		MemoryStore memoryStore = new MemoryStore();
		memoryStore.initialize();

		try (NotifyingSailConnection connection = memoryStore.getConnection()) {
			connection.begin(IsolationLevels.valueOf(isolationLevel));

			statementList.forEach(
					st -> connection.addStatement(st.getSubject(), st.getPredicate(), st.getObject(), st.getContext()));

			connection.commit();

			connection.begin(IsolationLevels.valueOf(isolationLevel));

			statementList.forEach(
					st -> connection.addStatement(st.getSubject(), st.getPredicate(), st.getObject(), st.getContext()));

			ValueFactory vf = memoryStore.getValueFactory();
			connection.addStatement(vf.createBNode(), RDFS.LABEL, vf.createLiteral("label"));

			long counter = 0;
			for (int i = 0; i < 10; i++) {
				try (CloseableIteration<? extends Statement, SailException> statements = connection.getStatements(null,
						null, null, false)) {
					counter += statements.next().toString().length();
				}
			}

			connection.commit();

			return counter;
		}

	}

	@Benchmark
	public long singleTransactionGetFirstStatement() {

		MemoryStore memoryStore = new MemoryStore();
		memoryStore.initialize();

		try (NotifyingSailConnection connection = memoryStore.getConnection()) {
			connection.begin(IsolationLevels.valueOf(isolationLevel));

			statementList.forEach(
					st -> connection.addStatement(st.getSubject(), st.getPredicate(), st.getObject(), st.getContext()));

			long count = 0;
			for (int i = 0; i < 10; i++) {
				try (CloseableIteration<? extends Statement, SailException> statements = connection.getStatements(null,
						null, null, false)) {
					count += statements.next().toString().length();
				}
			}

			connection.commit();

			return count;
		}

	}

	@Benchmark
	public long duplicatesAndNewStatementsIteratorMatchesNothing() {

		MemoryStore memoryStore = new MemoryStore();
		memoryStore.initialize();

		try (NotifyingSailConnection connection = memoryStore.getConnection()) {
			connection.begin(IsolationLevels.valueOf(isolationLevel));

			statementList.forEach(
					st -> connection.addStatement(st.getSubject(), st.getPredicate(), st.getObject(), st.getContext()));

			connection.commit();

			connection.begin(IsolationLevels.valueOf(isolationLevel));

			statementList.forEach(
					st -> connection.addStatement(st.getSubject(), st.getPredicate(), st.getObject(), st.getContext()));

			ValueFactory vf = memoryStore.getValueFactory();
			connection.addStatement(vf.createBNode(), RDFS.LABEL, vf.createLiteral("label"));

			long count = 0;
			for (int i = 0; i < 10; i++) {
				try (Stream<? extends Statement> stream = Iterations
						.stream(connection.getStatements(vf.createBNode(), null, null, false))) {
					count += stream.count();
				}
			}

			connection.commit();

			return count;
		}

	}

	private long getCount(NotifyingSailConnection connection) {
		long count;
		try (Stream<? extends Statement> stream = Iterations
				.stream(connection.getStatements(null, null, null, false))) {
			count = stream.count();
		}
		return count;
	}

	private static List<Statement> getStatements() {
		Random random = new Random();

		ValueFactory vf = SimpleValueFactory.getInstance();

		List<Statement> statementList = new ArrayList<>();

		int size = 1000;
		for (int i = 0; i < size; i++) {

			IRI subject = vf.createIRI("http://ex/" + i);
			statementList.add(vf.createStatement(subject, RDF.TYPE, FOAF.PERSON));
			statementList.add(vf.createStatement(subject, FOAF.AGE, vf.createLiteral(i % 80 + 1)));
			statementList.add(vf.createStatement(subject, FOAF.NAME, vf.createLiteral("fjeiwojf kldsfjewif " + i)));
			statementList
					.add(vf.createStatement(subject, FOAF.KNOWS, vf.createIRI("http://ex/" + random.nextInt(size))));
			statementList
					.add(vf.createStatement(subject, FOAF.KNOWS, vf.createIRI("http://ex/" + random.nextInt(size))));
			statementList
					.add(vf.createStatement(subject, FOAF.KNOWS, vf.createIRI("http://ex/" + random.nextInt(size))));
		}
		return statementList;
	}

}
