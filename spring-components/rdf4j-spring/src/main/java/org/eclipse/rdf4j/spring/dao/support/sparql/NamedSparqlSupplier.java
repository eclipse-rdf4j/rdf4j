package org.eclipse.rdf4j.spring.dao.support.sparql;

import java.util.Objects;
import java.util.function.Supplier;

/** Associates a String key with a {@link Supplier<String>} that provides a SPARQL operation. */
public class NamedSparqlSupplier {
	private final String name;
	private final Supplier<String> sparqlSupplier;

	public NamedSparqlSupplier(String name, Supplier<String> sparqlSupplier) {
		this.name = name;
		this.sparqlSupplier = sparqlSupplier;
	}

	public String getName() {
		return name;
	}

	public Supplier<String> getSparqlSupplier() {
		return sparqlSupplier;
	}

	public static NamedSparqlSupplier of(String key, Supplier<String> generator) {
		return new NamedSparqlSupplier(key, generator);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		NamedSparqlSupplier that = (NamedSparqlSupplier) o;
		return Objects.equals(name, that.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name);
	}
}
