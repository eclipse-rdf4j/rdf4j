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
function checkIsSafeToDelete(event) {
    event.preventDefault();
    var id = $('#id').val();
    var feedback = $('#delete-feedback');
    $
        .ajax({
        dataType: 'json',
        url: 'delete',
        timeout: 5000,
        data: {
            checkSafe: id
        },
        error: function (jqXHR, textStatus, errorThrown) {
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
            feedback.text('');
            var form = $(event.target).closest('form').get(0);
            var confirmed = data.safe ? Promise.resolve(true) : workbench.confirmDialog.open({
                title: 'Delete a proxied repository?',
                body: 'Another repository proxies this one and stops working once it is deleted.',
                confirmLabel: 'Delete repository',
                danger: true
            });
            confirmed.then(function (submit) {
                if (submit && form) {
                    form.submit();
                }
            });
        }
    });
}
//# sourceMappingURL=delete.js.map