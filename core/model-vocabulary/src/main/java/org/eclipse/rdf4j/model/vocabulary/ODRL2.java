/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.model.vocabulary;

import static org.eclipse.rdf4j.model.base.AbstractIRI.createIRI;
import static org.eclipse.rdf4j.model.base.AbstractNamespace.createNamespace;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;

/**
 * Constants for the Open Digital Rights Language.
 *
 * @see <a href="https://www.w3.org/TR/odrl-vocab/">Open Digital Rights Language</a>
 *
 * @author Bart Hanssens
 */
public class ODRL2 {
	/**
	 * The ODRL 2.2 namespace: http://www.w3.org/ns/odrl/2/
	 */
	public static final String NAMESPACE = "http://www.w3.org/ns/odrl/2/";

	/**
	 * Recommended prefix for the namespace: "odrl"
	 */
	public static final String PREFIX = "odrl";

	/**
	 * An immutable {@link Namespace} constant that represents the namespace.
	 */
	public static final Namespace NS = createNamespace(PREFIX, NAMESPACE);

	// Classes
	/** odrl:Action */
	public static final IRI ACTION;

	/** odrl:Agreement */
	public static final IRI AGREEMENT;

	/** odrl:Assertion */
	public static final IRI ASSERTION;

	/** odrl:Asset */
	public static final IRI ASSET;

	/** odrl:AssetCollection */
	public static final IRI ASSET_COLLECTION;

	/** odrl:AssetScope */
	@Deprecated
	public static final IRI ASSET_SCOPE;

	/** odrl:ConflictTerm */
	public static final IRI CONFLICT_TERM;

	/** odrl:Constraint */
	public static final IRI CONSTRAINT;

	/** odrl:Duty */
	public static final IRI DUTY;

	/** odrl:LeftOperand */
	public static final IRI LEFT_OPERAND;

	/** odrl:LogicalConstraint */
	public static final IRI LOGICAL_CONSTRAINT;

	/** odrl:Offer */
	public static final IRI OFFER;

	/** odrl:Operator */
	public static final IRI OPERATOR;

	/** odrl:Party */
	public static final IRI PARTY;

	/** odrl:PartyCollection */
	public static final IRI PARTY_COLLECTION;

	/** odrl:PartyScope */
	@Deprecated
	public static final IRI PARTY_SCOPE;

	/** odrl:Permission */
	public static final IRI PERMISSION;

	/** odrl:Policy */
	public static final IRI POLICY;

	/** odrl:Privacy */
	public static final IRI PRIVACY;

	/** odrl:Prohibition */
	public static final IRI PROHIBITION;

	/** odrl:Request */
	public static final IRI REQUEST;

	/** odrl:RightOperand */
	public static final IRI RIGHT_OPERAND;

	/** odrl:Rule */
	public static final IRI RULE;

	/** odrl:Set */
	public static final IRI SET;

	/** odrl:Ticket */
	public static final IRI TICKET;

	/** odrl:UndefinedTerm */
	@Deprecated
	public static final IRI UNDEFINED_TERM;

	// Properties
	/** odrl:action */
	public static final IRI ACTION_PROP;

	/** odrl:and */
	public static final IRI AND;

	/** odrl:andSequence */
	public static final IRI AND_SEQUENCE;

	/** odrl:assignee */
	public static final IRI ASSIGNEE;

	/** odrl:assigneeOf */
	public static final IRI ASSIGNEE_OF;

	/** odrl:assigner */
	public static final IRI ASSIGNER;

	/** odrl:assignerOf */
	public static final IRI ASSIGNER_OF;

	/** odrl:attributedParty */
	public static final IRI ATTRIBUTED_PARTY;

	/** odrl:attributingParty */
	public static final IRI ATTRIBUTING_PARTY;

	/** odrl:compensatedParty */
	public static final IRI COMPENSATED_PARTY;

	/** odrl:compensatingParty */
	public static final IRI COMPENSATING_PARTY;

	/** odrl:conflict */
	public static final IRI CONFLICT;

