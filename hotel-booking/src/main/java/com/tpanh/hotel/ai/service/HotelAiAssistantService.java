package com.tpanh.hotel.ai.service;

import com.tpanh.hotel.ai.dto.AiChatStreamEvent;
import com.tpanh.hotel.ai.prompt.HotelAiPrompt;
import com.tpanh.hotel.ai.rag.PolicyRetriever;
import io.micrometer.context.ContextSnapshot;
import io.micrometer.context.ContextSnapshotFactory;
import java.util.List;
import java.util.UUID;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class HotelAiAssistantService {

    private static final ContextSnapshotFactory CONTEXT_SNAPSHOT_FACTORY =
            ContextSnapshotFactory.builder().build();

    private final ChatClient chatClient;
    private final HotelAiTools hotelAiTools;
    private final AiRequestLogger requestLogger;
    private final AiConversationStore conversationStore;
    private final AiUiActionParser actionParser;
    private final AiTimeContext timeContext;
    private final AiDateNormalizer dateNormalizer;
    private final PolicyRetriever policyRetriever;

    @Autowired
    public HotelAiAssistantService(
            ObjectProvider<ChatModel> chatModelProvider,
            HotelAiTools hotelAiTools,
            AiRequestLogger requestLogger,
            AiConversationStore conversationStore,
            AiUiActionParser actionParser,
            AiTimeContext timeContext,
            AiDateNormalizer dateNormalizer,
            ObjectProvider<PolicyRetriever> policyRetrieverProvider) {
        ChatModel chatModel = chatModelProvider.getIfAvailable();
        this.chatClient = chatModel == null
                ? null
                : ChatClient.builder(chatModel)
                        .defaultSystem(HotelAiPrompt.SYSTEM_PROMPT)
                        .build();
        this.hotelAiTools = hotelAiTools;
        this.requestLogger = requestLogger;
        this.conversationStore = conversationStore;
        this.actionParser = actionParser;
        this.timeContext = timeContext;
        this.dateNormalizer = dateNormalizer;
        this.policyRetriever = policyRetrieverProvider.getIfAvailable();
    }

    HotelAiAssistantService(
            ChatClient.Builder chatClientBuilder,
            HotelAiTools hotelAiTools,
            AiRequestLogger requestLogger,
            AiConversationStore conversationStore,
            AiUiActionParser actionParser,
            AiTimeContext timeContext,
            AiDateNormalizer dateNormalizer,
            PolicyRetriever policyRetriever) {
        this.chatClient = chatClientBuilder == null
                ? null
                : chatClientBuilder.defaultSystem(HotelAiPrompt.SYSTEM_PROMPT).build();
        this.hotelAiTools = hotelAiTools;
        this.requestLogger = requestLogger;
        this.conversationStore = conversationStore;
        this.actionParser = actionParser;
        this.timeContext = timeContext;
        this.dateNormalizer = dateNormalizer;
        this.policyRetriever = policyRetriever;
    }

    public Flux<AiChatStreamEvent> stream(UUID conversationId, String message) {
        AiRequestLogger.RequestContext context = requestLogger.start();
        UUID userId = conversationStore.requireCurrentUserId();
        ChatClient availableChatClient = requireChatClient();
        ContextSnapshot requestContextSnapshot = CONTEXT_SNAPSHOT_FACTORY.captureAll();
        List<AiConversationStore.Message> history = conversationStore.get(userId, conversationId);
        String contextualMessage = contextualMessage(history, message);
        HotelPolicySearchTool policySearchTool = new HotelPolicySearchTool(policyRetriever);
        return availableChatClient.prompt().user(contextualMessage).tools(hotelAiTools, policySearchTool).stream()
                .content()
                .collectList()
                .flatMapMany(
                        chunks -> completeStream(userId, conversationId, message, chunks, policySearchTool, context))
                .contextWrite(requestContextSnapshot::updateContext)
                .doOnError(exception -> requestLogger.failure(message, exception, context));
    }

    private ChatClient requireChatClient() {
        if (chatClient == null) {
            throw new IllegalStateException("AI chat provider is not configured");
        }
        return chatClient;
    }

    private Flux<AiChatStreamEvent> completeStream(
            UUID userId,
            UUID conversationId,
            String message,
            List<String> chunks,
            HotelPolicySearchTool policySearchTool,
            AiRequestLogger.RequestContext requestContext) {
        AiUiActionParser.ParsedResponse parsed = policySearchTool.searchedWithoutEvidence()
                ? new AiUiActionParser.ParsedResponse(
                        "Xin lỗi, tôi chưa tìm thấy thông tin này trong chính sách khách sạn.",
                        java.util.Optional.empty())
                : actionParser.parse(String.join("", chunks));
        conversationStore.append(userId, conversationId, message, parsed.answer());
        requestLogger.success(message, parsed.answer(), requestContext);

        Flux<AiChatStreamEvent> answer =
                parsed.answer().isBlank() ? Flux.empty() : Flux.just(AiChatStreamEvent.delta(parsed.answer()));
        Flux<AiChatStreamEvent> action =
                parsed.action().map(AiChatStreamEvent::action).map(Flux::just).orElseGet(Flux::empty);
        Flux<AiChatStreamEvent> sources = policySearchTool.sources().isEmpty()
                ? Flux.empty()
                : Flux.just(AiChatStreamEvent.sources(policySearchTool.sources()));
        return answer.concatWith(action).concatWith(sources).concatWithValues(AiChatStreamEvent.done());
    }

    private String contextualMessage(List<AiConversationStore.Message> history, String message) {
        StringBuilder prompt = new StringBuilder(timeContext.current())
                .append(" Dùng mốc này để xác định hôm nay, ngày đã qua và ngày tương lai. ")
                .append("Nếu người dùng nhập ngày/tháng không có năm, ưu tiên lần xuất hiện gần nhất chưa qua.\n");
        dateNormalizer.contextFor(message).ifPresent(value -> prompt.append(value)
                .append('\n'));
        if (history.isEmpty())
            return prompt.append("Tin nhắn hiện tại của user: ").append(message).toString();
        prompt.append("Lịch sử của cùng phiên chat:\n");
        history.forEach(item ->
                prompt.append(item.role()).append(": ").append(item.content()).append('\n'));
        return prompt.append("Tin nhắn hiện tại của user: ").append(message).toString();
    }
}
