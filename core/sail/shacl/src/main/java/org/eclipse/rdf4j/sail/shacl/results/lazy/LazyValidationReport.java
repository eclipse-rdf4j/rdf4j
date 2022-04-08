/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.results.lazy;

import static org.eclipse.rdf4j.model.util.Values.literal;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.eclipse.rdf4j.common.annotation.InternalUseOnly;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.BooleanLiteral;
import org.eclipse.rdf4j.model.impl.DynamicModelFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDF4J;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.sail.shacl.results.ValidationReport;
import org.eclipse.rdf4j.sail.shacl.results.ValidationResult;

/**
 * A ValidationReport that will defer calculating any ValidationResults until the user asks for them
 *
 */
@InternalUseOnly
public class LazyValidationReport extends ValidationReport {

	private List<ValidationResultIterator> validationResultIterators;
	private final long limit;

	public LazyValidationReport(List<ValidationResultIterator> validationResultIterators, long limit) {

		this.validationResultIterators = validationResultIterators;
		this.limit = limit;

	}

	private void evaluateLazyAspect() {
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

	}

	public Model asModel(Model model) {
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
	}

	public Model asModel() {
		return asModel(new DynamicModelFactory().createEmptyModel());
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
		return "LazyValidationReport{" +
				"conforms=" + conforms +
				", validationResult=" + Arrays.toString(validationResult.toArray()) +
				'}';
	}

	public boolean isTruncated() {
		evaluateLazyAspect();
		return truncated;
	}

}
