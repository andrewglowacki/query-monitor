<template>
    <div class="col-md-12 runners-container">

        <div class="col-md-12">
            <div class="card">
                <div class="card-header">
                    <h3>{{getRowCount(runnerGrid)}} Query Runners</h3>
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
        <div class="col-md-3">
            <div class="card">
                <div class="card-header">
                    <h3 v-if="selected.name != '' && errorsGrid.rows.length > 0">{{errorsGrid.rows.length}} Errors</h3>
                    <h3 v-if="selected.name == '' || errorsGrid.rows.length == 0">No Errors</h3>
                </div>
                <div class="card-body">
                    <ag-grid-vue id="errorsGrid" style="width: 100%; height: 200px"
                                class="ag-theme-balham"
                                :columnDefs="errorsGrid.columns"
                                :rowData="errorsGrid.rows"
                                :gridOptions="errorsGrid.gridOptions"
                                :defaultColDef="columnDefaults">
                    </ag-grid-vue>
                </div>
            </div>
        </div>
        <div class="col-md-9">
            <div class="card">
                <div class="card-header">
                    <h3>
                        <button class="btn btn-info btn-sm btn-refresh" title="Refresh queries" @click="loadQueries()">
                            <i class="fa fa-redo"></i>
                        </button>
                        <span v-if="selected.name == '' && !loading.queries">
                            Queries: No runner selected
                        </span>
                        <span v-if="selected.name != ''">
                            <span>{{getRowCount(queryGrid)}}</span>
                            Queries: {{selected.name}}
                            <span v-if="loading.queries"> - <i class="fa fa-spinner fa-spin"></i> Loading...</span>
                        </span>
                        <div class="float-right btn-group" style="padding-top: 5px;">
                            <button class="btn btn-secondary btn-sm" :class="{ 'active': selected.running }" @click="setRunningStatus(true)">
                                <i class="fa fa-check text-success" v-if="selected.running"></i>
                                Running
                            </button>
                            <button class="btn btn-secondary btn-sm" :class="{ 'active': !selected.running }" @click="setRunningStatus(false)">
                                <i class="fa fa-check text-success" v-if="!selected.running"></i>
                                Finished
                            </button>
                        </div>
                    </h3>
                </div>
                <div class="card-body">
                    <ag-grid-vue id="queryGrid" style="width: 100%; height: 200px"
                                class="ag-theme-balham"
                                :columnDefs="queryGrid.columns"
                                :rowData="queryGrid.rows"
                                :gridOptions="queryGrid.gridOptions"
                                :defaultColDef="columnDefaults">
                    </ag-grid-vue>
                </div>
            </div>
        </div>
        <div class="col-md-6">
            <div class="card">
                <div class="card-header">
                    <h3>
                        <button class="btn btn-info btn-sm btn-refresh" title="Refresh shards" @click="loadShards()">
                            <i class="fa fa-redo"></i>
                        </button>
                        <span v-if="selected.query == -1 && !loading.shards">
                            Shards: No query selected
                        </span>
                        <span v-if="selected.query >= 0">
                            <span>{{getRowCount(shardsGrid)}}</span>
                            Shards: Idx# {{selected.query}}
                            <span v-if="loading.shards"> - <i class="fa fa-spinner fa-spin"></i> Loading...</span>
                        </span>
                    </h3>
                </div>
                <div class="card-body">
                    <ag-grid-vue id="shardsGrid" style="width: 100%; height: 200px"
                                class="ag-theme-balham"
                                :columnDefs="shardsGrid.columns"
                                :rowData="shardsGrid.rows"
                                :gridOptions="shardsGrid.gridOptions"
                                :defaultColDef="columnDefaults">
                    </ag-grid-vue>
                </div>
            </div>
        </div>
        <div class="col-md-6">
            <div class="card">
                <div class="card-header">
                    <h3>
                        <button class="btn btn-info btn-sm btn-refresh" title="Refresh shards" @click="loadShards()">
                            <i class="fa fa-redo"></i>
                        </button>
                        <span v-if="selected.shard == ''">
                            Attempts: No shard selected
                        </span>
                        <span v-if="selected.shard != ''">
                            <span>{{attemptsGrid.rows.length}}</span>
                            Attempts: {{selected.shard}}
                            <span v-if="loading.shards"> - <i class="fa fa-spinner fa-spin"></i> Loading...</span>
                        </span>
                    </h3>
                </div>
                <div class="card-body">
                    <ag-grid-vue id="attemptsGrid" style="width: 100%; height: 200px"
                                class="ag-theme-balham"
                                :columnDefs="attemptsGrid.columns"
                                :rowData="attemptsGrid.rows"
                                :gridOptions="attemptsGrid.gridOptions"
                                :defaultColDef="columnDefaults">
                    </ag-grid-vue>
                </div>
            </div>
        </div>
        <div class="col-md-4">
            <div class="card">
                <div class="card-header">
                    <h3>
                        <button class="btn btn-info btn-sm btn-refresh" title="Refresh shard attempt detail" @click="loadAttemptDetail()">
                            <i class="fa fa-redo"></i>
                        </button>
                        <span v-if="selected.attempt == null">
                            Attempt Info: No attempt selected
                        </span>
                        <span v-if="selected.attempt != null">
                            Attempt Info: {{selected.attempt.server}}
                            <span v-if="loading.detail"> - <i class="fa fa-spinner fa-spin"></i> Loading...</span>
                        </span>
                    </h3>
                </div>
                <div class="card-body">
                    <table class="table table-striped table-bordered table-sm" style="margin-bottom: 0px;">
                        <tbody>
                            <tr>
                                <th>Request Index</th>
                                <td v-text="attemptDetailInfo.index"></td>
                            </tr>
                            <tr>
                                <th>Shard</th>
                                <td v-text="attemptDetailInfo.shard"></td>
                            </tr>
                            <tr>
                                <th>Started</th>
                                <td v-text="attemptDetailInfo.started"></td>
                            </tr>
                            <tr>
                                <th>Finished</th>
                                <td v-text="attemptDetailInfo.finished"></td>
                            </tr>
                            <tr>
                                <th>Duration</th>
                                <td v-text="attemptDetailInfo.duration"></td>
                            </tr>
                            <tr>
                                <th>Queue Start</th>
                                <td v-text="attemptDetailInfo.startedQueueCount"></td>
                            </tr>
                            <tr>
                                <th>Queue End</th>
                                <td v-text="attemptDetailInfo.finishedQueueCount"></td>
                            </tr>
                            <tr>
                                <th>Results</th>
                                <td v-text="attemptDetailInfo.results"></td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
        <div class="col-md-8">
            <div class="card">
                <div class="card-header">
                    <h3>
                        <button class="btn btn-info btn-sm btn-refresh" title="Refresh shard attempt detail" @click="loadAttemptDetail()">
                            <i class="fa fa-redo"></i>
                        </button>
                        <span v-if="selected.attempt == null">
                            Attempt Parts: No attempt selected
                        </span>
                        <span v-if="selected.attempt != null">
                            Attempt Parts: {{selected.attempt.server}} - {{attemptDate}}
                            <span v-if="loading.detail"> - <i class="fa fa-spinner fa-spin"></i> Loading...</span>
                        </span>
                    </h3>
                </div>
                <div class="card-body" style="padding: 0px">
                    <QueryParts :parts="attemptDetailParts" style="margin-bottom: 0px; border-left: none"></QueryParts>
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
import moment from 'moment';
import QueryParts from './QueryParts.vue';

