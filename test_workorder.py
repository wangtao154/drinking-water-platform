import urllib.request
import json

BASE = "http://localhost:8080/api/v1"

def post(path, data, token=None):
    url = BASE + path
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = "Bearer " + token
    body = json.dumps(data).encode()
    req = urllib.request.Request(url, data=body, headers=headers, method="POST")
    try:
        resp = urllib.request.urlopen(req, timeout=10)
        return json.loads(resp.read())
    except Exception as e:
        return {"error": str(e)}

def get(path, token=None):
    url = BASE + path
    headers = {}
    if token:
        headers["Authorization"] = "Bearer " + token
    req = urllib.request.Request(url, headers=headers, method="GET")
    try:
        resp = urllib.request.urlopen(req, timeout=10)
        return json.loads(resp.read())
    except Exception as e:
        return {"error": str(e)}

def put(path, data, token=None):
    url = BASE + path
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = "Bearer " + token
    body = json.dumps(data).encode()
    req = urllib.request.Request(url, data=body, headers=headers, method="PUT")
    try:
        resp = urllib.request.urlopen(req, timeout=10)
        return json.loads(resp.read())
    except Exception as e:
        return {"error": str(e)}

# 1. Login as customer
r = post("/auth/customer-login", {"phone": "13800138000", "password": "123456"})
ct = r["data"]["accessToken"]
print("1. Customer login:", "OK" if r["code"] == 200 else "FAIL")

# 2. Login as worker
r = post("/auth/worker-login", {"phone": "13900000001", "password": "123456"})
wt = r["data"]["accessToken"]
print("2. Worker login:", "OK" if r["code"] == 200 else "FAIL")

# 3. Login as admin
r = post("/auth/login", {"account": "admin", "password": "admin123"})
at = r["data"]["accessToken"]
print("3. Admin login:", "OK" if r["code"] == 200 else "FAIL")

# 4. Customer creates work order
r = post("/work-orders/customer-create", {
    "description": "Water has bad taste, needs repair",
    "orderType": "REPAIR",
    "customerImages": ["http://192.168.21.2:9000/drinking-water/test1.jpg"]
}, ct)
print(f"4. Customer create: {json.dumps(r, ensure_ascii=False)[:200]}")
order_id = r["data"]["id"] if r.get("data") else None
order_no = r["data"]["orderNo"] if r.get("data") else "N/A"

# 5. Customer views my orders
r = get("/work-orders/my", ct)
count = len(r["data"]) if r.get("data") else 0
print(f"5. Customer my orders: Code={r['code']}, Count={count}")

# 6. Admin lists work orders
r = get("/work-orders?pageNum=1&pageSize=5", at)
total = r["data"]["total"] if r.get("data") else 0
print(f"6. Admin list work orders: Code={r['code']}, Total={total}")

# 7. Admin assigns work order to worker
if order_id:
    worker_id = 2074746153908920321  # Worker1
    r = put(f"/work-orders/{order_id}/dispatch", {"workerId": worker_id}, at)
    print(f"7. Admin assign to worker: Code={r['code']}")

# 8. Worker views assigned orders
r = get("/work-orders/worker", wt)
count = len(r["data"]) if r.get("data") else 0
print(f"8. Worker assigned orders: Code={r['code']}, Count={count}")

# 9. Worker completes work order
if order_id:
    r = put(f"/work-orders/{order_id}/complete", {
        "afterPhotos": ["http://192.168.21.2:9000/drinking-water/done1.jpg"],
        "remark": "Replaced filter cartridge, water quality restored"
    }, wt)
    print(f"9. Worker complete: Code={r['code']}")

# 10. Customer reviews
if order_id:
    r = put(f"/work-orders/{order_id}/review", {
        "rating": 5,
        "reviewContent": "Great work, water tastes good now"
    }, ct)
    print(f"10. Customer review: Code={r['code']}")

# 11. Admin views final work order detail
if order_id:
    r = get(f"/work-orders/{order_id}", at)
    status = r["data"]["orderStatus"] if r.get("data") else "N/A"
    rating = r["data"]["rating"] if r.get("data") else "N/A"
    print(f"11. Admin view detail: Code={r['code']}, Status={status}, Rating={rating}")

print("\n=== Full flow test complete ===")
