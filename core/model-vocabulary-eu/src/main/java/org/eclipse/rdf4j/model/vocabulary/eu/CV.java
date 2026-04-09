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
 * Constants for the SEMIC Core Vocabularies.
 *
 * @see <a href="https://semiceu.github.io/Consolidated-Core-Vocabularies/releases/2.2.0/">SEMIC Core Vocabularies</a>
 *
 * @author Bart Hanssens
 */
public class CV {
	/**
	 * The CV namespace: http://data.europa.eu/m8g/
	 */
	public static final String NAMESPACE = "http://data.europa.eu/m8g/";

	/**
	 * Recommended prefix for the namespace: "cv"
	 */
	public static final String PREFIX = "cv";

	/**
	 * An immutable {@link Namespace} constant that represents the namespace.
	 */
	public static final Namespace NS = Vocabularies.createNamespace(PREFIX, NAMESPACE);

	// Classes
	/** cv:AccountingDocument */
	public static final IRI ACCOUNTING_DOCUMENT;

	/** cv:AdminUnit */
	public static final IRI ADMIN_UNIT;

	/** cv:BusinessEvent */
	public static final IRI BUSINESS_EVENT;

	/** cv:Channel */
	public static final IRI CHANNEL;

	/** cv:Constraint */
	public static final IRI CONSTRAINT;

	/** cv:ContactPoint */
	public static final IRI CONTACT_POINT;

	/** cv:Cost */
	public static final IRI COST;

	/** cv:Criterion */
	public static final IRI CRITERION;

	/** cv:Event */
	public static final IRI EVENT;

	/** cv:Evidence */
	public static final IRI EVIDENCE;

	/** cv:EvidenceType */
	public static final IRI EVIDENCE_TYPE;

	/** cv:EvidenceTypeList */
	public static final IRI EVIDENCE_TYPE_LIST;

	/** cv:GenericDate */
	public static final IRI GENERIC_DATE;

	/** cv:ImageObject */
	public static final IRI IMAGE_OBJECT;

	/** cv:InformationConcept */
	public static final IRI INFORMATION_CONCEPT;

	/** cv:InformationRequirement */
	public static final IRI INFORMATION_REQUIREMENT;

	/** cv:LifeEvent */
	public static final IRI LIFE_EVENT;

	/** cv:Output */
	public static final IRI OUTPUT;

	/** cv:Participation */
	public static final IRI PARTICIPATION;

	/** cv:PublicEvent */
	public static final IRI PUBLIC_EVENT;

	/** cv:PublicOrganisation */
	public static final IRI PUBLIC_ORGANISATION;

	/** cv:ReferenceFramework */
	public static final IRI REFERENCE_FRAMEWORK;

	/** cv:Requirement */
	public static final IRI REQUIREMENT;

	/** cv:ServiceConcessionContract */
	public static final IRI SERVICE_CONCESSION_CONTRACT;

	/** cv:SupportedValue */
	public static final IRI SUPPORTED_VALUE;

	// Properties
	/** cv:accessibility */
	public static final IRI ACCESSIBILITY;

	/** cv:audience */
	public static final IRI AUDIENCE;

	/** cv:bias */
	public static final IRI BIAS;

	/** cv:birthDate */
	public static final IRI BIRTH_DATE;

	/** cv:code */
	public static final IRI CODE;

	/** cv:confidentialityLevelType */
	public static final IRI CONFIDENTIALITY_LEVEL_TYPE;

	/** cv:constrains */
	public static final IRI CONSTRAINS;

	/** cv:contactPage */
	public static final IRI CONTACT_PAGE;

	/** cv:contactPoint */
	public static final IRI CONTACT_POINT_PROP;

	/** cv:coordinates */
	public static final IRI COORDINATES;

	/** cv:crs */
	public static final IRI CRS;

	/** cv:currency */
	public static final IRI CURRENCY;

	/** cv:deathDate */
	public static final IRI DEATH_DATE;

	/** cv:email */
	public static final IRI EMAIL;

	/** cv:establishedUnder */
	public static final IRI ESTABLISHED_UNDER;

