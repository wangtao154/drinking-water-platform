#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Filter Management CRUD E2E API Test"""
import json
import urllib.request
import urllib.error
import sys

BASE = "http://localhost:8080/api/v1"

def api(method, path, token, body=None):
    url = f"{BASE}{path}"
    headers = {"Authorization": f"Bearer {token}", "Content-Type": "application/json; charset=utf-8"}
    data = json.dumps(body, ensure_ascii=False).encode("utf-8") if body else None
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req) as resp:
            return json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        return json.loads(e.read().decode("utf-8"))

def login():
    body = json.dumps({"account": "admin", "password": "admin123"}).encode("utf-8")
    req = urllib.request.Request(f"{BASE}/auth/login", data=body,
                                headers={"Content-Type": "application/json"}, method="POST")
    with urllib.request.urlopen(req) as resp:
        return json.loads(resp.read().decode("utf-8"))["data"]["accessToken"]

def main():
    token = login()
    print(f"[OK] Login token acquired\n")

    passed = 0
    failed = 0

    # ========== Filter Model CRUD ==========
    print("===== Filter Model CRUD =====")

    # 1. Create model
    print("\n[1] Create filter model...")
    res = api("POST", "/filter-models", token, {
        "modelName": "PP棉滤芯-测试型号",
        "category": "PP_COTTON",
        "filterLevel": 1,
        "standardLifeDuration": 6,
        "standardLifeFlow": 3000,
        "price": 5000,
        "status": 1,
        "description": "API测试创建的PP棉滤芯型号"
    })
    if res.get("code") == 200:
        model_id = res["data"]["id"]
        model_code = res["data"]["modelCode"]
        print(f"  PASS - id={model_id}, code={model_code}, name={res['data']['modelName']}")
        passed += 1
    else:
        print(f"  FAIL - {res}")
        failed += 1
        model_id = None

    # 2. List models
    print("\n[2] List filter models...")
    res = api("GET", "/filter-models?page=1&size=10", token)
    if res.get("code") == 200:
        total = res["data"]["total"]
        count = len(res["data"]["records"])
        print(f"  PASS - total={total}, page_records={count}")
        passed += 1
    else:
        print(f"  FAIL - {res}")
        failed += 1

    # 3. Get model by ID
    print("\n[3] Get filter model by ID...")
    if model_id:
        res = api("GET", f"/filter-models/{model_id}", token)
        if res.get("code") == 200 and res["data"]["modelName"] == "PP棉滤芯-测试型号":
            print(f"  PASS - name={res['data']['modelName']}, category={res['data']['category']}")
            passed += 1
        else:
            print(f"  FAIL - {res}")
            failed += 1
    else:
        print("  SKIP - no model_id")
        failed += 1

    # 4. Update model
    print("\n[4] Update filter model...")
    if model_id:
        res = api("PUT", f"/filter-models/{model_id}", token, {
            "modelName": "PP棉滤芯-测试型号(已更新)",
            "category": "PP_COTTON",
            "filterLevel": 1,
            "standardLifeDuration": 12,
            "standardLifeFlow": 5000,
            "price": 6800,
            "status": 1,
            "description": "更新后的描述"
        })
        if res.get("code") == 200 and res["data"]["standardLifeDuration"] == 12:
            print(f"  PASS - updated name={res['data']['modelName']}, life={res['data']['standardLifeDuration']}月")
            passed += 1
        else:
            print(f"  FAIL - {res}")
            failed += 1
    else:
        print("  SKIP - no model_id")
        failed += 1

    # 5. List enabled models
    print("\n[5] List enabled filter models...")
    res = api("GET", "/filter-models/enabled", token)
    if res.get("code") == 200 and isinstance(res["data"], list):
        print(f"  PASS - count={len(res['data'])}")
        passed += 1
    else:
        print(f"  FAIL - {res}")
        failed += 1

    # ========== Filter Instance CRUD ==========
    print("\n===== Filter Instance CRUD =====")

    # 6. Register filter
    print("\n[6] Register filter instance...")
    res = api("POST", "/filters/register", token, {
        "filterModelId": model_id,
        "productionDate": "2026-07-01",
        "productionBatch": "BATCH20260701"
    }) if model_id else {"code": 500, "message": "no model_id"}
    if res.get("code") == 200:
        filter_id = res["data"]["filterId"]
        print(f"  PASS - filterId={filter_id}, status={res['data']['lifecycleStatus']}")
        passed += 1
    else:
        print(f"  FAIL - {res}")
        failed += 1
        filter_id = None

    # 7. Page filter instances
    print("\n[7] Page filter instances...")
    res = api("GET", "/filters?page=1&size=10", token)
    if res.get("code") == 200:
        total = res["data"]["total"]
        print(f"  PASS - total={total}")
        passed += 1
    else:
        print(f"  FAIL - {res}")
        failed += 1

    # 8. Get filter by ID
    print("\n[8] Get filter by ID...")
    if filter_id:
        res = api("GET", f"/filters/{filter_id}", token)
        if res.get("code") == 200 and res["data"]["filterId"] == filter_id:
            print(f"  PASS - filterId={res['data']['filterId']}, status={res['data']['lifecycleStatus']}")
            passed += 1
        else:
            print(f"  FAIL - {res}")
            failed += 1
    else:
        print("  SKIP - no filter_id")
        failed += 1

    # 9. Update filter
    print("\n[9] Update filter instance...")
    if filter_id:
        res = api("PUT", f"/filters/{filter_id}", token, {
            "lifecycleStatus": "PENDING_INSTALL",
            "remark": "测试更新备注"
        })
        if res.get("code") == 200 and res["data"]["remark"] == "测试更新备注":
            print(f"  PASS - status={res['data']['lifecycleStatus']}, remark={res['data']['remark']}")
            passed += 1
        else:
            print(f"  FAIL - {res}")
            failed += 1
    else:
        print("  SKIP - no filter_id")
        failed += 1

    # 10. Trace filter
    print("\n[10] Trace filter lifecycle...")
    if filter_id:
        res = api("GET", f"/filters/{filter_id}/trace", token)
        if res.get("code") == 200:
            logs = res["data"]
            print(f"  PASS - trace records={len(logs)}")
            passed += 1
        else:
            print(f"  FAIL - {res}")
            failed += 1
    else:
        print("  SKIP - no filter_id")
        failed += 1

    # 11. Delete filter (not in use)
    print("\n[11] Delete filter instance...")
    if filter_id:
        res = api("DELETE", f"/filters/{filter_id}", token)
        if res.get("code") == 200:
            print(f"  PASS - deleted filterId={filter_id}")
            passed += 1
        else:
            print(f"  FAIL - {res}")
            failed += 1
    else:
        print("  SKIP - no filter_id")
        failed += 1

    # 12. Verify deletion
    print("\n[12] Verify filter deleted...")
    if filter_id:
        res = api("GET", f"/filters/{filter_id}", token)
        if res.get("code") == 40400:
            print(f"  PASS - filter not found after deletion")
            passed += 1
        else:
            print(f"  FAIL - expected 40400, got {res.get('code')}")
            failed += 1
    else:
        print("  SKIP - no filter_id")
        failed += 1

    # 13. Delete model (cleanup)
    print("\n[13] Delete filter model (cleanup)...")
    if model_id:
        res = api("DELETE", f"/filter-models/{model_id}", token)
        if res.get("code") == 200:
            print(f"  PASS - model deleted")
            passed += 1
        else:
            print(f"  FAIL - {res}")
            failed += 1
    else:
        print("  SKIP - no model_id")
        failed += 1

    # ========== Summary ==========
    print("\n" + "=" * 50)
    print(f"TOTAL: {passed + failed} | PASS: {passed} | FAIL: {failed}")
    print("=" * 50)
    return 0 if failed == 0 else 1

if __name__ == "__main__":
    sys.exit(main())
