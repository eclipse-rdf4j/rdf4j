package org.eclipse.rdf4j.sail.shacl.planNodes;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.sail.shacl.plan.Tuple;

import java.util.ArrayList;

public class TupleHelper {
	public static Tuple join(Tuple leftPeek, Tuple rightPeek) {

		ArrayList<Value> newLine = new ArrayList<>(leftPeek.line.size() + rightPeek.line.size() - 1);


		newLine.addAll(leftPeek.line);

		for(int i = 1; i<rightPeek.line.size(); i++){
			newLine.add(rightPeek.line.get(i));
		}

		return new Tuple(newLine);

	}
}
