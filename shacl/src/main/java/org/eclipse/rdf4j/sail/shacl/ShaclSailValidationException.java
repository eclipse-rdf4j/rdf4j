package org.eclipse.rdf4j.sail.shacl;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.LinkedHashModelFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.shacl.AST.PathPropertyShape;
import org.eclipse.rdf4j.sail.shacl.AST.PropertyShape;
import org.eclipse.rdf4j.sail.shacl.AST.SimplePath;
import org.eclipse.rdf4j.sail.shacl.planNodes.Tuple;

import java.util.List;

public class ShaclSailValidationException extends SailException {

	List<Tuple> invalidTuples;

	public ShaclSailValidationException(List<Tuple> invalidTuples) {
		super("Failed SHACL validation");
		this.invalidTuples = invalidTuples;
	}

	public Model validationReportAsModel() {

		SimpleValueFactory vf = SimpleValueFactory.getInstance();

		LinkedHashModel ret = new LinkedHashModelFactory().createEmptyModel();

		ret.setNamespace(SHACL.PREFIX, SHACL.NAMESPACE);

		ModelBuilder validationReport = new ModelBuilder().subject(vf.createBNode())
			.add(RDF.TYPE, SHACL.VALIDATION_REPORT)
			.add(SHACL.CONFORMS, false);


		for (Tuple invalidTuple : invalidTuples) {

			BNode resultSubject = vf.createBNode();
			ModelBuilder result = new ModelBuilder().subject(resultSubject)
				.add(RDF.TYPE, SHACL.VALIDATION_RESULT);


			for (PropertyShape causedByPropertyShape : invalidTuple.getCausedByPropertyShapes()) {
				result.add(SHACL.SOURCE_CONSTRAINT_COMPONENT, causedByPropertyShape.getSourceConstraintComponent());
				result.add(SHACL.SOURCE_SHAPE, causedByPropertyShape.getId());
				if (causedByPropertyShape instanceof PathPropertyShape) {
					PathPropertyShape causeByPathPropertyShape = (PathPropertyShape) causedByPropertyShape;
					if (causeByPathPropertyShape.getPath() != null) {
						result.add(SHACL.RESULT_PATH, ((SimplePath) causeByPathPropertyShape.getPath()).getPath());
					}
				}
			}

			if (invalidTuple.line.size() > 0) {
				result.add(SHACL.FOCUS_NODE, invalidTuple.line.get(0));
			}

			validationReport.add(SHACL.RESULT, resultSubject);

			ret.addAll(result.build());

		}
		ret.addAll(validationReport.build());

		return ret;

	}
}