	/** cv:eventNumber */
	public static final IRI EVENT_NUMBER;

	/** cv:eventStatus */
	public static final IRI EVENT_STATUS;

	/** cv:evidenceTypeClassification */
	public static final IRI EVIDENCE_TYPE_CLASSIFICATION;

	/** cv:expectedNumberOfParticipants */
	public static final IRI EXPECTED_NUMBER_OF_PARTICIPANTS;

	/** cv:expressionOfExpectedValue */
	public static final IRI EXPRESSION_OF_EXPECTED_VALUE;

	/** cv:format */
	public static final IRI FORMAT;

	/** cv:frequency */
	public static final IRI FREQUENCY;

	/** cv:fulfils */
	public static final IRI FULFILS;

	/** cv:gender */
	public static final IRI GENDER;

	/** cv:geometryType */
	public static final IRI GEOMETRY_TYPE;

	/** cv:hasChannel */
	public static final IRI HAS_CHANNEL;

	/** cv:hasCompetentAuthority */
	public static final IRI HAS_COMPETENT_AUTHORITY;

	/** cv:hasConcept */
	public static final IRI HAS_CONCEPT;

	/** cv:hasContractingAuthority */
	public static final IRI HAS_CONTRACTING_AUTHORITY;

	/** cv:hasCost */
	public static final IRI HAS_COST;

	/** cv:hasEconomicOperator */
	public static final IRI HAS_ECONOMIC_OPERATOR;

	/** cv:hasEvidenceTypeList */
	public static final IRI HAS_EVIDENCE_TYPE_LIST;

	/** cv:hasInputType */
	public static final IRI HAS_INPUT_TYPE;

	/** cv:hasLegalResource */
	public static final IRI HAS_LEGAL_RESOURCE;

	/** cv:hasParticipant */
	public static final IRI HAS_PARTICIPANT;

	/** cv:hasParticipation */
	public static final IRI HAS_PARTICIPATION;

	/** cv:hasQualifiedRelation */
	public static final IRI HAS_QUALIFIED_RELATION;

	/** cv:hasRelatedService */
	public static final IRI HAS_RELATED_SERVICE;

	/** cv:hasRequirement */
	public static final IRI HAS_REQUIREMENT;

	/** cv:hasSupportingEvidence */
	public static final IRI HAS_SUPPORTING_EVIDENCE;

	/** cv:hasValue */
	public static final IRI HAS_VALUE;

	/** cv:holdsRequirement */
	public static final IRI HOLDS_REQUIREMENT;

	/** cv:ifAccessedThrough */
	public static final IRI IF_ACCESSED_THROUGH;

	/** cv:isClassifiedBy */
	public static final IRI IS_CLASSIFIED_BY;

	/** cv:isDefinedBy */
	public static final IRI IS_DEFINED_BY;

	/** cv:isDerivedFrom */
	public static final IRI IS_DERIVED_FROM;

	/** cv:isGroupedBy */
	public static final IRI IS_GROUPED_BY;

	/** cv:isRequirementOf */
	public static final IRI IS_REQUIREMENT_OF;

	/** cv:isSpecifiedIn */
	public static final IRI IS_SPECIFIED_IN;

	/** cv:latitude */
	public static final IRI LATITUDE;

	/** cv:level */
	public static final IRI LEVEL;

	/** cv:longitude */
	public static final IRI LONGITUDE;

	/** cv:matronymicName */
	public static final IRI MATRONYMIC_NAME;

	/** cv:openingHours */
	public static final IRI OPENING_HOURS;

	/** cv:ownedBy */
	public static final IRI OWNED_BY;

	/** cv:participates */
	public static final IRI PARTICIPATES;

	/** cv:processingTime */
	public static final IRI PROCESSING_TIME;

	/** cv:providesValueFor */
	public static final IRI PROVIDES_VALUE_FOR;

	/** cv:query */
	public static final IRI QUERY;

	/** cv:registrationDate */
	public static final IRI REGISTRATION_DATE;

	/** cv:registrationPage */
	public static final IRI REGISTRATION_PAGE;

