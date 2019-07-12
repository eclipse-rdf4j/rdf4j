package org.eclipse.rdf4j.model.util;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@Warmup(iterations = 20)
@BenchmarkMode({ Mode.AverageTime })
@Fork(value = 1, jvmArgs = { "-Xms1G", "-Xmx1G", "-XX:+UseSerialGC" })
//@Fork(value = 1, jvmArgs = {"-Xms8G", "-Xmx8G", "-XX:+UseSerialGC", "-XX:+UnlockCommercialFeatures", "-XX:StartFlightRecording=delay=5s,duration=120s,filename=recording.jfr,settings=profile", "-XX:FlightRecorderOptions=samplethreads=true,stackdepth=1024", "-XX:+UnlockDiagnosticVMOptions", "-XX:+DebugNonSafepoints"})
@Measurement(iterations = 10)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class IsomorphicBenchmark {

	private Model empty = getModel("empty.ttl");
	private Model blankNodes = getModel("blankNodes.ttl");
	private Model shacl = getModel("shacl.ttl");
	private Model longChain = getModel("longChain.ttl");
	private Model sparqlTestCase = getModel("sparqlTestCase.ttl");
	private Model spinFullForwardchained = getModel("spin-full-forwardchained.ttl");
	private Model bsbm = getModel("bsbm-100.ttl");
	private Model bsbmChanged = getModel("bsbm-100-changed.ttl");
	private List<Statement> bsbm_arraylist = new ArrayList<>(bsbm);
	private Model bsbmTree = new TreeModel(bsbm);
	private Model list = getModel("list.ttl");
	private Model internallyIsomorphic = getModel("internallyIsomorphic.ttl");
	private Model manyProperties = getModel("manyProperties.ttl");
	private Model manyProperties2 = getModel("manyProperties2.ttl");

	private Model empty_2 = getModel("empty.ttl");
	private Model blankNodes_2 = getModel("blankNodes.ttl");
	private Model shacl_2 = getModel("shacl.ttl");
	private Model longChain_2 = getModel("longChain.ttl");
	private Model sparqlTestCase_2 = getModel("sparqlTestCase.ttl");
	private Model spinFullForwardchained_2 = getModel("spin-full-forwardchained.ttl");
	private Model bsbm_2 = getModel("bsbm-100.ttl");
	private List<Statement> bsbm_arraylist_2 = new ArrayList<>(bsbm);
	private Model bsbmTree_2 = new TreeModel(bsbm);
	private Model list_2 = getModel("list.ttl");
	private Model internallyIsomorphic_2 = getModel("internallyIsomorphic.ttl");
	private Model manyProperties_2 = getModel("manyProperties.ttl");
	private Model manyProperties2_2 = getModel("manyProperties2.ttl");

	@Setup(Level.Iteration)
	public void after() {
		System.gc();
	}

	@Benchmark
	public boolean sameModel() {

		return isomorphic(bsbm, bsbm);

	}

	@Benchmark
	public boolean empty() {

		return isomorphic(empty, empty_2);

	}

	@Benchmark
	public boolean blankNodes() {

		return isomorphic(blankNodes, blankNodes_2);

	}

	@Benchmark
	public boolean shacl() {

		return isomorphic(shacl, shacl_2);

	}

	@Benchmark
	public boolean longChain() {

		return isomorphic(longChain, longChain_2);

	}

	@Benchmark
	public boolean sparqlTestCase() {

		return isomorphic(sparqlTestCase, sparqlTestCase_2);

	}

	@Benchmark
	public boolean bsbm() {

		return isomorphic(bsbm, bsbm_2);

	}

	@Benchmark
	public boolean bsbmTree() {

		return isomorphic(bsbmTree, bsbmTree_2);

	}

	@Benchmark
	public boolean bsbmArrayList() {

		boolean isomorphic = Models.isomorphic(bsbm_arraylist, bsbm_arraylist_2);
		if (!isomorphic) {
			throw new IllegalStateException("Not isomorphic");
		}

		return isomorphic;

	}

	@Benchmark
	public boolean spinFullForwardchained() {

		return isomorphic(spinFullForwardchained, spinFullForwardchained_2);

	}

	@Benchmark
	public boolean list() {

		return isomorphic(list, list_2);

	}

	@Benchmark
	public boolean internallyIsomorphic() {

		return isomorphic(internallyIsomorphic, internallyIsomorphic_2);

	}

	@Benchmark
	public boolean manyProperties() {

		return isomorphic(manyProperties, manyProperties_2);

	}

	@Benchmark
	public boolean manyProperties2() {

		return isomorphic(manyProperties2, manyProperties2_2);

	}

	@Benchmark
	public boolean emptyNotIsomorphic() {

		return notIsomorphic(empty, bsbm);

	}

	@Benchmark
	public boolean bsbmNotIsomorphic() {

		return notIsomorphic(bsbm, bsbmChanged);

	}

	private Model getModel(String name) {
		try {
			try (InputStream resourceAsStream = IsomorphicBenchmark.class.getClassLoader()
					.getResourceAsStream("benchmark/" + name)) {
				return Rio.parse(resourceAsStream, "http://example.com/", RDFFormat.TURTLE);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private boolean isomorphic(Model m1, Model m2) {

		boolean isomorphic = Models.isomorphic(m1, m2);
		if (!isomorphic) {
			throw new IllegalStateException("Not isomorphic");
		}

		return isomorphic;
	}

	private boolean notIsomorphic(Model m1, Model m2) {

		boolean isomorphic = Models.isomorphic(m1, m2);
		if (isomorphic) {
			throw new IllegalStateException("Should not be isomorphic");
		}

		return isomorphic;
	}

}
