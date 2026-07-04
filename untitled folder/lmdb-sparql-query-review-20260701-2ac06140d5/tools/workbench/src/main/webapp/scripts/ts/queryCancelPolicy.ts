module workbench {

    export module queryCancelPolicy {

        export interface ExplainControlIds {
            buttonId: string;
            spinnerId: string;
            cancelId: string;
        }

        export function getExplainControlIds(buttonId?: string): ExplainControlIds {
            if (buttonId === 'rerun-explanation') {
                return {
                    buttonId: 'rerun-explanation',
                    spinnerId: 'rerun-explanation-spinner',
                    cancelId: 'rerun-explanation-cancel'
                };
            }
            if (buttonId === 'explain-trigger') {
                return {
                    buttonId: 'explain-trigger',
                    spinnerId: 'explain-trigger-spinner',
                    cancelId: 'explain-trigger-cancel'
                };
            }
            return {
                buttonId: '',
                spinnerId: '',
                cancelId: ''
            };
        }

        export function getExplainCancelAction(
            comparePendingRequestCount: number,
            hasPrimaryExplainRequest: boolean
        ): string {
            if (comparePendingRequestCount > 0) {
                return 'compare';
            }
            if (hasPrimaryExplainRequest) {
                return 'primary';
            }
            return 'none';
        }

        export function shouldShowComparePrimaryWaitState(
            buttonId: string,
            comparePendingRequestCount: number,
            hasActiveCompareRequest: boolean,
            spinnerTargetId: string
        ): boolean {
            var controlIds = getExplainControlIds(buttonId);
            return !!controlIds.buttonId
                && comparePendingRequestCount > 0
                && hasActiveCompareRequest
                && spinnerTargetId === controlIds.buttonId;
        }
    }
}

declare var module: any;

if (typeof module !== 'undefined' && module.exports) {
    module.exports = workbench.queryCancelPolicy;
}
