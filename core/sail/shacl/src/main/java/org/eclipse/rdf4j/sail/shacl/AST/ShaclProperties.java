package org.eclipse.rdf4j.sail.shacl.AST;

import org.eclipse.rdf4j.common.iteration.Iterations;
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

public class ShaclProperties {

	private static final Logger logger = LoggerFactory.getLogger(ShaclProperties.class);

	List<Resource> clazz = new ArrayList<>(0);
	List<Resource> or = new ArrayList<>(0);
	List<Resource> and = new ArrayList<>(0);
	List<Resource> not = new ArrayList<>(0);
	Long minCount;
	Long maxCount;

	Resource datatype;
	Resource in;

	Long minLength;
	Long maxLength;

	Resource languageIn;
	Resource nodeKind;

	Resource path;

	Literal minExclusive;
	Literal maxExclusive;
	Literal minInclusive;
	Literal maxInclusive;

	List<String> pattern = new ArrayList<>(0);
	String flags = "";

	Set<Resource> targetClass = new HashSet<>(0);
	TreeSet<Value> targetNode = new TreeSet<>(new ValueComparator());
	Set<IRI> targetSubjectsOf = new HashSet<>(0);
	Set<IRI> targetObjectsOf = new HashSet<>(0);

	boolean deactivated = false;

	boolean uniqueLang = false;

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

}
