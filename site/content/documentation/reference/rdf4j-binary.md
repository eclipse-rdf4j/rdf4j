---
title: "RDF4J Binary RDF Format"
toc: true
weight: 3
---

RDF4J supports reading and writing a custom binary RDF serialization format. Its main features are reduced parsing overhead and minimal memory requirements (for handling really long literals, amongst other things).
<!--more-->

# MIME Content Type

RDF4J assigns the content type `application/x-binary-rdf` to its format.

# Overall design

Results encoded in the RDF4J Binary RDF format consist of a header followed by zero or more records, and closes with an `END_OF_DATA` marker (see below). Values are stored in network order (Big-Endian).

All string values use UTF-16 encoding. Reference ids are assigned to recurring values to avoid having to repeat long strings.

# Header

The header is 8 bytes long:

- Bytes 0-3 contain a magic number, namely the ASCII codes for the string “BRDF”, which stands for Binary RDF.
- Bytes 4-7 specify the format version (a 4-byte signed integer).

For example, a header for a result in format version 1 will look like this:

      byte: 0  1  2  3 |  4  5  6  7 | 
    -------------------+-------------+
     value: B  R  D  F |  0  0  0  1 |

# Content records

Zero or more records follow after the header. Each record can be a namespace declaration, a comment, a value reference declaration, or a statement.

Each record starts with a record type marker (a single byte). The following record types are defined in the current format:

- `NAMESPACE_DECL` (byte value: 0):
  This indicates a namespace declaration record.
- `STATEMENT` (byte value: 1):
    This indicates an RDF statement record.
- `COMMENT` (byte value: 2):
    This indicates a comment record.
- `VALUE_DECL` (byte value: 3):
    This indicates a value declaration.
- `END_OF_DATA` (byte value: 127):
    This indicates the end of the data stream has been reached.

## Strings

All strings are encoded as UTF-16 encoded byte arrays. A String is preceeded by a 4-byte signed integer that encodes the length of the string (specifically, it records the number of Unicode code units). For example, the string ‘foo’ will be encoded as follows:

     byte: 0 1 2 3 | 4 6 8 |
    ---------------+-------+
    value: 0 0 0 3 | f o o |

## RDF Values

Each RDF value type has its own specific 1-byte record type marker:

- `NULL_VALUE` (byte value: 0)
    marks an empty RDF value (this is used, for example, in encoding of context in statements)
- `URI_VALUE` (byte value: 1)
    marks a URI value
- `BNODE_VALUE` (byte value: 2)
    marks a blank node value
- `PLAIN_LITERAL_VALUE` (byte value: 3)
    marks a plain literal value
- `LANG_LITERAL_VALUE` (byte value: 4)
    marks a language-tagged literal value
- `DATATYPE_LITERAL_VALUE` (byte value: 5)
    marks a datatyped literal value

### URIs

URIs are recorded by the `URI_VALUE` marker followed by the URI encoded as a string.

### Blank nodes

Blank nodes are recorded by the `BNODE_VALUE` marker followed by the id of the blank node encoded as a string.

### Literals

Depending on the specific literal type (plain, language-tagged, datatyped), a literal is recorded by one of the markers `PLAIN_LITERAL_VALUE`, `LANG_LITERAL_VALUE` or `DATATYPE_LITERAL_VALUE`. This is followed by the lexical label of the literal as a string, optionally followed by either a language tag encoded as a string value or a datatype encoded as a string.

## Value reference declaration records

To enable further compression of the byte stream, the Binary RDF format enables encoding of reference-identifiers for often-repeated RDF values. A value reference declaration starts with a `VALUE_DECL` record marker (1 byte, value 3), followed by a 4-byte signed integer that encodes the reference id. This is followed by the actual value, encoded as an RDF value (see above).

For example, a declaration that assigns id 42 to the URI ‘http://example.org/HHGTTG’ will look like this:

      byte: 0 | 1 2 3 4 | 5 | 6 7 8 9 | 10 12 14 16 18 (etc) | 
    ----------+---------+---+---------+----------------------+
     value: 3 | 0 0 0 42| 1 | 0 0 0 25| h  t  t  p  :  (etc) |

