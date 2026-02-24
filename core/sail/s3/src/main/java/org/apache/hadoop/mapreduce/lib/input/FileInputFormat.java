/*
 * Minimal stub — satisfies JVM class loading for parquet-hadoop.
 * ParquetInputFormat extends this class; loaded when ParquetReadOptions.Builder
 * calls ParquetInputFormat.getFilter(). Never used at runtime.
 */
package org.apache.hadoop.mapreduce.lib.input;

import org.apache.hadoop.mapreduce.InputFormat;

public abstract class FileInputFormat<K, V> extends InputFormat<K, V> {
}
