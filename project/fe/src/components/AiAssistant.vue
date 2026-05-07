<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useMessage } from 'naive-ui'
import {
  AddOutline,
  ArrowUpOutline,
  ChatbubbleEllipsesOutline,
  CloseOutline,
  ImageOutline,
  SparklesOutline,
  TrashOutline,
} from '@vicons/ionicons5'

type Attachment = {
  id: string
  name: string
  mimeType: string
  dataUrl: string
}

type ChatMessage = {
  id: string
  role: 'user' | 'assistant'
  text: string
  attachments?: Attachment[]
  createdAt: string
}

type ChatReply = {
  conversationId: string
  model: string
  reply: string
  promptTokens?: number | null
  completionTokens?: number | null
  totalTokens?: number | null
}

type RequestAttachment = Omit<Attachment, 'id'>

const STORAGE_KEY = 'waterlevel-ai-assistant'
const SILICONFLOW_API_URL = 'https://api.siliconflow.cn/v1/chat/completions'
const SILICONFLOW_API_KEY = 'sk-zjwnvprndsvebllzydamwitiehfllgiadyspauwnnammzrkb'

const route = useRoute()
const message = useMessage()
const panelOpen = ref(false)
const sending = ref(false)
const draft = ref('')
const conversationId = ref(createId())
const messages = ref<ChatMessage[]>([])
const pendingAttachments = ref<Attachment[]>([])
const scrollRef = ref<HTMLDivElement | null>(null)
const lastReply = ref<ChatReply | null>(null)
const dragActive = ref(false)
const dragDepth = ref(0)

const shouldRender = computed(() => route.path !== '/login')
const hasConversation = computed(() => messages.value.length > 1)

onMounted(() => {
  const saved = restoreState()
  if (saved) {
    conversationId.value = saved.conversationId
    messages.value = saved.messages
    panelOpen.value = saved.panelOpen
  } else {
    resetConversation()
  }
})

watch(
  [messages, panelOpen],
  async () => {
    await nextTick()
    if (panelOpen.value) {
      scrollToBottom()
    }
    persistState()
  },
  { deep: true },
)

watch(panelOpen, () => {
  persistState()
})

const assistantStatus = computed(() => {
  if (sending.value) {
    return '正在回复'
  }
  return lastReply.value?.model ?? 'Qwen/Qwen3.5-4B'
})

function createId() {
  return `${Date.now()}-${Math.random().toString(16).slice(2, 10)}`
}

function createWelcomeMessage(): ChatMessage {
  return {
    id: createId(),
    role: 'assistant',
    text: '你好，我是系统内置的 AI 智能助手。可以帮你解读预测结果、分析页面数据、结合截图定位问题，也支持你直接上传图片继续对话。',
    createdAt: new Date().toISOString(),
  }
}

function resetConversation() {
  conversationId.value = createId()
  messages.value = [createWelcomeMessage()]
  lastReply.value = null
  draft.value = ''
  pendingAttachments.value = []
}

function createNewConversation() {
  resetConversation()
  panelOpen.value = true
  message.success('已进入新的对话')
}

function clearHistory() {
  localStorage.removeItem(STORAGE_KEY)
  resetConversation()
  panelOpen.value = true
  message.success('聊天记录已清除')
}

function restoreState() {
  const raw = localStorage.getItem(STORAGE_KEY)
  if (!raw) {
    return null
  }
  try {
    const parsed = JSON.parse(raw) as {
      conversationId: string
      panelOpen: boolean
      messages: ChatMessage[]
    }
    if (!parsed.messages?.length) {
      return null
    }
    return parsed
  } catch {
    localStorage.removeItem(STORAGE_KEY)
    return null
  }
}

function persistState() {
  if (!shouldRender.value) {
    return
  }
  localStorage.setItem(
    STORAGE_KEY,
    JSON.stringify({
      conversationId: conversationId.value,
      panelOpen: panelOpen.value,
      messages: messages.value,
    }),
  )
}

function scrollToBottom() {
  if (!scrollRef.value) {
    return
  }
  scrollRef.value.scrollTo({
    top: scrollRef.value.scrollHeight,
    behavior: 'smooth',
  })
}

