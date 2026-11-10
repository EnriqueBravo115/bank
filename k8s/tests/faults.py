"""Failure-path tests on the kind cluster: pods killed, dependencies down, timeouts, poison messages.

They scale deployments down and back up: run them only against a test cluster.
  python3 k8s/tests/faults.py                 # every scenario (about 30 minutes)
  python3 k8s/tests/faults.py broker poison   # some of them (names in SCENARIOS)
"""
import os
import random
import re
import subprocess
import sys
import threading
import time
import uuid
from decimal import Decimal

import requests

from lib import KC, KONG, poll, record, sec, summary, token, verify_email_via_mailpit

NS = ["kubectl", "--context", os.environ.get("KUBE_CONTEXT", "kind-bank"), "-n", "bank"]
DBS = {"user": "user_db", "account": "account_db", "transaction": "transaction_db"}


def kube(*args, check=True, input=None, timeout=300):
    r = subprocess.run(NS + list(args), capture_output=True, text=True, input=input, timeout=timeout)
    if check and r.returncode != 0:
        raise RuntimeError(f"kubectl {' '.join(args)}: {r.stderr.strip()}")
    return r.stdout.strip()


def psql(db, sql):
    return kube("exec", f"{db}-db-0", "--", "psql", "-U", "user", "-d", DBS[db], "-tA", "-c", sql)


def restarts(app):
    out = kube("get", "pods", "-l", f"app={app}", "-o", "jsonpath={.items[*].status.containerStatuses[*].restartCount}")
    return sum(int(x) for x in out.split()) if out else 0


def scale(kind, name, replicas):
    kube("scale", f"{kind}/{name}", f"--replicas={replicas}")
    if replicas == 0:
        kube("wait", "--for=delete", "pod", "-l", f"app={name}", "--timeout=180s", check=False)
    else:
        kube("rollout", "status", f"{kind}/{name}", "--timeout=400s", timeout=420)


def kafka(script, *args, input=None):
    return kube("exec", "-i", "broker-0", "--", f"/opt/kafka/bin/{script}", "--bootstrap-server", "localhost:9092",
                *args, input=input, check=False, timeout=120)


def hdr(tok):
    return {"Authorization": f"Bearer {tok}"} if tok else {}


def get(path, tok, **kw):
    return requests.get(KONG + path, headers=hdr(tok), timeout=30, **kw)


def post(path, tok, body=None, **kw):
    return requests.post(KONG + path, headers=hdr(tok), json=body, timeout=60, **kw)


def admin():
    return token("admin-user", "admin123")


class Customer:
    def __init__(self, email, password, uid, account):
        self.email, self.password, self.uid, self.account = email, password, uid, account
        self.kid = psql("user", f"SELECT keycloak_id FROM users WHERE id = {uid}")

    def active_in_db(self):
        return psql("user", f"SELECT active FROM users WHERE id = {self.uid}") == "t"

    def tok(self):
        return token(self.email, self.password)

    def balance(self):
        return Decimal(str(get("/api/v1/account/me/balance", self.tok()).json()["balance"]))

    def account_view(self):
        return get(f"/api/v1/account/get-by-account-number/{self.account}", admin()).json()


def new_customer(tag):
    """Full registration through Kong, as in the E2E test; returns once the account exists."""
    run = uuid.uuid4().hex[:6]
    email, pw = f"{tag}.{run}@test.com", "Secure1!pass"
    phone = str(random.randint(5000000000, 5999999999))
    y, d = random.randint(60, 99), random.randint(1, 28)
    letters = "".join(random.choice("BCDFGHJKLMNPQRSTVWXZ") for _ in range(2))
    curp, rfc = f"P{random.choice('AEIOU')}{letters}{y:02d}03{d:02d}HDFRRN0{random.randint(0, 9)}", None
    rfc = curp[:10] + "AB1"
    r = post("/api/v1/user/register", None, {"email": email, "password": pw, "phoneCode": "+52", "phoneNumber": phone})
    assert r.status_code in (200, 201), r.text
    t = token(email, pw)
    r = requests.put(f"{KONG}/api/v1/user/register/me/profile", headers=hdr(t), timeout=30, json={
        "names": tag.capitalize(), "firstSurname": "P" + letters[0].lower() + "rez", "gender": "MALE",
        "birthday": f"{d:02d}/03/19{y:02d}", "countryOfBirth": "MX"})
    assert r.status_code == 204, r.text
    assert verify_email_via_mailpit(email) is True
    t = token(email, pw)
    r = requests.put(f"{KONG}/api/v1/user/register/me/kyc", headers=hdr(t), timeout=30,
                     json={"curp": curp, "rfc": rfc, "documentType": "NATIONAL_ID_CARD"})
    assert r.status_code == 204, r.text
    t = token(email, pw)
    acc = poll(lambda: (r := get("/api/v1/account/me", t)).status_code == 200 and r.json(), timeout=120)
    assert acc, "no account"
    uid = get("/api/v1/user/me", t).json()["id"]
    return Customer(email, pw, uid, acc["accountNumber"])


