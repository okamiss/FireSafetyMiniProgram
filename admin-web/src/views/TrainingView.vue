<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type UploadFile } from 'element-plus'
import { Download, Plus, Refresh, Upload, VideoPlay } from '@element-plus/icons-vue'
import { listEnterprises, listEnterpriseUsers, type Enterprise, type EnterpriseUser } from '../api/organizations'
import {
  createQuestion,
  createTask,
  downloadQuestionImportTemplate,
  getCertificateObjectUrl,
  importQuestions,
  listCertificates,
  listQuestions,
  listRecords,
  listTasks,
  publishTask,
  type QuestionType,
  type TrainingCertificate,
  type TrainingQuestion,
  type TrainingRecord,
  type TrainingTask,
} from '../api/training'

type TabName = 'questions' | 'tasks' | 'records' | 'certificates'

const activeTab = ref<TabName>('questions')
const loading = ref(false)
const submitting = ref(false)
const importing = ref(false)
const questions = ref<TrainingQuestion[]>([])
const tasks = ref<TrainingTask[]>([])
const records = ref<TrainingRecord[]>([])
const certificates = ref<TrainingCertificate[]>([])
const enterprises = ref<Enterprise[]>([])
const enterpriseUsers = ref<EnterpriseUser[]>([])
const questionDialog = ref(false)
const taskDialog = ref(false)
const recordKeyword = ref('')
const recordTaskId = ref<number | null>(null)
const certificateUrls = new Set<string>()

const questionForm = reactive({
  type: 'SINGLE_CHOICE' as QuestionType,
  title: '', optionA: '', optionB: '', optionC: '', optionD: '',
  correctAnswers: [] as string[], score: 10, category: '', explanation: '',
})
const taskForm = reactive({
  title: '', description: '', period: [] as string[], passScore: 60, maxAttempts: 3,
  questionIds: [] as number[], targetMode: 'enterprise' as 'enterprise' | 'user',
  targetEnterpriseIds: [] as number[], targetUserIds: [] as number[],
})

const typeLabels: Record<QuestionType, string> = {
  SINGLE_CHOICE: '单选题', MULTIPLE_CHOICE: '多选题', TRUE_FALSE: '判断题',
}
const filteredRecords = computed(() => records.value.filter(item => {
  const keyword = recordKeyword.value.trim().toLowerCase()
  const matchesKeyword = !keyword || [item.userName, item.enterpriseName, item.taskTitle]
    .some(value => value.toLowerCase().includes(keyword))
  return matchesKeyword && (!recordTaskId.value || item.taskId === recordTaskId.value)
}))
const singleAnswer = computed({
  get: () => questionForm.correctAnswers[0] || '',
  set: value => { questionForm.correctAnswers = value ? [value] : [] },
})

async function load() {
  loading.value = true
  try {
    const [questionData, taskData, recordData, certificateData, enterpriseData] = await Promise.all([
      listQuestions(), listTasks(), listRecords(), listCertificates(), listEnterprises(),
    ])
    questions.value = questionData
    tasks.value = taskData
    records.value = recordData
    certificates.value = certificateData
    enterprises.value = enterpriseData.filter(item => item.enabled)
    const groups = await Promise.all(enterprises.value.map(item => listEnterpriseUsers(item.id)))
    enterpriseUsers.value = groups.flat().filter(item => item.enabled)
  } catch { ElMessage.error('培训数据加载失败') }
  finally { loading.value = false }
}

function questionTypeChanged() {
  questionForm.correctAnswers = []
  if (questionForm.type === 'TRUE_FALSE') {
    questionForm.optionA = '正确'
    questionForm.optionB = '错误'
    questionForm.optionC = ''
    questionForm.optionD = ''
  }
}

async function submitQuestion() {
  const options = questionForm.type === 'TRUE_FALSE'
    ? { TRUE: '正确', FALSE: '错误' }
    : Object.fromEntries([
      ['A', questionForm.optionA], ['B', questionForm.optionB],
      ['C', questionForm.optionC], ['D', questionForm.optionD],
    ].filter(([, value]) => value.trim()))
  if (!questionForm.title.trim() || !questionForm.category.trim() || Object.keys(options).length < 2
    || questionForm.correctAnswers.length === 0) {
    ElMessage.warning('请完整填写题目、选项、答案和分类')
    return
  }
  submitting.value = true
  try {
    await createQuestion({
      type: questionForm.type, title: questionForm.title.trim(), options,
      correctAnswers: questionForm.correctAnswers, score: questionForm.score,
      category: questionForm.category.trim(), explanation: questionForm.explanation.trim() || null,
    })
    ElMessage.success('题目已创建')
    questionDialog.value = false
    resetQuestionForm()
    questions.value = await listQuestions()
  } catch { ElMessage.error('题目创建失败，请检查答案与选项') }
  finally { submitting.value = false }
}

