<template>
    <div id="app">
        <div class="navbar navbar-expand-lg navbar-light bg-light" style="margin-bottom: 10px">
            <a class="navbar-brand" href="#/">Query Monitor</a>
            <div class="collapse navbar-collapse">
                <ul class="navbar-nav mr-auto">
                    <li class="nav-item">
                        <a class="nav-link" href="#/runners" :class="{ 'active': currentPath == '/runners' }">
                            <i class="fa fa-play"></i> Runners
                        </a>
                    </li>
                    <li class="nav-item" :class="{ 'active': currentPath == '/executors' }">
                        <a class="nav-link" href="#/executors">
                            <i class="fa fa-list"></i> Executors
                        </a>
                    </li>
                    <li class="nav-item" :class="{ 'active': currentPath == '/scans' }">
                        <a class="nav-link" href="#/scans">
                            <i class="fa fa-cloud"></i> Scans
                        </a>
                    </li>
                    <li class="nav-item dropdown">
                        <a class="nav-link dropdown-toggle" href="#" id="optionsDropdown" role="button" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
                            <i class="fa fa-cog"></i> Options
                        </a>
                        <div class="dropdown-menu" aria-labelledby="optionsDropdown" style="width: 315px">
                            <form class="px-3 py-3">
                                <div class="form-group" style="margin-bottom: 10px;">
                                    <label>Time Format:</label>
                                    <div class="btn-group" style="height: 30px; margin-left: 10px">
                                        <button class="btn btn-secondary btn-sm" :class="{ 'active': options.durationDates }" @click="setOption('durationDates', true)">
                                            <i class="fa fa-check text-success" v-if="options.durationDates"></i>
                                            Durations
                                        </button>
                                        <button class="btn btn-secondary btn-sm" :class="{ 'active': !options.durationDates }" @click="setOption('durationDates', false)">
                                            <i class="fa fa-check text-success" v-if="!options.durationDates"></i>
                                            Date/Time
                                        </button>
                                    </div>
                                </div>
                                <div class="form-group" style="margin-bottom: 0px;">
                                    <label>Test Mode:</label>
                                    <div class="btn-group" style="height: 30px; margin-left: 10px">
                                        <button class="btn btn-secondary btn-sm" :class="{ 'active': options.test }" @click="setOption('test', true)">
                                            <i class="fa fa-check text-success" v-if="options.test"></i>
                                            Yes
                                        </button>
                                        <button class="btn btn-secondary btn-sm" :class="{ 'active': !options.test }" @click="setOption('test', false)">
                                            <i class="fa fa-check text-success" v-if="!options.test"></i>
                                            No
                                        </button>
                                    </div>
                                </div>
                            </form>
                        </div>
                    </li>
                </ul>
                <span class="navbar-text">
                    <span v-if="loading">
                        <i class="fa fa-spinner fa-spin"></i> Loading executor/runner statuses...
                    </span>
                    <span v-if="!loading && error == ''">
                        There are
                        <a href="#/runners">{{status.queryRunnerCount}} runners</a> and
                        <a href="#/executors">{{status.executorCount}} executors</a> active
                    </span>
                    <span class="text-danger" v-if="!loading && error != ''">
                        <i class="fa fa-exclamation-triangle"></i> {{error}}
                    </span>
                </span>
            </div>
        </div>
        <router-view></router-view>
    </div>
</template>

<script>
import axios from 'axios';
import "ag-grid-community/dist/styles/ag-grid.css";
import "ag-grid-community/dist/styles/ag-theme-balham.css";

export default {
    name: "App",
    components: {},
    data() {
        return {
            loading: true,
            error : '',
            options: {
                durationDates: false,
                test: false
            },
            currentPath: '/',
            test: true,
            status: {
                executors: [],
                queryRunners: [],
                executorCount: 0,
                queryRunnerCount: 0
            }
        };
    },
    methods: {
        saveOptions() {
            window.localStorage.setItem('options', JSON.stringify(this.options));
            this.$emit('optionsChanged', this.options);
        },
        setOption(option, value) {
            if (this.options[option] != value) {
                this.options[option] = value;
                this.saveOptions();
            }
        },
        loadTestData() {
            let status = { 
                executors: {},
                queryRunners: {},
                executorCount: 0,
                queryRunnerCount: 0
            };

            let num = function(max) {
                return Math.round(Math.random() * max);
            }

            let racks = num(6) + 1;
            let nodes = num(5) + 6;
            let max = 1024 * 1024 * 1024 * 4;
            let upSince = new Date().getTime() - (1000 * 60 * 60 * 24 * (num(30) + 1));

            let createEntity = function(runner) {
                
                return {
                    running: num(runner ? 6 : 20),
                    finished: num(1000),
                    lastHeard: new Date().getTime() - (num(10) * 1000),
                    health: {
                        memoryMax: max,
                        memoryUsed: Math.round(Math.random() * max),
                        threads: num(40) + 10,
                        mostRecentError: Math.random() > 0.9 ? (new Date().getTime() - (1000 * 60)) : 0,
                        upSince: upSince,
                        gcCount: num(100),
                        gcTime: (num(1000) * 1000)
                    }
                };
            };

            for (let r = 0; r < racks; r++) {
                for (let n = 0; n < nodes; n++) {
                    let name = 'r' + r + 'n' + n + '-node';
                    let entity = createEntity(false);
                    entity.recentErrorCount = Math.max(num(100) - 90, 0);
                    status.executors[name] = entity;
                }
            }
            status.executorCount = racks * nodes;

            let runners = num(7) + 3;
            for (let r = 0; r < runners; r++) {
                let errors = num(100) - 90;
                let entity = createEntity(true);
                entity.recentErrors = [];
                for (let i = 0; i < errors; i++) {
                    entity.recentErrors.push({
                        time: new Date().getTime() - (1000 * 60 * 60 * num(24)),
                        error: 'error text ' + i
                    });
                }
                status.queryRunners['runner-' + r] = entity;
            }
            status.queryRunnerCount = runners;
            this.status = status;
            this.loading = false;
            this.$emit("generalStatusChanged", status);
            setTimeout(this.getGeneralStatus, 30000);
        },
        getGeneralStatus() {
            let ctrl = this;
            if (ctrl.error != '') {
                ctrl.loading = true;
                ctrl.error = '';
            }

            if (ctrl.options.test) {
                setTimeout(ctrl.loadTestData, 500);
                return;
            }

            axios.get('api/status').then((response) => {
                ctrl.loading = false;
                ctrl.status = response.data;
                setTimeout(ctrl.getGeneralStatus, 30000);
                ctrl.$emit("generalStatusChanged", ctrl.status);
            }).catch((response) => {
                ctrl.loading = false;
                setTimeout(ctrl.getGeneralStatus, 30000);
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
            });
        }
    },
    mounted() {
        let ctrl = this;
        let optionsStr = window.localStorage.getItem('options');
        if (optionsStr != null) {
            ctrl.options = JSON.parse(optionsStr);
            ctrl.$emit('optionsChanged', ctrl.options);
        }
        ctrl.$on('getOptions', function() {
            ctrl.$emit('optionsChanged', ctrl.options);
        });
        ctrl.getGeneralStatus();
        ctrl.$on('getStatus', function() {
            ctrl.$emit("generalStatusChanged", ctrl.status);
        });
        ctrl.$router.afterEach((to) => {
            ctrl.currentPath = to.path;
        });
        ctrl.currentPath = ctrl.$route.path;
    }
};
</script>

<style>
</style>
