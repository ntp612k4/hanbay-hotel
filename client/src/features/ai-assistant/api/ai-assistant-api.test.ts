import { beforeEach, describe, expect, it, vi } from 'vitest';

import { tokenStorage } from '../../../lib/storage';
import { streamAiMessage } from './ai-assistant-api';

describe('streamAiMessage', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    vi.spyOn(tokenStorage, 'getAccessToken').mockReturnValue('access-token');
  });

  it('emits deltas from SSE frames split across network chunks', async () => {
    const encoder = new TextEncoder();
    const body = new ReadableStream({
      start(controller) {
        controller.enqueue(encoder.encode('event:delta\ndata:{"type":"delta","content":"Xin '));
        controller.enqueue(
          encoder.encode('chào"}\n\nevent:done\ndata:{"type":"done","content":""}\n\n'),
        );
        controller.close();
      },
    });
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(body, { status: 200 })));
    const deltas: string[] = [];

    await streamAiMessage(
      { conversationId: '11111111-1111-4111-8111-111111111111', message: 'Xin chào' },
      { onDelta: (delta) => deltas.push(delta) },
    );

    expect(deltas).toEqual(['Xin chào']);
    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining('/ai/chat/stream'),
      expect.objectContaining({
        method: 'POST',
        headers: expect.objectContaining({ Authorization: 'Bearer access-token' }),
      }),
    );
  });

  it('rejects an error event returned after streaming has started', async () => {
    const payload = 'data:{"type":"error","content":"AI unavailable"}\n\n';
    const body = new ReadableStream({
      start(controller) {
        controller.enqueue(new TextEncoder().encode(payload));
        controller.close();
      },
    });
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(body, { status: 200 })));

    await expect(
      streamAiMessage(
        { conversationId: '11111111-1111-4111-8111-111111111111', message: 'Test' },
        { onDelta: vi.fn() },
      ),
    ).rejects.toThrow('AI unavailable');
  });

  it('forwards a whitelisted UI action event', async () => {
    const action = {
      type: 'CONTINUE_BOOKING' as const,
      label: 'Tiếp tục đặt phòng',
      params: {
        roomTypeId: '11111111-1111-4111-8111-111111111111',
        checkIn: '2026-09-20',
        checkOut: '2026-09-22',
        adults: 2,
        children: 1,
      },
    };
    const payload = `event:action\ndata:${JSON.stringify({ type: 'action', content: '', action })}\n\nevent:done\ndata:{"type":"done","content":""}\n\n`;
    const body = new ReadableStream({
      start(controller) {
        controller.enqueue(new TextEncoder().encode(payload));
        controller.close();
      },
    });
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(body, { status: 200 })));
    const onAction = vi.fn();

    await streamAiMessage(
      { conversationId: '11111111-1111-4111-8111-111111111111', message: 'Đặt phòng đó' },
      { onDelta: vi.fn(), onAction },
    );

    expect(onAction).toHaveBeenCalledWith(action);
  });

  it('normalizes malformed SSE JSON instead of leaking a SyntaxError', async () => {
    const body = new ReadableStream({
      start(controller) {
        controller.enqueue(new TextEncoder().encode('data:{not-json}\n\n'));
        controller.close();
      },
    });
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(body, { status: 200 })));

    await expect(
      streamAiMessage(
        { conversationId: '11111111-1111-4111-8111-111111111111', message: 'Test' },
        { onDelta: vi.fn() },
      ),
    ).rejects.toThrow('Phản hồi từ trợ lý AI không đúng định dạng');
  });

  it('rejects a stream that closes without a done event', async () => {
    const body = new ReadableStream({
      start(controller) {
        controller.enqueue(new TextEncoder().encode('event: ping\n\n'));
        controller.close();
      },
    });
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(body, { status: 200 })));

    await expect(
      streamAiMessage(
        { conversationId: '11111111-1111-4111-8111-111111111111', message: 'Test' },
        { onDelta: vi.fn() },
      ),
    ).rejects.toThrow('không trả về nội dung');
  });

  it('forwards structured policy sources', async () => {
    const sources = [
      { title: 'Chính sách nhận phòng', category: 'CHECK_IN', section: 'Giờ nhận phòng' },
    ];
    const payload = `data:${JSON.stringify({ type: 'sources', content: '', sources })}\n\ndata:{"type":"done","content":""}\n\n`;
    const body = new ReadableStream({
      start(controller) {
        controller.enqueue(new TextEncoder().encode(payload));
        controller.close();
      },
    });
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(body, { status: 200 })));
    const onSources = vi.fn();

    await streamAiMessage(
      { conversationId: '11111111-1111-4111-8111-111111111111', message: 'Check-in?' },
      { onDelta: vi.fn(), onSources },
    );

    expect(onSources).toHaveBeenCalledWith(sources);
  });
});
