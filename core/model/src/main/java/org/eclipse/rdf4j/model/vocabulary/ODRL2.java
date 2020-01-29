/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.model.vocabulary;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

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
	public static final Namespace NS = new SimpleNamespace(PREFIX, NAMESPACE);

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
		ValueFactory factory = SimpleValueFactory.getInstance();

		ACTION = factory.createIRI(NAMESPACE, "Action");
		AGREEMENT = factory.createIRI(NAMESPACE, "Agreement");
		ASSERTION = factory.createIRI(NAMESPACE, "Assertion");
		ASSET = factory.createIRI(NAMESPACE, "Asset");
		ASSET_COLLECTION = factory.createIRI(NAMESPACE, "AssetCollection");
		ASSET_SCOPE = factory.createIRI(NAMESPACE, "AssetScope");
		CONFLICT_TERM = factory.createIRI(NAMESPACE, "ConflictTerm");
		CONSTRAINT = factory.createIRI(NAMESPACE, "Constraint");
		DUTY = factory.createIRI(NAMESPACE, "Duty");
		LEFT_OPERAND = factory.createIRI(NAMESPACE, "LeftOperand");
		LOGICAL_CONSTRAINT = factory.createIRI(NAMESPACE, "LogicalConstraint");
		OFFER = factory.createIRI(NAMESPACE, "Offer");
		OPERATOR = factory.createIRI(NAMESPACE, "Operator");
		PARTY = factory.createIRI(NAMESPACE, "Party");
		PARTY_COLLECTION = factory.createIRI(NAMESPACE, "PartyCollection");
		PARTY_SCOPE = factory.createIRI(NAMESPACE, "PartyScope");
		PERMISSION = factory.createIRI(NAMESPACE, "Permission");
		POLICY = factory.createIRI(NAMESPACE, "Policy");
		PRIVACY = factory.createIRI(NAMESPACE, "Privacy");
		PROHIBITION = factory.createIRI(NAMESPACE, "Prohibition");
		REQUEST = factory.createIRI(NAMESPACE, "Request");
		RIGHT_OPERAND = factory.createIRI(NAMESPACE, "RightOperand");
		RULE = factory.createIRI(NAMESPACE, "Rule");
		SET = factory.createIRI(NAMESPACE, "Set");
		TICKET = factory.createIRI(NAMESPACE, "Ticket");
		UNDEFINED_TERM = factory.createIRI(NAMESPACE, "UndefinedTerm");

		ACTION_PROP = factory.createIRI(NAMESPACE, "action");
		AND = factory.createIRI(NAMESPACE, "and");
		AND_SEQUENCE = factory.createIRI(NAMESPACE, "andSequence");
		ASSIGNEE = factory.createIRI(NAMESPACE, "assignee");
		ASSIGNEE_OF = factory.createIRI(NAMESPACE, "assigneeOf");
		ASSIGNER = factory.createIRI(NAMESPACE, "assigner");
		ASSIGNER_OF = factory.createIRI(NAMESPACE, "assignerOf");
		ATTRIBUTED_PARTY = factory.createIRI(NAMESPACE, "attributedParty");
		ATTRIBUTING_PARTY = factory.createIRI(NAMESPACE, "attributingParty");
		COMPENSATED_PARTY = factory.createIRI(NAMESPACE, "compensatedParty");
		COMPENSATING_PARTY = factory.createIRI(NAMESPACE, "compensatingParty");
		CONFLICT = factory.createIRI(NAMESPACE, "conflict");
		CONSENTED_PARTY = factory.createIRI(NAMESPACE, "consentedParty");
		CONSENTING_PARTY = factory.createIRI(NAMESPACE, "consentingParty");
		CONSEQUENCE = factory.createIRI(NAMESPACE, "consequence");
		CONSTRAINT_PROP = factory.createIRI(NAMESPACE, "constraint");
		CONTRACTED_PARTY = factory.createIRI(NAMESPACE, "contractedParty");
		CONTRACTING_PARTY = factory.createIRI(NAMESPACE, "contractingParty");
		DATA_TYPE = factory.createIRI(NAMESPACE, "dataType");
		DUTY_PROP = factory.createIRI(NAMESPACE, "duty");
		FAILURE = factory.createIRI(NAMESPACE, "failure");
		FUNCTION = factory.createIRI(NAMESPACE, "function");
		HAS_POLICY = factory.createIRI(NAMESPACE, "hasPolicy");
		IMPLIES = factory.createIRI(NAMESPACE, "implies");
		INCLUDED_IN = factory.createIRI(NAMESPACE, "includedIn");
		INFORMED_PARTY = factory.createIRI(NAMESPACE, "informedParty");
		INFORMING_PARTY = factory.createIRI(NAMESPACE, "informingParty");
		INHERIT_ALLOWED = factory.createIRI(NAMESPACE, "inheritAllowed");
		INHERIT_FROM = factory.createIRI(NAMESPACE, "inheritFrom");
		INHERIT_RELATION = factory.createIRI(NAMESPACE, "inheritRelation");
		LEFT_OPERAND_PROP = factory.createIRI(NAMESPACE, "leftOperand");
		OBLIGATION = factory.createIRI(NAMESPACE, "obligation");
		OPERAND = factory.createIRI(NAMESPACE, "operand");
		OPERATOR_PROP = factory.createIRI(NAMESPACE, "operator");
		OR = factory.createIRI(NAMESPACE, "or");
		OUTPUT = factory.createIRI(NAMESPACE, "output");
		PART_OF = factory.createIRI(NAMESPACE, "partOf");
		PAYEE_PARTY = factory.createIRI(NAMESPACE, "payeeParty");
		PERMISSION_PROP = factory.createIRI(NAMESPACE, "permission");
		PROFILE = factory.createIRI(NAMESPACE, "profile");
		PROHIBITION_PROP = factory.createIRI(NAMESPACE, "prohibition");
		PROXIMITY = factory.createIRI(NAMESPACE, "proximity");
		REFINEMENT = factory.createIRI(NAMESPACE, "refinement");
		RELATION = factory.createIRI(NAMESPACE, "relation");
		REMEDY = factory.createIRI(NAMESPACE, "remedy");
		RIGHT_OPERAND_PROP = factory.createIRI(NAMESPACE, "rightOperand");
		RIGHT_OPERAND_REFERENCE = factory.createIRI(NAMESPACE, "rightOperandReference");
		SCOPE = factory.createIRI(NAMESPACE, "scope");
		SOURCE = factory.createIRI(NAMESPACE, "source");
		STATUS = factory.createIRI(NAMESPACE, "status");
		TARGET = factory.createIRI(NAMESPACE, "target");
		TIMED_COUNT = factory.createIRI(NAMESPACE, "timedCount");
		TRACKED_PARTY = factory.createIRI(NAMESPACE, "trackedParty");
		TRACKING_PARTY = factory.createIRI(NAMESPACE, "trackingParty");
		UID = factory.createIRI(NAMESPACE, "uid");
		UNDEFINED = factory.createIRI(NAMESPACE, "undefined");
		UNIT = factory.createIRI(NAMESPACE, "unit");
		XONE = factory.createIRI(NAMESPACE, "xone");

		ALL = factory.createIRI(NAMESPACE, "All");
		ALL2ND_CONNECTIONS = factory.createIRI(NAMESPACE, "All2ndConnections");
		ALL_CONNECTIONS = factory.createIRI(NAMESPACE, "AllConnections");
		ALL_GROUPS = factory.createIRI(NAMESPACE, "AllGroups");
		GROUP = factory.createIRI(NAMESPACE, "Group");
		INDIVIDUAL = factory.createIRI(NAMESPACE, "Individual");
		ABSOLUTE_POSITION = factory.createIRI(NAMESPACE, "absolutePosition");
		ABSOLUTE_SIZE = factory.createIRI(NAMESPACE, "absoluteSize");
		ABSOLUTE_SPATIAL_POSITION = factory.createIRI(NAMESPACE, "absoluteSpatialPosition");
		ABSOLUTE_TEMPORAL_POSITION = factory.createIRI(NAMESPACE, "absoluteTemporalPosition");
		ACCEPT_TRACKING = factory.createIRI(NAMESPACE, "acceptTracking");
		AD_HOC_SHARE = factory.createIRI(NAMESPACE, "adHocShare");
		AGGREGATE = factory.createIRI(NAMESPACE, "aggregate");
		ANNOTATE = factory.createIRI(NAMESPACE, "annotate");
		ANONYMIZE = factory.createIRI(NAMESPACE, "anonymize");
		APPEND = factory.createIRI(NAMESPACE, "append");
		APPEND_TO = factory.createIRI(NAMESPACE, "appendTo");
		ARCHIVE = factory.createIRI(NAMESPACE, "archive");
		ATTACH_POLICY = factory.createIRI(NAMESPACE, "attachPolicy");
		ATTACH_SOURCE = factory.createIRI(NAMESPACE, "attachSource");
		ATTRIBUTE = factory.createIRI(NAMESPACE, "attribute");
		COMMERCIALIZE = factory.createIRI(NAMESPACE, "commercialize");
		COMPENSATE = factory.createIRI(NAMESPACE, "compensate");
		CONCURRENT_USE = factory.createIRI(NAMESPACE, "concurrentUse");
		COPY = factory.createIRI(NAMESPACE, "copy");
		COUNT = factory.createIRI(NAMESPACE, "count");
		DATE_TIME = factory.createIRI(NAMESPACE, "dateTime");
		DELAY_PERIOD = factory.createIRI(NAMESPACE, "delayPeriod");
		DELETE = factory.createIRI(NAMESPACE, "delete");
		DELIVERY_CHANNEL = factory.createIRI(NAMESPACE, "deliveryChannel");
		DERIVE = factory.createIRI(NAMESPACE, "derive");
		DEVICE = factory.createIRI(NAMESPACE, "device");
		DIGITIZE = factory.createIRI(NAMESPACE, "digitize");
		DISPLAY = factory.createIRI(NAMESPACE, "display");
		DISTRIBUTE = factory.createIRI(NAMESPACE, "distribute");
		ELAPSED_TIME = factory.createIRI(NAMESPACE, "elapsedTime");
		ENSURE_EXCLUSIVITY = factory.createIRI(NAMESPACE, "ensureExclusivity");
		EQ = factory.createIRI(NAMESPACE, "eq");
		EVENT = factory.createIRI(NAMESPACE, "event");
		EXECUTE = factory.createIRI(NAMESPACE, "execute");
		EXPORT = factory.createIRI(NAMESPACE, "export");
		EXTRACT = factory.createIRI(NAMESPACE, "extract");
		EXTRACT_CHAR = factory.createIRI(NAMESPACE, "extractChar");
		EXTRACT_PAGE = factory.createIRI(NAMESPACE, "extractPage");
		EXTRACT_WORD = factory.createIRI(NAMESPACE, "extractWord");
		FILE_FORMAT = factory.createIRI(NAMESPACE, "fileFormat");
		GIVE = factory.createIRI(NAMESPACE, "give");
		GRANT_USE = factory.createIRI(NAMESPACE, "grantUse");
		GT = factory.createIRI(NAMESPACE, "gt");
		GTEQ = factory.createIRI(NAMESPACE, "gteq");
		HAS_PART = factory.createIRI(NAMESPACE, "hasPart");
		IGNORE = factory.createIRI(NAMESPACE, "ignore");
		INCLUDE = factory.createIRI(NAMESPACE, "include");
		INDEX = factory.createIRI(NAMESPACE, "index");
		INDUSTRY = factory.createIRI(NAMESPACE, "industry");
		INFORM = factory.createIRI(NAMESPACE, "inform");
		INSTALL = factory.createIRI(NAMESPACE, "install");
		INVALID = factory.createIRI(NAMESPACE, "invalid");
		IS_A = factory.createIRI(NAMESPACE, "isA");
		IS_ALL_OF = factory.createIRI(NAMESPACE, "isAllOf");
		IS_ANY_OF = factory.createIRI(NAMESPACE, "isAnyOf");
		IS_NONE_OF = factory.createIRI(NAMESPACE, "isNoneOf");
		IS_PART_OF = factory.createIRI(NAMESPACE, "isPartOf");
		LANGUAGE = factory.createIRI(NAMESPACE, "language");
		LEASE = factory.createIRI(NAMESPACE, "lease");
		LEND = factory.createIRI(NAMESPACE, "lend");
		LICENSE = factory.createIRI(NAMESPACE, "license");
		LT = factory.createIRI(NAMESPACE, "lt");
		LTEQ = factory.createIRI(NAMESPACE, "lteq");
		MEDIA = factory.createIRI(NAMESPACE, "media");
		METERED_TIME = factory.createIRI(NAMESPACE, "meteredTime");
		MODIFY = factory.createIRI(NAMESPACE, "modify");
		MOVE = factory.createIRI(NAMESPACE, "move");
		NEQ = factory.createIRI(NAMESPACE, "neq");
		NEXT_POLICY = factory.createIRI(NAMESPACE, "nextPolicy");
		OBTAIN_CONSENT = factory.createIRI(NAMESPACE, "obtainConsent");
		PAY = factory.createIRI(NAMESPACE, "pay");
		PAY_AMOUNT = factory.createIRI(NAMESPACE, "payAmount");
		PERCENTAGE = factory.createIRI(NAMESPACE, "percentage");
		PERM = factory.createIRI(NAMESPACE, "perm");
		PLAY = factory.createIRI(NAMESPACE, "play");
		POLICY_USAGE = factory.createIRI(NAMESPACE, "policyUsage");
		PRESENT = factory.createIRI(NAMESPACE, "present");
		PREVIEW = factory.createIRI(NAMESPACE, "preview");
		PRINT = factory.createIRI(NAMESPACE, "print");
		PRODUCT = factory.createIRI(NAMESPACE, "product");
		PROHIBIT = factory.createIRI(NAMESPACE, "prohibit");
		PURPOSE = factory.createIRI(NAMESPACE, "purpose");
		READ = factory.createIRI(NAMESPACE, "read");
		RECIPIENT = factory.createIRI(NAMESPACE, "recipient");
		RELATIVE_POSITION = factory.createIRI(NAMESPACE, "relativePosition");
		RELATIVE_SIZE = factory.createIRI(NAMESPACE, "relativeSize");
		RELATIVE_SPATIAL_POSITION = factory.createIRI(NAMESPACE, "relativeSpatialPosition");
		RELATIVE_TEMPORAL_POSITION = factory.createIRI(NAMESPACE, "relativeTemporalPosition");
		REPRODUCE = factory.createIRI(NAMESPACE, "reproduce");
		RESOLUTION = factory.createIRI(NAMESPACE, "resolution");
		REVIEW_POLICY = factory.createIRI(NAMESPACE, "reviewPolicy");
		SECONDARY_USE = factory.createIRI(NAMESPACE, "secondaryUse");
		SELL = factory.createIRI(NAMESPACE, "sell");
		SHARE = factory.createIRI(NAMESPACE, "share");
		SHARE_ALIKE = factory.createIRI(NAMESPACE, "shareAlike");
		SPATIAL = factory.createIRI(NAMESPACE, "spatial");
		SPATIAL_COORDINATES = factory.createIRI(NAMESPACE, "spatialCoordinates");
		STREAM = factory.createIRI(NAMESPACE, "stream");
		SUPPORT = factory.createIRI(NAMESPACE, "support");
		SYNCHRONIZE = factory.createIRI(NAMESPACE, "synchronize");
		SYSTEM = factory.createIRI(NAMESPACE, "system");
		SYSTEM_DEVICE = factory.createIRI(NAMESPACE, "systemDevice");
		TEXT_TO_SPEECH = factory.createIRI(NAMESPACE, "textToSpeech");
		TIME_INTERVAL = factory.createIRI(NAMESPACE, "timeInterval");
		TRANSFER = factory.createIRI(NAMESPACE, "transfer");
		TRANSFORM = factory.createIRI(NAMESPACE, "transform");
		TRANSLATE = factory.createIRI(NAMESPACE, "translate");
		UNINSTALL = factory.createIRI(NAMESPACE, "uninstall");
		UNIT_OF_COUNT = factory.createIRI(NAMESPACE, "unitOfCount");
		USE = factory.createIRI(NAMESPACE, "use");
		VERSION = factory.createIRI(NAMESPACE, "version");
		VIRTUAL_LOCATION = factory.createIRI(NAMESPACE, "virtualLocation");
		WATERMARK = factory.createIRI(NAMESPACE, "watermark");
		WRITE = factory.createIRI(NAMESPACE, "write");
		WRITE_TO = factory.createIRI(NAMESPACE, "writeTo");
	}
}
