package org.eclipse.rdf4j.plan;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by heshanjayasinghe on 7/17/17.
 */
public class Tuple {
    public List<PlanNode> line = new ArrayList<>();


    @Override
    public String toString() {
        return "Tuple{" +
                "line=" + Arrays.toString(line.toArray()) +
                '}';
    }
}
