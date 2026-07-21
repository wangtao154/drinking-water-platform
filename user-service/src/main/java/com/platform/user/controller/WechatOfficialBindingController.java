package com.platform.user.controller;

import com.platform.common.auth.UserContext;
import com.platform.common.exception.BusinessException;
import com.platform.common.result.R;
import com.platform.common.result.ResultCode;
import com.platform.user.service.WechatOfficialBindingService;
import com.platform.user.vo.OfficialAccountBindStatusVO;
import com.platform.user.vo.OfficialAccountBindUrlVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

@RestController
@RequestMapping("/api/v1/wechat/official")
@RequiredArgsConstructor
public class WechatOfficialBindingController {

    private final WechatOfficialBindingService bindingService;

    @GetMapping("/bind-url")
    public R<OfficialAccountBindUrlVO> bindUrl() {
        Long workerId = requireWorker();
        return R.ok(bindingService.createBindUrl(workerId));
    }

    @GetMapping("/bind-status")
    public R<OfficialAccountBindStatusVO> bindStatus() {
        Long workerId = requireWorker();
        return R.ok(bindingService.getBindStatus(workerId));
    }

    @GetMapping(value = "/callback", produces = MediaType.TEXT_HTML_VALUE)
    public String callback(@RequestParam String code, @RequestParam String state) {
        try {
            bindingService.handleCallback(code, state);
            return htmlPage("绑定成功", "公众号通知绑定成功，请返回小程序查看状态。", true);
        } catch (Exception e) {
            String message = e instanceof BusinessException ? e.getMessage() : "绑定失败，请返回小程序重新尝试。";
            return htmlPage("绑定失败", message, false);
        }
    }

    private Long requireWorker() {
        Long userId = UserContext.getUserId();
        String userType = UserContext.getUserType();
        if (userId == null || !"WORKER".equals(userType)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "仅运维人员可以绑定公众号通知");
        }
        return userId;
    }

    private String htmlPage(String title, String message, boolean success) {
        String color = success ? "#1890ff" : "#f56c6c";
        String escapedTitle = HtmlUtils.htmlEscape(title);
        String escapedMessage = HtmlUtils.htmlEscape(message);
        return """
                <!doctype html>
                <html lang="zh-CN">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>%s</title>
                  <style>
                    body { margin:0; font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif; background:#f5f7fa; color:#303133; }
                    .wrap { min-height:100vh; display:flex; align-items:center; justify-content:center; padding:24px; box-sizing:border-box; }
                    .card { width:100%%; max-width:420px; background:#fff; border-radius:12px; padding:32px 24px; box-shadow:0 8px 24px rgba(0,0,0,.08); text-align:center; }
                    .icon { width:56px; height:56px; border-radius:50%%; margin:0 auto 18px; background:%s; color:#fff; display:flex; align-items:center; justify-content:center; font-size:34px; }
                    h1 { font-size:22px; margin:0 0 12px; }
                    p { font-size:15px; line-height:1.7; color:#606266; margin:0; }
                  </style>
                </head>
                <body>
                  <div class="wrap">
                    <div class="card">
                      <div class="icon">%s</div>
                      <h1>%s</h1>
                      <p>%s</p>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(escapedTitle, color, success ? "✓" : "!", escapedTitle, escapedMessage);
    }
}
