/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.model.vocabulary;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;

/**
 * Constants for the Provenance Ontology.
 *
 * @author Bart Hanssens
 * @see <a href="https://www.w3.org/TR/prov-overview/">Provenance Ontology</a>
 */
public class PROV {
	/**
	 * The PROV-O namespace: http://www.w3.org/ns/prov#
	 */
	public static final String NAMESPACE = "http://www.w3.org/ns/prov#";

	/**
	 * Recommended prefix for the namespace: "prov"
	 */
	public static final String PREFIX = "prov";

	/**
	 * An immutable {@link Namespace} constant that represents the namespace.
	 */
	public static final Namespace NS = Vocabularies.createNamespace(PREFIX, NAMESPACE);

	// Classes
	/** prov:Accept */
	public static final IRI ACCEPT;

	/** prov:Activity */
	public static final IRI ACTIVITY;

	/** prov:ActivityInfluence */
	public static final IRI ACTIVITY_INFLUENCE;

	/** prov:Agent */
	public static final IRI AGENT;

	/** prov:AgentInfluence */
	public static final IRI AGENT_INFLUENCE;

	/** prov:Association */
	public static final IRI ASSOCIATION;

	/** prov:Attribution */
	public static final IRI ATTRIBUTION;

	/** prov:Bundle */
	public static final IRI BUNDLE;

	/** prov:Collection */
	public static final IRI COLLECTION;

	/** prov:Communication */
	public static final IRI COMMUNICATION;

	/** prov:Contribute */
	public static final IRI CONTRIBUTE;

	/** prov:Contributor */
	public static final IRI CONTRIBUTOR;

	/** prov:Copyright */
	public static final IRI COPYRIGHT;

	/** prov:Create */
	public static final IRI CREATE;

	/** prov:Creator */
	public static final IRI CREATOR;

	/** prov:Delegation */
	public static final IRI DELEGATION;

	/** prov:Derivation */
	public static final IRI DERIVATION;

	/** prov:Dictionary */
	public static final IRI DICTIONARY;

	/** prov:DirectQueryService */
	public static final IRI DIRECT_QUERY_SERVICE;

	/** prov:EmptyCollection */
	public static final IRI EMPTY_COLLECTION;

	/** prov:EmptyDictionary */
	public static final IRI EMPTY_DICTIONARY;

	/** prov:End */
	public static final IRI END;

	/** prov:Entity */
	public static final IRI ENTITY;

	/** prov:EntityInfluence */
	public static final IRI ENTITY_INFLUENCE;

	/** prov:Generation */
	public static final IRI GENERATION;

	/** prov:Influence */
	public static final IRI INFLUENCE;

	/** prov:Insertion */
	public static final IRI INSERTION;

	/** prov:InstantaneousEvent */
	public static final IRI INSTANTANEOUS_EVENT;

	/** prov:Invalidation */
	public static final IRI INVALIDATION;

	/** prov:KeyEntityPair */
	public static final IRI KEY_ENTITY_PAIR;

	/** prov:Location */
	public static final IRI LOCATION;

	/** prov:Modify */
	public static final IRI MODIFY;

	/** prov:Organization */
	public static final IRI ORGANIZATION;

	/** prov:Person */
	public static final IRI PERSON;

	/** prov:Plan */
	public static final IRI PLAN;

	/** prov:PrimarySource */
	public static final IRI PRIMARY_SOURCE;

	/** prov:Publish */
	public static final IRI PUBLISH;

	/** prov:Publisher */
	public static final IRI PUBLISHER;

	/** prov:Quotation */
	public static final IRI QUOTATION;

	/** prov:Removal */
	public static final IRI REMOVAL;

	/** prov:Replace */
	public static final IRI REPLACE;

	/** prov:Revision */
	public static final IRI REVISION;

	/** prov:RightsAssignment */
	public static final IRI RIGHTS_ASSIGNMENT;

	/** prov:RightsHolder */
	public static final IRI RIGHTS_HOLDER;

