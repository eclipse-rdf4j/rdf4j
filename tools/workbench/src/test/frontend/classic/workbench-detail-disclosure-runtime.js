/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

function installDetailDisclosureTemplateRuntime(workbench) {
	workbench.detailDisclosure = workbench.detailDisclosure || {};
	workbench.detailDisclosure.render = (html, options, content) => {
		const template = html`
		<div id="__owner_id__" class="__owner_class__"
			data-workbench-detail-disclosure="true" ?hidden=${!!options.hidden}>
			<button id="__toggle_id__" type="button"
				class="__toggle_class__"
				aria-controls=${options.panelId} aria-expanded=${options.expanded ? 'true' : 'false'}
				aria-label=${options.accessibleName || options.label} ?hidden=${!!options.toggleHidden}>
				<span class="workbench-disclosure__toggle-label">${options.label}</span>
				<svg class="workbench-action-icon workbench-action-icon--chevron workbench-disclosure-chevron"
					data-workbench-icon="chevron" viewBox="0 0 24 24" aria-hidden="true"></svg>
			</button>
			<div id="__panel_id__" class="__panel_class__"
				role="__panel_role__" aria-labelledby="__toggle_id__"
				?hidden=${!options.expanded}>
				<div class="__content_class__">${content}</div>
			</div>
		</div>`;
		template.strings = template.strings.map(value => value
			.replace('__owner_id__', options.id || '')
			.replace('__owner_class__', 'workbench-disclosure ' + (options.ownerClass || ''))
			.replace('__toggle_id__', options.toggleId)
			.replace('__toggle_class__', 'workbench-disclosure__toggle ' + (options.toggleClass || ''))
			.replace('__panel_id__', options.panelId)
			.replace('__panel_class__', 'workbench-disclosure__panel ' + (options.panelClass || ''))
			.replace('__panel_role__', options.panelRole || 'region')
			.replace('__content_class__', 'workbench-disclosure__content ' + (options.contentClass || '')));
		template.detailDisclosureOptions = options;
		return template;
	};
	workbench.detailDisclosure.renderSeparated = (html, options, content) => {
		const owner = html`<div id="__owner_id__" class="__owner_class__"
			data-workbench-detail-disclosure="true" ?hidden=${!!options.hidden}>
			<button id="__toggle_id__" type="button" class="__toggle_class__"
				aria-controls=${options.panelId} aria-expanded=${options.expanded ? 'true' : 'false'}
				aria-label=${options.accessibleName || options.label} ?hidden=${!!options.toggleHidden}>
				<span class="workbench-disclosure__toggle-label">${options.label}</span>
				<svg class="workbench-action-icon workbench-action-icon--chevron workbench-disclosure-chevron"
					data-workbench-icon="chevron" viewBox="0 0 24 24" aria-hidden="true"></svg>
			</button></div>`;
		owner.strings = owner.strings.map(value => value
			.replace('__owner_id__', options.id || '')
			.replace('__owner_class__', 'workbench-disclosure ' + (options.ownerClass || ''))
			.replace('__toggle_id__', options.toggleId)
			.replace('__toggle_class__', 'workbench-disclosure__toggle ' + (options.toggleClass || '')));
		owner.detailDisclosureOptions = options;

		const panel = html`<div id="__panel_id__" class="__panel_class__"
			role="__panel_role__" aria-labelledby="__toggle_id__" ?hidden=${!options.expanded}>
			<div class="__content_class__">${content}</div></div>`;
		panel.strings = panel.strings.map(value => value
			.replace('__panel_id__', options.panelId)
			.replace('__panel_class__', 'workbench-disclosure__panel ' + (options.panelClass || ''))
			.replace('__panel_role__', options.panelRole || 'region')
			.replace('__toggle_id__', options.toggleId)
			.replace('__content_class__', 'workbench-disclosure__content ' + (options.contentClass || '')));
		panel.detailDisclosureOptions = options;
		return { owner, panel };
	};
}

module.exports = { installDetailDisclosureTemplateRuntime };
