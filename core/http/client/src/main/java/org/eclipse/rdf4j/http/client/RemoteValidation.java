package org.eclipse.rdf4j.http.client;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;

import java.io.IOException;
import java.io.StringReader;

public class RemoteValidation implements ModelInterface {

	StringReader stringReader;
	String baseUri;
	RDFFormat format;

	Model model;

	public RemoteValidation(StringReader stringReader, String baseUri, RDFFormat format) {
		this.stringReader = stringReader;
		this.baseUri = baseUri;
		this.format = format;
	}

	@Override
	public Model asModel(Model model) {
		model.addAll(asModel());
		return model;
	}

	@Override
	public Model asModel() {
		if (model == null) {
			try {
				model = Rio.parse(stringReader, baseUri, format);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		return model;
	}

	@Override
	public Resource getId() {
		return null;
	}
}