	/** prov:Role */
	public static final IRI ROLE;

	/** prov:ServiceDescription */
	public static final IRI SERVICE_DESCRIPTION;

	/** prov:SoftwareAgent */
	public static final IRI SOFTWARE_AGENT;

	/** prov:Start */
	public static final IRI START;

	/** prov:Submit */
	public static final IRI SUBMIT;

	/** prov:Usage */
	public static final IRI USAGE;

	// Properties
	/** prov:actedOnBehalfOf */
	public static final IRI ACTED_ON_BEHALF_OF;

	/** prov:activity */
	public static final IRI ACTIVITY_PROP;

	/** prov:agent */
	public static final IRI AGENT_PROP;

	/** prov:alternateOf */
	public static final IRI ALTERNATE_OF;

	/** prov:asInBundle */
	public static final IRI AS_IN_BUNDLE;

	/** prov:atLocation */
	public static final IRI AT_LOCATION;

	/** prov:atTime */
	public static final IRI AT_TIME;

	/** prov:derivedByInsertionFrom */
	public static final IRI DERIVED_BY_INSERTION_FROM;

	/** prov:derivedByRemovalFrom */
	public static final IRI DERIVED_BY_REMOVAL_FROM;

	/** prov:describesService */
	public static final IRI DESCRIBES_SERVICE;

	/** prov:dictionary */
	public static final IRI DICTIONARY_PROP;

	/** prov:endedAtTime */
	public static final IRI ENDED_AT_TIME;

	/** prov:entity */
	public static final IRI ENTITY_PROP;

	/** prov:generated */
	public static final IRI GENERATED;

	/** prov:generatedAtTime */
	public static final IRI GENERATED_AT_TIME;

	/** prov:hadActivity */
	public static final IRI HAD_ACTIVITY;

	/** prov:hadDictionaryMember */
	public static final IRI HAD_DICTIONARY_MEMBER;

	/** prov:hadGeneration */
	public static final IRI HAD_GENERATION;

	/** prov:hadMember */
	public static final IRI HAD_MEMBER;

	/** prov:hadPlan */
	public static final IRI HAD_PLAN;

	/** prov:hadPrimarySource */
	public static final IRI HAD_PRIMARY_SOURCE;

	/** prov:hadRole */
	public static final IRI HAD_ROLE;

	/** prov:hadUsage */
	public static final IRI HAD_USAGE;

	/** prov:has_anchor */
	public static final IRI HAS_ANCHOR;

	/** prov:has_provenance */
	public static final IRI HAS_PROVENANCE;

	/** prov:has_query_service */
	public static final IRI HAS_QUERY_SERVICE;

	/** prov:influenced */
	public static final IRI INFLUENCED;

	/** prov:influencer */
	public static final IRI INFLUENCER;

	/** prov:insertedKeyEntityPair */
	public static final IRI INSERTED_KEY_ENTITY_PAIR;

	/** prov:invalidated */
	public static final IRI INVALIDATED;

	/** prov:invalidatedAtTime */
	public static final IRI INVALIDATED_AT_TIME;

	/** prov:mentionOf */
	public static final IRI MENTION_OF;

	/** prov:pairEntity */
	public static final IRI PAIR_ENTITY;

	/** prov:pairKey */
	public static final IRI PAIR_KEY;

	/** prov:pingback */
	public static final IRI PINGBACK;

	/** prov:provenanceUriTemplate */
	public static final IRI PROVENANCE_URI_TEMPLATE;

	/** prov:qualifiedAssociation */
	public static final IRI QUALIFIED_ASSOCIATION;

	/** prov:qualifiedAttribution */
	public static final IRI QUALIFIED_ATTRIBUTION;

	/** prov:qualifiedCommunication */
	public static final IRI QUALIFIED_COMMUNICATION;

	/** prov:qualifiedDelegation */
	public static final IRI QUALIFIED_DELEGATION;

	/** prov:qualifiedDerivation */
	public static final IRI QUALIFIED_DERIVATION;

