package org.eclipse.rdf4j.sail.memory.benchmark;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

@State(Scope.Benchmark)
@Warmup(iterations = 5)
@BenchmarkMode({ Mode.AverageTime })
//@Fork(value = 1, jvmArgs = {"-Xms4G", "-Xmx4G", "-XX:+UnlockCommercialFeatures", "-XX:StartFlightRecording=delay=5s,duration=60s,filename=recording.jfr,settings=profile", "-XX:FlightRecorderOptions=samplethreads=true,stackdepth=1024", "-XX:+UnlockDiagnosticVMOptions", "-XX:+DebugNonSafepoints"})
@Fork(value = 1, jvmArgs = { "-Xms4G", "-Xmx4G" })
@Measurement(iterations = 5)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class LoadingBenchmark {

	@Param({ "NONE", "SNAPSHOT", "SERIALIZABLE" })
	public String isolationLevel;

	private static final List<Statement> statementList = getStatements();
	private static final Model realData = getRealData();

	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include("LoadingBenchmark.*") // adapt to run other benchmark tests
				.forks(1)
				.build();

		new Runner(opt).run();
	}

	@Benchmark
	public void loadSynthetic() {

		MemoryStore memoryStore = new MemoryStore();
		memoryStore.init();

		try (NotifyingSailConnection connection = memoryStore.getConnection()) {
			connection.begin(IsolationLevels.valueOf(isolationLevel));
			statementList.forEach(getStatementConsumer(connection));

			connection.commit();
		}

	}

	@Benchmark
	public void loadSyntheticOneStatementPerTransaction() {

		MemoryStore memoryStore = new MemoryStore();
		memoryStore.init();

		try (NotifyingSailConnection connection = memoryStore.getConnection()) {

			for (Statement statement : statementList) {
				connection.begin(IsolationLevels.valueOf(isolationLevel));
				getStatementConsumer(connection).accept(statement);
				connection.commit();
			}

		}

	}

	@Benchmark
	public void loadSyntheticOneStatementPerTransactionClearPrevious() {

		MemoryStore memoryStore = new MemoryStore();
		memoryStore.init();

		try (NotifyingSailConnection connection = memoryStore.getConnection()) {

			for (Statement statement : statementList) {
				connection.begin(IsolationLevels.valueOf(isolationLevel));
				connection.clear();
				getStatementConsumer(connection).accept(statement);
				connection.commit();
			}

		}

	}

	@Benchmark
	public void loadRealData() {

		MemoryStore memoryStore = new MemoryStore();
		memoryStore.init();

		try (NotifyingSailConnection connection = memoryStore.getConnection()) {
			connection.begin(IsolationLevels.valueOf(isolationLevel));
			realData.forEach(getStatementConsumer(connection));

			connection.commit();
		}

	}

	@Benchmark
	public long loadSyntheticAndSize() {

		MemoryStore memoryStore = new MemoryStore();
		memoryStore.init();

		try (NotifyingSailConnection connection = memoryStore.getConnection()) {
			connection.begin(IsolationLevels.valueOf(isolationLevel));
			statementList.forEach(getStatementConsumer(connection));

			connection.commit();

			return connection.size();
		}

	}

	@Benchmark
	public long loadSyntheticWithDuplicates() {

		MemoryStore memoryStore = new MemoryStore();
		memoryStore.init();

		try (NotifyingSailConnection connection = memoryStore.getConnection()) {
			connection.begin(IsolationLevels.valueOf(isolationLevel));

			statementList.forEach(getStatementConsumer(connection));

			connection.commit();

			connection.begin(IsolationLevels.valueOf(isolationLevel));

			statementList.forEach(getStatementConsumer(connection));

			long count = 0;
			for (int i = 0; i < 10; i++) {
				count += getCount(connection);
			}

			connection.commit();

			return count;
		}

	}

	@Benchmark
	public long loadSyntheticWithDuplicatesFlush() {

		MemoryStore memoryStore = new MemoryStore();
		memoryStore.init();

		try (NotifyingSailConnection connection = memoryStore.getConnection()) {
			connection.begin(IsolationLevels.valueOf(isolationLevel));

			statementList.forEach(getStatementConsumer(connection));

			connection.commit();

			connection.begin(IsolationLevels.valueOf(isolationLevel));

			statementList.forEach(getStatementConsumer(connection));

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
	public long loadSyntheticWithDuplicatesAndNewStatements() {

		MemoryStore memoryStore = new MemoryStore();
		memoryStore.init();

		try (NotifyingSailConnection connection = memoryStore.getConnection()) {
			connection.begin(IsolationLevels.valueOf(isolationLevel));

			statementList.forEach(getStatementConsumer(connection));

			connection.commit();

			connection.begin(IsolationLevels.valueOf(isolationLevel));

			statementList.forEach(getStatementConsumer(connection));

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
	public long loadSyntheticWithDuplicatesAndNewStatementsGetFirst() {

		MemoryStore memoryStore = new MemoryStore();
		memoryStore.init();

		try (NotifyingSailConnection connection = memoryStore.getConnection()) {
			connection.begin(IsolationLevels.valueOf(isolationLevel));

			statementList.forEach(getStatementConsumer(connection));

			connection.commit();

			connection.begin(IsolationLevels.valueOf(isolationLevel));

			statementList.forEach(getStatementConsumer(connection));

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
	public long loadSyntheticSingleTransactionGetFirstStatement() {

		MemoryStore memoryStore = new MemoryStore();
		memoryStore.init();

		try (NotifyingSailConnection connection = memoryStore.getConnection()) {
			connection.begin(IsolationLevels.valueOf(isolationLevel));

			statementList.forEach(getStatementConsumer(connection));

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
	public long loadSyntheticWithDuplicatesAndNewStatementsIteratorMatchesNothing() {

		MemoryStore memoryStore = new MemoryStore();
		memoryStore.init();

		try (NotifyingSailConnection connection = memoryStore.getConnection()) {
			connection.begin(IsolationLevels.valueOf(isolationLevel));

			statementList.forEach(getStatementConsumer(connection));

			connection.commit();

			connection.begin(IsolationLevels.valueOf(isolationLevel));

			statementList.forEach(getStatementConsumer(connection));

			ValueFactory vf = memoryStore.getValueFactory();
			connection.addStatement(vf.createBNode(), RDFS.LABEL, vf.createLiteral("label"));

			long count = 0;
			for (int i = 0; i < 10; i++) {
				try (Stream<? extends Statement> stream = connection.getStatements(vf.createBNode(), null, null, false)
						.stream()) {
					count += stream.count();
				}
			}

			connection.commit();

			return count;
		}

	}

	private long getCount(NotifyingSailConnection connection) {
		long count;
		try (Stream<? extends Statement> stream = connection.getStatements(null, null, null, false).stream()) {
			count = stream.count();
		}
		return count;
	}

	private static List<Statement> getStatements() {
		Random random = new Random(34435325);

		ValueFactory vf = SimpleValueFactory.getInstance();

		List<Statement> statementList = new ArrayList<>();

		int size = 5000;
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

	private static Model getRealData() {
		try {
			try (InputStream inputStream = new BufferedInputStream(LoadingBenchmark.class.getClassLoader()
					.getResourceAsStream("benchmarkFiles/datagovbe-valid.ttl"))) {
				return Rio.parse(inputStream, RDFFormat.TURTLE);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (NullPointerException e) {
			throw new RuntimeException("Could not load file: benchmarkFiles/datagovbe-valid.ttl", e);
		}
	}

	private static Consumer<Statement> getStatementConsumer(SailConnection connection) {
		return st -> connection.addStatement(st.getSubject(), st.getPredicate(), st.getObject(), st.getContext());
	}
}