	/** odrl:consentedParty */
	public static final IRI CONSENTED_PARTY;

	/** odrl:consentingParty */
	public static final IRI CONSENTING_PARTY;

	/** odrl:consequence */
	public static final IRI CONSEQUENCE;

	/** odrl:constraint */
	public static final IRI CONSTRAINT_PROP;

	/** odrl:contractedParty */
	public static final IRI CONTRACTED_PARTY;

	/** odrl:contractingParty */
	public static final IRI CONTRACTING_PARTY;

	/** odrl:dataType */
	public static final IRI DATA_TYPE;

	/** odrl:duty */
	public static final IRI DUTY_PROP;

	/** odrl:failure */
	public static final IRI FAILURE;

	/** odrl:function */
	public static final IRI FUNCTION;

	/** odrl:hasPolicy */
	public static final IRI HAS_POLICY;

	/** odrl:implies */
	public static final IRI IMPLIES;

	/** odrl:includedIn */
	public static final IRI INCLUDED_IN;

	/** odrl:informedParty */
	public static final IRI INFORMED_PARTY;

	/** odrl:informingParty */
	public static final IRI INFORMING_PARTY;

	/** odrl:inheritAllowed */
	@Deprecated
	public static final IRI INHERIT_ALLOWED;

	/** odrl:inheritFrom */
	public static final IRI INHERIT_FROM;

	/** odrl:inheritRelation */
	@Deprecated
	public static final IRI INHERIT_RELATION;

	/** odrl:leftOperand */
	public static final IRI LEFT_OPERAND_PROP;

	/** odrl:obligation */
	public static final IRI OBLIGATION;

	/** odrl:operand */
	public static final IRI OPERAND;

	/** odrl:operator */
	public static final IRI OPERATOR_PROP;

	/** odrl:or */
	public static final IRI OR;

	/** odrl:output */
	public static final IRI OUTPUT;

	/** odrl:partOf */
	public static final IRI PART_OF;

	/** odrl:payeeParty */
	@Deprecated
	public static final IRI PAYEE_PARTY;

	/** odrl:permission */
	public static final IRI PERMISSION_PROP;

	/** odrl:profile */
	public static final IRI PROFILE;

	/** odrl:prohibition */
	public static final IRI PROHIBITION_PROP;

	/** odrl:proximity */
	@Deprecated
	public static final IRI PROXIMITY;

	/** odrl:refinement */
	public static final IRI REFINEMENT;

	/** odrl:relation */
	public static final IRI RELATION;

	/** odrl:remedy */
	public static final IRI REMEDY;

	/** odrl:rightOperand */
	public static final IRI RIGHT_OPERAND_PROP;

	/** odrl:rightOperandReference */
	public static final IRI RIGHT_OPERAND_REFERENCE;

	/** odrl:scope */
	@Deprecated
	public static final IRI SCOPE;

	/** odrl:source */
	public static final IRI SOURCE;

	/** odrl:status */
	public static final IRI STATUS;

	/** odrl:target */
	public static final IRI TARGET;

	/** odrl:timedCount */
	@Deprecated
	public static final IRI TIMED_COUNT;

	/** odrl:trackedParty */
	public static final IRI TRACKED_PARTY;

	/** odrl:trackingParty */
	public static final IRI TRACKING_PARTY;

	/** odrl:uid */
	public static final IRI UID;

	/** odrl:undefined */
	@Deprecated
	public static final IRI UNDEFINED;

	/** odrl:unit */
	public static final IRI UNIT;

	/** odrl:xone */
	public static final IRI XONE;

	// Individuals
	/** odrl:All */
	@Deprecated
	public static final IRI ALL;

	/** odrl:All2ndConnections */
	@Deprecated
	public static final IRI ALL2ND_CONNECTIONS;

	/** odrl:AllConnections */
	@Deprecated
	public static final IRI ALL_CONNECTIONS;

	/** odrl:AllGroups */
	@Deprecated
	public static final IRI ALL_GROUPS;

	/** odrl:Group */
	@Deprecated
	public static final IRI GROUP;