	/** prov:qualifiedEnd */
	public static final IRI QUALIFIED_END;

	/** prov:qualifiedGeneration */
	public static final IRI QUALIFIED_GENERATION;

	/** prov:qualifiedInfluence */
	public static final IRI QUALIFIED_INFLUENCE;

	/** prov:qualifiedInsertion */
	public static final IRI QUALIFIED_INSERTION;

	/** prov:qualifiedInvalidation */
	public static final IRI QUALIFIED_INVALIDATION;

	/** prov:qualifiedPrimarySource */
	public static final IRI QUALIFIED_PRIMARY_SOURCE;

	/** prov:qualifiedQuotation */
	public static final IRI QUALIFIED_QUOTATION;

	/** prov:qualifiedRemoval */
	public static final IRI QUALIFIED_REMOVAL;

	/** prov:qualifiedRevision */
	public static final IRI QUALIFIED_REVISION;

	/** prov:qualifiedStart */
	public static final IRI QUALIFIED_START;

	/** prov:qualifiedUsage */
	public static final IRI QUALIFIED_USAGE;

	/** prov:removedKey */
	public static final IRI REMOVED_KEY;

	/** prov:specializationOf */
	public static final IRI SPECIALIZATION_OF;

	/** prov:startedAtTime */
	public static final IRI STARTED_AT_TIME;

	/** prov:used */
	public static final IRI USED;

	/** prov:value */
	public static final IRI VALUE;

	/** prov:wasAssociatedWith */
	public static final IRI WAS_ASSOCIATED_WITH;

	/** prov:wasAttributedTo */
	public static final IRI WAS_ATTRIBUTED_TO;

	/** prov:wasDerivedFrom */
	public static final IRI WAS_DERIVED_FROM;

	/** prov:wasEndedBy */
	public static final IRI WAS_ENDED_BY;

	/** prov:wasGeneratedBy */
	public static final IRI WAS_GENERATED_BY;

	/** prov:wasInfluencedBy */
	public static final IRI WAS_INFLUENCED_BY;

	/** prov:wasInformedBy */
	public static final IRI WAS_INFORMED_BY;

	/** prov:wasInvalidatedBy */
	public static final IRI WAS_INVALIDATED_BY;

	/** prov:wasQuotedFrom */
	public static final IRI WAS_QUOTED_FROM;

	/** prov:wasRevisionOf */
	public static final IRI WAS_REVISION_OF;

	/** prov:wasStartedBy */
	public static final IRI WAS_STARTED_BY;

	// recommended inverse properties

	/** prov:activityOfInfluence – inverse of prov:hadActivity */
	public static final IRI ACTIVITY_OF_INFLUENCE;

	/** prov:agentOfInfluence – inverse of prov:agent */
	public static final IRI AGENT_OF_INFLUENCE;

	/** prov:contributed – inverse of prov:wasAttributedTo */
	public static final IRI CONTRIBUTED;

	/** prov:ended – inverse of prov:wasEndedBy */
	public static final IRI ENDED;

	/** prov:entityOfInfluence – inverse of prov:entity */
	public static final IRI ENTITY_OF_INFLUENCE;

	/** prov:generalizationOf – inverse of prov:specializationOf */
	public static final IRI GENERALIZATION_OF;

	/** prov:hadDelegate – inverse of prov:actedOnBehalfOf */
	public static final IRI HAD_DELEGATE;

	/** prov:hadDerivation – inverse of prov:wasDerivedFrom */
	public static final IRI HAD_DERIVATION;

	/** prov:hadInfluence – inverse of prov:influencer */
	public static final IRI HAD_INFLUENCE;

	/** prov:hadRevision – inverse of prov:wasRevisionOf */
	public static final IRI HAD_REVISION;

	/** prov:informed – inverse of prov:wasInformedBy */
	public static final IRI INFORMED;

	/** prov:locationOf – inverse of prov:atLocation */
	public static final IRI LOCATION_OF;

