package org.eclipse.rdf4j.http.server.readonly.sparql;

import java.io.OutputStream;

import org.eclipse.rdf4j.http.server.readonly.sparql.EvaluateResult;

public class EvaluateResultDefault implements EvaluateResult {
	private String contentType;
	private OutputStream outputstream;

	public EvaluateResultDefault(OutputStream outputstream) {
		this.outputstream = outputstream;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public OutputStream getOutputstream() {
		return outputstream;
	}

	public void setOutputstream(OutputStream outputstream) {
		this.outputstream = outputstream;
	}
}