async function handleImport(uploadFile: UploadFile) {
  if (!uploadFile.raw) return
  importing.value = true
  try {
    const result = await importQuestions(uploadFile.raw)
    if (result.errors.length) {
      await ElMessageBox.alert(result.errors.map(item => `第 ${item.rowNumber} 行：${item.message}`).join('\n'),
        '导入校验未通过', { confirmButtonText: '知道了' })
      return
    }
    ElMessage.success(`成功导入 ${result.importedRows} 道题目`)
    questions.value = await listQuestions()
  } catch { ElMessage.error('题库导入失败，请确认文件为标准 .xlsx 模板') }
  finally { importing.value = false }
}

async function submitTask() {
  if (!taskForm.title.trim() || taskForm.period.length !== 2 || taskForm.questionIds.length === 0) {
    ElMessage.warning('请填写任务名称、时间范围并选择题目')
    return
  }
  const targetEnterpriseIds = taskForm.targetMode === 'enterprise' ? taskForm.targetEnterpriseIds : []
  const targetUserIds = taskForm.targetMode === 'user' ? taskForm.targetUserIds : []
  if (targetEnterpriseIds.length === 0 && targetUserIds.length === 0) {
    ElMessage.warning('请选择参训企业或指定人员')
    return
  }
  submitting.value = true
  try {
    await createTask({
      title: taskForm.title.trim(), description: taskForm.description.trim() || null,
      startAt: new Date(taskForm.period[0]).toISOString(), endAt: new Date(taskForm.period[1]).toISOString(),
      passScore: taskForm.passScore, maxAttempts: taskForm.maxAttempts,
      questionIds: taskForm.questionIds, targetEnterpriseIds, targetUserIds,
    })
    ElMessage.success('培训任务草稿已创建')
    taskDialog.value = false
    resetTaskForm()
    tasks.value = await listTasks()
  } catch { ElMessage.error('任务创建失败，请检查时间和分值') }
  finally { submitting.value = false }
}

async function publish(item: TrainingTask) {
  await ElMessageBox.confirm('发布后将按当前企业或人员生成参训名单，确认发布吗？', '发布培训任务', {
    type: 'warning', confirmButtonText: '发布', cancelButtonText: '取消',
  })
  try {
    await publishTask(item.id)
    ElMessage.success('培训任务已发布')
    tasks.value = await listTasks()
  } catch { ElMessage.error('发布失败，请确认题目总分和参训账号') }
}

async function openCertificate(item: TrainingCertificate) {
  try {
    const url = await getCertificateObjectUrl(item.id)
    certificateUrls.add(url)
    window.open(url, '_blank', 'noopener,noreferrer')
  } catch { ElMessage.error('证书加载失败') }
}

function resetQuestionForm() {
  Object.assign(questionForm, { type: 'SINGLE_CHOICE', title: '', optionA: '', optionB: '', optionC: '',
    optionD: '', correctAnswers: [], score: 10, category: '', explanation: '' })
}
function resetTaskForm() {
  Object.assign(taskForm, { title: '', description: '', period: [], passScore: 60, maxAttempts: 3,
    questionIds: [], targetMode: 'enterprise', targetEnterpriseIds: [], targetUserIds: [] })
}
function formatTime(value: string | null) { return value ? new Date(value).toLocaleString('zh-CN') : '-' }
function enterpriseName(id: number) { return enterprises.value.find(item => item.id === id)?.name || `企业 #${id}` }

onMounted(load)
onBeforeUnmount(() => certificateUrls.forEach(url => URL.revokeObjectURL(url)))
</script>

