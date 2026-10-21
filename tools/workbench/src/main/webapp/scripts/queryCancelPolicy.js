var workbench;
(function (workbench) {
    var queryCancelPolicy;
    (function (queryCancelPolicy) {
        function getExplainControlIds(buttonId) {
            if (buttonId === 'rerun-explanation') {
                return {
                    buttonId: 'rerun-explanation',
                    cancelId: 'rerun-explanation-cancel'
                };
            }
            if (buttonId === 'explain-trigger') {
                return {
                    buttonId: 'explain-trigger',
                    cancelId: 'explain-trigger-cancel'
                };
            }
            return {
                buttonId: '',
                cancelId: ''
            };
        }
        queryCancelPolicy.getExplainControlIds = getExplainControlIds;
        function getExplainCancelAction(comparePendingRequestCount, hasPrimaryExplainRequest) {
            if (comparePendingRequestCount > 0) {
                return 'compare';
            }
            if (hasPrimaryExplainRequest) {
                return 'primary';
            }
            return 'none';
        }
        queryCancelPolicy.getExplainCancelAction = getExplainCancelAction;
        function shouldShowComparePrimaryWaitState(buttonId, comparePendingRequestCount, hasActiveCompareRequest, spinnerTargetId) {
            var controlIds = getExplainControlIds(buttonId);
            return !!controlIds.buttonId
                && comparePendingRequestCount > 0
                && hasActiveCompareRequest
                && spinnerTargetId === controlIds.buttonId;
        }
        queryCancelPolicy.shouldShowComparePrimaryWaitState = shouldShowComparePrimaryWaitState;
    })(queryCancelPolicy = workbench.queryCancelPolicy || (workbench.queryCancelPolicy = {}));
})(workbench || (workbench = {}));
if (typeof module !== 'undefined' && module.exports) {
    module.exports = workbench.queryCancelPolicy;
}
//# sourceMappingURL=queryCancelPolicy.js.map