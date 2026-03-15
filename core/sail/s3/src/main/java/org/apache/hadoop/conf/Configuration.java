/*
 * Minimal stub for org.apache.hadoop.conf.Configuration.
 *
 * Parquet-hadoop references this class in abstract method signatures
 * (WriteSupport.init, ParquetWriter.Builder.getWriteSupport). Our code
 * overrides the ParquetConfiguration variants instead, so this class is
 * never instantiated or used at runtime. It exists only to satisfy the
 * JVM class loader.
 */
package org.apache.hadoop.conf;

public class Configuration {
	public Configuration() {
	}
}
