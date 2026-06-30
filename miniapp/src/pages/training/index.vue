<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import {
  downloadTrainingCertificate,
  getTrainingPaper,
  listAssignedTasks,
  listTrainingCertificates,
  submitTrainingAnswers,
  type AssignedTrainingTask,
  type AttemptResult,
  type PaperQuestion,
  type TrainingCertificate,
  type TrainingPaper,
} from '../../api/training'
import { useSessionStore } from '../../stores/session'

const session = useSessionStore()
const activeTab = ref<'tasks' | 'certificates'>('tasks')
const loading = ref(false)
const submitting = ref(false)
const tasks = ref<AssignedTrainingTask[]>([])
const certificates = ref<TrainingCertificate[]>([])
const paper = ref<TrainingPaper | null>(null)
const result = ref<AttemptResult | null>(null)
const answers = ref<Record<number, string[]>>({})

async function load() {
  loading.value = true
  try {
    const [taskData, certificateData] = await Promise.all([listAssignedTasks(), listTrainingCertificates()])
    tasks.value = taskData
    certificates.value = certificateData
  } catch (error) { showError(error, '培训数据加载失败') }
  finally { loading.value = false }
}

async function start(item: AssignedTrainingTask) {
  if (item.passed) {
    uni.showToast({ title: '该任务已通过', icon: 'none' })
    return
  }
  loading.value = true
  try {
    paper.value = await getTrainingPaper(item.task.id)
    answers.value = Object.fromEntries(paper.value.questions.map(question => [question.id, []]))
    result.value = null
  } catch (error) { showError(error, '试卷加载失败') }
  finally { loading.value = false }
}

function setSingleAnswer(questionId: number, value: string) {
  answers.value[questionId] = [value]
}

function setMultipleAnswers(questionId: number, values: string[]) {
  answers.value[questionId] = values
}

async function submit() {
  if (!paper.value) return
  const unanswered = paper.value.questions.some(question => !answers.value[question.id]?.length)
  if (unanswered) {
    uni.showToast({ title: '请完成全部题目后提交', icon: 'none' })
    return
  }
  const confirmation = await uni.showModal({
    title: '提交答卷', content: '提交后将计入一次答题次数，确认提交吗？',
  })
  if (!confirmation.confirm) return
  submitting.value = true
  try {
    result.value = await submitTrainingAnswers(paper.value.task.id, answers.value)
    uni.showToast({ title: result.value.passed ? '培训已通过' : '本次未通过', icon: 'none' })
    await load()
  } catch (error) { showError(error, '答卷提交失败') }
  finally { submitting.value = false }
}

async function openCertificate(item: TrainingCertificate) {
  uni.showLoading({ title: '加载证书' })
  try {
    const filePath = await downloadTrainingCertificate(item.id)
    await uni.openDocument({ filePath, fileType: 'pdf', showMenu: true })
  } catch (error) { showError(error, '证书打开失败') }
  finally { uni.hideLoading() }
}

function closePaper() {
  paper.value = null
  result.value = null
  answers.value = {}
}
function optionEntries(question: PaperQuestion) { return Object.entries(question.options) }
function answerText(values: string[]) { return values.join('、') || '-' }
function formatTime(value: string) { return new Date(value).toLocaleString('zh-CN') }
function showError(error: unknown, fallback: string) {
  uni.showToast({ title: error instanceof Error ? error.message : fallback, icon: 'none' })
}

onShow(() => {
  session.restore()
  if (!session.authenticated) { uni.navigateTo({ url: '/pages/login/index' }); return }
  load()
})
</script>

