package com.schemafy.core.chat.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.schemafy.core.chat.controller.dto.ChatMessageResponse;
import com.schemafy.core.chat.service.ChatService;
import com.schemafy.core.common.config.ConditionalOnRedisEnabled;
import com.schemafy.core.common.constant.ApiPath;
import com.schemafy.core.common.type.BaseResponse;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@ConditionalOnRedisEnabled
@RequestMapping(ApiPath.API)
public class ChatController {

    private final ChatService chatService;

    @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR')")
    @GetMapping("/projects/{projectId}/chat/messages")
    public Mono<BaseResponse<List<ChatMessageResponse>>> getChatMessages(
            @PathVariable String projectId,
            @RequestParam(required = false) String before,
            @RequestParam(required = false) Integer limit) {
        return chatService.getMessages(projectId, before, limit)
                .map(ChatMessageResponse::from)
                .collectList()
                .map(BaseResponse::success);
    }

}

