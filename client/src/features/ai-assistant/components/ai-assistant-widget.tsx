import {
  LoginOutlined,
  PlusOutlined,
  RobotOutlined,
  SendOutlined,
  UserOutlined,
} from '@ant-design/icons';
import { Alert, Avatar, Button, Drawer, FloatButton, Input, Space, Typography } from 'antd';
import { useEffect, useRef, useState } from 'react';

import { useAuth } from '../../authentication/providers/auth-context';
import { ApiClientError } from '../../../types/api';
import { useAiAssistant } from '../hooks/use-ai-assistant';
import type { AiChatMessage, AiUiAction } from '../types/ai-assistant';

import './ai-assistant-widget.css';

const { Text } = Typography;
const { TextArea } = Input;
const CONVERSATION_ID_KEY = 'hanbay.ai.conversation-id';

function getConversationId() {
  const current = window.sessionStorage.getItem(CONVERSATION_ID_KEY);
  if (current) return current;
  const created = crypto.randomUUID();
  window.sessionStorage.setItem(CONVERSATION_ID_KEY, created);
  return created;
}

function actionLocation(action: AiUiAction): string {
  switch (action.type) {
    case 'CONTINUE_BOOKING': {
      const query = new URLSearchParams({
        roomTypeId: action.params.roomTypeId,
        checkIn: action.params.checkIn,
        checkOut: action.params.checkOut,
        adults: String(action.params.adults),
        children: String(action.params.children),
      });
      return `/bookings/new?${query.toString()}`;
    }
    case 'REVIEW_CANCELLATION':
      return `/account/profile?section=bookings&reviewCancellation=${encodeURIComponent(action.params.bookingCode)}`;
    case 'SELECT_ADDONS':
      return `/account/profile?section=bookings&bookingCode=${encodeURIComponent(action.params.bookingCode)}`;
    case 'VIEW_PAYMENT':
      return `/payments/${encodeURIComponent(action.params.paymentId)}/result`;
  }
}

const suggestedQuestions = [
  'Khách sạn check-in lúc mấy giờ?',
  'Tìm phòng cho 2 người trong 3 đêm tới',
  'Cho tôi xem các booking của mình',
  'Các dịch vụ bổ sung đang có là gì?',
];

const initialMessage: AiChatMessage = {
  id: 'welcome',
  role: 'assistant',
  content:
    'Xin chào! Tôi có thể giúp bạn tìm phòng, tính giá, xem booking, thanh toán và kiểm tra điều kiện hủy phòng.',
};

