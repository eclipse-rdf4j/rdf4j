/** *****************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 ****************************************************************************** */
package org.eclipse.rdf4j.query.algebra.evaluation.iterator;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.ConvertingIteration;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.MultiProjection;
import org.eclipse.rdf4j.query.algebra.Projection;
import org.eclipse.rdf4j.query.algebra.ProjectionElem;
import org.eclipse.rdf4j.query.algebra.ProjectionElemList;
import org.eclipse.rdf4j.query.algebra.QueryModelNode;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryBindingSet;
import org.eclipse.rdf4j.query.impl.ArrayBindingSet;

public class ProjectionIterator extends ConvertingIteration<BindingSet, BindingSet, QueryEvaluationException> {

    /*-----------*
	 * Constants *
	 *-----------*/
    private final Function<BindingSet, ? extends BindingSet> converter;

    /*--------------*
	 * Constructors *
	 *--------------*/
    public ProjectionIterator(Projection projection, CloseableIteration<BindingSet, QueryEvaluationException> iter,
            BindingSet parentBindings) throws QueryEvaluationException {
        super(iter);
        ProjectionElemList pel = projection.getProjectionElemList();
        if (parentBindings.size() == 0) {
            if (pel.getElements().size() == 1) {
                Function<BindingSet, ArrayBindingSet> oneVariableConversion = convertASingleVariable(pel);
                this.converter = oneVariableConversion;
            } else {
                Function<BindingSet, ArrayBindingSet> manyVariableConversion = convertManyVariables(pel);
                this.converter = manyVariableConversion;
            }
        } else {
            this.converter = (s) -> project(projection.getProjectionElemList(), s, parentBindings, !determineOuterProjection(projection));
        }
    }

    Function<BindingSet, ArrayBindingSet> convertASingleVariable(ProjectionElemList pel) {
        ProjectionElem el = pel
                .getElements().get(0);
        String targetName = el.getTargetName();
        ArrayBindingSet abs = new ArrayBindingSet(targetName);
        String sourceName = el.getSourceName();
        BiConsumer<ArrayBindingSet, Value> setter = getSetterToTarget(abs, targetName);
        Function<BindingSet, ArrayBindingSet> oneVariableConversion = sb -> {
            ArrayBindingSet abs2 = new ArrayBindingSet(targetName);
            Value targetValue = sb.getValue(sourceName);
            if (targetValue != null) {
                setter.accept(abs2, targetValue);
            }
            return abs2;
        };
        return oneVariableConversion;
    }

    BiConsumer<ArrayBindingSet, Value> getSetterToTarget(ArrayBindingSet abs, String targetName) {
        BiConsumer<ArrayBindingSet, Value> setter = abs.getDirectSetterForVariable(targetName);
        return setter;
    }

    private static boolean determineOuterProjection(Projection projection) {
        QueryModelNode ancestor = projection;
        while (ancestor.getParentNode() != null) {
            ancestor = ancestor.getParentNode();
            if (ancestor instanceof Projection || ancestor instanceof MultiProjection) {
                return false;
            }
        }
        return true;
    }

    /*---------*
	 * Methods *
	 *---------*/
    @Override
    protected BindingSet convert(BindingSet sourceBindings) throws QueryEvaluationException {
        return converter.apply(sourceBindings);
    }

    public static BindingSet project(ProjectionElemList projElemList, BindingSet sourceBindings,
            BindingSet parentBindings) {
        return project(projElemList, sourceBindings, parentBindings, false);
    }

    public static BindingSet project(ProjectionElemList projElemList, BindingSet sourceBindings,
            BindingSet parentBindings, boolean includeAllParentBindings) {
        final QueryBindingSet resultBindings = new QueryBindingSet();
        if (includeAllParentBindings) {
            resultBindings.addAll(parentBindings);
        }

        for (ProjectionElem pe : projElemList.getElements()) {
            Value targetValue = sourceBindings.getValue(pe.getSourceName());
            if (!includeAllParentBindings && targetValue == null) {
                targetValue = parentBindings.getValue(pe.getSourceName());
            }
            if (targetValue != null) {
                resultBindings.setBinding(pe.getTargetName(), targetValue);
            }
        }

        return resultBindings;
    }

    private Function<BindingSet, ArrayBindingSet> convertManyVariables(ProjectionElemList pel) {
        String[] targetNames = pel.getTargetNames().toArray(new String[0]);
        ArrayBindingSet abs = new ArrayBindingSet(targetNames);
        int size = pel.getElements().size();
        Map<String, BiConsumer<ArrayBindingSet, Value>> setters = new HashMap<>(size);
        for (ProjectionElem el : pel.getElements()) {
            String targetName = el.getTargetName();

            String sourceName = el.getSourceName();
            BiConsumer<ArrayBindingSet, Value> setter = getSetterToTarget(abs, targetName);
            setters.put(sourceName, setter);
        }
        Set<Map.Entry<String, BiConsumer<ArrayBindingSet, Value>>> entrySet = setters.entrySet();
        return (sb) -> {
            ArrayBindingSet abs2 = new ArrayBindingSet(targetNames);
            for (Map.Entry<String, BiConsumer<ArrayBindingSet, Value>> entry:entrySet) {
                Value targetValue = sb.getValue(entry.getKey());
                if (targetValue != null) {
                    entry.getValue().accept(abs2, targetValue);
                }
            }
            return abs2;
        };
    }
}
