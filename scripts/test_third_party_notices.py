#!/usr/bin/env python3
# Copyright (c) 2026 Eclipse RDF4J contributors.
# SPDX-License-Identifier: BSD-3-Clause
"""Offline contract tests for release third-party notice generation."""
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
import zipfile

SCRIPT = Path(__file__).with_name('generate-third-party-notices.py')
MIT = 'MIT License\nCopyright 2026 Shared Authors\nPermission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction.\nTHE SOFTWARE IS PROVIDED "AS IS".\n'


class NoticeTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.project = self.root / 'frontend'
        self.project.mkdir()
        self.libs = self.root / 'lib'
        self.libs.mkdir()
        self.packages = {'': {'dependencies': {'direct': '1.0.0'}, 'devDependencies': {'build-only': '1.0.0'}}}
        self.package('direct', {'dependencies': {'transitive': '2.0.0'}}, copyright='Direct contributors')
        self.package('transitive', version='2.0.0', copyright='Transitive contributors')
        self.package('build-only', dev=True)
        (self.project / 'package.json').write_text(json.dumps(self.packages['']))
        (self.project / 'package-lock.json').write_text(json.dumps({'lockfileVersion': 3, 'packages': self.packages}))
        self.modules = self.root / 'modules.json'
        self.modules.write_text(json.dumps(['node_modules/direct/index.js', 'node_modules/transitive/index.js']))

    def package(self, name, extra=None, version='1.0.0', dev=False, copyright=None):
        path = self.project / 'node_modules' / name
        path.mkdir(parents=True)
        data = {'name': name, 'version': version, 'license': 'MIT', **(extra or {})}
        if copyright:
            data['copyright'] = copyright
        (path / 'package.json').write_text(json.dumps(data))
        (path / 'LICENSE').write_text(MIT)
        (path / 'index.js').write_text('export default 1;')
        self.packages['node_modules/' + name] = {**data, 'dev': dev, 'resolved': 'https://registry.example/' + name}
        return path

    def run_script(self, *extra):
        out = self.root / 'out'
        result = subprocess.run([sys.executable, str(SCRIPT), '--repo', str(self.root), '--version', '6.2.0-test',
                                 '--npm-project', str(self.project), '--npm-modules', str(self.modules),
                                 '--java-lib-dir', str(self.libs), '--output-dir', str(out), *extra],
                                capture_output=True, text=True)
        return result, out

    def test_transitives_dedupe_attribution_dev_filter_and_determinism(self):
        result, out = self.run_script()
        self.assertEqual(result.returncode, 0, result.stderr)
        data = json.loads((out / 'third-party-inventory.json').read_text())
        self.assertEqual([c['name'] for c in data['components']], ['direct', 'transitive'])
        self.assertEqual(len(data['texts']), 1)
        self.assertEqual(data['components'][0]['attribution'], ['Direct contributors'])
        self.assertEqual(data['components'][1]['attribution'], ['Transitive contributors'])
        html = (out / 'third-party-notices.html').read_text()
        self.assertIn('6.2.0-test', html)
        self.assertIn('Direct contributors', html)
        self.assertIn('Transitive contributors', html)
        before = {p.name: p.read_bytes() for p in out.iterdir()}
        self.run_script()
        self.assertEqual(before, {p.name: p.read_bytes() for p in out.iterdir()})

    def test_unknown_license_is_flagged_not_fabricated(self):
        path = self.project / 'node_modules/transitive'
        data = json.loads((path / 'package.json').read_text())
        del data['license']
        (path / 'package.json').write_text(json.dumps(data))
        (path / 'LICENSE').unlink()
        result, out = self.run_script()
        self.assertEqual(result.returncode, 1, result.stderr)
        data = json.loads((out / 'third-party-inventory.json').read_text())
        self.assertTrue(data['issues'])
        self.assertFalse(data['complete'])
        self.assertIn('transitive', str(data['issues']))

    def test_jars_include_all_transitive_artifacts_and_notices(self):
        for name in ('java-direct', 'java-transitive'):
            with zipfile.ZipFile(self.libs / (name + '.jar'), 'w') as jar:
                prefix = 'META-INF/maven/example/' + name + '/'
                jar.writestr(prefix + 'pom.properties', f'groupId=example\nartifactId={name}\nversion=1.2.3\n')
                jar.writestr(prefix + 'pom.xml', '<project><licenses><license><name>MIT</name></license></licenses></project>')
                jar.writestr('META-INF/LICENSE', MIT)
                jar.writestr('META-INF/NOTICE', 'Copyright ' + name)
        result, out = self.run_script()
        self.assertEqual(result.returncode, 0, result.stderr)
        data = json.loads((out / 'third-party-inventory.json').read_text())
        self.assertEqual(len(data['components']), 4)
        self.assertEqual(len(data['texts']), 3)
        self.assertIn('Copyright java-transitive', (out / 'third-party-notices.html').read_text())

    def test_preserves_license_expression_and_exception(self):
        path = self.project / 'node_modules/direct/package.json'
        data = json.loads(path.read_text())
        data['license'] = '(MIT OR Apache-2.0) AND GPL-2.0-only WITH Classpath-exception-2.0'
        path.write_text(json.dumps(data))
        result, out = self.run_script()
        inventory = json.loads((out / 'third-party-inventory.json').read_text())
        self.assertEqual(inventory['components'][0]['declaredLicenses'], [data['license']])
        self.assertEqual(result.returncode, 1)  # MIT text alone does not establish the other license texts.

    def test_unresolved_module_is_not_silently_omitted(self):
        self.modules.write_text(json.dumps(['node_modules/missing/index.js']))
        result, out = self.run_script()
        self.assertEqual(result.returncode, 1, result.stderr)
        self.assertIn('missing', (out / 'third-party-inventory.json').read_text())

    def test_manual_evidence_is_hash_anchored_and_preserves_source(self):
        import hashlib
        source = self.root / 'copied.js'
        source.write_text('/*\n' + MIT + '*/\n')
        manual = self.root / 'manual.json'
        manual.write_text(json.dumps({'schemaVersion': 1, 'components': [{
            'id': 'embedded:manual@1', 'name': 'Manual embedded component', 'version': '1',
            'reason': 'Source retains upstream notice', 'source': 'https://example.test/component/1',
            'declaredLicenses': ['MIT'], 'attribution': ['Manual contributor'],
            'documents': [{'path': 'copied.js', 'sha256': hashlib.sha256(source.read_bytes()).hexdigest(),
                           'startLine': 2, 'endLine': 5}]}]}))
        result, out = self.run_script('--manual', str(manual))
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn('copied.js#L2-L5', (out / 'third-party-inventory.json').read_text())
        source.write_text('changed')
        result, out = self.run_script('--manual', str(manual))
        self.assertEqual(result.returncode, 1, result.stderr)
        self.assertIn('hash mismatch', (out / 'third-party-inventory.json').read_text())

    def test_local_repository_identity_for_jar_without_embedded_pom(self):
        local = self.root / '.m2_repo/example/java-direct/1.2.3'
        local.mkdir(parents=True)
        with zipfile.ZipFile(self.libs / 'java-direct-1.2.3.jar', 'w') as jar:
            jar.writestr('META-INF/LICENSE', MIT)
        (local / 'java-direct-1.2.3.jar').write_bytes((self.libs / 'java-direct-1.2.3.jar').read_bytes())
        (local / 'java-direct-1.2.3.pom').write_text('<project><licenses><license><name>MIT</name></license></licenses></project>')
        result, out = self.run_script()
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn('maven:example:java-direct:1.2.3', (out / 'third-party-inventory.json').read_text())

    def test_explicit_extra_javascript_is_flagged(self):
        extra = self.root / 'external'
        extra.mkdir()
        (extra / 'unknown.js').write_text('thirdParty();')
        result, out = self.run_script('--extra-content-dir', str(extra))
        self.assertEqual(result.returncode, 1, result.stderr)
        self.assertIn('Uncatalogued JavaScript', (out / 'third-party-inventory.json').read_text())

    def test_modes_are_explicit_and_mutually_exclusive(self):
        self.modules.write_text(json.dumps(['node_modules/missing/index.js']))
        result, out = self.run_script('--allow-incomplete')
        self.assertEqual(result.returncode, 0)
        self.assertFalse(json.loads((out / 'third-party-inventory.json').read_text())['complete'])
        result, _ = self.run_script('--strict')
        self.assertEqual(result.returncode, 1)
        result, _ = self.run_script('--strict', '--allow-incomplete')
        self.assertEqual(result.returncode, 2)

    def test_owned_file_exclusion_requires_matching_file_and_license_hashes(self):
        import hashlib
        extra = self.root / 'external'
        extra.mkdir()
        source = extra / 'theme.js'
        source.write_text('projectTheme();')
        (self.root / 'LICENSE').write_text(MIT)
        manual = self.root / 'manual.json'
        manual.write_text(json.dumps({'schemaVersion': 1, 'ownedFiles': [{
            'path': 'external/theme.js', 'sha256': hashlib.sha256(source.read_bytes()).hexdigest(),
            'reason': 'Verified project-owned fixture', 'source': 'https://example.test/project/theme.js',
            'licensePath': 'LICENSE', 'licenseSha256': hashlib.sha256(MIT.encode()).hexdigest()}]}))
        result, out = self.run_script('--manual', str(manual), '--extra-content-dir', str(extra))
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(len(json.loads((out / 'third-party-inventory.json').read_text())['projectFiles']), 1)
        source.write_text('changed();')
        result, out = self.run_script('--manual', str(manual), '--extra-content-dir', str(extra))
        self.assertEqual(result.returncode, 1)

    def test_license_named_code_packages_are_not_legal_documents(self):
        with zipfile.ZipFile(self.libs / 'code.jar', 'w') as jar:
            jar.writestr('META-INF/maven/example/code/pom.properties', 'groupId=example\nartifactId=code\nversion=1\n')
            jar.writestr('META-INF/maven/example/code/pom.xml', '<project><licenses><license><name>MIT</name></license></licenses></project>')
            jar.writestr('META-INF/licenses/MIT.txt', MIT)
            jar.writestr('example/license/License.class', bytes([0xca, 0xfe, 0xba, 0xbe]))
        result, out = self.run_script()
        self.assertEqual(result.returncode, 0, result.stderr + (out / 'third-party-inventory.json').read_text())

    def test_classpath_input_rejects_missing_artifacts(self):
        classpath = self.root / 'runtime-classpath.txt'
        classpath.write_text(str(self.root / 'missing.jar'))
        result = subprocess.run([sys.executable, str(SCRIPT), '--repo', str(self.root), '--version', 'test',
                                 '--npm-project', str(self.project), '--npm-modules', str(self.modules),
                                 '--java-classpath', str(classpath), '--output-dir', str(self.root / 'out')],
                                capture_output=True, text=True)
        self.assertEqual(result.returncode, 2)
        self.assertIn('Missing runtime classpath artifact', result.stderr)

    def test_review_template_keeps_identity_missing_evidence_and_declared_terms(self):
        (self.project / 'node_modules/direct/LICENSE').unlink()
        result, out = self.run_script()
        self.assertEqual(result.returncode, 1)
        template = json.loads((out / 'third-party-review-template.json').read_text())
        direct = next(c for c in template['components'] if c['id'] == 'npm:direct@1.0.0')
        self.assertEqual(direct['version'], '1.0.0')
        self.assertEqual(direct['declaredLicenses'], ['MIT'])
        self.assertTrue(direct['discoveryEvidence']['artifacts'][0]['sha256'])
        self.assertEqual(direct['documents'], [])
        self.assertIn('No complete license text', str(direct['unresolved']))

    def test_classpath_scans_only_listed_jar_not_stale_siblings(self):
        import os
        paths = []
        for name in ('listed', 'stale'):
            path = self.libs / (name + '.jar')
            with zipfile.ZipFile(path, 'w') as jar:
                jar.writestr('META-INF/maven/example/' + name + '/pom.properties', 'groupId=example\nartifactId=' + name + '\nversion=1\n')
                jar.writestr('META-INF/maven/example/' + name + '/pom.xml', '<project><licenses><license><name>MIT</name></license></licenses></project>')
                jar.writestr('META-INF/LICENSE', MIT)
            paths.append(path)
        classpath = self.root / 'classpath.txt'
        classpath.write_text(str(paths[0]))
        out = self.root / 'out'
        result = subprocess.run([sys.executable, str(SCRIPT), '--repo', str(self.root), '--version', 'test',
                                 '--npm-project', str(self.project), '--npm-modules', str(self.modules),
                                 '--java-classpath', str(classpath), '--output-dir', str(out)], capture_output=True, text=True)
        self.assertEqual(result.returncode, 0, result.stderr)
        data = json.loads((out / 'third-party-inventory.json').read_text())
        self.assertTrue(any(c['name'] == 'example:listed' for c in data['components']))
        self.assertFalse(any(c['name'] == 'example:stale' for c in data['components']))

    def test_completed_review_resolves_only_exact_hash_bound_choice(self):
        path = self.project / 'node_modules/direct/package.json'
        package = json.loads(path.read_text())
        package['license'] = 'MIT OR Apache-2.0'
        path.write_text(json.dumps(package))
        result, out = self.run_script()
        self.assertEqual(result.returncode, 1)
        template = json.loads((out / 'third-party-review-template.json').read_text())
        review = next(r for r in template['reviews'] if r['component'] == 'npm:direct@1.0.0')
        self.assertEqual(review['status'], 'pending')
        review.update(status='completed', reviewer='Fixture reviewer', date='2026-11-04',
                      reason='Fixture reviewer selected MIT and verified the referenced text.')
        manual = self.root / 'review.json'
        manual.write_text(json.dumps({'schemaVersion': 1, 'reviews': [review]}))
        result, out = self.run_script('--manual', str(manual))
        self.assertEqual(result.returncode, 0, result.stderr)
        inventory = json.loads((out / 'third-party-inventory.json').read_text())
        self.assertEqual(inventory['components'][0]['coverageReviews'][0]['status'], 'completed')
        self.assertIn('Fixture reviewer', (out / 'third-party-notices.html').read_text())
        (self.project / 'node_modules/direct/index.js').write_text('changed source')
        result, out = self.run_script('--manual', str(manual))
        self.assertEqual(result.returncode, 1)
        self.assertIn('review evidence', (out / 'third-party-inventory.json').read_text())

    def test_reviews_reject_missing_reviewer_wildcards_and_unknown_type(self):
        path = self.project / 'node_modules/direct/package.json'
        package = json.loads(path.read_text())
        package['license'] = 'MIT OR Apache-2.0'
        path.write_text(json.dumps(package))
        _, out = self.run_script()
        template = json.loads((out / 'third-party-review-template.json').read_text())
        base = next(r for r in template['reviews'] if r['component'] == 'npm:direct@1.0.0')
        base.update(status='completed', reviewer='Fixture reviewer', date='2026-11-04', reason='Selected MIT')
        manual = self.root / 'review.json'
        for change in ({'reviewer': ''}, {'component': '*'}, {'type': '*'}, {'type': 'missing-license'}, {'documentSha256': []}):
            with self.subTest(change=change):
                manual.write_text(json.dumps({'schemaVersion': 1, 'reviews': [{**base, **change}]}))
                result, out = self.run_script('--manual', str(manual))
                self.assertEqual(result.returncode, 1)
                issues = json.loads((out / 'third-party-inventory.json').read_text())['issues']
                self.assertTrue(any('Compound license' in i['message'] for i in issues))
                self.assertTrue(any('Invalid completed review' in i['message'] for i in issues))
        (self.project / 'node_modules/direct/LICENSE').unlink()
        manual.write_text(json.dumps({'schemaVersion': 1, 'reviews': [base]}))
        result, out = self.run_script('--manual', str(manual))
        self.assertEqual(result.returncode, 1)
        self.assertIn('No complete license text', (out / 'third-party-inventory.json').read_text())

    def test_missing_npm_inventory_emits_incomplete_developer_and_strict_reports(self):
        self.modules.unlink()
        for mode, expected in (('--allow-incomplete', 0), ('--strict', 1)):
            with self.subTest(mode=mode):
                result, out = self.run_script(mode)
                self.assertEqual(result.returncode, expected, result.stderr)
                inventory = json.loads((out / 'third-party-inventory.json').read_text())
                self.assertFalse(inventory['complete'])
                self.assertEqual(inventory['components'], [])
                self.assertIn('Missing npm bundle inventory', str(inventory['issues']))
                self.assertIn('Missing npm bundle inventory', (out / 'third-party-notices.html').read_text())

    def test_malformed_or_nonfile_npm_inventory_is_not_relaxed(self):
        self.modules.write_text('{ invalid JSON')
        result, _ = self.run_script('--allow-incomplete')
        self.assertEqual(result.returncode, 2)
        self.modules.unlink()
        self.modules.mkdir()
        result, _ = self.run_script('--allow-incomplete')
        self.assertEqual(result.returncode, 2)

    def test_duplicate_npm_installations_keep_all_evidence_and_hash_binding(self):
        import hashlib
        nested = self.project / 'node_modules/other/node_modules/direct'
        nested.mkdir(parents=True)
        (nested / 'package.json').write_text(json.dumps({'name': 'direct', 'version': '1.0.0', 'license': 'MIT', 'copyright': 'Nested contributors'}))
        (nested / 'LICENSE').write_text(MIT)
        (nested / 'index.js').write_text('nested implementation')
        lock_path = self.project / 'package-lock.json'
        lock = json.loads(lock_path.read_text())
        lock['packages']['node_modules/direct']['integrity'] = 'sha512-root'
        lock['packages']['node_modules/other/node_modules/direct'] = {'name': 'direct', 'version': '1.0.0', 'license': 'MIT', 'integrity': 'sha512-nested', 'resolved': 'https://registry.example/nested-direct'}
        lock_path.write_text(json.dumps(lock))
        self.modules.write_text(json.dumps(['node_modules/direct/index.js', 'node_modules/other/node_modules/direct/index.js']))
        result, out = self.run_script()
        self.assertEqual(result.returncode, 0, result.stderr)
        component = json.loads((out / 'third-party-inventory.json').read_text())['components'][0]
        self.assertEqual(len(component['artifacts']), 2)
        self.assertEqual(len(component['installations']), 2)
        self.assertEqual({i['integrity'] for i in component['installations']}, {'sha512-root', 'sha512-nested'})
        self.assertEqual(component['attribution'], ['Direct contributors', 'Nested contributors'])
        before = component['reviewEvidenceSha256']
        # The lexically first installation previously disappeared from evidence.
        (self.project / 'node_modules/direct/index.js').write_text('modified root copy')
        _, out = self.run_script()
        changed = json.loads((out / 'third-party-inventory.json').read_text())['components'][0]
        self.assertNotEqual(before, changed['reviewEvidenceSha256'])
        before_nested = changed['reviewEvidenceSha256']
        (nested / 'index.js').write_text('modified nested copy')
        _, out = self.run_script()
        changed = json.loads((out / 'third-party-inventory.json').read_text())['components'][0]
        self.assertNotEqual(before_nested, changed['reviewEvidenceSha256'])
        package = json.loads((nested / 'package.json').read_text())
        package['license'] = 'BSD-3-Clause'
        (nested / 'package.json').write_text(json.dumps(package))
        result, out = self.run_script('--allow-incomplete')
        changed = json.loads((out / 'third-party-inventory.json').read_text())['components'][0]
        self.assertEqual(changed['declaredLicenses'], ['BSD-3-Clause', 'MIT'])
        self.assertEqual(changed['installations'][0]['declaredLicenses'], ['MIT'])
        self.assertEqual(changed['installations'][1]['declaredLicenses'], ['BSD-3-Clause'])

    def test_extra_jar_is_discovered_before_manual_evidence_and_conditionals(self):
        import hashlib
        extra = self.root / 'external'
        extra.mkdir()
        with zipfile.ZipFile(extra / 'a.jar', 'w') as jar:
            jar.writestr('META-INF/maven/g/a/pom.properties', 'groupId=g\nartifactId=a\nversion=1\n')
        license_path = self.root / 'external-license.txt'
        license_path.write_text(MIT)
        document = {'path': 'external-license.txt', 'sha256': hashlib.sha256(license_path.read_bytes()).hexdigest()}
        manual = self.root / 'manual.json'
        manual.write_text(json.dumps({'schemaVersion': 1, 'components': [
            {'id': 'maven:g:a:1', 'name': 'g:a', 'version': '1', 'source': 'https://example.test/g/a/1',
             'reason': 'Verified external archive license', 'declaredLicenses': ['MIT'], 'documents': [document]},
            {'id': 'embedded:extra@1', 'name': 'extra', 'version': '1', 'source': 'https://example.test/extra/1',
             'reason': 'Embedded external archive component', 'includedWhenComponent': 'maven:g:a:1',
             'declaredLicenses': ['MIT'], 'documents': [document]}]}))
        result, out = self.run_script('--extra-content-dir', str(extra), '--manual', str(manual))
        self.assertEqual(result.returncode, 0, result.stderr)
        components = json.loads((out / 'third-party-inventory.json').read_text())['components']
        self.assertTrue(any(c['id'] == 'embedded:extra@1' for c in components))
        self.assertEqual(next(c for c in components if c['id'] == 'maven:g:a:1')['declaredLicenses'], ['MIT'])

    def native_jar(self, group='example', payload=b'native-v1'):
        path = self.libs / 'native.jar'
        names = ['linux/libx.so', 'linux/libx.so.1.2', 'windows/x.DLL', 'mac/libx.dylib',
                 'mac/libx.jnilib', 'web/x.wasm', 'static/libx.a', 'windows/x.lib']
        with zipfile.ZipFile(path, 'w') as jar:
            prefix = 'META-INF/maven/' + group + '/native/'
            jar.writestr(prefix + 'pom.properties', 'groupId=' + group + '\nartifactId=native\nversion=1\n')
            jar.writestr(prefix + 'pom.xml', '<project><licenses><license><name>MIT</name></license></licenses></project>')
            jar.writestr('META-INF/LICENSE', MIT)
            jar.writestr('example/Normal.class', bytes([0xca, 0xfe, 0xba, 0xbe]))
            for name in names:
                jar.writestr(name, payload)
        return names

    def test_native_payload_detection_and_hash_bound_coverage_review(self):
        names = self.native_jar()
        result, out = self.run_script()
        self.assertEqual(result.returncode, 1)
        data = json.loads((out / 'third-party-inventory.json').read_text())
        component = next(c for c in data['components'] if c['id'] == 'maven:example:native:1')
        self.assertEqual({p['path'] for p in component['nativePayloads']}, set(names))
        self.assertTrue(all(p['sha256'] and p['artifactSha256'] for p in component['nativePayloads']))
        self.assertIn('libx.so.1.2', (out / 'third-party-notices.html').read_text())
        review = next(r for r in json.loads((out / 'third-party-review-template.json').read_text())['reviews']
                      if r['type'] == 'native-components')
        self.assertEqual(review['status'], 'pending')
        manual = self.root / 'review.json'
        manual.write_text(json.dumps({'schemaVersion': 1, 'reviews': [review]}))
        result, _ = self.run_script('--manual', str(manual))
        self.assertEqual(result.returncode, 1)
        review.update(status='completed', reviewer='Fixture reviewer', date='2026-11-04',
                      reason='Fixture review of all named native payload licenses and attribution.')
        manual.write_text(json.dumps({'schemaVersion': 1, 'reviews': [review]}))
        result, out = self.run_script('--manual', str(manual))
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn('Fixture reviewer', (out / 'third-party-notices.html').read_text())
        self.native_jar(payload=b'native-v2')
        result, out = self.run_script('--manual', str(manual))
        self.assertEqual(result.returncode, 1)
        self.assertIn('Invalid completed review evidence', (out / 'third-party-inventory.json').read_text())

    def test_project_jar_native_payload_is_not_hidden_by_project_classification(self):
        self.native_jar(group='org.eclipse.rdf4j')
        (self.root / 'LICENSE').write_text(MIT)
        result, out = self.run_script()
        self.assertEqual(result.returncode, 1)
        component = next(c for c in json.loads((out / 'third-party-inventory.json').read_text())['components']
                         if c['id'] == 'maven:org.eclipse.rdf4j:native:1')
        self.assertEqual(component['role'], 'project')
        self.assertEqual(len(component['nativePayloads']), 8)
        self.assertIn('native binary payloads', (out / 'third-party-inventory.json').read_text())

    def test_viz_runtime_supplement_preserves_exact_license_evidence(self):
        repo = SCRIPT.parent.parent
        manual = json.loads((repo / 'scripts/third-party-notices.manual.json').read_text())
        runtime = next((c for c in manual['components']
                        if c['id'] == 'embedded:emscripten-runtime@6.0.10'), None)
        self.assertIsNotNone(runtime, 'Viz ships Emscripten JS/WASM runtime support, not only Graphviz and Expat')
        self.assertEqual(runtime['includedWhenComponent'], 'npm:@viz-js/viz@3.31.0')
        self.assertTrue(runtime.get('unresolved'), 'Unknown linked-runtime membership must remain explicit')
        paths = {Path(d['path']).name for d in runtime['documents']}
        self.assertTrue({'LICENSE', 'musl-COPYRIGHT', 'libcxx-LICENSE.TXT',
                         'libcxxabi-LICENSE.TXT', 'compiler-rt-LICENSE.TXT'} <= paths)
        import hashlib
        for document in runtime['documents']:
            data = (repo / document['path']).read_bytes()
            self.assertEqual(hashlib.sha256(data).hexdigest(), document['sha256'])

    def test_html_escapes_upstream_text(self):
        (self.project / 'node_modules/direct/LICENSE').write_text(MIT + '<script>alert(1)</script>')
        result, out = self.run_script()
        self.assertNotIn('<script>', (out / 'third-party-notices.html').read_text())


if __name__ == '__main__':
    unittest.main()
