#!/usr/bin/env python3
# Copyright (c) 2026 Eclipse RDF4J contributors.
# SPDX-License-Identifier: BSD-3-Clause
"""Collect offline release evidence, never download licenses or approve IP reviews.

Inputs are the actual Vite module list, a directory containing the resolved
runtime/transitive JARs, and the checked-in vendored inventory. See
scripts/third-party-notices.md for the input and manual evidence contracts.
Exit codes: 0 collected, 1 incomplete evidence, 2 invalid input/tool error.
"""
import argparse
from datetime import date
import hashlib
import html
import json
import os
from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET
import zipfile

LEGAL = re.compile(r'^(?:licen[cs]es?|notices?|copying|copyright|third[-_ ]party)(?:[.\-_ ]|$)', re.I)

NATIVE = re.compile(r'\.(?:so(?:\.[0-9]+)*|dll|dylib|jnilib|wasm|a|lib)$', re.I)

REVIEW_MESSAGES = {
    'license-choice': 'Compound license/exception declaration retained verbatim; verify all required texts and choices',
    'embedded-components': 'Contains compiled non-npm code; review embedded engine/component license coverage',
    'native-components': 'Contains native binary payloads; review embedded native component licenses and attribution',
    'shaded-attribution': 'JAR contains multiple Maven identities; verify shaded component attribution coverage',
}


def evidence_hash(component):
    evidence = {key: component.get(key) for key in
                ('id', 'version', 'sources', 'declaredLicenses', 'documents', 'artifacts', 'installations', 'nativePayloads')}
    return digest(json.dumps(evidence, sort_keys=True, separators=(',', ':')).encode())


def read_json(path):
    return json.loads(path.read_text(encoding='utf-8'))


def digest(data):
    return hashlib.sha256(data).hexdigest()


def legal_file(path):
    if NATIVE.search(path.name):
        return False
    if path.suffix.lower() in ('.class', '.js', '.mjs', '.cjs', '.ts', '.java', '.wasm', '.png', '.gif', '.jpg', '.map'):
        return False
    return bool(LEGAL.match(path.name) or any(LEGAL.match(p) for p in path.parts[:-1]))


def labels(value):
    if isinstance(value, str):
        return [value]
    if isinstance(value, dict):
        return [value.get('type', 'UNKNOWN')]
    if isinstance(value, list):
        return sorted({label for item in value for label in labels(item)})
    return []