<template>
  <section class="work-section training-section">
    <div class="section-toolbar">
      <div><h2>消防培训管理</h2><p>统一维护题库、培训任务、答题记录和培训证书</p></div>
      <el-button :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
    </div>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="题库" name="questions">
        <div class="training-actions">
          <el-button type="primary" :icon="Plus" @click="questionDialog = true">新增题目</el-button>
          <el-upload accept=".xlsx" :auto-upload="false" :show-file-list="false" :on-change="handleImport">
            <el-button :icon="Upload" :loading="importing">导入 Excel</el-button>
          </el-upload>
          <el-button :icon="Download" @click="downloadQuestionImportTemplate">下载模板</el-button>
          <span class="training-count">共 {{ questions.length }} 道</span>
        </div>
        <el-table v-loading="loading" :data="questions" empty-text="暂无题目">
          <el-table-column prop="title" label="题干" min-width="280" show-overflow-tooltip />
          <el-table-column label="题型" width="100"><template #default="scope">{{ typeLabels[scope.row.type as QuestionType] }}</template></el-table-column>
          <el-table-column prop="category" label="分类" width="140" />
          <el-table-column prop="score" label="分值" width="80" />
          <el-table-column label="正确答案" width="130"><template #default="scope">{{ scope.row.correctAnswers.join('、') }}</template></el-table-column>
          <el-table-column label="状态" width="90"><template #default="scope"><el-tag :type="scope.row.enabled ? 'success' : 'info'">{{ scope.row.enabled ? '启用' : '停用' }}</el-tag></template></el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="培训任务" name="tasks">
        <div class="training-actions"><el-button type="primary" :icon="Plus" :disabled="questions.length === 0" @click="taskDialog = true">新建任务</el-button><span class="training-count">共 {{ tasks.length }} 个</span></div>
        <el-table v-loading="loading" :data="tasks" empty-text="暂无培训任务">
          <el-table-column prop="title" label="任务名称" min-width="220" />
          <el-table-column label="有效期" min-width="260"><template #default="scope">{{ formatTime(scope.row.startAt) }} 至 {{ formatTime(scope.row.endAt) }}</template></el-table-column>
          <el-table-column prop="passScore" label="及格分" width="90" />
          <el-table-column prop="maxAttempts" label="次数" width="80" />
          <el-table-column label="参训范围" min-width="180"><template #default="scope"><span v-if="scope.row.targetEnterpriseIds.length">{{ scope.row.targetEnterpriseIds.map(enterpriseName).join('、') }}</span><span v-else>指定 {{ scope.row.targetUserIds.length }} 人</span></template></el-table-column>
          <el-table-column label="状态" width="100"><template #default="scope"><el-tag :type="scope.row.status === 'PUBLISHED' ? 'success' : 'warning'">{{ scope.row.status === 'PUBLISHED' ? '已发布' : '草稿' }}</el-tag></template></el-table-column>
          <el-table-column label="操作" width="110" fixed="right"><template #default="scope"><el-button v-if="scope.row.status === 'DRAFT'" link type="primary" :icon="VideoPlay" @click="publish(scope.row)">发布</el-button><span v-else>-</span></template></el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="答题记录" name="records">
        <div class="record-filters"><el-input v-model="recordKeyword" clearable placeholder="搜索姓名、企业或任务" /><el-select v-model="recordTaskId" clearable placeholder="全部任务"><el-option v-for="item in tasks" :key="item.id" :label="item.title" :value="item.id" /></el-select><span class="training-count">{{ filteredRecords.length }} 条</span></div>
        <el-table v-loading="loading" :data="filteredRecords" empty-text="暂无答题记录">
          <el-table-column prop="taskTitle" label="培训任务" min-width="200" />
          <el-table-column prop="userName" label="参训人员" width="120" />
          <el-table-column prop="enterpriseName" label="所属企业" min-width="180" />
          <el-table-column prop="attemptNo" label="第几次" width="90" />
          <el-table-column prop="score" label="成绩" width="80" />
          <el-table-column label="结果" width="90"><template #default="scope"><el-tag :type="scope.row.passed ? 'success' : 'danger'">{{ scope.row.passed ? '通过' : '未通过' }}</el-tag></template></el-table-column>
          <el-table-column label="提交时间" width="190"><template #default="scope">{{ formatTime(scope.row.submittedAt) }}</template></el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="培训证书" name="certificates">
        <el-table v-loading="loading" :data="certificates" empty-text="暂无培训证书">
          <el-table-column prop="certificateNo" label="证书编号" min-width="220" />
          <el-table-column prop="taskId" label="任务编号" width="110" />
          <el-table-column prop="recordId" label="答题记录" width="110" />
          <el-table-column label="签发时间" width="190"><template #default="scope">{{ formatTime(scope.row.issuedAt) }}</template></el-table-column>
          <el-table-column label="操作" width="110"><template #default="scope"><el-button link type="primary" :icon="Download" @click="openCertificate(scope.row)">查看证书</el-button></template></el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </section>

  <el-dialog v-model="questionDialog" title="新增题目" width="680px" top="4vh" class="training-dialog" @closed="resetQuestionForm">
    <el-form label-position="top" @submit.prevent="submitQuestion">
      <div class="form-grid"><el-form-item label="题型"><el-select v-model="questionForm.type" class="full-width" @change="questionTypeChanged"><el-option label="单选题" value="SINGLE_CHOICE" /><el-option label="多选题" value="MULTIPLE_CHOICE" /><el-option label="判断题" value="TRUE_FALSE" /></el-select></el-form-item><el-form-item label="分类"><el-input v-model="questionForm.category" maxlength="100" /></el-form-item></div>
      <el-form-item label="题干"><el-input v-model="questionForm.title" type="textarea" :rows="3" maxlength="1000" show-word-limit /></el-form-item>
      <template v-if="questionForm.type !== 'TRUE_FALSE'"><div class="form-grid"><el-form-item label="选项 A"><el-input v-model="questionForm.optionA" /></el-form-item><el-form-item label="选项 B"><el-input v-model="questionForm.optionB" /></el-form-item></div><div class="form-grid"><el-form-item label="选项 C"><el-input v-model="questionForm.optionC" /></el-form-item><el-form-item label="选项 D"><el-input v-model="questionForm.optionD" /></el-form-item></div></template>
      <el-form-item label="正确答案"><el-radio-group v-if="questionForm.type === 'SINGLE_CHOICE'" v-model="singleAnswer"><el-radio-button value="A">A</el-radio-button><el-radio-button value="B">B</el-radio-button><el-radio-button v-if="questionForm.optionC" value="C">C</el-radio-button><el-radio-button v-if="questionForm.optionD" value="D">D</el-radio-button></el-radio-group><el-checkbox-group v-else-if="questionForm.type === 'MULTIPLE_CHOICE'" v-model="questionForm.correctAnswers"><el-checkbox-button value="A">A</el-checkbox-button><el-checkbox-button value="B">B</el-checkbox-button><el-checkbox-button v-if="questionForm.optionC" value="C">C</el-checkbox-button><el-checkbox-button v-if="questionForm.optionD" value="D">D</el-checkbox-button></el-checkbox-group><el-radio-group v-else v-model="singleAnswer"><el-radio value="TRUE">正确</el-radio><el-radio value="FALSE">错误</el-radio></el-radio-group></el-form-item>
      <div class="form-grid"><el-form-item label="分值"><el-input-number v-model="questionForm.score" :min="1" :max="1000" /></el-form-item><el-form-item label="答案解析"><el-input v-model="questionForm.explanation" maxlength="2000" /></el-form-item></div>
    </el-form>
    <template #footer><el-button @click="questionDialog = false">取消</el-button><el-button type="primary" :loading="submitting" @click="submitQuestion">创建题目</el-button></template>
  </el-dialog>

  <el-dialog v-model="taskDialog" title="新建培训任务" width="760px" top="4vh" class="training-dialog" @closed="resetTaskForm">
    <el-form label-position="top" @submit.prevent="submitTask">
      <el-form-item label="任务名称"><el-input v-model="taskForm.title" maxlength="200" /></el-form-item>
      <el-form-item label="任务说明"><el-input v-model="taskForm.description" type="textarea" :rows="2" maxlength="2000" /></el-form-item>
      <el-form-item label="培训有效期"><el-date-picker v-model="taskForm.period" type="datetimerange" value-format="YYYY-MM-DDTHH:mm:ss" start-placeholder="开始时间" end-placeholder="结束时间" class="full-width" /></el-form-item>
      <div class="form-grid"><el-form-item label="及格分"><el-input-number v-model="taskForm.passScore" :min="1" :max="1000" /></el-form-item><el-form-item label="最多答题次数"><el-input-number v-model="taskForm.maxAttempts" :min="1" :max="10" /></el-form-item></div>
      <el-form-item label="任务题目"><el-select v-model="taskForm.questionIds" multiple filterable class="full-width" placeholder="选择题目"><el-option v-for="item in questions" :key="item.id" :label="`${item.title}（${item.score} 分）`" :value="item.id" /></el-select></el-form-item>
      <el-form-item label="参训范围"><el-radio-group v-model="taskForm.targetMode"><el-radio-button value="enterprise">按企业</el-radio-button><el-radio-button value="user">指定人员</el-radio-button></el-radio-group></el-form-item>
      <el-form-item v-if="taskForm.targetMode === 'enterprise'" label="参训企业"><el-select v-model="taskForm.targetEnterpriseIds" multiple filterable class="full-width"><el-option v-for="item in enterprises" :key="item.id" :label="item.name" :value="item.id" /></el-select></el-form-item>
      <el-form-item v-else label="参训人员"><el-select v-model="taskForm.targetUserIds" multiple filterable class="full-width"><el-option v-for="item in enterpriseUsers" :key="item.id" :label="`${item.displayName}（${enterpriseName(item.enterpriseId)}）`" :value="item.id" /></el-select></el-form-item>
    </el-form>
    <template #footer><el-button @click="taskDialog = false">取消</el-button><el-button type="primary" :loading="submitting" @click="submitTask">创建草稿</el-button></template>
  </el-dialog>
</template>

<style scoped>
.training-actions, .record-filters { display: flex; align-items: center; gap: 10px; margin: 4px 0 16px; }
.training-count { margin-left: auto; color: #667085; font-size: 13px; }
.record-filters .el-input { width: 280px; }
.record-filters .el-select { width: 220px; }
:deep(.training-dialog .el-dialog__body) { max-height: calc(100vh - 180px); overflow-y: auto; }
@media (max-width: 900px) { .record-filters { align-items: stretch; flex-direction: column; } .record-filters .el-input, .record-filters .el-select { width: 100%; } .training-count { margin-left: 0; } }
</style>