export function AiAssistantWidget({ navigateTo }: { navigateTo: (location: string) => void }) {
  const { isAuthenticated } = useAuth();
  const [open, setOpen] = useState(false);
  const [input, setInput] = useState('');
  const [lastSubmitted, setLastSubmitted] = useState('');
  const [messages, setMessages] = useState<AiChatMessage[]>([initialMessage]);
  const mutation = useAiAssistant();
  const messageEndRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    messageEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, mutation.isPending]);

  const submit = (rawMessage: string) => {
    const message = rawMessage.trim();
    if (!message || mutation.isPending || !isAuthenticated) return;

    const assistantMessageId = crypto.randomUUID();
    setLastSubmitted(message);
    setMessages((current) => [
      ...current,
      { id: crypto.randomUUID(), role: 'user', content: message },
      { id: assistantMessageId, role: 'assistant', content: '' },
    ]);
    setInput('');
    mutation.mutate(
      {
        payload: { conversationId: getConversationId(), message },
        options: {
          onDelta: (content) => {
            setMessages((current) =>
              current.map((item) =>
                item.id === assistantMessageId
                  ? { ...item, content: item.content + content }
                  : item,
              ),
            );
          },
          onAction: (action) => {
            setMessages((current) =>
              current.map((item) => (item.id === assistantMessageId ? { ...item, action } : item)),
            );
          },
          onSources: (sources) => {
            setMessages((current) =>
              current.map((item) => (item.id === assistantMessageId ? { ...item, sources } : item)),
            );
          },
        },
      },
      {
        onError: () => {
          setMessages((current) => current.filter((item) => item.id !== assistantMessageId));
        },
      },
    );
  };

  const startNewConversation = () => {
    window.sessionStorage.setItem(CONVERSATION_ID_KEY, crypto.randomUUID());
    mutation.reset();
    setInput('');
    setMessages([initialMessage]);
  };

  const errorMessage = mutation.error
    ? mutation.error instanceof ApiClientError
      ? mutation.error.message
      : 'Trợ lý AI chưa thể trả lời. Vui lòng thử lại.'
    : null;

  return (
    <>
      <FloatButton
        className="ai-widget__trigger"
        type="primary"
        icon={<RobotOutlined />}
        tooltip="Trợ lý HANBAY AI"
        aria-label="Mở trợ lý HANBAY AI"
        onClick={() => setOpen(true)}
      />
      <Drawer
        className="ai-widget"
        title={
          <Space>
            <Avatar icon={<RobotOutlined />} />
            <span>HANBAY AI Concierge</span>
          </Space>
        }
        placement="right"
        size="large"
        open={open}
        extra={
          isAuthenticated ? (
            <Button
              type="text"
              icon={<PlusOutlined />}
              disabled={mutation.isPending}
              onClick={startNewConversation}
            >
              Chat mới
            </Button>
          ) : null
        }
        destroyOnHidden={false}
        onClose={() => setOpen(false)}
        styles={{ body: { padding: 0 } }}
      >
        {!isAuthenticated ? (
          <div className="ai-widget__login">
            <RobotOutlined />
            <Typography.Title level={4}>Đăng nhập để trò chuyện</Typography.Title>
            <Typography.Paragraph type="secondary">
              Trợ lý cần xác thực để bảo vệ thông tin booking và thanh toán của bạn.
            </Typography.Paragraph>
            <Button type="primary" icon={<LoginOutlined />} href="/login">
              Đăng nhập
            </Button>
          </div>
        ) : (
          <div className="ai-widget__chat">
            <div
              className="ai-widget__messages"
              aria-live="polite"
              aria-label="Nội dung trò chuyện"
            >
              {messages.map((message) => (
                <article className={`ai-message ai-message--${message.role}`} key={message.id}>
                  <Avatar
                    className="ai-message__avatar"
                    icon={message.role === 'assistant' ? <RobotOutlined /> : <UserOutlined />}
                  />
                  <div className="ai-message__content">
                    <div className="ai-message__bubble">{message.content}</div>
                    {message.action && (
                      <Button
                        className="ai-message__action"
                        type="primary"
                        onClick={() => {
                          setOpen(false);
                          navigateTo(actionLocation(message.action!));
                        }}
                      >
                        {message.action.label}
                      </Button>
                    )}
                    {!!message.sources?.length && (
                      <div className="ai-message__sources" aria-label="Nguồn chính sách">
                        <Text type="secondary">Nguồn:</Text>
                        {message.sources.map((source) => (
                          <Text key={`${source.title}-${source.section}`} type="secondary">
                            {source.title} — {source.section}
                          </Text>
                        ))}
                      </div>
                    )}
                  </div>
                </article>
              ))}
              {mutation.isPending && messages.at(-1)?.content === '' && (
                <article className="ai-message ai-message--assistant" aria-label="AI đang xử lý">
                  <Avatar className="ai-message__avatar" icon={<RobotOutlined />} />
                  <div className="ai-message__bubble ai-message__typing">
                    <span />
                    <span />
                    <span />
                  </div>
                </article>
              )}
              <div ref={messageEndRef} />
            </div>

            <div className="ai-widget__composer">
              {errorMessage && (
                <Alert
                  showIcon
                  closable
                  type="error"
                  message={errorMessage}
                  action={
                    lastSubmitted ? (
                      <Button size="small" onClick={() => submit(lastSubmitted)}>
                        Thử lại
                      </Button>
                    ) : undefined
                  }
                  onClose={() => mutation.reset()}
                />
              )}
              {messages.length === 1 && (
                <Space wrap size={[8, 8]}>
                  {suggestedQuestions.map((question) => (
                    <Button key={question} size="small" onClick={() => submit(question)}>
                      {question}
                    </Button>
                  ))}
                </Space>
              )}
              <div className="ai-widget__input-row">
                <TextArea
                  autoSize={{ minRows: 1, maxRows: 4 }}
                  maxLength={2_000}
                  placeholder="Hỏi về chính sách, phòng, giá hoặc booking..."
                  value={input}
                  disabled={mutation.isPending}
                  aria-label="Tin nhắn cho trợ lý AI"
                  onChange={(event) => setInput(event.target.value)}
                  onPressEnter={(event) => {
                    if (!event.shiftKey) {
                      event.preventDefault();
                      submit(input);
                    }
                  }}
                />
                <Button
                  type="primary"
                  icon={<SendOutlined />}
                  loading={mutation.isPending}
                  disabled={!input.trim()}
                  aria-label="Gửi tin nhắn"
                  onClick={() => submit(input)}
                />
              </div>
              <Text type="secondary" className="ai-widget__notice">
                AI có thể mắc lỗi. Hãy kiểm tra lại thông tin quan trọng.
              </Text>
            </div>
          </div>
        )}
      </Drawer>
    </>
  );
}
