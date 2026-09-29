const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const scripts = process.env.WORKBENCH_SCRIPT_DIR
	|| path.resolve(__dirname, '../../tools/workbench/src/main/webapp/scripts');

function loadExportMarkup({ search = '', cookie = '', configuredFormat = 'application/n-quads' } = {}) {
	const window = { location: { search } };
	const context = vm.createContext({
		URLSearchParams,
		window,
		workbench: {
			getCookie(name) {
				return name === 'Accept' ? cookie : '';
			}
		}
	});
	const source = fs.readFileSync(path.join(scripts, 'workbenchViews.js'), 'utf8');
	vm.runInContext(source, context, { filename: 'workbenchViews.js' });
	const runtime = {
		nothing: '',
		html(strings, ...values) { return { strings: Array.from(strings), values }; },
		render() {}
	};
	const template = context.workbench.views.pageTemplate({
		viewId: 'export', vars: [], rows: [], metadata: {}
	}, {
		basePath: '/workbench',
		repositoryId: 'repo-1',
		workbench: {
			defaults: {
				'default-Accept': 'application/rdf+xml',
				'default-export-format': configuredFormat
			},
			graphDownloadFormats: [
				'application/rdf+xml RDF/XML',
				'application/n-triples N-Triples',
				'application/n-quads N-Quads'
			]
		}
	}, runtime);
	return flatten(template);
}

function flatten(value) {
	if (Array.isArray(value)) {
		return value.map(flatten).join('');
	}
	if (value && Array.isArray(value.strings) && Array.isArray(value.values)) {
		let result = '';
		for (let index = 0; index < value.strings.length; index++) {
			result += value.strings[index];
			if (index < value.values.length) {
				result += flatten(value.values[index]);
			}
		}
		return result;
	}
	return value === null || typeof value === 'undefined' ? '' : String(value);
}

function selectedFormats(markup) {
	const select = markup.match(/<select id="Accept"[\s\S]*?<\/select>/);
	assert.ok(select, 'the export-format selector should be present');
	return Array.from(select[0].matchAll(/<option value=([^\s>]+) \?selected=(true|false)>/g))
		.filter((match) => match[2] === 'true')
		.map((match) => match[1]);
}

test('Export defaults to the configured N-Quads format over generic RDF/XML', () => {
	assert.deepEqual(selectedFormats(loadExportMarkup()), ['application/n-quads']);
});

test('an explicit Accept cookie takes precedence over the configured export format', () => {
	assert.deepEqual(selectedFormats(loadExportMarkup({ cookie: 'application/n-triples' })), ['application/n-triples']);
});

test('an N-Quads Accept cookie remains selected over a different configured export format', () => {
	assert.deepEqual(selectedFormats(loadExportMarkup({
		cookie: 'application/n-quads',
		configuredFormat: 'application/n-triples'
	})), ['application/n-quads']);
});

test('an explicit Accept URL parameter takes precedence over the cookie', () => {
	assert.deepEqual(selectedFormats(loadExportMarkup({
		search: '?Accept=application%2Fn-quads',
		cookie: 'application/n-triples',
		configuredFormat: 'application/n-triples'
	})), ['application/n-quads']);
});
