<template>
    <div class="part">
        <table class="table table-borderless table-sm">
            <thead>
                <th></th>
                <th>Query</th>
                <th>Started</th>
                <th>Finished</th>
                <th>Duration</th>
                <th>Results</th>
            </thead>
            <QueryPart v-for="(part, index) in parts" :idx="index" v-bind:key="part.id" :part="part"></QueryPart>
        </table>
    </div>
</template>

<script>
import QueryPart from './QueryPart.vue';
import moment from 'moment';

export default {
    props: ['parts'],
    name: 'QueryParts',
    components: {
        QueryPart
    },
    methods: {
        processDetailParts(parts) {
            for (let i = 0; i < parts.length; i++) {
                let part = parts[i];
                if (typeof part.duration !== 'undefined') {
                    continue;
                }
                
                let duration = (part.finished == 0 ? new Date().getTime() : part.finished) - part.started;
                if (duration < 5000) {
                    duration = '' + duration + ' ms';
                } else if (duration < 120000) {
                    duration = '' + Math.round(duration / 1000) + ' seconds';
                } else {
                    duration = moment.duration(duration).humanize();
                }

                part.id = '' + part.started + part.finished + part.results + part.partString;
                part.duration = duration;
                part.started = moment(part.started).format("MM/DD/YYYY HH:mm:ss");
                part.finished = moment(part.finished).format("MM/DD/YYYY HH:mm:ss");
            }
        }
    },
    mounted() {
        try {
            this.processDetailParts(this.parts);
        } catch (ex) {
            console.error(ex);
        }
    },
    watch: {
        parts(newParts) {
            this.processDetailParts(newParts);
        }
    }
}
</script>

<style>
.part {
    margin-bottom: 10px;
    border-left: 1px gray dashed
}
.part .table {
    margin-bottom: 0px;
}
.part th:first-child {
    background-color: #EFE;
    width: 40px;
}
.part .table {
    border-top: 1px #EEE solid;
    border-bottom: 1px #EEE solid;
    border-right: 1px #EEE solid;
}
</style>