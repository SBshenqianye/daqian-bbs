import Vue from 'vue'
import App from './App.vue'
//引入 router
import VueRouter from 'vue-router';
import router from "@/router";
//import store from "@/store";

// 引入 Tailwind CSS 全局样式
import './assets/tailwind.css'

//按需引入element-ui
import {
    Carousel,
    CarouselItem,
    Button,
    Container,
    Col,
    Input,
    Main,
    MessageBox,
    Dialog,
    FormItem,
    Select,
    Option,
    CheckboxGroup,
    Checkbox,
    Form,
    Switch,
    Radio,
    TimePicker,
    Tooltip,
    Upload,
    Cascader,
    Row,
    RadioGroup,
    Tag,
    Avatar,
    Tabs,
    TabPane,
    Table,
    TableColumn,
    Tree,
} from 'element-ui';

//引入 封装好的请求
import {postRequest} from "@/api/api";
import {putRequest} from "@/api/api";
import {getRequest} from "@/api/api";
import {deleteRequest} from "@/api/api";

// 引入阿里矢量图标
import './assets/iconfont/iconfont.css'



//关闭生产提示
Vue.config.productionTip = false

Vue.use(VueRouter)

Vue.component(Carousel.name,Carousel)
Vue.component(CarouselItem.name,CarouselItem)
Vue.component(Button.name,Button)
Vue.component(Container.name,Container)
Vue.component(Col.name, Col)
Vue.component(Input.name,Input)
Vue.component(Main.name,Main)
Vue.component(MessageBox.name,MessageBox),
Vue.component(Dialog.name,Dialog)
Vue.component(FormItem.name,FormItem)
Vue.component(Select.name,Select)
Vue.component(Option.name,Option)
Vue.component(CheckboxGroup.name,CheckboxGroup)
Vue.component(Checkbox.name,Checkbox)
Vue.component(Form.name,Form)
Vue.component(Switch.name,Switch)
Vue.component(Radio.name,Radio)
Vue.component(TimePicker.name,TimePicker)
Vue.component(Tooltip.name,Tooltip)
Vue.component(Upload.name,Upload)
Vue.component(Cascader.name,Cascader)
Vue.component(Row.name,Row)
Vue.component(RadioGroup.name,RadioGroup)
Vue.component(Tag.name,Tag)
Vue.component(Avatar.name,Avatar)
Vue.component(Tabs.name,Tabs)
Vue.component(TabPane.name,TabPane)
Vue.component(Table.name,Table)
Vue.component(TableColumn.name,TableColumn)
Vue.component(Tree.name,Tree)


Vue.prototype.$confirm = MessageBox.confirm;

//插件形式使用请求
Vue.prototype.postRequest = postRequest;
Vue.prototype.putRequest = putRequest;
Vue.prototype.getRequest = getRequest;
Vue.prototype.deleteRequest = deleteRequest;

// 需要登录才能访问的路径（未登录时直接跳转登录页）
const authPaths = ['/write', '/userinfo', '/stat'];
const needAuth = (path) => authPaths.some(p => path === p || path.startsWith(p + '/'));

router.beforeEach(((to, from, next) => {
    var hasToken = window.sessionStorage.getItem('tokenStr');
    if (needAuth(to.path) && !hasToken) {
        next({ path: '/login', query: { redirect: to.fullPath } });
        return;
    }

    // 首次登录强制改密：除改密页和登录页外，isFirstLogin==1 时一律拦截
    if (hasToken && to.path !== '/change-password' && to.path !== '/login') {
        try {
            var user = JSON.parse(window.sessionStorage.getItem('user') || '{}');
            if (user.isFirstLogin === 1) {
                next({ path: '/change-password', query: { redirect: to.fullPath } });
                return;
            }
        } catch(e) {}
    }

    next();
}))


new Vue({
    el:'#app',
    render: h => h(App),
    router:router,

    beforeCreate() {
        //安装全局事件总线
        Vue.prototype.$bus = this
    },

})

