/*******************************************************************************
 * Copyright (c) 2016 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.inferencer.fc;

import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.sail.NotifyingSail;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.inferencer.InferencerConnection;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.StampedLock;
import java.util.stream.Collectors;

/**
 * @author Håvard Mikkelsen Ottestad
 */

public class FastRdfsForwardChainingSail extends AbstractForwardChainingInferencer {

    NotifyingSail data;
    Repository schema;

    static final Random random = new Random();

    private StampedLock readWriteLock = new StampedLock();

    boolean useAllRdfsRules = true;

    List<Statement> subClassOfStatements = new ArrayList<>();
    Set<Resource> properties = new HashSet<>();
    List<Statement> subPropertyOfStatements = new ArrayList<>();
    List<Statement> rangeStatements = new ArrayList<>();
    List<Statement> domainStatements = new ArrayList<>();


    Map<Resource, Set<Resource>> calculatedTypes = new HashMap<>();
    Map<Resource, Set<Resource>> calculatedProperties = new HashMap<>();
    Map<Resource, Set<Resource>> calculatedRange = new HashMap<>();
    Map<Resource, Set<Resource>> calculatedDomain = new HashMap<>();
    private boolean sharedSchema;

    void clearInferenceTables() {
        subClassOfStatements = new ArrayList<>();
        properties = new HashSet<>();
        subPropertyOfStatements = new ArrayList<>();
        rangeStatements = new ArrayList<>();
        domainStatements = new ArrayList<>();
        calculatedTypes = new HashMap<>();
        calculatedProperties = new HashMap<>();
        calculatedRange = new HashMap<>();
        calculatedDomain = new HashMap<>();
    }

    void readLock(FastRdfsForwardChainingSailConnection connection) {

        if (numberOfThreadsWaitingForWriteLock.get() > 0) {
            System.err.println("starve reads");
        }

        while (numberOfThreadsWaitingForWriteLock.get() > 0) {
            Thread.yield();
        }

        if (connection.lockStamp != 0) {
            throw new IllegalStateException("Connection already has a lock!");
        }
        connection.lockStamp = readWriteLock.readLock();
        System.err.println("readLock: " + connection.lockStamp);

    }

    void releaseLock(FastRdfsForwardChainingSailConnection connection) {
        readWriteLock.unlock(connection.lockStamp);
        System.err.println("Released lock: " + connection.lockStamp);

        connection.lockStamp = 0;
    }


    AtomicInteger numberOfThreadsWaitingForWriteLock = new AtomicInteger(0);