	/** prov:qualifiedAssociationOf – inverse of prov:qualifiedAssociation */
	public static final IRI QUALIFIED_ASSOCIATION_OF;

	/** prov:qualifiedAttributionOf – inverse of prov:qualifiedAttribution */
	public static final IRI QUALIFIED_ATTRIBUTION_OF;

	/** prov:qualifiedCommunicationOf – inverse of prov:qualifiedCommunication */
	public static final IRI QUALIFIED_COMMUNICATION_OF;

	/** prov:qualifiedDelegationOf – inverse of prov:qualifiedDelegation */
	public static final IRI QUALIFIED_DELEGATION_OF;

	/** prov:qualifiedDerivationOf – inverse of prov:qualifiedDerivation */
	public static final IRI QUALIFIED_DERIVATION_OF;

	/** prov:qualifiedEndOf – inverse of prov:qualifiedEnd */
	public static final IRI QUALIFIED_END_OF;

	/** prov:qualifiedGenerationOf – inverse of prov:qualifiedGeneration */
	public static final IRI QUALIFIED_GENERATION_OF;

	/** prov:qualifiedInfluenceOf – inverse of prov:qualifiedInfluence */
	public static final IRI QUALIFIED_INFLUENCE_OF;

	/** prov:qualifiedInvalidationOf – inverse of prov:qualifiedInvalidation */
	public static final IRI QUALIFIED_INVALIDATION_OF;

	/** prov:qualifiedQuotationOf – inverse of prov:qualifiedQuotation */
	public static final IRI QUALIFIED_QUOTATION_OF;

	/** prov:qualifiedSourceOf – inverse of prov:qualifiedPrimarySource */
	public static final IRI QUALIFIED_SOURCE_OF;

	/** prov:qualifiedStartOf – inverse of prov:qualifiedStart */
	public static final IRI QUALIFIED_START_OF;

	/** prov:qualifiedUsingActivity – inverse of prov:qualifiedUsage */
	public static final IRI QUALIFIED_USING_ACTIVITY;

	/** prov:quotedAs – inverse of prov:wasQuotedFrom */
	public static final IRI QUOTED_AS;

	/** prov:revisedEntity – inverse of prov:qualifiedRevision */
	public static final IRI REVISED_ENTITY;

	/** prov:started – inverse of prov:wasStartedBy */
	public static final IRI STARTED;

	/** prov:wasActivityOfInfluence – inverse of prov:hadActivity (activity perspective) */
	public static final IRI WAS_ACTIVITY_OF_INFLUENCE;

	/** prov:wasAssociateFor – inverse of prov:wasAssociatedWith */
	public static final IRI WAS_ASSOCIATE_FOR;

	/** prov:wasMemberOf – inverse of prov:hadMember */
	public static final IRI WAS_MEMBER_OF;

	/** prov:wasPlanOf – inverse of prov:hadPlan */
	public static final IRI WAS_PLAN_OF;

	/** prov:wasPrimarySourceOf – inverse of prov:hadPrimarySource */
	public static final IRI WAS_PRIMARY_SOURCE_OF;

	/** prov:wasRoleIn – inverse of prov:hadRole */
	public static final IRI WAS_ROLE_IN;

	/** prov:wasUsedBy – inverse of prov:used */
	public static final IRI WAS_USED_BY;

	/** prov:wasUsedInDerivation – inverse of prov:hadUsage */
	public static final IRI WAS_USED_IN_DERIVATION;

