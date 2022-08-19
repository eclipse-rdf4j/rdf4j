/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.shacl.ast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.query.algebra.evaluation.util.ValueComparator;
import org.eclipse.rdf4j.sail.shacl.wrapper.shape.ShapeSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShaclProperties {

	private static final Logger logger = LoggerFactory.getLogger(ShaclProperties.class);

	private IRI type;

	private final List<IRI> clazz = new ArrayList<>();
	private final List<Resource> or = new ArrayList<>();
	private final List<Resource> xone = new ArrayList<>();
	private final List<Resource> and = new ArrayList<>();
	private final List<Resource> not = new ArrayList<>();
	private final List<Resource> node = new ArrayList<>();
	private final List<Resource> property = new ArrayList<>();

	private final List<IRI> equals = new ArrayList<>();
	private final List<IRI> disjoint = new ArrayList<>();
	private final List<IRI> lessThan = new ArrayList<>();
	private final List<IRI> lessThanOrEquals = new ArrayList<>();

	private Long minCount;
	private Long maxCount;

	private IRI datatype;
	private Resource in;
	private final List<Value> hasValue = new ArrayList<>();
	private final List<Resource> hasValueIn = new ArrayList<>();

	private Long minLength;
	private Long maxLength;

	private Resource languageIn;
	private Resource nodeKind;

	private Resource path;

	private Literal minExclusive;
	private Literal maxExclusive;
	private Literal minInclusive;
	private Literal maxInclusive;

	private final List<String> pattern = new ArrayList<>();
	private String flags = null;

	private final Set<Resource> targetClass = new HashSet<>();
	private final TreeSet<Value> targetNode = new TreeSet<>(new ValueComparator());
	private final Set<IRI> targetSubjectsOf = new HashSet<>();
	private final Set<IRI> targetObjectsOf = new HashSet<>();
	private final List<Resource> targetShape = new ArrayList<>();

	private Resource qualifiedValueShape;
	private Long qualifiedMinCount;
	private Long qualifiedMaxCount;
	private Boolean qualifiedValueShapesDisjoint;

	private final List<Resource> target = new ArrayList<>();

	private boolean deactivated = false;

	private boolean uniqueLang = false;

	boolean closed = false;
	private Resource ignoredProperties;

	private Resource id;

	private final List<Literal> message = new ArrayList<>();

	public ShaclProperties() {
	}

	public ShaclProperties(Resource id, ShapeSource connection) {
		this.id = id;
		try (Stream<Statement> stream = connection.getAllStatements(id)) {
			stream.forEach(statement -> {

				String predicate = statement.getPredicate().toString();
				Value object = statement.getObject();

				switch (predicate) {
				case "http://www.w3.org/1999/02/22-rdf-syntax-ns#type":
					if (object.stringValue().equals("http://www.w3.org/ns/shacl#NodeShape")) {
						if (type != null && !type.equals(SHACL.NODE_SHAPE)) {
							throw new IllegalStateException(
									"Shape with multiple types: <" + type + ">, <" + SHACL.NODE_SHAPE + ">");
						}
						type = SHACL.NODE_SHAPE;
					} else if (object.stringValue().equals("http://www.w3.org/ns/shacl#PropertyShape")) {
						if (type != null && !type.equals(SHACL.PROPERTY_SHAPE)) {
							throw new IllegalStateException(
									"Shape with multiple types: <" + type + ">, <" + SHACL.PROPERTY_SHAPE + ">");
						}
						type = SHACL.PROPERTY_SHAPE;
					}
					break;
				case "http://www.w3.org/ns/shacl#or":
					or.add((Resource) object);
					break;
				case "http://www.w3.org/ns/shacl#xone":
					xone.add((Resource) object);
					break;
				case "http://www.w3.org/ns/shacl#and":
					and.add((Resource) object);
					break;
				case "http://www.w3.org/ns/shacl#not":
					not.add((Resource) object);
					break;
				case "http://www.w3.org/ns/shacl#property":
					property.add((Resource) object);
					break;
				case "http://www.w3.org/ns/shacl#node":
					node.add((Resource) object);
					break;
				case "http://www.w3.org/ns/shacl#message":
					message.add((Literal) object);
					break;
				case "http://www.w3.org/ns/shacl#languageIn":
					if (languageIn != null) {
						throw new IllegalStateException(predicate + " already populated");
					}
					languageIn = (Resource) object;
					break;
				case "http://www.w3.org/ns/shacl#nodeKind":
					if (nodeKind != null) {
						throw new IllegalStateException(predicate + " already populated");
					}
					nodeKind = (Resource) object;
					break;
				case "http://www.w3.org/ns/shacl#datatype":
					if (datatype != null) {
						throw new IllegalStateException(predicate + " already populated");
					}
					datatype = (IRI) object;
					break;
				case "http://www.w3.org/ns/shacl#minCount":
					if (minCount != null) {
						throw new IllegalStateException(predicate + " already populated");
					}
					minCount = ((Literal) object).longValue();
					break;
				case "http://www.w3.org/ns/shacl#maxCount":
					if (maxCount != null) {
						throw new IllegalStateException(predicate + " already populated");
					}
					maxCount = ((Literal) object).longValue();
					break;
				case "http://www.w3.org/ns/shacl#minLength":
					if (minLength != null) {
						throw new IllegalStateException(predicate + " already populated");
					}
					minLength = ((Literal) object).longValue();
					break;
				case "http://www.w3.org/ns/shacl#maxLength":
					if (maxLength != null) {
						throw new IllegalStateException(predicate + " already populated");
					}
					maxLength = ((Literal) object).longValue();
					break;
				case "http://www.w3.org/ns/shacl#minExclusive":
					if (minExclusive != null) {
						throw new IllegalStateException(predicate + " already populated");
					}
					minExclusive = (Literal) object;
					break;
				case "http://www.w3.org/ns/shacl#maxExclusive":
					if (maxExclusive != null) {
						throw new IllegalStateException(predicate + " already populated");
					}
					maxExclusive = (Literal) object;
					break;
				case "http://www.w3.org/ns/shacl#minInclusive":
					if (minInclusive != null) {
						throw new IllegalStateException(predicate + " already populated");
					}
					minInclusive = (Literal) object;
					break;
				case "http://www.w3.org/ns/shacl#maxInclusive":
					if (maxInclusive != null) {
						throw new IllegalStateException(predicate + " already populated");
					}
					maxInclusive = (Literal) object;
					break;
				case "http://www.w3.org/ns/shacl#pattern":
					pattern.add(object.stringValue());
					break;
				case "http://www.w3.org/ns/shacl#class":
					clazz.add((IRI) object);
					break;
				case "http://www.w3.org/ns/shacl#targetNode":
					targetNode.add(object);
					break;
				case "http://www.w3.org/ns/shacl#targetClass":
					targetClass.add((Resource) object);
					break;
				case "http://www.w3.org/ns/shacl#targetSubjectsOf":
					targetSubjectsOf.add((IRI) object);
					break;
				case "http://www.w3.org/ns/shacl#targetObjectsOf":
					targetObjectsOf.add((IRI) object);
					break;
				case "http://www.w3.org/ns/shacl#deactivated":
					deactivated = ((Literal) object).booleanValue();
					break;
				case "http://www.w3.org/ns/shacl#uniqueLang":
					uniqueLang = ((Literal) object).booleanValue();
					break;
				case "http://www.w3.org/ns/shacl#closed":
					closed = ((Literal) object).booleanValue();
					break;
				case "http://www.w3.org/ns/shacl#ignoredProperties":
					if (ignoredProperties != null) {
						throw new IllegalStateException(predicate + " already populated");
					}
					ignoredProperties = ((Resource) object);
					break;
				case "http://www.w3.org/ns/shacl#flags":
					if (flags != null) {
						throw new IllegalStateException(predicate + " already populated");
					}
					flags = object.stringValue();
					break;
				case "http://www.w3.org/ns/shacl#path":
					if (path != null) {
						throw new IllegalStateException(predicate + " already populated");
					}
					assert type != SHACL.NODE_SHAPE;
					path = (Resource) object;
					break;
				case "http://www.w3.org/ns/shacl#in":
					if (in != null) {
						throw new IllegalStateException(predicate + " already populated");
					}
					in = (Resource) object;
					break;
				case "http://www.w3.org/ns/shacl#equals":
					equals.add((IRI) object);
					break;
				case "http://www.w3.org/ns/shacl#disjoint":
					disjoint.add((IRI) object);
					break;
				case "http://www.w3.org/ns/shacl#lessThan":
					lessThan.add((IRI) object);
					break;
				case "http://www.w3.org/ns/shacl#lessThanOrEquals":
					lessThanOrEquals.add((IRI) object);
					break;
				case "http://www.w3.org/ns/shacl#target":
					target.add((Resource) object);
					break;
				case "http://www.w3.org/ns/shacl#hasValue":
					hasValue.add(object);
					break;
				case "http://www.w3.org/ns/shacl#qualifiedValueShape":
					if (qualifiedValueShape != null) {
						throw new IllegalStateException(predicate + " already populated");
					}
					qualifiedValueShape = ((Resource) object);
					break;
				case "http://www.w3.org/ns/shacl#qualifiedValueShapesDisjoint":
					if (qualifiedValueShapesDisjoint != null) {
						throw new IllegalStateException(predicate + " already populated");
					}
					qualifiedValueShapesDisjoint = ((Literal) object).booleanValue();
					break;
				case "http://www.w3.org/ns/shacl#qualifiedMinCount":
					if (qualifiedMinCount != null) {
						throw new IllegalStateException(predicate + " already populated");
					}
					qualifiedMinCount = ((Literal) object).longValue();
					break;
				case "http://www.w3.org/ns/shacl#qualifiedMaxCount":
					if (qualifiedMaxCount != null) {
						throw new IllegalStateException(predicate + " already populated");
					}
					qualifiedMaxCount = ((Literal) object).longValue();
					break;
				case "http://datashapes.org/dash#hasValueIn":
					hasValueIn.add((Resource) object);
					break;
				case "http://rdf4j.org/shacl-extensions#targetShape":
					targetShape.add((Resource) object);
					break;

				default:
					if (predicate.startsWith(SHACL.NAMESPACE)) {
						logger.warn("Unsupported SHACL feature detected {} in statement {}",
								predicate.replace("http://www.w3.org/ns/shacl#", "sh:"),
								statement);
					}
				}

			});
		}

		// We default to sh:NodeShape if no other type is given.
		if (type == null) {
			type = SHACL.NODE_SHAPE;
		}

	}

	public List<IRI> getClazz() {
		return clazz;
	}

	public List<Resource> getOr() {
		return or;
	}

	public List<Resource> getAnd() {
		return and;
	}

	public List<Resource> getNot() {
		return not;
	}

	public Long getMinCount() {
		return minCount;
	}

	public Long getMaxCount() {
		return maxCount;
	}

	public IRI getDatatype() {
		return datatype;
	}

	public Resource getIn() {
		return in;
	}

	public Long getMinLength() {
		return minLength;
	}

	public Long getMaxLength() {
		return maxLength;
	}

	public Resource getLanguageIn() {
		return languageIn;
	}

	public Resource getNodeKind() {
		return nodeKind;
	}

	public Resource getPath() {
		return path;
	}

	public Literal getMinExclusive() {
		return minExclusive;
	}

	public Literal getMaxExclusive() {
		return maxExclusive;
	}

	public Literal getMinInclusive() {
		return minInclusive;
	}

	public Literal getMaxInclusive() {
		return maxInclusive;
	}

	public List<String> getPattern() {
		return pattern;
	}

	public String getFlags() {
		return flags;
	}

	public Set<Resource> getTargetClass() {
		return targetClass;
	}

	public TreeSet<Value> getTargetNode() {
		return targetNode;
	}

	public Set<IRI> getTargetSubjectsOf() {
		return targetSubjectsOf;
	}

	public Set<IRI> getTargetObjectsOf() {
		return targetObjectsOf;
	}

	public boolean isDeactivated() {
		return deactivated;
	}

	public boolean isUniqueLang() {
		return uniqueLang;
	}

	public Resource getId() {
		return id;
	}

	public IRI getType() {
		return type;
	}

	public List<Literal> getMessage() {
		return message;
	}

	public List<Resource> getProperty() {
		return property;
	}

	public List<Resource> getNode() {
		return node;
	}

	public boolean isClosed() {
		return closed;
	}

	public Resource getIgnoredProperties() {
		return ignoredProperties;
	}

	public List<Resource> getXone() {
		return xone;
	}

	public List<Value> getHasValue() {
		return hasValue;
	}

	public List<IRI> getEquals() {
		return equals;
	}

	public List<IRI> getDisjoint() {
		return disjoint;
	}

	public List<IRI> getLessThan() {
		return lessThan;
	}

	public List<IRI> getLessThanOrEquals() {
		return lessThanOrEquals;
	}

	public List<Resource> getTarget() {
		return target;
	}

	public List<Resource> getTargetShape() {
		return targetShape;
	}

	public List<Resource> getHasValueIn() {
		return hasValueIn;
	}

	public Resource getQualifiedValueShape() {
		return qualifiedValueShape;
	}

	public Long getQualifiedMinCount() {
		return qualifiedMinCount;
	}

	public Long getQualifiedMaxCount() {
		return qualifiedMaxCount;
	}

	public Boolean getQualifiedValueShapesDisjoint() {
		return qualifiedValueShapesDisjoint;
	}
}
