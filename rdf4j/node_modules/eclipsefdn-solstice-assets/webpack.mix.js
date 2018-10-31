/*!
 * Copyright (c) 2018 Eclipse Foundation, Inc.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 * 
 * Contributors:
 *   Christopher Guindon <chris.guindon@eclipse-foundation.org>
 * 
 * SPDX-License-Identifier: EPL-2.0
*/

let mix = require('laravel-mix');
mix.options({uglify: {uglifyOptions: {compress: false, output: {comments: true}}}});
mix.setPublicPath('dist');
mix.setResourceRoot('../');

// Default CSS
mix.less('less/quicksilver/styles.less', 'dist/css/quicksilver.css');
mix.less('less/quicksilver/jakarta/styles.less', 'dist/css/jakarta.css');
mix.less('less/quicksilver/eclipse-ide/styles.less', 'dist/css/eclipse-ide.css');
mix.less('less/solstice/_barebone/styles.less', 'dist/css/barebone.css');
mix.less('less/solstice/_barebone/footer.less', 'dist/css/barebone-footer.css');
mix.less('less/solstice/table.less', 'dist/css/table.css');
mix.less('less/solstice/locationtech/styles.less', 'dist/css/locationtech.css');
mix.less('less/solstice/locationtech/barebone.less', 'dist/css/locationtech-barebone.css');
mix.less('less/solstice/polarsys/styles.less', 'dist/css/polarsys.css');
mix.less('less/solstice/polarsys/barebone.less', 'dist/css/polarsys-barebone.css');
mix.less('less/solstice/styles.less', 'dist/css/solstice.css');

// Copy cookieconsent files
 mix.copy('node_modules/cookieconsent/build/cookieconsent.min.css', 'dist/css/vendor/cookieconsent.min.css');
 mix.copy('node_modules/cookieconsent/src/cookieconsent.js', 'dist/js/vendor/cookieconsent.min.js');

// Drupal sites
mix.less('less/solstice/drupal.less', 'dist/css/drupal-solstice.css');

// JavaScript
mix.scripts([
    './node_modules/jquery/dist/jquery.min.js',
    './node_modules/bootstrap/dist/js/bootstrap.min.js',
    './node_modules/jquery-match-height/dist/jquery.matchHeight-min.js',
    './node_modules/feather-icons/dist/feather.min.js',
    './node_modules/cookieconsent/src/cookieconsent.js',
    'js/eclipsefdn.videos.js',
    'js/solstice.cookieconsent.js',
    'js/solstice.cookies.js',
    'js/solstice.js',
    'js/solstice.donate.js'
], 'dist/js/solstice.js');