	/** cv:relatedService */
	public static final IRI RELATED_SERVICE;

	/** cv:role */
	public static final IRI ROLE;

	/** cv:sector */
	public static final IRI SECTOR;

	/** cv:sex */
	public static final IRI SEX;

	/** cv:specialOpeningHoursSpecification */
	public static final IRI SPECIAL_OPENING_HOURS_SPECIFICATION;

	/** cv:specifiesEvidenceType */
	public static final IRI SPECIFIES_EVIDENCE_TYPE;

	/** cv:supportsConcept */
	public static final IRI SUPPORTS_CONCEPT;

	/** cv:supportsRequirement */
	public static final IRI SUPPORTS_REQUIREMENT;

	/** cv:supportsValue */
	public static final IRI SUPPORTS_VALUE;

	/** cv:telephone */
	public static final IRI TELEPHONE;

	/** cv:thematicArea */
	public static final IRI THEMATIC_AREA;

	/** cv:validityPeriod */
	public static final IRI VALIDITY_PERIOD;

	/** cv:validityPeriodConstraint */
	public static final IRI VALIDITY_PERIOD_CONSTRAINT;

	/** cv:value */
	public static final IRI VALUE;

	/** cv:weight */
	public static final IRI WEIGHT;

	/** cv:weightingConsiderationDescription */
	public static final IRI WEIGHTING_CONSIDERATION_DESCRIPTION;

	/** cv:weightingType */
	public static final IRI WEIGHTING_TYPE;

