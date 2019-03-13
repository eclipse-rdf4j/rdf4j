package org.eclipse.rdf4j.sail.shacl.AST;

import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class ShaclProperties {


	Resource clazz;
	Resource or;
	Long minCount;
	Long maxCount;

	Resource datatype;

	Long minLength;
	Long maxLength;

	Resource languageIn;
	Resource nodeKind;

	Literal minExclusive;
	Literal maxExclusive;
	Literal minInclusive;
	Literal maxInclusive;

	String pattern;

	Resource targetClass;
	List<Value> targetNode = new ArrayList<>(0);


	public ShaclProperties(Resource propertyShapeId, SailRepositoryConnection connection) {

		try (Stream<Statement> stream = Iterations.stream(connection.getStatements(propertyShapeId, null, null))) {
			stream.forEach(statement -> {
				String predicate = statement.getPredicate().toString();
				Value object = statement.getObject();
				switch (predicate) {
					case "http://www.w3.org/ns/shacl#or":
						if (or != null) {
							throw new IllegalStateException("sh:or already populated");
						}
						or = (Resource) object;
						break;
					case "http://www.w3.org/ns/shacl#languageIn":
						if (languageIn != null) {
							throw new IllegalStateException("sh:languageIn already populated");
						}
						languageIn = (Resource) object;
						break;
					case "http://www.w3.org/ns/shacl#nodeKind":
						if (nodeKind != null) {
							throw new IllegalStateException("sh:nodeKind already populated");
						}
						nodeKind = (Resource) object;
						break;
					case "http://www.w3.org/ns/shacl#datatype":
						if (datatype != null) {
							throw new IllegalStateException("sh:datatype already populated");
						}
						datatype = (Resource) object;
						break;
					case "http://www.w3.org/ns/shacl#minCount":
						if (minCount != null) {
							throw new IllegalStateException("sh:minCount aleady populated");
						}
						minCount = ((Literal) object).longValue();
						break;
					case "http://www.w3.org/ns/shacl#maxCount":
						if (maxCount != null) {
							throw new IllegalStateException("sh:maxCount aleady populated");
						}
						maxCount = ((Literal) object).longValue();
						break;
					case "http://www.w3.org/ns/shacl#minLength":
						if (minLength != null) {
							throw new IllegalStateException("sh:minLength aleady populated");
						}
						minLength = ((Literal) object).longValue();
						break;
					case "http://www.w3.org/ns/shacl#maxLength":
						if (maxLength != null) {
							throw new IllegalStateException("sh:maxLength aleady populated");
						}
						maxLength = ((Literal) object).longValue();
						break;
					case "http://www.w3.org/ns/shacl#minExclusive":
						if (minExclusive != null) {
							throw new IllegalStateException("sh:minExclusive aleady populated");
						}
						minExclusive = (Literal) object;
						break;
					case "http://www.w3.org/ns/shacl#maxExclusive":
						if (maxExclusive != null) {
							throw new IllegalStateException("sh:maxExclusive aleady populated");
						}
						maxExclusive = (Literal) object;
						break;
					case "http://www.w3.org/ns/shacl#minInclusive":
						if (minInclusive != null) {
							throw new IllegalStateException("sh:minInclusive aleady populated");
						}
						minInclusive = (Literal) object;
						break;
					case "http://www.w3.org/ns/shacl#maxInclusive":
						if (maxInclusive != null) {
							throw new IllegalStateException("sh:maxInclusive aleady populated");
						}
						maxInclusive = (Literal) object;
						break;
					case "http://www.w3.org/ns/shacl#pattern":
						if (pattern != null) {
							throw new IllegalStateException("sh:pattern aleady populated");
						}
						pattern = object.stringValue();
						break;
					case "http://www.w3.org/ns/shacl#class":
						if (clazz != null) {
							throw new IllegalStateException("sh:class aleady populated");
						}
						clazz = (Resource) object;
						break;
					case "http://www.w3.org/ns/shacl#targetNode":
						targetNode.add(object);
						break;
					case "http://www.w3.org/ns/shacl#targetClass":
						if (targetClass != null) {
							throw new IllegalStateException("sh:targetClass aleady populated");
						}
						targetClass = (Resource) object;
						break;
				}


			});
		}

	}


}
