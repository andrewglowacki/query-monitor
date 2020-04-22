<template>
    <div class="col-md-12">

        <div class="col-md-6">
            <div class="card">
                <div class="card-header">
                    <h3>Query Runners - {{runnerGrid.rows.length}}</h3>
                </div>
                <div class="card-body">
                    <ag-grid-vue id="runnerGrid" style="width: 100%; height: 200px"
                                class="ag-theme-balham"
                                :columnDefs="runnerGrid.columns"
                                :rowData="runnerGrid.rows"
                                :gridOptions="runnerGrid.gridOptions"
                                :defaultColDef="columnDefaults">
                    </ag-grid-vue>
                </div>
            </div>
        </div>

        <div class="col-md-6">
            <div class="card">
                <div class="card-header">
                    <h3>Executors - {{executorGrid.rows.length}}</h3>
                </div>
                <div class="card-body">
                    <ag-grid-vue id="executorGrid" style="width: 100%; height: 200px"
                                class="ag-theme-balham"
                                :columnDefs="executorGrid.columns"
                                :rowData="executorGrid.rows"
                                :gridOptions="executorGrid.gridOptions"
                                :defaultColDef="columnDefaults">
                    </ag-grid-vue>
                </div>
            </div>
        </div>
    </div>
</template>

<script>
import { AgGridVue } from 'ag-grid-vue';
import "ag-grid-community/dist/styles/ag-grid.css";
import "ag-grid-community/dist/styles/ag-theme-balham.css";
import moment from 'moment';

export default {
    data() {
        let ctrl = this;
        return {
            status: {},
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
            runnerGrid: {
                gridOptions: {
                    onRowClicked: ctrl.runnerClicked
                },
                columns: [{
                    headerName: 'Name',
                    field: 'name'
                }, {
                    headerName: 'Running',
                    field: 'running',
                    filter: 'agNumberColumnFilter'
                }, {
                    headerName: 'Finished',
                    field: 'finished',
                    filter: 'agNumberColumnFilter'
                }, {
                    headerName: 'Last Heard',
                    field: 'lastHeard',
                    filter: 'agDateColumnFilter',
                    comparator: ctrl.dateSortComparator,
                    valueFormatter: ctrl.dataValueFormatter,
                    tooltip: ctrl.dateTooltipFormatter,
                    filterParams: {
                        browserDatePicker: true,
                        applyButton: true,
                        resetButton: true,
                        defaultOption: 'inRange',
                        comparator: ctrl.dateFilterComparator
                    }
                }, {
                    headerName: 'Errors',
                    field: 'errors',
                    filter: 'agNumberColumnFilter'
                }, {
                    headerName: 'Mem Free',
                    field: 'memoryFree',
                    filter: 'agNumberColumnFilter',
                    valueFormatter: ctrl.formatMem
                }, {
                    headerName: 'Mem Max',
                    field: 'memoryMax',
                    filter: 'agNumberColumnFilter',
                    valueFormatter: ctrl.formatMem
                }],
                rows: []
            },
            executorGrid: {
                gridOptions: {
                    onRowClicked: ctrl.executorClicked
                },
                columns: [{
                    headerName: 'Name',
                    field: 'name'
                }, {
                    headerName: 'Running',
                    field: 'running',
                    filter: 'agNumberColumnFilter'
                }, {
                    headerName: 'Finished',
                    field: 'finished',
                    filter: 'agNumberColumnFilter'
                }, {
                    headerName: 'Last Heard',
                    field: 'lastHeard',
                    filter: 'agDateColumnFilter',
                    comparator: ctrl.dateSortComparator,
                    valueFormatter: ctrl.dataValueFormatter,
                    tooltip: ctrl.dateTooltipFormatter,
                    filterParams: {
                        browserDatePicker: true,
                        applyButton: true,
                        resetButton: true,
                        defaultOption: 'inRange',
                        comparator: ctrl.dateFilterComparator
                    }
                }, {
                    headerName: 'Errors',
                    field: 'errors',
                    filter: 'agNumberColumnFilter'
                }, {
                    headerName: 'Mem Free',
                    field: 'memoryFree',
                    filter: 'agNumberColumnFilter',
                    valueFormatter: ctrl.formatMem
                }, {
                    headerName: 'Mem Max',
                    field: 'memoryMax',
                    filter: 'agNumberColumnFilter',
                    valueFormatter: ctrl.formatMem
                }],
                rows: []
            }
        };
    },
    components: {
        AgGridVue
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
        dateSortComparator(timeOne, timeTwo) {
            if (timeOne < timeTwo) {
                return -1;
            } else if (timeOne > timeTwo) {
                return 1;
            } else {
                return 0;
            }
        },
        dateFilterComparator(filterLocalDateAtMidnight, cellEpoch) {
            let filterEpoch = filterLocalDateAtMidnight.getTime();
            if (cellEpoch < filterEpoch) {
                return -1;
            } else if (cellEpoch > filterEpoch) {
                return 1;
            } else {
                return 0;
            }
        },
        dateTooltipFormatter(params) {
            return 'Last heard at: ' + moment(params.value).format("MM/DD/YYYY HH:mm:ss");
        },
        dataValueFormatter(params) {
            let value = params.value;
            return moment(value).fromNow();
        },
        formatMem(params) {
            let value = params.value;
            const labels = ['B', 'KB', 'MB', 'GB'];
            let labelIndex = 0;
            while (value > 1024.0) {
                value = value / 1024.0;
                labelIndex++;
            }
            if (labelIndex > 0) {
                value = Math.round(value * 100) / 100;
            }
            return "" + value + " " + labels[labelIndex];
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
        },
        handleResize() {
            let executorGrid = document.getElementById("executorGrid");
            let runnerGrid = document.getElementById("runnerGrid");
            let windowHeight = window.innerHeight;
            let top = executorGrid.offsetTop
            executorGrid.style.height = (windowHeight - (top + 110)) + "px";
            runnerGrid.style.height = executorGrid.style.height;
        }
    },
    mounted() {
        let ctrl = this;
        ctrl.$parent.$on('generalStatusChanged', function(status) {
            ctrl.status = status;
            ctrl.formatData();
        });
        ctrl.$parent.$emit('getStatus');

        window.addEventListener("resize", ctrl.handleResize);
        ctrl.handleResize();
        ctrl.runnerGrid.gridOptions.api.sizeColumnsToFit();
        ctrl.executorGrid.gridOptions.api.sizeColumnsToFit();
    },
    unmounted() {
        window.removeEventListener("resize", this.handleResize);
    }
}
</script>

<style>
.col-md-6 {
    display: inline-block;
}
</style>