export default {
    data() {
        let ctrl = this;
        return {
            status: {
                queryRunners: {}
            },
            options: {},
            shardMap: {},
            attemptDetailInfo: ctrl.createEmptyAttemptDetailInfo(),
            attemptDetailParts: [],
            selected: {
                running: true,
                name: '',
                query: -1,
                shard: '',
                attempt: null
            },
            cancelTokens: {
                queries: null,
                shards: null,
                attempt: null
            },
            loading: {
                queries: false,
                shards: false,
                detail: false
            },
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
                    onRowClicked: ctrl.runnerClicked,
                    rowSelection: 'single'
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
                    headerName: 'Threads',
                    field: 'threads',
                    filter: 'agNumberColumnFilter'
                }, {
                    headerName: 'Errors',
                    field: 'errors',
                    filter: 'agNumberColumnFilter'
                }, 
                    ctrl.dateColumn('Last Error', 'mostRecentError'),
                {
                    headerName: 'Mem Free',
                    field: 'memoryFree',
                    filter: 'agNumberColumnFilter',
                    valueFormatter: ctrl.formatMem
                }, {
                    headerName: 'Mem Used',
                    field: 'memoryUsed',
                    filter: 'agNumberColumnFilter',
                    valueFormatter: ctrl.formatMem
                }, {
                    headerName: 'Mem Max',
                    field: 'memoryMax',
                    filter: 'agNumberColumnFilter',
                    valueFormatter: ctrl.formatMem
                }, 
                    ctrl.dateColumn('Last Heard', 'lastHeard'),
                {
                    headerName: 'GC Time',
                    field: 'gcTime',
                    filter: 'agNumberColumnFilter',
                    valueFormatter: ctrl.durationValueFormatter,
                    tooltipValueGetter: ctrl.durationTooltip
                }, {
                    headerName: 'GC Count',
                    field: 'gcCount',
                    filter: 'agNumberColumnFilter'
                }, 
                    ctrl.dateColumn('Up Since', 'upSince')
                ],
                rows: []
            },
            queryGrid: {
                gridOptions: {
                    onRowClicked: ctrl.queryClicked,
                    rowSelection: 'single'
                },
                columns: [{
                    headerName: 'Idx#',
                    field: 'index',
                    filter: 'agNumberColumnFilter'
                }, 
                    ctrl.dateColumn('Started'), 
                    ctrl.dateColumn('Finished'), 
                {
                    headerName: 'Query Type',
                    field: 'queryType',
                }, {
                    headerName: 'Results Type',
                    field: 'resultsType',
                }, {
                    headerName: 'Query String',
                    field: 'queryString',
                }, {
                    headerName: '# BlobIds ',
                    field: 'numBlobIds',
                    filter: 'agNumberColumnFilter'
                }, {
                    headerName: 'Sh-Tot',
                    field: 'shardsTotal',
                    filter: 'agNumberColumnFilter'
                }, {
                    headerName: 'Sh-Comp',
                    field: 'shardsComplete',
                    filter: 'agNumberColumnFilter'
                }, {
                    headerName: 'Origin Thread',
                    field: 'originThreadName'
                }, {
                    headerName: '# Results',
                    field: 'results',
                    filter: 'agNumberColumnFilter'
                }, {
                    headerName: 'Result Size',
                    field: 'resultSize',
                    filter: 'agNumberColumnFilter',
                    valueFormatter: ctrl.formatMem
                }, {
                    headerName: 'Error',
                    field: 'error'
                }],
                rows: []
            },
            errorsGrid: {
                columns: [
                    ctrl.dateColumn('Time'),
                {
                    headerName: 'Error',
                    field: 'error'
                }],
                gridOptions: {},
                rows: []
            },
            shardsGrid: {
                columns: [{
                    headerName: 'Shard',
                    field: 'shard'
                }, {
                    headerName: 'Failures',
                    field: 'failures',
                    filter: 'agNumberColumnFilter'
                }, 
                    ctrl.dateColumn('Started'),
                    ctrl.dateColumn('Finished'),
                {
                    headerName: 'Server',
                    field: 'server'
                }, {
                    headerName: 'Errored',
                    field: 'errored'
                }],
                gridOptions: {
                    onRowClicked: ctrl.shardClicked,
                    rowSelection: 'single'
                },
                rows: []
            },
            attemptsGrid: {
                columns: [{
                    headerName: 'Server',
                    field: 'server'
                }, 
                    ctrl.dateColumn('Started'),
                {
                    headerName: 'Index Results',
                    field: 'indexResults',
                    filter: 'agNumberColumnFilter'
                },
                    ctrl.dateColumn('Index Finished', 'indexFinished'),
                {
                    headerName: 'Data Results',
                    field: 'dataResults',
                    filter: 'agNumberColumnFilter'
                }, {
                    headerName: 'Data Size',
                    field: 'dataSize',
                    filter: 'agNumberColumnFilter',
                    valueFormatter: ctrl.formatMem
                },
                    ctrl.dateColumn('Finished'),
                {
                    headerName: 'Error',
                    field: 'error'
                }],
                gridOptions: {
                    onRowClicked: ctrl.attemptClicked,
                    rowSelection: 'single'
                },
                rows: []
            }
        };
    },
    components: {
        AgGridVue,
        QueryParts
    },
    computed: {
        attemptDate() {
            return moment(this.selected.attempt.started).format("MM/DD/YYYY HH:mm:ss");
        }
    },
    methods: {
        dateColumn(headerName, field) {
            let ctrl = this;
            if (typeof field === 'undefined') {
                field = headerName.toLowerCase();
            }
            return {
                headerName: headerName,
                field: field,
                filter: 'agDateColumnFilter',
                comparator: ctrl.dateSortComparator,
                valueFormatter: ctrl.dateValueFormatter,
                tooltipValueGetter: ctrl.dateTooltipFormatter,
                filterParams: {
                    browserDatePicker: true,
                    applyButton: true,
                    resetButton: true,
                    defaultOption: 'inRange',
                    comparator: ctrl.dateFilterComparator
                }
            };
        },
        dateValueFormatter(params) {
            return this.options.durationDates ? this.dateDurationFormatter(params) : this.dateTimeFormatter(params);
        },
        dateTooltipFormatter(params) {
            return this.options.durationDates ? this.dateTimeFormatter(params) : this.dateDurationFormatter(params);
        },
        createEmptyAttemptDetailInfo() {
            return {
                index: '',
                shard: '',
                started: '',
                finished: '',
                duration: '',
                startedQueueCount: '',
                finishedQueueCount: '',
                results: '',
                queryString: ''
            };
        },
        queryClicked(params) {
            let index = params.data.index;
            if (this.selected.query != index) {
                this.selected.query = index;
                this.selected.shard = '';
                this.selected.attempt = null;
                this.shardsGrid.rows = [];
                this.attemptDetailInfo = this.createEmptyAttemptDetailInfo();
                this.attemptDetailParts = [];
                this.setRouteParams();
            }
            this.loadShards();
        },
        setRunningStatus(running) {
            if (running == this.selected.running) {
                this.loadQueries();
                return;
            }
            this.selected.running = running;
            this.selected.query = -1;
            this.selected.shard = '';
            this.selected.attempt = null;
            this.setRouteParams();
            this.loadQueries();
        },
        runnerClicked(params) {
            let name = params.data.name;
            if (this.selected.name != name) {
                this.selected.name = name;
                this.selected.query = -1;
                this.selected.shard = '';
                this.selected.attempt = null;
                this.queryGrid.rows = [];
                this.shardsGrid.rows = [];
                this.attemptDetailParts = [];
                this.attemptDetailInfo = this.createEmptyAttemptDetailInfo();
                this.setRouteParams();
            }
            this.loadQueries();
        },
        shardClicked(params) {
            let shard = params.data.shard;
            if (this.selected.shard != shard) {
                this.selected.shard = shard;
                this.selected.attempt = null;
                this.attemptDetailParts = [];
                this.attemptDetailInfo = this.createEmptyAttemptDetailInfo();
                this.setRouteParams();
            }
            this.populateAttempts();
        },
        attemptClicked(params) {
            let attempt = params.data;
            let currentAttempt = this.selected.attempt;
            if (currentAttempt == null || currentAttempt.server != attempt.server || currentAttempt.started != attempt.started) {
                this.selected.attempt = attempt;
                this.setRouteParams();
            }
            this.loadAttemptDetail();
        },
        loadTestDetail() {
            let ctrl = this;
            ctrl.loading.detail = false;

            let attempt = ctrl.selected.attempt;
            let query = ctrl.getSelectedQuery();
            
            let num = function(max) {
                return Math.floor(Math.random() * max);
            };

            let duration = attempt.finished == 0 ? new Date().getTime() : attempt.finished;
            duration -= attempt.started;
            ctrl.attemptDetailInfo = {
                index: num(100000),
                shard: ctrl.selected.shard,
                started: moment(attempt.started).format("MM/DD/YYYY HH:mm:ss"),
                finished: attempt.finished == 0 ? 'Not Finished' : moment(attempt.finished).format("MM/DD/YYYY HH:mm:ss"),
                duration: ctrl.durationValueFormatter({ value: duration }),
                results: attempt.finished != 0 ? attempt.indexResults : num(10000),
                startedQueueCount: num(20),
                finishedQueueCount: num(20),
                queryString: query.queryString
            };

            let createPart = function(start, finished) {
                let results = num(10000);
                return {
                    started: start,
                    finished: finished,
                    results: results,
                    unfilteredResults: results,
                    waitTime: num(finished),
                    partString: ("queryPart='" + num(10000000) + "'"),
                    children: []
                };
            };

            let recursiveCreate = function(start, end, max) {
                let createHere = num(max) + 1;
                let createLeft = max - createHere;
                let items = [];
                let duration = end - start;
                let durationEach = Math.round(duration / createHere);
                let eachHalf = Math.round(durationEach / 2);
                for (let i = 0; i < createHere; i++) {
                    let useEnd = start + eachHalf + num(eachHalf);
                    if (i == createHere - 1) {
                        useEnd = end;
                    }
                    let part = createPart(start, useEnd);
                    if (createLeft > 0 && num(100) >= 70) {
                        part.children = recursiveCreate(start, useEnd, createLeft);
                        createLeft -= part.children.length;
                    }
                    start = useEnd;
                    items.push(part);
                }
                return items;
            }

            let count = attempt.finished == 0 ? (num(20) + 1) : (num(50) + 1);

            let finished = attempt.finished == 0 ? new Date().getTime() : attempt.finished;
            this.attemptDetailParts = recursiveCreate(attempt.started, finished, count);
        },
        loadAttemptDetail() {
            let ctrl = this;

            if (ctrl.selected.attempt == null) {
                return;
            }

            if (ctrl.loading.detail) {
                ctrl.loading.detail = false;
                ctrl.cancelTokens.detail.cancel();
            }

            let attempt = ctrl.selected.attempt;
            let query = ctrl.getSelectedQuery();

            if (query == null) {
                console.error("No query string found to load detail for.");
                return;
            }

            let url = 'api/executor/' + encodeURIComponent(attempt.server) + "/find";
            let params = {
                started: attempt.started,
                shard: ctrl.selected.shard,
                queryString: query.queryString
            };

            ctrl.loading.detail = true;
            if (ctrl.isTest()) {
                let token = setTimeout(ctrl.loadTestDetail, 1000);
                ctrl.cancelTokens.detail = {
                    cancel() {
                        clearTimeout(token);
                    }
                }
                return;
            }
            let token = ctrl.cancelTokens.detail = axios.CancelToken.source();
            axios.post(url, params, {
                cancelToken: token.token
            }).then((response) => {
                ctrl.loading.detail = false;
                let entry = response.data;

                let duration = entry.info.finished == 0 ? new Date().getTime() : entry.info.finished;
                duration -= entry.info.started;

                entry.info.started = moment(entry.info.started).format("MM/DD/YYYY HH:mm:ss"),
                entry.info.finished = entry.info.finished == 0 ? 'Not Finished' : moment(entry.info.finished).format("MM/DD/YYYY HH:mm:ss"),
                entry.info.duration = ctrl.durationValueFormatter({ value: duration }),
                ctrl.attemptDetailInfo = entry.info;
                ctrl.attemptDetailParts = entry.queryParts;
            }).catch((response) => {
                ctrl.loading.detail = false;
                if (!axios.isCancel(response)) {
                    ctrl.handleQueryError(response);
                }
            });
        },
        populateAttempts() {
            if (this.selected.shard == '') {
                this.attemptsGrid.rows = [];
                this.attemptDetailInfo = this.createEmptyAttemptDetailInfo();
                return;
            }

            let shard = this.shardMap[this.selected.shard];
            if (typeof shard === 'undefined') {
                this.attemptsGrid.rows = [];
                this.attemptDetailInfo = this.createEmptyAttemptDetailInfo();
                return;
            }
            let rows = [];
            rows.push(shard.latestAttempt);
            for (let i = 0; i < shard.failedAttempts.length; i++) {
                rows.push(shard.failedAttempts[i]);
            }
            this.attemptsGrid.rows = rows;
            this.attemptsGrid.gridOptions.api.sizeColumnsToFit();
            this.loadAttemptDetail();
        },
        populateErrors() {
            if (this.selected.name == '') {
                this.errorsGrid.rows = [];
                return;
            }
            let runner = this.status.queryRunners[this.selected.name];
            if (typeof runner === 'undefined') {
                this.errorsGrid.rows = [];
                return;
            }
            this.errorsGrid.rows = runner.recentErrors.concat([]);
        },
        loadTestQueries() {
            let ctrl = this;
            ctrl.loading.queries = false;
            if (ctrl.selected.name == '') {
                console.warn("no test query name selected")
                return;
            }
            let runner = ctrl.status.queryRunners[ctrl.selected.name];
            if (typeof runner === 'undefined') {
                if (Object.keys(ctrl.status.queryRunners).length == 0) {
                    setTimeout(ctrl.loadTestQueries, 500);
                }
                console.warn("no test query runners found")
                return;
            }
            let count = ctrl.selected.running ? runner.running : runner.finished;

            let num = function(max) {
                return Math.floor(Math.random() * max);
            }

            const resultTypes = ['METADATA', 'BLOB', 'UUID'];
            let queries = [];
            for (let i = 0; i < count; i++) {
                let started = 0;
                let finished = 0;
                let shardsTotal = (num(40) * 24);
                let shardsComplete = shardsTotal;
                let results = num(1000000);
                let resultSize = (num(1024) + 100) * results;
                let error = null;
                if (ctrl.selected.running) {
                    started = new Date().getTime() - (1000 * (num(50) + 10));
                    shardsComplete = num(shardsTotal);
                } else {
                    started = new Date().getTime() - (1000 * (num(120) + 120));
                    finished = started + (1000 * (num(60) + 1));
                    if (num(10) == 0) {
                        error = 'this is an error string';
                    }
                }
                let queryType = (Math.random() > 0.25 ? 'NORMAL' : 'BLOB_ID');
                let resultsType = resultTypes[num(3)];
                if (resultsType == 'BLOB') {
                    resultSize *= 1024;
                } else if (resultsType == 'UUID') {
                    resultSize = results * 38;
                }
                queries.push({
                    index: i,
                    started: started,
                    finished: finished,
                    queryType: queryType,
                    resultsType: resultsType,
                    queryString: queryType == 'BLOB_ID' ? null : ('this is a query for query-string-' + i),
                    numBlobIds: queryType == 'BLOB_ID' ? (num(1000) + 1) : 0,
                    shardsTotal: shardsTotal,
                    shardsComplete: shardsComplete,
                    originThreadName: 'query request thread ' + i,
                    resultSize: resultSize,
                    results: results,
                    error: error
                });
            }
            ctrl.updateGrid(ctrl.queryGrid, queries, 'index');
            ctrl.loadShards();
        },
        loadQueries() {
            let ctrl = this;

            ctrl.populateErrors();

            if (ctrl.loading.queries) {
                ctrl.loading.queries = false;
                ctrl.cancelTokens.queries.cancel();
            }

            let url = 'api/runner/' + encodeURIComponent(ctrl.selected.name);
            if (ctrl.selected.running) {
                url += '/running';
            } else {
                url += '/finished';
            }

            ctrl.loading.queries = true;
            if (ctrl.isTest()) {
                let token = setTimeout(ctrl.loadTestQueries, 1000);
                ctrl.cancelTokens.queries = {
                    cancel() {
                        clearTimeout(token);
                    }
                }
                return;
            }
            let token = ctrl.cancelTokens.queries = axios.CancelToken.source();
            axios.get(url, {
                cancelToken: token.token
            }).then((response) => {
                ctrl.loading.queries = false;
                ctrl.updateGrid(ctrl.queryGrid, response.data, 'index');
                ctrl.loadShards();
            }).catch((response) => {
                ctrl.loading.queries = false;
                if (!axios.isCancel(response)) {
                    ctrl.handleQueryError(response);
                }
            });

        },
        handleQueryError(response) {
            let ctrl = this;
            if (typeof response.message !== 'undefined') {
                ctrl.error = response.message;
            } else if (typeof response.data !== 'undefined') {
                if (typeof response.data.message !== 'undefined') {
                    ctrl.error = response.data.message;
                } else {
                    ctrl.error = JSON.stringify(response.data);
                }
            } else if (typeof response.statusText !== 'undefined') {
                ctrl.error = response.statusText;
            } else {
                ctrl.error = JSON.stringify(response);
            }
        },
        getRowCount(grid) {
            if (typeof grid.gridOptions.api === 'undefined' || grid.gridOptions.api == null) {
                return 0;
            }
            return grid.gridOptions.api.getModel().getTopLevelRowCount();
        },
        getSelectedQuery() {
            let ctrl = this;
            let query = null;
            ctrl.queryGrid.gridOptions.api.forEachNode((node) => {
                if (ctrl.selected.query == node.data.index) {
                    query = node.data;
                }
            });
            return query;
        },
        loadTestShards() {
            let ctrl = this;
            
            ctrl.loading.shards = false;

            let query = ctrl.getSelectedQuery();

            if (query == null) {
                return;
            }

            let num = function(max) {
                return Math.floor(Math.random() * max);
            }

            let left = query.shardsTotal - query.shardsComplete;
            let count = num(left) + query.shardsComplete;
            let serverList = Object.keys(ctrl.status.executors);
            serverList.sort();

            let createAttempt = function(failed, complete, lastStart) {
                let server = serverList[num(serverList.length)];
                let started;
                let finished = 0;
                if (typeof lastStart !== 'undefined') {
                    finished = lastStart - 1000;
                    started = finished - (1000 * num(60));
                } else {
                    started = new Date().getTime() - (1000 * (num(120) + 10));
                    if (complete) {
                        finished = started + (1000 * (num(60) + 5));
                    }
                }

                let indexFinished = 0;
                let indexResults = 0;
                
                if (complete || num(2) == 1) {
                    let useEnd = finished;
                    if (!complete) {
                        useEnd = new Date().getTime();
                    }
                    let duration = useEnd - started;
                    indexFinished = Math.round(started + (duration * (0.1 + (0.1 * Math.random()))));
                    indexResults = num(1000);
                }

                let dataResults = complete || indexResults == 0 ? indexResults : num(indexResults);
                let dataSize = (num(1024) + 100);

                if (query.resultsType == 'BLOB') {
                    dataSize *= 1024;
                } else if (query.resultsType == 'UUID') {
                    dataSize = dataResults * 38;
                }

                return {
                    server: server,
                    started: started,
                    finished: finished,
                    indexFinished: indexFinished,
                    indexResults: indexResults,
                    dataResults: dataResults,
                    dataSize: dataSize,
                    dataFinished: finished,
                    error: failed ? 'this is a really long error message' : null
                };
            };

            let shards = [];
            let completeCount = 0;
            for (let i = 0; i < count; i++) {
                let piece = Math.floor(i / 24);
                let index = i % 24;
                let shard = '00';
                if (index < 10) {
                    shard += '0';
                }
                shard += index + '_' + piece;

                let thisComplete = (num(2) == 1 && completeCount < query.shardsComplete) || !ctrl.selected.running;
                if (thisComplete) {
                    completeCount++;
                }

                let latestAttempt = createAttempt(false, thisComplete);

                let failureCount = num(20) - 17;
                let failures = [];
                for (let f = 0; f < failureCount; f++) {
                    failures.push(createAttempt(true, true, latestAttempt.started));
                }

                shards.push({
                    shard: shard,
                    failedAttempts: failures,
                    latestAttempt: latestAttempt
                });
            }

            ctrl.formatShards(shards);
        },
        loadShards() {
            let ctrl = this;
            
            if (ctrl.loading.shards) {
                ctrl.loading.shards = false;
                ctrl.cancelTokens.shards.cancel();
            }
            if (ctrl.selected.name == '' || ctrl.selected.query == -1) {
                ctrl.shardsGrid.rows = [];
                ctrl.attemptsGrid.rows = [];
                return;
            }

            ctrl.loading.shards = true;
            if (ctrl.isTest()) {
                let token = setTimeout(ctrl.loadTestShards, 1000);
                ctrl.cancelTokens.shards = {
                    cancel: () => {
                        clearTimeout(token);
                    }
                }
                return;
            }

            let token = ctrl.cancelTokens.shards = axios.CancelToken.source();
            let url = 'api/runner/' + encodeURIComponent(ctrl.selected.name) + '/' + ctrl.selected.query;
            ctrl.loadingPromises.shards = axios.get(url, {
                cancelToken: token.token
            }).then((response) => {
                ctrl.loading.shards = false;
                ctrl.formatShards(response.data);
            }).catch((response) => {
                ctrl.loading.shards = false;
                if (!axios.isCancel(response)) {
                    ctrl.handleQueryError(response);
                }
            });
        },
        formatShards(shards) {
            let ctrl = this;
            let shardMap = {};
            let shardRows = [];
            for (let i = 0; i < shards.length; i++) {
                let shard = shards[i];
                shardMap[shard.shard] = shard;
                shardRows.push({
                    shard: shard.shard,
                    failures: shard.failedAttempts.length,
                    started: shard.latestAttempt.started,
                    finished: shard.latestAttempt.finished,
                    server: shard.latestAttempt.server,
                    errored: shard.latestAttempt.error != null
                });
            }
            ctrl.shardMap = shardMap;
            ctrl.updateGrid(ctrl.shardsGrid, shardRows, 'shard');
            if (ctrl.shardsGrid.gridOptions.api != null) {
                ctrl.shardsGrid.gridOptions.api.sizeColumnsToFit();
            } else {
                setTimeout(ctrl.shardsGrid.gridOptions.api.sizeColumnsToFit, 500);
            }
            ctrl.populateAttempts();
        },
        setRouteParams() {
            let ctrl = this;
            let params = {
                running: ctrl.selected.running
            };
            if (ctrl.selected.name != '') {
                params.name = ctrl.selected.name;
                if (ctrl.selected.query >= 0) {
                    params.query = ctrl.selected.query;
                    if (ctrl.selected.shard != '') {
                        params.shard = ctrl.selected.shard;
                    }
                }
            }
            ctrl.$router.push({
                path: '/runners',
                query: params
            });
        },
        dateSortComparator(timeOne, timeTwo) {
            if (timeOne == timeTwo) {
                return 0;
            } else if (timeOne != 0 && timeTwo != 0) {
                if (timeOne < timeTwo) {
                    return -1;
                } else {
                    return 1;
                }
            } else if (timeOne != 0) {
                return 1;
            } else {
                return -1;
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
        dateTimeFormatter(params) {
            if (params.value == 0) {
                return 'Never';
            } else {
                return moment(params.value).format("MM/DD/YYYY HH:mm:ss");
            }
        },
        dateDurationFormatter(params) {
            let value = params.value;
            if (value == 0) {
                return 'Never';
            } else {
                let ago = moment(value).fromNow();
                if (ago == 'a few seconds ago') {
                    return '' + Math.round((new Date().getTime() - value) / 1000) + ' seconds ago';
                } else {
                    return ago;
                }
            }
        },
        durationValueFormatter(params) {
            let value = params.value;
            if (value < 5000) {
                return '' + value + ' ms';
            } else if (value < 120000) {
                return '' + Math.round(value / 1000) + ' seconds';
            } else {
                return moment.duration(value).humanize();
            }
        },
        durationTooltip(params) {
            let value = params.value;
            if (value < 1000) {
                return '' + value + ' ms';
            } else {
                return '' + (Math.round(value / 10) / 100) + ' seconds';
            }
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
        isTest() {
            return typeof this.$route.query.test !== 'undefined';
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
        formatRunners() {
            let ctrl = this;

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
                    memoryUsed: status.health.memoryUsed,
                    memoryMax: status.health.memoryMax,
                    threads: status.health.threads,
                    gcTime: status.health.gcTime,
                    gcCount: status.health.gcCount,
                    mostRecentError: status.health.mostRecentError,
                    upSince: status.health.upSince
                });
            }

            ctrl.updateGrid(ctrl.runnerGrid, runnerRows, 'name');
            ctrl.populateErrors();
        },
        handleResize() {
            let runnerGrid = document.getElementById("runnerGrid");
            let windowHeight = window.innerHeight;
            let top = runnerGrid.offsetTop
            let amount = Math.round((windowHeight - (top + 220)) / 2);
            runnerGrid.style.height = amount + "px";
            document.getElementById("queryGrid").style.height = runnerGrid.style.height;
            document.getElementById("errorsGrid").style.height = runnerGrid.style.height;
            document.getElementById("shardsGrid").style.height = runnerGrid.style.height;
            document.getElementById("attemptsGrid").style.height = runnerGrid.style.height;
        }
    },
    mounted() {
        let ctrl = this;
        
        let params = ctrl.$route.query;
        if (typeof params.name !== 'undefined') {
            ctrl.selected.name = params.name;
            ctrl.loadQueries();
            if (typeof params.query !== 'undefined') { 
                ctrl.selected.query = parseInt(params.query);
                if (typeof params.shard !== 'undefined') { 
                    ctrl.selected.shard = params.shard;
                }
            }
        }

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
            ctrl.formatRunners();
        });
        ctrl.$parent.$emit('getStatus');

        window.addEventListener("resize", ctrl.handleResize);
        ctrl.handleResize();
        ctrl.runnerGrid.gridOptions.api.sizeColumnsToFit();
        ctrl.queryGrid.gridOptions.api.sizeColumnsToFit();
    },
    unmounted() {
        window.removeEventListener("resize", this.handleResize);
    }
}
</script>

<style>
.col-md-6, .col-md-9, .col-md-3, .col-md-4, .col-md-8 {
    display: inline-block;
    margin-top: 10px;
}
.btn-refresh {
    margin-right: 10px;
}
.runners-container {
    padding-bottom: 20px;
}
</style>