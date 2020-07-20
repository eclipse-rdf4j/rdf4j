package org.eclipse.rdf4j.sail.shacl.AST;

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
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShaclProperties {

	private static final Logger logger = LoggerFactory.getLogger(ShaclProperties.class);

	private final List<Resource> clazz = new ArrayList<>();
	private final List<Resource> or = new ArrayList<>();
	private final List<Resource> and = new ArrayList<>();
	private final List<Resource> not = new ArrayList<>();
	private Long minCount;
	private Long maxCount;

	private Resource datatype;
	private Resource in;
	private Value hasValue;
	private Resource hasValueIn;

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
	private String flags = "";

	private final Set<Resource> targetClass = new HashSet<>();
	private final TreeSet<Value> targetNode = new TreeSet<>(new ValueComparator());
	private final Set<IRI> targetSubjectsOf = new HashSet<>();
	private final Set<IRI> targetObjectsOf = new HashSet<>();
	private final List<Resource> targetShape = new ArrayList<>();

	private final List<Resource> target = new ArrayList<>();

	private boolean deactivated = false;

	private boolean uniqueLang = false;

	public ShaclProperties() {
	}

	public ShaclProperties(Resource propertyShapeId, SailRepositoryConnection connection) {

		try (Stream<Statement> stream = connection.getStatements(propertyShapeId, null, null).stream()) {
			stream.forEach(statement -> {
				String predicate = statement.getPredicate().toString();
				Value object = statement.getObject();
				switch (predicate) {
				case "http://www.w3.org/ns/shacl#or":
					or.add((Resource) object);
					break;
				case "http://www.w3.org/ns/shacl#and":
					and.add((Resource) object);
					break;
				case "http://www.w3.org/ns/shacl#not":
					not.add((Resource) object);
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
					datatype = (Resource) object;
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
					clazz.add((Resource) object);
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
				case "http://www.w3.org/ns/shacl#flags":
					flags += object.stringValue();
					break;
				case "http://www.w3.org/ns/shacl#path":
					if (path != null) {
						throw new IllegalStateException(predicate + " already populated");
					}
					path = (Resource) object;
					break;
				case "http://www.w3.org/ns/shacl#in":
					if (in != null) {
						throw new IllegalStateException(predicate + " already populated");
					}
					in = (Resource) object;
					break;
				case "http://www.w3.org/ns/shacl#property":
					break;
				case "http://www.w3.org/ns/shacl#target":
					target.add((Resource) object);
					break;
				case "http://www.w3.org/ns/shacl#hasValue":
					if (hasValue != null) {
						throw new IllegalStateException(predicate + " already populated");
					}
					hasValue = object;
					break;
				case "http://datashapes.org/dash#hasValueIn":
					if (hasValueIn != null) {
						throw new IllegalStateException(predicate + " already populated");
					}
					hasValueIn = (Resource) object;
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

	}

	public List<Resource> getClazz() {
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

	public Resource getDatatype() {
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

	public Value getHasValue() {
		return hasValue;
	}

	public List<Resource> getTarget() {
		return target;
	}

	public List<Resource> getTargetShape() {
		return targetShape;
	}

	public Resource getHasValueIn() {
		return hasValueIn;
	}
}
