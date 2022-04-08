/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.shacl.config;

import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.config.SailConfigException;
import org.eclipse.rdf4j.sail.config.SailFactory;
import org.eclipse.rdf4j.sail.config.SailImplConfig;
import org.eclipse.rdf4j.sail.shacl.ShaclSail;

/**
 * Factory class for creation of {@link ShaclSail}s as part of a Sail stack.
 *
 * @author Jeen Broekstra
 */
public class ShaclSailFactory implements SailFactory {

	/**
	 * The type of Sails that are created by this factory.
	 *
	 * @see SailFactory#getSailType()
	 */
	public static final String SAIL_TYPE = "rdf4j:ShaclSail";

	@Override
	public String getSailType() {
		return SAIL_TYPE;
	}

	@Override
	public SailImplConfig getConfig() {
		return new ShaclSailConfig();
	}

	@Override
	public Sail getSail(SailImplConfig config) throws SailConfigException {
		if (!SAIL_TYPE.equals(config.getType())) {
			throw new SailConfigException("Invalid Sail type: " + config.getType());
		}

		ShaclSail sail = new ShaclSail();

		if (config instanceof ShaclSailConfig) {
			ShaclSailConfig shaclSailConfig = (ShaclSailConfig) config;

			if (shaclSailConfig.isValidationEnabled()) {
				sail.enableValidation();
			} else {
				sail.disableValidation();
			}

			sail.setCacheSelectNodes(shaclSailConfig.isCacheSelectNodes());
			sail.setLogValidationPlans(shaclSailConfig.isLogValidationPlans());
			sail.setLogValidationViolations(shaclSailConfig.isLogValidationViolations());
			sail.setParallelValidation(shaclSailConfig.isParallelValidation());
			sail.setGlobalLogValidationExecution(shaclSailConfig.isGlobalLogValidationExecution());
			sail.setPerformanceLogging(shaclSailConfig.isPerformanceLogging());
			sail.setSerializableValidation(shaclSailConfig.isSerializableValidation());
			sail.setRdfsSubClassReasoning(shaclSailConfig.isRdfsSubClassReasoning());
			sail.setEclipseRdf4jShaclExtensions(shaclSailConfig.isEclipseRdf4jShaclExtensions());
			sail.setDashDataShapes(shaclSailConfig.isDashDataShapes());
			sail.setValidationResultsLimitTotal(shaclSailConfig.getValidationResultsLimitTotal());
			sail.setValidationResultsLimitPerConstraint(shaclSailConfig.getValidationResultsLimitPerConstraint());
			sail.setShapesGraphs(shaclSailConfig.getShapesGraphs());

		}

		return sail;

	}

}
