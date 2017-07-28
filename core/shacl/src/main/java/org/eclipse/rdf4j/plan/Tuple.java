package org.eclipse.rdf4j.plan;

import org.eclipse.rdf4j.model.Value;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by heshanjayasinghe on 7/17/17.
 */
public class Tuple {
    public List<Value> line = new ArrayList<>();

    public List<Value> getlist (){
        return line;
    }

    @Override
    public String toString() {
        return "Tuple{" +
                "line=" + Arrays.toString(line.toArray()) +
                '}';
    }
}
