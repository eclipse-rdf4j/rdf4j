/// <reference path="template.ts" />
/// <reference path="jquery.d.ts" />
// WARNING: Do not edit the *.js version of this file. Instead, always edit the
// corresponding *.ts source in the ts subfolder, and then invoke the
// compileTypescript.sh bash script to generate new *.js and *.js.map files.
var pendingRepositoryDeletes = new WeakSet();
/**
 * Invoked by the "Delete" button on the rendered delete form. Checks with the
 * DeleteServlet whether the given ID has been proxied, giving a chance to back
 * out if it is.
 */
function checkIsSafeToDelete(event, ownsPage) {
    event.preventDefault();
    var form = $(event.target).closest('form').get(0);
    if (!form || pendingRepositoryDeletes.has(form)) {
        return;
    }
    var owner = workbench.captureFormOwner(form);
    var owns = function () { return owner.isCurrent() && (!ownsPage || ownsPage()); };
    if (!owns()) {
        return;
    }
    var selected = form.querySelector('#id');
    var password = form.querySelector('#delete-password');
    var action = form.querySelector('button[type="submit"]');
    var id = selected ? selected.value : '';
    if (!id || password && !password.value) {
        return;
    }
    var feedback = $(form.querySelector('#delete-feedback'));
    var unchanged = function () { return owns() && selected.value === id; };
    var release = function () {
        pendingRepositoryDeletes.delete(form);
        if (owns() && action) {
            action.disabled = !selected.value || !!password && !password.value;
        }
    };
    pendingRepositoryDeletes.add(form);
    // Keep the opener focusable while checking; the pending set prevents duplicate requests and dialogs.
    $
        .ajax({
        dataType: 'json',
        url: owner.url ? new URL('delete', owner.url).href : 'delete',
        timeout: 5000,
        data: {
            checkSafe: id
        },
        error: function (jqXHR, textStatus, errorThrown) {
            release();
            if (!unchanged()) {
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
            if (!unchanged()) {
                release();
                return;
            }
            feedback.text('');
            // The modal makes the form inert. Keep its opener focusable when Cancel restores focus.
            if (action) {
                action.disabled = false;
            }
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
                if (!submit || !unchanged()) {
                    release();
                    return;
                }
                try {
                    // submitForm serializes the live field synchronously; secrets never enter a page model or draft.
                    var sent = workbench.submitForm(form);
                    if (password) {
                        password.value = '';
                    }
                    if (action) {
                        action.disabled = !!password;
                    }
                    Promise.resolve(sent).then(release, release);
                }
                catch (error) {
                    if (password) {
                        password.value = '';
                    }
                    release();
                    if (owns()) {
                        feedback.text('The delete request could not be sent. Try again.');
                    }
                }
            }, release);
        }
    });
}
//# sourceMappingURL=delete.js.map