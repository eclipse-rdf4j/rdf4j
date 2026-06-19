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

package org.eclipse.rdf4j.model.vocabulary.biblio;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.base.Vocabularies;

/**
 * Constants for the Functional Requirements for Bibliographic Records.
 *
 * @see <a href="http://purl.org/vocab/frbr/core">Functional Requirements for Bibliographic Records</a>
 *
 * @author Bart Hanssens
 */
public class FRBR {
	/**
	 * The FRBR namespace: http://purl.org/vocab/frbr/core#
	 */
	public static final String NAMESPACE = "http://purl.org/vocab/frbr/core#";

	/**
	 * Recommended prefix for the namespace: "frbr"
	 */
	public static final String PREFIX = "frbr";

	/**
	 * An immutable {@link Namespace} constant that represents the namespace.
	 */
	public static final Namespace NS = Vocabularies.createNamespace(PREFIX, NAMESPACE);

	// Classes
	/** frbr:Concept */
	public static final IRI Concept;

	/** frbr:CorporateBody */
	public static final IRI CorporateBody;

	/** frbr:Endeavour */
	public static final IRI Endeavour;

	/** frbr:Event */
	public static final IRI Event;

	/** frbr:Expression */
	public static final IRI Expression;

	/** frbr:Item */
	public static final IRI Item;

	/** frbr:Manifestation */
	public static final IRI Manifestation;

	/** frbr:Object */
	public static final IRI Object;

	/** frbr:Person */
	public static final IRI Person;

	/** frbr:Place */
	public static final IRI Place;

	/** frbr:ResponsibleEntity */
	public static final IRI ResponsibleEntity;

	/** frbr:Subject */
	public static final IRI Subject;

	/** frbr:Work */
	public static final IRI Work;

	// Properties
	/** frbr:abridgement */
	public static final IRI abridgement;

	/** frbr:abridgementOf */
	public static final IRI abridgementOf;

	/** frbr:adaption */
	public static final IRI adaption;

	/** frbr:adaptionOf */
	public static final IRI adaptionOf;

	/** frbr:alternate */
	public static final IRI alternate;

	/** frbr:alternateOf */
	public static final IRI alternateOf;

	/** frbr:arrangement */
	public static final IRI arrangement;

	/** frbr:arrangementOf */
	public static final IRI arrangementOf;

	/** frbr:complement */
	public static final IRI complement;

	/** frbr:complementOf */
	public static final IRI complementOf;

	/** frbr:creator */
	public static final IRI creator;

	/** frbr:creatorOf */
	public static final IRI creatorOf;

	/** frbr:embodiment */
	public static final IRI embodiment;

	/** frbr:embodimentOf */
	public static final IRI embodimentOf;

	/** frbr:exemplar */
	public static final IRI exemplar;

	/** frbr:exemplarOf */
	public static final IRI exemplarOf;

	/** frbr:imitation */
	public static final IRI imitation;

	/** frbr:imitationOf */
	public static final IRI imitationOf;

	/** frbr:owner */
	public static final IRI owner;

	/** frbr:ownerOf */
	public static final IRI ownerOf;

	/** frbr:part */
	public static final IRI part;

	/** frbr:partOf */
	public static final IRI partOf;

	/** frbr:producer */
	public static final IRI producer;

	/** frbr:producerOf */
	public static final IRI producerOf;

	/** frbr:realization */
	public static final IRI realization;

	/** frbr:realizationOf */
	public static final IRI realizationOf;

	/** frbr:realizer */
	public static final IRI realizer;

	/** frbr:realizerOf */
	public static final IRI realizerOf;

	/** frbr:reconfiguration */
	public static final IRI reconfiguration;

	/** frbr:reconfigurationOf */
	public static final IRI reconfigurationOf;

	/** frbr:relatedEndeavour */
	public static final IRI relatedEndeavour;

	/** frbr:reproduction */
	public static final IRI reproduction;

	/** frbr:reproductionOf */
	public static final IRI reproductionOf;

	/** frbr:responsibleEntity */
	public static final IRI responsibleEntity;

	/** frbr:responsibleEntityOf */
	public static final IRI responsibleEntityOf;

	/** frbr:revision */
	public static final IRI revision;

	/** frbr:revisionOf */
	public static final IRI revisionOf;

	/** frbr:subject */
	public static final IRI subject;

	/** frbr:successor */
	public static final IRI successor;

	/** frbr:successorOf */
	public static final IRI successorOf;

	/** frbr:summarization */
	public static final IRI summarization;

	/** frbr:summarizationOf */
	public static final IRI summarizationOf;

	/** frbr:supplement */
	public static final IRI supplement;

	/** frbr:supplementOf */
	public static final IRI supplementOf;

	/** frbr:transformation */
	public static final IRI transformation;

	/** frbr:transformationOf */
	public static final IRI transformationOf;

	/** frbr:translation */
	public static final IRI translation;

	/** frbr:translationOf */
	public static final IRI translationOf;