	static {
		ACCOUNTING_DOCUMENT = Vocabularies.createIRI(NAMESPACE, "AccountingDocument");
		ADMIN_UNIT = Vocabularies.createIRI(NAMESPACE, "AdminUnit");
		BUSINESS_EVENT = Vocabularies.createIRI(NAMESPACE, "BusinessEvent");
		CHANNEL = Vocabularies.createIRI(NAMESPACE, "Channel");
		CONSTRAINT = Vocabularies.createIRI(NAMESPACE, "Constraint");
		CONTACT_POINT = Vocabularies.createIRI(NAMESPACE, "ContactPoint");
		COST = Vocabularies.createIRI(NAMESPACE, "Cost");
		CRITERION = Vocabularies.createIRI(NAMESPACE, "Criterion");
		EVENT = Vocabularies.createIRI(NAMESPACE, "Event");
		EVIDENCE = Vocabularies.createIRI(NAMESPACE, "Evidence");
		EVIDENCE_TYPE = Vocabularies.createIRI(NAMESPACE, "EvidenceType");
		EVIDENCE_TYPE_LIST = Vocabularies.createIRI(NAMESPACE, "EvidenceTypeList");
		GENERIC_DATE = Vocabularies.createIRI(NAMESPACE, "GenericDate");
		IMAGE_OBJECT = Vocabularies.createIRI(NAMESPACE, "ImageObject");
		INFORMATION_CONCEPT = Vocabularies.createIRI(NAMESPACE, "InformationConcept");
		INFORMATION_REQUIREMENT = Vocabularies.createIRI(NAMESPACE, "InformationRequirement");
		LIFE_EVENT = Vocabularies.createIRI(NAMESPACE, "LifeEvent");
		OUTPUT = Vocabularies.createIRI(NAMESPACE, "Output");
		PARTICIPATION = Vocabularies.createIRI(NAMESPACE, "Participation");
		PUBLIC_EVENT = Vocabularies.createIRI(NAMESPACE, "PublicEvent");
		PUBLIC_ORGANISATION = Vocabularies.createIRI(NAMESPACE, "PublicOrganisation");
		REFERENCE_FRAMEWORK = Vocabularies.createIRI(NAMESPACE, "ReferenceFramework");
		REQUIREMENT = Vocabularies.createIRI(NAMESPACE, "Requirement");
		SERVICE_CONCESSION_CONTRACT = Vocabularies.createIRI(NAMESPACE, "ServiceConcessionContract");
		SUPPORTED_VALUE = Vocabularies.createIRI(NAMESPACE, "SupportedValue");

		ACCESSIBILITY = Vocabularies.createIRI(NAMESPACE, "accessibility");
		AUDIENCE = Vocabularies.createIRI(NAMESPACE, "audience");
		BIAS = Vocabularies.createIRI(NAMESPACE, "bias");
		BIRTH_DATE = Vocabularies.createIRI(NAMESPACE, "birthDate");
		CODE = Vocabularies.createIRI(NAMESPACE, "code");
		CONFIDENTIALITY_LEVEL_TYPE = Vocabularies.createIRI(NAMESPACE, "confidentialityLevelType");
		CONSTRAINS = Vocabularies.createIRI(NAMESPACE, "constrains");
		CONTACT_PAGE = Vocabularies.createIRI(NAMESPACE, "contactPage");
		CONTACT_POINT_PROP = Vocabularies.createIRI(NAMESPACE, "contactPoint");
		COORDINATES = Vocabularies.createIRI(NAMESPACE, "coordinates");
		CRS = Vocabularies.createIRI(NAMESPACE, "crs");
		CURRENCY = Vocabularies.createIRI(NAMESPACE, "currency");
		DEATH_DATE = Vocabularies.createIRI(NAMESPACE, "deathDate");
		EMAIL = Vocabularies.createIRI(NAMESPACE, "email");
		ESTABLISHED_UNDER = Vocabularies.createIRI(NAMESPACE, "establishedUnder");
		EVENT_NUMBER = Vocabularies.createIRI(NAMESPACE, "eventNumber");
		EVENT_STATUS = Vocabularies.createIRI(NAMESPACE, "eventStatus");
		EVIDENCE_TYPE_CLASSIFICATION = Vocabularies.createIRI(NAMESPACE, "evidenceTypeClassification");
		EXPECTED_NUMBER_OF_PARTICIPANTS = Vocabularies.createIRI(NAMESPACE, "expectedNumberOfParticipants");
		EXPRESSION_OF_EXPECTED_VALUE = Vocabularies.createIRI(NAMESPACE, "expressionOfExpectedValue");
		FORMAT = Vocabularies.createIRI(NAMESPACE, "format");
		FREQUENCY = Vocabularies.createIRI(NAMESPACE, "frequency");
		FULFILS = Vocabularies.createIRI(NAMESPACE, "fulfils");
		GENDER = Vocabularies.createIRI(NAMESPACE, "gender");
		GEOMETRY_TYPE = Vocabularies.createIRI(NAMESPACE, "geometryType");
		HAS_CHANNEL = Vocabularies.createIRI(NAMESPACE, "hasChannel");
		HAS_COMPETENT_AUTHORITY = Vocabularies.createIRI(NAMESPACE, "hasCompetentAuthority");
		HAS_CONCEPT = Vocabularies.createIRI(NAMESPACE, "hasConcept");
		HAS_CONTRACTING_AUTHORITY = Vocabularies.createIRI(NAMESPACE, "hasContractingAuthority");
		HAS_COST = Vocabularies.createIRI(NAMESPACE, "hasCost");
		HAS_ECONOMIC_OPERATOR = Vocabularies.createIRI(NAMESPACE, "hasEconomicOperator");
		HAS_EVIDENCE_TYPE_LIST = Vocabularies.createIRI(NAMESPACE, "hasEvidenceTypeList");
		HAS_INPUT_TYPE = Vocabularies.createIRI(NAMESPACE, "hasInputType");
		HAS_LEGAL_RESOURCE = Vocabularies.createIRI(NAMESPACE, "hasLegalResource");
		HAS_PARTICIPANT = Vocabularies.createIRI(NAMESPACE, "hasParticipant");
		HAS_PARTICIPATION = Vocabularies.createIRI(NAMESPACE, "hasParticipation");
		HAS_QUALIFIED_RELATION = Vocabularies.createIRI(NAMESPACE, "hasQualifiedRelation");
		HAS_RELATED_SERVICE = Vocabularies.createIRI(NAMESPACE, "hasRelatedService");
		HAS_REQUIREMENT = Vocabularies.createIRI(NAMESPACE, "hasRequirement");
		HAS_SUPPORTING_EVIDENCE = Vocabularies.createIRI(NAMESPACE, "hasSupportingEvidence");
		HAS_VALUE = Vocabularies.createIRI(NAMESPACE, "hasValue");
		HOLDS_REQUIREMENT = Vocabularies.createIRI(NAMESPACE, "holdsRequirement");
		IF_ACCESSED_THROUGH = Vocabularies.createIRI(NAMESPACE, "ifAccessedThrough");
		IS_CLASSIFIED_BY = Vocabularies.createIRI(NAMESPACE, "isClassifiedBy");
		IS_DEFINED_BY = Vocabularies.createIRI(NAMESPACE, "isDefinedBy");
		IS_DERIVED_FROM = Vocabularies.createIRI(NAMESPACE, "isDerivedFrom");
		IS_GROUPED_BY = Vocabularies.createIRI(NAMESPACE, "isGroupedBy");
		IS_REQUIREMENT_OF = Vocabularies.createIRI(NAMESPACE, "isRequirementOf");
		IS_SPECIFIED_IN = Vocabularies.createIRI(NAMESPACE, "isSpecifiedIn");
		LATITUDE = Vocabularies.createIRI(NAMESPACE, "latitude");
		LEVEL = Vocabularies.createIRI(NAMESPACE, "level");
		LONGITUDE = Vocabularies.createIRI(NAMESPACE, "longitude");
		MATRONYMIC_NAME = Vocabularies.createIRI(NAMESPACE, "matronymicName");
		OPENING_HOURS = Vocabularies.createIRI(NAMESPACE, "openingHours");
		OWNED_BY = Vocabularies.createIRI(NAMESPACE, "ownedBy");
		PARTICIPATES = Vocabularies.createIRI(NAMESPACE, "participates");
		PROCESSING_TIME = Vocabularies.createIRI(NAMESPACE, "processingTime");
		PROVIDES_VALUE_FOR = Vocabularies.createIRI(NAMESPACE, "providesValueFor");
		QUERY = Vocabularies.createIRI(NAMESPACE, "query");
		REGISTRATION_DATE = Vocabularies.createIRI(NAMESPACE, "registrationDate");
		REGISTRATION_PAGE = Vocabularies.createIRI(NAMESPACE, "registrationPage");
		RELATED_SERVICE = Vocabularies.createIRI(NAMESPACE, "relatedService");
		ROLE = Vocabularies.createIRI(NAMESPACE, "role");
		SECTOR = Vocabularies.createIRI(NAMESPACE, "sector");
		SEX = Vocabularies.createIRI(NAMESPACE, "sex");
		SPECIAL_OPENING_HOURS_SPECIFICATION = Vocabularies.createIRI(NAMESPACE, "specialOpeningHoursSpecification");
		SPECIFIES_EVIDENCE_TYPE = Vocabularies.createIRI(NAMESPACE, "specifiesEvidenceType");
		SUPPORTS_CONCEPT = Vocabularies.createIRI(NAMESPACE, "supportsConcept");
		SUPPORTS_REQUIREMENT = Vocabularies.createIRI(NAMESPACE, "supportsRequirement");
		SUPPORTS_VALUE = Vocabularies.createIRI(NAMESPACE, "supportsValue");
		TELEPHONE = Vocabularies.createIRI(NAMESPACE, "telephone");
		THEMATIC_AREA = Vocabularies.createIRI(NAMESPACE, "thematicArea");
		VALIDITY_PERIOD = Vocabularies.createIRI(NAMESPACE, "validityPeriod");
		VALIDITY_PERIOD_CONSTRAINT = Vocabularies.createIRI(NAMESPACE, "validityPeriodConstraint");
		VALUE = Vocabularies.createIRI(NAMESPACE, "value");
		WEIGHT = Vocabularies.createIRI(NAMESPACE, "weight");
		WEIGHTING_CONSIDERATION_DESCRIPTION = Vocabularies.createIRI(NAMESPACE, "weightingConsiderationDescription");
		WEIGHTING_TYPE = Vocabularies.createIRI(NAMESPACE, "weightingType");

	}
}
