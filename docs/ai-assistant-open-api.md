# AI Assistant Open API v1

## Scope

The external endpoint only invokes the existing controlled, read-only assistant. It cannot dispatch devices, change accounts or roles, issue refunds, edit configurations, or call arbitrary internal APIs.

Each API Key is bound to the backend account that created it. At request time the key uses that account's current enabled role permissions. Disabling the account, changing its role permissions, disabling the key, revoking the key, or reaching the expiry time immediately prevents or limits future calls.

## Create a key

Authenticated administrators with `AI_ASSISTANT_API_KEY_MANAGE` call:

```http
POST /api/v1/ai/assistant/api-keys
Authorization: Bearer <admin-jwt>
Content-Type: application/json

{
  "name": "partner-reporting",
  "rateLimitPerMinute": 30,
  "expiresAt": "2027-09-11 00:00:00"
}
```

The response includes `apiKey` exactly once. Store it in the caller's secret vault. The platform stores only its SHA-256 hash and cannot recover the original secret.

`GET /api/v1/ai/assistant/api-keys` lists keys owned by the current account, without their secrets. `PATCH /api/v1/ai/assistant/api-keys/{id}/status` accepts `ENABLED`, `DISABLED`, or `REVOKED`. A revoked key must be replaced with a new key.

## External chat

```http
POST /api/v1/ai/open/chat
X-API-Key: dwai_v1_<key-id>_<secret>
Content-Type: application/json

{
  "question": "查询设备0002近三天的制水情况",
  "conversationId": "optional-previous-conversation-id"
}
```

On the first request omit `conversationId`; pass the returned ID on later requests when multi-turn context is needed. A response includes `requestId`, `answer`, `model`, `fallback`, citations and controlled data-source metadata.

## Security and audit

- Only `X-API-Key` is accepted; do not put the key in URL query parameters or browser code.
- Keys are limited to `AI_CHAT_READONLY`, default 30 requests/minute and a maximum of 120/minute.
- The account bound to the key must remain enabled and identity-verified.
- Every AI response remains covered by the AI audit chain. API invocation metadata (key ID, endpoint, status, elapsed time and masked source IP) is additionally retained for at least six months; complete questions and answers are not duplicated in this access log.
- Rotate a key by creating a replacement first, updating the partner configuration, then setting the old key to `REVOKED`.
