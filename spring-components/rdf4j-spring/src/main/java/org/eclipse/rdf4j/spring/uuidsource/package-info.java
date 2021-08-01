/**
 * This package contains three different approaches for obtaining UUIDs from a
 * {@link org.eclipse.rdf4j.repository.Repository Repository}, guaranteeing their uniqueness. Due to the <b>very, very,
 * very</b> low probability of a collision, it is recommended not to use any of these and instead rely on the
 * {@link org.eclipse.rdf4j.spring.support.DefaultUUIDSource DefaultUUIDSource}.
 *
 * <ol>
 * <li>{@link org.eclipse.rdf4j.spring.uuidsource.noveltychecking.NoveltyCheckingUUIDSource NoveltyCheckingUUIDSource}:
 * Generate a {@link java.util.UUID UUID} locally using {@link java.util.UUID#randomUUID() UUID.randomUUID()} and then
 * ask the repository if the UUID is unique. Enable with
 * <code>rdf4j.spring.uuidsource.noveltychecking.enabled=true</code>
 * <li>{@link org.eclipse.rdf4j.spring.uuidsource.simple.SimpleRepositoryUUIDSource SimpleRepositoryUUIDSource}: Ask the
 * repository for a new UUID each time one is needed. Enable with
 * <code>rdf4j.spring.uuidsource.simple.enabled=true</code>
 * <li>{@link org.eclipse.rdf4j.spring.uuidsource.sequence.UUIDSequence UUIDSequence}: When a UUID is needed, ask the
 * repository for N >> 1 UUIDs and answer one at a time. Enable with <code>
 *       rdf4j.spring.uuidsource.sequence.enabled=true</code>
 * </ol>
 *
 * Only one of these approaches can be activated.
 */
package org.eclipse.rdf4j.spring.uuidsource;
