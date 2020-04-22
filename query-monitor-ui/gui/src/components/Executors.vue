<template>
    <div class="col-md-12 executors-container">
        <div class="col-md-12">
            <div class="card">
                <div class="card-header">
                    <h3>{{getRowCount(executorGrid)}} Executors</h3>
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
        <div class="col-md-3">
            <div class="card">
                <div class="card-header">
                    <h3>
                        <span>{{errorsGrid.rows.length}} Errors</span>
                        <span v-if="selected.name != '' && !loading.detail"> - {{selected.name}}</span>
                        <span v-if="loading.detail"> - <i class="fa fa-spinner fa-spin"></i> Loading...</span>
                    </h3>
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
                        <button class="btn btn-info btn-sm btn-refresh" title="Refresh executor detail" @click="loadExecutorDetail()">
                            <i class="fa fa-redo"></i>
                        </button>
                        Executor Stats: 
                        <span v-if="!loading.detail && selected.name != ''">{{selected.name}}</span>
                        <span v-if="!loading.detail && selected.name == ''">No Executor Selected</span>
                        <span v-if="loading.detail"><i class="fa fa-spinner fa-spin"></i> Loading...</span>
                    </h3>
                </div>
                <div class="card-body">
                    <canvas id="executorStats" class="chart chart-line" style="width: 100%; height: 200px"></canvas>
                </div>
            </div>
        </div>
        <div class="col-md-12">
            <div class="card">
                <div class="card-header">
                    <h3>
                        <button class="btn btn-info btn-sm btn-refresh" title="Refresh for the selected query executor" @click="loadShards()">
                            <i class="fa fa-redo"></i>
                        </button>
                        {{getRowCount(shardsGrid)}} Shards: 
                        <span v-if="selected.name == ''">
                            No executor selected
                        </span>
                        <span v-if="selected.name != ''">
                            {{selected.name}}
                        </span>
                        <span v-if="loading.shards"> - <i class="fa fa-spinner fa-spin"></i> Loading...</span>

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
        <div class="col-md-4">
            <div class="card">
                <div class="card-header">
                    <h3>
                        <button class="btn btn-info btn-sm btn-refresh" title="Refresh shard attempt detail" @click="loadQueryParts()">
                            <i class="fa fa-redo"></i>
                        </button>
                        <span v-if="selected.shard == -1">
                            Shard Info: No attempt selected
                        </span>
                        <span v-if="selected.shard != -1">
                            Shard Info: Idx # {{selected.shard}}
                            <span v-if="loading.parts"> - <i class="fa fa-spinner fa-spin"></i> Loading...</span>
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
                            <tr>
                                <th>Source</th>
                                <td v-text="attemptDetailInfo.sourceServer"></td>
                            </tr>
                            <tr>
                                <th>Error</th>
                                <td v-text="attemptDetailInfo.error"></td>
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
                        <button class="btn btn-info btn-sm btn-refresh" title="Refresh shard query parts" @click="loadQueryParts()">
                            <i class="fa fa-redo"></i>
                        </button>
                        <span v-if="selected.shard == -1">
                            Query Parts: No shard selected
                        </span>
                        <span v-if="selected.shard != -1">
                            Query Parts: Idx # {{selected.shard}}
                            <span v-if="loading.parts"> - <i class="fa fa-spinner fa-spin"></i> Loading...</span>
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
import Chart from 'chart.js';
import QueryParts from './QueryParts.vue';

