package org.eclipse.rdf4j.repository.config;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;

public class RepositorySettings {

	Map<IRI, Literal> settings;

	public RepositorySettings(Map<IRI, Literal> settings) {
		this.settings = new HashMap<>(settings);
	}

	public RepositorySettings set(IRI iri, Literal value) {

		RepositorySettings repositorySettings = new RepositorySettings(settings);
		repositorySettings.settings.put(iri, value);

		return repositorySettings;
	}

	public Literal get(IRI iri) {
		return settings.get(iri);
	}

	public Map<IRI, Literal> allSettings() {
		return new HashMap<>(settings);
	}

}