	static {

		ACCEPT = Vocabularies.createIRI(NAMESPACE, "Accept");
		ACTIVITY = Vocabularies.createIRI(NAMESPACE, "Activity");
		ACTIVITY_INFLUENCE = Vocabularies.createIRI(NAMESPACE, "ActivityInfluence");
		AGENT = Vocabularies.createIRI(NAMESPACE, "Agent");
		AGENT_INFLUENCE = Vocabularies.createIRI(NAMESPACE, "AgentInfluence");
		ASSOCIATION = Vocabularies.createIRI(NAMESPACE, "Association");
		ATTRIBUTION = Vocabularies.createIRI(NAMESPACE, "Attribution");
		BUNDLE = Vocabularies.createIRI(NAMESPACE, "Bundle");
		COLLECTION = Vocabularies.createIRI(NAMESPACE, "Collection");
		COMMUNICATION = Vocabularies.createIRI(NAMESPACE, "Communication");
		CONTRIBUTE = Vocabularies.createIRI(NAMESPACE, "Contribute");
		CONTRIBUTOR = Vocabularies.createIRI(NAMESPACE, "Contributor");
		COPYRIGHT = Vocabularies.createIRI(NAMESPACE, "Copyright");
		CREATE = Vocabularies.createIRI(NAMESPACE, "Create");
		CREATOR = Vocabularies.createIRI(NAMESPACE, "Creator");
		DELEGATION = Vocabularies.createIRI(NAMESPACE, "Delegation");
		DERIVATION = Vocabularies.createIRI(NAMESPACE, "Derivation");
		DICTIONARY = Vocabularies.createIRI(NAMESPACE, "Dictionary");
		DIRECT_QUERY_SERVICE = Vocabularies.createIRI(NAMESPACE, "DirectQueryService");
		EMPTY_COLLECTION = Vocabularies.createIRI(NAMESPACE, "EmptyCollection");
		EMPTY_DICTIONARY = Vocabularies.createIRI(NAMESPACE, "EmptyDictionary");
		END = Vocabularies.createIRI(NAMESPACE, "End");
		ENTITY = Vocabularies.createIRI(NAMESPACE, "Entity");
		ENTITY_INFLUENCE = Vocabularies.createIRI(NAMESPACE, "EntityInfluence");
		GENERATION = Vocabularies.createIRI(NAMESPACE, "Generation");
		INFLUENCE = Vocabularies.createIRI(NAMESPACE, "Influence");
		INSERTION = Vocabularies.createIRI(NAMESPACE, "Insertion");
		INSTANTANEOUS_EVENT = Vocabularies.createIRI(NAMESPACE, "InstantaneousEvent");
		INVALIDATION = Vocabularies.createIRI(NAMESPACE, "Invalidation");
		KEY_ENTITY_PAIR = Vocabularies.createIRI(NAMESPACE, "KeyEntityPair");
		LOCATION = Vocabularies.createIRI(NAMESPACE, "Location");
		MODIFY = Vocabularies.createIRI(NAMESPACE, "Modify");
		ORGANIZATION = Vocabularies.createIRI(NAMESPACE, "Organization");
		PERSON = Vocabularies.createIRI(NAMESPACE, "Person");
		PLAN = Vocabularies.createIRI(NAMESPACE, "Plan");
		PRIMARY_SOURCE = Vocabularies.createIRI(NAMESPACE, "PrimarySource");
		PUBLISH = Vocabularies.createIRI(NAMESPACE, "Publish");
		PUBLISHER = Vocabularies.createIRI(NAMESPACE, "Publisher");
		QUOTATION = Vocabularies.createIRI(NAMESPACE, "Quotation");
		REMOVAL = Vocabularies.createIRI(NAMESPACE, "Removal");
		REPLACE = Vocabularies.createIRI(NAMESPACE, "Replace");
		REVISION = Vocabularies.createIRI(NAMESPACE, "Revision");
		RIGHTS_ASSIGNMENT = Vocabularies.createIRI(NAMESPACE, "RightsAssignment");
		RIGHTS_HOLDER = Vocabularies.createIRI(NAMESPACE, "RightsHolder");
		ROLE = Vocabularies.createIRI(NAMESPACE, "Role");
		SERVICE_DESCRIPTION = Vocabularies.createIRI(NAMESPACE, "ServiceDescription");
		SOFTWARE_AGENT = Vocabularies.createIRI(NAMESPACE, "SoftwareAgent");
		START = Vocabularies.createIRI(NAMESPACE, "Start");
		SUBMIT = Vocabularies.createIRI(NAMESPACE, "Submit");
		USAGE = Vocabularies.createIRI(NAMESPACE, "Usage");

		ACTED_ON_BEHALF_OF = Vocabularies.createIRI(NAMESPACE, "actedOnBehalfOf");
		ACTIVITY_PROP = Vocabularies.createIRI(NAMESPACE, "activity");
		AGENT_PROP = Vocabularies.createIRI(NAMESPACE, "agent");
		ALTERNATE_OF = Vocabularies.createIRI(NAMESPACE, "alternateOf");
		AS_IN_BUNDLE = Vocabularies.createIRI(NAMESPACE, "asInBundle");
		AT_LOCATION = Vocabularies.createIRI(NAMESPACE, "atLocation");
		AT_TIME = Vocabularies.createIRI(NAMESPACE, "atTime");
		DERIVED_BY_INSERTION_FROM = Vocabularies.createIRI(NAMESPACE, "derivedByInsertionFrom");
		DERIVED_BY_REMOVAL_FROM = Vocabularies.createIRI(NAMESPACE, "derivedByRemovalFrom");
		DESCRIBES_SERVICE = Vocabularies.createIRI(NAMESPACE, "describesService");
		DICTIONARY_PROP = Vocabularies.createIRI(NAMESPACE, "dictionary");
		ENDED_AT_TIME = Vocabularies.createIRI(NAMESPACE, "endedAtTime");
		ENTITY_PROP = Vocabularies.createIRI(NAMESPACE, "entity");
		GENERATED = Vocabularies.createIRI(NAMESPACE, "generated");
		GENERATED_AT_TIME = Vocabularies.createIRI(NAMESPACE, "generatedAtTime");
		HAD_ACTIVITY = Vocabularies.createIRI(NAMESPACE, "hadActivity");
		HAD_DICTIONARY_MEMBER = Vocabularies.createIRI(NAMESPACE, "hadDictionaryMember");
		HAD_GENERATION = Vocabularies.createIRI(NAMESPACE, "hadGeneration");
		HAD_MEMBER = Vocabularies.createIRI(NAMESPACE, "hadMember");
		HAD_PLAN = Vocabularies.createIRI(NAMESPACE, "hadPlan");
		HAD_PRIMARY_SOURCE = Vocabularies.createIRI(NAMESPACE, "hadPrimarySource");
		HAD_ROLE = Vocabularies.createIRI(NAMESPACE, "hadRole");
		HAD_USAGE = Vocabularies.createIRI(NAMESPACE, "hadUsage");
		HAS_ANCHOR = Vocabularies.createIRI(NAMESPACE, "has_anchor");
		HAS_PROVENANCE = Vocabularies.createIRI(NAMESPACE, "has_provenance");
		HAS_QUERY_SERVICE = Vocabularies.createIRI(NAMESPACE, "has_query_service");
		INFLUENCED = Vocabularies.createIRI(NAMESPACE, "influenced");
		INFLUENCER = Vocabularies.createIRI(NAMESPACE, "influencer");
		INSERTED_KEY_ENTITY_PAIR = Vocabularies.createIRI(NAMESPACE, "insertedKeyEntityPair");
		INVALIDATED = Vocabularies.createIRI(NAMESPACE, "invalidated");
		INVALIDATED_AT_TIME = Vocabularies.createIRI(NAMESPACE, "invalidatedAtTime");
		MENTION_OF = Vocabularies.createIRI(NAMESPACE, "mentionOf");
		PAIR_ENTITY = Vocabularies.createIRI(NAMESPACE, "pairEntity");
		PAIR_KEY = Vocabularies.createIRI(NAMESPACE, "pairKey");
		PINGBACK = Vocabularies.createIRI(NAMESPACE, "pingback");
		PROVENANCE_URI_TEMPLATE = Vocabularies.createIRI(NAMESPACE, "provenanceUriTemplate");
		QUALIFIED_ASSOCIATION = Vocabularies.createIRI(NAMESPACE, "qualifiedAssociation");
		QUALIFIED_ATTRIBUTION = Vocabularies.createIRI(NAMESPACE, "qualifiedAttribution");
		QUALIFIED_COMMUNICATION = Vocabularies.createIRI(NAMESPACE, "qualifiedCommunication");
		QUALIFIED_DELEGATION = Vocabularies.createIRI(NAMESPACE, "qualifiedDelegation");
		QUALIFIED_DERIVATION = Vocabularies.createIRI(NAMESPACE, "qualifiedDerivation");
		QUALIFIED_END = Vocabularies.createIRI(NAMESPACE, "qualifiedEnd");
		QUALIFIED_GENERATION = Vocabularies.createIRI(NAMESPACE, "qualifiedGeneration");
		QUALIFIED_INFLUENCE = Vocabularies.createIRI(NAMESPACE, "qualifiedInfluence");
		QUALIFIED_INSERTION = Vocabularies.createIRI(NAMESPACE, "qualifiedInsertion");
		QUALIFIED_INVALIDATION = Vocabularies.createIRI(NAMESPACE, "qualifiedInvalidation");
		QUALIFIED_PRIMARY_SOURCE = Vocabularies.createIRI(NAMESPACE, "qualifiedPrimarySource");
		QUALIFIED_QUOTATION = Vocabularies.createIRI(NAMESPACE, "qualifiedQuotation");
		QUALIFIED_REMOVAL = Vocabularies.createIRI(NAMESPACE, "qualifiedRemoval");
		QUALIFIED_REVISION = Vocabularies.createIRI(NAMESPACE, "qualifiedRevision");
		QUALIFIED_START = Vocabularies.createIRI(NAMESPACE, "qualifiedStart");
		QUALIFIED_USAGE = Vocabularies.createIRI(NAMESPACE, "qualifiedUsage");
		REMOVED_KEY = Vocabularies.createIRI(NAMESPACE, "removedKey");
		SPECIALIZATION_OF = Vocabularies.createIRI(NAMESPACE, "specializationOf");
		STARTED_AT_TIME = Vocabularies.createIRI(NAMESPACE, "startedAtTime");
		USED = Vocabularies.createIRI(NAMESPACE, "used");
		VALUE = Vocabularies.createIRI(NAMESPACE, "value");
		WAS_ASSOCIATED_WITH = Vocabularies.createIRI(NAMESPACE, "wasAssociatedWith");
		WAS_ATTRIBUTED_TO = Vocabularies.createIRI(NAMESPACE, "wasAttributedTo");
		WAS_DERIVED_FROM = Vocabularies.createIRI(NAMESPACE, "wasDerivedFrom");
		WAS_ENDED_BY = Vocabularies.createIRI(NAMESPACE, "wasEndedBy");
		WAS_GENERATED_BY = Vocabularies.createIRI(NAMESPACE, "wasGeneratedBy");
		WAS_INFLUENCED_BY = Vocabularies.createIRI(NAMESPACE, "wasInfluencedBy");
		WAS_INFORMED_BY = Vocabularies.createIRI(NAMESPACE, "wasInformedBy");
		WAS_INVALIDATED_BY = Vocabularies.createIRI(NAMESPACE, "wasInvalidatedBy");
		WAS_QUOTED_FROM = Vocabularies.createIRI(NAMESPACE, "wasQuotedFrom");
		WAS_REVISION_OF = Vocabularies.createIRI(NAMESPACE, "wasRevisionOf");
		WAS_STARTED_BY = Vocabularies.createIRI(NAMESPACE, "wasStartedBy");

		ACTIVITY_OF_INFLUENCE = Vocabularies.createIRI(NAMESPACE, "activityOfInfluence");
		AGENT_OF_INFLUENCE = Vocabularies.createIRI(NAMESPACE, "agentOfInfluence");
		CONTRIBUTED = Vocabularies.createIRI(NAMESPACE, "contributed");
		ENDED = Vocabularies.createIRI(NAMESPACE, "ended");
		ENTITY_OF_INFLUENCE = Vocabularies.createIRI(NAMESPACE, "entityOfInfluence");
		GENERALIZATION_OF = Vocabularies.createIRI(NAMESPACE, "generalizationOf");
		HAD_DELEGATE = Vocabularies.createIRI(NAMESPACE, "hadDelegate");
		HAD_DERIVATION = Vocabularies.createIRI(NAMESPACE, "hadDerivation");
		HAD_INFLUENCE = Vocabularies.createIRI(NAMESPACE, "hadInfluence");
		HAD_REVISION = Vocabularies.createIRI(NAMESPACE, "hadRevision");
		INFORMED = Vocabularies.createIRI(NAMESPACE, "informed");
		LOCATION_OF = Vocabularies.createIRI(NAMESPACE, "locationOf");
		QUALIFIED_ASSOCIATION_OF = Vocabularies.createIRI(NAMESPACE, "qualifiedAssociationOf");
		QUALIFIED_ATTRIBUTION_OF = Vocabularies.createIRI(NAMESPACE, "qualifiedAttributionOf");
		QUALIFIED_COMMUNICATION_OF = Vocabularies.createIRI(NAMESPACE, "qualifiedCommunicationOf");
		QUALIFIED_DELEGATION_OF = Vocabularies.createIRI(NAMESPACE, "qualifiedDelegationOf");
		QUALIFIED_DERIVATION_OF = Vocabularies.createIRI(NAMESPACE, "qualifiedDerivationOf");
		QUALIFIED_END_OF = Vocabularies.createIRI(NAMESPACE, "qualifiedEndOf");
		QUALIFIED_GENERATION_OF = Vocabularies.createIRI(NAMESPACE, "qualifiedGenerationOf");
		QUALIFIED_INFLUENCE_OF = Vocabularies.createIRI(NAMESPACE, "qualifiedInfluenceOf");
		QUALIFIED_INVALIDATION_OF = Vocabularies.createIRI(NAMESPACE, "qualifiedInvalidationOf");
		QUALIFIED_QUOTATION_OF = Vocabularies.createIRI(NAMESPACE, "qualifiedQuotationOf");
		QUALIFIED_SOURCE_OF = Vocabularies.createIRI(NAMESPACE, "qualifiedSourceOf");
		QUALIFIED_START_OF = Vocabularies.createIRI(NAMESPACE, "qualifiedStartOf");
		QUALIFIED_USING_ACTIVITY = Vocabularies.createIRI(NAMESPACE, "qualifiedUsingActivity");
		QUOTED_AS = Vocabularies.createIRI(NAMESPACE, "quotedAs");
		REVISED_ENTITY = Vocabularies.createIRI(NAMESPACE, "revisedEntity");
		STARTED = Vocabularies.createIRI(NAMESPACE, "started");
		WAS_ACTIVITY_OF_INFLUENCE = Vocabularies.createIRI(NAMESPACE, "wasActivityOfInfluence");
		WAS_ASSOCIATE_FOR = Vocabularies.createIRI(NAMESPACE, "wasAssociateFor");
		WAS_MEMBER_OF = Vocabularies.createIRI(NAMESPACE, "wasMemberOf");
		WAS_PLAN_OF = Vocabularies.createIRI(NAMESPACE, "wasPlanOf");
		WAS_PRIMARY_SOURCE_OF = Vocabularies.createIRI(NAMESPACE, "wasPrimarySourceOf");
		WAS_ROLE_IN = Vocabularies.createIRI(NAMESPACE, "wasRoleIn");
		WAS_USED_BY = Vocabularies.createIRI(NAMESPACE, "wasUsedBy");
		WAS_USED_IN_DERIVATION = Vocabularies.createIRI(NAMESPACE, "wasUsedInDerivation");
	}
}
