/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.results.lazy;

import static org.eclipse.rdf4j.model.util.Values.literal;

import java.io.StringWriter;
import java.util.HashSet;
import java.util.List;

import org.eclipse.rdf4j.common.annotation.InternalUseOnly;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.BooleanLiteral;
import org.eclipse.rdf4j.model.impl.DynamicModel;
import org.eclipse.rdf4j.model.impl.DynamicModelFactory;
import org.eclipse.rdf4j.model.vocabulary.DASH;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDF4J;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.RSX;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.WriterConfig;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.eclipse.rdf4j.sail.shacl.results.ValidationReport;
import org.eclipse.rdf4j.sail.shacl.results.ValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A ValidationReport that will defer calculating any ValidationResults until the user asks for them
 */
@InternalUseOnly
public class LazyValidationReport extends ValidationReport {

	private static final Logger logger = LoggerFactory.getLogger(LazyValidationReport.class);

	private List<ValidationResultIterator> validationResultIterators;
	private final long limit;

	public LazyValidationReport(List<ValidationResultIterator> validationResultIterators, long limit) {

		this.validationResultIterators = validationResultIterators;
		this.limit = limit;

	}

	private void evaluateLazyAspect() {
		try {
			if (validationResultIterators != null) {
				long counter = 0;
				for (ValidationResultIterator validationResultIterator : validationResultIterators) {
					while (validationResultIterator.hasNext()) {
						if (limit >= 0 && counter >= limit) {
							truncated = true;
							break;
						}
						counter++;

						validationResult.add(validationResultIterator.next());
					}

					this.conforms = conforms && validationResultIterator.conforms();
					this.truncated = truncated || validationResultIterator.isTruncated();
				}

				validationResultIterators = null;

			}
		} catch (Throwable e) {
			logger.warn("Error evaluating lazy validation report", e);
			throw e;
		}

	}

	public Model asModel(Model model) {
		try {
			evaluateLazyAspect();

			model.add(getId(), SHACL.CONFORMS, literal(conforms));
			model.add(getId(), RDF.TYPE, SHACL.VALIDATION_REPORT);
			model.add(getId(), RDF4J.TRUNCATED, BooleanLiteral.valueOf(truncated));

			HashSet<Resource> rdfListDedupe = new HashSet<>();

			for (ValidationResult result : validationResult) {
				model.add(getId(), SHACL.RESULT, result.getId());
				result.asModel(model, rdfListDedupe);
			}

			return model;
		} catch (Throwable e) {
			logger.warn("Error converting validation report to model", e);
			throw e;
		}

	}

	public Model asModel() {
		DynamicModel emptyModel = new DynamicModelFactory().createEmptyModel();
		emptyModel.setNamespace(SHACL.NS);
		emptyModel.setNamespace(RSX.NS);
		emptyModel.setNamespace(DASH.NS);

		emptyModel.setNamespace(RDF.NS);
		emptyModel.setNamespace(RDFS.NS);
		emptyModel.setNamespace(OWL.NS);
		emptyModel.setNamespace(XSD.NS);

		emptyModel.setNamespace(RDF4J.NS);

		return asModel(emptyModel);
	}

	/**
	 * @return false if the changes violated a SHACL Shape
	 */
	public boolean conforms() {
		evaluateLazyAspect();
		return conforms;
	}

	/**
	 * @return list of ValidationResult with more information about each violation
	 */
	public List<ValidationResult> getValidationResult() {
		evaluateLazyAspect();
		return validationResult;
	}

	@Override
	public String toString() {
		StringWriter stringWriter = new StringWriter();

		WriterConfig writerConfig = new WriterConfig()
				.set(BasicWriterSettings.PRETTY_PRINT, true)
				.set(BasicWriterSettings.INLINE_BLANK_NODES, true);

		Rio.write(asModel(), stringWriter, RDFFormat.TURTLE, writerConfig);

		return stringWriter.toString()
				.replaceAll("(?m)^(@prefix)(.*)(\\.)$", "") // remove all lines that are prefix declarations
				.trim();
	}

	public boolean isTruncated() {
		evaluateLazyAspect();
		return truncated;
	}

}
