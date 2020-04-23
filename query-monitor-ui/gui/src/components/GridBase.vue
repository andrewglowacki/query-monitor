<template>
    <ag-grid-vue style="width: 100%; height: 200px"
                class="ag-theme-balham"
                :columnDefs="agColumns"
                :rowData="agRows"
                :gridOptions="gridOptions"
                :defaultColDef="columnDefaults">
    </ag-grid-vue>
</template>

<script>
import moment from 'moment';
import { AgGridVue } from 'ag-grid-vue';
import "ag-grid-community/dist/styles/ag-grid.css";
import "ag-grid-community/dist/styles/ag-theme-balham.css";

export default {
    name: 'GridBase',
    props: ['columns', 'rows', 'gridOptions', 'size', 'updateKey', 'durationDates'],
    components: {
        AgGridVue
    },
    data() {
        return {
            sized: false,
            options: {},
            agColumns: [],
            agRows: [],
            columnDefaults: {
                sortable: true,
                editable: false,
                resizable: true,
                filter: true,
                filterParams: {
                    applyButton: true,
                    resetButton: true
                }
            }
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
            return this.durationDates ? this.dateDurationFormatter(params) : this.dateTimeFormatter(params);
        },
        dateTooltipFormatter(params) {
            return this.durationDates ? this.dateTimeFormatter(params) : this.dateDurationFormatter(params);
        },
        createColumns() {
            let agColumns = [];
            this.columns.forEach((def) => {
                let label = def.label;
                let field = def.field;
                if (typeof field === 'undefined') {
                    field = label.toLowerCase();
                }
                let type = def.type;
                if (typeof type === 'undefined') {
                    type = '';
                }
                type = type.toLowerCase();

                let column = {
                    headerName: label,
                    field:  field
                };

                if (type == 'date') {
                    column = this.dateColumn(label, field);
                } else if (type == 'duration') {
                    column.valueFormatter = this.durationValueFormatter;
                    column.tooltipValueGetter = this.durationTooltip;
                    column.filter = 'agNumberColumnFilter';
                } else if (type == 'mem') {
                    column.valueFormatter = this.formatMem;
                    column.filter = 'agNumberColumnFilter';
                } else if (type == 'count') {
                    column.filter = 'agNumberColumnFilter';
                }
                agColumns.push(column)
                
            });
            this.agColumns = agColumns;
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
        updateGrid() {
            let newRows = this.rows;
            if (typeof this.updateKey === 'undefined') {
                this.agRows = newRows.concat([]);
                return;
            }

            let key = this.updateKey;

            let newMap = {};
            for (let i = 0; i < newRows.length; i++) {
                newMap[newRows[i][key]] = newRows[i];
            }

            let oldMap = {};
            this.gridOptions.api.forEachNode((node) => {
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
                this.gridOptions.api.updateRowData({
                    add: add,
                    remove: remove
                });
            }

            if (Object.keys(update).length > 0) {
                this.gridOptions.api.forEachNode((node) => {
                    let row = update[node.data[key]];
                    if (typeof row !== 'undefined') {
                        node.setData(row);
                    }
                });
            }
        },
        handleResize() {
            let adjustSize = 2;
            if (typeof this.size !== 'undefined') {
                if (this.size == 'full') {
                    adjustSize = 1;
                }
            }
            let bufferAmount = 20 + (100 * adjustSize);

            let elem = this.$el;
            let windowHeight = window.innerHeight;
            let top = elem.parentElement.children[0].offsetTop;
            let amount = Math.round((windowHeight - (top + bufferAmount)) / adjustSize);
            elem.style.height = amount + "px";
        }
    },
    mounted() {
        this.createColumns();
        this.updateGrid();

        window.addEventListener("resize", this.handleResize);
        this.handleResize();
    },
    unmounted() {
        window.removeEventListener("resize", this.handleResize);
    },
    watch: {
        columns() {
            this.createColumns();
        },
        rows() {
            this.updateGrid();
            if (!this.sized && this.rows.length > 0) {
                this.sized = true;
                this.gridOptions.api.sizeColumnsToFit();
            }
        }
    }
}
</script>

<style>

</style>