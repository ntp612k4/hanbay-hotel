export interface AiChatRequest {
  conversationId: string;
  message: string;
}

export interface AiPolicySource {
  title: string;
  category: string;
  section: string;
}

export type AiUiAction =
  | {
      type: 'CONTINUE_BOOKING';
      label: string;
      params: {
        roomTypeId: string;
        checkIn: string;
        checkOut: string;
        adults: number;
        children: number;
      };
    }
  | {
      type: 'REVIEW_CANCELLATION' | 'SELECT_ADDONS';
      label: string;
      params: { bookingCode: string };
    }
  | {
      type: 'VIEW_PAYMENT';
      label: string;
      params: { paymentId: string };
    };

export interface AiChatMessage {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  action?: AiUiAction;
  sources?: AiPolicySource[];
}
