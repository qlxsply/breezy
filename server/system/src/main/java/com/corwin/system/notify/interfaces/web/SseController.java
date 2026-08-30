package com.corwin.system.notify.interfaces.web;

import com.corwin.framework.constant.UserType;
import com.corwin.framework.web.auth.SseTicketService;
import com.corwin.framework.web.auth.TokenPayload;
import com.corwin.framework.web.ctx.CtxUtil;
import com.corwin.framework.web.response.ApiResponse;
import com.corwin.system.auth.published.Authenticated;
import com.corwin.system.notify.application.service.SseAppService;
import com.corwin.system.notify.application.service.SseSessionManager;
import com.corwin.system.notify.interfaces.web.res.SseTicketRes;
import com.corwin.system.resource.published.ApiMeta;
import com.corwin.system.resource.published.ApiModuleCode;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * SSE 通知接入控制器
 *
 * @author Corwin 2026/3/16
 */
@RestController
@ApiMeta(module = ApiModuleCode.SYSTEM)
@RequestMapping("/api/sse")
@AllArgsConstructor
public class SseController {

  private final SseSessionManager sessionManager;
  private final SseAppService sseAppService;
  private final SseTicketService sseTicketService;

  /** 创建 SSE 连接票据 */
  @PostMapping("/ticket")
  @Authenticated
  public ApiResponse<SseTicketRes> createTicket() {
    Long userId = CtxUtil.getPrincipal().userId();
    String username = CtxUtil.getPrincipal().username();
    UserType userType = CtxUtil.getPrincipal().userType();
    TokenPayload payload = new TokenPayload(userId, username, userType);
    SseTicketService.SseTicket ticket = sseTicketService.issue(payload);
    return ApiResponse.ok(new SseTicketRes(ticket.value(), ticket.expiresAtEpochMillis()));
  }

  /** 建立 SSE 消息流连接 */
  @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  @Authenticated
  public SseEmitter stream(
      @RequestHeader(value = "Last-Event-ID", required = false) String headerLastEventId,
      @RequestParam(value = "lastEventId", required = false) String queryLastEventId) {
    Long userId = CtxUtil.getPrincipal().userId();
    UserType userType = CtxUtil.getPrincipal().userType();
    SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
    sessionManager.register(userId, userType, emitter);

    // 根据最后事件 ID 进行初始补偿推送
    sseAppService.catchUp(userId, userType, parseLastEventId(headerLastEventId, queryLastEventId));

    return emitter;
  }

  /** 解析最后消费的事件 ID，优先读取请求头，其次读取查询参数 */
  private Long parseLastEventId(String headerLastEventId, String queryLastEventId) {
    String raw = headerLastEventId;
    if (raw == null || raw.isBlank()) {
      raw = queryLastEventId;
    }
    if (raw == null || raw.isBlank()) {
      return null;
    }
    try {
      return Long.parseLong(raw.trim());
    } catch (NumberFormatException ex) {
      return null;
    }
  }
}
