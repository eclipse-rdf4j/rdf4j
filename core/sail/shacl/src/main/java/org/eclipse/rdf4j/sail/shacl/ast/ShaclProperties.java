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
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.query.algebra.evaluation.util.ValueComparator;
import org.eclipse.rdf4j.sail.shacl.wrapper.shape.ShapeSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShaclProperties {

	private static final Logger logger = LoggerFactory.getLogger(ShaclProperties.class);

	private Resource id;
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

	private String pattern;
	private String flags;

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

	private Boolean deactivated = null;

	private Boolean uniqueLang = null;

	private Boolean closed = null;
	private Resource ignoredProperties;

	private final List<Literal> message = new ArrayList<>();
	private IRI severity;

	private final List<Literal> name = new ArrayList<>();
	private final List<Literal> description = new ArrayList<>();

	private Value defaultValue;
	private Value order;
	private Value group;

	private final List<Resource> sparql = new ArrayList<>();

	public ShaclProperties(Resource id, ShapeSource connection) {
		this.id = id;
		try (Stream<Statement> stream = connection.getAllStatements(id)) {
			stream.forEach(statement -> {

				String predicate = statement.getPredicate().toString();
				Value object = statement.getObject();

				switch (predicate) {
				case "http://www.w3.org/1999/02/22-rdf-syntax-ns#type":
					if (object.equals(SHACL.NODE_SHAPE)) {
						if (type != null && !type.equals(SHACL.NODE_SHAPE)) {
							throw new ShaclShapeParsingException(
									"Shape with multiple types: <" + type + ">, <" + SHACL.NODE_SHAPE + ">", id);
						}
						type = SHACL.NODE_SHAPE;
					} else if (object.equals(SHACL.PROPERTY_SHAPE)) {
						if (type != null && !type.equals(SHACL.PROPERTY_SHAPE)) {
							throw new ShaclShapeParsingException(
									"Shape with multiple types: <" + type + ">, <" + SHACL.PROPERTY_SHAPE
											+ ">",
									id);
						}
						type = SHACL.PROPERTY_SHAPE;
					}
					break;
				case "http://www.w3.org/ns/shacl#or":
					try {
						or.add((Resource) object);
					} catch (ClassCastException e) {
						throw getExceptionForCastIssue(id, predicate, Resource.class, object);
					}
					break;
				case "http://www.w3.org/ns/shacl#xone":
					try {
						xone.add((Resource) object);
					} catch (ClassCastException e) {
						throw getExceptionForCastIssue(id, predicate, Resource.class, object);
					}
					break;
				case "http://www.w3.org/ns/shacl#and":
					try {
						and.add((Resource) object);
					} catch (ClassCastException e) {
						throw getExceptionForCastIssue(id, predicate, Resource.class, object);
					}
					break;
				case "http://www.w3.org/ns/shacl#not":
					try {
						not.add((Resource) object);
					} catch (ClassCastException e) {
						throw getExceptionForCastIssue(id, predicate, Resource.class, object);
					}
					break;
				case "http://www.w3.org/ns/shacl#property":
					try {
						property.add((Resource) object);
					} catch (ClassCastException e) {
						throw getExceptionForCastIssue(id, predicate, Resource.class, object);
					}
					break;
				case "http://www.w3.org/ns/shacl#node":
					try {
						node.add((Resource) object);
					} catch (ClassCastException e) {
						throw getExceptionForCastIssue(id, predicate, Resource.class, object);
					}
					break;
				case "http://www.w3.org/ns/shacl#message":
					try {
						message.add((Literal) object);
					} catch (ClassCastException e) {
						throw getExceptionForCastIssue(id, predicate, Literal.class, object);
					}
					break;
				case "http://www.w3.org/ns/shacl#name":
					try {
						name.add((Literal) object);
					} catch (ClassCastException e) {
						throw getExceptionForCastIssue(id, predicate, Literal.class, object);
					}
					break;
				case "http://www.w3.org/ns/shacl#description":
					try {
						description.add((Literal) object);
					} catch (ClassCastException e) {
						throw getExceptionForCastIssue(id, predicate, Literal.class, object);
					}
					break;

				case "http://www.w3.org/ns/shacl#severity":
					if (severity != null) {
						throw getExceptionForAlreadyPopulated(id, predicate, severity, object);
					}
					try {
						severity = (IRI) object;
					} catch (ClassCastException e) {
						throw getExceptionForCastIssue(id, predicate, IRI.class, object);
					}
					break;
				case "http://www.w3.org/ns/shacl#defaultValue":
					if (defaultValue != null) {
						throw getExceptionForAlreadyPopulated(id, predicate, defaultValue, object);
					}
					defaultValue = object;
					break;
				case "http://www.w3.org/ns/shacl#group":
					if (group != null) {
						throw getExceptionForAlreadyPopulated(id, predicate, group, object);
					}
					group = object;
					break;
				case "http://www.w3.org/ns/shacl#order":
					if (order != null) {
						throw getExceptionForAlreadyPopulated(id, predicate, order, object);
					}
					order = object;
					break;
				case "http://www.w3.org/ns/shacl#languageIn":
					if (languageIn != null) {
						throw getExceptionForAlreadyPopulated(id, predicate, languageIn, object);
					}
					try {
						languageIn = (Resource) object;
					} catch (ClassCastException e) {
						throw getExceptionForCastIssue(id, predicate, Resource.class, object);
					}
					break;
				case "http://www.w3.org/ns/shacl#nodeKind":
					if (nodeKind != null) {
						throw getExceptionForAlreadyPopulated(id, predicate, nodeKind, object);
					}
					try {
						nodeKind = (Resource) object;
					} catch (ClassCastException e) {
						throw getExceptionForCastIssue(id, predicate, Resource.class, object);
					}
					break;
				case "http://www.w3.org/ns/shacl#datatype":
					if (datatype != null) {
						throw getExceptionForAlreadyPopulated(id, predicate, datatype, object);
					}
					try {
						datatype = (IRI) object;
					} catch (ClassCastException e) {
						throw getExceptionForCastIssue(id, predicate, IRI.class, object);
					}
					break;
				case "http://www.w3.org/ns/shacl#minCount":
					if (minCount != null) {
						throw getExceptionForAlreadyPopulated(id, predicate, minCount, object);
					}
					try {
						minCount = ((Literal) object).longValue();
					} catch (ClassCastException e) {
						throw getExceptionForCastIssue(id, predicate, Literal.class, object);
					} catch (NumberFormatException e) {
						throw getExceptionForLiteralFormatIssue(id, predicate, object, Long.class);
					}
					break;
				case "http://www.w3.org/ns/shacl#maxCount":
					if (maxCount != null) {
						throw getExceptionForAlreadyPopulated(id, predicate, maxCount, object);
					}
					try {
						maxCount = ((Literal) object).longValue();
					} catch (ClassCastException e) {
						throw getExceptionForCastIssue(id, predicate, Literal.class, object);
					} catch (NumberFormatException e) {
						throw getExceptionForLiteralFormatIssue(id, predicate, object, Long.class);
					}
					break;
				case "http://www.w3.org/ns/shacl#minLength":
					if (minLength != null) {
						throw getExceptionForAlreadyPopulated(id, predicate, minLength, object);
					}
					try {
						minLength = ((Literal) object).longValue();
					} catch (ClassCastException e) {
						throw getExceptionForCastIssue(id, predicate, Literal.class, object);
					} catch (NumberFormatException e) {
						throw getExceptionForLiteralFormatIssue(id, predicate, object, Long.class);
					}
					break;
				case "http://www.w3.org/ns/shacl#maxLength":
					if (maxLength != null) {
						throw getExceptionForAlreadyPopulated(id, predicate, maxLength, object);
					}
					try {
						maxLength = ((Literal) object).longValue();
					} catch (ClassCastException e) {
						throw getExceptionForCastIssue(id, predicate, Literal.class, object);
					} catch (NumberFormatException e) {
						throw getExceptionForLiteralFormatIssue(id, predicate, object, Long.class);
					}
					break;
				case "http://www.w3.org/ns/shacl#minExclusive":
					if (minExclusive != null) {
						throw getExceptionForAlreadyPopulated(id, predicate, minExclusive, object);
					}
					try {
						minExclusive = (Literal) object;
					} catch (ClassCastException e) {
						throw getExceptionForCastIssue(id, predicate, Literal.class, object);
					}
					break;
				case "http://www.w3.org/ns/shacl#maxExclusive":
					if (maxExclusive != null) {
						throw getExceptionForAlreadyPopulated(id, predicate, maxExclusive, object);
					}
					try {
						maxExclusive = (Literal) object;
					} catch (ClassCastException e) {
						throw getExceptionForCastIssue(id, predicate, Literal.class, object);
					}
					break;
				case "http://www.w3.org/ns/shacl#minInclusive":
					if (minInclusive != null) {
						throw getExceptionForAlreadyPopulated(id, predicate, minInclusive, object);
					}
					try {
						minInclusive = (Literal) object;
					} catch (ClassCastException e) {
						throw getExceptionForCastIssue(id, predicate, Literal.class, object);
					}
					break;
				case "http://www.w3.org/ns/shacl#maxInclusive":
					if (maxInclusive != null) {
						throw getExceptionForAlreadyPopulated(id, predicate, maxInclusive, object);
					}
					try {
						maxInclusive = (Literal) object;
					} catch (ClassCastException e) {
						throw getExceptionForCastIssue(id, predicate, Literal.class, object);
					}
					break;
				case "http://www.w3.org/ns/shacl#pattern":
					if (pattern != null) {
						throw getExceptionForAlreadyPopulated(id, predicate, pattern, object);
					}
					try {
						pattern = ((Literal) object).getLabel();
					} catch (ClassCastException e) {
						throw getExceptionForCastIssue(id, predicate, Literal.class, object);
					}
					break;
				case "http://www.w3.org/ns/shacl#class":
					try {
						clazz.add((IRI) object);
					} catch (ClassCastException e) {
						throw getExceptionForCastIssue(id, predicate, IRI.class, object);
					}
					break;
				case "http://www.w3.org/ns/shacl#targetNode":
					if (!object.isLiteral() && !object.isIRI()) {
						throw new ShaclShapeParsingException("Expected predicate <" + predicate
								+ "> to have a Literal or an IRI as object, but found "
								+ getClassName(object) + " for " + object, id);
					}
					targetNode.add(object);
					break;
				case "http://www.w3.org/ns/shacl#targetClass":
					try {
						targetClass.add((Resource) object);
					} catch (ClassCastException e) {
						throw getExceptionForCastIssue(id, predicate, Resource.class, object);
					}
					break;
				case "http://www.w3.org/ns/shacl#targetSubjectsOf":
					try {
						targetSubjectsOf.add((IRI) object);
					} catch (ClassCastException e) {
						throw getExceptionForCastIssue(id, predicate, IRI.class, object);
					}
					break;
				case "http://www.w3.org/ns/shacl#targetObjectsOf":
					try {
						targetObjectsOf.add((IRI) object);
					} catch (ClassCastException e) {
						throw getExceptionForCastIssue(id, predicate, IRI.class, object);
					}
					break;
				case "http://www.w3.org/ns/shacl#deactivated":
					if (deactivated != null) {
						throw getExceptionForAlreadyPopulated(id, predicate, deactivated, object);
					}
					try {
						deactivated = ((Literal) object).booleanValue();
					} catch (ClassCastException e) {
						throw getExceptionForCastIssue(id, predicate, Literal.class, object);
					} catch (IllegalArgumentException e) {
						throw getExceptionForLiteralFormatIssue(id, predicate, object, Boolean.class);
					}
					break;
				case "http://www.w3.org/ns/shacl#uniqueLang":
					if (uniqueLang != null) {
						throw getExceptionForAlreadyPopulated(id, predicate, uniqueLang, object);
					}
					try {
						uniqueLang = ((Literal) object).booleanValue();
					} catch (ClassCastException e) {
						throw getExceptionForCastIssue(id, predicate, Literal.class, object);
					} catch (IllegalArgumentException e) {
						throw getExceptionForLiteralFormatIssue(id, predicate, object, Boolean.class);
					}
					break;
				case "http://www.w3.org/ns/shacl#closed":
					if (closed != null) {
						throw getExceptionForAlreadyPopulated(id, predicate, closed, object);
					}
					try {
						closed = ((Literal) object).booleanValue();
					} catch (ClassCastException e) {
						throw getExceptionForCastIssue(id, predicate, Literal.class, object);
					} catch (IllegalArgumentException e) {
						throw getExceptionForLiteralFormatIssue(id, predicate, object, Boolean.class);
					}
					break;
				case "http://www.w3.org/ns/shacl#ignoredProperties":
					if (ignoredProperties != null) {
						throw getExceptionForAlreadyPopulated(id, predicate, ignoredProperties, object);
					}
					try {
						ignoredProperties = ((Resource) object);
					} catch (ClassCastException e) {
						throw getExceptionForCastIssue(id, predicate, Resource.class, object);
					}
					break;
				case "http://www.w3.org/ns/shacl#flags":
					if (flags != null) {
						throw getExceptionForAlreadyPopulated(id, predicate, flags, object);
					}
					try {
						flags = ((Literal) object).getLabel();
					} catch (ClassCastException e) {
						throw getExceptionForCastIssue(id, predicate, Literal.class, object);
					}
					break;
				case "http://www.w3.org/ns/shacl#path":
					if (path != null) {
						throw getExceptionForAlreadyPopulated(id, predicate, path, object);
					}
					if (type == null) {
						type = SHACL.PROPERTY_SHAPE;
					} else if (!type.equals(SHACL.PROPERTY_SHAPE)) {
						throw new IllegalStateException("Shape " + id
								+ " has sh:path and must be of type sh:PropertyShape but is type " + type);
					}
					try {
						path = (Resource) object;
					} catch (ClassCastException e) {
						throw getExceptionForCastIssue(id, predicate, Resource.class, object);
					}
					break;
				case "http://www.w3.org/ns/shacl#in":
					if (in != null) {
						throw getExceptionForAlreadyPopulated(id, predicate, in, object);
					}
					try {
						in = (Resource) object;
					} catch (ClassCastException e) {
						throw getExceptionForCastIssue(id, predicate, Resource.class, object);
					}
					break;
				case "http://www.w3.org/ns/shacl#equals":
					try {
						equals.add((IRI) object);
					} catch (ClassCastException e) {
						throw getExceptionForCastIssue(id, predicate, IRI.class, object);
					}
					break;
				case "http://www.w3.org/ns/shacl#disjoint":
					try {
						disjoint.add((IRI) object);
					} catch (ClassCastException e) {
						throw getExceptionForCastIssue(id, predicate, IRI.class, object);
					}
					break;
				case "http://www.w3.org/ns/shacl#lessThan":
					try {
						lessThan.add((IRI) object);
					} catch (ClassCastException e) {
						throw getExceptionForCastIssue(id, predicate, IRI.class, object);
					}
					break;
				case "http://www.w3.org/ns/shacl#lessThanOrEquals":
					try {
						lessThanOrEquals.add((IRI) object);
					} catch (ClassCastException e) {
						throw getExceptionForCastIssue(id, predicate, IRI.class, object);
					}
					break;
				case "http://www.w3.org/ns/shacl#target":
					try {
						target.add((Resource) object);
					} catch (ClassCastException e) {
						throw getExceptionForCastIssue(id, predicate, Resource.class, object);
					}
					break;
				case "http://www.w3.org/ns/shacl#hasValue":
					hasValue.add(object);
					break;
				case "http://www.w3.org/ns/shacl#qualifiedValueShape":
					if (qualifiedValueShape != null) {
						throw getExceptionForAlreadyPopulated(id, predicate, qualifiedValueShape, object);
					}
					try {
						qualifiedValueShape = ((Resource) object);
					} catch (ClassCastException e) {
						throw getExceptionForCastIssue(id, predicate, Resource.class, object);
					}
					break;
				case "http://www.w3.org/ns/shacl#qualifiedValueShapesDisjoint":
					if (qualifiedValueShapesDisjoint != null) {
						throw getExceptionForAlreadyPopulated(id, predicate, qualifiedValueShapesDisjoint, object);
					}
					try {
						qualifiedValueShapesDisjoint = ((Literal) object).booleanValue();
					} catch (ClassCastException e) {
						throw getExceptionForCastIssue(id, predicate, Literal.class, object);
					} catch (IllegalArgumentException e) {
						throw getExceptionForLiteralFormatIssue(id, predicate, object, Boolean.class);
					}
					break;
				case "http://www.w3.org/ns/shacl#qualifiedMinCount":
					if (qualifiedMinCount != null) {
						throw getExceptionForAlreadyPopulated(id, predicate, qualifiedMinCount, object);
					}
					try {
						qualifiedMinCount = ((Literal) object).longValue();
					} catch (ClassCastException e) {
						throw getExceptionForCastIssue(id, predicate, Literal.class, object);
					} catch (NumberFormatException e) {
						throw getExceptionForLiteralFormatIssue(id, predicate, object, Long.class);
					}
					break;
				case "http://www.w3.org/ns/shacl#qualifiedMaxCount":
					if (qualifiedMaxCount != null) {
						throw getExceptionForAlreadyPopulated(id, predicate, qualifiedMaxCount, object);
					}
					try {
						qualifiedMaxCount = ((Literal) object).longValue();
					} catch (ClassCastException e) {
						throw getExceptionForCastIssue(id, predicate, Literal.class, object);
					} catch (NumberFormatException e) {
						throw getExceptionForLiteralFormatIssue(id, predicate, object, Long.class);
					}
					break;
				case "http://datashapes.org/dash#hasValueIn":
					try {
						hasValueIn.add((Resource) object);
					} catch (ClassCastException e) {
						throw getExceptionForCastIssue(id, predicate, Resource.class, object);
					}
					break;
				case "http://rdf4j.org/shacl-extensions#targetShape":
					try {
						targetShape.add((Resource) object);
					} catch (ClassCastException e) {
						throw getExceptionForCastIssue(id, predicate, Resource.class, object);
					}
					break;
				case "http://www.w3.org/ns/shacl#sparql":
					try {
						sparql.add((Resource) object);
					} catch (ClassCastException e) {
						throw getExceptionForCastIssue(id, predicate, Resource.class, object);
					}
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
			type = path == null ? SHACL.NODE_SHAPE : SHACL.PROPERTY_SHAPE;
		}

	}

	private static ShaclShapeParsingException getExceptionForLiteralFormatIssue(Resource id, String predicate,
			Value object, Class<?> clazz) {
		return new ShaclShapeParsingException("Expected predicate <" + predicate + "> to have a "
				+ clazz.getSimpleName() + " as object but found " + object, id);
	}

	private static ShaclShapeParsingException getExceptionForAlreadyPopulated(Resource id, String predicate,
			Object existingObject, Value secondValue) {
		Value existingValue;

		if (existingObject instanceof Value) {
			existingValue = (Value) existingObject;
		} else if (existingObject instanceof String) {
			existingValue = Values.literal(existingObject);
		} else if (existingObject instanceof Boolean) {
			existingValue = Values.literal(existingObject);
		} else {
			return new ShaclShapeParsingException("Expected predicate <" + predicate
					+ "> to have no more than 1 object, found " + existingObject + " and " + secondValue, id);
		}

		return new ShaclShapeParsingException("Expected predicate <" + predicate
				+ "> to have no more than 1 object, found " + existingValue + " and " + secondValue, id);
	}

	private static ShaclShapeParsingException getExceptionForCastIssue(Resource id, String predicate,
			Class<?> expectedClass, Value object) {
		String expectedClassString;
		if (expectedClass == IRI.class) {
			expectedClassString = "an IRI";
		} else {
			expectedClassString = "a " + expectedClass.getSimpleName();
		}

		return new ShaclShapeParsingException("Expected predicate <" + predicate + "> to have " + expectedClassString
				+ " as object, but found " + getClassName(object) + " for " + object, id);
	}

	private static String getClassName(Value object) {
		if (object == null) {
			return "null";
		}
		String actualClassName;
		if (object.isIRI()) {
			actualClassName = "IRI";
		} else if (object.isLiteral()) {
			actualClassName = "Literal";
		} else if (object.isBNode()) {
			actualClassName = "BNode";
		} else if (object.isTriple()) {
			actualClassName = "Triple";
		} else {
			assert false;
			actualClassName = object.getClass().getSimpleName();
		}
		return actualClassName;
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

	public String getPattern() {
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
		return deactivated != null && deactivated;
	}

	public boolean isUniqueLang() {
		return uniqueLang != null && uniqueLang;
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

	public IRI getSeverity() {
		return severity;
	}

	public List<Literal> getName() {
		return name;
	}

	public List<Literal> getDescription() {
		return description;
	}

	public Value getDefaultValue() {
		return defaultValue;
	}

	public Value getOrder() {
		return order;
	}

	public Value getGroup() {
		return group;
	}

	public List<Resource> getProperty() {
		return property;
	}

	public List<Resource> getNode() {
		return node;
	}

	public boolean isClosed() {
		return closed != null && closed;
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

	public boolean getQualifiedValueShapesDisjoint() {
		return qualifiedValueShapesDisjoint != null && qualifiedValueShapesDisjoint;
	}

	public List<Resource> getSparql() {
		return sparql;
	}
}
