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

/**
 * Constants for the JRC DataCite to DCAT-AP Mapping.
 *
 * @see <a href="https://ec-jrc.github.io/datacite-to-dcat-ap/">JRC DataCite to DCAT-AP Mapping</a>
 *
 * @author Bart Hanssens
 */
public class CITEDCAT {
	/**
	 * The CITEDCAT namespace: https://w3id.org/citedcat-ap/
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
	public static final IRI DATA_PAPER;

	/** citedcat:Model */
	public static final IRI MODEL;

	/** citedcat:Workflow */
	public static final IRI WORKFLOW;

	// Properties
	/** citedcat:compiles */
	public static final IRI COMPILES;

	/** citedcat:continues */
	public static final IRI CONTINUES;

	/** citedcat:dataCollector */
	public static final IRI DATA_COLLECTOR;

	/** citedcat:dataCurator */
	public static final IRI DATA_CURATOR;

	/** citedcat:dataManager */
	public static final IRI DATA_MANAGER;

	/** citedcat:describes */
	public static final IRI DESCRIBES;

	/** citedcat:funder */
	public static final IRI FUNDER;

	/** citedcat:hostingInstitution */
	public static final IRI HOSTING_INSTITUTION;

	/** citedcat:isAwardedBy */
	public static final IRI IS_AWARDED_BY;

	/** citedcat:isCompiledBy */
	public static final IRI IS_COMPILED_BY;

	/** citedcat:isContinuedBy */
	public static final IRI IS_CONTINUED_BY;

	/** citedcat:isFundedBy */
	public static final IRI IS_FUNDED_BY;

	/** citedcat:isOriginalFormOf */
	public static final IRI IS_ORIGINAL_FORM_OF;

	/** citedcat:isReviewedBy */
	public static final IRI IS_REVIEWED_BY;

	/** citedcat:isSupplementTo */
	public static final IRI IS_SUPPLEMENT_TO;

	/** citedcat:isSupplementedBy */
	public static final IRI IS_SUPPLEMENTED_BY;

	/** citedcat:isVariantFormOf */
	public static final IRI IS_VARIANT_FORM_OF;

	/** citedcat:projectLeader */
	public static final IRI PROJECT_LEADER;

	/** citedcat:projectManager */
	public static final IRI PROJECT_MANAGER;

	/** citedcat:projectMember */
	public static final IRI PROJECT_MEMBER;

	/** citedcat:registrationAgency */
	public static final IRI REGISTRATION_AGENCY;

	/** citedcat:registrationAuthority */
	public static final IRI REGISTRATION_AUTHORITY;

	/** citedcat:researchGroup */
	public static final IRI RESEARCH_GROUP;

	/** citedcat:researcher */
	public static final IRI RESEARCHER;

	/** citedcat:sponsor */
	public static final IRI SPONSOR;

	/** citedcat:supervisor */
	public static final IRI SUPERVISOR;

	/** citedcat:workPackageLeader */
	public static final IRI WORK_PACKAGE_LEADER;

	// Individuals

	static {
		DATA_PAPER = Vocabularies.createIRI(NAMESPACE, "DataPaper");
		MODEL = Vocabularies.createIRI(NAMESPACE, "Model");
		WORKFLOW = Vocabularies.createIRI(NAMESPACE, "Workflow");

		COMPILES = Vocabularies.createIRI(NAMESPACE, "compiles");
		CONTINUES = Vocabularies.createIRI(NAMESPACE, "continues");
		DATA_COLLECTOR = Vocabularies.createIRI(NAMESPACE, "dataCollector");
		DATA_CURATOR = Vocabularies.createIRI(NAMESPACE, "dataCurator");
		DATA_MANAGER = Vocabularies.createIRI(NAMESPACE, "dataManager");
		DESCRIBES = Vocabularies.createIRI(NAMESPACE, "describes");
		FUNDER = Vocabularies.createIRI(NAMESPACE, "funder");
		HOSTING_INSTITUTION = Vocabularies.createIRI(NAMESPACE, "hostingInstitution");
		IS_AWARDED_BY = Vocabularies.createIRI(NAMESPACE, "isAwardedBy");
		IS_COMPILED_BY = Vocabularies.createIRI(NAMESPACE, "isCompiledBy");
		IS_CONTINUED_BY = Vocabularies.createIRI(NAMESPACE, "isContinuedBy");
		IS_FUNDED_BY = Vocabularies.createIRI(NAMESPACE, "isFundedBy");
		IS_ORIGINAL_FORM_OF = Vocabularies.createIRI(NAMESPACE, "isOriginalFormOf");
		IS_REVIEWED_BY = Vocabularies.createIRI(NAMESPACE, "isReviewedBy");
		IS_SUPPLEMENT_TO = Vocabularies.createIRI(NAMESPACE, "isSupplementTo");
		IS_SUPPLEMENTED_BY = Vocabularies.createIRI(NAMESPACE, "isSupplementedBy");
		IS_VARIANT_FORM_OF = Vocabularies.createIRI(NAMESPACE, "isVariantFormOf");
		PROJECT_LEADER = Vocabularies.createIRI(NAMESPACE, "projectLeader");
		PROJECT_MANAGER = Vocabularies.createIRI(NAMESPACE, "projectManager");
		PROJECT_MEMBER = Vocabularies.createIRI(NAMESPACE, "projectMember");
		REGISTRATION_AGENCY = Vocabularies.createIRI(NAMESPACE, "registrationAgency");
		REGISTRATION_AUTHORITY = Vocabularies.createIRI(NAMESPACE, "registrationAuthority");
		RESEARCH_GROUP = Vocabularies.createIRI(NAMESPACE, "researchGroup");
		RESEARCHER = Vocabularies.createIRI(NAMESPACE, "researcher");
		SPONSOR = Vocabularies.createIRI(NAMESPACE, "sponsor");
		SUPERVISOR = Vocabularies.createIRI(NAMESPACE, "supervisor");
		WORK_PACKAGE_LEADER = Vocabularies.createIRI(NAMESPACE, "workPackageLeader");

	}
}
