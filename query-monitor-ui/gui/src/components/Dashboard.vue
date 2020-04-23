<template>
    <div class="col-md-12">

        <div class="col-md-6">
            <div class="card">
                <div class="card-header">
                    <h3>Query Runners - {{runnerGrid.rows.length}}</h3>
                </div>
                <div class="card-body">
                    <GridBase :columns="runnerGrid.columns"
                        :rows="runnerGrid.rows"
                        :gridOptions="runnerGrid.gridOptions"
                        :durationDates="options.durationDates"
                        updateKey="name"
                        size="full">
                    </GridBase>
                </div>
            </div>
        </div>

        <div class="col-md-6">
            <div class="card">
                <div class="card-header">
                    <h3>Executors - {{executorGrid.rows.length}}</h3>
                </div>
                <div class="card-body">
                    <GridBase :columns="executorGrid.columns"
                        :rows="executorGrid.rows"
                        :gridOptions="executorGrid.gridOptions"
                        :durationDates="options.durationDates"
                        updateKey="name"
                        size="full">
                    </GridBase>
                </div>
            </div>
        </div>
    </div>
</template>

<script>
import GridBase from './GridBase.vue';

export default {
    data() {
        let ctrl = this;
        return {
            status: {},
            options: { },
            runnerGrid: {
                gridOptions: {
                    onRowClicked: ctrl.runnerClicked
                },
                columns: [
                    { label: 'Name' }, 
                    { label: 'Running',     type: 'count' }, 
                    { label: 'Finished',    type: 'count' }, 
                    { label: 'Last Heard',  type: 'date',   field: 'lastHeard' }, 
                    { label: 'Errors',      type: 'count' }, 
                    { label: 'Mem Free',    type: 'mem',    field: 'memoryFree' }, 
                    { label: 'Mem Max',     type: 'mem',    field: 'memoryMax' }
                ],
                rows: []
            },
            executorGrid: {
                gridOptions: {
                    onRowClicked: ctrl.executorClicked
                },
                columns: [
                    { label: 'Name' }, 
                    { label: 'Running',     type: 'count' }, 
                    { label: 'Finished',    type: 'count' }, 
                    { label: 'Last Heard',  type: 'date',   field: 'lastHeard' }, 
                    { label: 'Errors',      type: 'count' }, 
                    { label: 'Mem Free',    type: 'mem',    field: 'memoryFree' }, 
                    { label: 'Mem Max',     type: 'mem',    field: 'memoryMax' }
                ],
                rows: []
            }
        };
    },
    components: {
        GridBase
    },
    methods: {
        executorClicked(params) {
            let name = params.data.name;
            this.$router.push({
                path: '/executors',
                query: {
                    name: name
                }
            });
        },
        runnerClicked(params) {
            let name = params.data.name;
            this.$router.push({
                path: '/runners',
                query: {
                    name: name
                }
            });
        },
        formatData() {
            let ctrl = this;

            let executorRows = [];
            for (let name in ctrl.status.executors) {
                let status = ctrl.status.executors[name];
                executorRows.push({
                    name: name,
                    running: status.running,
                    finished: status.finished,
                    lastHeard: status.lastHeard,
                    errors: status.recentErrorCount,
                    memoryFree: (status.health.memoryMax - status.health.memoryUsed),
                    memoryMax: status.health.memoryMax
                });
            }

            let runnerRows = [];
            for (let name in ctrl.status.queryRunners) {
                let status = ctrl.status.queryRunners[name];
                runnerRows.push({
                    name: name,
                    running: status.running,
                    finished: status.finished,
                    errors: status.recentErrors.length,
                    lastHeard: status.lastHeard,
                    memoryFree: (status.health.memoryMax - status.health.memoryUsed),
                    memoryMax: status.health.memoryMax
                });
            }

            ctrl.executorGrid.rows = executorRows;
            ctrl.runnerGrid.rows = runnerRows;
        }
    },
    mounted() {
        let ctrl = this;
        
        ctrl.$parent.$on('optionsChanged', function(options) {
            ctrl.$emit('optionsChanged', options);
        });
        ctrl.$on('optionsChanged', function(options) {
            ctrl.options = options;
        });

        ctrl.$parent.$on('generalStatusChanged', function(status) {
            ctrl.$emit('generalStatusChanged', status);
        });
        ctrl.$on('generalStatusChanged', function(status) {
            ctrl.status = status;
            ctrl.formatData();
        });
        ctrl.$parent.$emit('getStatus');
    }
}
</script>

<style>
.col-md-6 {
    display: inline-block;
}
</style>