<template>
  <view class="page training-page">
    <view v-if="paper" class="training-paper">
      <view class="detail-heading"><text class="section-title">{{ paper.task.title }}</text><text class="detail-close" @click="closePaper">退出答题</text></view>
      <view class="paper-summary"><text>及格分 {{ paper.task.passScore }}</text><text>已答 {{ paper.attemptsUsed }}/{{ paper.task.maxAttempts }} 次</text></view>

      <view v-if="result" class="result-panel">
        <text class="result-label">第 {{ result.attemptNo }} 次成绩</text>
        <text class="result-score">{{ result.score }} 分</text>
        <text :class="['result-status', { passed: result.passed }]">{{ result.passed ? '培训通过' : '未达到及格分' }}</text>
      </view>

      <view v-for="(question, index) in paper.questions" :key="question.id" class="question-block">
        <view class="question-heading"><text>{{ index + 1 }}. {{ question.title }}</text><text>{{ question.score }} 分</text></view>
        <radio-group v-if="question.type !== 'MULTIPLE_CHOICE'" @change="setSingleAnswer(question.id, $event.detail.value)">
          <label v-for="([key, label]) in optionEntries(question)" :key="key" class="answer-option">
            <radio :value="key" :checked="answers[question.id]?.includes(key)" :disabled="Boolean(result)" color="#d92d20" />
            <text><text class="option-key">{{ key === 'TRUE' ? '对' : key === 'FALSE' ? '错' : key }}</text>{{ label }}</text>
          </label>
        </radio-group>
        <checkbox-group v-else @change="setMultipleAnswers(question.id, $event.detail.value)">
          <label v-for="([key, label]) in optionEntries(question)" :key="key" class="answer-option">
            <checkbox :value="key" :checked="answers[question.id]?.includes(key)" :disabled="Boolean(result)" color="#d92d20" />
            <text><text class="option-key">{{ key }}</text>{{ label }}</text>
          </label>
        </checkbox-group>
        <view v-if="result" :class="['answer-review', { correct: result.details.find(item => item.questionId === question.id)?.correct }]">
          <text>正确答案：{{ answerText(result.details.find(item => item.questionId === question.id)?.correctAnswers || []) }}</text>
          <text v-if="result.details.find(item => item.questionId === question.id)?.explanation" class="review-explanation">{{ result.details.find(item => item.questionId === question.id)?.explanation }}</text>
        </view>
      </view>
      <button v-if="!result" class="primary-button" :loading="submitting" :disabled="submitting" @click="submit">提交答卷</button>
      <button v-else class="secondary-button" @click="closePaper">返回任务列表</button>
    </view>

    <template v-else>
      <view class="training-tabs">
        <view :class="['training-tab', { active: activeTab === 'tasks' }]" @click="activeTab = 'tasks'">培训任务</view>
        <view :class="['training-tab', { active: activeTab === 'certificates' }]" @click="activeTab = 'certificates'">我的证书</view>
      </view>

      <template v-if="activeTab === 'tasks'">
        <view class="list-header"><text class="section-title">待办与记录</text><text class="muted">{{ loading ? '加载中' : `${tasks.length} 个任务` }}</text></view>
        <view v-if="!loading && tasks.length === 0" class="empty-state">暂无培训任务</view>
        <view v-for="item in tasks" :key="item.task.id" class="training-card">
          <view class="training-card-head"><text class="training-title">{{ item.task.title }}</text><text :class="['training-state', { passed: item.passed }]">{{ item.passed ? '已通过' : '待完成' }}</text></view>
          <text v-if="item.task.description" class="training-desc">{{ item.task.description }}</text>
          <view class="training-meta"><text>及格分 {{ item.task.passScore }}</text><text>最好成绩 {{ item.bestScore }}</text><text>已答 {{ item.attemptsUsed }}/{{ item.task.maxAttempts }} 次</text></view>
          <text class="training-time">截止 {{ formatTime(item.task.endAt) }}</text>
          <button class="card-action" :disabled="item.passed || item.attemptsUsed >= item.task.maxAttempts" @click="start(item)">{{ item.passed ? '已完成' : item.attemptsUsed ? '继续答题' : '开始答题' }}</button>
        </view>
      </template>

      <template v-else>
        <view class="list-header"><text class="section-title">培训证书</text><text class="muted">{{ certificates.length }} 份</text></view>
        <view v-if="!loading && certificates.length === 0" class="empty-state">通过培训后自动生成证书</view>
        <view v-for="item in certificates" :key="item.id" class="certificate-row" @click="openCertificate(item)">
          <view><text class="certificate-no">{{ item.certificateNo }}</text><text class="muted">签发于 {{ formatTime(item.issuedAt) }}</text></view><text class="certificate-open">查看</text>
        </view>
      </template>
    </template>
  </view>
