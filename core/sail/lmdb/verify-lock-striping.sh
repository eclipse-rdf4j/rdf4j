#!/bin/bash
# Verification script for lock striping implementation

set -e

echo "==============================================="
echo "Lock Striping Implementation Verification"
echo "==============================================="
echo ""

# Change to LMDB module directory
cd "$(dirname "$0")"

echo "1. Compiling LMDB Sail module..."
mvn clean compile -DskipTests -q
if [ $? -eq 0 ]; then
    echo "   ✓ Compilation successful"
else
    echo "   ✗ Compilation failed"
    exit 1
fi

echo ""
echo "2. Compiling tests..."
mvn test-compile -DskipTests -q
if [ $? -eq 0 ]; then
    echo "   ✓ Test compilation successful"
else
    echo "   ✗ Test compilation failed"
    exit 1
fi

echo ""
echo "3. Running concurrency test..."
mvn test -Dtest=LockStripingConcurrencyTest -q
if [ $? -eq 0 ]; then
    echo "   ✓ Concurrency tests passed"
else
    echo "   ✗ Concurrency tests failed"
    exit 1
fi

echo ""
echo "4. Checking for lock striping constants..."
if grep -q "LOCK_STRIPE_COUNT = 16" src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbSailStore.java; then
    echo "   ✓ Lock stripe count configured (16 stripes)"
else
    echo "   ✗ Lock stripe constant not found"
    exit 1
fi

echo ""
echo "5. Verifying helper methods exist..."
if grep -q "private int getLockStripe" src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbSailStore.java && \
   grep -q "private ReentrantLock acquireWriteLock" src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbSailStore.java; then
    echo "   ✓ Helper methods implemented"
else
    echo "   ✗ Helper methods not found"
    exit 1
fi

echo ""
echo "6. Verifying addStatement uses lock striping..."
if grep -A 5 "private void addStatement" src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbSailStore.java | grep -q "acquireWriteLock"; then
    echo "   ✓ addStatement() uses lock striping"
else
    echo "   ✗ addStatement() not using lock striping"
    exit 1
fi

echo ""
echo "7. Verifying approveAll uses ordered lock acquisition..."
if grep -A 50 "public void approveAll" src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbSailStore.java | grep -q "statementsByStripe"; then
    echo "   ✓ approveAll() uses ordered multi-lock acquisition"
else
    echo "   ✗ approveAll() not properly updated"
    exit 1
fi

echo ""
echo "8. Verifying documentation exists..."
if [ -f "LOCK_STRIPING_OPTIMIZATION.md" ]; then
    echo "   ✓ Documentation file found"
else
    echo "   ✗ Documentation missing"
    exit 1
fi

echo ""
echo "==============================================="
echo "All verification checks passed! ✓"
echo "==============================================="
echo ""
echo "Lock striping implementation is ready."
echo ""
echo "Next steps:"
echo "  1. Run full test suite: mvn test"
echo "  2. Run benchmarks: mvn test -Dtest=FullIndexBenchmark"
echo "  3. Review performance improvements"
echo ""
