<template>
    <div class="col-md-12" style="margin-bottom: 20px; margin-top: 0px">

        <div class="col-md-6">
            <div class="card">
                <div class="card-header">
                    <h3>{{targetGrid.rows.length}} Runners/Executors</h3>
                </div>
                <div class="card-body">
                    <GridBase :columns="targetGrid.columns"
                        :rows="targetGrid.rows"
                        :gridOptions="targetGrid.gridOptions"
                        size="full"
                        updateKey="name">
                    </GridBase>
                </div>
            </div>
        </div>

        <div class="col-md-6">
            <div class="card">
                <div class="card-header">
                    <h3>
                        <button class="btn btn-info btn-sm btn-refresh" title="Refresh scans" @click="loadScans()">
                            <i class="fa fa-redo"></i>
                        </button>
                        {{scanGrid.rows.length}} Scans for {{loadingTokens.length}} targets
                        <span v-if="loading > 0">
                            - <i class="fa fa-spinner fa-spin"></i> Loading {{loading}} more...
                        </span>
                    </h3>
                </div>
                <div class="card-body">
                    <GridBase :columns="scanGrid.columns"
                        :rows="scanGrid.rows"
                        :gridOptions="scanGrid.gridOptions"
                        size="full">
                    </GridBase>
                </div>
            </div>
        </div>
    </div>
</template>

<script>
import axios from 'axios';
import GridBase from './GridBase.vue';

export default {
    data() {
        let ctrl = this;
        return {
            status: {
                queryRunners: {},
                executors: {}
            },
            loading: 0,
            loadingTokens: [],
            targetGrid: {
                gridOptions: {
                    onRowClicked: ctrl.targetClicked,
                    rowSelection: 'multiple'
                },
                columns: [
                    { label: 'Name' }, 
                    { label: 'Type' }, 
                    { label: 'Running', type: 'count' }
                ],
                rows: []
            },
            scanGrid: {
                gridOptions: { },
                columns: [
                    { label: 'Runner/Executor', field: 'target' }, 
                    { label: 'TServer',         field: 'server' }, 
                    { label: 'Ranges',          field: 'ranges',    type: 'count' }, 
                    { label: 'Table',           field: 'table' }
                ],
                rows: []
            }
        }
    },
    components: {
        GridBase
    },
    methods: {
        targetClicked() {
            this.loadScans();
        },
        createTestScans(name, type) {
            let ctrl = this;
            ctrl.loading--;
            if (ctrl.loading < 0) {
                ctrl.loading = 0;
            }
            
            let num = function(max) {
                return Math.floor(Math.random() * max);
            };

            let count = type == 'runner' ? num(20) : num(5);

            let scans = [];

            for (let i = 0; i < count; i++) {
                let ranges = 1;
                let table = null;
                if (type == 'runner') {
                    ranges = num(20) + 1;
                } else if (num(2) == 1) {
                    table = "" + num(10);
                }

                let servers = Object.keys(ctrl.status.executors);
                let server = servers[num(servers.length)];
                scans.push({
                    target: name,
                    server: server,
                    ranges: ranges,
                    table: table
                });
            }

            ctrl.scanGrid.rows = ctrl.scanGrid.rows.concat(scans);
        },
        loadScans() {
            let ctrl = this;

            if (ctrl.loading > 0) {
                ctrl.loadingTokens.forEach((token) => {
                    token.cancel();
                });
                ctrl.loading = 0;
            }

            let tokens = [];

            ctrl.scanGrid.rows = [];

            let selected = this.targetGrid.gridOptions.api.getSelectedRows();
            selected.forEach((row) => {
                let type = row.type.toLowerCase();
                let name = row.name;
                let url = 'api/' + type + '/' + encodeURIComponent(name) + "/scans";

                if (ctrl.isTest()) {
                    let token = setTimeout(ctrl.createTestScans, Math.round(Math.random() * 2000), name, type);
                    tokens.push({
                        cancel: () => {
                            clearTimeout(token);
                        }
                    });
                    return;
                }

                let token = axios.CancelToken.source();
                tokens.push(token);

                axios.get(url, {
                    cancelToken: token.token
                }).then((response) => {
                    ctrl.loading--;
                    if (ctrl.loading < 0) {
                        ctrl.loading = 0;
                    }
                    ctrl.scanGrid.rows = ctrl.scanGrid.rows.concat(response.data);
                }).catch((response) => {
                    ctrl.loading--;
                    if (ctrl.loading < 0) {
                        ctrl.loading = 0;
                    }
                    if (!axios.isCancel(response)) {
                        console.error(response);
                    }
                });
            });

            ctrl.loadingTokens = tokens;
            ctrl.loading = tokens.length;
        },
        formatData() {
            let rows = [];
            for (let name in this.status.queryRunners) {
                rows.push({
                    name: name,
                    type: 'Runner',
                    running: this.status.queryRunners[name].running
                });
            }
            for (let name in this.status.executors) {
                rows.push({
                    name: name,
                    type: 'Executor',
                    running: this.status.executors[name].running
                });
            }
            this.targetGrid.rows = rows;
        },
        isTest() {
            return typeof this.$route.query.test !== 'undefined';
        }
    },
    mounted() {
        let ctrl = this;
        
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

</style>