package org.eclipse.rdf4j;

import org.eclipse.rdf4j.AST.MinCountPropertyShape;
import org.eclipse.rdf4j.AST.Path;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

/**
 * Created by heshanjayasinghe on 6/20/17.
 */
public class Main2 {
    static ValueFactory vf = SimpleValueFactory.getInstance();

    public static void main(String[] args) {
        MinCountPropertyShape minCountPropertyShape = new MinCountPropertyShape(null,null);
        minCountPropertyShape.minCount = 1;
        Path path = new Path(null,null);
        path.path = vf.createIRI("http://example.org/ssn");
        minCountPropertyShape.path =path;
    }
}