class Collector:
    def __init__(self, repo, version, maven_repo):
        self.repo, self.version, self.maven_repo = repo, version, maven_repo
        self.components = {}
        self.texts = {}
        self.issues = []
        self.notes = []
        self.covered_files = set()
        self.project_files = []
        self.reviews = []

    def issue(self, component, message):
        self.issues.append({'component': component, 'message': message})

    def component(self, identity, name, version, ecosystem, source):
        if identity not in self.components:
            self.components[identity] = dict(id=identity, name=name, version=version, ecosystem=ecosystem,
                                            sources=[], declaredLicenses=[], attribution=[], documents=[])
        component = self.components[identity]
        if source and source not in component['sources']:
            component['sources'].append(source)
        return component

    def document(self, component, data, source, kind='license'):
        try:
            text = data.decode('utf-8-sig')
        except UnicodeDecodeError:
            self.issue(component['id'], 'License/notice is not UTF-8: ' + source)
            return
        if not text.strip():
            self.issue(component['id'], 'Empty license/notice: ' + source)
            return
        # Exact source-byte hash, not SPDX name, is the deduplication key.
        key = digest(data)
        self.texts[key] = {'sha256': key, 'text': text}
        doc = dict(sha256=key, source=source, kind=kind)
        if doc not in component['documents']:
            component['documents'].append(doc)

    def package_documents(self, component, directory, source_prefix=""):
        for path in sorted(directory.rglob('*')):
            relative = path.relative_to(directory)
            if 'node_modules' in relative.parts or not path.is_file() or path.is_symlink():
                continue
            if legal_file(relative):
                self.document(component, path.read_bytes(), source_prefix + relative.as_posix(),
                              'notice' if 'notice' in path.name.lower() or 'copyright' in path.name.lower() else 'license')
            elif path.suffix in ('.js', '.mjs', '.cjs'):
                prefix = path.read_bytes()[:32768]
                match = re.match(rb'\s*(/\*[!*]?[\s\S]*?\*/)', prefix)
                if match and re.search(rb'copyright|this distribution contains', match[1], re.I):
                    self.document(component, match[1], source_prefix + relative.as_posix() + '#leading-comment', 'notice')
            elif path.name == 'metadata.json':
                # E.g. Viz.js carries Graphviz and its embedded libraries here.
                data = path.read_bytes()
                if re.search(rb'"(?:license|licenses|copyright)"', data, re.I):
                    self.document(component, data, source_prefix + relative.as_posix(), 'metadata')

    def npm(self, project, modules_path):
        lock = read_json(project / 'package-lock.json')
        if lock.get('lockfileVersion') not in (2, 3):
            raise ValueError('Expected npm lockfile v2 or v3')
        try:
            modules = read_json(modules_path)
        except FileNotFoundError:
            if modules_path.is_symlink():
                raise  # A broken link is malformed input, not a skipped frontend build.
            source = modules_path.relative_to(self.repo).as_posix() if modules_path.is_relative_to(self.repo) else modules_path.name
            self.issue('npm', 'Missing npm bundle inventory: ' + source
                       + '; run the full frontend build before strict release notice verification')
            return  # Never infer shipped membership from package.json or a lockfile.

        if not isinstance(modules, list) or not all(isinstance(p, str) for p in modules):
            raise ValueError('npm module input must be a JSON array of bundled module paths')
        locations = set()
        bundled_files = {}
        for module in modules:
            clean = module.split('?', 1)[0].replace('\\', '/')
            if 'node_modules/' not in clean:
                continue  # Own project source; not an npm dependency.
            path = Path(clean)
            path = path if path.is_absolute() else project / path
            # The closest package root, including nested installations, owns the module.
            cursor = path if path.is_dir() else path.parent
            while cursor != project and cursor != cursor.parent and not (cursor / 'package.json').is_file():
                cursor = cursor.parent
            try:
                location = cursor.relative_to(project).as_posix()
            except ValueError:
                self.issue(module, 'Bundled package resolves outside npm project; provide manual provenance')
                continue
            if location not in lock['packages'] or not (cursor / 'package.json').is_file():
                self.issue(module, 'Bundled npm module has no matching installed package and lockfile entry')
                continue
            locations.add(location)
            if path.is_file():
                bundled_files.setdefault(location, set()).add(path)
            else:
                self.issue(module, 'Bundled module file missing from installed package')
        if not locations:
            self.issue('npm', 'No bundled npm packages discovered; module list may be empty or incomplete')
        for location in sorted(locations):
            directory = project / location
            package, locked = read_json(directory / 'package.json'), lock['packages'][location]
            name, version = package.get('name'), package.get('version')
            if not name or not version or version != locked.get('version'):
                self.issue(location, 'Installed npm identity/version differs from lockfile')
                continue
            component = self.component('npm:' + name + '@' + version, name, version, 'npm', locked.get('resolved', location))
            declared = labels(package.get('license', package.get('licenses')))
            attribution = labels(package.get('copyright'))
            artifacts = [{'source': p.relative_to(project).as_posix(), 'sha256': digest(p.read_bytes())}
                         for p in sorted(bundled_files.get(location, set()))]
            component['declaredLicenses'] = sorted(set(component['declaredLicenses'] + declared))
            component['attribution'] = sorted(set(component['attribution'] + attribution))
            for artifact in artifacts:
                if artifact not in component.setdefault('artifacts', []):
                    component['artifacts'].append(artifact)
            component.setdefault('installations', []).append(dict(
                location=location, source=locked.get('resolved', location), integrity=locked.get('integrity'),
                declaredLicenses=declared, attribution=attribution, artifacts=artifacts))
            self.package_documents(component, directory, location + '/')
            # Metadata alone cannot tell whether compiled native components have all notices.
            if any(directory.rglob('*.wasm')) or name == '@viz-js/viz':
                self.issue(component['id'], 'Contains compiled non-npm code; review embedded engine/component license coverage')

    def pom_licenses(self, data, seen=None):
        root = ET.fromstring(data)
        def child(element, name):
            return next((v for v in element if v.tag.rsplit('}', 1)[-1] == name), None)
        def value(element, name):
            node = child(element, name)
            return node.text.strip() if node is not None and node.text else ''
        block = child(root, 'licenses')
        if block is not None and len(block):
            return [dict(name=value(item, 'name'), url=value(item, 'url')) for item in block]
        parent = child(root, 'parent')
        if parent is not None:
            group, artifact, version = (value(parent, k) for k in ('groupId', 'artifactId', 'version'))
            key = (group, artifact, version)
            seen = set() if seen is None else seen
            if all(key) and key not in seen and all(re.fullmatch(r'[\w.\-]+', v) for v in key):
                path = self.maven_repo / group.replace('.', '/') / artifact / version / (artifact + '-' + version + '.pom')
                if path.is_file():
                    return self.pom_licenses(path.read_bytes(), seen | {key})
        return []

    def jars(self, directory=None, artifacts=None):
        if artifacts is None:
            if not directory.is_dir():
                raise ValueError('Runtime JAR directory does not exist: ' + str(directory))
            artifacts = directory.rglob('*.jar')
        for path in sorted(set(artifacts)):
            with zipfile.ZipFile(path) as jar:
                names = sorted(jar.namelist())
                properties = [n for n in names if n.startswith('META-INF/maven/') and n.endswith('/pom.properties')]
                identities = []
                for entry in properties:
                    values = dict(re.findall(r'^([^#!\s=]+)\s*=\s*(.*?)\s*$', jar.read(entry).decode('utf-8'), re.M))
                    if all(values.get(k) for k in ('groupId', 'artifactId', 'version')):
                        identities.append((values, entry.rsplit('/', 1)[0] + '/pom.xml'))
                if not identities:
                    # Some publishers omit Maven metadata from JARs. Recover only from
                    # byte-identical local repository artifacts, never filename guesses.
                    for candidate in sorted(self.maven_repo.rglob(path.name)):
                        if not candidate.is_file() or digest(candidate.read_bytes()) != digest(path.read_bytes()):
                            continue
                        relative = candidate.relative_to(self.maven_repo)
                        if len(relative.parts) < 4:
                            continue
                        group = '.'.join(relative.parts[:-3])
                        artifact, version = relative.parts[-3:-1]
                        pom_path = candidate.parent / (artifact + '-' + version + '.pom')
                        if pom_path.is_file():
                            identities.append((dict(groupId=group, artifactId=artifact, version=version), None))
                    if len(identities) > 1:
                        self.issue(path.name, 'Multiple byte-identical local Maven artifacts; verify coordinate identity')
                if not identities:
                    component = self.component('jar:' + path.name, path.name, 'UNKNOWN', 'maven', path.name)
                    self.issue(component['id'], 'JAR lacks Maven coordinates; add exact manual component provenance')
                    identities = [(None, None)]
                for values, pom in identities:
                    if values:
                        group, artifact, version = (values[k] for k in ('groupId', 'artifactId', 'version'))
                        identity = 'maven:' + group + ':' + artifact + ':' + version
                        component = self.component(identity, group + ':' + artifact, version, 'maven', path.name)
                        metadata = []
                        if pom in names:
                            metadata = self.pom_licenses(jar.read(pom))
                        if not metadata and all(re.fullmatch(r'[\w.\-]+', v) for v in (group, artifact, version)):
                            local = self.maven_repo / group.replace('.', '/') / artifact / version / (artifact + '-' + version + '.pom')
                            if local.is_file():
                                metadata = self.pom_licenses(local.read_bytes())
                        component['declaredLicenses'] = [m['name'] or m['url'] for m in metadata]
                        component['licenseMetadata'] = metadata
                        if group == 'org.eclipse.rdf4j':
                            component['role'] = 'project'
                        if group == 'org.eclipse.rdf4j' and (self.repo / 'LICENSE').is_file():
                            component['declaredLicenses'] = ['BSD-3-Clause']
                            self.document(component, (self.repo / 'LICENSE').read_bytes(), 'RDF4J/LICENSE')
                    artifact = {'source': path.name, 'sha256': digest(path.read_bytes())}
                    if artifact not in component.setdefault('artifacts', []):
                        component['artifacts'].append(artifact)
                    native_entries = [entry for entry in names if not entry.endswith('/') and NATIVE.search(entry)]
                    for entry in native_entries:
                        payload = dict(artifact=path.name, artifactSha256=artifact['sha256'], path=entry,
                                       sha256=digest(jar.read(entry)), source=path.name + '!/' + entry)
                        if payload not in component.setdefault('nativePayloads', []):
                            component['nativePayloads'].append(payload)
                    if native_entries:
                        self.issue(component['id'], REVIEW_MESSAGES['native-components'])
                    for entry in names:
                        if not entry.endswith('/') and legal_file(Path(entry)):
                            self.document(component, jar.read(entry), path.name + '!/' + entry,
                                          'notice' if 'notice' in entry.lower() else 'license')
                    if len(identities) > 1:
                        self.issue(component['id'], 'JAR contains multiple Maven identities; verify shaded component attribution coverage')

    def vendors(self):
        path = self.repo / 'tools/workbench/frontend-vendor-dependencies.json'
        if not path.is_file():
            return
        inventory = read_json(path)
        self.notes.extend(inventory.get('bundleLimitations', []))
        for package in inventory['dependencies']:
            name, version = package['name'], package['version']
            component = self.component('npm:' + name + '@' + version, name, version, 'vendored', package.get('source', ''))
            for relative in package['files']:
                file = (self.repo / relative).resolve()
                self.covered_files.add(file)
                if not file.is_relative_to(self.repo) or not file.is_file():
                    self.issue(component['id'], 'Missing/unsafe vendored file: ' + relative)
                    continue
                data = file.read_bytes()
                if digest(data) != package.get('sha256', {}).get(relative):
                    self.issue(component['id'], 'Vendored hash mismatch: ' + relative)
                if legal_file(Path(relative).relative_to('tools/workbench/src/main/frontend/static')) or '.LICENSE' in file.name:
                    self.document(component, data, relative)
            # Do not guess a package license from its npm name. Text is the primary evidence.
            component['declaredLicenses'] = ['See accompanying license text']
            detected = set()
            for doc in component['documents']:
                text = self.texts[doc['sha256']]['text']
                if 'Permission is hereby granted, free of charge' in text:
                    detected.add('MIT')
                elif 'Redistribution and use in source and binary forms' in text:
                    detected.add('BSD-3-Clause' if 'Neither the name' in text or 'BSD 3-Clause' in text else 'BSD-2-Clause')
            component['detectedLicenses'] = sorted(detected)

    def manual(self, path):
        if not path:
            return
        data = read_json(path)
        if data.get('schemaVersion') != 1:
            raise ValueError('Manual evidence requires schemaVersion 1')
        self.reviews.extend(data.get('reviews', []))
        for item in data.get('ownedFiles', []):
            file = (self.repo / item['path']).resolve()
            if not item.get('reason') or not item.get('source') or not item.get('licensePath'):
                raise ValueError('Owned-file evidence requires reason, source and licensePath')
            license_path = (self.repo / item['licensePath']).resolve()
            if not file.is_relative_to(self.repo) or not license_path.is_relative_to(self.repo):
                raise ValueError('Unsafe owned-file evidence path')
            if not file.is_file() or digest(file.read_bytes()) != item.get('sha256'):
                self.issue(item['path'], 'Owned-file provenance hash mismatch or missing file')
                continue
            if not license_path.is_file() or digest(license_path.read_bytes()) != item.get('licenseSha256'):
                self.issue(item['path'], 'Owned-file license hash mismatch or missing file')
                continue
            self.covered_files.add(file)
            self.project_files.append(item)
        for item in data.get('components', []):
            if item.get('includedWhenComponent') and item['includedWhenComponent'] not in self.components:
                continue
            identity = item['id']
            if not item.get('reason') or not item.get('source'):
                raise ValueError('Manual component requires reason and source: ' + identity)
            component = self.component(identity, item['name'], item['version'], 'manual', item['source'])
            component.setdefault('manualEvidence', []).append({'reason': item['reason'], 'source': item['source']})
            component['declaredLicenses'] = sorted(set(component['declaredLicenses'] + item.get('declaredLicenses', [])))
            component['attribution'] = sorted(set(component['attribution'] + item.get('attribution', [])))
            for document in item.get('documents', []):
                file = (self.repo / document['path']).resolve()
                if not file.is_relative_to(self.repo) or not file.is_file():
                    self.issue(identity, 'Missing/unsafe manual evidence file: ' + document['path'])
                    continue
                self.covered_files.add(file)
                raw = file.read_bytes()
                if digest(raw) != document.get('sha256'):
                    self.issue(identity, 'Manual evidence hash mismatch: ' + document['path'])
                    continue
                if 'startLine' in document or 'endLine' in document:
                    start, end = document.get('startLine', 1), document.get('endLine', len(raw.splitlines()))
                    if not isinstance(start, int) or not isinstance(end, int) or not 1 <= start <= end <= len(raw.splitlines()):
                        raise ValueError('Invalid manual source line range: ' + document['path'])
                    raw = b''.join(raw.splitlines(keepends=True)[start - 1:end])
                source = document['path']
                if 'startLine' in document or 'endLine' in document:
                    source += '#L' + str(start) + '-L' + str(end)
                self.document(component, raw, source, document.get('kind', 'license'))
            for reason in item.get('unresolved', []):
                self.issue(identity, reason)

    def loose_javascript(self, directory):
        if not directory.is_dir():
            return
        for path in sorted(directory.rglob('*')):
            if path.suffix not in ('.js', '.mjs', '.cjs') or path.resolve() in self.covered_files:
                continue
            # Recognize the project's own source headers. Unidentified content is
            # surfaced for review, not automatically assigned a license.
            text = path.read_text(encoding='utf-8', errors='replace')
            if re.search(r'Copyright[^\n]*(?:Eclipse RDF4J|Aduna)', text[:1500]):
                continue
            source = path.relative_to(self.repo).as_posix() if path.is_relative_to(self.repo) else path.name
            component = self.component('javascript:' + source, source, 'UNKNOWN', 'javascript', source)
            component['artifactSha256'] = digest(path.read_bytes())
            self.issue(component['id'], 'Uncatalogued JavaScript: supply version, provenance, and complete license evidence in manual inventory')

    def finish(self):
        for component in self.components.values():
            identity = component['id']
            if not component['declaredLicenses'] or any(v in ('UNKNOWN', 'UNLICENSED') for v in component['declaredLicenses']):
                self.issue(identity, 'Missing or unknown license declaration')
            if not any(d['kind'] == 'license' for d in component['documents']):
                self.issue(identity, 'No complete license text found in local evidence')
            if len(component['declaredLicenses']) > 1 or any(re.search(r'\b(?:AND|OR|WITH)\b|\bexception\b', v, re.I) for v in component['declaredLicenses']):
                self.issue(identity, 'Compound license/exception declaration retained verbatim; verify all required texts and choices')
            for key in ('sources', 'declaredLicenses', 'attribution'):
                component[key] = sorted(set(component[key]))
            component['documents'].sort(key=lambda d: (d['source'], d['sha256'], d['kind']))
            if component.get('nativePayloads'):
                component['nativePayloads'].sort(key=lambda p: (p['artifact'], p['path'], p['sha256']))
        for component in self.components.values():
            component['reviewEvidenceSha256'] = evidence_hash(component)
        for review in self.reviews:
            if review.get('status') == 'pending':
                continue
            identity = review.get('component', '')
            component = self.components.get(identity)
            review_type = review.get('type')
            valid = (review.get('status') == 'completed' and component is not None
                     and review_type in REVIEW_MESSAGES and review.get('version') == component['version']
                     and review.get('evidenceSha256') == component['reviewEvidenceSha256']
                     and isinstance(review.get('reviewer'), str) and bool(review['reviewer'].strip())
                     and isinstance(review.get('reason'), str) and bool(review['reason'].strip()))
            try:
                date.fromisoformat(review.get('date', ''))
            except (ValueError, TypeError):
                valid = False
            references = review.get('documentSha256', [])
            known_documents = {d['sha256'] for d in component['documents']} if component else set()
            license_documents = {d['sha256'] for d in component['documents'] if d['kind'] == 'license'} if component else set()
            valid = (valid and isinstance(references, list) and bool(references)
                     and all(isinstance(ref, str) and ref in known_documents for ref in references)
                     and any(ref in license_documents for ref in references))
            if not valid:
                self.issue(identity or 'manual-review', 'Invalid completed review evidence: exact identity, version, hashes, '
                           'known review type, reviewer, date, reason, and referenced license documents are required')
                continue
            component.setdefault('coverageReviews', []).append(review)
            self.issues = [issue for issue in self.issues if not
                           (issue['component'] == identity and issue['message'] == REVIEW_MESSAGES[review_type])]
        issues = sorted({json.dumps(i, sort_keys=True) for i in self.issues})
        return dict(schemaVersion=1, project='Eclipse RDF4J', version=self.version,
                    complete=not issues, coverage='Collected local evidence, not legal approval or proof of compliance. '
                    'npm follows supplied bundle modules; Java follows supplied resolved runtime JAR inputs; '
                    'vendored and manual content follow their hash-checked inventories.',
                    components=sorted(self.components.values(), key=lambda c: c['id']),
                    texts=[self.texts[k] for k in sorted(self.texts)],
                    issues=[json.loads(i) for i in issues], notes=sorted(set(self.notes)),
                    projectFiles=sorted(self.project_files, key=lambda item: item['path']))


