-- Repair the display name only; preserve role assignments and permission metadata.
-- UTF-8 hex avoids terminal/client encoding corruption. Expected: AI assistant use (Chinese).
SET NAMES utf8mb4;
UPDATE sys_permission
SET permission_name = CONVERT(0x414920E58AA9E6898BE4BDBFE794A8 USING utf8mb4)
WHERE permission_code = 'AI_ASSISTANT_VIEW'
  AND HEX(permission_name) <> '414920E58AA9E6898BE4BDBFE794A8';

SELECT permission_code, permission_name, HEX(permission_name) AS name_hex
FROM sys_permission WHERE permission_code = 'AI_ASSISTANT_VIEW';
