/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.model.vocabulary.eu;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.base.Vocabularies;

/**
 * Constants for the JRC DataCite to DCAT-AP Mapping.
 *
 * @see <a href="https://ec-jrc.github.io/datacite-to-dcat-ap/">JRC DataCite to DCAT-AP Mapping</a>
 *
 * @author Bart Hanssens
 */
public class CiteDCATAP {
	/**
	 * The CiteDCAT-AP namespace: https://w3id.org/citedcat-ap/
	 */
	public static final String NAMESPACE = "https://w3id.org/citedcat-ap/";

	/**
	 * Recommended prefix for the namespace: "citedcat"
	 */
	public static final String PREFIX = "citedcat";

	/**
	 * An immutable {@link Namespace} constant that represents the namespace.
	 */
	public static final Namespace NS = Vocabularies.createNamespace(PREFIX, NAMESPACE);

	// Classes
	/** citedcat:DataPaper */
	public static final IRI DataPaper;

	/** citedcat:Model */
	public static final IRI Model;

	/** citedcat:Workflow */
	public static final IRI Workflow;

	// Properties
	/** citedcat:compiles */
	public static final IRI compiles;

	/** citedcat:continues */
	public static final IRI continues;

	/** citedcat:dataCollector */
	public static final IRI dataCollector;

	/** citedcat:dataCurator */
	public static final IRI dataCurator;

	/** citedcat:dataManager */
	public static final IRI dataManager;

	/** citedcat:describes */
	public static final IRI describes;

	/** citedcat:funder */
	public static final IRI funder;

	/** citedcat:hostingInstitution */
	public static final IRI hostingInstitution;

	/** citedcat:isAwardedBy */
	public static final IRI isAwardedBy;

	/** citedcat:isCompiledBy */
	public static final IRI isCompiledBy;

	/** citedcat:isContinuedBy */
	public static final IRI isContinuedBy;

	/** citedcat:isFundedBy */
	public static final IRI isFundedBy;

	/** citedcat:isOriginalFormOf */
	public static final IRI isOriginalFormOf;

	/** citedcat:isReviewedBy */
	public static final IRI isReviewedBy;

	/** citedcat:isSupplementTo */
	public static final IRI isSupplementTo;

	/** citedcat:isSupplementedBy */
	public static final IRI isSupplementedBy;

	/** citedcat:isVariantFormOf */
	public static final IRI isVariantFormOf;

	/** citedcat:projectLeader */
	public static final IRI projectLeader;

	/** citedcat:projectManager */
	public static final IRI projectManager;

	/** citedcat:projectMember */
	public static final IRI projectMember;

	/** citedcat:registrationAgency */
	public static final IRI registrationAgency;

	/** citedcat:registrationAuthority */
	public static final IRI registrationAuthority;

	/** citedcat:researchGroup */
	public static final IRI researchGroup;

	/** citedcat:researcher */
	public static final IRI researcher;

	/** citedcat:sponsor */
	public static final IRI sponsor;

	/** citedcat:supervisor */
	public static final IRI supervisor;

	/** citedcat:workPackageLeader */
	public static final IRI workPackageLeader;

	static {
		DataPaper = Vocabularies.createIRI(NAMESPACE, "DataPaper");
		Model = Vocabularies.createIRI(NAMESPACE, "Model");
		Workflow = Vocabularies.createIRI(NAMESPACE, "Workflow");

		compiles = Vocabularies.createIRI(NAMESPACE, "compiles");
		continues = Vocabularies.createIRI(NAMESPACE, "continues");
		dataCollector = Vocabularies.createIRI(NAMESPACE, "dataCollector");
		dataCurator = Vocabularies.createIRI(NAMESPACE, "dataCurator");
		dataManager = Vocabularies.createIRI(NAMESPACE, "dataManager");
		describes = Vocabularies.createIRI(NAMESPACE, "describes");
		funder = Vocabularies.createIRI(NAMESPACE, "funder");
		hostingInstitution = Vocabularies.createIRI(NAMESPACE, "hostingInstitution");
		isAwardedBy = Vocabularies.createIRI(NAMESPACE, "isAwardedBy");
		isCompiledBy = Vocabularies.createIRI(NAMESPACE, "isCompiledBy");
		isContinuedBy = Vocabularies.createIRI(NAMESPACE, "isContinuedBy");
		isFundedBy = Vocabularies.createIRI(NAMESPACE, "isFundedBy");
		isOriginalFormOf = Vocabularies.createIRI(NAMESPACE, "isOriginalFormOf");
		isReviewedBy = Vocabularies.createIRI(NAMESPACE, "isReviewedBy");
		isSupplementTo = Vocabularies.createIRI(NAMESPACE, "isSupplementTo");
		isSupplementedBy = Vocabularies.createIRI(NAMESPACE, "isSupplementedBy");
		isVariantFormOf = Vocabularies.createIRI(NAMESPACE, "isVariantFormOf");
		projectLeader = Vocabularies.createIRI(NAMESPACE, "projectLeader");
		projectManager = Vocabularies.createIRI(NAMESPACE, "projectManager");
		projectMember = Vocabularies.createIRI(NAMESPACE, "projectMember");
		registrationAgency = Vocabularies.createIRI(NAMESPACE, "registrationAgency");
		registrationAuthority = Vocabularies.createIRI(NAMESPACE, "registrationAuthority");
		researchGroup = Vocabularies.createIRI(NAMESPACE, "researchGroup");
		researcher = Vocabularies.createIRI(NAMESPACE, "researcher");
		sponsor = Vocabularies.createIRI(NAMESPACE, "sponsor");
		supervisor = Vocabularies.createIRI(NAMESPACE, "supervisor");
		workPackageLeader = Vocabularies.createIRI(NAMESPACE, "workPackageLeader");

	}
}