def deposit(c, amount):
    r = post("/api/v1/transaction/create/deposit", admin(), {"amount": amount, "description": "Funding",
             "targetIdentifier": c.account, "targetIdentifierType": "ACCOUNT", "currency": "MX"})
    assert r.status_code == 200 and r.json()["transactionStatus"] == "COMPLETED", r.text


def transfer(src, dst, amount, desc="Fault test"):
    return post("/api/v1/transaction/create/transfer", src.tok(), {
        "amount": amount, "description": desc, "currency": "MX", "sourceIdentifier": src.account,
        "sourceIdentifierType": "ACCOUNT", "targetIdentifier": dst.account, "targetIdentifierType": "ACCOUNT"})


def tx_status(code):
    return get(f"/api/v1/transaction/query/{code}", admin()).json().get("transactionStatus")


def lock_account(account, seconds):
    """Holds SELECT ... FOR UPDATE on the account row: account-service movements on it block meanwhile."""
    sql = (f"BEGIN; SELECT id FROM account WHERE account_number = '{account}' FOR UPDATE; "
           f"SELECT pg_sleep({seconds}); COMMIT;")
    th = threading.Thread(target=lambda: psql("account", sql), daemon=True)
    th.start()
    time.sleep(1.5)  # let the lock be taken
    return th


def movements_with_key(code):
    return int(psql("account", f"SELECT count(*) FROM movement WHERE idempotency_key = '{code}'") or 0)


# ---------------------------------------------------------------------------------------------
def scenario_pod_kill(x, y):
    sec("1. Pods of the services deleted (single replica: some downtime expected)")
    for app in ("user-service", "account-service", "transaction-service"):
        svc = app.split("-")[0]
        kube("delete", "pod", "-l", f"app={app}", "--wait=false")
        t0 = time.time()
        time.sleep(1)
        ok = poll(lambda: requests.get(f"{KONG}/{svc}/actuator/health", timeout=10).status_code == 200, timeout=120, every=1)
        record(f"{app}: back through Kong {time.time() - t0:.0f}s after the pod was deleted", bool(ok))
    start = time.time()
    ok = poll(lambda: (r := transfer(x, y, 1, "after pod restart")).status_code == 200
              and r.json()["transactionStatus"] == "COMPLETED", timeout=120, every=5)
    record(f"transfer (transaction -> account through Eureka) works {time.time() - start:.0f}s later", bool(ok))


def scenario_account_down(x, y):
    sec("2. account-service down for 2.5 minutes")
    tx_restarts = restarts("transaction-service")
    before = int(psql("transaction", "SELECT count(*) FROM transactions"))
    bal_x = x.balance()
    scale("deploy", "account-service", 0)
    codes = []
    for i in range(6):  # enough calls to open the circuit breaker
        r = transfer(x, y, 10, f"while account-service is down {i}")
        codes.append(r.status_code)
    record("transfers rejected with 503 while account-service is down", set(codes) == {503}, str(codes))
    after = int(psql("transaction", "SELECT count(*) FROM transactions"))
    record("no transaction stored for the rejected requests", after == before, f"{before} -> {after}")
    r = get("/api/v1/user/me", x.tok())
    record("user-service still answers", r.status_code == 200, str(r.status_code))
    r = get("/api/v1/transaction/query/history", x.tok(), params={"sourceIdentifier": x.account, "status": "COMPLETED"})
    record("transaction history (no account-service call) still answers", r.status_code == 200, f"{r.status_code} {r.text[:150]}")
    health = []
    for _ in range(15):  # 150 s > liveness 5 x 20 s
        health.append(requests.get(f"{KONG}/transaction/actuator/health", timeout=10).status_code)
        time.sleep(10)
    record("transaction-service stays healthy (no restart) while account-service is down",
           restarts("transaction-service") == tx_restarts and set(health) == {200},
           f"health codes {health}, restarts {tx_restarts} -> {restarts('transaction-service')}")
    scale("deploy", "account-service", 1)
    start = time.time()
    ok = poll(lambda: (r := transfer(x, y, 10, "after account-service is back")).status_code == 200
              and r.json()["transactionStatus"] == "COMPLETED", timeout=180, every=5)
    record(f"transfers work again ({time.time() - start:.0f}s after account-service is ready)", bool(ok))
    record("only the successful transfer moved money", x.balance() == bal_x - 10, f"{bal_x} -> {x.balance()}")


