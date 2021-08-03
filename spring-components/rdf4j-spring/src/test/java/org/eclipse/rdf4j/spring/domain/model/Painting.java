package org.eclipse.rdf4j.spring.domain.model;

import org.eclipse.rdf4j.model.IRI;

public class Painting {
	private IRI id;
	private String title;
	private String technique;

	public IRI getId() {
		return id;
	}

	public void setId(IRI id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getTechnique() {
		return technique;
	}

	public void setTechnique(String technique) {
		this.technique = technique;
	}
}
