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
	public static final IRI AccountingDocument;

	/** cv:AdminUnit */
	public static final IRI AdminUnit;

	/** cv:BusinessEvent */
	public static final IRI BusinessEvent;

	/** cv:Channel */
	public static final IRI Channel;

	/** cv:Constraint */
	public static final IRI Constraint;

	/** cv:ContactPoint */
	public static final IRI ContactPoint;

	/** cv:Cost */
	public static final IRI Cost;

	/** cv:Criterion */
	public static final IRI Criterion;

	/** cv:Event */
	public static final IRI Event;

	/** cv:Evidence */
	public static final IRI Evidence;

	/** cv:EvidenceType */
	public static final IRI EvidenceType;

	/** cv:EvidenceTypeList */
	public static final IRI EvidenceTypeList;

	/** cv:GenericDate */
	public static final IRI GenericDate;

	/** cv:ImageObject */
	public static final IRI ImageObject;

	/** cv:InformationConcept */
	public static final IRI InformationConcept;

	/** cv:InformationRequirement */
	public static final IRI InformationRequirement;

	/** cv:LifeEvent */
	public static final IRI LifeEvent;

	/** cv:Output */
	public static final IRI Output;

	/** cv:Participation */
	public static final IRI Participation;

	/** cv:PublicEvent */
	public static final IRI PublicEvent;

	/** cv:PublicOrganisation */
	public static final IRI PublicOrganisation;

	/** cv:ReferenceFramework */
	public static final IRI ReferenceFramework;

	/** cv:Requirement */
	public static final IRI Requirement;

	/** cv:ServiceConcessionContract */
	public static final IRI ServiceConcessionContract;

	/** cv:SupportedValue */
	public static final IRI SupportedValue;

	// Properties
	/** cv:accessibility */
	public static final IRI accessibility;

	/** cv:audience */
	public static final IRI audience;

	/** cv:bias */
	public static final IRI bias;

	/** cv:birthDate */
	public static final IRI birthDate;

	/** cv:code */
	public static final IRI code;

	/** cv:confidentialityLevelType */
	public static final IRI confidentialityLevelType;

	/** cv:constrains */
	public static final IRI constrains;

	/** cv:contactPage */
	public static final IRI contactPage;

	/** cv:contactPoint */
	public static final IRI contactPoint;

	/** cv:coordinates */
	public static final IRI coordinates;

	/** cv:crs */
	public static final IRI crs;

	/** cv:currency */
	public static final IRI currency;

	/** cv:deathDate */
	public static final IRI deathDate;

	/** cv:email */
	public static final IRI email;

	/** cv:establishedUnder */
	public static final IRI establishedUnder;

	/** cv:eventNumber */
	public static final IRI eventNumber;

	/** cv:eventStatus */
	public static final IRI eventStatus;

	/** cv:evidenceTypeClassification */
	public static final IRI evidenceTypeClassification;

	/** cv:expectedNumberOfParticipants */
	public static final IRI expectedNumberOfParticipants;

	/** cv:expressionOfExpectedValue */
	public static final IRI expressionOfExpectedValue;

	/** cv:format */
	public static final IRI format;

	/** cv:frequency */
	public static final IRI frequency;

	/** cv:fulfils */
	public static final IRI fulfils;

	/** cv:gender */
	public static final IRI gender;

	/** cv:geometryType */
	public static final IRI geometryType;

	/** cv:hasChannel */
	public static final IRI hasChannel;

	/** cv:hasCompetentAuthority */
	public static final IRI hasCompetentAuthority;

	/** cv:hasConcept */
	public static final IRI hasConcept;

	/** cv:hasContractingAuthority */
	public static final IRI hasContractingAuthority;

	/** cv:hasCost */
	public static final IRI hasCost;

	/** cv:hasEconomicOperator */
	public static final IRI hasEconomicOperator;

	/** cv:hasEvidenceTypeList */
	public static final IRI hasEvidenceTypeList;

	/** cv:hasInputType */
	public static final IRI hasInputType;

	/** cv:hasLegalResource */
	public static final IRI hasLegalResource;

	/** cv:hasParticipant */
	public static final IRI hasParticipant;

	/** cv:hasParticipation */
	public static final IRI hasParticipation;

	/** cv:hasQualifiedRelation */
	public static final IRI hasQualifiedRelation;

	/** cv:hasRelatedService */
	public static final IRI hasRelatedService;

	/** cv:hasRequirement */
	public static final IRI hasRequirement;

	/** cv:hasSupportingEvidence */
	public static final IRI hasSupportingEvidence;

	/** cv:hasValue */
	public static final IRI hasValue;

	/** cv:holdsRequirement */
	public static final IRI holdsRequirement;

	/** cv:ifAccessedThrough */
	public static final IRI ifAccessedThrough;

	/** cv:isClassifiedBy */
	public static final IRI isClassifiedBy;

	/** cv:isDefinedBy */
	public static final IRI isDefinedBy;

	/** cv:isDerivedFrom */
	public static final IRI isDerivedFrom;

	/** cv:isGroupedBy */
	public static final IRI isGroupedBy;

	/** cv:isRequirementOf */
	public static final IRI isRequirementOf;

	/** cv:isSpecifiedIn */
	public static final IRI isSpecifiedIn;

	/** cv:latitude */
	public static final IRI latitude;

	/** cv:level */
	public static final IRI level;

	/** cv:longitude */
	public static final IRI longitude;

	/** cv:matronymicName */
	public static final IRI matronymicName;

	/** cv:openingHours */
	public static final IRI openingHours;

	/** cv:ownedBy */
	public static final IRI ownedBy;

	/** cv:participates */
	public static final IRI participates;

	/** cv:processingTime */
	public static final IRI processingTime;

	/** cv:providesValueFor */
	public static final IRI providesValueFor;

	/** cv:query */
	public static final IRI query;

	/** cv:registrationDate */
	public static final IRI registrationDate;

	/** cv:registrationPage */
	public static final IRI registrationPage;

	/** cv:relatedService */
	public static final IRI relatedService;

	/** cv:role */
	public static final IRI role;

	/** cv:sector */
	public static final IRI sector;

	/** cv:sex */
	public static final IRI sex;

	/** cv:specialOpeningHoursSpecification */
	public static final IRI specialOpeningHoursSpecification;

	/** cv:specifiesEvidenceType */
	public static final IRI specifiesEvidenceType;

	/** cv:supportsConcept */
	public static final IRI supportsConcept;

	/** cv:supportsRequirement */
	public static final IRI supportsRequirement;

	/** cv:supportsValue */
	public static final IRI supportsValue;

	/** cv:telephone */
	public static final IRI telephone;

	/** cv:thematicArea */
	public static final IRI thematicArea;

	/** cv:validityPeriod */
	public static final IRI validityPeriod;

	/** cv:validityPeriodConstraint */
	public static final IRI validityPeriodConstraint;

	/** cv:value */
	public static final IRI value;

	/** cv:weight */
	public static final IRI weight;

	/** cv:weightingConsiderationDescription */
	public static final IRI weightingConsiderationDescription;

	/** cv:weightingType */
	public static final IRI weightingType;

	static {
		AccountingDocument = Vocabularies.createIRI(NAMESPACE, "AccountingDocument");
		AdminUnit = Vocabularies.createIRI(NAMESPACE, "AdminUnit");
		BusinessEvent = Vocabularies.createIRI(NAMESPACE, "BusinessEvent");
		Channel = Vocabularies.createIRI(NAMESPACE, "Channel");
		Constraint = Vocabularies.createIRI(NAMESPACE, "Constraint");
		ContactPoint = Vocabularies.createIRI(NAMESPACE, "ContactPoint");
		Cost = Vocabularies.createIRI(NAMESPACE, "Cost");
		Criterion = Vocabularies.createIRI(NAMESPACE, "Criterion");
		Event = Vocabularies.createIRI(NAMESPACE, "Event");
		Evidence = Vocabularies.createIRI(NAMESPACE, "Evidence");
		EvidenceType = Vocabularies.createIRI(NAMESPACE, "EvidenceType");
		EvidenceTypeList = Vocabularies.createIRI(NAMESPACE, "EvidenceTypeList");
		GenericDate = Vocabularies.createIRI(NAMESPACE, "GenericDate");
		ImageObject = Vocabularies.createIRI(NAMESPACE, "ImageObject");
		InformationConcept = Vocabularies.createIRI(NAMESPACE, "InformationConcept");
		InformationRequirement = Vocabularies.createIRI(NAMESPACE, "InformationRequirement");
		LifeEvent = Vocabularies.createIRI(NAMESPACE, "LifeEvent");
		Output = Vocabularies.createIRI(NAMESPACE, "Output");
		Participation = Vocabularies.createIRI(NAMESPACE, "Participation");
		PublicEvent = Vocabularies.createIRI(NAMESPACE, "PublicEvent");
		PublicOrganisation = Vocabularies.createIRI(NAMESPACE, "PublicOrganisation");
		ReferenceFramework = Vocabularies.createIRI(NAMESPACE, "ReferenceFramework");
		Requirement = Vocabularies.createIRI(NAMESPACE, "Requirement");
		ServiceConcessionContract = Vocabularies.createIRI(NAMESPACE, "ServiceConcessionContract");
		SupportedValue = Vocabularies.createIRI(NAMESPACE, "SupportedValue");

		accessibility = Vocabularies.createIRI(NAMESPACE, "accessibility");
		audience = Vocabularies.createIRI(NAMESPACE, "audience");
		bias = Vocabularies.createIRI(NAMESPACE, "bias");
		birthDate = Vocabularies.createIRI(NAMESPACE, "birthDate");
		code = Vocabularies.createIRI(NAMESPACE, "code");
		confidentialityLevelType = Vocabularies.createIRI(NAMESPACE, "confidentialityLevelType");
		constrains = Vocabularies.createIRI(NAMESPACE, "constrains");
		contactPage = Vocabularies.createIRI(NAMESPACE, "contactPage");
		contactPoint = Vocabularies.createIRI(NAMESPACE, "contactPoint");
		coordinates = Vocabularies.createIRI(NAMESPACE, "coordinates");
		crs = Vocabularies.createIRI(NAMESPACE, "crs");
		currency = Vocabularies.createIRI(NAMESPACE, "currency");
		deathDate = Vocabularies.createIRI(NAMESPACE, "deathDate");
		email = Vocabularies.createIRI(NAMESPACE, "email");
		establishedUnder = Vocabularies.createIRI(NAMESPACE, "establishedUnder");
		eventNumber = Vocabularies.createIRI(NAMESPACE, "eventNumber");
		eventStatus = Vocabularies.createIRI(NAMESPACE, "eventStatus");
		evidenceTypeClassification = Vocabularies.createIRI(NAMESPACE, "evidenceTypeClassification");
		expectedNumberOfParticipants = Vocabularies.createIRI(NAMESPACE, "expectedNumberOfParticipants");
		expressionOfExpectedValue = Vocabularies.createIRI(NAMESPACE, "expressionOfExpectedValue");
		format = Vocabularies.createIRI(NAMESPACE, "format");
		frequency = Vocabularies.createIRI(NAMESPACE, "frequency");
		fulfils = Vocabularies.createIRI(NAMESPACE, "fulfils");
		gender = Vocabularies.createIRI(NAMESPACE, "gender");
		geometryType = Vocabularies.createIRI(NAMESPACE, "geometryType");
		hasChannel = Vocabularies.createIRI(NAMESPACE, "hasChannel");
		hasCompetentAuthority = Vocabularies.createIRI(NAMESPACE, "hasCompetentAuthority");
		hasConcept = Vocabularies.createIRI(NAMESPACE, "hasConcept");
		hasContractingAuthority = Vocabularies.createIRI(NAMESPACE, "hasContractingAuthority");
		hasCost = Vocabularies.createIRI(NAMESPACE, "hasCost");
		hasEconomicOperator = Vocabularies.createIRI(NAMESPACE, "hasEconomicOperator");
		hasEvidenceTypeList = Vocabularies.createIRI(NAMESPACE, "hasEvidenceTypeList");
		hasInputType = Vocabularies.createIRI(NAMESPACE, "hasInputType");
		hasLegalResource = Vocabularies.createIRI(NAMESPACE, "hasLegalResource");
		hasParticipant = Vocabularies.createIRI(NAMESPACE, "hasParticipant");
		hasParticipation = Vocabularies.createIRI(NAMESPACE, "hasParticipation");
		hasQualifiedRelation = Vocabularies.createIRI(NAMESPACE, "hasQualifiedRelation");
		hasRelatedService = Vocabularies.createIRI(NAMESPACE, "hasRelatedService");
		hasRequirement = Vocabularies.createIRI(NAMESPACE, "hasRequirement");
		hasSupportingEvidence = Vocabularies.createIRI(NAMESPACE, "hasSupportingEvidence");
		hasValue = Vocabularies.createIRI(NAMESPACE, "hasValue");
		holdsRequirement = Vocabularies.createIRI(NAMESPACE, "holdsRequirement");
		ifAccessedThrough = Vocabularies.createIRI(NAMESPACE, "ifAccessedThrough");
		isClassifiedBy = Vocabularies.createIRI(NAMESPACE, "isClassifiedBy");
		isDefinedBy = Vocabularies.createIRI(NAMESPACE, "isDefinedBy");
		isDerivedFrom = Vocabularies.createIRI(NAMESPACE, "isDerivedFrom");
		isGroupedBy = Vocabularies.createIRI(NAMESPACE, "isGroupedBy");
		isRequirementOf = Vocabularies.createIRI(NAMESPACE, "isRequirementOf");
		isSpecifiedIn = Vocabularies.createIRI(NAMESPACE, "isSpecifiedIn");
		latitude = Vocabularies.createIRI(NAMESPACE, "latitude");
		level = Vocabularies.createIRI(NAMESPACE, "level");
		longitude = Vocabularies.createIRI(NAMESPACE, "longitude");
		matronymicName = Vocabularies.createIRI(NAMESPACE, "matronymicName");
		openingHours = Vocabularies.createIRI(NAMESPACE, "openingHours");
		ownedBy = Vocabularies.createIRI(NAMESPACE, "ownedBy");
		participates = Vocabularies.createIRI(NAMESPACE, "participates");
		processingTime = Vocabularies.createIRI(NAMESPACE, "processingTime");
		providesValueFor = Vocabularies.createIRI(NAMESPACE, "providesValueFor");
		query = Vocabularies.createIRI(NAMESPACE, "query");
		registrationDate = Vocabularies.createIRI(NAMESPACE, "registrationDate");
		registrationPage = Vocabularies.createIRI(NAMESPACE, "registrationPage");
		relatedService = Vocabularies.createIRI(NAMESPACE, "relatedService");
		role = Vocabularies.createIRI(NAMESPACE, "role");
		sector = Vocabularies.createIRI(NAMESPACE, "sector");
		sex = Vocabularies.createIRI(NAMESPACE, "sex");
		specialOpeningHoursSpecification = Vocabularies.createIRI(NAMESPACE, "specialOpeningHoursSpecification");
		specifiesEvidenceType = Vocabularies.createIRI(NAMESPACE, "specifiesEvidenceType");
		supportsConcept = Vocabularies.createIRI(NAMESPACE, "supportsConcept");
		supportsRequirement = Vocabularies.createIRI(NAMESPACE, "supportsRequirement");
		supportsValue = Vocabularies.createIRI(NAMESPACE, "supportsValue");
		telephone = Vocabularies.createIRI(NAMESPACE, "telephone");
		thematicArea = Vocabularies.createIRI(NAMESPACE, "thematicArea");
		validityPeriod = Vocabularies.createIRI(NAMESPACE, "validityPeriod");
		validityPeriodConstraint = Vocabularies.createIRI(NAMESPACE, "validityPeriodConstraint");
		value = Vocabularies.createIRI(NAMESPACE, "value");
		weight = Vocabularies.createIRI(NAMESPACE, "weight");
		weightingConsiderationDescription = Vocabularies.createIRI(NAMESPACE, "weightingConsiderationDescription");
		weightingType = Vocabularies.createIRI(NAMESPACE, "weightingType");
	}
}
