/*******************************************************************************
 * Copyright (c) 2016 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/


package org.eclipse.rdf4j.benchmark;


import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.inferencer.fc.FastRdfsForwardChainingSail;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * @author Håvard Mikkelsen Ottestad
 */

@State(Scope.Thread)
abstract public class RdfsBenchmark {


    abstract SailRepository getSail(SailRepository schema);

    abstract Class getSailClass();
//
//    @Benchmark
//    @BenchmarkMode(Mode.AverageTime)
//    @OutputTimeUnit(TimeUnit.MILLISECONDS)
//    public void initialize() {
//
//        getSail(null).initialize();
//    }
//
//
//    @Benchmark
//    @BenchmarkMode(Mode.AverageTime)
//    @OutputTimeUnit(TimeUnit.MILLISECONDS)
//    public void simple() throws IOException {
//        SailRepository sail = getSail(null);
//        sail.initialize();
//
//        try (SailRepositoryConnection connection = sail.getConnection()) {
//            connection.begin();
//
//            connection.add(RdfsBenchmark.class.getClassLoader().getResourceAsStream("simple/mixed.ttl"), "", RDFFormat.TURTLE);
//
//
//            connection.commit();
//        }
//    }
//
//    @Benchmark
//    @BenchmarkMode(Mode.AverageTime)
//    @OutputTimeUnit(TimeUnit.MILLISECONDS)
//    public void medium() throws IOException {
//        SailRepository sail = getSail(null);
//        sail.initialize();
//
//        try (SailRepositoryConnection connection = sail.getConnection()) {
//            connection.begin();
//
//            connection.add(RdfsBenchmark.class.getClassLoader().getResourceAsStream("medium/mixed.ttl"), "", RDFFormat.TURTLE);
//
//
//            connection.commit();
//        }
//    }
//
//    @Benchmark
//    @BenchmarkMode(Mode.AverageTime)
//    @OutputTimeUnit(TimeUnit.MILLISECONDS)
//    public void moreRdfs() throws IOException {
//        SailRepository sail = getSail(null);
//        sail.initialize();
//
//        try (SailRepositoryConnection connection = sail.getConnection()) {
//            connection.begin();
//
//            connection.add(RdfsBenchmark.class.getClassLoader().getResourceAsStream("moreRdfs/mixed.ttl"), "", RDFFormat.TURTLE);
//
//
//            connection.commit();
//        }
//    }
//
//    @Benchmark
//    @BenchmarkMode(Mode.AverageTime)
//    @OutputTimeUnit(TimeUnit.MILLISECONDS)
//    public void moreRdfsLoop() throws IOException {
//        SailRepository sail = getSail(null);
//        sail.initialize();
//
//
//        try (SailRepositoryConnection connection = sail.getConnection()) {
//            connection.begin();
//            connection.add(RdfsBenchmark.class.getClassLoader().getResourceAsStream("moreRdfs/mixed.ttl"), "", RDFFormat.TURTLE);
//            connection.commit();
//        }
//
//
//        for (int i = 0; i < 10; i++) {
//            try (SailRepositoryConnection connection = sail.getConnection()) {
//                connection.begin();
//                connection.add(RdfsBenchmark.class.getClassLoader().getResourceAsStream("moreRdfs/data" + i + ".ttl"), "", RDFFormat.TURTLE);
//                connection.commit();
//            }
//        }
//
//
//    }
//
//    @Benchmark
//    @BenchmarkMode(Mode.AverageTime)
//    @OutputTimeUnit(TimeUnit.MILLISECONDS)
//    public void moreRdfsLoopTwoTransactions() throws IOException {
//        SailRepository sail = getSail(null);
//        sail.initialize();
//
//
//        try (SailRepositoryConnection connection = sail.getConnection()) {
//            connection.begin();
//            connection.add(RdfsBenchmark.class.getClassLoader().getResourceAsStream("moreRdfs/mixed.ttl"), "", RDFFormat.TURTLE);
//            connection.commit();
//        }
//
//        try (SailRepositoryConnection connection = sail.getConnection()) {
//            connection.begin();
//            for (int i = 0; i < 10; i++) {
//                connection.add(RdfsBenchmark.class.getClassLoader().getResourceAsStream("moreRdfs/data" + i + ".ttl"), "", RDFFormat.TURTLE);
//            }
//            connection.commit();
//        }
//
//
//    }
//
//    @Benchmark
//    @BenchmarkMode(Mode.AverageTime)
//    @OutputTimeUnit(TimeUnit.MILLISECONDS)
//    public void moreRdfsLoopSchema() throws IOException {
//        SailRepository schema = null;
//        if (getSailClass() == FastRdfsForwardChainingSail.class) {
//            schema = new SailRepository(new MemoryStore());
//            schema.initialize();
//            try (SailRepositoryConnection schemaConnection = schema.getConnection()) {
//                schemaConnection.begin();
//
//                schemaConnection.add(RdfsBenchmark.class.getClassLoader().getResourceAsStream("moreRdfs/schema.ttl"), "", RDFFormat.TURTLE);
//
//
//                schemaConnection.commit();
//            }
//        }
//
//
//        SailRepository sail = getSail(schema);
//        sail.initialize();
//        if (getSailClass() != FastRdfsForwardChainingSail.class) {
//
//            try (SailRepositoryConnection connection = sail.getConnection()) {
//                connection.begin();
//                connection.add(RdfsBenchmark.class.getClassLoader().getResourceAsStream("moreRdfs/schema.ttl"), "", RDFFormat.TURTLE);
//                connection.commit();
//            }
//        }
//        try (SailRepositoryConnection connection = sail.getConnection()) {
//            connection.begin();
//            connection.add(RdfsBenchmark.class.getClassLoader().getResourceAsStream("moreRdfs/schema.ttl"), "", RDFFormat.TURTLE);
//            connection.commit();
//        }
//
//        for (int i = 0; i < 10; i++) {
//            try (SailRepositoryConnection connection = sail.getConnection()) {
//                connection.begin();
//                connection.add(RdfsBenchmark.class.getClassLoader().getResourceAsStream("moreRdfs/data" + i + ".ttl"), "", RDFFormat.TURTLE);
//                connection.commit();
//            }
//        }
//
//
//    }
//
//    @Benchmark
//    @BenchmarkMode(Mode.AverageTime)
//    @OutputTimeUnit(TimeUnit.MILLISECONDS)
//    public void moreRdfsLoopTwoTransactionsSchema() throws IOException {
//        SailRepository schema = null;
//        if (getSailClass() == FastRdfsForwardChainingSail.class) {
//            schema = new SailRepository(new MemoryStore());
//            schema.initialize();
//            try (SailRepositoryConnection schemaConnection = schema.getConnection()) {
//                schemaConnection.begin();
//
//                schemaConnection.add(RdfsBenchmark.class.getClassLoader().getResourceAsStream("moreRdfs/schema.ttl"), "", RDFFormat.TURTLE);
//
//
//                schemaConnection.commit();
//            }
//        }
//
//
//        SailRepository sail = getSail(schema);
//        sail.initialize();
//        if (getSailClass() != FastRdfsForwardChainingSail.class) {
//
//            try (SailRepositoryConnection connection = sail.getConnection()) {
//                connection.begin();
//                connection.add(RdfsBenchmark.class.getClassLoader().getResourceAsStream("moreRdfs/schema.ttl"), "", RDFFormat.TURTLE);
//                connection.commit();
//            }
//        }
//        try (SailRepositoryConnection connection = sail.getConnection()) {
//            connection.begin();
//            connection.add(RdfsBenchmark.class.getClassLoader().getResourceAsStream("moreRdfs/schema.ttl"), "", RDFFormat.TURTLE);
//            connection.commit();
//        }
//        try (SailRepositoryConnection connection = sail.getConnection()) {
//            connection.begin();
//            for (int i = 0; i < 10; i++) {
//
//                connection.add(RdfsBenchmark.class.getClassLoader().getResourceAsStream("moreRdfs/data" + i + ".ttl"), "", RDFFormat.TURTLE);
//
//            }
//            connection.commit();
//        }
//
//
//    }
//
//
//    @Benchmark
//    @BenchmarkMode(Mode.AverageTime)
//    @OutputTimeUnit(TimeUnit.MILLISECONDS)
//    public void longSubClassOfChain() throws IOException {
//        SailRepository sail = getSail(null);
//        sail.initialize();
//
//        try (SailRepositoryConnection connection = sail.getConnection()) {
//            connection.begin();
//
//            connection.add(RdfsBenchmark.class.getClassLoader().getResourceAsStream("longChain/mixed.ttl"), "", RDFFormat.TURTLE);
//
//
//            connection.commit();
//        }
//    }
//
//    @Benchmark
//    @BenchmarkMode(Mode.AverageTime)
//    @OutputTimeUnit(TimeUnit.MILLISECONDS)
//    public void longSubClassOfChainSchema() throws IOException {
//        SailRepository schema = null;
//        if (getSailClass() == FastRdfsForwardChainingSail.class) {
//            schema = new SailRepository(new MemoryStore());
//            schema.initialize();
//            try (SailRepositoryConnection schemaConnection = schema.getConnection()) {
//                schemaConnection.begin();
//
//                schemaConnection.add(RdfsBenchmark.class.getClassLoader().getResourceAsStream("longChain/schema.ttl"), "", RDFFormat.TURTLE);
//
//
//                schemaConnection.commit();
//            }
//        }
//
//        SailRepository sail = getSail(schema);
//        sail.initialize();
//
//        try (SailRepositoryConnection connection = sail.getConnection()) {
//            connection.begin();
//            if (getSailClass() != FastRdfsForwardChainingSail.class) {
//                connection.add(RdfsBenchmark.class.getClassLoader().getResourceAsStream("longChain/schema.ttl"), "", RDFFormat.TURTLE);
//            }
//
//            connection.add(RdfsBenchmark.class.getClassLoader().getResourceAsStream("longChain/data.ttl"), "", RDFFormat.TURTLE);
//
//            connection.commit();
//        }
//
//        try (SailRepositoryConnection connection = sail.getConnection()) {
//            connection.begin();
//            long count = Iterations.stream(connection.getStatements(null, null, null, true)).count();
//            System.out.println("COUNT: " + count);
//            connection.commit();
//        }
//    }

}