def render(data):
    esc = lambda value: html.escape(str(value), quote=True)
    parts = ['<!doctype html><html lang="en"><meta charset="utf-8"><meta name="viewport" content="width=device-width">',
             '<title>RDF4J third-party notices</title><style>body{font:16px system-ui;max-width:70rem;margin:2rem auto;padding:0 1rem;line-height:1.5;overflow-wrap:anywhere}pre{white-space:pre-wrap;overflow-wrap:anywhere;background:#f5f5f5;padding:1rem}a{color:#0645ad}.warning{border:2px solid #b60;padding:1rem}</style>',
             '<h1>Eclipse RDF4J ' + esc(data['version']) + ': third-party notices</h1>',
             '<p><a href="NOTICE.md">Project NOTICE</a> · <a href="edl-v1.0.txt">Project license</a></p>',
             '<p>' + esc(data['coverage']) + '</p>']
    if data['issues']:
        parts.append('<section class="warning"><h2>Unresolved evidence</h2><p>This inventory is incomplete and requires review before release.</p><ul>')
        parts.extend('<li>' + esc(i['component']) + ': ' + esc(i['message']) + '</li>' for i in data['issues'])
        parts.append('</ul></section>')
    parts.append('<h2>Components</h2>')
    for component in data['components']:
        parts.append('<section><h3>' + esc(component['name']) + ' ' + esc(component['version']) + '</h3>')
        if component.get('role') == 'project':
            parts.append('<p><strong>RDF4J project component, not third-party software.</strong></p>')
        parts.append('<p>Declared licenses: ' + esc('; '.join(component['declaredLicenses']) or 'UNKNOWN') + '</p>')
        if component.get('detectedLicenses'):
            parts.append('<p>License text identifies: ' + esc('; '.join(component['detectedLicenses'])) + '</p>')
        parts.extend('<p>' + esc(v) + '</p>' for v in component['attribution'])
        if component.get('nativePayloads'):
            parts.append('<h4>Native binary payloads</h4><ul>')
            parts.extend('<li>' + esc(payload['source']) + ' (SHA-256: ' + esc(payload['sha256']) + ')</li>'
                         for payload in component['nativePayloads'])
            parts.append('</ul>')
        for review in component.get('coverageReviews', []):
            parts.append('<p>Human coverage review (' + esc(review['type']) + '): ' + esc(review['reviewer'])
                         + ', ' + esc(review['date']) + '. ' + esc(review['reason']) + '</p>')
        parts.append('<p>Source: ' + esc('; '.join(component['sources'])) + '</p><ul>')
        parts.extend('<li><a href="#text-' + d['sha256'] + '">' + esc(d['kind'] + ': ' + d['source']) + '</a></li>' for d in component['documents'])
        parts.append('</ul></section>')
    parts.append('<h2>License and notice texts</h2>')
    for document in data['texts']:
        parts.append('<section id="text-' + document['sha256'] + '"><h3>Text ' + document['sha256'][:12] + '</h3><pre>' + esc(document['text']) + '</pre></section>')
    if data['notes']:
        parts.append('<h2>Provenance notes</h2><ul>')
        parts.extend('<li>' + esc(note) + '</li>' for note in data['notes'])
        parts.append('</ul>')
    return '\n'.join(parts) + '\n</html>\n'


