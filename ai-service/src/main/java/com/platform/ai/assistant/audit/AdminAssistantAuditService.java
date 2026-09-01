package com.platform.ai.assistant.audit;

/** Keeps AI compliance logging independent from normal application logs. */
public interface AdminAssistantAuditService {

    void record(AdminAssistantAuditEvent event);

    /**
     * Called in the same transaction as backend account deletion. Existing AI
     * records are retained for at least six months after the account is deleted.
     */
    void retainAfterAccountCancellation(Long accountId);

    int purgeExpired();
}
