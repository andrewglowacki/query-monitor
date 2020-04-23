<template>
    <tbody>
        <tr :class="{ 'even': even }">
            <td>
                <span v-if="part.children.length == 0" class="fa fa-leaf leaf-toggle"></span>
                <span v-if="part.children.length > 0" class="fa toggle" 
                    :class="{ 'fa-chevron-circle-right': !expanded, 'fa-chevron-circle-down': expanded }" 
                    @click="expanded = !expanded">
                </span>
            </td>
            <td v-text="part.partString"></td>
            <td v-text="part.started"></td>
            <td v-text="part.finished"></td>
            <td v-text="part.duration"></td>
            <td v-text="part.results"></td>
        </tr>
        <tr v-if="expanded && part.children.length > 0">
            <td></td>
            <td class="part-children" colspan="5">
                <QueryParts :parts="part.children"></QueryParts>
            </td>
        </tr>
    </tbody>
</template>

<script>
export default {
    props: ['part', 'idx'],
    name: 'QueryPart',
    data() {
        return {
            even: false,
            expanded: false
        }
    },
    components: {
        QueryParts: () => import('./QueryParts.vue')
    },
    mounted() {
        this.even = parseInt(this.idx) % 2 == 0;
    }
}
</script>

<style>
tr.even {
    background-color: #EEF;
}
.part td:first-child {
    background-color: #EFE;
    text-align: center;
}
.toggle {
    cursor: pointer;
}
td.part-children {
    padding-left: 0px;
}
.leaf-toggle {
    color: #aca;
}
</style>