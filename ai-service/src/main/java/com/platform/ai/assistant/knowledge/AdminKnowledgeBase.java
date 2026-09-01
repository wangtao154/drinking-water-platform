package com.platform.ai.assistant.knowledge;

import com.platform.ai.config.AdminAssistantProperties;
import com.platform.common.auth.CurrentUser;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Loads a fixed, packaged knowledge base. User input can never select arbitrary files.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminKnowledgeBase {

    private static final Pattern FRONT_MATTER = Pattern.compile("(?s)^---\\R(.*?)\\R---\\R(.*)$");
    private static final List<String> KNOWLEDGE_FILES = List.of(
            "device-and-telemetry.md",
            "alerts-and-thresholds.md",
            "work-orders.md",
            "scan-orders-and-refunds.md",
            "reports-and-export.md",
            "rbac-and-audit.md",
            "data-boundary.md"
    );
    private static final Map<String, List<String>> TOPIC_KEYWORDS = Map.of(
            "device-and-telemetry", List.of("设备", "sn", "控制板", "在线", "离线", "心跳", "历史", "遥测", "曲线", "点位", "influx",
                    "滤芯", "pp棉", "活性炭", "ro膜", "更换", "维护", "寿命", "pp_cotton", "ro_membrane"),
            "alerts-and-thresholds", List.of("告警", "报警", "阈值", "未处理", "告警列表"),
            "work-orders", List.of("工单", "报修", "派单", "接单", "核验", "维修", "退机", "移机"),
            "scan-orders-and-refunds", List.of("扫码", "取水", "订单", "支付", "退款", "q74", "ack", "出水",
                    "计费", "收费", "价格", "单价", "flow_based", "monthly_rent", "package_recharge", "shared", "冷水", "热水", "元/升"),
            "reports-and-export", List.of("报表", "导出", "excel", "统计", "数据导出", "时间间隔"),
            "rbac-and-audit", List.of("角色", "权限", "账户", "用户", "审计", "登录", "管理员"),
            "data-boundary", List.of("隐私", "脱敏", "千问", "第三方", "模型", "数据边界", "敏感", "openid", "密钥", "token")
    );

    private final AdminAssistantProperties properties;
    private final List<AdminKnowledgeDocument> documents = new ArrayList<>();

    @PostConstruct
    void load() {
        for (String fileName : KNOWLEDGE_FILES) {
            try {
                documents.add(read(fileName));
            } catch (IOException | IllegalArgumentException ex) {
                throw new IllegalStateException("无法加载 AI 助手知识库文件: " + fileName, ex);
            }
        }
        log.info("[Assistant] Loaded {} controlled knowledge documents", documents.size());
    }

    public List<AdminKnowledgeDocument> search(String question, CurrentUser user) {
        String normalizedQuestion = question.toLowerCase(Locale.ROOT);
        return documents.stream()
                .filter(document -> hasAnyPermission(user, document.permissions()))
                .map(document -> Map.entry(document, score(document, normalizedQuestion)))
                .filter(entry -> entry.getValue() > 0)
                .sorted(Map.Entry.<AdminKnowledgeDocument, Integer>comparingByValue().reversed()
                        .thenComparing(entry -> entry.getKey().title()))
                .limit(properties.getMaxKnowledgeDocuments())
                .map(Map.Entry::getKey)
                .toList();
    }

    public boolean hasAuthorizedDocuments(CurrentUser user) {
        return documents.stream().anyMatch(document -> hasAnyPermission(user, document.permissions()));
    }

    private AdminKnowledgeDocument read(String fileName) throws IOException {
        ClassPathResource resource = new ClassPathResource("knowledge/admin-assistant/" + fileName);
        String markdown = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        Matcher matcher = FRONT_MATTER.matcher(markdown);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("缺少 front matter");
        }
        Map<String, String> fields = parseFields(matcher.group(1));
        String id = required(fields, "id");
        String title = required(fields, "title");
        String updatedAt = required(fields, "updatedAt");
        return new AdminKnowledgeDocument(
                id,
                title,
                fields.getOrDefault("scope", ""),
                parseList(fields.get("permissions")),
                parseList(fields.get("sources")),
                updatedAt,
                matcher.group(2).trim()
        );
    }

    private static int score(AdminKnowledgeDocument document, String question) {
        int score = 0;
        String titleAndScope = (document.title() + " " + document.scope()).toLowerCase(Locale.ROOT);
        for (String keyword : TOPIC_KEYWORDS.getOrDefault(document.id(), List.of())) {
            if (question.contains(keyword)) {
                score += 3;
            }
            if (titleAndScope.contains(keyword) && question.contains(keyword)) {
                score += 1;
            }
        }
        if (question.contains(document.id().replace('-', ' '))) {
            score += 4;
        }
        return score;
    }

    private static boolean hasAnyPermission(CurrentUser user, List<String> permissions) {
        return user != null && permissions.stream().anyMatch(user::hasPermission);
    }

    private static Map<String, String> parseFields(String frontMatter) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (String line : frontMatter.lines().toList()) {
            int separator = line.indexOf(':');
            if (separator > 0) {
                fields.put(line.substring(0, separator).trim(), line.substring(separator + 1).trim());
            }
        }
        return fields;
    }

    private static String required(Map<String, String> fields, String name) {
        String value = fields.get(name);
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("缺少元数据: " + name);
        }
        return value;
    }

    private static List<String> parseList(String value) {
        if (!StringUtils.hasText(value)) {
            return List.of();
        }
        String normalized = value.trim();
        if (normalized.startsWith("[") && normalized.endsWith("]")) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        return Arrays.stream(normalized.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }
}
