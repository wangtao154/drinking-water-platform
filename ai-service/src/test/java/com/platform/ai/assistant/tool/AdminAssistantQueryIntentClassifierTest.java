package com.platform.ai.assistant.tool;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminAssistantQueryIntentClassifierTest {

    private final AdminAssistantQueryIntentClassifier classifier = new AdminAssistantQueryIntentClassifier();

    @Test
    void shouldRouteLiveDataQuestions() {
        assertTrue(classifier.requiresDataTools("查询设备0002近三天的制水情况"));
        assertTrue(classifier.requiresDataTools("今天有多少笔退款订单"));
        assertTrue(classifier.requiresDataTools("设备0002当前是否在线"));
    }

    @Test
    void shouldSkipToolRouterForConversationAndKnowledgeQuestions() {
        assertFalse(classifier.requiresDataTools("你好"));
        assertFalse(classifier.requiresDataTools("工单从报修到完成的流程是什么？"));
        assertFalse(classifier.requiresDataTools("退款中的订单为什么没有变成已退款？"));
    }
}
