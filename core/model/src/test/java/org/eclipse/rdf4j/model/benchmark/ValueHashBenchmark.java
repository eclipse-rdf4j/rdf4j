package org.eclipse.rdf4j.model.benchmark;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Triple;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.base.AbstractValueFactory;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.OptionsBuilder;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2)
@Measurement(iterations = 5)
@Fork(value = 1, jvmArgs = { "-Xms4G", "-Xmx4G" })
public class ValueHashBenchmark {

	private static final long samples = 1_000_000L;

	public static void main(String[] args) throws RunnerException {
		new Runner(new OptionsBuilder()
				.include(ValueHashBenchmark.class.getSimpleName())
				.build()
		).run();
	}

	// private ValueFactory factory=SimpleValueFactory.getInstance();
	private final ValueFactory factory = new BenchmarkValueFactory();

	private final BNode bnode = factory.createBNode();
	private final BNode bnodeId = factory.createBNode(string("id"));

	private final IRI iriUnary = factory.createIRI(string("http://example.com/name"));
	private final IRI iriBinary = factory.createIRI(string("http://example.com/"), string("name"));

	private final Literal plain = factory.createLiteral(string("text"));
	private final Literal typed = factory.createLiteral(string("text"), iriUnary);
	private final Literal tagged = factory.createLiteral(string("text"), string("en"));

	private final Literal _boolean = factory.createLiteral(true);

	private final Literal _byte = factory.createLiteral((byte) 100);
	private final Literal _short = factory.createLiteral((short) 100);
	private final Literal _int = factory.createLiteral(100);
	private final Literal _long = factory.createLiteral(100L);
	private final Literal _float = factory.createLiteral(100.0F);
	private final Literal _double = factory.createLiteral(100.0D);

	private final Literal integer = factory.createLiteral(new BigInteger("100"));
	private final Literal decimal = factory.createLiteral(new BigDecimal("100"));

	private final Literal calendar = factory.createLiteral(calendar("2020-10-22T15:53:12.345Z"));
	private final Literal date = factory.createLiteral(new Date(1_000_000L));

	private final Triple triple = factory.createTriple(iriUnary, iriUnary, iriUnary);

	private final Statement statement = factory.createStatement(iriUnary, iriUnary, iriUnary);
	private final Statement statementContext = factory.createStatement(iriUnary, iriUnary, iriUnary, iriUnary);

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Benchmark
	public void hashBNode() {
		for (long c = samples; c > 0; --c) {
			bnode.hashCode();
		}
	}

	@Benchmark
	public void hashBNodeId() {
		for (long c = samples; c > 0; --c) {
			bnodeId.hashCode();
		}
	}

	@Benchmark
	public void hashIRIUnary() {
		for (long c = samples; c > 0; --c) {
			iriUnary.hashCode();
		}
	}

	@Benchmark
	public void hashIRIBinary() {
		for (long c = samples; c > 0; --c) {
			iriBinary.hashCode();
		}
	}

	@Benchmark
	public void hashPlain() {
		for (long c = samples; c > 0; --c) {
			plain.hashCode();
		}
	}

	@Benchmark
	public void hashTyped() {
		for (long c = samples; c > 0; --c) {
			typed.hashCode();
		}
	}

	@Benchmark
	public void hashTagged() {
		for (long c = samples; c > 0; --c) {
			tagged.hashCode();
		}
	}

	@Benchmark
	public void hashBoolean() {
		for (long c = samples; c > 0; --c) {
			_boolean.hashCode();
		}
	}

	@Benchmark
	public void hashByte() {
		for (long c = samples; c > 0; --c) {
			_byte.hashCode();
		}
	}

	@Benchmark
	public void hashShort() {
		for (long c = samples; c > 0; --c) {
			_short.hashCode();
		}
	}

	@Benchmark
	public void hashInt() {
		for (long c = samples; c > 0; --c) {
			_int.hashCode();
		}
	}

	@Benchmark
	public void hashLong() {
		for (long c = samples; c > 0; --c) {
			_long.hashCode();
		}
	}

	@Benchmark
	public void hashFloat() {
		for (long c = samples; c > 0; --c) {
			_float.hashCode();
		}
	}

	@Benchmark
	public void hashDouble() {
		for (long c = samples; c > 0; --c) {
			_double.hashCode();
		}
	}

	@Benchmark
	public void hashInteger() {
		for (long c = samples; c > 0; --c) {
			integer.hashCode();
		}
	}

	@Benchmark
	public void hashDecimal() {
		for (long c = samples; c > 0; --c) {
			decimal.hashCode();
		}
	}

	@Benchmark
	public void hashCalendar() {
		for (long c = samples; c > 0; --c) {
			calendar.hashCode();
		}
	}

	@Benchmark
	public void hashDate() {
		for (long c = samples; c > 0; --c) {
			date.hashCode();
		}
	}

	@Benchmark
	public void hashTriple() {
		for (long c = samples; c > 0; --c) {
			triple.hashCode();
		}
	}

	@Benchmark
	public void hashStatement() {
		for (long c = samples; c > 0; --c) {
			statement.hashCode();
		}
	}

	@Benchmark
	public void hashStatementContext() {
		for (long c = samples; c > 0; --c) {
			statementContext.hashCode();
		}
	}

	private static String string(String string) {
		return new String(string); // force unique object creation
	}

	private XMLGregorianCalendar calendar(String string) {
		try {

			return DatatypeFactory.newInstance().newXMLGregorianCalendar(string);

		} catch (DatatypeConfigurationException e) {
			throw new RuntimeException(e);
		}
	}

	private static class BenchmarkValueFactory extends AbstractValueFactory {
	}

}