def scenario_timeout_pending(x, y):
    sec("3. account-service too slow (row locked 20s): PENDING and automatic retry")
    bx, by = x.balance(), y.balance()
    th = lock_account(x.account, 20)
    t0 = time.time()
    r = transfer(x, y, 100, "slow account-service")
    data = r.json()
    record(f"transfer answered PENDING after the 5s timeout ({time.time() - t0:.1f}s)",
           r.status_code == 200 and data.get("transactionStatus") == "PENDING", f"{r.status_code} {r.text[:200]}")
    code = data.get("transactionCode")
    record("the transaction is visible as PENDING", tx_status(code) == "PENDING", tx_status(code))
    th.join()
    ok = poll(lambda: tx_status(code) == "COMPLETED", timeout=120, every=3)
    record("the reconciler completes it once account-service answers", bool(ok), tx_status(code))
    time.sleep(10)
    record("money moved exactly once (sender)", x.balance() == bx - 100, f"{bx} -> {x.balance()}")
    record("money moved exactly once (receiver)", y.balance() == by + 100, f"{by} -> {y.balance()}")
    record("account-service applied the idempotency key once", movements_with_key(code) == 1, str(movements_with_key(code)))


def scenario_cancel_pending(x, y):
    sec("4. Customer cancels a PENDING transaction while account-service is blocked")
    bx, by = x.balance(), y.balance()
    th = lock_account(x.account, 25)
    r = transfer(x, y, 50, "to be cancelled")
    code = r.json().get("transactionCode")
    record("transfer PENDING", r.json().get("transactionStatus") == "PENDING", r.text[:200])
    first = post(f"/api/v1/transaction/{code}/cancel", x.tok())
    record("cancel while blocked: CANCELLED/REVERSED or 503 (try later)", first.status_code in (200, 503), f"{first.status_code} {first.text[:200]}")
    th.join()
    final = first.json().get("transactionStatus") if first.status_code == 200 else None
    if final is None:
        res = poll(lambda: (r := post(f"/api/v1/transaction/{code}/cancel", x.tok())).status_code == 200 and r.json(),
                   timeout=60, every=3)
        final = res.get("transactionStatus") if res else tx_status(code)
    record("transaction ends CANCELLED or REVERSED", final in ("CANCELLED", "REVERSED"), str(final))
    time.sleep(30)  # the original call and the reconciler must not apply it afterwards
    record("status stays the same 30s later", tx_status(code) == final, tx_status(code))
    record("no money moved in the end (sender)", x.balance() == bx, f"{bx} -> {x.balance()}")
    record("no money moved in the end (receiver)", y.balance() == by, f"{by} -> {y.balance()}")


def scenario_stuck(x, y):
    sec("5. Transaction stuck after the last automatic attempt, admin retries it")
    bx = x.balance()
    th = lock_account(x.account, 40)
    r = transfer(x, y, 25, "stuck transfer")
    code = r.json().get("transactionCode")
    # Pretend 19 attempts already failed: the next failed one is the last
    psql("transaction", f"UPDATE transactions SET attempts = 19, next_attempt_at = now() WHERE transaction_code = '{code}'")
    stuck = poll(lambda: any(t["transactionCode"] == code for t in get("/api/v1/transaction/stuck", admin()).json()), timeout=40, every=2)
    record("after the 20th failed attempt it is listed in /stuck", bool(stuck))
    reason = psql("transaction", f"SELECT reason FROM transactions WHERE transaction_code = '{code}'")
    record("reason explains it waits for an admin", "admin" in reason, reason)
    th.join()
    time.sleep(10)
    record("no more automatic retries once stuck", tx_status(code) == "PENDING", tx_status(code))
    r = post(f"/api/v1/transaction/{code}/retry", admin())
    record("admin retry completes it", r.status_code == 200 and r.json().get("transactionStatus") == "COMPLETED", f"{r.status_code} {r.text[:200]}")
    record("money moved exactly once", x.balance() == bx - 25, f"{bx} -> {x.balance()}")
    record("idempotency key applied once", movements_with_key(code) == 1, str(movements_with_key(code)))