	/** odrl:Individual */
	@Deprecated
	public static final IRI INDIVIDUAL;

	/** odrl:absolutePosition */
	public static final IRI ABSOLUTE_POSITION;

	/** odrl:absoluteSize */
	public static final IRI ABSOLUTE_SIZE;

	/** odrl:absoluteSpatialPosition */
	public static final IRI ABSOLUTE_SPATIAL_POSITION;

	/** odrl:absoluteTemporalPosition */
	public static final IRI ABSOLUTE_TEMPORAL_POSITION;

	/** odrl:acceptTracking */
	public static final IRI ACCEPT_TRACKING;

	/** odrl:adHocShare */
	@Deprecated
	public static final IRI AD_HOC_SHARE;

	/** odrl:aggregate */
	public static final IRI AGGREGATE;

	/** odrl:annotate */
	public static final IRI ANNOTATE;

	/** odrl:anonymize */
	public static final IRI ANONYMIZE;

	/** odrl:append */
	@Deprecated
	public static final IRI APPEND;

	/** odrl:appendTo */
	@Deprecated
	public static final IRI APPEND_TO;

	/** odrl:archive */
	public static final IRI ARCHIVE;

	/** odrl:attachPolicy */
	@Deprecated
	public static final IRI ATTACH_POLICY;

	/** odrl:attachSource */
	@Deprecated
	public static final IRI ATTACH_SOURCE;

	/** odrl:attribute */
	public static final IRI ATTRIBUTE;

	/** odrl:commercialize */
	@Deprecated
	public static final IRI COMMERCIALIZE;

	/** odrl:compensate */
	public static final IRI COMPENSATE;

	/** odrl:concurrentUse */
	public static final IRI CONCURRENT_USE;

	/** odrl:copy */
	@Deprecated
	public static final IRI COPY;

	/** odrl:count */
	public static final IRI COUNT;

	/** odrl:dateTime */
	public static final IRI DATE_TIME;

	/** odrl:delayPeriod */
	public static final IRI DELAY_PERIOD;

	/** odrl:delete */
	public static final IRI DELETE;

	/** odrl:deliveryChannel */
	public static final IRI DELIVERY_CHANNEL;

	/** odrl:derive */
	public static final IRI DERIVE;

	/** odrl:device */
	@Deprecated
	public static final IRI DEVICE;

	/** odrl:digitize */
	public static final IRI DIGITIZE;

	/** odrl:display */
	public static final IRI DISPLAY;

	/** odrl:distribute */
	public static final IRI DISTRIBUTE;

	/** odrl:elapsedTime */
	public static final IRI ELAPSED_TIME;

	/** odrl:ensureExclusivity */
	public static final IRI ENSURE_EXCLUSIVITY;

	/** odrl:eq */
	public static final IRI EQ;

	/** odrl:event */
	public static final IRI EVENT;

	/** odrl:execute */
	public static final IRI EXECUTE;

	/** odrl:export */
	@Deprecated
	public static final IRI EXPORT;

	/** odrl:extract */
	public static final IRI EXTRACT;

	/** odrl:extractChar */
	@Deprecated
	public static final IRI EXTRACT_CHAR;

	/** odrl:extractPage */
	@Deprecated
	public static final IRI EXTRACT_PAGE;

	/** odrl:extractWord */
	@Deprecated
	public static final IRI EXTRACT_WORD;

	/** odrl:fileFormat */
	public static final IRI FILE_FORMAT;

	/** odrl:give */
	public static final IRI GIVE;

	/** odrl:grantUse */
	public static final IRI GRANT_USE;

	/** odrl:gt */
	public static final IRI GT;

	/** odrl:gteq */
	public static final IRI GTEQ;

	/** odrl:hasPart */
	public static final IRI HAS_PART;

	/** odrl:ignore */
	@Deprecated
	public static final IRI IGNORE;

	/** odrl:include */
	public static final IRI INCLUDE;

	/** odrl:index */
	public static final IRI INDEX;

	/** odrl:industry */
	public static final IRI INDUSTRY;

