<template>
    <div class="col-md-12" style="margin-bottom: 20px; margin-top: 0px">

        <div class="col-md-6">
            <div class="card">
                <div class="card-header">
                    <h3>{{getRowCount(targetGrid)}} Runners/Executors</h3>
                </div>
                <div class="card-body">
                    <ag-grid-vue id="targetGrid" style="width: 100%; height: 200px"
                                class="ag-theme-balham"
                                :columnDefs="targetGrid.columns"
                                :rowData="targetGrid.rows"
                                :gridOptions="targetGrid.gridOptions"
                                :defaultColDef="columnDefaults">
                    </ag-grid-vue>
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
                    <ag-grid-vue id="scanGrid" style="width: 100%; height: 200px"
                                class="ag-theme-balham"
                                :columnDefs="scanGrid.columns"
                                :rowData="scanGrid.rows"
                                :gridOptions="scanGrid.gridOptions"
                                :defaultColDef="columnDefaults">
                    </ag-grid-vue>
                </div>
            </div>
        </div>
    </div>
</template>

<script>
import { AgGridVue } from 'ag-grid-vue';
import axios from 'axios';
import "ag-grid-community/dist/styles/ag-grid.css";
import "ag-grid-community/dist/styles/ag-theme-balham.css";

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
            columnDefaults: {
                sortable: true,
                editable: false,
                resizable: true,
                filter: true,
                filterParams: {
                    applyButton: true,
                    resetButton: true
                }
            },
            targetGrid: {
                gridOptions: {
                    onRowClicked: ctrl.targetClicked,
                    rowSelection: 'multiple'
                },
                columns: [{
                    headerName: 'Name',
                    field: 'name'
                }, {
                    headerName: 'Type',
                    field: 'type'
                }, {
                    headerName: 'Running',
                    field: 'running',
                    filter: 'agNumberColumnFilter'
                }],
                rows: []
            },
            scanGrid: {
                gridOptions: { },
                columns: [{
                    headerName: 'Runner/Executor',
                    field: 'target',
                }, {
                    headerName: 'TServer',
                    field: 'server',
                }, {
                    headerName: 'Ranges',
                    field: 'ranges',
                    filter: 'agNumberColumnFilter'
                }, {
                    headerName: 'Table',
                    field: 'table',
                }],
                rows: []
            }
        }
    },
    components: {
        AgGridVue
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
            this.updateGrid(this.targetGrid, rows, 'name');
        },
        updateGrid(grid, newRows, key) {
            let newMap = {};
            for (let i = 0; i < newRows.length; i++) {
                newMap[newRows[i][key]] = newRows[i];
            }

            let oldMap = {};
            grid.gridOptions.api.forEachNode((node) => {
                oldMap[node.data[key]] = node.data;
            });

            let add = [];
            let remove = [];
            let update = {};

            for (let key in newMap) {
                if (key in oldMap) {
                    update[key] = newMap[key];
                } else {
                    add.push(newMap[key]);
                }
            }

            for (let key in oldMap) {
                if (key in newMap) {
                    continue;
                }
                remove.push(oldMap[key]);
            }

            if (add.length > 0 || remove.length > 0) {
                grid.gridOptions.api.updateRowData({
                    add: add,
                    remove: remove
                });
            }

            if (Object.keys(update).length > 0) {
                grid.gridOptions.api.forEachNode((node) => {
                    let row = update[node.data[key]];
                    if (typeof row !== 'undefined') {
                        node.setData(row);
                    }
                });
            }
        },
        getRowCount(grid) {
            if (typeof grid.gridOptions.api === 'undefined' || grid.gridOptions.api == null) {
                return 0;
            }
            return grid.gridOptions.api.getModel().getTopLevelRowCount();
        },
        isTest() {
            return typeof this.$route.query.test !== 'undefined';
        },
        handleResize() {
            let targetGrid = document.getElementById("targetGrid");
            let windowHeight = window.innerHeight;
            let top = targetGrid.offsetTop;
            targetGrid.style.height = (windowHeight - (top + 120)) + "px";
            document.getElementById("scanGrid").style.height = targetGrid.style.height;
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

        window.addEventListener("resize", ctrl.handleResize);
        ctrl.handleResize();
        ctrl.targetGrid.gridOptions.api.sizeColumnsToFit();
        ctrl.scanGrid.gridOptions.api.sizeColumnsToFit();
    }
}
</script>

<style>

</style>