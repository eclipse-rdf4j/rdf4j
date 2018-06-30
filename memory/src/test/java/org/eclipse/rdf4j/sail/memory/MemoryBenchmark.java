package org.eclipse.rdf4j.sail.memory;

import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
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

import java.util.Random;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@Warmup(iterations = 10)
@BenchmarkMode({Mode.AverageTime})
//@Fork(value = 1, jvmArgs = {"-Xms4G", "-Xmx4G", "-Xmn2G", "-XX:+UseSerialGC", "-XX:+UnlockCommercialFeatures", "-XX:StartFlightRecording=delay=5s,duration=60s,filename=recording.jfr,settings=profile", "-XX:FlightRecorderOptions=samplethreads=true,stackdepth=1024", "-XX:+UnlockDiagnosticVMOptions", "-XX:+DebugNonSafepoints"})
@Fork(value = 1, jvmArgs = {"-Xms4G", "-Xmx4G", "-Xmn2G", "-XX:+UseSerialGC"})
@Measurement(iterations = 10)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class MemoryBenchmark {

	@Param({"NONE", "READ_UNCOMMITTED", "READ_COMMITTED", "SNAPSHOT_READ", "SNAPSHOT", "SERIALIZABLE"})
	public String isolationLevel;

	@Setup(Level.Iteration)
	public void setUp() {
		System.gc();
	}

	@Benchmark
	public void simpleFoafModel() {

		MemoryStore memoryStore = new MemoryStore();
		memoryStore.initialize();
		ValueFactory vf = memoryStore.getValueFactory();

		Random random = new Random();

		try (NotifyingSailConnection connection = memoryStore.getConnection()) {
			connection.begin(IsolationLevels.valueOf(isolationLevel));
			int size = 1000;
			for (int i = 0; i < size; i++) {

				IRI subject = vf.createIRI("http://ex/" + i);
				connection.addStatement(subject, RDF.TYPE, FOAF.PERSON);
				connection.addStatement(subject, FOAF.AGE, vf.createLiteral(i % 80 + 1));
				connection.addStatement(subject, FOAF.NAME, vf.createLiteral("fjeiwojf kldsfjewif " + i));
				connection.addStatement(subject, FOAF.KNOWS, vf.createIRI("http://ex/" + random.nextInt(size)));
				connection.addStatement(subject, FOAF.KNOWS, vf.createIRI("http://ex/" + random.nextInt(size)));
				connection.addStatement(subject, FOAF.KNOWS, vf.createIRI("http://ex/" + random.nextInt(size)));
			}
			connection.commit();
		}

	}

}
