/* 会员端与教练端页面 */
(function (Gym) {
    /* ---------------- 会员个人中心 ---------------- */
    Gym.define('my-profile', {
        template: `
        <div>
            <div class="stat-row">
                <div class="stat-card">
                    <div class="label">我的会员卡</div>
                    <div class="value">{{ cards.length }}<small>张</small></div>
                    <div class="sub">含有效期内的期限卡与次卡</div>
                </div>
                <div class="stat-card c2">
                    <div class="label">累计缴费</div>
                    <div class="value">{{ totalAmount }}<small>元</small></div>
                    <div class="sub">共 {{ recharges.length }} 笔记录</div>
                </div>
                <div class="stat-card c3">
                    <div class="label">我的预约</div>
                    <div class="value">{{ reservations.length }}<small>条</small></div>
                    <div class="sub">近期预约记录（最多显示 5 条）</div>
                </div>
                <div class="stat-card c4">
                    <div class="label">最近体重</div>
                    <div class="value">{{ latestBody ? latestBody.weight : '-' }}<small>kg</small></div>
                    <div class="sub">BMI {{ latestBody ? latestBody.bmi : '-' }}</div>
                </div>
            </div>

            <div class="two-col">
                <div class="card">
                    <div class="card-title"><span class="t">我的会员卡</span></div>
                    <el-table :data="cards" border size="small">
                        <el-table-column prop="cardNo" label="卡号" width="148"/>
                        <el-table-column prop="typeName" label="卡种" width="100"/>
                        <el-table-column label="有效期" width="180">
                            <template #default="{row}">{{ date(row.startDate) }} 至 {{ row.endDate ? date(row.endDate) : '不限' }}</template>
                        </el-table-column>
                        <el-table-column label="剩余" width="110">
                            <template #default="{row}">
                                <span v-if="row.cardKind === 'TIMES'">{{ row.remainTimes }} / {{ row.totalTimes }} 次</span>
                                <span v-else>{{ row.remainDays }} 天</span>
                            </template>
                        </el-table-column>
                        <el-table-column label="状态" width="86">
                            <template #default="{row}"><el-tag size="small" :type="statusTag[row.status]">{{ statusText[row.status] }}</el-tag></template>
                        </el-table-column>
                    </el-table>
                    <div class="card-title" style="margin-top:18px"><span class="t">缴费记录</span></div>
                    <el-table :data="recharges" border size="small" height="220">
                        <el-table-column prop="rechargeNo" label="流水号" width="170"/>
                        <el-table-column label="金额" width="110"><template #default="{row}"><span class="money">{{ money(row.amount) }}</span></template></el-table-column>
                        <el-table-column label="方式" width="90"><template #default="{row}">{{ payType[row.payType] }}</template></el-table-column>
                        <el-table-column label="时间"><template #default="{row}">{{ (row.createTime||'').replace('T',' ').slice(0,16) }}</template></el-table-column>
                    </el-table>
                </div>
                <div class="card">
                    <div class="card-title"><span class="t">我的提醒</span><span class="muted">到期与流失提醒</span></div>
                    <div v-for="al in alerts" :key="al.id" class="notice-item">
                        <span class="tag" :style="{background: al.alertType==='DUE' ? '#fdf6ec' : '#fef0f0', color: al.alertType==='DUE' ? '#E9A23B' : '#E5484D'}">
                            {{ al.alertType === 'DUE' ? '卡到期' : '流失提醒' }}
                        </span>
                        <span class="txt">{{ al.content }}</span>
                    </div>
                    <div v-if="!alerts.length" class="empty-tip">暂无提醒，继续保持运动习惯</div>
                    <div class="card-title" style="margin-top:18px"><span class="t">体测趋势</span><span class="muted">体重 / BMI</span></div>
                    <div ref="chart" class="chart-sm" style="height:300px"></div>
                    <div class="card-title" style="margin-top:8px"><span class="t">我的预约</span></div>
                    <el-table :data="reservations" border size="small" height="190">
                        <el-table-column prop="courseName" label="课程" width="100"/>
                        <el-table-column label="上课时间" width="150">
                            <template #default="{row}">{{ date(row.classDate) }} {{ time(row.startTime) }}</template>
                        </el-table-column>
                        <el-table-column prop="coachName" label="教练" width="86"/>
                        <el-table-column label="状态" width="90">
                            <template #default="{row}"><el-tag size="small" :type="resTag[row.status]">{{ statusText[row.status] }}</el-tag></template>
                        </el-table-column>
                        <el-table-column label="操作">
                            <template #default="{row}">
                                <el-button v-if="row.status === 'BOOKED' && row.classDate >= today" link type="danger" @click="cancel(row)">取消</el-button>
                                <span v-else class="muted">-</span>
                            </template>
                        </el-table-column>
                    </el-table>
                </div>
            </div>
        </div>`,
        data() {
            return {
                cards: [], recharges: [], reservations: [], bodyRecords: [], latestBody: null,
                statusText: Gym.statusText, statusTag: Gym.statusTag, payType: Gym.payType, today: Gym.date(new Date().toISOString()), alerts: []
            };
        },
        computed: {
            totalAmount() {
                return this.recharges.reduce((s, i) => s + Number(i.amount || 0), 0).toFixed(2);
            }
        },
        async mounted() {
            const [cardData, home] = await Promise.all([
                Gym.api.get('/api/member-cards/my'),
                Gym.api.get('/api/reports/member-home')
            ]);
            this.cards = cardData.cards || [];
            this.recharges = cardData.recharges || [];
            this.reservations = home.reservations || [];
            this.alerts = (await Gym.api.get('/api/member-alerts')).records || [];
            this.bodyRecords = home.bodyRecords || [];
            this.latestBody = home.latestBody;
            this.render();
        },
        methods: {
            date: Gym.date, time: Gym.time, money: Gym.money,
            render() {
                const list = [...this.bodyRecords].sort((a, b) => a.recordDate > b.recordDate ? 1 : -1);
                Gym.drawChart(this.$refs.chart, {
                    grid: { left: 44, right: 44, top: 30, bottom: 26 },
                    tooltip: { trigger: 'axis' },
                    legend: { data: ['体重(kg)', 'BMI'], top: 0, right: 0, itemWidth: 12, itemHeight: 8 },
                    xAxis: { type: 'category', data: list.map(i => Gym.date(i.recordDate)), ...Gym.axisStyle },
                    yAxis: [{ type: 'value', scale: true, ...Gym.axisStyle }, { type: 'value', scale: true, ...Gym.axisStyle, splitLine: { show: false } }],
                    series: [
                        { name: '体重(kg)', type: 'line', smooth: true, data: list.map(i => i.weight), itemStyle: { color: '#0071E3' }, areaStyle: { color: 'rgba(0,113,227,.08)' } },
                        { name: 'BMI', type: 'line', smooth: true, yAxisIndex: 1, data: list.map(i => i.bmi), itemStyle: { color: '#E9A23B' } }
                    ]
                });
            },
            async cancel(row) {
                await ElementPlus.ElMessageBox.confirm('确认取消该次预约？', '提示', { type: 'warning' });
                await Gym.api.put('/api/reservations/' + row.id + '/cancel');
                Gym.msg.ok('已取消预约');
                const home = await Gym.api.get('/api/reports/member-home');
                this.reservations = home.reservations || [];
            this.alerts = (await Gym.api.get('/api/member-alerts')).records || [];
            }
        }
    });

    /* ---------------- 会员课程预约 ---------------- */
    Gym.define('my-course', {
        template: `
        <div>
            <div class="card">
                <div class="toolbar">
                    <span class="muted">可预约课程</span>
                    <el-date-picker v-model="range" type="daterange" value-format="YYYY-MM-DD" style="width:250px"/>
                    <el-button type="primary" @click="load">查询</el-button>
                    <span class="muted" style="margin-left:auto">开课前 2 小时停止预约，每人同一节课限约一次</span>
                </div>
                <div class="two-col" style="flex-wrap:wrap;gap:12px">
                    <div class="course-card" v-for="s in schedules" :key="s.id" style="flex:0 0 calc(33.33% - 8px)">
                        <div class="hd">
                            <span class="nm">{{ s.courseName }}</span>
                            <el-tag size="small" :type="diffTag[s.difficulty]" effect="light">
                                {{ ['','入门','进阶','高强度'][s.difficulty] }}
                            </el-tag>
                        </div>
                        <div class="meta">
                            教练：{{ s.coachName }}　{{ s.room }}<br>
                            {{ date(s.classDate) }} {{ time(s.startTime) }}-{{ time(s.endTime) }}（{{ s.duration }} 分钟）<br>
                            余位：<span class="seat" :class="{full: s.remain === 0}">{{ s.remain }}</span> / {{ s.capacity }}
                        </div>
                        <el-button v-if="!bookedIds.includes(s.id) && !queuedIds.includes(s.id)" style="margin-top:10px;width:100%" type="primary" plain
                                   :disabled="s.remain === 0" @click="s.remain === 0 ? joinWaitlist(s) : reserve(s)">
                            {{ s.remain === 0 ? '名额已满，排候补' : '立即预约' }}
                        </el-button>
                        <el-button v-else-if="queuedIds.includes(s.id)" style="margin-top:10px;width:100%" plain disabled>候补中</el-button>
                        <el-button v-else style="margin-top:10px;width:100%" plain disabled>已预约</el-button>
                    </div>
                </div>
                <div v-if="!schedules.length" class="empty-tip">该时间段暂无可约课程</div>
            </div>

            <div class="card">
                <div class="card-title"><span class="t">我的候补记录</span><span class="muted">名额释放后按排队顺序自动递补</span></div>
                <el-table :data="waitlist" border size="small">
                    <el-table-column label="状态" width="96">
                        <template #default="{row}"><el-tag size="small" :type="{WAITING:'warning',PROMOTED:'success',CANCELLED:'info'}[row.status]">{{ {WAITING:'排队中',PROMOTED:'已递补',CANCELLED:'已退出'}[row.status] }}</el-tag></template>
                    </el-table-column>
                    <el-table-column prop="scheduleId" label="课程编号" width="110"/>
                    <el-table-column label="排队时间" width="160"><template #default="{row}">{{ (row.enqueueTime||'').replace('T',' ').slice(0,16) }}</template></el-table-column>
                    <el-table-column label="操作">
                        <template #default="{row}">
                            <el-button v-if="row.status === 'WAITING'" link type="danger" @click="leaveWaitlist(row)">退出候补</el-button>
                            <span v-else class="muted">-</span>
                        </template>
                    </el-table-column>
                </el-table>
            </div>

            <div class="card">
                <div class="card-title"><span class="t">我的预约记录</span></div>
                <el-table :data="myList" border stripe>
                    <el-table-column prop="reserveNo" label="预约号" width="150"/>
                    <el-table-column prop="courseName" label="课程" width="110"/>
                    <el-table-column label="上课时间" width="180">
                        <template #default="{row}">{{ date(row.classDate) }} {{ time(row.startTime) }}-{{ time(row.endTime) }}</template>
                    </el-table-column>
                    <el-table-column prop="coachName" label="教练" width="90"/>
                    <el-table-column prop="room" label="教室" width="90"/>
                    <el-table-column label="位置" width="70"><template #default="{row}">{{ row.positionNo || '-' }}</template></el-table-column>
                    <el-table-column label="预约时间" width="150">
                        <template #default="{row}">{{ (row.reserveTime||'').replace('T',' ').slice(0,16) }}</template>
                    </el-table-column>
                    <el-table-column label="状态" width="96">
                        <template #default="{row}">
                            <el-tag size="small" :type="resTag[row.status]">
                                {{ statusText[row.status] }}
                            </el-tag>
                        </template>
                    </el-table-column>
                    <el-table-column label="操作" width="90">
                        <template #default="{row}">
                            <el-button v-if="row.status === 'BOOKED' && row.classDate >= today" link type="danger" @click="cancel(row)">取消</el-button>
                            <span v-else class="muted">-</span>
                        </template>
                    </el-table-column>
                </el-table>
            </div>
        </div>`,
        data() {
            const now = new Date();
            const fmt = d => d.toISOString().slice(0, 10);
            return {
                range: [fmt(now), fmt(new Date(now.getTime() + 7 * 86400000))],
                schedules: [], myList: [], bookedIds: [], queuedIds: [], waitlist: [],
                statusText: Gym.statusText, today: Gym.date(new Date().toISOString())
            };
        },
        async mounted() { await this.load(); },
        methods: {
            date: Gym.date, time: Gym.time,
            async load() {
                const params = { startDate: this.range[0], endDate: this.range[1], status: 'WAITING' };
                this.schedules = (await Gym.api.get('/api/schedules', params)).filter(s => s.remain > 0 || s.bookedCount > 0);
                this.myList = await Gym.api.get('/api/reservations/my');
                this.bookedIds = this.myList.filter(i => i.status !== 'CANCELLED' && i.status !== 'NO_SHOW').map(i => i.scheduleId);
                this.waitlist = await Gym.api.get('/api/reservations/waitlist/my');
                this.queuedIds = this.waitlist.filter(i => i.status === 'WAITING').map(i => i.scheduleId);
            },
            async reserve(s) {
                await Gym.api.post('/api/reservations', { scheduleId: s.id });
                Gym.msg.ok('预约成功，请按时到场');
                await this.load();
            },
            async cancel(row) {
                await ElementPlus.ElMessageBox.confirm('确认取消该次预约？', '提示', { type: 'warning' });
                await Gym.api.put('/api/reservations/' + row.id + '/cancel');
                Gym.msg.ok('已取消预约');
                await this.load();
            },
            async joinWaitlist(s) {
                await Gym.api.post('/api/reservations/waitlist/' + s.id);
                Gym.msg.ok('已加入候补队列，名额释放后自动为你预约');
                await this.load();
            },
            async leaveWaitlist(row) {
                await Gym.api.delete('/api/reservations/waitlist/' + row.scheduleId);
                Gym.msg.ok('已退出候补');
                await this.load();
            }
        }
    });

    /* ---------------- 教练工作台 ---------------- */

    /* ---------------- 教练风采（会员端） ---------------- */
    Gym.define('coach-showcase', {
        template: `
        <div>
            <div class="card">
                <div class="card-title"><span class="t">教练团队</span><span class="muted">资质与教学方向</span></div>
                <div class="coach-grid">
                    <div v-for="c in coaches" :key="c.id" class="coach-card">
                        <div class="coach-avatar">{{ (c.name || '').slice(0, 1) }}</div>
                        <div class="coach-body">
                            <div class="coach-name">
                                {{ c.name }}
                                <span class="coach-level">{{ c.coachLevel }}</span>
                            </div>
                            <div class="coach-meta">
                                {{ c.specialty }}<span v-if="c.yearsOfService"> · 教龄 {{ c.yearsOfService }} 年</span>
                                <span v-if="c.hireDate"> · {{ c.hireDate }} 入职</span>
                            </div>
                            <div class="coach-intro">{{ c.intro }}</div>
                        </div>
                    </div>
                    <div v-if="!coaches.length" class="empty-tip">暂无教练信息</div>
                </div>
            </div>

            <div class="card" v-if="videos.length">
                <div class="card-title">
                    <span class="t">教学实录</span>
                    <span class="muted">{{ videos[idx].coach }} · {{ videos[idx].title }}</span>
                </div>
                <div class="video-stage">
                    <video ref="player" :src="videos[idx].src" :poster="videos[idx].poster"
                           controls playsinline preload="metadata" @play="playing = true" @pause="playing = false">
                        您的浏览器不支持视频播放，请升级浏览器后重试。
                    </video>
                </div>
                <div class="video-bar">
                    <button v-for="(v, i) in videos" :key="i" class="video-thumb"
                            :class="{ active: i === idx }" @click="switchTo(i)"
                            :aria-label="'播放：' + v.title">
                        <img :src="v.poster" :alt="v.title">
                        <span>{{ v.title }}</span>
                    </button>
                    <button class="video-mute" type="button" @click="toggleMute"
                            :aria-label="muted ? '开启声音' : '静音'">{{ muted ? '开启声音' : '静音' }}</button>
                </div>
            </div>
        </div>`,
        data() {
            return {
                coaches: [],
                idx: 0,
                playing: false,
                muted: false,
                videos: [
                    {
                        title: '力量训练示范', coach: '阮腾',
                        src: 'media/coach/coach-1.mp4', poster: 'media/coach/coach-1-poster.jpg'
                    },
                    {
                        title: '团课带练实录', coach: '阮腾',
                        src: 'media/coach/coach-2.mp4', poster: 'media/coach/coach-2-poster.jpg'
                    }
                ]
            };
        },
        async mounted() {
            this.coaches = await Gym.api.get('/api/coaches/showcase');
        },
        methods: {
            switchTo(i) {
                if (i === this.idx) return;
                const wasPlaying = this.playing;
                this.idx = i;
                this.$nextTick(() => {
                    const v = this.$refs.player;
                    if (!v) return;
                    v.load();
                    if (wasPlaying) v.play().catch(() => { });
                });
            },
            toggleMute() {
                const v = this.$refs.player;
                if (!v) return;
                v.muted = !v.muted;
                this.muted = v.muted;
            }
        }
    });

    Gym.define('coach-home', {
        template: `
        <div>
            <div class="stat-row">
                <div class="stat-card">
                    <div class="label">今日课程</div>
                    <div class="value">{{ home.todaySchedules ? home.todaySchedules.length : 0 }}<small>节</small></div>
                    <div class="sub">含待开课与进行中</div>
                </div>
                <div class="stat-card c2">
                    <div class="label">待签到人数</div>
                    <div class="value">{{ home.pendingCheckIn || 0 }}<small>人</small></div>
                    <div class="sub">今日课程已预约未签到</div>
                </div>
                <div class="stat-card c3">
                    <div class="label">本月课时</div>
                    <div class="value">{{ home.monthLesson || 0 }}<small>节</small></div>
                    <div class="sub">已结束课程统计</div>
                </div>
                <div class="stat-card c4">
                    <div class="label">本月服务学员</div>
                    <div class="value">{{ home.monthMember || 0 }}<small>人</small></div>
                    <div class="sub">去重后的上课人数</div>
                </div>
            </div>

            <div class="two-col">
                <div class="card" style="flex:1.4">
                    <div class="card-title"><span class="t">今日课表</span><span class="muted">{{ today }}</span></div>
                    <el-table :data="home.todaySchedules || []" border stripe>
                        <el-table-column label="时间" width="112">
                            <template #default="{row}">{{ time(row.startTime) }}-{{ time(row.endTime) }}</template>
                        </el-table-column>
                        <el-table-column prop="courseName" label="课程" width="120"/>
                        <el-table-column prop="room" label="教室" width="100"/>
                        <el-table-column label="预约" width="120">
                            <template #default="{row}"><span class="seat">{{ row.bookedCount }} / {{ row.capacity }}</span> 人</template>
                        </el-table-column>
                        <el-table-column label="状态" width="96">
                            <template #default="{row}">
                                <el-tag size="small" :type="scheduleTag[row.status]">{{ statusText[row.status] }}</el-tag>
                            </template>
                        </el-table-column>
                    </el-table>
                </div>
                <div class="card" style="flex:1.2">
                    <div class="card-title"><span class="t">本月课时结算单</span><span class="muted">课时费 {{ money(settle.unitFee) }} / 节</span></div>
                    <el-table :data="settle.rows" border size="small" max-height="300">
                        <el-table-column label="上课日期" width="110"><template #default="{row}">{{ row.classDate }}</template></el-table-column>
                        <el-table-column label="时间" width="112"><template #default="{row}">{{ row.start.slice(0,5) }}-{{ row.end.slice(0,5) }}</template></el-table-column>
                        <el-table-column label="课时费" width="100"><template #default="{row}"><span class="money">{{ money(row.fee) }}</span></template></el-table-column>
                    </el-table>
                    <div style="margin-top:10px;text-align:right;color:#303133;font-weight:600">
                        本月合计：{{ settle.lessonCount }} 节 / {{ money(settle.totalFee) }}
                    </div>
                </div>
            </div>
            <div class="card">
                <div class="card-title"><span class="t">场馆公告</span></div>
                <div v-for="n in notices" :key="n.id" class="notice-item">
                    <span class="txt">{{ n.title }}</span>
                    <span class="time">{{ date(n.publishTime) }}</span>
                </div>
                <div v-if="!notices.length" class="empty-tip">暂无公告</div>
            </div>
        </div>`,
        data() {
            return { home: {}, notices: [], settle: { rows: [], unitFee: 0, totalFee: 0, lessonCount: 0 },
                     today: Gym.date(new Date().toISOString()), statusText: Gym.statusText };
        },
        async mounted() {
            this.home = await Gym.api.get('/api/reports/coach-home');
            this.notices = await Gym.api.get('/api/notices/latest', { limit: 4 });
            const u = JSON.parse(Gym.session.get('user') || '{}');
            this.settle = await Gym.api.get('/api/reports/coach-settlement', { coachId: u.bizId });
        },
        methods: { time: Gym.time, date: Gym.date, money: Gym.money }
    });

    /* ---------------- 会员端：动作库（内嵌，不再新开标签） ----------------
       动作库是独立静态模块（1324 个动作 + 17MB JSON + 139MB 媒体），
       用 iframe 隔离加载，避免 17MB 数据拖慢主系统首屏。
       带 embed=1 时动作库会隐藏自身的品牌栏，只保留搜索与筛选。 */
    Gym.define('exercise', {
        template: `
        <div class="exercise-embed">
            <iframe src="/exercises/?embed=1" title="健身动作库"
                    loading="lazy" referrerpolicy="no-referrer"></iframe>
        </div>`
    });
})(window.Gym);
