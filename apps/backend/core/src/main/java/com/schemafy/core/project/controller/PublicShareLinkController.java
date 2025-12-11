package com.schemafy.core.project.controller;

import java.net.InetSocketAddress;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.schemafy.core.common.constant.ApiPath;
import com.schemafy.core.common.type.BaseResponse;
import com.schemafy.core.project.controller.dto.response.ShareLinkAccessResponse;
import com.schemafy.core.project.service.ShareLinkService;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(ApiPath.PUBLIC_API)
@RequiredArgsConstructor
public class PublicShareLinkController {

    private final ShareLinkService shareLinkService;

    @GetMapping("/share/{token}")
    public Mono<BaseResponse<ShareLinkAccessResponse>> accessByToken(
            @PathVariable String token, ServerHttpRequest request,
            @Nullable Authentication authentication) {
        String userId = (authentication != null) ? authentication.getName()
                : null;
        String ipAddress = extractIpAddress(request);
        String userAgent = request.getHeaders().getFirst("User-Agent");

        return shareLinkService
                .accessByToken(token, userId, ipAddress, userAgent)
                .map(BaseResponse::success);
    }

    private String extractIpAddress(ServerHttpRequest request) {
        String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }

        InetSocketAddress remoteAddress = request.getRemoteAddress();
        if (remoteAddress != null && remoteAddress.getAddress() != null) {
            return remoteAddress.getAddress().getHostAddress();
        }

        return "unknown";
    }

}