    void upgradeLock(FastRdfsForwardChainingSailConnection connection) {

//        System.err.println("Attempt writelock: "+connection.lockStamp);

        numberOfThreadsWaitingForWriteLock.incrementAndGet();

        try {
           while(true) {
                long l = readWriteLock.tryConvertToWriteLock(connection.lockStamp);

                if (l != 0) {
                    long temp = connection.lockStamp;
                    connection.lockStamp = l;
                    if (temp != l) {
                        System.err.println("readLock: " + temp + " writeLock: " + connection.lockStamp);
                    }


                    return;
                }

                try {
                    Thread.sleep(random.nextInt(2));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                // detect potential deadlock scenario
                if(numberOfThreadsWaitingForWriteLock.get() > 1  //More than 1 connection waiting for a write lock
                    && numberOfThreadsWaitingForWriteLock.get() <= readWriteLock.getReadLockCount() // no connection only wants a read lock
                    && random.nextBoolean()){ // randomly kick this conection out
                    break;
                }

            }

            System.err.println("isReadLocked: " + readWriteLock.isReadLocked());
            System.err.println("isWriteLocked(): " + readWriteLock.isWriteLocked());
            System.err.println("read lock count: " + readWriteLock.getReadLockCount());

            releaseLock(connection);
            throw new IllegalStateException("Could not acquire Tbox write lock");
        } finally {

            numberOfThreadsWaitingForWriteLock.decrementAndGet();
        }

    }


    public FastRdfsForwardChainingSail(NotifyingSail data) {
        super(data);
        schema = null;
        this.data = data;

    }


    public FastRdfsForwardChainingSail(NotifyingSail data, Repository schema) {
        super(data);

        this.data = data;
        this.schema = schema;

    }

    public FastRdfsForwardChainingSail(NotifyingSail data, boolean useAllRdfsRules) {
        super(data);
        schema = null;

        this.data = data;
        this.useAllRdfsRules = useAllRdfsRules;

    }

    public FastRdfsForwardChainingSail(NotifyingSail data, Repository schema, boolean useAllRdfsRules) {
        super(data);

        this.data = data;
        this.schema = schema;
        this.useAllRdfsRules = useAllRdfsRules;

    }


    public void initialize() throws SailException {
        super.initialize();

        if (sharedSchema) {
            return;
        }


        FastRdfsForwardChainingSailConnection connection = null;

        try {
            connection = getConnection();
            final FastRdfsForwardChainingSailConnection finalConnection = connection;
            finalConnection.begin();

            finalConnection.addAxiomStatements();


            List<Statement> tboxStatments = new ArrayList<>();

            if (schema != null) {

                try (RepositoryConnection schemaConnection = schema.getConnection()) {
                    schemaConnection.begin();
                    RepositoryResult<Statement> statements = schemaConnection.getStatements(null, null, null);

                    tboxStatments = Iterations.stream(statements)
                        .peek(finalConnection::statementCollector)
                        .collect(Collectors.toList());
                    schemaConnection.commit();
                }
            }

            finalConnection.calculateInferenceMaps();

            if (schema != null) {
                tboxStatments.forEach(statement -> finalConnection.addStatement(statement.getSubject(), statement.getPredicate(), statement.getObject(), statement.getContext()));
            }

            finalConnection.commit();
        } finally {
            if (connection != null) {
                connection.close();
            }
        }

    }


    public void setDataDir(File file) {
        throw new UnsupportedOperationException();
    }

    public File getDataDir() {
        throw new UnsupportedOperationException();
    }


    public FastRdfsForwardChainingSailConnection getConnection() throws SailException {
        InferencerConnection e = (InferencerConnection) super.getConnection();
        return new FastRdfsForwardChainingSailConnection(this, e);
    }

    public ValueFactory getValueFactory() {

        return SimpleValueFactory.getInstance();
    }

    static public FastRdfsForwardChainingSail fastInstantiateFrom(FastRdfsForwardChainingSail baseSail, NotifyingSail store) {
        return fastInstantiateFrom(baseSail, store, true);
    }

    static public FastRdfsForwardChainingSail fastInstantiateFrom(FastRdfsForwardChainingSail baseSail, NotifyingSail store, boolean useAllRdfsRules) {

        baseSail.getConnection().close();

        FastRdfsForwardChainingSail ret = new FastRdfsForwardChainingSail(store, baseSail.schema, useAllRdfsRules);

        ret.sharedSchema = true;

        baseSail.calculatedTypes.forEach((key, value) -> {
            value.forEach(v -> {
                if (!ret.calculatedTypes.containsKey(key)) {
                    ret.calculatedTypes.put(key, new HashSet<>());
                }
                ret.calculatedTypes.get(key).add(v);
            });
        });

        baseSail.calculatedProperties.forEach((key, value) -> {
            value.forEach(v -> {
                if (!ret.calculatedProperties.containsKey(key)) {
                    ret.calculatedProperties.put(key, new HashSet<>());
                }
                ret.calculatedProperties.get(key).add(v);
            });
        });

        baseSail.calculatedRange.forEach((key, value) -> {
            value.forEach(v -> {
                if (!ret.calculatedRange.containsKey(key)) {
                    ret.calculatedRange.put(key, new HashSet<>());
                }
                ret.calculatedRange.get(key).add(v);
            });
        });

        baseSail.calculatedDomain.forEach((key, value) -> {
            value.forEach(v -> {
                if (!ret.calculatedDomain.containsKey(key)) {
                    ret.calculatedDomain.put(key, new HashSet<>());
                }
                ret.calculatedDomain.get(key).add(v);
            });
        });


        return ret;

    }


}
