/* 管理员：课程、排课、预约签到、体测、器材、公告、报表 */
(function (Gym) {
    /* ---------------- 课程管理 ---------------- */
    Gym.define('course', {
        template: `
        <div>
            <div class="card">
                <div class="toolbar">
                    <el-input v-model="query.keyword" placeholder="课程名称" clearable style="width:200px" @keyup.enter="load"/>
                    <el-select v-model="query.courseType" placeholder="课程类型" clearable style="width:150px">
                        <el-option v-for="(v,k) in courseType" :key="k" :label="v" :value="k"/>
                    </el-select>
                    <el-button type="primary" @click="load">查询</el-button>
                    <el-button @click="reset">重置</el-button>
                    <div class="right"><el-button type="primary" @click="openCreate">新增课程</el-button></div>
                </div>
                <el-table :data="rows" border stripe height="520">
                    <el-table-column prop="courseName" label="课程名称" width="130"/>
                    <el-table-column label="类型" width="110"><template #default="{row}">{{ courseType[row.courseType] }}</template></el-table-column>
                    <el-table-column label="难度" width="90">
                        <template #default="{row}">
                            <el-tag size="small" :type="['','success','warning','danger'][row.difficulty]" effect="light">
                                {{ ['','入门','进阶','高强度'][row.difficulty] }}
                            </el-tag>
                        </template>
                    </el-table-column>
                    <el-table-column label="时长" width="90"><template #default="{row}">{{ row.duration }} 分钟</template></el-table-column>
                    <el-table-column prop="capacity" label="容量" width="80"/>
                    <el-table-column label="单节价" width="100"><template #default="{row}"><span class="money">{{ money(row.price) }}</span></template></el-table-column>
                    <el-table-column label="默认教练" width="110">
                        <template #default="{row}">{{ coachName(row.coachId) }}</template>
                    </el-table-column>
                    <el-table-column prop="description" label="课程简介" show-overflow-tooltip/>
                    <el-table-column label="状态" width="90">
                        <template #default="{row}">
                            <el-tag size="small" :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '已上架' : '已下架' }}</el-tag>
                        </template>
                    </el-table-column>
                    <el-table-column label="操作" width="140" fixed="right">
                        <template #default="{row}">
                            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
                            <el-button link :type="row.status === 1 ? 'danger' : 'success'" @click="toggle(row)">{{ row.status === 1 ? '下架' : '上架' }}</el-button>
                        </template>
                    </el-table-column>
                </el-table>
                <div class="pager">
                    <el-pagination background layout="total, prev, pager, next" :total="total"
                                   :page-size="query.pageSize" :current-page="query.pageNum"
                                   @current-change="p => { query.pageNum = p; load(); }"/>
                </div>
            </div>

            <el-dialog v-model="dialog" :title="form.id ? '编辑课程' : '新增课程'" width="620px">
                <el-form :model="form" label-width="96px">
                    <el-row :gutter="12">
                        <el-col :span="12"><el-form-item label="课程名称"><el-input v-model="form.courseName"/></el-form-item></el-col>
                        <el-col :span="12">
                            <el-form-item label="课程类型">
                                <el-select v-model="form.courseType" style="width:100%">
                                    <el-option v-for="(v,k) in courseType" :key="k" :label="v" :value="k"/>
                                </el-select>
                            </el-form-item>
                        </el-col>
                        <el-col :span="12">
                            <el-form-item label="难度">
                                <el-select v-model="form.difficulty" style="width:100%">
                                    <el-option label="入门" :value="1"/><el-option label="进阶" :value="2"/><el-option label="高强度" :value="3"/>
                                </el-select>
                            </el-form-item>
                        </el-col>
                        <el-col :span="12"><el-form-item label="时长(分钟)"><el-input-number v-model="form.duration" :min="15" :step="5" style="width:100%"/></el-form-item></el-col>
                        <el-col :span="12"><el-form-item label="容量(人)"><el-input-number v-model="form.capacity" :min="1" style="width:100%"/></el-form-item></el-col>
                        <el-col :span="12">
                            <el-form-item label="默认教练">
                                <el-select v-model="form.coachId" style="width:100%">
                                    <el-option v-for="c in coaches" :key="c.id" :label="c.name" :value="c.id"/>
                                </el-select>
                            </el-form-item>
                        </el-col>
                        <el-col :span="24"><el-form-item label="课程简介"><el-input v-model="form.description" type="textarea" :rows="2"/></el-form-item></el-col>
                    </el-row>
                </el-form>
                <template #footer>
                    <el-button @click="dialog = false">取消</el-button>
                    <el-button type="primary" @click="save">保存</el-button>
                </template>
            </el-dialog>
        </div>`,
        data() {
            return {
                query: { keyword: '', courseType: '', pageNum: 1, pageSize: 10 },
                rows: [], total: 0, dialog: false, form: {}, coaches: [],
                courseType: Gym.courseType
            };
        },
        async mounted() {
            await this.load();
            const page = await Gym.api.get('/api/coaches', { pageNum: 1, pageSize: 50 });
            this.coaches = page.records || [];
        },
        methods: {
            money: Gym.money,
            coachName(id) {
                const c = this.coaches.find(i => i.id === id);
                return c ? c.name : '-';
            },
            async load() {
                const page = await Gym.api.get('/api/courses', this.query);
                this.rows = page.records || [];
                this.total = page.total || 0;
            },
            reset() { this.query = { keyword: '', courseType: '', pageNum: 1, pageSize: 10 }; this.load(); },
            openCreate() { this.form = { difficulty: 1, duration: 60, capacity: 20, price: 0 }; this.dialog = true; },
            openEdit(row) { this.form = JSON.parse(JSON.stringify(row)); this.dialog = true; },
            async save() {
                if (this.form.id) await Gym.api.put('/api/courses/' + this.form.id, this.form);
                else await Gym.api.post('/api/courses', this.form);
                Gym.msg.ok('保存成功');
                this.dialog = false;
                this.load();
            },
            async toggle(row) {
                await Gym.api.put('/api/courses/' + row.id + '/status', { status: row.status === 1 ? 0 : 1 });
                Gym.msg.ok('状态已更新');
                this.load();
            }
        }
    });

    /* ---------------- 排课管理 ---------------- */
    Gym.define('schedule', {
        template: `
        <div>
            <div class="card">
                <div class="toolbar">
                    <el-date-picker v-model="range" type="daterange" range-separator="至" start-placeholder="开始日期"
                                    end-placeholder="结束日期" value-format="YYYY-MM-DD" style="width:250px"/>
                    <el-select v-model="query.coachId" placeholder="教练" clearable style="width:120px" v-if="isAdmin">
                        <el-option v-for="c in coaches" :key="c.id" :label="c.name" :value="c.id"/>
                    </el-select>
                    <el-select v-model="query.courseId" placeholder="课程" clearable style="width:140px">
                        <el-option v-for="c in courses" :key="c.id" :label="c.courseName" :value="c.id"/>
                    </el-select>
                    <el-select v-model="query.status" placeholder="状态" clearable style="width:120px">
                        <el-option label="待开课" value="WAITING"/><el-option label="进行中" value="ONGOING"/>
                        <el-option label="已结束" value="FINISHED"/><el-option label="已取消" value="CANCELLED"/>
                    </el-select>
                    <el-button type="primary" @click="load">查询</el-button>
                    <div class="right" v-if="isAdmin">
                        <el-button type="primary" @click="openCreate">新增排课</el-button>
                    </div>
                </div>
                <el-table :data="rows" border stripe height="520">
                    <el-table-column label="上课日期" width="112"><template #default="{row}">{{ date(row.classDate) }}</template></el-table-column>
                    <el-table-column label="时间" width="112">
                        <template #default="{row}">{{ time(row.startTime) }}-{{ time(row.endTime) }}</template>
                    </el-table-column>
                    <el-table-column prop="courseName" label="课程" width="118"/>
                    <el-table-column label="类型" width="100"><template #default="{row}">{{ courseType[row.courseType] }}</template></el-table-column>
                    <el-table-column prop="coachName" label="教练" width="92"/>
                    <el-table-column prop="room" label="教室" width="94"/>
                    <el-table-column label="预约情况" width="132">
                        <template #default="{row}">
                            <span class="seat" :class="{full: row.remain === 0}">{{ row.bookedCount }} / {{ row.capacity }}</span>
                            <span class="muted"> 人</span>
                        </template>
                    </el-table-column>
                    <el-table-column label="状态" width="92">
                        <template #default="{row}">
                            <el-tag size="small" :type="scheduleTag[row.status]">
                                {{ statusText[row.status] }}
                            </el-tag>
                        </template>
                    </el-table-column>
                    <el-table-column prop="remark" label="备注" show-overflow-tooltip/>
                    <el-table-column label="操作" width="168" fixed="right">
                        <template #default="{row}">
                            <el-button link type="primary" @click="openRoster(row)">预约名单</el-button>
                            <el-button link type="danger" v-if="isAdmin && row.status !== 'CANCELLED' && row.status !== 'FINISHED'"
                                       @click="cancel(row)">取消排课</el-button>
                        </template>
                    </el-table-column>
                </el-table>
            </div>

            <el-dialog v-model="dialog" title="新增排课" width="600px">
                <el-form :model="form" label-width="92px">
                    <el-row :gutter="12">
                        <el-col :span="12">
                            <el-form-item label="课程">
                                <el-select v-model="form.courseId" style="width:100%" @change="onCourse">
                                    <el-option v-for="c in courses" :key="c.id" :label="c.courseName" :value="c.id"/>
                                </el-select>
                            </el-form-item>
                        </el-col>
                        <el-col :span="12">
                            <el-form-item label="授课教练">
                                <el-select v-model="form.coachId" style="width:100%">
                                    <el-option v-for="c in coaches" :key="c.id" :label="c.name" :value="c.id"/>
                                </el-select>
                            </el-form-item>
                        </el-col>
                        <el-col :span="12">
                            <el-form-item label="上课日期">
                                <el-date-picker v-model="form.classDate" type="date" value-format="YYYY-MM-DD" style="width:100%"/>
                            </el-form-item>
                        </el-col>
                        <el-col :span="12"><el-form-item label="教室"><el-input v-model="form.room" placeholder="如 操房A"/></el-form-item></el-col>
                        <el-col :span="12">
                            <el-form-item label="开始时间">
                                <el-time-picker v-model="form.startTime" value-format="HH:mm:ss" style="width:100%"/>
                            </el-form-item>
                        </el-col>
                        <el-col :span="12">
                            <el-form-item label="结束时间">
                                <el-time-picker v-model="form.endTime" value-format="HH:mm:ss" style="width:100%"/>
                            </el-form-item>
                        </el-col>
                        <el-col :span="12"><el-form-item label="容量"><el-input-number v-model="form.capacity" :min="1" style="width:100%"/></el-form-item></el-col>
                    </el-row>
                </el-form>
                <template #footer>
                    <el-button @click="dialog = false">取消</el-button>
                    <el-button type="primary" @click="save">保存</el-button>
                </template>
            </el-dialog>

            <el-drawer v-model="drawer" :title="rosterTitle" size="640px" :modal="false" :lock-scroll="false">
                <el-table :data="roster" border size="small">
                    <el-table-column prop="memberName" label="会员" width="80"/>
                    <el-table-column prop="memberNo" label="会员编号" width="104"/>
                    <el-table-column label="预约时间" width="136"><template #default="{row}">{{ (row.reserveTime||'').replace('T',' ').slice(0,16) }}</template></el-table-column>
                    <el-table-column label="状态" width="80">
                        <template #default="{row}">
                            <el-tag size="small" :type="resTag[row.status]">
                                {{ statusText[row.status] }}
                            </el-tag>
                        </template>
                    </el-table-column>
                    <el-table-column label="操作">
                        <template #default="{row}">
                            <el-button v-if="row.status === 'BOOKED' && row.classDate === today" link type="primary" @click="checkin(row)">签到</el-button>
                            <span v-else class="muted">{{ row.checkInTime ? (row.checkInTime||'').replace('T',' ').slice(11,16) + ' 已签到' : '-' }}</span>
                        </template>
                    </el-table-column>
                </el-table>
                <div v-if="!roster.length" class="empty-tip">该节课暂无预约记录</div>
            </el-drawer>
        </div>`,
        data() {
            const now = new Date();
            const fmt = d => d.toISOString().slice(0, 10);
            const before = new Date(now.getTime() - 7 * 86400000);
            const after = new Date(now.getTime() + 7 * 86400000);
            return {
                range: [fmt(before), fmt(after)],
                query: { coachId: null, courseId: null, status: '', pageNum: 1, pageSize: 100 },
                rows: [], dialog: false, form: {}, coaches: [], courses: [],
                drawer: false, roster: [], rosterTitle: '',
                courseType: Gym.courseType, statusText: Gym.statusText, today: Gym.date(new Date().toISOString())
            };
        },
        computed: {
            isAdmin() { return this.$root.user.role === 'ADMIN'; }
        },
        async mounted() {
            const [c, co] = await Promise.all([
                Gym.api.get('/api/coaches', { pageNum: 1, pageSize: 50 }),
                Gym.api.get('/api/courses', { pageNum: 1, pageSize: 50 })
            ]);
            this.coaches = c.records || [];
            this.courses = co.records || [];
            await this.load();
        },
        methods: {
            date: Gym.date, time: Gym.time,
            async load() {
                const params = { ...this.query, coachId: this.query.coachId || undefined };
                if (this.range && this.range.length === 2) {
                    params.startDate = this.range[0];
                    params.endDate = this.range[1];
                }
                this.rows = await Gym.api.get('/api/schedules', params);
            },
            onCourse(id) {
                const c = this.courses.find(i => i.id === id);
                if (c) {
                    this.form.capacity = c.capacity;
                    this.form.coachId = this.form.coachId || c.coachId;
                }
            },
            openCreate() {
                this.form = { classDate: new Date().toISOString().slice(0, 10), startTime: '19:00:00', endTime: '20:00:00', room: '操房A' };
                this.dialog = true;
            },
            async save() {
                await Gym.api.post('/api/schedules', this.form);
                Gym.msg.ok('排课成功');
                this.dialog = false;
                this.load();
            },
            async cancel(row) {
                await ElementPlus.ElMessageBox.confirm('取消后该节课的预约将一并取消，是否继续？', '取消排课', { type: 'warning' });
                const res = await Gym.api.put('/api/schedules/' + row.id + '/cancel', { reason: '场馆调整' });
                Gym.msg.ok('已取消排课，同时取消 ' + res.cancelledReservations + ' 条预约');
                this.load();
            },
            async openRoster(row) {
                this.rosterTitle = row.courseName + ' · ' + this.date(row.classDate) + ' ' + this.time(row.startTime);
                this.roster = await Gym.api.get('/api/schedules/' + row.id + '/reservations');
                this.currentRow = row;
                this.drawer = true;
            },
            async checkin(row) {
                await Gym.api.put('/api/reservations/' + row.id + '/checkin');
                Gym.msg.ok('签到成功');
                this.openRoster(this.currentRow);
            }
        }
    });

    /* ---------------- 预约与签到 ---------------- */
    Gym.define('reservation', {
        template: `
        <div>
            <div class="card">
                <div class="toolbar">
                    <el-select v-model="query.status" placeholder="预约状态" clearable style="width:140px">
                        <el-option label="已预约" value="BOOKED"/><el-option label="已签到" value="CHECKED_IN"/>
                        <el-option label="已完成" value="COMPLETED"/><el-option label="已取消" value="CANCELLED"/>
                        <el-option label="未到场" value="NO_SHOW"/>
                    </el-select>
                    <el-button type="primary" @click="load">查询</el-button>
                    <el-button @click="reset">重置</el-button>
                    <span class="muted" style="margin-left:8px">共 {{ total }} 条预约记录</span>
                </div>
                <el-table :data="rows" border stripe height="560">
                    <el-table-column prop="reserveNo" label="预约号" width="146"/>
                    <el-table-column prop="memberName" label="会员" min-width="120"/>
                    <el-table-column prop="courseName" label="课程" min-width="132"/>
                    <el-table-column prop="coachName" label="教练" width="88"/>
                    <el-table-column label="上课时间" width="176">
                        <template #default="{row}">{{ date(row.classDate) }} {{ time(row.startTime) }}-{{ time(row.endTime) }}</template>
                    </el-table-column>
                    <el-table-column prop="room" label="教室" width="88"/>
                    <el-table-column label="预约时间" width="150">
                        <template #default="{row}">{{ (row.reserveTime||'').replace('T',' ').slice(0,16) }}</template>
                    </el-table-column>
                    <el-table-column label="状态" width="92">
                        <template #default="{row}">
                            <el-tag size="small" :type="resTag[row.status]">
                                {{ statusText[row.status] }}
                            </el-tag>
                        </template>
                    </el-table-column>
                    <el-table-column label="操作" width="110" fixed="right">
                        <template #default="{row}">
                            <el-button v-if="row.status === 'BOOKED' && row.classDate === today" link type="primary" @click="checkin(row)">签到</el-button>
                            <span v-else class="muted">-</span>
                        </template>
                    </el-table-column>
                </el-table>
                <div class="pager">
                    <el-pagination background layout="total, prev, pager, next" :total="total"
                                   :page-size="query.pageSize" :current-page="query.pageNum"
                                   @current-change="p => { query.pageNum = p; load(); }"/>
                </div>
            </div>
        </div>`,
        data() {
            return {
                query: { status: '', pageNum: 1, pageSize: 10 },
                rows: [], total: 0, statusText: Gym.statusText, today: Gym.date(new Date().toISOString())
            };
        },
        mounted() { this.load(); },
        methods: {
            date: Gym.date, time: Gym.time,
            async load() {
                const page = await Gym.api.get('/api/reservations', this.query);
                this.rows = page.records || [];
                this.total = page.total || 0;
            },
            reset() { this.query = { status: '', pageNum: 1, pageSize: 10 }; this.load(); },
            async checkin(row) {
                await Gym.api.put('/api/reservations/' + row.id + '/checkin');
                Gym.msg.ok('签到成功');
                this.load();
            }
        }
    });

    /* ---------------- 体测记录 ---------------- */
    Gym.define('body', {
        template: `
        <div>
            <div class="card">
                <div class="toolbar">
                    <el-select v-if="role !== 'MEMBER'" v-model="memberId" placeholder="选择会员" filterable clearable
                               style="width:210px" @change="load">
                        <el-option v-for="m in members" :key="m.id" :label="m.name + '（' + m.memberNo + '）'" :value="m.id"/>
                    </el-select>
                    <el-button type="primary" @click="load">查询</el-button>
                    <span class="muted" style="margin-left:6px">BMI 由后端按 体重 / 身高² 自动计算</span>
                    <div class="right" v-if="role !== 'MEMBER'">
                        <el-button type="primary" @click="openCreate">录入体测</el-button>
                    </div>
                </div>
                <el-table :data="rows" border stripe height="350">
                    <el-table-column label="会员" width="150"><template #default="{row}">{{ nameOf(row.memberId) }}</template></el-table-column>
                    <el-table-column label="体测日期" width="126"><template #default="{row}">{{ date(row.recordDate) }}</template></el-table-column>
                    <el-table-column prop="height" label="身高(cm)" width="104"/>
                    <el-table-column prop="weight" label="体重(kg)" width="104"/>
                    <el-table-column prop="bmi" label="BMI" width="86"/>
                    <el-table-column prop="bodyFat" label="体脂率(%)" width="106"/>
                    <el-table-column prop="muscle" label="骨骼肌(kg)" width="112"/>
                    <el-table-column prop="waist" label="腰围(cm)" width="100"/>
                    <el-table-column prop="heartRate" label="心率" width="86"/>
                    <el-table-column prop="remark" label="教练建议" show-overflow-tooltip/>
                    <el-table-column v-if="role === 'ADMIN'" label="操作" width="80" fixed="right">
                        <template #default="{row}"><el-button link type="danger" @click="remove(row)">删除</el-button></template>
                    </el-table-column>
                </el-table>
            </div>
            <div class="card">
                <div class="card-title"><span class="t">体重与 BMI 变化趋势</span><span class="muted">按体测日期</span></div>
                <div ref="chart" class="chart" style="height:300px"></div>
            </div>

            <el-dialog v-model="dialog" title="录入体测记录" width="600px">
                <el-form :model="form" label-width="96px">
                    <el-row :gutter="12">
                        <el-col :span="12">
                            <el-form-item label="会员">
                                <el-select v-model="form.memberId" filterable style="width:100%">
                                    <el-option v-for="m in members" :key="m.id" :label="m.name + '（' + m.memberNo + '）'" :value="m.id"/>
                                </el-select>
                            </el-form-item>
                        </el-col>
                        <el-col :span="12">
                            <el-form-item label="体测日期">
                                <el-date-picker v-model="form.recordDate" type="date" value-format="YYYY-MM-DD" style="width:100%"/>
                            </el-form-item>
                        </el-col>
                        <el-col :span="12"><el-form-item label="身高(cm)"><el-input-number v-model="form.height" :min="100" :max="250" :precision="1" style="width:100%"/></el-form-item></el-col>
                        <el-col :span="12"><el-form-item label="体重(kg)"><el-input-number v-model="form.weight" :min="30" :max="200" :precision="1" style="width:100%"/></el-form-item></el-col>
                        <el-col :span="12"><el-form-item label="体脂率(%)"><el-input-number v-model="form.bodyFat" :min="0" :max="60" :precision="1" style="width:100%"/></el-form-item></el-col>
                        <el-col :span="12"><el-form-item label="骨骼肌(kg)"><el-input-number v-model="form.muscle" :min="0" :max="80" :precision="1" style="width:100%"/></el-form-item></el-col>
                        <el-col :span="12"><el-form-item label="腰围(cm)"><el-input-number v-model="form.waist" :min="40" :max="150" :precision="1" style="width:100%"/></el-form-item></el-col>
                        <el-col :span="12"><el-form-item label="静息心率"><el-input-number v-model="form.heartRate" :min="30" :max="180" style="width:100%"/></el-form-item></el-col>
                        <el-col :span="24"><el-form-item label="教练建议"><el-input v-model="form.remark" type="textarea" :rows="2"/></el-form-item></el-col>
                    </el-row>
                </el-form>
                <template #footer>
                    <el-button @click="dialog = false">取消</el-button>
                    <el-button type="primary" @click="save">保存</el-button>
                </template>
            </el-dialog>
        </div>`,
        data() {
            return {
                memberId: null, rows: [], members: [], names: {}, dialog: false, form: {},
                role: (JSON.parse(Gym.session.get('user') || 'null') || {}).role || 'ADMIN'
            };
        },
        async mounted() {
            const page = await Gym.api.get('/api/members', { pageNum: 1, pageSize: 100 });
            this.members = page.records || [];
            if (this.role === 'MEMBER') {
                const u = JSON.parse(Gym.session.get('user') || 'null') || {};
                this.memberId = u.bizId;
            } else {
                // 默认选中体测记录最多的会员，避免首屏空表
                const all = await Gym.api.get('/api/body-records', {});
                const counts = {};
                (all.records || []).forEach(r => { counts[r.memberId] = (counts[r.memberId] || 0) + 1; });
                const best = Object.keys(counts).sort((a, b) => counts[b] - counts[a])[0];
                this.memberId = best ? Number(best) : (this.members[0] ? this.members[0].id : null);
            }
            await this.load();
        },
        methods: {
            date: Gym.date,
            nameOf(id) { return this.names[id] || '-'; },
            async load() {
                const params = {};
                if (this.memberId) params.memberId = this.memberId;
                const data = await Gym.api.get('/api/body-records', params);
                this.rows = data.records || [];
                this.names = data.memberNames || {};
                this.render();
            },
            render() {
                const list = [...this.rows].sort((a, b) => a.recordDate > b.recordDate ? 1 : -1);
                Gym.drawChart(this.$refs.chart, {
                    grid: { left: 46, right: 46, top: 34, bottom: 30 },
                    tooltip: { trigger: 'axis' },
                    legend: { data: ['体重(kg)', 'BMI'], top: 0, right: 0, itemWidth: 12, itemHeight: 8 },
                    xAxis: { type: 'category', data: list.map(i => Gym.date(i.recordDate)), ...Gym.axisStyle },
                    yAxis: [
                        { type: 'value', scale: true, ...Gym.axisStyle },
                        { type: 'value', scale: true, ...Gym.axisStyle, splitLine: { show: false } }
                    ],
                    series: [
                        { name: '体重(kg)', type: 'line', smooth: true, symbolSize: 7, data: list.map(i => i.weight), itemStyle: { color: '#0071E3' }, areaStyle: { color: 'rgba(0,113,227,.08)' } },
                        { name: 'BMI', type: 'line', smooth: true, symbolSize: 7, yAxisIndex: 1, data: list.map(i => i.bmi), itemStyle: { color: '#E9A23B' } }
                    ]
                });
            },
            openCreate() {
                this.form = { memberId: this.memberId, recordDate: new Date().toISOString().slice(0, 10) };
                this.dialog = true;
            },
            async save() {
                await Gym.api.post('/api/body-records', this.form);
                Gym.msg.ok('录入成功');
                this.dialog = false;
                this.load();
            },
            async remove(row) {
                await ElementPlus.ElMessageBox.confirm('确认删除该条体测记录？', '提示', { type: 'warning' });
                await Gym.api.del('/api/body-records/' + row.id);
                Gym.msg.ok('已删除');
                this.load();
            }
        }
    });

    /* ---------------- 器材管理 ---------------- */
    Gym.define('equipment', {
        template: `
        <div>
            <div class="stat-row">
                <div class="stat-card"><div class="label">器材总数</div><div class="value">{{ stat.total || 0 }}<small>台</small></div><div class="sub">在册台账</div></div>
                <div class="stat-card c2"><div class="label">正常可用</div><div class="value">{{ stat.byStatus.NORMAL || 0 }}<small>台</small></div><div class="sub">可正常使用</div></div>
                <div class="stat-card c3"><div class="label">维修中</div><div class="value">{{ stat.byStatus.REPAIR || 0 }}<small>台</small></div><div class="sub">已报修待处理</div></div>
                <div class="stat-card c4"><div class="label">已报废</div><div class="value">{{ stat.byStatus.SCRAPPED || 0 }}<small>台</small></div><div class="sub">待处置</div></div>
            </div>
            <div class="card">
                <div class="toolbar">
                    <el-input v-model="query.keyword" placeholder="名称 / 编号 / 品牌" clearable style="width:220px" @keyup.enter="load"/>
                    <el-select v-model="query.category" placeholder="分类" clearable style="width:130px">
                        <el-option label="有氧器械" value="CARDIO"/><el-option label="力量器械" value="STRENGTH"/><el-option label="辅助器材" value="OTHER"/>
                    </el-select>
                    <el-select v-model="query.status" placeholder="状态" clearable style="width:120px">
                        <el-option label="正常" value="NORMAL"/><el-option label="维修中" value="REPAIR"/><el-option label="已报废" value="SCRAPPED"/>
                    </el-select>
                    <el-button type="primary" @click="load">查询</el-button>
                    <el-button @click="reset">重置</el-button>
                    <div class="right"><el-button type="primary" @click="openCreate">登记器材</el-button></div>
                </div>
                <el-table :data="rows" border stripe height="470">
                    <el-table-column prop="equipmentNo" label="编号" width="96"/>
                    <el-table-column prop="equipmentName" label="器材名称" width="140"/>
                    <el-table-column label="分类" width="110">
                        <template #default="{row}">{{ categoryText[row.category] }}</template>
                    </el-table-column>
                    <el-table-column prop="brand" label="品牌" width="92"/>
                    <el-table-column label="购入日期" width="112"><template #default="{row}">{{ date(row.purchaseDate) }}</template></el-table-column>
                    <el-table-column label="购入价格" width="112"><template #default="{row}"><span class="money">{{ money(row.price) }}</span></template></el-table-column>
                    <el-table-column prop="location" label="存放位置" width="118"/>
                    <el-table-column label="状态" width="92">
                        <template #default="{row}">
                            <el-tag size="small" :type="equipTag[row.status]">
                                {{ statusText[row.status] }}
                            </el-tag>
                        </template>
                    </el-table-column>
                    <el-table-column prop="faultDesc" label="故障描述" show-overflow-tooltip/>
                    <el-table-column label="操作" width="150" fixed="right">
                        <template #default="{row}">
                            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
                            <el-button link type="warning" v-if="row.status === 'NORMAL'" @click="openRepair(row)">报修</el-button>
                            <el-button link type="success" v-if="row.status === 'REPAIR'" @click="restore(row)">修复归位</el-button>
                        </template>
                    </el-table-column>
                </el-table>
                <div class="pager">
                    <el-pagination background layout="total, prev, pager, next" :total="page.total"
                                   :page-size="query.pageSize" :current-page="query.pageNum"
                                   @current-change="p => { query.pageNum = p; load(); }"/>
                </div>
            </div>

            <el-dialog v-model="dialog" :title="form.id ? '编辑器材' : '登记器材'" width="600px">
                <el-form :model="form" label-width="96px">
                    <el-row :gutter="12">
                        <el-col :span="12"><el-form-item label="器材编号"><el-input v-model="form.equipmentNo" placeholder="EQ0011"/></el-form-item></el-col>
                        <el-col :span="12"><el-form-item label="器材名称"><el-input v-model="form.equipmentName"/></el-form-item></el-col>
                        <el-col :span="12">
                            <el-form-item label="分类">
                                <el-select v-model="form.category" style="width:100%">
                                    <el-option label="有氧器械" value="CARDIO"/><el-option label="力量器械" value="STRENGTH"/><el-option label="辅助器材" value="OTHER"/>
                                </el-select>
                            </el-form-item>
                        </el-col>
                        <el-col :span="12"><el-form-item label="品牌"><el-input v-model="form.brand"/></el-form-item></el-col>
                        <el-col :span="12">
                            <el-form-item label="购入日期">
                                <el-date-picker v-model="form.purchaseDate" type="date" value-format="YYYY-MM-DD" style="width:100%"/>
                            </el-form-item>
                        </el-col>
                        <el-col :span="12"><el-form-item label="购入价格"><el-input-number v-model="form.price" :min="0" :step="100" style="width:100%"/></el-form-item></el-col>
                        <el-col :span="24"><el-form-item label="存放位置"><el-input v-model="form.location" placeholder="如有氧区A"/></el-form-item></el-col>
                    </el-row>
                </el-form>
                <template #footer>
                    <el-button @click="dialog = false">取消</el-button>
                    <el-button type="primary" @click="save">保存</el-button>
                </template>
            </el-dialog>

            <el-dialog v-model="repairDialog" title="器材报修" width="480px">
                <el-form :model="repairForm" label-width="86px">
                    <el-form-item label="故障描述"><el-input v-model="repairForm.faultDesc" type="textarea" :rows="3"/></el-form-item>
                </el-form>
                <template #footer>
                    <el-button @click="repairDialog = false">取消</el-button>
                    <el-button type="primary" @click="doRepair">提交报修</el-button>
                </template>
            </el-dialog>
        </div>`,
        data() {
            return {
                query: { keyword: '', category: '', status: '', pageNum: 1, pageSize: 10 },
                rows: [], page: {}, stat: { byStatus: {} }, dialog: false, form: {},
                repairDialog: false, repairForm: {}, statusText: Gym.statusText
            };
        },
        mounted() { this.load(); },
        methods: {
            date: Gym.date, money: Gym.money,
            async load() {
                const data = await Gym.api.get('/api/equipments', this.query);
                this.rows = data.page.records || [];
                this.page = data.page;
                this.stat = { total: data.total, byStatus: data.byStatus || {} };
            },
            reset() { this.query = { keyword: '', category: '', status: '', pageNum: 1, pageSize: 10 }; this.load(); },
            openCreate() { this.form = { category: 'CARDIO' }; this.dialog = true; },
            openEdit(row) { this.form = JSON.parse(JSON.stringify(row)); this.dialog = true; },
            async save() {
                if (this.form.id) await Gym.api.put('/api/equipments/' + this.form.id, this.form);
                else await Gym.api.post('/api/equipments', this.form);
                Gym.msg.ok('保存成功');
                this.dialog = false;
                this.load();
            },
            openRepair(row) { this.repairForm = { id: row.id, faultDesc: '' }; this.repairDialog = true; },
            async doRepair() {
                await Gym.api.put('/api/equipments/' + this.repairForm.id + '/status',
                    { status: 'REPAIR', faultDesc: this.repairForm.faultDesc });
                Gym.msg.ok('已提交报修');
                this.repairDialog = false;
                this.load();
            },
            async restore(row) {
                await Gym.api.put('/api/equipments/' + row.id + '/status', { status: 'NORMAL', repairCost: 0 });
                Gym.msg.ok('已修复归位');
                this.load();
            }
        }
    });

    /* ---------------- 公告管理 ---------------- */
    Gym.define('notice', {
        template: `
        <div>
            <div class="card">
                <div class="toolbar">
                    <el-input v-model="query.keyword" placeholder="公告标题" clearable style="width:230px" @keyup.enter="load"/>
                    <el-select v-if="isAdmin" v-model="query.status" placeholder="状态" clearable style="width:120px">
                        <el-option label="已发布" :value="1"/><el-option label="已下线" :value="0"/>
                    </el-select>
                    <el-button type="primary" @click="load">查询</el-button>
                    <div class="right" v-if="isAdmin"><el-button type="primary" @click="openCreate">发布公告</el-button></div>
                </div>
                <el-table :data="rows" border stripe height="520">
                    <el-table-column prop="title" label="标题" width="260"/>
                    <el-table-column prop="content" label="内容" show-overflow-tooltip/>
                    <el-table-column label="可见范围" width="100">
                        <template #default="{row}">{{ targetRole[row.targetRole] }}</template>
                    </el-table-column>
                    <el-table-column label="发布时间" width="150">
                        <template #default="{row}">{{ (row.publishTime||'').replace('T',' ').slice(0,16) }}</template>
                    </el-table-column>
                    <el-table-column prop="viewCount" label="阅读" width="80"/>
                    <el-table-column label="状态" width="90">
                        <template #default="{row}">
                            <el-tag size="small" :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '已发布' : '已下线' }}</el-tag>
                        </template>
                    </el-table-column>
                    <el-table-column v-if="isAdmin" label="操作" width="130" fixed="right">
                        <template #default="{row}">
                            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
                            <el-button link :type="row.status === 1 ? 'danger' : 'success'" @click="toggle(row)">{{ row.status === 1 ? '下线' : '发布' }}</el-button>
                        </template>
                    </el-table-column>
                </el-table>
                <div class="pager">
                    <el-pagination background layout="total, prev, pager, next" :total="total"
                                   :page-size="query.pageSize" :current-page="query.pageNum"
                                   @current-change="p => { query.pageNum = p; load(); }"/>
                </div>
            </div>

            <el-dialog v-model="dialog" :title="form.id ? '编辑公告' : '发布公告'" width="620px">
                <el-form :model="form" label-width="86px">
                    <el-form-item label="标题"><el-input v-model="form.title"/></el-form-item>
                    <el-form-item label="可见范围">
                        <el-radio-group v-model="form.targetRole">
                            <el-radio label="ALL">全部</el-radio><el-radio label="MEMBER">仅会员</el-radio><el-radio label="COACH">仅教练</el-radio>
                        </el-radio-group>
                    </el-form-item>
                    <el-form-item label="内容"><el-input v-model="form.content" type="textarea" :rows="5"/></el-form-item>
                </el-form>
                <template #footer>
                    <el-button @click="dialog = false">取消</el-button>
                    <el-button type="primary" @click="save">保存并发布</el-button>
                </template>
            </el-dialog>
        </div>`,
        data() {
            return { query: { keyword: '', status: null, pageNum: 1, pageSize: 10 }, rows: [], total: 0, dialog: false, form: {}, targetRole: Gym.targetRole };
        },
        computed: {
            isAdmin() { return this.$root.user.role === 'ADMIN'; }
        },
        mounted() { this.load(); },
        methods: {
            async load() {
                const page = await Gym.api.get('/api/notices', this.query);
                this.rows = page.records || [];
                this.total = page.total || 0;
            },
            openCreate() { this.form = { targetRole: 'ALL', status: 1 }; this.dialog = true; },
            openEdit(row) { this.form = JSON.parse(JSON.stringify(row)); this.dialog = true; },
            async save() {
                this.form.status = 1;
                if (this.form.id) await Gym.api.put('/api/notices/' + this.form.id, this.form);
                else await Gym.api.post('/api/notices', this.form);
                Gym.msg.ok('保存成功');
                this.dialog = false;
                this.load();
            },
            async toggle(row) {
                await Gym.api.put('/api/notices/' + row.id + '/status', { status: row.status === 1 ? 0 : 1 });
                Gym.msg.ok('状态已更新');
                this.load();
            }
        }
    });

    /* ---------------- 统计报表 ---------------- */
    Gym.define('report', {
        template: `
        <div>
            <div class="two-col">
                <div class="card">
                    <div class="card-title"><span class="t">会员增长趋势</span><span class="muted">近 6 个月</span></div>
                    <div ref="growth" class="chart-sm"></div>
                </div>
                <div class="card">
                    <div class="card-title"><span class="t">会员卡营收</span><span class="muted">近 6 个月</span></div>
                    <div ref="revenue" class="chart-sm"></div>
                </div>
            </div>
            <div class="two-col">
                <div class="card">
                    <div class="card-title"><span class="t">课程预约热度</span><span class="muted">Top 5</span></div>
                    <div ref="hot" class="chart-sm"></div>
                </div>
                <div class="card">
                    <div class="card-title"><span class="t">教练课时统计</span><span class="muted">本月已结束课程</span></div>
                    <div ref="workload" class="chart-sm"></div>
                </div>
            </div>
        </div>`,
        async mounted() {
            const [growth, revenue, hot, workload] = await Promise.all([
                Gym.api.get('/api/reports/member-growth', { months: 6 }),
                Gym.api.get('/api/reports/revenue', { months: 6 }),
                Gym.api.get('/api/reports/course-hot', { limit: 5 }),
                Gym.api.get('/api/reports/coach-workload')
            ]);
            Gym.drawChart(this.$refs.growth, {
                grid: { left: 40, right: 18, top: 26, bottom: 28 },
                tooltip: { trigger: 'axis' },
                legend: { data: ['新增会员', '累计会员'], top: 0, right: 0, itemWidth: 12, itemHeight: 8 },
                xAxis: { type: 'category', data: growth.map(i => i.month), ...Gym.axisStyle },
                yAxis: { type: 'value', ...Gym.axisStyle },
                series: [
                    { name: '新增会员', type: 'bar', barWidth: 16, data: growth.map(i => i.newCount), itemStyle: { color: '#0071E3', borderRadius: [4, 4, 0, 0] } },
                    { name: '累计会员', type: 'line', smooth: true, data: growth.map(i => i.totalCount), itemStyle: { color: '#E9A23B' } }
                ]
            });
            Gym.drawChart(this.$refs.revenue, {
                grid: { left: 54, right: 18, top: 26, bottom: 28 },
                tooltip: { trigger: 'axis' },
                xAxis: { type: 'category', data: revenue.map(i => i.month), ...Gym.axisStyle },
                yAxis: { type: 'value', ...Gym.axisStyle },
                series: [{
                    type: 'bar', barWidth: 22, data: revenue.map(i => i.amount),
                    itemStyle: { color: '#E9A23B', borderRadius: [4, 4, 0, 0] },
                    label: { show: true, position: 'top', color: '#86868B', fontSize: 11 }
                }]
            });
            Gym.drawChart(this.$refs.hot, {
                grid: { left: 80, right: 40, top: 20, bottom: 24 },
                tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
                xAxis: { type: 'value', ...Gym.axisStyle },
                yAxis: { type: 'category', data: hot.map(i => i.courseName).reverse(), ...Gym.axisStyle },
                series: [{
                    type: 'bar', barWidth: 14, data: hot.map(i => i.reserveCount).reverse(),
                    itemStyle: { color: '#0071E3', borderRadius: [0, 6, 6, 0] },
                    label: { show: true, position: 'right', color: '#86868B', fontSize: 12 }
                }]
            });
            Gym.drawChart(this.$refs.workload, {
                grid: { left: 54, right: 20, top: 26, bottom: 28 },
                tooltip: { trigger: 'axis' },
                legend: { data: ['课时数', '服务人数'], top: 0, right: 0, itemWidth: 12, itemHeight: 8 },
                xAxis: { type: 'category', data: workload.map(i => i.coachName), ...Gym.axisStyle },
                yAxis: { type: 'value', ...Gym.axisStyle },
                series: [
                    { name: '课时数', type: 'bar', barWidth: 18, data: workload.map(i => i.lessonCount), itemStyle: { color: '#E9A23B', borderRadius: [4, 4, 0, 0] } },
                    { name: '服务人数', type: 'bar', barWidth: 18, data: workload.map(i => i.memberCount), itemStyle: { color: '#0071E3', borderRadius: [4, 4, 0, 0] } }
                ]
            });
        }
    });
})(window.Gym);

    /* ---------------- 会员预警 ---------------- */
    Gym.define('alerts', {
        template: `
        <div>
            <div class="card">
                <div class="toolbar">
                    <el-select v-model="query.alertType" placeholder="预警类型" clearable style="width:150px">
                        <el-option label="卡到期提醒" value="DUE"/><el-option label="流失风险" value="CHURN"/>
                    </el-select>
                    <el-select v-model="query.status" placeholder="处理状态" clearable style="width:140px">
                        <el-option label="待处理" value="OPEN"/><el-option label="已处理" value="HANDLED"/>
                    </el-select>
                    <el-button type="primary" @click="load">查询</el-button>
                    <span class="muted" style="margin-left:8px">到期预警：卡临期 7 天内自动生成；流失预警：连续 14 天未到店自动生成</span>
                </div>
                <el-table :data="rows" border stripe height="560">
                    <el-table-column prop="memberName" label="会员" width="92"/>
                    <el-table-column prop="memberNo" label="会员编号" width="120"/>
                    <el-table-column prop="memberPhone" label="手机号" width="132"/>
                    <el-table-column label="类型" width="100">
                        <template #default="{row}">
                            <el-tag size="small" :type="row.alertType === 'DUE' ? 'warning' : 'danger'" effect="light">
                                {{ row.alertType === 'DUE' ? '卡到期' : '流失风险' }}
                            </el-tag>
                        </template>
                    </el-table-column>
                    <el-table-column prop="content" label="预警内容" show-overflow-tooltip/>
                    <el-table-column label="生成时间" width="150"><template #default="{row}">{{ fmt(row.createTime) }}</template></el-table-column>
                    <el-table-column label="状态" width="90">
                        <template #default="{row}">
                            <el-tag size="small" :type="row.status === 'OPEN' ? 'primary' : 'success'">{{ row.status === 'OPEN' ? '待处理' : '已处理' }}</el-tag>
                        </template>
                    </el-table-column>
                    <el-table-column label="操作" width="110" fixed="right">
                        <template #default="{row}">
                            <el-button v-if="row.status === 'OPEN'" link type="primary" @click="handle(row)">标记已处理</el-button>
                            <span v-else class="muted">-</span>
                        </template>
                    </el-table-column>
                </el-table>
                <div class="pager">
                    <el-pagination background layout="total, prev, pager, next" :total="total"
                                   :page-size="query.pageSize" :current-page="query.pageNum"
                                   @current-change="p => { query.pageNum = p; load(); }"/>
                </div>
            </div>
        </div>`,
        data() {
            return { query: { alertType: '', status: 'OPEN', pageNum: 1, pageSize: 10 }, rows: [], total: 0 };
        },
        mounted() { this.load(); },
        methods: {
            fmt(v) { return (v || '').replace('T', ' ').slice(0, 16); },
            async load() {
                const page = await Gym.api.get('/api/member-alerts', this.query);
                this.rows = page.records || [];
                this.total = page.total || 0;
            },
            async handle(row) {
                await ElementPlus.ElMessageBox.prompt('回填处理结果（如：已电话回访并续费）', '标记已处理', { type: 'warning' })
                    .then(({ value }) => Gym.api.put('/api/member-alerts/' + row.id + '/handle', { note: value || '已处理' }));
                Gym.msg.ok('已处理');
                this.load();
            }
        }
    });

    /* ---------------- 到店记录 ---------------- */
    Gym.define('checkins', {
        template: `
        <div>
            <div class="card">
                <div class="toolbar">
                    <span class="muted">到店率口径：到店人次 ÷ 有效会员数，区分团课签到（COURSE）与自助训练（SELF）</span>
                    <div class="right">
                        <el-button type="primary" @click="openAdd">登记自助入场</el-button>
                    </div>
                </div>
                <el-table :data="rows" border stripe height="540">
                    <el-table-column prop="memberName" label="会员" width="100"/>
                    <el-table-column prop="memberNo" label="会员编号" width="120"/>
                    <el-table-column label="类型" width="120">
                        <template #default="{row}">
                            <el-tag size="small" :type="row.checkinType === 'COURSE' ? 'primary' : 'success'" effect="light">
                                {{ row.checkinType === 'COURSE' ? '团课签到' : '自助训练' }}
                            </el-tag>
                        </template>
                    </el-table-column>
                    <el-table-column label="到店时间" width="160">
                        <template #default="{row}">{{ (row.createTime||'').replace('T',' ').slice(0,16) }}</template>
                    </el-table-column>
                </el-table>
                <div class="pager">
                    <el-pagination background layout="total, prev, pager, next" :total="total"
                                   :page-size="query.pageSize" :current-page="query.pageNum"
                                   @current-change="p => { query.pageNum = p; load(); }"/>
                </div>
            </div>

            <el-dialog v-model="dialog" title="登记自助入场" width="440px">
                <el-form label-width="86px">
                    <el-form-item label="会员">
                        <el-select v-model="form.memberId" filterable style="width:100%">
                            <el-option v-for="m in members" :key="m.id" :label="m.name + '（' + m.memberNo + '）'" :value="m.id"/>
                        </el-select>
                    </el-form-item>
                </el-form>
                <template #footer>
                    <el-button @click="dialog = false">取消</el-button>
                    <el-button type="primary" @click="save">登记</el-button>
                </template>
            </el-dialog>
        </div>`,
        data() {
            return { query: { pageNum: 1, pageSize: 10 }, rows: [], total: 0, dialog: false, form: {}, members: [] };
        },
        async mounted() {
            await this.load();
            const page = await Gym.api.get('/api/members', { pageNum: 1, pageSize: 100 });
            this.members = page.records || [];
        },
        methods: {
            async load() {
                const page = await Gym.api.get('/api/checkin-logs', this.query);
                this.rows = page.records || [];
                this.total = page.total || 0;
            },
            openAdd() { this.form = {}; this.dialog = true; },
            async save() {
                await Gym.api.post('/api/checkin-logs', this.form);
                Gym.msg.ok('已登记');
                this.dialog = false;
                this.load();
            }
        }
    });