</template>

<style scoped>
.training-tabs { display: grid; grid-template-columns: 1fr 1fr; margin-bottom: 28rpx; border-bottom: 1rpx solid #d8e0ea; }
.training-tab { padding: 24rpx 8rpx; color: #667085; text-align: center; }
.training-tab.active { color: #b42318; border-bottom: 4rpx solid #d92d20; font-weight: 600; }
.training-card { margin-bottom: 20rpx; padding: 28rpx; background: #fff; border: 1rpx solid #d8e0ea; border-radius: 8rpx; }
.training-card-head, .question-heading, .paper-summary { display: flex; align-items: flex-start; justify-content: space-between; gap: 20rpx; }
.training-title { color: #1f2933; font-size: 32rpx; font-weight: 600; }
.training-state { flex: 0 0 auto; padding: 6rpx 12rpx; color: #b54708; background: #fffaeb; font-size: 24rpx; }
.training-state.passed { color: #027a48; background: #ecfdf3; }
.training-desc { display: block; margin-top: 14rpx; color: #475467; line-height: 1.6; }
.training-meta { display: flex; flex-wrap: wrap; gap: 18rpx; margin-top: 22rpx; color: #475467; font-size: 25rpx; }
.training-time { display: block; margin-top: 14rpx; color: #667085; font-size: 24rpx; }
.card-action { width: 100%; height: 76rpx; margin-top: 24rpx; color: #b42318; background: #fff; border: 1rpx solid #d92d20; border-radius: 6rpx; font-size: 28rpx; line-height: 74rpx; }
.card-action[disabled] { color: #98a2b3; border-color: #d0d5dd; }
.training-paper { padding-bottom: 40rpx; }
.paper-summary { margin: 20rpx 0 28rpx; color: #667085; font-size: 25rpx; }
.question-block { margin-bottom: 20rpx; padding: 28rpx; background: #fff; border: 1rpx solid #d8e0ea; border-radius: 8rpx; }
.question-heading > text:first-child { color: #1f2933; font-weight: 600; line-height: 1.6; }
.question-heading > text:last-child { flex: 0 0 auto; color: #667085; font-size: 24rpx; }
.answer-option { display: flex; align-items: flex-start; gap: 12rpx; padding: 22rpx 0; border-bottom: 1rpx solid #eef2f6; line-height: 1.5; }
.answer-option:last-child { border-bottom: 0; }
.option-key { display: inline-block; min-width: 46rpx; color: #b42318; font-weight: 600; }
.answer-review { margin-top: 18rpx; padding: 18rpx; color: #b42318; background: #fff4ed; }
.answer-review.correct { color: #027a48; background: #ecfdf3; }
.review-explanation { display: block; margin-top: 10rpx; line-height: 1.5; }
.result-panel { display: flex; align-items: center; gap: 18rpx; margin-bottom: 24rpx; padding: 24rpx; background: #fff; border-left: 6rpx solid #d92d20; }
.result-label { color: #667085; }.result-score { font-size: 40rpx; font-weight: 700; }.result-status { margin-left: auto; color: #b42318; }.result-status.passed { color: #027a48; }
.secondary-button { height: 88rpx; color: #344054; background: #fff; border: 1rpx solid #d0d5dd; border-radius: 6rpx; font-size: 30rpx; line-height: 86rpx; }
.certificate-row { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16rpx; padding: 26rpx; background: #fff; border: 1rpx solid #d8e0ea; border-radius: 8rpx; }
.certificate-no, .certificate-row .muted { display: block; }.certificate-no { margin-bottom: 8rpx; font-weight: 600; }.certificate-open { color: #b42318; }
</style>
