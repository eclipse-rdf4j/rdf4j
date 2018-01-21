package org.eclipse.rdf4j.spanqit.constraint;

import org.eclipse.rdf4j.spanqit.rdf.Iri;

public class CustomFunction extends Expression<CustomFunction> {
    CustomFunction(Iri functionIri) {
        super(functionIri, ", ");
        parenthesize();
        setOperatorName(operator.getQueryString(), false);
    }
}
