import { env } from '../../../config/env';
import { tokenStorage } from '../../../lib/storage';
import { ApiClientError } from '../../../types/api';
import type { AiChatRequest, AiPolicySource, AiUiAction } from '../types/ai-assistant';

interface AiChatStreamEvent {
  type: 'delta' | 'action' | 'sources' | 'done' | 'error';
  content: string;
  action?: AiUiAction;
  sources?: AiPolicySource[];
}

export interface StreamAiMessageOptions {
  signal?: AbortSignal;
  onDelta: (content: string) => void;
  onAction?: (action: AiUiAction) => void;
  onSources?: (sources: AiPolicySource[]) => void;
}

export async function streamAiMessage(
  payload: AiChatRequest,
  options: StreamAiMessageOptions,
): Promise<void> {
  const timeoutController = new AbortController();
  const timeout = globalThis.setTimeout(() => timeoutController.abort(), 30_000);
  const abort = () => timeoutController.abort();
  options.signal?.addEventListener('abort', abort, { once: true });
  try {
    await streamAiMessageOnce(payload, { ...options, signal: timeoutController.signal });
  } catch (error) {
    if (timeoutController.signal.aborted && !options.signal?.aborted) {
      throw new ApiClientError({
        message: 'Trợ lý AI phản hồi quá thời gian. Vui lòng thử lại.',
        kind: 'network',
      });
    }
    throw error;
  } finally {
    globalThis.clearTimeout(timeout);
    options.signal?.removeEventListener('abort', abort);
  }
}

async function streamAiMessageOnce(
  payload: AiChatRequest,
  options: StreamAiMessageOptions,
): Promise<void> {
  const accessToken = tokenStorage.getAccessToken();
  const response = await fetch(`${env.apiBaseUrl}/ai/chat/stream`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'text/event-stream',
      ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
    },
    body: JSON.stringify(payload),
    signal: options.signal,
  });

  if (!response.ok || !response.body) {
    throw new ApiClientError({
      message:
        response.status === 401
          ? 'Phiên đăng nhập đã hết hạn.'
          : 'Không thể kết nối đến trợ lý AI.',
      kind: response.status ? 'api' : 'network',
      status: response.status,
    });
  }

  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  let buffer = '';
  let completed = false;
  let receivedPayload = false;

  while (true) {
    const { done, value } = await reader.read();
    buffer += decoder.decode(value, { stream: !done });
    const frames = buffer.split(/\r?\n\r?\n/);
    buffer = frames.pop() ?? '';
    frames.forEach((frame) => {
      const type = processSseFrame(frame, options);
      if (type) receivedPayload = true;
      if (type === 'done') completed = true;
    });

    if (done) {
      if (buffer.trim()) {
        const type = processSseFrame(buffer, options);
        if (type) receivedPayload = true;
        if (type === 'done') completed = true;
      }
      break;
    }
  }
  if (!completed) {
    throw new ApiClientError({
      message: receivedPayload
        ? 'Kết nối với trợ lý AI bị gián đoạn. Vui lòng thử lại.'
        : 'Trợ lý AI không trả về nội dung. Vui lòng thử lại.',
      kind: 'api',
    });
  }
}

function processSseFrame(frame: string, options: StreamAiMessageOptions) {
  const data = frame
    .split(/\r?\n/)
    .filter((line) => line.startsWith('data:'))
    .map((line) => line.slice(5).trimStart())
    .join('\n');
  if (!data) return undefined;

  let event: AiChatStreamEvent;
  try {
    event = JSON.parse(data) as AiChatStreamEvent;
  } catch {
    throw new ApiClientError({
      message: 'Phản hồi từ trợ lý AI không đúng định dạng. Vui lòng thử lại.',
      kind: 'api',
    });
  }
  if (!['delta', 'action', 'sources', 'done', 'error'].includes(event.type)) {
    throw new ApiClientError({ message: 'Phản hồi từ trợ lý AI không hợp lệ.', kind: 'api' });
  }
  if (typeof event.content !== 'string') {
    throw new ApiClientError({ message: 'Phản hồi từ trợ lý AI không hợp lệ.', kind: 'api' });
  }
  if (event.type === 'delta') options.onDelta(event.content);
  if (event.type === 'action' && event.action) options.onAction?.(event.action);
  if (event.type === 'sources' && Array.isArray(event.sources)) options.onSources?.(event.sources);
  if (event.type === 'error') {
    throw new ApiClientError({ message: event.content, kind: 'api' });
  }
  return event.type;
}
