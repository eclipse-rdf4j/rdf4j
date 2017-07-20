package org.eclipse.rdf4j.example;

import org.eclipse.rdf4j.plan.PlanNode;
import org.eclipse.rdf4j.plan.Tuple;

import java.util.Iterator;
/**
 * Created by heshanjayasinghe on 7/10/17.
 */

public class Select implements PlanNode {
    DataSource dataSource;

    public Select(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Iterator<Tuple> iterator() {

        return new Iterator<Tuple>() {

            int counter = 0;

            @Override
            public boolean hasNext() {
                return dataSource.strings.length>counter;
            }

            @Override
            public Tuple next() {

                String string = dataSource.strings[counter];
                counter++;
                Tuple tuple = new Tuple();
                tuple.line.add(string);
                return tuple;
            }
        };
    }


    @Override
    public boolean validate() {
        return false;
    }
}