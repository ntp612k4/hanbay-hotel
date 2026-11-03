import { useMutation } from '@tanstack/react-query';

import { streamAiMessage, type StreamAiMessageOptions } from '../api/ai-assistant-api';
import type { AiChatRequest } from '../types/ai-assistant';

interface StreamVariables {
  payload: AiChatRequest;
  options: StreamAiMessageOptions;
}

export function useAiAssistant() {
  return useMutation({
    mutationFn: ({ payload, options }: StreamVariables) => streamAiMessage(payload, options),
  });
}