function togglePanel() {
  panelOpen.value = !panelOpen.value
}

async function handleImageChange(event: Event) {
  const input = event.target as HTMLInputElement
  const files = Array.from(input.files ?? [])
  input.value = ''
  if (!files.length) {
    return
  }
  await appendAttachments(files)
}

function readImageFile(file: File) {
  return new Promise<Attachment>((resolve, reject) => {
    if (file.size > 5 * 1024 * 1024) {
      reject(new Error('图片大小不能超过 5MB'))
      return
    }
    const reader = new FileReader()
    reader.onload = () =>
      resolve({
        id: createId(),
        name: file.name,
        mimeType: file.type || 'image/png',
        dataUrl: String(reader.result ?? ''),
      })
    reader.onerror = () => reject(new Error('图片读取失败'))
    reader.readAsDataURL(file)
  })
}

function removeAttachment(id: string) {
  pendingAttachments.value = pendingAttachments.value.filter((item) => item.id !== id)
}

async function appendAttachments(files: File[]) {
  const imageFiles = files.filter((file) => file.type.startsWith('image/'))
  if (!imageFiles.length) {
    message.warning('仅支持上传图片文件')
    return
  }
  if (pendingAttachments.value.length + imageFiles.length > 4) {
    message.warning('单次最多发送 4 张图片')
    return
  }
  try {
    const uploads = await Promise.all(imageFiles.map(readImageFile))
    pendingAttachments.value = [...pendingAttachments.value, ...uploads]
  } catch (error) {
    message.error(error instanceof Error ? error.message : '图片读取失败')
  }
}

function getImageFiles(dataTransfer: DataTransfer | null) {
  if (!dataTransfer) {
    return []
  }
  if (dataTransfer.items?.length) {
    return Array.from(dataTransfer.items)
      .filter((item) => item.kind === 'file' && item.type.startsWith('image/'))
      .map((item) => item.getAsFile())
      .filter((file): file is File => Boolean(file))
  }
  return Array.from(dataTransfer.files).filter((file) => file.type.startsWith('image/'))
}

function handleDragEnter() {
  dragDepth.value += 1
  dragActive.value = true
}

function handleDragOver(event: DragEvent) {
  if (event.dataTransfer) {
    event.dataTransfer.dropEffect = 'copy'
  }
  dragActive.value = true
}

function handleDragLeave() {
  dragDepth.value = Math.max(0, dragDepth.value - 1)
  if (dragDepth.value === 0) {
    dragActive.value = false
  }
}

async function handleDrop(event: DragEvent) {
  dragDepth.value = 0
  dragActive.value = false
  const imageFiles = getImageFiles(event.dataTransfer ?? null)
  if (!imageFiles.length) {
    return
  }
  await appendAttachments(imageFiles)
}

async function handlePaste(event: ClipboardEvent) {
  const imageFiles = getImageFiles(event.clipboardData ?? null)
  if (!imageFiles.length) {
    return
  }
  event.preventDefault()
  await appendAttachments(imageFiles)
}

