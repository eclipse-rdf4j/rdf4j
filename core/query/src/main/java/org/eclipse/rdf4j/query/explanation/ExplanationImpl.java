/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.explanation;

import org.eclipse.rdf4j.common.annotation.Experimental;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This is an experimental feature. It may be changed, moved or potentially removed in a future release.
 *
 * @since 3.2.0
 */
@Experimental
public class ExplanationImpl implements Explanation {

	private final GenericPlanNode genericPlanNodeGenerator;

	public ExplanationImpl(GenericPlanNode genericPlanNodeGenerator) {
		this.genericPlanNodeGenerator = genericPlanNodeGenerator;
	}

	ObjectMapper objectMapper = new ObjectMapper();

	@Override
	public GenericPlanNode toGenericPlanNode() {
		return genericPlanNodeGenerator;
	}

	@Override
	public String toJson() {
		try {
			// TODO: Consider removing pretty printer
			return objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)
					.writerWithDefaultPrettyPrinter()
					.writeValueAsString(toGenericPlanNode());
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String toString() {
		return toGenericPlanNode().toString();
	}

}