Explanation: byte 0 marks the record as a `VALUE_DECL`, bytes 1-4 encode the reference id, byte 5 encodes the value type (`URI_VALUE`), bytes 6-9 encode the length of the string value, bytes 10 and further encode the actual string value as an UTF-16 encoded byte array.

Note that the format allows the same reference id to be assigned more than once. When a second value declaration occurs, it effectively overwrites a previous declaration, reassigning the id to a new value for all following statements.

## Namespace records

A namespace declaration is recorded by the `NAMESPACE_DECL marker`. Next follows the namespace prefix, as a string, then followed by the namespace URI, as a string.

For example, a namespace declaration record for prefix ‘ex’ and namespace uri ‘http://example.org/’ will look like this:

      byte: 0 | 1 2 3 4 | 5 6 | 7 8 9 10 | 11 13 15 17 19 (etc) |
    ----------+---------+-----+----------+----------------------+
     value: 0 | 0 0 0 2 | e x | 0 0 0 19 | h  t  t  p  :  (etc) |

## Comment records

A comment is recorded by the `COMMENT` marker, followed by the comment text encoded as a string.

For example, a record for the comment ‘example’ will look like this:

      byte: 0 | 1 2 3 4 | 5 7 9 11 13 15 17 |
    ----------+---------+-------------------+
     value: 2 | 0 0 0 7 | e x a m  p  l  e  |

## Statement records

Each statement record starts with a `STATEMENT` marker (1 byte, value 1). For the encoding of the statement’s subject, predicate, object and context, either the RDF value is encoded directly, or a previously assigned value reference (see section 2.3) is reused. A Value references is recorded with the `VALUE_REF` marker (1 byte, value 6), followed by the reference id as a 4-byte signed integer.

### An example statement

Consider the following RDF statement:

    <http://example.org/George> <http://example.org/name> "George" .

Assume that the subject and predicate previously been assigned reference ids,
(42 and 43 respecively). The object value has not been assigned a reference id.

This statement would then be recorded as follows:

     byte: 0 | 1 | 2 3 4 5 | 6 | 7 8 9 10| 11 | 12 13 14 15 | 16 18 20 22 24 26 | 28 |
    ---------+---+---------+---+---------+----+-------------+-------------------+----+
    value: 1 | 6 | 0 0 0 42| 6 | 0 0 0 43|  3 |  0  0  0  5 |  G  e  o  r  g  e |  0 |

Explanation: byte 0 marks the record as a `STATEMENT`. Byte 1 marks the subject of the statement as a `VALUE_REF`. Bytes 2-5 encode the reference id of the subject. Byte 6 marks the predicate of the statement as a `VALUE_REF`. Byte 7-10 encode the reference id of the predicate. Byte 11 marks the obect of the statement as a `PLAIN_LITERA`L value, bytes 12-15 encode the length of the lexical value of the literal, and bytes 16-26 encode the literal’s lexical value as a UTF-16 encoded byte array. Finally, byte 28 marks the context field of the statement as a `NULL_VALUE`.

# Buffering and value reference handling

The binary RDF format enables declaration of value references for more compressed representation of often-repeated values.

A binary RDF producer may choose to introduce a reference for every RDF value. This is a simple approach, but it produces a suboptimal compression (because for values which occur only once, direct encoding of the value uses fewer bytes than introducing a reference for it).

Another approach is to introduce a buffered writing strategy: statements to be serialized are put on a queue with a certain capacity, and for each RDF value in these queued statements the number of occurrences in the queue is determined. As the queue is emptied and each statement is serialized, all values that occur more than once in the queue are assigned a reference id. This is, in fact, the strategy employed by the Rio Writer.

It is also important to note that reference ids are not necessarily global over the entire document: ids are assigned on the basis of number of occurrences of a value in the current statement queue. If that number drops to zero, the reference id for that value can be ‘recycled’, that is, reassigned to another value. This ensures that we never run out of reference ids, even for very large datasets.
