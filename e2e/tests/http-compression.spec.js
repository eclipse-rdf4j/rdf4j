// @ts-check
const http = require('http');
const https = require('https');
const zlib = require('zlib');

const { test, expect } = require('@playwright/test');

const SERVER_BASE_URL = process.env.RDF4J_SERVER_BASE_URL || 'http://127.0.0.1:8080/rdf4j-server';
const TARGET_NAME = process.env.RDF4J_E2E_TARGET_NAME || 'rdf4j-server';
const COMPRESSION_ENCODINGS = [
    {
        name: 'gzip',
        compress: value => zlib.gzipSync(value),
        decompress: value => zlib.gunzipSync(value)
    },
    {
        name: 'deflate',
        compress: value => zlib.deflateSync(value),
        decompress: value => zlib.inflateSync(value)
    }
];

test.describe(`HTTP compression (${TARGET_NAME})`, () => {
    test.beforeEach(({ browserName }) => {
        test.skip(browserName !== 'chromium', 'HTTP-only compression coverage runs once per target');
    });

    for (const encoding of COMPRESSION_ENCODINGS) {
        test(`SPARQL endpoint accepts ${encoding.name} request bodies and returns ${encoding.name} when supported`,
                async () => {
                    const repoId = `compression-${encoding.name}-${process.pid}-${Date.now()}`;
                    const query = valuesQuery(80);
                    const compressedQuery = encoding.compress(Buffer.from(query, 'utf8'));

                    await createMemoryRepository(repoId);

                    try {
                        const response = await request(serverUrl(`repositories/${encodeURIComponent(repoId)}`), {
                            method: 'POST',
                            headers: {
                                Accept: 'application/sparql-results+json',
                                'Accept-Encoding': encoding.name,
                                'Content-Encoding': encoding.name,
                                'Content-Length': compressedQuery.length,
                                'Content-Type': 'application/sparql-query',
                                'User-Agent': 'rdf4j-e2e-compression'
                            }
                        }, compressedQuery);

                        expect(response.statusCode).toBe(200);
                        expect(response.headers['content-encoding']).toBe(encoding.name);

                        const body = encoding.decompress(response.body).toString('utf8');
                        const json = JSON.parse(body);

                        expect(json.head.vars).toEqual(['value']);
                        expect(json.results.bindings).toHaveLength(80);
                        expect(json.results.bindings[0].value.value).toBe('value-000');
                        expect(json.results.bindings[79].value.value).toBe('value-079');
                    } finally {
                        await deleteRepository(repoId);
                    }
                });
    }
});

async function createMemoryRepository(repoId) {
    const body = Buffer.from(memoryRepositoryConfig(repoId), 'utf8');
    const response = await request(serverUrl(`repositories/${encodeURIComponent(repoId)}`), {
        method: 'PUT',
        headers: {
            'Content-Length': body.length,
            'Content-Type': 'text/turtle',
            'User-Agent': 'rdf4j-e2e-compression'
        }
    }, body);

    expect([200, 201, 204]).toContain(response.statusCode);
}

async function deleteRepository(repoId) {
    const response = await request(serverUrl(`repositories/${encodeURIComponent(repoId)}`), {
        method: 'DELETE',
        headers: {
            'User-Agent': 'rdf4j-e2e-compression'
        }
    });

    expect([200, 202, 204, 404]).toContain(response.statusCode);
}

function memoryRepositoryConfig(repoId) {
    return `@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>.
@prefix config: <tag:rdf4j.org,2023:config/>.

[] a config:Repository ;
   config:rep.id "${repoId}" ;
   rdfs:label "HTTP compression test repository" ;
   config:rep.impl [
      config:rep.type "openrdf:SailRepository" ;
      config:sail.impl [
         config:sail.type "openrdf:MemoryStore" ;
         config:mem.persist false
      ]
   ].
`;
}

function valuesQuery(rowCount) {
    const values = Array.from({ length: rowCount }, (_, index) => {
        return `"value-${String(index).padStart(3, '0')}"`;
    }).join(' ');
    return `SELECT ?value WHERE { VALUES ?value { ${values} } }`;
}

function serverUrl(relativePath) {
    const base = SERVER_BASE_URL.endsWith('/') ? SERVER_BASE_URL : `${SERVER_BASE_URL}/`;
    return new URL(relativePath, base);
}

function request(url, options, body) {
    const transport = url.protocol === 'https:' ? https : http;

    return new Promise((resolve, reject) => {
        const request = transport.request(url, options, response => {
            const chunks = [];
            response.on('data', chunk => chunks.push(chunk));
            response.on('end', () => {
                resolve({
                    body: Buffer.concat(chunks),
                    headers: response.headers,
                    statusCode: response.statusCode
                });
            });
        });

        request.on('error', reject);
        request.setTimeout(15000, () => {
            request.destroy(new Error(`Timed out waiting for ${url}`));
        });
        request.end(body);
    });
}
