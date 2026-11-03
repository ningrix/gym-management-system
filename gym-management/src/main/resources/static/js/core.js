/* 基础设施：请求封装、通用工具、登录与整体布局 */
window.Gym = { pages: {} };
(function (Gym) {
    /**
     * 会话存储：按“角色域”分键，使同一浏览器内三个标签页可分别登录三种角色。
     * - scope 记在 sessionStorage，标签页内刷新保留、跨标签页不共享；
     * - 带 ?role=admin|member|coach 打开时以 URL 为准（覆盖 window.open 复制过去的 scope）；
     * - token / user 以 gym_token_<scope> 的形式存 localStorage，刷新后仍在线。
     */
    const ROLE_SCOPE = { ADMIN: 'admin', MEMBER: 'member', COACH: 'coach' };
    Gym.session = {
        init() {
            const urlRole = (new URLSearchParams(location.search).get('role') || '').toLowerCase();
            if (['admin', 'member', 'coach'].indexOf(urlRole) >= 0) {
                sessionStorage.setItem('gym_scope', urlRole);
            }
        },
        scope() {
            return sessionStorage.getItem('gym_scope') || '';
        },
        key(name) {
            const s = this.scope();
            return s ? 'gym_' + name + '_' + s : 'gym_' + name;
        },
        get(name) {
            return localStorage.getItem(this.key(name));
        },
        set(name, value) {
            localStorage.setItem(this.key(name), value);
        },
        remove(name) {
            localStorage.removeItem(this.key(name));
        },
        /** 仅清除当前角色域的登录态，不影响其他标签页里的角色 */
        clearLogin() {
            this.remove('token');
            this.remove('user');
        },
        setScope(role) {
            const s = ROLE_SCOPE[role] || (role || '').toLowerCase();
            if (['admin', 'member', 'coach'].indexOf(s) >= 0) {
                sessionStorage.setItem('gym_scope', s);
            }
        }
    };
    Gym.session.init();

    function onUnauthorized() {
        Gym.session.clearLogin();
        location.hash = '';
        location.reload();
    }

    const http = axios.create({ baseURL: '', timeout: 20000 });
    http.interceptors.request.use(cfg => {
        const token = Gym.session.get('token');
        if (token) cfg.headers['X-Token'] = token;
        return cfg;
    });
    http.interceptors.response.use(resp => {
        const body = resp.data;
        if (body && body.code !== undefined && body.code !== 200) {
            ElementPlus.ElMessage.error(body.msg || '操作失败');
            if (body.code === 401) {
                onUnauthorized();
            }
            return Promise.reject(body);
        }
        return body.data;
    }, err => {
        // 后端按 HTTP 状态码返回时，业务提示在 err.response.data 中
        const body = err.response && err.response.data;
        if (body && body.code) {
            ElementPlus.ElMessage.error(body.msg || '操作失败');
            if (body.code === 401) {
                onUnauthorized();
            }
            return Promise.reject(body);
        }
        ElementPlus.ElMessage.error('请求失败：' + (err.message || '网络异常'));
        return Promise.reject(err);
    });

    Gym.api = {
        get: (url, params) => http.get(url, { params }),
        post: (url, data) => http.post(url, data),
        put: (url, data) => http.put(url, data),
        del: url => http.delete(url)
    };
    Gym.msg = {
        ok: m => ElementPlus.ElMessage.success(m || '操作成功'),
        err: m => ElementPlus.ElMessage.error(m || '操作失败')
    };
    Gym.date = v => (v ? String(v).slice(0, 10) : '');
    Gym.time = v => (v ? String(v).slice(0, 5) : '');
    Gym.money = v => '¥ ' + Number(v || 0).toFixed(2);
    Gym.statusText = {
        ACTIVE: '正常', FROZEN: '已冻结', EXPIRED: '已过期', REFUNDED: '已退卡',
        BOOKED: '已预约', CHECKED_IN: '已签到', COMPLETED: '已完成',
        CANCELLED: '已取消', NO_SHOW: '未到场',
        WAITING: '待开课', ONGOING: '进行中', FINISHED: '已结束',
        NORMAL: '正常', REPAIR: '维修中', SCRAPPED: '已报废'
    };
    Gym.statusTag = {
        ACTIVE: 'success', FROZEN: 'warning', EXPIRED: 'info', REFUNDED: 'info',
        BOOKED: 'primary', CHECKED_IN: 'success', COMPLETED: '', CANCELLED: 'info', NO_SHOW: 'danger',
        WAITING: 'primary', ONGOING: 'warning', FINISHED: 'info', CANCELLED_S: 'info',
        NORMAL: 'success', REPAIR: 'warning', SCRAPPED: 'info'
    };
    Gym.courseType = {
        YOGA: '瑜伽', SPIN: '动感单车', BOXING: '搏击', AEROBIC: '有氧操',
        STRENGTH: '力量训练', PILATES: '普拉提'
    };
    /* 标签配色与文案映射（模板中直接用标识符取值，避免对象字面量被解析成代码块） */
    Gym.resTag = { BOOKED: 'primary', CHECKED_IN: 'success', COMPLETED: '', CANCELLED: 'info', NO_SHOW: 'danger' };
    Gym.scheduleTag = { WAITING: 'primary', ONGOING: 'warning', FINISHED: 'info', CANCELLED: 'info' };
    Gym.equipTag = { NORMAL: 'success', REPAIR: 'warning', SCRAPPED: 'info' };
    Gym.diffTag = { 1: 'success', 2: 'warning', 3: 'danger' };
    Gym.categoryText = { CARDIO: '有氧器械', STRENGTH: '力量器械', OTHER: '辅助器材' };
    Gym.diffText = { 1: '入门', 2: '进阶', 3: '高强度' };
    Gym.payType = { CASH: '现金', WECHAT: '微信', ALIPAY: '支付宝', CARD: '银行卡' };
    Gym.targetRole = { ALL: '全部', MEMBER: '仅会员', COACH: '仅教练' };

    Gym.define = (name, options) => { Gym.pages[name] = options; };

    Gym.rootOptions = {
        data() {
            const scope = Gym.session.scope();
            const fill = { admin: 'admin', member: 'member01', coach: 'coach01' }[scope] || '';
            return {
                token: Gym.session.get('token') || '',
                user: JSON.parse(Gym.session.get('user') || 'null') || {},
                scope: scope,
                scopeLabel: { admin: '管理员端', member: '会员端', coach: '教练端' }[scope] || '',
                loginForm: { username: fill, password: fill ? (scope === 'admin' ? 'admin2026' : 'gym2026') : '' },
                loginLoading: false,
                page: 'login',
                pwdVisible: false,
                pwdForm: { oldPassword: '', newPassword: '' }
            };
        },
        computed: {
            logged() {
                return !!this.token;
            },
            role() {
                return this.user.role;
            },
            roleLabel() {
                return { ADMIN: '管理员', MEMBER: '会员', COACH: '教练' }[this.user.role] || '';
            },
            roleClass() {
                return { ADMIN: '', MEMBER: 'member', COACH: 'coach' }[this.user.role] || '';
            },
            menus() {
                const r = this.user.role;
                if (r === 'ADMIN') {
                    return [
                        { title: '数据概览', items: [{ key: 'dashboard', label: '工作台' }, { key: 'report', label: '统计报表' }] },
                        { title: '会员管理', items: [{ key: 'member', label: '会员信息' }, { key: 'member-card', label: '会员卡管理' }] },
                        { title: '教务管理', items: [
                            { key: 'coach', label: '教练管理' }, { key: 'course', label: '课程管理' },
                            { key: 'schedule', label: '排课管理' }, { key: 'reservation', label: '预约与签到' }] },
                        { title: '场馆运营', items: [
                            { key: 'body', label: '体测记录' }, { key: 'equipment', label: '器材管理' },
                            { key: 'alerts', label: '会员预警' }, { key: 'checkins', label: '到店记录' },
                            { key: 'notice', label: '公告管理' }] }
                    ];
                }
                if (r === 'MEMBER') {
                    return [
                        { title: '会员服务', items: [
                            { key: 'my-profile', label: '个人中心' }, { key: 'my-course', label: '课程预约' },
                            { key: 'coach-showcase', label: '教练风采' }] },
                        { title: '拓展', items: [
                            { key: 'exercise', label: '动作库' }] },
                        { title: '通知', items: [{ key: 'notice', label: '场馆公告' }] }
                    ];
                }
                return [
                    { title: '教练工作台', items: [
                        { key: 'coach-home', label: '今日概览' }, { key: 'schedule', label: '我的课表' },
                        { key: 'reservation', label: '预约名单' }, { key: 'body', label: '体测记录' }] },
                    { title: '通知', items: [{ key: 'notice', label: '场馆公告' }] }
                ];
            },
            currentTitle() {
                for (const g of this.menus) {
                    for (const i of g.items) if (i.key === this.page) return i.label;
                }
                return '工作台';
            },
            subTitle() {
                return '';
            }
        },
        mounted() {
            if (this.logged) {
                this.page = this.defaultPage();
            }
            window.addEventListener('hashchange', this.onHash);
        },
        methods: {
            allowedKeys() {
                return this.menus.reduce((acc, g) => acc.concat(g.items.map(i => i.key)), []);
            },
            defaultPage() {
                const r = this.user.role;
                const hash = (location.hash || '').replace('#/', '');
                if (hash && hash !== 'login' && this.allowedKeys().indexOf(hash) >= 0) return hash;
                if (r === 'MEMBER') return 'my-profile';
                if (r === 'COACH') return 'coach-home';
                return 'dashboard';
            },
            onHash() {
                const h = (location.hash || '').replace('#/', '');
                if (!h || h === 'login') return;
                if (this.allowedKeys().indexOf(h) < 0) {
                    Gym.msg.err('当前窗口以“' + this.scopeLabel + '”登录，无法访问该页面');
                    location.hash = '#/' + this.page;
                    return;
                }
                this.page = h;
            },
            openExternal(item) {
                window.open(item.href, '_blank', 'noopener');
            },
            go(key) {
                if (this.allowedKeys().indexOf(key) < 0) {
                    Gym.msg.err('当前窗口以“' + this.scopeLabel + '”登录，无法访问该页面');
                    return;
                }
                this.page = key;
                location.hash = '#/' + key;
            },
            async doLogin() {
                if (!this.loginForm.username || !this.loginForm.password) {
                    Gym.msg.err('请输入账号与密码');
                    return;
                }
                this.loginLoading = true;
                try {
                    const data = await Gym.api.post('/api/auth/login', this.loginForm);
                    // 先根据返回角色确定本标签页的作用域，再按作用域写 token
                    Gym.session.setScope(data.role);
                    this.scope = Gym.session.scope();
                    this.scopeLabel = { admin: '管理员端', member: '会员端', coach: '教练端' }[this.scope] || '';
                    Gym.session.set('token', data.token);
                    Gym.session.set('user', JSON.stringify(data));
                    this.token = data.token;
                    this.user = data;
                    this.page = this.defaultPage();
                    location.hash = '#/' + this.page;
                    Gym.msg.ok('欢迎回来，' + data.realName);
                } catch (e) {
                    /* 错误提示已由拦截器处理 */
                } finally {
                    this.loginLoading = false;
                }
            },
            logout() {
                Gym.api.post('/api/auth/logout').catch(() => {});
                Gym.session.clearLogin();   // 只退当前角色域，其他标签页不受影响
                this.token = '';
                this.user = {};
                this.page = 'login';
                this.loginForm = { username: '', password: '' };
                location.hash = '';
            },
            async changePwd() {
                if (!this.pwdForm.newPassword || this.pwdForm.newPassword.length < 6) {
                    Gym.msg.err('新密码不少于 6 位');
                    return;
                }
                await Gym.api.put('/api/auth/password', this.pwdForm);
                Gym.msg.ok('密码已修改');
                this.pwdVisible = false;
                this.pwdForm = { oldPassword: '', newPassword: '' };
            }
        }
    };
})(window.Gym);