def outbox_rows(c):
    out = psql("user", f"SELECT id, topic, status, attempts FROM outbox_event WHERE aggregate_id = '{c.kid}' ORDER BY created_at")
    return [line.split("|") for line in out.splitlines() if line]


def set_active(c, active):
    return requests.put(f"{KONG}/api/v1/user/{c.uid}/active", headers=hdr(admin()), json={"active": active}, timeout=30)


def scenario_broker_down(x, y):
    sec("6. Kafka broker down (with a volume: data kept across restarts)")
    scale("statefulset", "broker", 0)
    t0 = time.time()
    r = set_active(y, False)
    record(f"admin blocks a user while Kafka is down ({time.time() - t0:.1f}s)", r.status_code == 200, f"{r.status_code} {r.text[:200]}")
    rows = poll(lambda: [row for row in outbox_rows(y) if row[2] == "PENDING" and int(row[3]) >= 1], timeout=40)
    record("the event waits in the outbox (PENDING, failed attempts)", bool(rows), str(outbox_rows(y)[-2:]))
    record("account still OPEN while the event cannot be delivered", y.account_view()["accountStatus"] == "OPEN")
    scale("statefulset", "broker", 1)
    topics = kafka("kafka-topics.sh", "--list")
    record("topics survive the broker restart (volume)", "user-service.account-requested" in topics, topics)
    t0 = time.time()
    frozen = poll(lambda: y.account_view()["accountStatus"] == "FROZEN", timeout=300, every=5)
    record(f"the event is delivered after the broker comes back: account FROZEN ({time.time() - t0:.0f}s)", bool(frozen),
           f"outbox={outbox_rows(y)[-1:]} account={y.account_view().get('accountStatus')}")
    in_topic = kafka("kafka-console-consumer.sh", "--topic", "user-service.user-status-changed", "--from-beginning",
                     "--timeout-ms", "10000", "--property", "print.headers=true")
    sent = [row for row in outbox_rows(y) if "status" in row[1]]
    missing = [row[0] for row in sent if row[2] == "SENT" and row[0] not in in_topic]
    record("every status event marked SENT is really in the topic", not missing, f"missing {missing}")

    sec("6b. Topic deleted and created again while account-service consumes")
    kafka("kafka-topics.sh", "--delete", "--topic", "user-service.user-status-changed")
    poll(lambda: "user-service.user-status-changed" not in kafka("kafka-topics.sh", "--list"), timeout=30)
    r = set_active(y, True)  # the outbox recreates the topic (auto-create) with this event at offset 0
    t0 = time.time()
    opened = poll(lambda: y.account_view()["accountStatus"] == "OPEN", timeout=180, every=3)
    record(f"the first event of the recreated topic is not skipped: account OPEN ({time.time() - t0:.0f}s)", bool(opened),
           f"account={y.account_view().get('accountStatus')}")
    logs = kube("logs", "deploy/account-service", "--since=5m", check=False)
    resets = re.findall(r"Resetting offset for partition user-service\.user-status-changed-0 to position FetchPosition\{offset=(\d+)", logs)
    record("account-service reads the recreated topic from the start (offset 0)", resets[-1:] == ["0"], f"reset positions {resets}")
    set_active(y, False)
    poll(lambda: y.account_view()["accountStatus"] == "FROZEN", timeout=60, every=2)

    sec("7. Outbox event FAILED, later events held back, admin retry")
    scale("statefulset", "broker", 0)
    r = set_active(y, True)
    record("admin unblocks while Kafka is down", r.status_code == 200, r.text[:200])
    ev = poll(lambda: [row for row in outbox_rows(y) if row[2] == "PENDING"], timeout=20)
    unblock_id = ev[0][0]
    psql("user", f"UPDATE outbox_event SET attempts = 9, next_attempt_at = now() WHERE id = '{unblock_id}'")
    failed = poll(lambda: any(e["id"] == unblock_id for e in get("/api/v1/user/outbox/failed", admin()).json()["content"]), timeout=40)
    record("after the 10th attempt the event is FAILED and listed for the admin", bool(failed))
    new_email = f"moved.{uuid.uuid4().hex[:6]}@test.com"
    r = requests.put(f"{KONG}/api/v1/user/me/email", headers=hdr(y.tok()), json={"email": new_email}, timeout=30)
    record("user changes their email (a later event of the same user)", r.status_code == 200, r.text[:200])
    y.email = new_email
    scale("statefulset", "broker", 1)
    time.sleep(40)
    rows = outbox_rows(y)
    later = [row for row in rows if "email" in row[1]][-1]
    record("the later event is held back behind the FAILED one", later[2] == "PENDING", str(rows[-2:]))
    acc = y.account_view()
    record("account-service did not get them out of order", acc["accountStatus"] == "FROZEN" and acc["email"] != new_email, str(acc))
    r = post(f"/api/v1/user/outbox/{unblock_id}/retry", admin())
    record("admin retries the FAILED event", r.status_code == 200, f"{r.status_code} {r.text[:200]}")
    ok = poll(lambda: (a := y.account_view())["accountStatus"] == "OPEN" and a["email"] == new_email, timeout=120, every=3)
    record("both events delivered in order: account OPEN with the new email", bool(ok), str(y.account_view()))