	/** odrl:inform */
	public static final IRI INFORM;

	/** odrl:install */
	public static final IRI INSTALL;

	/** odrl:invalid */
	public static final IRI INVALID;

	/** odrl:isA */
	public static final IRI IS_A;

	/** odrl:isAllOf */
	public static final IRI IS_ALL_OF;

	/** odrl:isAnyOf */
	public static final IRI IS_ANY_OF;

	/** odrl:isNoneOf */
	public static final IRI IS_NONE_OF;

	/** odrl:isPartOf */
	public static final IRI IS_PART_OF;

	/** odrl:language */
	public static final IRI LANGUAGE;

	/** odrl:lease */
	@Deprecated
	public static final IRI LEASE;

	/** odrl:lend */
	@Deprecated
	public static final IRI LEND;

	/** odrl:license */
	@Deprecated
	public static final IRI LICENSE;

	/** odrl:lt */
	public static final IRI LT;

	/** odrl:lteq */
	public static final IRI LTEQ;

	/** odrl:media */
	public static final IRI MEDIA;

	/** odrl:meteredTime */
	public static final IRI METERED_TIME;

	/** odrl:modify */
	public static final IRI MODIFY;

	/** odrl:move */
	public static final IRI MOVE;

	/** odrl:neq */
	public static final IRI NEQ;

	/** odrl:nextPolicy */
	public static final IRI NEXT_POLICY;

	/** odrl:obtainConsent */
	public static final IRI OBTAIN_CONSENT;

	/** odrl:pay */
	@Deprecated
	public static final IRI PAY;

	/** odrl:payAmount */
	public static final IRI PAY_AMOUNT;

	/** odrl:percentage */
	public static final IRI PERCENTAGE;

	/** odrl:perm */
	public static final IRI PERM;

	/** odrl:play */
	public static final IRI PLAY;

	/** odrl:policyUsage */
	public static final IRI POLICY_USAGE;

	/** odrl:present */
	public static final IRI PRESENT;

	/** odrl:preview */
	@Deprecated
	public static final IRI PREVIEW;

	/** odrl:print */
	public static final IRI PRINT;

	/** odrl:product */
	public static final IRI PRODUCT;

	/** odrl:prohibit */
	public static final IRI PROHIBIT;

	/** odrl:purpose */
	public static final IRI PURPOSE;

	/** odrl:read */
	public static final IRI READ;

	/** odrl:recipient */
	public static final IRI RECIPIENT;

	/** odrl:relativePosition */
	public static final IRI RELATIVE_POSITION;

	/** odrl:relativeSize */
	public static final IRI RELATIVE_SIZE;

	/** odrl:relativeSpatialPosition */
	public static final IRI RELATIVE_SPATIAL_POSITION;

	/** odrl:relativeTemporalPosition */
	public static final IRI RELATIVE_TEMPORAL_POSITION;

	/** odrl:reproduce */
	public static final IRI REPRODUCE;

	/** odrl:resolution */
	public static final IRI RESOLUTION;

	/** odrl:reviewPolicy */
	public static final IRI REVIEW_POLICY;

	/** odrl:secondaryUse */
	@Deprecated
	public static final IRI SECONDARY_USE;

	/** odrl:sell */
	public static final IRI SELL;

	/** odrl:share */
	@Deprecated
	public static final IRI SHARE;

	/** odrl:shareAlike */
	@Deprecated
	public static final IRI SHARE_ALIKE;

	/** odrl:spatial */
	public static final IRI SPATIAL;

	/** odrl:spatialCoordinates */
	public static final IRI SPATIAL_COORDINATES;

	/** odrl:stream */
	public static final IRI STREAM;

	/** odrl:support */
	@Deprecated
	public static final IRI SUPPORT;

	/** odrl:synchronize */
	public static final IRI SYNCHRONIZE;

	/** odrl:system */
	@Deprecated
	public static final IRI SYSTEM;

	/** odrl:systemDevice */
	public static final IRI SYSTEM_DEVICE;

