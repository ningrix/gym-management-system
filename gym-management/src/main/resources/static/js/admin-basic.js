/* 管理员：工作台、会员、会员卡、教练 */
(function (Gym) {
    Gym.drawChart = function (el, option) {
        if (!el) return null;
        const ins = echarts.getInstanceByDom(el) || echarts.init(el);
        ins.setOption(option, true);
        return ins;
    };
    const axisStyle = {
        axisLine: { lineStyle: { color: '#F0EDE7' } },
        axisLabel: { color: '#86868B', fontSize: 12 },
        splitLine: { lineStyle: { color: '#F7F4EF' } }
    };
    Gym.axisStyle = axisStyle;

    /* ---------------- 工作台 ---------------- */
    Gym.define('dashboard', {
        template: `
        <div>
            <div class="stat-row">
                <div class="stat-card">
                    <div class="label">会员总数</div>
                    <div class="value">{{ stat.memberTotal || 0 }}<small>人</small></div>
                    <div class="sub">在册会员累计</div>
                </div>
                <div class="stat-card c2">
                    <div class="label">有效会员卡</div>
                    <div class="value">{{ stat.activeCardTotal || 0 }}<small>张</small></div>
                    <div class="sub">状态正常的持卡记录</div>
                </div>
                <div class="stat-card c3">
                    <div class="label">今日课程</div>
                    <div class="value">{{ stat.todayClassTotal || 0 }}<small>节</small></div>
                    <div class="sub">今日待开课与进行中</div>
                </div>
                <div class="stat-card c4">
                    <div class="label">本月营收</div>
                    <div class="value">{{ money(stat.monthRevenue) }}</div>
                    <div class="sub">会员卡开卡与续费合计</div>
                </div>
            </div>
            <div class="two-col">
                <div class="card" style="flex:1.35">
                    <div class="card-title"><span class="t">近半年会员增长</span><span class="muted">单位：人</span></div>
                    <div ref="growth" class="chart"></div>
                </div>
                <div class="card">
                    <div class="card-title"><span class="t">课程预约热度</span><span class="muted">近全部记录</span></div>
                    <div ref="hot" class="chart"></div>
                </div>
            </div>
            <div class="card">
                <div class="card-title"><span class="t">最新公告</span></div>
                <div v-for="n in notices" :key="n.id" class="notice-item">
                    <span class="tag">{{ targetRole[n.targetRole] }}</span>
                    <span class="txt">{{ n.title }}</span>
                    <span class="time">{{ date(n.publishTime) }}</span>
                </div>
                <div v-if="!notices.length" class="empty-tip">暂无公告</div>
            </div>
        </div>`,
        data() {
            return { stat: {}, notices: [], targetRole: Gym.targetRole };
        },
        async mounted() {
            this.stat = await Gym.api.get('/api/reports/overview');
            this.notices = await Gym.api.get('/api/notices/latest', { limit: 4 });
            this.render();
        },
        methods: {
            money: Gym.money,
            date: Gym.date,
            render() {
                const growth = this.stat.memberGrowth || [];
                Gym.drawChart(this.$refs.growth, {
                    grid: { left: 40, right: 20, top: 24, bottom: 30 },
                    tooltip: { trigger: 'axis' },
                    legend: { data: ['新增会员', '累计会员'], right: 0, top: 0, itemWidth: 12, itemHeight: 8, textStyle: { fontSize: 12 } },
                    xAxis: { type: 'category', data: growth.map(i => i.month), boundaryGap: false, ...Gym.axisStyle },
                    yAxis: { type: 'value', ...Gym.axisStyle },
                    series: [
                        { name: '新增会员', type: 'line', smooth: true, symbolSize: 7, data: growth.map(i => i.newCount), itemStyle: { color: '#0071E3' }, areaStyle: { color: 'rgba(0,113,227,.10)' } },
                        { name: '累计会员', type: 'line', smooth: true, symbolSize: 7, data: growth.map(i => i.totalCount), itemStyle: { color: '#E9A23B' } }
                    ]
                });
                const hot = this.stat.courseHot || [];
                Gym.drawChart(this.$refs.hot, {
                    grid: { left: 76, right: 34, top: 20, bottom: 24 },
                    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
                    xAxis: { type: 'value', ...Gym.axisStyle },
                    yAxis: { type: 'category', data: hot.map(i => i.courseName).reverse(), ...Gym.axisStyle },
                    series: [{
                        type: 'bar', barWidth: 13, data: hot.map(i => i.reserveCount).reverse(),
                        itemStyle: { color: '#0071E3', borderRadius: [0, 6, 6, 0] },
                        label: { show: true, position: 'right', color: '#86868B', fontSize: 12 }
                    }]
                });
            }
        }
    });

    /* ---------------- 会员信息 ---------------- */
    Gym.define('member', {
        template: `
        <div>
            <div class="card">
                <div class="toolbar">
                    <el-input v-model="query.keyword" placeholder="姓名 / 手机号 / 会员编号" clearable
                              style="width:260px" @keyup.enter="load"/>
                    <el-select v-model="query.status" placeholder="会员状态" clearable style="width:130px">
                        <el-option label="在册" :value="1"/><el-option label="停用" :value="0"/>
                    </el-select>
                    <el-button type="primary" @click="load">查询</el-button>
                    <el-button @click="reset">重置</el-button>
                    <div class="right" v-if="isAdmin">
                        <el-button type="primary" @click="openCreate">新增会员</el-button>
                    </div>
                </div>
                <el-table :data="rows" border stripe height="520">
                    <el-table-column prop="memberNo" label="会员编号" width="118"/>
                    <el-table-column prop="name" label="姓名" width="90"/>
                    <el-table-column label="性别" width="64">
                        <template #default="{row}">{{ row.gender === 0 ? '女' : '男' }}</template>
                    </el-table-column>
                    <el-table-column prop="phone" label="手机号" width="132"/>
                    <el-table-column label="身高(cm)" width="92">
                        <template #default="{row}">{{ row.height || '-' }}</template>
                    </el-table-column>
                    <el-table-column label="入会日期" width="112">
                        <template #default="{row}">{{ date(row.joinDate) }}</template>
                    </el-table-column>
                    <el-table-column prop="source" label="来源" width="106"/>
                    <el-table-column prop="remark" label="备注" show-overflow-tooltip/>
                    <el-table-column label="状态" width="86">
                        <template #default="{row}">
                            <el-tag :type="row.status === 1 ? 'success' : 'info'" effect="light">
                                {{ row.status === 1 ? '在册' : '停用' }}
                            </el-tag>
                        </template>
                    </el-table-column>
                    <el-table-column label="操作" width="196" fixed="right">
                        <template #default="{row}">
                            <el-button link type="primary" @click="detail(row)">详情</el-button>
                            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
                            <el-button link :type="row.status === 1 ? 'danger' : 'success'" @click="toggle(row)">
                                {{ row.status === 1 ? '停用' : '启用' }}
                            </el-button>
                        </template>
                    </el-table-column>
                </el-table>
                <div class="pager">
                    <el-pagination background layout="total, prev, pager, next" :total="total"
                                   :page-size="query.pageSize" :current-page="query.pageNum"
                                   @current-change="p => { query.pageNum = p; load(); }"/>
                </div>
            </div>

            <el-dialog v-model="dialog" :title="form.id ? '编辑会员' : '新增会员'" width="620px">
                <el-form :model="form" label-width="92px">
                    <el-row :gutter="12">
                        <el-col :span="12"><el-form-item label="姓名"><el-input v-model="form.name"/></el-form-item></el-col>
                        <el-col :span="12">
                            <el-form-item label="性别">
                                <el-radio-group v-model="form.gender">
                                    <el-radio :label="1">男</el-radio><el-radio :label="0">女</el-radio>
                                </el-radio-group>
                            </el-form-item>
                        </el-col>
                        <el-col :span="12"><el-form-item label="手机号"><el-input v-model="form.phone"/></el-form-item></el-col>
                        <el-col :span="12">
                            <el-form-item label="出生日期">
                                <el-date-picker v-model="form.birthday" type="date" value-format="YYYY-MM-DD" style="width:100%"/>
                            </el-form-item>
                        </el-col>
                        <el-col :span="12"><el-form-item label="身高(cm)"><el-input-number v-model="form.height" :min="100" :max="250" :precision="1" style="width:100%"/></el-form-item></el-col>
                        <el-col :span="12">
                            <el-form-item label="来源">
                                <el-select v-model="form.source" style="width:100%">
                                    <el-option label="到店咨询" value="到店咨询"/>
                                    <el-option label="线上推广" value="线上推广"/>
                                    <el-option label="会员转介绍" value="会员转介绍"/>
                                </el-select>
                            </el-form-item>
                        </el-col>
                        <el-col :span="24"><el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2"/></el-form-item></el-col>
                    </el-row>
                </el-form>
                <template #footer>
                    <el-button @click="dialog = false">取消</el-button>
                    <el-button type="primary" @click="save">保存</el-button>
                </template>
            </el-dialog>

            <el-drawer v-model="drawer" title="会员详情" size="640px">
                <template v-if="current.member">
                    <el-descriptions :column="2" border size="small">
                        <el-descriptions-item label="会员编号">{{ current.member.memberNo }}</el-descriptions-item>
                        <el-descriptions-item label="姓名">{{ current.member.name }}</el-descriptions-item>
                        <el-descriptions-item label="手机号">{{ current.member.phone }}</el-descriptions-item>
                        <el-descriptions-item label="入会日期">{{ date(current.member.joinDate) }}</el-descriptions-item>
                        <el-descriptions-item label="身高">{{ current.member.height }} cm</el-descriptions-item>
                        <el-descriptions-item label="来源">{{ current.member.source }}</el-descriptions-item>
                    </el-descriptions>
                    <div class="card-title" style="margin-top:20px"><span class="t">持有会员卡</span></div>
                    <el-table :data="current.cards" size="small" border>
                        <el-table-column prop="cardNo" label="卡号" width="150"/>
                        <el-table-column prop="typeName" label="卡种" width="90"/>
                        <el-table-column label="有效期" width="176">
                            <template #default="{row}">{{ date(row.startDate) }} 至 {{ row.endDate ? date(row.endDate) : '不限' }}</template>
                        </el-table-column>
                        <el-table-column label="状态" width="80">
                            <template #default="{row}"><el-tag size="small" :type="statusTag[row.status]">{{ statusText[row.status] }}</el-tag></template>
                        </el-table-column>
                    </el-table>
                    <div class="card-title" style="margin-top:20px"><span class="t">体测记录</span></div>
                    <el-table :data="current.bodyRecords" size="small" border>
                        <el-table-column label="日期" width="110"><template #default="{row}">{{ date(row.recordDate) }}</template></el-table-column>
                        <el-table-column prop="weight" label="体重(kg)" width="92"/>
                        <el-table-column prop="bmi" label="BMI" width="76"/>
                        <el-table-column prop="bodyFat" label="体脂率(%)" width="96"/>
                        <el-table-column prop="remark" label="教练建议" show-overflow-tooltip/>
                    </el-table>
                </template>
            </el-drawer>
        </div>`,
        data() {
            return {
                query: { keyword: '', status: null, pageNum: 1, pageSize: 10 },
                rows: [], total: 0, dialog: false, drawer: false,
                form: {}, current: {},
                statusText: Gym.statusText, statusTag: Gym.statusTag
            };
        },
        computed: {
            isAdmin() { return this.$root.user.role === 'ADMIN'; }
        },
        mounted() { this.load(); },
        methods: {
            date: Gym.date,
            async load() {
                const page = await Gym.api.get('/api/members', this.query);
                this.rows = page.records || [];
                this.total = page.total || 0;
            },
            reset() {
                this.query = { keyword: '', status: null, pageNum: 1, pageSize: 10 };
                this.load();
            },
            openCreate() {
                this.form = { gender: 1, height: 170, source: '到店咨询' };
                this.dialog = true;
            },
            openEdit(row) {
                this.form = JSON.parse(JSON.stringify(row));
                this.dialog = true;
            },
            async save() {
                if (this.form.id) {
                    await Gym.api.put('/api/members/' + this.form.id, this.form);
                } else {
                    await Gym.api.post('/api/members', this.form);
                }
                Gym.msg.ok('保存成功');
                this.dialog = false;
                this.load();
            },
            async toggle(row) {
                await Gym.api.put('/api/members/' + row.id + '/status', { status: row.status === 1 ? 0 : 1 });
                Gym.msg.ok('状态已更新');
                this.load();
            },
            async detail(row) {
                this.current = await Gym.api.get('/api/members/' + row.id);
                this.drawer = true;
            }
        }
    });

    /* ---------------- 会员卡 ---------------- */
    Gym.define('member-card', {
        template: `
        <div>
            <div class="card">
                <div class="toolbar">
                    <el-input v-model="query.keyword" placeholder="卡号 / 会员姓名" clearable style="width:230px" @keyup.enter="load"/>
                    <el-select v-model="query.status" placeholder="卡状态" clearable style="width:130px">
                        <el-option label="正常" value="ACTIVE"/><el-option label="已冻结" value="FROZEN"/>
                        <el-option label="已过期" value="EXPIRED"/>
                    </el-select>
                    <el-button type="primary" @click="load">查询</el-button>
                    <el-button @click="reset">重置</el-button>
                    <div class="right" v-if="isAdmin"><el-button type="primary" @click="openCreate">办理开卡</el-button></div>
                </div>
                <el-table :data="rows" border stripe height="470">
                    <el-table-column prop="cardNo" label="卡号" width="150"/>
                    <el-table-column label="会员" width="150">
                        <template #default="{row}">{{ row.memberName }} <span class="muted">{{ row.memberNo }}</span></template>
                    </el-table-column>
                    <el-table-column prop="typeName" label="卡种" width="106"/>
                    <el-table-column label="有效期" width="188">
                        <template #default="{row}">
                            {{ date(row.startDate) }} 至 {{ row.endDate ? date(row.endDate) : '不限' }}
                        </template>
                    </el-table-column>
                    <el-table-column label="剩余次数" width="140">
                        <template #default="{row}">
                            <span v-if="row.cardKind === 'TIMES'">{{ row.remainTimes }} / {{ row.totalTimes }}</span>
                            <span v-else class="muted">不限次</span>
                        </template>
                    </el-table-column>
                    <el-table-column label="剩余天数" width="96">
                        <template #default="{row}">
                            <span v-if="row.remainDays !== null && row.remainDays !== undefined"
                                  :style="{color: row.remainDays < 0 ? '#E5484D' : (row.remainDays <= 7 ? '#E9A23B' : '#424245')}">
                                {{ row.remainDays }} 天
                            </span>
                            <span v-else class="muted">-</span>
                        </template>
                    </el-table-column>
                    <el-table-column label="状态" width="88">
                        <template #default="{row}"><el-tag size="small" :type="statusTag[row.status]" effect="light">{{ statusText[row.status] }}</el-tag></template>
                    </el-table-column>
                    <el-table-column label="操作" width="180" fixed="right">
                        <template #default="{row}">
                            <el-button link type="primary" :disabled="!isAdmin || row.status === 'REFUNDED'" @click="openRenew(row)">续费</el-button>
                            <el-button link type="warning" v-if="row.status === 'ACTIVE'" :disabled="!isAdmin" @click="freeze(row)">冻结</el-button>
                            <el-button link type="success" v-if="row.status === 'FROZEN'" :disabled="!isAdmin" @click="unfreeze(row)">解冻</el-button>
                        </template>
                    </el-table-column>
                </el-table>
                <div class="pager">
                    <el-pagination background layout="total, prev, pager, next" :total="total"
                                   :page-size="query.pageSize" :current-page="query.pageNum"
                                   @current-change="p => { query.pageNum = p; load(); }"/>
                </div>
            </div>

            <div class="card">
                <div class="card-title"><span class="t">到期提醒</span><span class="muted">7 天内到期与已过期的有效卡</span></div>
                <el-table :data="expiring" size="small" border>
                    <el-table-column prop="cardNo" label="卡号" width="150"/>
                    <el-table-column prop="memberName" label="会员" width="90"/>
                    <el-table-column prop="typeName" label="卡种" width="100"/>
                    <el-table-column label="到期日" width="120"><template #default="{row}">{{ date(row.endDate) }}</template></el-table-column>
                    <el-table-column label="剩余天数" width="100">
                        <template #default="{row}"><span style="color:#E9A23B">{{ row.remainDays }} 天</span></template>
                    </el-table-column>
                    <el-table-column label="操作">
                        <template #default="{row}"><el-button link type="primary" @click="openRenew(row)">立即续费</el-button></template>
                    </el-table-column>
                </el-table>
            </div>

            <el-dialog v-model="dialog" :title="form.id ? '会员卡续费' : '办理开卡'" width="540px">
                <el-form :model="form" label-width="92px">
                    <el-form-item v-if="!form.id" label="会员">
                        <el-select v-model="form.memberId" filterable placeholder="输入姓名检索" style="width:100%">
                            <el-option v-for="m in members" :key="m.id" :label="m.name + '（' + m.memberNo + '）'" :value="m.id"/>
                        </el-select>
                    </el-form-item>
                    <el-form-item v-else label="卡号"><el-input :model-value="form.cardNo" disabled/></el-form-item>
                    <el-form-item label="卡种">
                        <el-select v-model="form.cardTypeId" style="width:100%" @change="onType">
                            <el-option v-for="t in types" :key="t.id" :label="t.typeName + ' / ' + t.price + ' 元'" :value="t.id"/>
                        </el-select>
                    </el-form-item>
                    <el-form-item label="支付方式">
                        <el-radio-group v-model="form.payType">
                            <el-radio label="WECHAT">微信</el-radio><el-radio label="ALIPAY">支付宝</el-radio>
                            <el-radio label="CASH">现金</el-radio><el-radio label="CARD">银行卡</el-radio>
                        </el-radio-group>
                    </el-form-item>
                    <el-form-item label="实收金额">
                        <el-input-number v-model="form.amount" :min="0" :precision="2" :step="100" style="width:180px"/>
                        <span class="muted" style="margin-left:8px">元</span>
                    </el-form-item>
                </el-form>
                <template #footer>
                    <el-button @click="dialog = false">取消</el-button>
                    <el-button type="primary" @click="save">{{ form.id ? '确认续费' : '确认开卡' }}</el-button>
                </template>
            </el-dialog>
        </div>`,
        data() {
            return {
                query: { keyword: '', status: null, pageNum: 1, pageSize: 10 },
                rows: [], total: 0, expiring: [], dialog: false, form: {},
                members: [], types: [],
                statusText: Gym.statusText, statusTag: Gym.statusTag
            };
        },
        computed: {
            isAdmin() { return this.$root.user.role === 'ADMIN'; }
        },
        async mounted() {
            await this.load();
            this.loadExpiring();
        },
        methods: {
            date: Gym.date,
            async load() {
                const page = await Gym.api.get('/api/member-cards', this.query);
                this.rows = page.records || [];
                this.total = page.total || 0;
            },
            async loadExpiring() {
                this.expiring = await Gym.api.get('/api/member-cards/expiring', { days: 7 });
            },
            reset() {
                this.query = { keyword: '', status: null, pageNum: 1, pageSize: 10 };
                this.load();
            },
            async openCreate() {
                this.form = { payType: 'WECHAT', amount: undefined };
                if (!this.members.length) {
                    const page = await Gym.api.get('/api/members', { pageNum: 1, pageSize: 100 });
                    this.members = page.records || [];
                }
                if (!this.types.length) this.types = await Gym.api.get('/api/card-types', { onlyOnSale: true });
                this.dialog = true;
            },
            async openRenew(row) {
                if (!this.types.length) this.types = await Gym.api.get('/api/card-types', { onlyOnSale: true });
                this.form = { id: row.id, cardNo: row.cardNo, cardTypeId: row.cardTypeId, payType: 'WECHAT', amount: undefined };
                const t = this.types.find(i => i.id === row.cardTypeId);
                if (t) this.form.amount = t.price;
                this.dialog = true;
            },
            onType(id) {
                const t = this.types.find(i => i.id === id);
                if (t) this.form.amount = t.price;
            },
            async save() {
                if (this.form.id) {
                    await Gym.api.post('/api/member-cards/' + this.form.id + '/renew', this.form);
                    Gym.msg.ok('续费成功');
                } else {
                    await Gym.api.post('/api/member-cards/open', this.form);
                    Gym.msg.ok('开卡成功');
                }
                this.dialog = false;
                this.load();
                this.loadExpiring();
            },
            async freeze(row) {
                await Gym.api.put('/api/member-cards/' + row.id + '/status', { status: 'FROZEN', freezeReason: '前台申请冻结' });
                Gym.msg.ok('已冻结');
                this.load();
            },
            async unfreeze(row) {
                await Gym.api.put('/api/member-cards/' + row.id + '/status', { status: 'ACTIVE' });
                Gym.msg.ok('已解冻');
                this.load();
            }
        }
    });

    /* ---------------- 教练管理 ---------------- */
    Gym.define('coach', {
        template: `
        <div>
            <div class="card">
                <div class="toolbar">
                    <el-input v-model="query.keyword" placeholder="姓名 / 手机号" clearable style="width:210px" @keyup.enter="load"/>
                    <el-select v-model="query.specialty" placeholder="专长" clearable style="width:150px">
                        <el-option v-for="s in ['增肌塑形','瑜伽普拉提','搏击体能']" :key="s" :label="s" :value="s"/>
                    </el-select>
                    <el-button type="primary" @click="load">查询</el-button>
                    <el-button @click="reset">重置</el-button>
                    <div class="right"><el-button type="primary" @click="openCreate">新增教练</el-button></div>
                </div>
                <el-table :data="rows" border stripe height="520">
                    <el-table-column prop="coachNo" label="编号" width="90"/>
                    <el-table-column prop="name" label="姓名" width="90"/>
                    <el-table-column label="性别" width="64"><template #default="{row}">{{ row.gender === 0 ? '女' : '男' }}</template></el-table-column>
                    <el-table-column prop="phone" label="手机号" width="132"/>
                    <el-table-column prop="specialty" label="专长" width="120"/>
                    <el-table-column prop="coachLevel" label="等级" width="80"/>
                    <el-table-column label="课时费" width="104"><template #default="{row}"><span class="money">{{ money(row.hourlyWage) }}</span></template></el-table-column>
                    <el-table-column label="入职日期" width="112"><template #default="{row}">{{ date(row.hireDate) }}</template></el-table-column>
                    <el-table-column prop="intro" label="简介" show-overflow-tooltip/>
                    <el-table-column label="状态" width="84">
                        <template #default="{row}"><el-tag size="small" :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '在职' : '离职' }}</el-tag></template>
                    </el-table-column>
                    <el-table-column label="操作" width="130" fixed="right">
                        <template #default="{row}">
                            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
                            <el-button link :type="row.status === 1 ? 'danger' : 'success'" @click="toggle(row)">{{ row.status === 1 ? '停用' : '启用' }}</el-button>
                        </template>
                    </el-table-column>
                </el-table>
                <div class="pager">
                    <el-pagination background layout="total, prev, pager, next" :total="total"
                                   :page-size="query.pageSize" :current-page="query.pageNum"
                                   @current-change="p => { query.pageNum = p; load(); }"/>
                </div>
            </div>

            <el-dialog v-model="dialog" :title="form.id ? '编辑教练' : '新增教练'" width="600px">
                <el-form :model="form" label-width="92px">
                    <el-row :gutter="12">
                        <el-col :span="12"><el-form-item label="姓名"><el-input v-model="form.name"/></el-form-item></el-col>
                        <el-col :span="12"><el-form-item label="手机号"><el-input v-model="form.phone"/></el-form-item></el-col>
                        <el-col :span="12">
                            <el-form-item label="专长">
                                <el-select v-model="form.specialty" style="width:100%">
                                    <el-option v-for="s in ['增肌塑形','瑜伽普拉提','搏击体能']" :key="s" :label="s" :value="s"/>
                                </el-select>
                            </el-form-item>
                        </el-col>
                        <el-col :span="12">
                            <el-form-item label="等级">
                                <el-select v-model="form.coachLevel" style="width:100%">
                                    <el-option label="初级" value="初级"/><el-option label="中级" value="中级"/><el-option label="高级" value="高级"/>
                                </el-select>
                            </el-form-item>
                        </el-col>
                        <el-col :span="12"><el-form-item label="课时费"><el-input-number v-model="form.hourlyWage" :min="0" :step="10" style="width:100%"/></el-form-item></el-col>
                        <el-col :span="12">
                            <el-form-item label="入职日期">
                                <el-date-picker v-model="form.hireDate" type="date" value-format="YYYY-MM-DD" style="width:100%"/>
                            </el-form-item>
                        </el-col>
                        <el-col :span="24"><el-form-item label="简介"><el-input v-model="form.intro" type="textarea" :rows="2"/></el-form-item></el-col>
                    </el-row>
                </el-form>
                <template #footer>
                    <el-button @click="dialog = false">取消</el-button>
                    <el-button type="primary" @click="save">保存</el-button>
                </template>
            </el-dialog>
        </div>`,
        data() {
            return { query: { keyword: '', specialty: '', pageNum: 1, pageSize: 10 }, rows: [], total: 0, dialog: false, form: {} };
        },
        mounted() { this.load(); },
        methods: {
            date: Gym.date, money: Gym.money,
            async load() {
                const page = await Gym.api.get('/api/coaches', this.query);
                this.rows = page.records || [];
                this.total = page.total || 0;
            },
            reset() { this.query = { keyword: '', specialty: '', pageNum: 1, pageSize: 10 }; this.load(); },
            openCreate() { this.form = { gender: 1, coachLevel: '中级', hourlyWage: 150 }; this.dialog = true; },
            openEdit(row) { this.form = JSON.parse(JSON.stringify(row)); this.dialog = true; },
            async save() {
                if (this.form.id) await Gym.api.put('/api/coaches/' + this.form.id, this.form);
                else await Gym.api.post('/api/coaches', this.form);
                Gym.msg.ok('保存成功');
                this.dialog = false;
                this.load();
            },
            async toggle(row) {
                await Gym.api.put('/api/coaches/' + row.id + '/status', { status: row.status === 1 ? 0 : 1 });
                Gym.msg.ok('状态已更新');
                this.load();
            }
        }
    });
})(window.Gym);