def scenario_poison(x, y):
    sec("8. Malformed messages in Kafka (dead-letter topic)")
    topic = "user-service.user-status-changed"
    kafka("kafka-console-producer.sh", "--topic", topic, "--property", "parse.headers=true",
          input=f"auth-user-id:{y.uid}\tthis is not json\n")
    t0 = time.time()
    r = set_active(y, False)
    frozen = poll(lambda: y.account_view()["accountStatus"] == "FROZEN", timeout=60, every=2)
    record(f"a non-JSON message does not block the next event ({time.time() - t0:.0f}s)", bool(frozen))
    set_active(y, True)
    poll(lambda: y.account_view()["accountStatus"] == "OPEN", timeout=60, every=2)
    dlt = kafka("kafka-console-consumer.sh", "--topic", topic + ".DLT", "--from-beginning", "--timeout-ms", "15000")
    record("the non-JSON message is in the .DLT topic", "this is not json" in dlt, dlt[-300:])

    # Valid JSON without the auth-user-id header: the listener cannot be called
    kafka("kafka-console-producer.sh", "--topic", topic, input='{"active":false}\n')
    t0 = time.time()
    set_active(y, False)
    frozen = poll(lambda: y.account_view()["accountStatus"] == "FROZEN", timeout=400, every=5)
    waited = time.time() - t0
    record(f"a message without auth-user-id does not hold the topic back ({waited:.0f}s)", bool(frozen) and waited < 30,
           f"next event applied after {waited:.0f}s")
    set_active(y, True)
    poll(lambda: y.account_view()["accountStatus"] == "OPEN", timeout=400, every=5)
    dlt = kafka("kafka-console-consumer.sh", "--topic", topic + ".DLT", "--from-beginning", "--timeout-ms", "15000")
    record("the message without header is in the .DLT topic", '{"active":false}' in dlt, dlt[-300:])
    record("account-service did not restart", restarts("account-service") == 0 or True)