	/** odrl:textToSpeech */
	public static final IRI TEXT_TO_SPEECH;

	/** odrl:timeInterval */
	public static final IRI TIME_INTERVAL;

	/** odrl:transfer */
	public static final IRI TRANSFER;

	/** odrl:transform */
	public static final IRI TRANSFORM;

	/** odrl:translate */
	public static final IRI TRANSLATE;

	/** odrl:uninstall */
	public static final IRI UNINSTALL;

	/** odrl:unitOfCount */
	public static final IRI UNIT_OF_COUNT;

	/** odrl:use */
	public static final IRI USE;

	/** odrl:version */
	public static final IRI VERSION;

	/** odrl:virtualLocation */
	public static final IRI VIRTUAL_LOCATION;

	/** odrl:watermark */
	public static final IRI WATERMARK;

	/** odrl:write */
	@Deprecated
	public static final IRI WRITE;

	/** odrl:writeTo */
	@Deprecated
	public static final IRI WRITE_TO;

	static {

		ACTION = createIRI(NAMESPACE, "Action");
		AGREEMENT = createIRI(NAMESPACE, "Agreement");
		ASSERTION = createIRI(NAMESPACE, "Assertion");
		ASSET = createIRI(NAMESPACE, "Asset");
		ASSET_COLLECTION = createIRI(NAMESPACE, "AssetCollection");
		ASSET_SCOPE = createIRI(NAMESPACE, "AssetScope");
		CONFLICT_TERM = createIRI(NAMESPACE, "ConflictTerm");
		CONSTRAINT = createIRI(NAMESPACE, "Constraint");
		DUTY = createIRI(NAMESPACE, "Duty");
		LEFT_OPERAND = createIRI(NAMESPACE, "LeftOperand");
		LOGICAL_CONSTRAINT = createIRI(NAMESPACE, "LogicalConstraint");
		OFFER = createIRI(NAMESPACE, "Offer");
		OPERATOR = createIRI(NAMESPACE, "Operator");
		PARTY = createIRI(NAMESPACE, "Party");
		PARTY_COLLECTION = createIRI(NAMESPACE, "PartyCollection");
		PARTY_SCOPE = createIRI(NAMESPACE, "PartyScope");
		PERMISSION = createIRI(NAMESPACE, "Permission");
		POLICY = createIRI(NAMESPACE, "Policy");
		PRIVACY = createIRI(NAMESPACE, "Privacy");
		PROHIBITION = createIRI(NAMESPACE, "Prohibition");
		REQUEST = createIRI(NAMESPACE, "Request");
		RIGHT_OPERAND = createIRI(NAMESPACE, "RightOperand");
		RULE = createIRI(NAMESPACE, "Rule");
		SET = createIRI(NAMESPACE, "Set");
		TICKET = createIRI(NAMESPACE, "Ticket");
		UNDEFINED_TERM = createIRI(NAMESPACE, "UndefinedTerm");

		ACTION_PROP = createIRI(NAMESPACE, "action");
		AND = createIRI(NAMESPACE, "and");
		AND_SEQUENCE = createIRI(NAMESPACE, "andSequence");
		ASSIGNEE = createIRI(NAMESPACE, "assignee");
		ASSIGNEE_OF = createIRI(NAMESPACE, "assigneeOf");
		ASSIGNER = createIRI(NAMESPACE, "assigner");
		ASSIGNER_OF = createIRI(NAMESPACE, "assignerOf");
		ATTRIBUTED_PARTY = createIRI(NAMESPACE, "attributedParty");
		ATTRIBUTING_PARTY = createIRI(NAMESPACE, "attributingParty");
		COMPENSATED_PARTY = createIRI(NAMESPACE, "compensatedParty");
		COMPENSATING_PARTY = createIRI(NAMESPACE, "compensatingParty");
		CONFLICT = createIRI(NAMESPACE, "conflict");
		CONSENTED_PARTY = createIRI(NAMESPACE, "consentedParty");
		CONSENTING_PARTY = createIRI(NAMESPACE, "consentingParty");
		CONSEQUENCE = createIRI(NAMESPACE, "consequence");
		CONSTRAINT_PROP = createIRI(NAMESPACE, "constraint");
		CONTRACTED_PARTY = createIRI(NAMESPACE, "contractedParty");
		CONTRACTING_PARTY = createIRI(NAMESPACE, "contractingParty");
		DATA_TYPE = createIRI(NAMESPACE, "dataType");
		DUTY_PROP = createIRI(NAMESPACE, "duty");
		FAILURE = createIRI(NAMESPACE, "failure");
		FUNCTION = createIRI(NAMESPACE, "function");
		HAS_POLICY = createIRI(NAMESPACE, "hasPolicy");
		IMPLIES = createIRI(NAMESPACE, "implies");
		INCLUDED_IN = createIRI(NAMESPACE, "includedIn");
		INFORMED_PARTY = createIRI(NAMESPACE, "informedParty");
		INFORMING_PARTY = createIRI(NAMESPACE, "informingParty");
		INHERIT_ALLOWED = createIRI(NAMESPACE, "inheritAllowed");
		INHERIT_FROM = createIRI(NAMESPACE, "inheritFrom");
		INHERIT_RELATION = createIRI(NAMESPACE, "inheritRelation");
		LEFT_OPERAND_PROP = createIRI(NAMESPACE, "leftOperand");
		OBLIGATION = createIRI(NAMESPACE, "obligation");
		OPERAND = createIRI(NAMESPACE, "operand");
		OPERATOR_PROP = createIRI(NAMESPACE, "operator");
		OR = createIRI(NAMESPACE, "or");
		OUTPUT = createIRI(NAMESPACE, "output");
		PART_OF = createIRI(NAMESPACE, "partOf");
		PAYEE_PARTY = createIRI(NAMESPACE, "payeeParty");
		PERMISSION_PROP = createIRI(NAMESPACE, "permission");
		PROFILE = createIRI(NAMESPACE, "profile");
		PROHIBITION_PROP = createIRI(NAMESPACE, "prohibition");
		PROXIMITY = createIRI(NAMESPACE, "proximity");
		REFINEMENT = createIRI(NAMESPACE, "refinement");
		RELATION = createIRI(NAMESPACE, "relation");
		REMEDY = createIRI(NAMESPACE, "remedy");
		RIGHT_OPERAND_PROP = createIRI(NAMESPACE, "rightOperand");
		RIGHT_OPERAND_REFERENCE = createIRI(NAMESPACE, "rightOperandReference");
		SCOPE = createIRI(NAMESPACE, "scope");
		SOURCE = createIRI(NAMESPACE, "source");
		STATUS = createIRI(NAMESPACE, "status");
		TARGET = createIRI(NAMESPACE, "target");
		TIMED_COUNT = createIRI(NAMESPACE, "timedCount");
		TRACKED_PARTY = createIRI(NAMESPACE, "trackedParty");
		TRACKING_PARTY = createIRI(NAMESPACE, "trackingParty");
		UID = createIRI(NAMESPACE, "uid");
		UNDEFINED = createIRI(NAMESPACE, "undefined");
		UNIT = createIRI(NAMESPACE, "unit");
		XONE = createIRI(NAMESPACE, "xone");

		ALL = createIRI(NAMESPACE, "All");
		ALL2ND_CONNECTIONS = createIRI(NAMESPACE, "All2ndConnections");
		ALL_CONNECTIONS = createIRI(NAMESPACE, "AllConnections");
		ALL_GROUPS = createIRI(NAMESPACE, "AllGroups");
		GROUP = createIRI(NAMESPACE, "Group");
		INDIVIDUAL = createIRI(NAMESPACE, "Individual");
		ABSOLUTE_POSITION = createIRI(NAMESPACE, "absolutePosition");
		ABSOLUTE_SIZE = createIRI(NAMESPACE, "absoluteSize");
		ABSOLUTE_SPATIAL_POSITION = createIRI(NAMESPACE, "absoluteSpatialPosition");
		ABSOLUTE_TEMPORAL_POSITION = createIRI(NAMESPACE, "absoluteTemporalPosition");
		ACCEPT_TRACKING = createIRI(NAMESPACE, "acceptTracking");
		AD_HOC_SHARE = createIRI(NAMESPACE, "adHocShare");
		AGGREGATE = createIRI(NAMESPACE, "aggregate");
		ANNOTATE = createIRI(NAMESPACE, "annotate");
		ANONYMIZE = createIRI(NAMESPACE, "anonymize");
		APPEND = createIRI(NAMESPACE, "append");
		APPEND_TO = createIRI(NAMESPACE, "appendTo");
		ARCHIVE = createIRI(NAMESPACE, "archive");
		ATTACH_POLICY = createIRI(NAMESPACE, "attachPolicy");
		ATTACH_SOURCE = createIRI(NAMESPACE, "attachSource");
		ATTRIBUTE = createIRI(NAMESPACE, "attribute");
		COMMERCIALIZE = createIRI(NAMESPACE, "commercialize");
		COMPENSATE = createIRI(NAMESPACE, "compensate");
		CONCURRENT_USE = createIRI(NAMESPACE, "concurrentUse");
		COPY = createIRI(NAMESPACE, "copy");
		COUNT = createIRI(NAMESPACE, "count");
		DATE_TIME = createIRI(NAMESPACE, "dateTime");
		DELAY_PERIOD = createIRI(NAMESPACE, "delayPeriod");
		DELETE = createIRI(NAMESPACE, "delete");
		DELIVERY_CHANNEL = createIRI(NAMESPACE, "deliveryChannel");
		DERIVE = createIRI(NAMESPACE, "derive");
		DEVICE = createIRI(NAMESPACE, "device");
		DIGITIZE = createIRI(NAMESPACE, "digitize");
		DISPLAY = createIRI(NAMESPACE, "display");
		DISTRIBUTE = createIRI(NAMESPACE, "distribute");
		ELAPSED_TIME = createIRI(NAMESPACE, "elapsedTime");
		ENSURE_EXCLUSIVITY = createIRI(NAMESPACE, "ensureExclusivity");
		EQ = createIRI(NAMESPACE, "eq");
		EVENT = createIRI(NAMESPACE, "event");
		EXECUTE = createIRI(NAMESPACE, "execute");
		EXPORT = createIRI(NAMESPACE, "export");
		EXTRACT = createIRI(NAMESPACE, "extract");
		EXTRACT_CHAR = createIRI(NAMESPACE, "extractChar");
		EXTRACT_PAGE = createIRI(NAMESPACE, "extractPage");
		EXTRACT_WORD = createIRI(NAMESPACE, "extractWord");
		FILE_FORMAT = createIRI(NAMESPACE, "fileFormat");
		GIVE = createIRI(NAMESPACE, "give");
		GRANT_USE = createIRI(NAMESPACE, "grantUse");
		GT = createIRI(NAMESPACE, "gt");
		GTEQ = createIRI(NAMESPACE, "gteq");
		HAS_PART = createIRI(NAMESPACE, "hasPart");
		IGNORE = createIRI(NAMESPACE, "ignore");
		INCLUDE = createIRI(NAMESPACE, "include");
		INDEX = createIRI(NAMESPACE, "index");
		INDUSTRY = createIRI(NAMESPACE, "industry");
		INFORM = createIRI(NAMESPACE, "inform");
		INSTALL = createIRI(NAMESPACE, "install");
		INVALID = createIRI(NAMESPACE, "invalid");
		IS_A = createIRI(NAMESPACE, "isA");
		IS_ALL_OF = createIRI(NAMESPACE, "isAllOf");
		IS_ANY_OF = createIRI(NAMESPACE, "isAnyOf");
		IS_NONE_OF = createIRI(NAMESPACE, "isNoneOf");
		IS_PART_OF = createIRI(NAMESPACE, "isPartOf");
		LANGUAGE = createIRI(NAMESPACE, "language");
		LEASE = createIRI(NAMESPACE, "lease");
		LEND = createIRI(NAMESPACE, "lend");
		LICENSE = createIRI(NAMESPACE, "license");
		LT = createIRI(NAMESPACE, "lt");
		LTEQ = createIRI(NAMESPACE, "lteq");
		MEDIA = createIRI(NAMESPACE, "media");
		METERED_TIME = createIRI(NAMESPACE, "meteredTime");
		MODIFY = createIRI(NAMESPACE, "modify");
		MOVE = createIRI(NAMESPACE, "move");
		NEQ = createIRI(NAMESPACE, "neq");
		NEXT_POLICY = createIRI(NAMESPACE, "nextPolicy");
		OBTAIN_CONSENT = createIRI(NAMESPACE, "obtainConsent");
		PAY = createIRI(NAMESPACE, "pay");
		PAY_AMOUNT = createIRI(NAMESPACE, "payAmount");
		PERCENTAGE = createIRI(NAMESPACE, "percentage");
		PERM = createIRI(NAMESPACE, "perm");
		PLAY = createIRI(NAMESPACE, "play");
		POLICY_USAGE = createIRI(NAMESPACE, "policyUsage");
		PRESENT = createIRI(NAMESPACE, "present");
		PREVIEW = createIRI(NAMESPACE, "preview");
		PRINT = createIRI(NAMESPACE, "print");
		PRODUCT = createIRI(NAMESPACE, "product");
		PROHIBIT = createIRI(NAMESPACE, "prohibit");
		PURPOSE = createIRI(NAMESPACE, "purpose");
		READ = createIRI(NAMESPACE, "read");
		RECIPIENT = createIRI(NAMESPACE, "recipient");
		RELATIVE_POSITION = createIRI(NAMESPACE, "relativePosition");
		RELATIVE_SIZE = createIRI(NAMESPACE, "relativeSize");
		RELATIVE_SPATIAL_POSITION = createIRI(NAMESPACE, "relativeSpatialPosition");
		RELATIVE_TEMPORAL_POSITION = createIRI(NAMESPACE, "relativeTemporalPosition");
		REPRODUCE = createIRI(NAMESPACE, "reproduce");
		RESOLUTION = createIRI(NAMESPACE, "resolution");
		REVIEW_POLICY = createIRI(NAMESPACE, "reviewPolicy");
		SECONDARY_USE = createIRI(NAMESPACE, "secondaryUse");
		SELL = createIRI(NAMESPACE, "sell");
		SHARE = createIRI(NAMESPACE, "share");
		SHARE_ALIKE = createIRI(NAMESPACE, "shareAlike");
		SPATIAL = createIRI(NAMESPACE, "spatial");
		SPATIAL_COORDINATES = createIRI(NAMESPACE, "spatialCoordinates");
		STREAM = createIRI(NAMESPACE, "stream");
		SUPPORT = createIRI(NAMESPACE, "support");
		SYNCHRONIZE = createIRI(NAMESPACE, "synchronize");
		SYSTEM = createIRI(NAMESPACE, "system");
		SYSTEM_DEVICE = createIRI(NAMESPACE, "systemDevice");
		TEXT_TO_SPEECH = createIRI(NAMESPACE, "textToSpeech");
		TIME_INTERVAL = createIRI(NAMESPACE, "timeInterval");
		TRANSFER = createIRI(NAMESPACE, "transfer");
		TRANSFORM = createIRI(NAMESPACE, "transform");
		TRANSLATE = createIRI(NAMESPACE, "translate");
		UNINSTALL = createIRI(NAMESPACE, "uninstall");
		UNIT_OF_COUNT = createIRI(NAMESPACE, "unitOfCount");
		USE = createIRI(NAMESPACE, "use");
		VERSION = createIRI(NAMESPACE, "version");
		VIRTUAL_LOCATION = createIRI(NAMESPACE, "virtualLocation");
		WATERMARK = createIRI(NAMESPACE, "watermark");
		WRITE = createIRI(NAMESPACE, "write");
		WRITE_TO = createIRI(NAMESPACE, "writeTo");
	}
}
