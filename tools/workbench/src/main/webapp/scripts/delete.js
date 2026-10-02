/// <reference path="template.ts" />
/// <reference path="jquery.d.ts" />
// WARNING: Do not edit the *.js version of this file. Instead, always edit the
// corresponding *.ts source in the ts subfolder, and then invoke the
// compileTypescript.sh bash script to generate new *.js and *.js.map files.
/**
 * Invoked by the "Delete" button on the rendered delete form. Checks with the
 * DeleteServlet whether the given ID has been proxied, giving a chance to back
 * out if it is.
 */
function checkIsSafeToDelete(event, ownsPage) {
    event.preventDefault();
    var form = $(event.target).closest('form').get(0);
    var owner = workbench.captureFormOwner(form);
    var owns = function () { return owner.isCurrent() && (!ownsPage || ownsPage()); };
    if (!owns()) {
        return;
    }
    var id = $('#id').val();
    var feedback = $('#delete-feedback');
    $
        .ajax({
        dataType: 'json',
        url: owner.url ? new URL('delete', owner.url).href : 'delete',
        timeout: 5000,
        data: {
            checkSafe: id
        },
        error: function (jqXHR, textStatus, errorThrown) {
            if (!owns()) {
                return;
            }
            if (textStatus == 'timeout') {
                feedback
                    .text('The server seems unresponsive. Delete request not sent.');
            }
            else {
                feedback
                    .text('There is a problem with the server. Delete request not sent. Error Type = '
                    + textStatus
                    + ', HTTP Status Text = "'
                    + errorThrown + '"');
            }
        },
        success: function (data) {
            if (!owns()) {
                return;
            }
            feedback.text('');
            var body = 'This permanently deletes ' + id + ' and all of its statements.';
            if (!data.safe) {
                body += ' Another repository proxies this one and stops working once it is deleted.';
            }
            workbench.confirmDialog.open({
                title: 'Delete repository ' + id + '?',
                body: body,
                confirmLabel: 'Delete repository',
                danger: true,
                requireText: String(id)
            }).then(function (submit) {
                if (submit && owns()) {
                    workbench.submitForm(form);
                }
            });
        }
    });
}
//# sourceMappingURL=delete.js.map