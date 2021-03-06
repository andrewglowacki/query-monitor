import Vue from 'vue'
import App from './App.vue'
import 'bootstrap/dist/js/bootstrap.bundle'
import BootstrapVue from 'bootstrap-vue'
import VueRouter from 'vue-router'
import 'bootstrap/dist/css/bootstrap.css'
import 'bootstrap-vue/dist/bootstrap-vue.css'
import '@fortawesome/fontawesome-free/css/all.css'
import Dashboard from './components/Dashboard.vue'
import Runners from './components/runners/Runners.vue'
import Executors from './components/executors/Executors.vue'
import Scans from './components/Scans.vue'

Vue.use(BootstrapVue);
Vue.use(VueRouter);

const routes = [
    { path: '/', component: Dashboard },
    { path: '/runners', component: Runners },
    { path: '/executors', component: Executors },
    { path: '/scans', component: Scans }
];

const router = new VueRouter({
    routes: routes
})

Vue.config.productionTip = false

new Vue({
    render: h => h(App),
    router: router
}).$mount('#app')
