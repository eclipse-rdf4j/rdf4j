---
title: "RDF4J REST API: Changelog"
toc: true
weight: 1
---

[OpenAPI specification](/documentation/reference/rest-api/)

## Version history

The RDF4J REST API uses a single integer version numbering scheme. It does not follow semantic versioning. However, the implementations of the API client and server in RDF4J make a conscious effort to stay backward compatible with at least one previous protocol version.

### 14: since RDF4J 4.3.0

- Replaced usage of the `action` parameter on the `/transactions/{txnID}` endpoint with separate endpoints for each available action: `/transactions/{txnID}/add`, `/transactions/{txnID}/query`, and so on.
- `action` parameter for transactions is deprecated

### 13: since RDF4J 4.0.0

- Removed support for the deprecated SYSTEM repository.

### 12: since RDF4J 3.5.0

- Added suport for the `prepare` transaction operation.

### 11: since RDF4J 3.3.0

- Added support for sending general transaction setting parameters.

### 10: since RDF4J 3.1.0

- Added support for retrieving a repository configuration.

### 9: since RDF4J 3.0.0

- Added direct API support for creating a new repository remotely and/or updating an existing repository's configuration. 
- SYSTEM repository support is deprecated.
