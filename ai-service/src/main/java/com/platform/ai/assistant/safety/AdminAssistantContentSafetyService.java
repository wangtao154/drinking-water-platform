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
            new Rule("CONTRABAND_DRUG", "疑似毒品相关", "MISUSE_REPORT",
                    Pattern.compile(".*(毒品|制毒|贩毒|吸毒|冰毒|海洛因|可卡因|摇头丸|麻古|芬太尼|罂粟|氯胺酮|k粉|大麻(种植|交易|购买|吸食|制品)).*", Pattern.CASE_INSENSITIVE),
                    "该问题疑似涉及毒品、制毒、贩毒或吸毒相关内容，系统已拒绝处理。"),
            new Rule("PT_TO_SITES", "疑似站外引流", "MISUSE_REPORT",
                    Pattern.compile(".*(加(我)?(微信|微|v|vx)|联系(我|客服)?(微信|qq)|qq群|telegram|电报群|扫码(加|进).{0,4}群|私聊.{0,6}(微信|qq|联系方式)|点击.{0,8}(外链|站外链接)|跳转.{0,8}(站外|外部网站)).*", Pattern.CASE_INSENSITIVE),
                    "该问题疑似包含站外引流、外部联系或绕过平台沟通的内容，系统已拒绝处理。"),
            new Rule("CONTRABAND_GAMBLING", "疑似赌博相关", "MISUSE_REPORT",
                    Pattern.compile(".*(赌博|博彩|赌场|下注|投注|赔率|盘口|百家乐|六合彩|赌球|网赌|老虎机|炸金花).*", Pattern.CASE_INSENSITIVE),
                    "该问题疑似涉及赌博、博彩或投注相关内容，系统已拒绝处理。"),
            new Rule("CONTRABAND_ACT", "疑似违法行为", "MISUSE_REPORT",
                    Pattern.compile(".*(枪支|弹药|管制刀具|爆炸物|制作炸弹|洗钱|诈骗|走私|假证|伪造证件|盗窃|抢劫|绑架|买卖人口|代开发票|黑产|销赃).*", Pattern.CASE_INSENSITIVE),
                    "该问题疑似涉及违法犯罪活动或违禁物品，系统已拒绝处理。"),
            new Rule("VIOLENT_INCIDENTS", "疑似暴恐极端内容", "MISUSE_REPORT",
                    Pattern.compile(".*(恐怖主义|恐怖组织|极端主义|恐怖袭击|恐袭|爆炸袭击|自杀式袭击|屠杀|斩首|圣战组织).*", Pattern.CASE_INSENSITIVE),
                    "该问题疑似涉及暴力、恐怖主义或极端主义内容，系统已拒绝处理。"),
            new Rule("PORNOGRAPHIC_ADULT", "疑似色情内容", "MISUSE_REPORT",
                    Pattern.compile(".*(色情|成人视频|淫秽|裸聊|约炮|性爱视频|黄色网站|成人网站|援交|性交易).*", Pattern.CASE_INSENSITIVE),
                    "该问题疑似涉及色情、淫秽或成人内容，系统已拒绝处理。"),
            new Rule("POLITICAL_A", "疑似敏感政治内容", "MISUSE_REPORT",
                    Pattern.compile(".*(政治敏感|煽动颠覆|分裂国家|推翻政府|非法政治组织|侮辱国旗|反政府|政治谣言).*", Pattern.CASE_INSENSITIVE),
                    "该问题疑似涉及违法或敏感政治内容，系统已拒绝处理。"),
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
