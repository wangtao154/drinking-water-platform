package com.platform.ai.assistant.safety;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Rule-based safety gate that runs before model routing and data-tool calls.
 * It deliberately returns a safe refusal instead of forwarding risky content
 * to the external model.
 */
@Component
public class AdminAssistantContentSafetyService {

    private static final List<Rule> RULES = List.of(
            new Rule("SENSITIVE_CREDENTIAL", "安全或隐私风险", "SECURITY_PRIVACY",
                    Pattern.compile(".*(密码|密钥|secret|token|appsecret|api[ _-]?key|private[ _-]?key|证书|银行卡|身份证|openid|unionid).*(查看|显示|提供|导出|发送|复制|告诉|给我|泄露).*", Pattern.CASE_INSENSITIVE),
                    "该问题涉及凭据或个人敏感信息，不能由 AI 获取、展示或转发。"),
            new Rule("MISUSE_OR_BYPASS", "违规或绕过行为", "MISUSE_REPORT",
                    Pattern.compile(".*(绕过|破解|伪造|篡改|盗用|攻击|注入|刷单|越权|删除审计|清除审计|修改日志).*", Pattern.CASE_INSENSITIVE),
                    "该问题包含绕过权限、破坏审计或其他违规操作，系统已拒绝处理。"),
            new Rule("HIGH_RISK_OPERATION", "高风险业务操作", "MISUSE_REPORT",
                    Pattern.compile(".*((帮我|请|立即|自动|执行|下发|控制).*(设备|mqtt|q[0-9]+|订单|退款|账户|角色|权限|阈值|配置)|(退款|转账|派单|删除|修改).*(订单|账户|角色|权限|阈值|配置)).*", Pattern.CASE_INSENSITIVE),
                    "该问题要求执行设备、资金或权限相关高风险操作，必须由具备权限的人员在业务页面人工确认。"),
            new Rule("HIGH_RISK_DECISION", "高风险决策", "MISUSE_REPORT",
                    Pattern.compile(".*(替我决定|直接决定|最终结论|是否必须.*(更换|报废)|医疗诊断|处方|法律意见|投资建议|经营决策|定价决策).*", Pattern.CASE_INSENSITIVE),
                    "该问题需要人工承担设备维护、经营或其他高风险决策责任，AI 不能给出可直接执行的最终结论。")
    );

    public SafetyDecision inspect(String question) {
        if (!StringUtils.hasText(question)) {
            return SafetyDecision.allow();
        }
        String normalized = question.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
        for (Rule rule : RULES) {
            if (rule.pattern().matcher(normalized).matches()) {
                return new SafetyDecision(true, rule.code(), rule.label(), rule.category(), rule.reason());
            }
        }
        return SafetyDecision.allow();
    }

    private record Rule(String code, String label, String category, Pattern pattern, String reason) {
    }

    public record SafetyDecision(boolean blocked, String ruleCode, String ruleLabel, String complaintCategory,
                                 String reason) {
        private static SafetyDecision allow() {
            return new SafetyDecision(false, null, null, null, null);
        }
    }
}