	static {
		Concept = Vocabularies.createIRI(NAMESPACE, "Concept");
		CorporateBody = Vocabularies.createIRI(NAMESPACE, "CorporateBody");
		Endeavour = Vocabularies.createIRI(NAMESPACE, "Endeavour");
		Event = Vocabularies.createIRI(NAMESPACE, "Event");
		Expression = Vocabularies.createIRI(NAMESPACE, "Expression");
		Item = Vocabularies.createIRI(NAMESPACE, "Item");
		Manifestation = Vocabularies.createIRI(NAMESPACE, "Manifestation");
		Object = Vocabularies.createIRI(NAMESPACE, "Object");
		Person = Vocabularies.createIRI(NAMESPACE, "Person");
		Place = Vocabularies.createIRI(NAMESPACE, "Place");
		ResponsibleEntity = Vocabularies.createIRI(NAMESPACE, "ResponsibleEntity");
		Subject = Vocabularies.createIRI(NAMESPACE, "Subject");
		Work = Vocabularies.createIRI(NAMESPACE, "Work");

		abridgement = Vocabularies.createIRI(NAMESPACE, "abridgement");
		abridgementOf = Vocabularies.createIRI(NAMESPACE, "abridgementOf");
		adaption = Vocabularies.createIRI(NAMESPACE, "adaption");
		adaptionOf = Vocabularies.createIRI(NAMESPACE, "adaptionOf");
		alternate = Vocabularies.createIRI(NAMESPACE, "alternate");
		alternateOf = Vocabularies.createIRI(NAMESPACE, "alternateOf");
		arrangement = Vocabularies.createIRI(NAMESPACE, "arrangement");
		arrangementOf = Vocabularies.createIRI(NAMESPACE, "arrangementOf");
		complement = Vocabularies.createIRI(NAMESPACE, "complement");
		complementOf = Vocabularies.createIRI(NAMESPACE, "complementOf");
		creator = Vocabularies.createIRI(NAMESPACE, "creator");
		creatorOf = Vocabularies.createIRI(NAMESPACE, "creatorOf");
		embodiment = Vocabularies.createIRI(NAMESPACE, "embodiment");
		embodimentOf = Vocabularies.createIRI(NAMESPACE, "embodimentOf");
		exemplar = Vocabularies.createIRI(NAMESPACE, "exemplar");
		exemplarOf = Vocabularies.createIRI(NAMESPACE, "exemplarOf");
		imitation = Vocabularies.createIRI(NAMESPACE, "imitation");
		imitationOf = Vocabularies.createIRI(NAMESPACE, "imitationOf");
		owner = Vocabularies.createIRI(NAMESPACE, "owner");
		ownerOf = Vocabularies.createIRI(NAMESPACE, "ownerOf");
		part = Vocabularies.createIRI(NAMESPACE, "part");
		partOf = Vocabularies.createIRI(NAMESPACE, "partOf");
		producer = Vocabularies.createIRI(NAMESPACE, "producer");
		producerOf = Vocabularies.createIRI(NAMESPACE, "producerOf");
		realization = Vocabularies.createIRI(NAMESPACE, "realization");
		realizationOf = Vocabularies.createIRI(NAMESPACE, "realizationOf");
		realizer = Vocabularies.createIRI(NAMESPACE, "realizer");
		realizerOf = Vocabularies.createIRI(NAMESPACE, "realizerOf");
		reconfiguration = Vocabularies.createIRI(NAMESPACE, "reconfiguration");
		reconfigurationOf = Vocabularies.createIRI(NAMESPACE, "reconfigurationOf");
		relatedEndeavour = Vocabularies.createIRI(NAMESPACE, "relatedEndeavour");
		reproduction = Vocabularies.createIRI(NAMESPACE, "reproduction");
		reproductionOf = Vocabularies.createIRI(NAMESPACE, "reproductionOf");
		responsibleEntity = Vocabularies.createIRI(NAMESPACE, "responsibleEntity");
		responsibleEntityOf = Vocabularies.createIRI(NAMESPACE, "responsibleEntityOf");
		revision = Vocabularies.createIRI(NAMESPACE, "revision");
		revisionOf = Vocabularies.createIRI(NAMESPACE, "revisionOf");
		subject = Vocabularies.createIRI(NAMESPACE, "subject");
		successor = Vocabularies.createIRI(NAMESPACE, "successor");
		successorOf = Vocabularies.createIRI(NAMESPACE, "successorOf");
		summarization = Vocabularies.createIRI(NAMESPACE, "summarization");
		summarizationOf = Vocabularies.createIRI(NAMESPACE, "summarizationOf");
		supplement = Vocabularies.createIRI(NAMESPACE, "supplement");
		supplementOf = Vocabularies.createIRI(NAMESPACE, "supplementOf");
		transformation = Vocabularies.createIRI(NAMESPACE, "transformation");
		transformationOf = Vocabularies.createIRI(NAMESPACE, "transformationOf");
		translation = Vocabularies.createIRI(NAMESPACE, "translation");
		translationOf = Vocabularies.createIRI(NAMESPACE, "translationOf");
	}
}