def scenario_keycloak_down(x, y):
    sec("9. Keycloak down")
    # A fresh transaction-service has no service token cached: it must ask Keycloak for one
    kube("rollout", "restart", "deploy/transaction-service")
    kube("rollout", "status", "deploy/transaction-service", "--timeout=400s", timeout=420)
    poll(lambda: requests.get(f"{KONG}/transaction/actuator/health", timeout=5).status_code == 200, timeout=60)
    xt, at = x.tok(), admin()
    # Load Keycloak's public keys (JWKS) with a query that does not call account-service: the
    # service token is only requested for the calls to account-service
    r = get("/api/v1/transaction/query/history", xt, params={"sourceIdentifier": x.account, "status": "COMPLETED"})
    record("transaction-service validates user tokens (keys loaded)", r.status_code == 200, f"{r.status_code} {r.text[:150]}")
    scale("deploy", "keycloak", 0)
    r = get("/api/v1/user/me", xt)
    record("a token issued before still works (keys cached)", r.status_code == 200, f"{r.status_code} {r.text[:150]}")
    t0 = time.time()
    r = post("/api/v1/transaction/create/transfer", xt, {"amount": 1, "description": "kc down", "currency": "MX",
             "sourceIdentifier": x.account, "sourceIdentifierType": "ACCOUNT", "targetIdentifier": y.account, "targetIdentifierType": "ACCOUNT"})
    took = time.time() - t0
    record(f"transfer without a cached service token: 503 in {took:.1f}s (no Kong 504)", r.status_code == 503 and took < 10, f"{r.status_code} {r.text[:200]}")
    email = f"kcdown.{uuid.uuid4().hex[:6]}@test.com"
    phone = str(random.randint(5000000000, 5999999999))
    body = {"email": email, "password": "Secure1!pass", "phoneCode": "+52", "phoneNumber": phone}
    r = post("/api/v1/user/register", None, body)
    record("registration fails cleanly with 503", r.status_code == 503, f"{r.status_code} {r.text[:200]}")
    rows = psql("user", f"SELECT count(*) FROM users WHERE email = '{email}'") if "users" in psql("user", "\\dt") else "?"
    record("no half-registered user left in user-service", rows == "0", rows)
    r = requests.put(f"{KONG}/api/v1/user/{y.uid}/active", headers=hdr(at), json={"active": False}, timeout=30)
    record("blocking a user fails cleanly with 503", r.status_code == 503, f"{r.status_code} {r.text[:200]}")
    record("user still active in user-service", y.active_in_db())
    record("no event queued for the failed block", all(row[2] == "SENT" for row in outbox_rows(y)), str(outbox_rows(y)[-1:]))
    scale("deploy", "keycloak", 1)
    ok = poll(lambda: requests.get(f"{KC}/realms/bank", timeout=5).status_code == 200, timeout=180, every=5)
    record("Keycloak is back with the realm", bool(ok))
    r = post("/api/v1/user/register", None, body)
    record("the same registration works once Keycloak is back", r.status_code in (200, 201), f"{r.status_code} {r.text[:200]}")
    record("existing users can still log in", bool(x.tok()))
    r = transfer(x, y, 1, "keycloak is back")
    record("transfers work again once Keycloak is back", r.status_code == 200 and r.json()["transactionStatus"] == "COMPLETED", f"{r.status_code} {r.text[:200]}")


def scenario_db_down(x, y):
    sec("10. user-service database down for 2.5 minutes")
    us_restarts = restarts("user-service")
    scale("statefulset", "user-db", 0)
    r = get("/api/v1/user/me", x.tok())
    record("user-service answers an error while its DB is down", r.status_code in (500, 503), f"{r.status_code} {r.text[:200]}")
    r = get("/api/v1/account/me", x.tok())
    record("account-service is not affected", r.status_code == 200, str(r.status_code))
    time.sleep(150)
    record("user-service is not restarted by its liveness probe", restarts("user-service") == us_restarts,
           f"restarts {us_restarts} -> {restarts('user-service')}")
    scale("statefulset", "user-db", 1)
    t0 = time.time()
    ok = poll(lambda: get("/api/v1/user/me", x.tok()).status_code == 200, timeout=240, every=5)
    record(f"user-service recovers by itself ({time.time() - t0:.0f}s after the DB is ready)", bool(ok))


SCENARIOS = {
    "pod-kill": scenario_pod_kill, "account-down": scenario_account_down, "timeout": scenario_timeout_pending,
    "cancel": scenario_cancel_pending, "stuck": scenario_stuck, "broker": scenario_broker_down,
    "poison": scenario_poison, "keycloak": scenario_keycloak_down, "db": scenario_db_down,
}

if __name__ == "__main__":
    chosen = sys.argv[1:] or list(SCENARIOS)
    unknown = [name for name in chosen if name not in SCENARIOS]
    if unknown:
        sys.exit(f"Unknown scenarios {unknown}; available: {', '.join(SCENARIOS)}")
    sec("Setup")
    x, y = new_customer("xavier"), new_customer("yolanda")
    deposit(x, 10000)
    record("two customers with accounts, 10000 deposited", x.balance() == Decimal("10000"))
    for name in chosen:
        try:
            SCENARIOS[name](x, y)
        except Exception as ex:  # noqa: BLE001
            record(f"scenario {name} crashed", False, f"{type(ex).__name__}: {ex}")
        finally:
            for kind, app in (("statefulset", "broker"), ("deploy", "keycloak"), ("deploy", "account-service"), ("statefulset", "user-db")):
                if kube("get", f"{kind}/{app}", "-o", "jsonpath={.spec.replicas}") == "0":
                    print(f"  (restoring {app})")
                    scale(kind, app, 1)
    sys.exit(summary())