def review_template(data):
    by_id = {c['id']: c for c in data['components']}
    grouped = {}
    for issue in data['issues']:
        grouped.setdefault(issue['component'], []).append(issue['message'])
    components = []
    reviews = []
    for identity, problems in sorted(grouped.items()):
        component = by_id.get(identity, {})
        for review_type, message in REVIEW_MESSAGES.items():
            if message in problems and component:
                reviews.append(dict(component=identity, version=component['version'], type=review_type,
                                    status='pending', evidenceSha256=component['reviewEvidenceSha256'],
                                    reviewer='', date='', reason='',
                                    documentSha256=sorted({d['sha256'] for d in component['documents']})))
        components.append(dict(
            id=identity, name=component.get('name', identity), version=component.get('version', 'UNKNOWN'),
            source='; '.join(component.get('sources', [])) or identity,
            reason='Manual evidence required for: ' + '; '.join(problems),
            declaredLicenses=component.get('declaredLicenses', []),
            attribution=component.get('attribution', []), documents=[], unresolved=problems,
            discoveryEvidence=dict(artifacts=component.get('artifacts', []),
                                   installations=component.get('installations', []),
                                   nativePayloads=component.get('nativePayloads', []),
                                   licenseMetadata=component.get('licenseMetadata', []),
                                   existingDocuments=component.get('documents', [])),
        ))
    return dict(schemaVersion=1, version=data['version'],
                instructions='Review each exact component/version. Add complete license/NOTICE evidence as '
                'repo-relative documents with exact SHA-256 and source provenance. Preserve license choices, '
                'exceptions and attribution. Remove an unresolved item only after obtaining and checking '
                'the missing evidence. This file neither approves IP nor publishes anything; merge reviewed '
                'entries into the maintained manual inventory. Discovery issues cannot be blanket suppressed.',
                components=components, reviews=reviews)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--repo', type=Path, default=Path(__file__).resolve().parents[1])
    parser.add_argument('--version', required=True)
    parser.add_argument('--npm-project', type=Path, required=True)
    parser.add_argument('--npm-modules', type=Path, required=True)
    java_input = parser.add_mutually_exclusive_group(required=True)
    java_input.add_argument('--java-lib-dir', type=Path)
    java_input.add_argument('--java-classpath', type=Path, help='Maven dependency:build-classpath output, separated by the host path separator')
    parser.add_argument('--maven-repo', type=Path)
    parser.add_argument('--manual', type=Path)
    parser.add_argument('--extra-content-dir', type=Path, action='append', default=[], help='Additional directories of external JARs and loose JavaScript to inventory')
    parser.add_argument('--output-dir', type=Path, required=True)
    mode = parser.add_mutually_exclusive_group()
    mode.add_argument('--allow-incomplete', action='store_true', help='Developer-only: retain warnings but return success')
    mode.add_argument('--strict', action='store_true', help='Fail unresolved evidence (the default)')
    args = parser.parse_args()
    try:
        repo = args.repo.resolve()
        collector = Collector(repo, args.version, (args.maven_repo or repo / '.m2_repo').resolve())
        collector.npm(args.npm_project.resolve(), args.npm_modules.absolute())
        if args.java_classpath:
            classpath = args.java_classpath.read_text(encoding='utf-8').strip()
            artifacts = [Path(entry).resolve() for entry in classpath.split(os.pathsep)] if classpath else []
            for path in artifacts:
                if not path.is_file():
                    raise ValueError('Missing runtime classpath artifact: ' + str(path))
                if path.suffix.lower() != '.jar':
                    raise ValueError('Runtime classpath artifact is not a JAR: ' + str(path))
            collector.jars(artifacts=artifacts)
        else:
            collector.jars(args.java_lib_dir.resolve())
        collector.vendors()
        for directory in args.extra_content_dir:
            collector.jars(directory.resolve())
        manual = args.manual or repo / 'scripts/third-party-notices.manual.json'
        if args.manual and not manual.is_file():
            raise ValueError('Explicit manual evidence file does not exist: ' + str(manual))
        collector.manual(manual if manual.is_file() else None)
        collector.loose_javascript(repo / 'tools/workbench/src/main/frontend/static')
        for directory in args.extra_content_dir:
            collector.loose_javascript(directory.resolve())
        data = collector.finish()
        args.output_dir.mkdir(parents=True, exist_ok=True)
        (args.output_dir / 'third-party-inventory.json').write_text(json.dumps(data, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')
        (args.output_dir / 'third-party-notices.html').write_text(render(data), encoding='utf-8')
        (args.output_dir / 'third-party-review-template.json').write_text(
            json.dumps(review_template(data), indent=2, ensure_ascii=False) + '\n', encoding='utf-8')
        print(f"Collected {len(data['components'])} components, {len(data['texts'])} distinct texts, {len(data['issues'])} unresolved items.")
        return 0 if data['complete'] or args.allow_incomplete else 1
    except (OSError, ValueError, KeyError, ET.ParseError, zipfile.BadZipFile) as error:
        print('Notice generation failed: ' + str(error), file=sys.stderr)
        return 2


if __name__ == '__main__':
    sys.exit(main())