function formatTime(value: string) {
  return new Intl.DateTimeFormat('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value))
}

async function handleSend() {
  if (sending.value) {
    return
  }
  const text = draft.value.trim()
  if (!text && !pendingAttachments.value.length) {
    return
  }
  const userMessage: ChatMessage = {
    id: createId(),
    role: 'user',
    text,
    attachments: pendingAttachments.value.length ? [...pendingAttachments.value] : undefined,
    createdAt: new Date().toISOString(),
  }
  messages.value = [...messages.value, userMessage]
  draft.value = ''
  pendingAttachments.value = []
  sending.value = true
  panelOpen.value = true
  try {
    const reply = await requestChat({
      conversationId: conversationId.value,
      messages: messages.value.map((item) => ({
        role: item.role,
        text: item.text,
        attachments: item.attachments?.map((attachment) => ({
          name: attachment.name,
          mimeType: attachment.mimeType,
          dataUrl: attachment.dataUrl,
        })),
      })),
    })
    conversationId.value = reply.conversationId
    lastReply.value = reply
  } catch (error: unknown) {
    const errorMessage = error instanceof Error ? error.message : '发送失败'
    appendAssistantMessage(`当前无法连接 AI 服务：${errorMessage}`)
    message.error(errorMessage)
  } finally {
    sending.value = false
  }
}

async function requestChat(payload: {
  conversationId: string
  messages: Array<{
    role: ChatMessage['role']
    text: string
    attachments?: RequestAttachment[]
  }>
}) {
  return await chat(payload)
}

async function chat(payload: {
  conversationId: string
  messages: Array<{
    role: ChatMessage['role']
    text: string
    attachments?: RequestAttachment[]
  }>
}) {
  const response = await fetch(SILICONFLOW_API_URL, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${SILICONFLOW_API_KEY}`,
    },
    body: JSON.stringify({
      model: 'Qwen/Qwen3.5-4B',
      messages: buildSiliconFlowMessages(payload.messages),
    }),
  })
  if (!response.ok) {
    throw new Error(await resolveResponseError(response))
  }
  const result = (await response.json()) as {
    model?: string
    choices?: Array<{
      message?: {
        content?: string
        reasoning_content?: string
      }
    }>
    usage?: {
      prompt_tokens?: number
      completion_tokens?: number
      total_tokens?: number
    }
  }
  const reply = result.choices?.[0]?.message?.content?.trim() ?? ''
  if (!reply) {
    throw new Error('AI 服务未返回正文内容')
  }
  ensureAssistantMessage('', reply)
  return {
    conversationId: payload.conversationId,
    model: result.model ?? 'Qwen/Qwen3.5-4B',
    reply,
    promptTokens: result.usage?.prompt_tokens,
    completionTokens: result.usage?.completion_tokens,
    totalTokens: result.usage?.total_tokens,
  }
}

function ensureAssistantMessage(messageId: string, text: string) {
  if (!messageId) {
    const createdId = createId()
    messages.value = [
      ...messages.value,
      {
        id: createdId,
        role: 'assistant',
        text,
        createdAt: new Date().toISOString(),
      },
    ]
    return createdId
  }
  messages.value = messages.value.map((item) =>
    item.id === messageId
      ? {
          ...item,
          text,
        }
      : item,
  )
  return messageId
}

function appendAssistantMessage(text: string) {
  messages.value = [
    ...messages.value,
    {
      id: createId(),
      role: 'assistant',
      text,
      createdAt: new Date().toISOString(),
    },
  ]
}

async function resolveResponseError(response: Response) {
  const contentType = response.headers.get('content-type') ?? ''
  const raw = await response.text()
  if (contentType.includes('application/json')) {
    try {
      const parsed = JSON.parse(raw) as { message?: string }
      if (parsed.message) {
        return parsed.message
      }
    } catch {
      return raw || '发送失败'
    }
  }
  return raw || '发送失败'
}

function buildSiliconFlowMessages(
  messagesList: Array<{
    role: ChatMessage['role']
    text: string
    attachments?: RequestAttachment[]
  }>,
) {
  return [
    {
      role: 'system',
      content: '你是一个有用的助手',
    },
    ...messagesList.map((item) => ({
      role: item.role,
      content: buildMessageContent(item),
    })),
  ]
}

function buildMessageContent(messageItem: {
  text: string
  attachments?: RequestAttachment[]
}) {
  const text = messageItem.text?.trim() ?? ''
  if (!messageItem.attachments?.length) {
    return text
  }
  const attachmentLines = messageItem.attachments.map(
    (attachment) => `- ${attachment.name} (${attachment.mimeType})`,
  )
  return [text, '用户附带了图片，请结合上下文回答。', '图片列表：', ...attachmentLines]
    .filter(Boolean)
    .join('\n')
}

function handleKeydown(event: KeyboardEvent) {
  if (event.shiftKey) {
    return
  }
  event.preventDefault()
  void handleSend()
}
</script>

<template>
  <div v-if="shouldRender" class="ai-assistant-root">
    <transition name="assistant-panel">
      <section
        v-if="panelOpen"
        class="ai-assistant-panel"
        :class="{ 'is-drag-active': dragActive }"
        @dragenter.prevent="handleDragEnter"
        @dragover.prevent="handleDragOver"
        @dragleave.prevent="handleDragLeave"
        @drop.prevent="handleDrop"
        @paste.capture="handlePaste"
      >
        <header class="ai-assistant-header">
          <div class="ai-assistant-header-main">
            <div class="ai-assistant-mark">
              <n-icon size="20">
                <SparklesOutline />
              </n-icon>
            </div>
            <div>
              <div class="ai-assistant-title">AI 智能助手</div>
              <div class="ai-assistant-subtitle">{{ assistantStatus }}</div>
            </div>
          </div>
          <n-space :size="6">
            <n-button quaternary circle @click="createNewConversation">
              <template #icon>
                <n-icon><AddOutline /></n-icon>
              </template>
            </n-button>
            <n-button quaternary circle @click="clearHistory">
              <template #icon>
                <n-icon><TrashOutline /></n-icon>
              </template>
            </n-button>
            <n-button quaternary circle @click="togglePanel">
              <template #icon>
                <n-icon><CloseOutline /></n-icon>
              </template>
            </n-button>
          </n-space>
        </header>

        <div ref="scrollRef" class="ai-assistant-body">
          <transition-group name="assistant-message">
            <article
              v-for="item in messages"
              :key="item.id"
              class="ai-message"
              :class="item.role === 'user' ? 'is-user' : 'is-assistant'"
            >
              <div class="ai-message-bubble">
                <div v-if="item.attachments?.length" class="ai-message-images">
                  <img
                    v-for="attachment in item.attachments"
                    :key="attachment.id"
                    :src="attachment.dataUrl"
                    :alt="attachment.name"
                  />
                </div>
                <div v-if="item.text" class="ai-message-text">
                  {{ item.text }}
                </div>
              </div>
              <time class="ai-message-time">{{ formatTime(item.createdAt) }}</time>
            </article>
          </transition-group>
          <div v-if="sending" class="ai-assistant-thinking">
            <span></span>
            <span></span>
            <span></span>
          </div>
        </div>

        <footer class="ai-assistant-footer">
          <div v-if="pendingAttachments.length" class="ai-upload-list">
            <div
              v-for="attachment in pendingAttachments"
              :key="attachment.id"
              class="ai-upload-item"
            >
              <img :src="attachment.dataUrl" :alt="attachment.name" />
              <button type="button" @click="removeAttachment(attachment.id)">×</button>
            </div>
          </div>

          <n-input
            v-model:value="draft"
            type="textarea"
            placeholder="输入问题，或上传截图后继续提问"
            :autosize="{ minRows: 2, maxRows: 5 }"
            class="ai-assistant-input"
            @keydown.enter="handleKeydown"
          />

          <div class="ai-assistant-actions">
            <div class="ai-assistant-tools">
              <label class="ai-upload-trigger">
                <input type="file" accept="image/*" multiple @change="handleImageChange" />
                <n-icon size="18">
                  <ImageOutline />
                </n-icon>
                <span>发送图片</span>
              </label>
            </div>
            <n-space align="center" :size="10">
              <span v-if="hasConversation" class="ai-token-text">
                {{ lastReply?.totalTokens ? `Tokens ${lastReply.totalTokens}` : '多轮对话中' }}
              </span>
              <n-button
                type="primary"
                round
                :disabled="sending"
                class="ai-send-button"
                @click="handleSend"
              >
                <template #icon>
                  <n-icon><ArrowUpOutline /></n-icon>
                </template>
                发送
              </n-button>
            </n-space>
          </div>
        </footer>
        <div v-if="dragActive" class="ai-drop-overlay">
          <div class="ai-drop-overlay-title">松开即可添加图片</div>
          <div class="ai-drop-overlay-text">也支持直接粘贴截图或从本地上传图片</div>
        </div>
      </section>
    </transition>

    <button class="ai-assistant-launcher" :class="{ active: panelOpen }" @click="togglePanel">
      <span class="ai-assistant-launcher-ring"></span>
      <span class="ai-assistant-launcher-core">
        <n-icon size="24">
          <ChatbubbleEllipsesOutline />
        </n-icon>
      </span>
    </button>
  </div>
</template>

<style scoped>
.ai-assistant-root {
  position: fixed;
  right: 28px;
  bottom: 28px;
  z-index: 80;
}

.ai-assistant-panel {
  position: relative;
  width: min(388px, calc(100vw - 32px));
  height: min(680px, calc(100vh - 112px));
  margin-bottom: 18px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border-radius: 28px;
  background:
    linear-gradient(180deg, rgba(9, 18, 31, 0.96), rgba(10, 22, 39, 0.92)),
    rgba(8, 18, 31, 0.9);
  border: 1px solid rgba(117, 164, 230, 0.16);
  box-shadow:
    0 32px 100px rgba(5, 12, 22, 0.36),
    inset 0 1px 0 rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(24px);
}

.ai-assistant-panel.is-drag-active {
  border-color: rgba(93, 178, 255, 0.48);
  box-shadow:
    0 32px 100px rgba(5, 12, 22, 0.36),
    0 0 0 1px rgba(93, 178, 255, 0.22),
    inset 0 1px 0 rgba(255, 255, 255, 0.08);
}

.ai-assistant-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 18px 18px 14px;
  border-bottom: 1px solid rgba(117, 164, 230, 0.1);
}

.ai-assistant-header-main {
  display: flex;
  align-items: center;
  gap: 12px;
}

.ai-assistant-mark {
  width: 40px;
  height: 40px;
  border-radius: 14px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #eef6ff;
  background: linear-gradient(135deg, rgba(31, 118, 255, 0.92), rgba(58, 201, 255, 0.82));
  box-shadow: 0 12px 28px rgba(42, 121, 246, 0.28);
}

.ai-assistant-title {
  color: #f4f8ff;
  font-size: 16px;
  font-weight: 600;
}

.ai-assistant-subtitle {
  margin-top: 4px;
  color: rgba(198, 216, 241, 0.66);
  font-size: 12px;
}

.ai-assistant-body {
  flex: 1;
  overflow: auto;
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.ai-message {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.ai-message.is-user {
  align-items: flex-end;
}

.ai-message.is-assistant {
  align-items: flex-start;
}

.ai-message-bubble {
  max-width: 88%;
  padding: 14px 16px;
  border-radius: 20px;
  line-height: 1.7;
  font-size: 14px;
  white-space: pre-wrap;
}

.is-assistant .ai-message-bubble {
  color: #e6eefb;
  background: rgba(22, 40, 66, 0.9);
  border: 1px solid rgba(115, 160, 223, 0.1);
}

.is-user .ai-message-bubble {
  color: #0d1b2b;
  background: linear-gradient(135deg, #e9f4ff, #c7e5ff);
  box-shadow: 0 12px 28px rgba(39, 113, 205, 0.18);
}

.ai-message-images {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  margin-bottom: 10px;
}

.ai-message-images img {
  width: 100%;
  height: 104px;
  object-fit: cover;
  border-radius: 14px;
}

.ai-message-time {
  padding: 0 4px;
  color: rgba(191, 210, 236, 0.46);
  font-size: 11px;
}

.ai-assistant-thinking {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 12px 14px;
  border-radius: 18px;
  width: fit-content;
  background: rgba(22, 40, 66, 0.9);
  border: 1px solid rgba(115, 160, 223, 0.1);
}

.ai-assistant-thinking span {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #88c4ff;
  animation: assistant-thinking 1.1s infinite ease-in-out;
}

.ai-assistant-thinking span:nth-child(2) {
  animation-delay: 0.14s;
}

.ai-assistant-thinking span:nth-child(3) {
  animation-delay: 0.28s;
}

.ai-assistant-footer {
  padding: 14px 16px 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  border-top: 1px solid rgba(117, 164, 230, 0.1);
  background: rgba(8, 18, 31, 0.78);
}

.ai-upload-list {
  display: flex;
  gap: 10px;
  overflow-x: auto;
}

.ai-upload-item {
  position: relative;
  flex: 0 0 72px;
  width: 72px;
  height: 72px;
  border-radius: 18px;
  overflow: hidden;
}

.ai-upload-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.ai-upload-item button {
  position: absolute;
  top: 6px;
  right: 6px;
  width: 22px;
  height: 22px;
  border: none;
  border-radius: 50%;
  color: #fff;
  background: rgba(8, 18, 31, 0.7);
  cursor: pointer;
}

.ai-assistant-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.ai-assistant-tools {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.ai-upload-trigger {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  border-radius: 14px;
  color: #dce8fa;
  background: rgba(22, 40, 66, 0.82);
  border: 1px solid rgba(115, 160, 223, 0.12);
  cursor: pointer;
  transition:
    transform 0.5s cubic-bezier(0.22, 1, 0.36, 1),
    border-color 0.4s ease,
    background 0.4s ease;
}

.ai-upload-trigger:hover {
  transform: translateY(-1px);
  background: rgba(28, 51, 82, 0.92);
  border-color: rgba(140, 186, 245, 0.24);
}

.ai-upload-trigger input {
  display: none;
}

.ai-token-text {
  color: rgba(194, 212, 236, 0.62);
  font-size: 12px;
}

.ai-send-button {
  min-width: 94px;
}

.ai-drop-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  background: rgba(5, 14, 26, 0.76);
  backdrop-filter: blur(14px);
  color: #eef6ff;
  text-align: center;
  z-index: 2;
}

.ai-drop-overlay-title {
  font-size: 18px;
  font-weight: 600;
}

.ai-drop-overlay-text {
  max-width: 240px;
  color: rgba(211, 226, 245, 0.74);
  font-size: 13px;
  line-height: 1.6;
}

.ai-assistant-launcher {
  position: relative;
  width: 64px;
  height: 64px;
  padding: 0;
  border: none;
  background: transparent;
  cursor: pointer;
}

.ai-assistant-launcher-ring,
.ai-assistant-launcher-core {
  position: absolute;
  inset: 0;
  border-radius: 50%;
}

.ai-assistant-launcher-ring {
  background:
    conic-gradient(from 120deg, rgba(63, 147, 255, 0.15), rgba(57, 212, 255, 0.9), rgba(63, 147, 255, 0.15));
  filter: blur(2px);
  animation: assistant-orbit 3.4s linear infinite;
}

.ai-assistant-launcher-core {
  inset: 5px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #eff6ff;
  background: linear-gradient(135deg, #0f4bd6, #1ec0ff);
  box-shadow:
    0 20px 36px rgba(16, 74, 177, 0.3),
    inset 0 1px 0 rgba(255, 255, 255, 0.3);
  transition:
    transform 0.65s cubic-bezier(0.22, 1, 0.36, 1),
    box-shadow 0.5s ease;
}

.ai-assistant-launcher:hover .ai-assistant-launcher-core,
.ai-assistant-launcher.active .ai-assistant-launcher-core {
  transform: scale(1.06);
  box-shadow:
    0 26px 44px rgba(16, 74, 177, 0.38),
    inset 0 1px 0 rgba(255, 255, 255, 0.34);
}

.assistant-panel-enter-active,
.assistant-panel-leave-active {
  transition:
    opacity 0.42s cubic-bezier(0.22, 1, 0.36, 1),
    transform 0.56s cubic-bezier(0.22, 1, 0.36, 1);
  transform-origin: bottom right;
}

.assistant-panel-enter-from,
.assistant-panel-leave-to {
  opacity: 0;
  transform: translateY(18px) scale(0.88);
}

.assistant-message-enter-active,
.assistant-message-leave-active {
  transition:
    opacity 0.38s cubic-bezier(0.22, 1, 0.36, 1),
    transform 0.48s cubic-bezier(0.22, 1, 0.36, 1);
}

.assistant-message-enter-from,
.assistant-message-leave-to {
  opacity: 0;
  transform: translateY(12px) scale(0.98);
}

@keyframes assistant-thinking {
  0%,
  80%,
  100% {
    transform: translateY(0);
    opacity: 0.45;
  }
  40% {
    transform: translateY(-4px);
    opacity: 1;
  }
}

@keyframes assistant-orbit {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
</style>
