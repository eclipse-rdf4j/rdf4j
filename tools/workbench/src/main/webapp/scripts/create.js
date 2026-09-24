/// <reference path="template.ts" />
/// <reference path="jquery.d.ts" />
// WARNING: Do not edit the *.js version of this file. Instead, always edit the
// corresponding *.ts source in the ts subfolder, and then invoke the
// compileTypescript.sh bash script to generate new *.js and *.js.map files.
var workbench;
(function (workbench) {
    var create;
    (function (create) {
        function findFieldByRole(role, fallbackSelector) {
            var field = $('[data-field-role="' + role + '"]');
            return field.length ? field : $(fallbackSelector);
        }
        create.findFieldByRole = findFieldByRole;
        create.id = findFieldByRole('repository-id', '#id');
        create.title = findFieldByRole('repository-title', '#title');
    })(create = workbench.create || (workbench.create = {}));
})(workbench || (workbench = {}));
/**
 * Invoked by the "Create" button on the form for all but
 * create-federate.xsl. Checks with the InfoServlet for the user-provided id
 * for the existence of the id already, giving a chance to back out if it
 * does. Depends on the current behavior of getting a failure response (500
 * Internal Server Error at present), when the ID does not exist.
 */
function checkOverwrite() {
    var submit = false;
    var id = workbench.create.id.val();
    $.ajax({
        url: '../' + id + '/info',
        success: function () {
            submit = confirm('WARNING: You are about to overwrite the ' +
                'configuration of an existing repository!');
        },
        statusCode: {
            500: function () {
                submit = true;
            }
        },
        complete: function (xhr, status) {
            if (submit && !id.match(/^[a-z0-9._-]+$/)) {
                submit = confirm('WARNING: There are potentially incompatible ' +
                    'characters in the repository id.');
            }
            if (submit) {
                $("form[action='create']").submit();
            }
        }
    });
}
workbench.addLoad(function createPageLoaded() {
    /**
     * Keep required identity and endpoint fields visible while moving the
     * lower-frequency repository tuning fields into one native disclosure. The
     * rows are moved rather than cloned so every existing input name, value and
     * form submission remains unchanged.
     */
    function installAdvancedFields() {
        var table = document.querySelector("form[action='create'] table.dataentry");
        if (!table || document.querySelector('details.workbench-advanced')) {
            return;
        }
        var body = table.tBodies.length ? table.tBodies[0] : null;
        if (!body || body.rows.length < 3) {
            return;
        }
        var rowsToMove = [];
        for (var i = 1; i < body.rows.length - 1; i++) {
            var row = body.rows[i];
            var roleControl = row.querySelector('[data-field-role], [data-config-property]');
            var role = roleControl ? roleControl.getAttribute('data-field-role') || '' : '';
            var property = roleControl ? roleControl.getAttribute('data-config-property') || '' : '';
            var fieldId = roleControl ? roleControl.id : '';
            var required = role === 'repository-id' || role === 'repository-title'
                || role === 'federation-member'
                || property === 'config:http.url'
                || property === 'config:sparql.queryEndpoint'
                || property === 'config:sparql.updateEndpoint'
                || property === 'config:cgqi.queryLanguage'
                || fieldId === 'sp_text';
            if (!required) {
                rowsToMove.push(row);
            }
        }
        if (!rowsToMove.length) {
            return;
        }
        var details = document.createElement('details');
        details.className = 'workbench-advanced';
        var summary = document.createElement('summary');
        summary.textContent = table.getAttribute('data-advanced-label') || 'Advanced settings';
        details.appendChild(summary);
        var advancedTable = document.createElement('table');
        advancedTable.className = 'dataentry workbench-advanced-fields';
        var advancedBody = document.createElement('tbody');
        advancedTable.appendChild(advancedBody);
        details.appendChild(advancedTable);
        for (var j = 0; j < rowsToMove.length; j++) {
            advancedBody.appendChild(rowsToMove[j]);
        }
        var actionRow = body.rows[body.rows.length - 1];
        body.removeChild(actionRow);
        table.parentNode.insertBefore(details, table.nextSibling);
        var actionTable = table.cloneNode(false);
        var actionBody = document.createElement('tbody');
        actionBody.appendChild(actionRow);
        actionTable.appendChild(actionBody);
        details.parentNode.insertBefore(actionTable, details.nextSibling);
    }
    // The script is loaded after the form markup, so this runs before the
    // window load hook and is available to keyboard and automated clients at
    // DOMContentLoaded as well.
    installAdvancedFields();
    /**
     * Disables the create button if the id field doesn't have any text.
     */
    function disableCreateIfEmptyId() {
        $('input#create').prop('disabled', !(/.+/.test(workbench.create.id.val())));
    }
    // Populate parameters
    var elements = workbench.getQueryStringElements();
    for (var i = 0; elements.length - i; i++) {
        var pair = elements[i].split('=');
        var value = decodeURIComponent(pair[1]).replace(/\+/g, ' ');
        if (pair[0] == 'id') {
            workbench.create.id.val(value);
        }
        if (pair[0] == 'title') {
            workbench.create.title.val(value);
        }
    }
    disableCreateIfEmptyId();
    // Calls another function with a delay of 0 msec. (Workaround for 
    // annoying browser behavior.)
    workbench.create.id.on('keydown paste cut', function () {
        setTimeout(disableCreateIfEmptyId, 0);
    });
});
//# sourceMappingURL=create.js.map