export default {
    data() {
        let ctrl = this;
        return {
            status: {
                executors: {},
                queryRunners: {'unknown': {}}
            },
            attemptDetailInfo: ctrl.createEmptyAttemptDetailInfo(),
            attemptDetailParts: [],
            executorDetail: ctrl.createEmptyExecutorDetail(),
            executorDetailChart: {
                chart: null,
                data: {
                    type: 'line',
                    data: {
                        labels: [],
                        datasets: [],
                    },
                    options: { }
                }
            },
            selected: {
                running: true,
                name: '',
                shard: -1,
            },
            cancelTokens: {
                detail: null,
                shards: null,
                parts: null
            },
            loading: {
                detail: false,
                shards: false,
                parts: false
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
            executorGrid: {
                gridOptions: {
                    onRowClicked: ctrl.executorClicked,
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
                    tooltip: ctrl.durationTooltip
                }, {
                    headerName: 'GC Count',
                    field: 'gcCount',
                    filter: 'agNumberColumnFilter'
                }, 
                    ctrl.dateColumn('Up Since', 'upSince')
                ],
                rows: []
            },
            errorsGrid: {
                columns: [
                    ctrl.dateColumn('Time', true),
                {
                    headerName: 'Error',
                    field: 'error'
                }],
                gridOptions: {},
                rows: []
            },
            shardsGrid: {
                gridOptions: {
                    onRowClicked: ctrl.shardClicked,
                    rowSelection: 'single'
                },
                columns: [{
                    headerName: 'Idx',
                    field: 'index',
                    filter: 'agNumberColumnFilter'
                }, {
                    headerName: 'Shard',
                    field: 'shard'
                },
                    ctrl.dateColumn('Started'),
                    ctrl.dateColumn('Finished'), 
                {
                    headerName: 'Source',
                    field: 'sourceServer'
                }, {
                    headerName: 'Results',
                    field: 'results',
                    filter: 'agNumberColumnFilter'
                }, {
                    headerName: 'Queue Start',
                    field: 'startedQueueCount',
                    filter: 'agNumberColumnFilter'
                }, {
                    headerName: 'Queue End',
                    field: 'finishedQueueCount',
                    filter: 'agNumberColumnFilter'
                }, {
                    headerName: 'Query',
                    field: 'queryString'
                }, {
                    headerName: 'Error',
                    field: 'error'
                }],
                rows: []
            },
        };
    },
    components: {
        AgGridVue,
        QueryParts
    },
    methods: {
        dateColumn(headerName, field, reverse) {
            let ctrl = this;
            if (typeof field === 'boolean') {
                reverse = field;
                field = headerName.toLowerCase();
            } else if (typeof field === 'undefined') {
                field = headerName.toLowerCase();
            }
            reverse = typeof reverse === 'undefined' ? false : reverse;
            return {
                headerName: headerName,
                field: field,
                filter: 'agDateColumnFilter',
                comparator: ctrl.dateSortComparator,
                valueFormatter: reverse ? ctrl.dateTooltipFormatter : ctrl.dataValueFormatter,
                tooltip: reverse ? ctrl.dataValueFormatter : ctrl.dateTooltipFormatter,
                filterParams: {
                    browserDatePicker: true,
                    applyButton: true,
                    resetButton: true,
                    defaultOption: 'inRange',
                    comparator: ctrl.dateFilterComparator
                }
            };
        },
        createEmptyExecutorDetail() {
            return {
                recentErrors: [],
                statTimes: [],
                finishedStats: [],
                resultStats: [],
            };
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
                queryString: '',
                source: '',
                error: ''
            };
        },
        setRunningStatus(running) {
            if (running == this.selected.running) {
                this.loadShards();
                return;
            }
            this.selected.running = running;
            this.selected.shard = -1;
            this.setRouteParams();
            this.loadShards();
        },
        executorClicked(params) {
            let name = params.data.name;
            if (this.selected.name != name) {
                this.selected.name = name;
                this.selected.shard = -1;
                this.attemptDetailParts = [];
                this.attemptDetailInfo = this.createEmptyAttemptDetailInfo();
                this.setRouteParams();
            }
            this.loadExecutorDetail();
            this.loadShards();
        },
        shardClicked(params) {
            let shard = params.data.index;
            if (this.selected.shard != shard) {
                this.selected.shard = shard;
                this.selected.attempt = null;
                this.attemptDetailParts = [];
                this.attemptDetailInfo = this.createEmptyAttemptDetailInfo();
                this.setRouteParams();
            }
            this.loadQueryParts();
        },
        populateErrors() {
            if (this.selected.name == '') {
                this.errorsGrid.rows = [];
                return;
            }
            this.errorsGrid.rows = this.executorDetail.recentErrors.concat([]);
        },
        loadTestExecutorDetail() {
            let ctrl = this;
            ctrl.loading.detail = false;
            if (ctrl.selected.name == '') {
                console.warn("no executor selected")
                return;
            }
            
            let executor = ctrl.status.executors[ctrl.selected.name];
            if (typeof executor === 'undefined') {
                if (Object.keys(ctrl.status.executors).length == 0) {
                    setTimeout(ctrl.loadTestExecutorDetail, 500);
                }
                console.warn("no test query runners found")
                return;
            }

            let num = function(max) {
                return Math.floor(Math.random() * max);
            }

            let detail = ctrl.createEmptyExecutorDetail();

            let errorCount = executor.recentErrorCount;
            for (let i = 0; i < errorCount; i++) {
                detail.recentErrors.push({
                    time: new Date().getTime() - (1000 * 60 * 60 * num(24)),
                    error: 'error text ' + i
                });
            }

            const interval = 5 * 60 * 1000;
            let statCount = num(24 * (60 / 5)) + 1;
            let nextTime = new Date();
            nextTime.setUTCMinutes(-1 * (nextTime.getUTCMinutes() % 5));
            nextTime = nextTime.getTime() - (statCount * interval);
            for (let i = 0; i < statCount; i++) {
                detail.statTimes.push(nextTime);
                detail.finishedStats.push(num(50));
                detail.resultStats.push(num(1000000));
                nextTime = nextTime + interval;
            }

            ctrl.executorDetail = detail;
            ctrl.populateErrors();
            ctrl.formatDetailStats();
        },
        loadExecutorDetail() {
            let ctrl = this;

            if (ctrl.loading.detail) {
                ctrl.loading.detail = false;
                ctrl.cancelTokens.detail.cancel();
            }

            let url = 'api/executor/' + encodeURIComponent(ctrl.selected.name) + '/status';
            
            ctrl.loading.detail = true;
            if (ctrl.isTest()) {
                let token = setTimeout(ctrl.loadTestExecutorDetail, 500);
                ctrl.cancelTokens.detail = {
                    cancel() {
                        clearTimeout(token);
                    }
                }
                return;
            }

            let token = ctrl.cancelTokens.detail = axios.CancelToken.source();
            axios.get(url, {
                cancelToken: token.token
            }).then((response) => {
                ctrl.loading.detail = false;
                ctrl.executorDetail = response.data;
                ctrl.populateErrors();
                ctrl.formatDetailStats();
            }).catch((response) => {
                ctrl.loading.detail = false;
                if (!axios.isCancel(response)) {
                    ctrl.handleQueryError(response);
                }
            });
        },
        formatDetailStats() {
            let ctrl = this;
            
            let data = {
                labels: [],
                datasets: []
            };

            for (let i = 0; i < ctrl.executorDetail.statTimes.length; i++) {
                let time = ctrl.executorDetail.statTimes[i];
                data.labels.push(moment(time).format('HH:mm'))
            }

            const labels = ['', ' (K)', ' (M)', ' (B)', ' (T)'];

            let determineScale = (counts) => {
                let max = Math.max(...counts);
                let scale = 0;
                while (max >= 1000 && scale < (labels.length - 1)) {
                    scale++;
                    max /= 1000;
                }
                return scale;
            }

            let finishedStats = ctrl.executorDetail.finishedStats;
            let resultStats = ctrl.executorDetail.resultStats;

            let finishedScale = determineScale(finishedStats);
            let resultScale = determineScale(resultStats);

            if (finishedScale > 0) {
                let scale = Math.pow(1000, finishedScale);
                finishedStats = finishedStats.map((num) => Math.round(num / scale));
            }
            if (resultScale > 0) {
                let scale = Math.pow(1000, resultScale);
                resultStats = resultStats.map((num) => Math.round(num / scale));
            }

            data.datasets.push({
                label: 'Finished' + labels[finishedScale],
                borderColor: 'blue',
                fill: false,
                data: finishedStats
            });
            data.datasets.push({
                label: 'Results' + labels[resultScale],
                borderColor: 'red',
                fill: false,
                data: resultStats
            });

            ctrl.executorDetailChart.data.data = data;
            ctrl.executorDetailChart.chart.update();
        },
        loadTestShards() {
            let ctrl = this;
            ctrl.loading.shards = false;
            if (ctrl.selected.name == '') {
                console.warn("no test query name selected")
                return;
            }
            let executor = ctrl.status.executors[ctrl.selected.name];
            if (typeof executor === 'undefined') {
                if (Object.keys(ctrl.status.executors).length == 0) {
                    setTimeout(ctrl.loadTestShards, 500);
                }
                console.warn("no test executor found")
                return;
            }
            let count = ctrl.selected.running ? executor.running : executor.finished;

            let num = function(max) {
                return Math.floor(Math.random() * max);
            }

            let runners = Object.keys(ctrl.status.queryRunners);

            let shards = [];
            for (let i = 0; i < count; i++) {
                let started = 0;
                let finished = 0;
                let error = null;
                if (ctrl.selected.running) {
                    started = new Date().getTime() - (1000 * (num(50) + 10));
                } else {
                    started = new Date().getTime() - (1000 * (num(120) + 20));
                    finished = started + (1000 * (num(10) + 1));
                    if (num(10) == 0) {
                        error = 'this is an error string';
                    }
                }
                let shard = num(24);
                if (shard < 10) {
                    shard = '000' + shard;
                } else {
                    shard = '00' + shard;
                }
                shard += '_' + num(100);

                shards.push({
                    index: i,
                    started: started,
                    finished: finished,
                    shard: shard,
                    sourceServer: runners[num(runners.length)],
                    startedQueueCount: num(20),
                    finishedQueueCount: num(20),
                    queryString: 'this is a query for query-string-' + i,
                    results: num(10000),
                    error: error
                });
            }
            ctrl.updateGrid(ctrl.shardsGrid, shards, 'index');
            ctrl.shardsGrid.gridOptions.api.sizeColumnsToFit();
        },
        loadShards() {
            let ctrl = this;

            if (ctrl.loading.shards) {
                ctrl.loading.shards = false;
                ctrl.cancelTokens.shards.cancel();
            }

            let url = 'api/executor/' + encodeURIComponent(ctrl.selected.name);
            if (ctrl.selected.running) {
                url += '/running';
            } else {
                url += '/finished';
            }

            ctrl.loading.shards = true;
            if (ctrl.isTest()) {
                let token = setTimeout(ctrl.loadTestShards, 1000);
                ctrl.cancelTokens.shards = {
                    cancel() {
                        clearTimeout(token);
                    }
                }
                return;
            }
            let token = ctrl.cancelTokens.shards = axios.CancelToken.source();
            axios.get(url, {
                cancelToken: token.token
            }).then((response) => {
                ctrl.loading.shards = false;
                ctrl.updateGrid(ctrl.shardsGrid, response.data, 'index');
            }).catch((response) => {
                ctrl.loading.shards = false;
                if (!axios.isCancel(response)) {
                    ctrl.handleQueryError(response);
                }
            });

        },
        loadTestQueryParts() {
            let ctrl = this;
            ctrl.loading.parts = false;

            let index = ctrl.selected.shard;
            
            let num = function(max) {
                return Math.floor(Math.random() * max);
            };

            let shard = null;
            ctrl.shardsGrid.gridOptions.api.forEachNode((node) => {
                if (node.data.index == index) {
                    shard = node.data;
                }
            });

            if (shard == null) {
                console.error("no test shard found with index " + index + " in shards grid");
                return;
            }

            let duration = shard.finished == 0 ? new Date().getTime() : shard.finished;
            duration -= shard.started;
            ctrl.attemptDetailInfo = {
                index: index,
                shard: shard.shard,
                started: moment(shard.started).format("MM/DD/YYYY HH:mm:ss"),
                finished: shard.finished == 0 ? 'Not Finished' : moment(shard.finished).format("MM/DD/YYYY HH:mm:ss"),
                duration: ctrl.durationValueFormatter({ value: duration }),
                results: shard.results,
                startedQueueCount: shard.startedQueueCount,
                finishedQueueCount: shard.finishedQueueCount,
                queryString: shard.queryString,
                sourceServer: shard.sourceServer,
                error: shard.error
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

            let count = shard.finished == 0 ? (num(20) + 1) : (num(50) + 1);

            let finished = shard.finished == 0 ? new Date().getTime() : shard.finished;
            this.attemptDetailParts = recursiveCreate(shard.started, finished, count);
        },
        loadQueryParts() {
            let ctrl = this;

            if (ctrl.selected.shard == -1 || ctrl.selected.name == '') {
                this.attemptDetailParts = [];
                return;
            }

            if (ctrl.loading.parts) {
                ctrl.loading.parts = false;
                ctrl.cancelTokens.parts.cancel();
            }

            let url = 'api/executor/' + encodeURIComponent(ctrl.selected.name) + "/" + ctrl.selected.shard;

            ctrl.loading.parts = true;
            if (ctrl.isTest()) {
                let token = setTimeout(ctrl.loadTestQueryParts, 1000);
                ctrl.cancelTokens.parts = {
                    cancel() {
                        clearTimeout(token);
                    }
                }
                return;
            }
            
            let token = ctrl.cancelTokens.parts = axios.CancelToken.source();
            axios.get(url, {
                cancelToken: token.token
            }).then((response) => {
                ctrl.loading.parts = false;
                let entry = response.data;

                let duration = entry.info.finished == 0 ? new Date().getTime() : entry.info.finished;
                duration -= entry.info.started;

                entry.info.started = moment(entry.info.started).format("MM/DD/YYYY HH:mm:ss"),
                entry.info.finished = entry.info.finished == 0 ? 'Not Finished' : moment(entry.info.finished).format("MM/DD/YYYY HH:mm:ss"),
                entry.info.duration = ctrl.durationValueFormatter({ value: duration }),
                ctrl.attemptDetailInfo = entry.info;
                ctrl.attemptDetailParts = entry.queryParts;
            }).catch((response) => {
                ctrl.loading.parts = false;
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
        setRouteParams() {
            let ctrl = this;
            let params = {
                running: ctrl.selected.running
            };
            if (ctrl.selected.name != '') {
                params.name = ctrl.selected.name;
                if (ctrl.selected.shard != -1) {
                    params.shard = ctrl.selected.shard;
                }
            }
            ctrl.$router.push({
                path: '/executors',
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
        dateTooltipFormatter(params) {
            if (params.value == 0) {
                return 'Never';
            } else {
                return moment(params.value).format("MM/DD/YYYY HH:mm:ss");
            }
        },
        dataValueFormatter(params) {
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
        formatExecutors() {
            let ctrl = this;

            let rows = [];
            for (let name in ctrl.status.executors) {
                let status = ctrl.status.executors[name];
                rows.push({
                    name: name,
                    running: status.running,
                    finished: status.finished,
                    errors: status.recentErrorCount,
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

            ctrl.updateGrid(ctrl.executorGrid, rows, 'name');
        },
        handleResize() {
            let executorGrid = document.getElementById("executorGrid");
            let windowHeight = window.innerHeight;
            let top = executorGrid.offsetTop
            let amount = Math.round((windowHeight - (top + 230)) / 2);
            executorGrid.style.height = amount + "px";
            document.getElementById("errorsGrid").style.height = executorGrid.style.height;
            document.getElementById("executorStats").style.height = executorGrid.style.height;
            document.getElementById("shardsGrid").style.height = executorGrid.style.height;

            if (this.executorDetailChart.chart != null) {
                this.executorDetailChart.chart.update();
            }
        }
    },
    mounted() {
        let ctrl = this;
        
        let params = ctrl.$route.query;
        if (typeof params.name !== 'undefined') {
            ctrl.selected.name = params.name;
            ctrl.loadShards();
            ctrl.loadExecutorDetail();
        }
        
        ctrl.$parent.$on('generalStatusChanged', function(status) {
            ctrl.$emit('generalStatusChanged', status);
        });

        ctrl.$on('generalStatusChanged', function(status) {
            ctrl.status = status;
            ctrl.formatExecutors();
        });
        ctrl.$parent.$emit('getStatus');

        window.addEventListener("resize", ctrl.handleResize);
        ctrl.handleResize();
        ctrl.executorGrid.gridOptions.api.sizeColumnsToFit();
        ctrl.executorDetailChart.chart = new Chart(document.getElementById('executorStats'), ctrl.executorDetailChart.data);
    },
    unmounted() {
        window.removeEventListener("resize", this.handleResize);
    }
}
</script>

<style>
.col-md-6, .col-md-9, .col-md-3, .col-md-4, .col-md-8, .col-md-12 {
    display: inline-block;
    margin-top: 10px;
}

.col-md-12:first-child {
    margin-top: 0px;
}

.btn-refresh {
    margin-right: 10px;
}
.executors-container {
    padding-bottom: 20px;
}
</style>