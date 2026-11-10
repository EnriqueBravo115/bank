"""Helpers shared by e2e.py and faults.py: URLs of the kind deployment, checks, tokens, polling."""
import html
import base64
import json
import os
import re
import time

import requests

# Host ports of k8s/kind-config.yaml
KONG = os.environ.get("KONG_URL", "http://localhost:8000")
KC = os.environ.get("KEYCLOAK_URL", "http://localhost:9090")
MAILPIT = os.environ.get("MAILPIT_URL", "http://localhost:8025")
PROM = os.environ.get("PROMETHEUS_URL", "http://localhost:9091")
GRAFANA = os.environ.get("GRAFANA_URL", "http://localhost:3000")
EUREKA = os.environ.get("EUREKA_URL", "http://localhost:8761")

results = []  # (section, name, ok, detail)
section = ""


def sec(name):
    global section
    section = name
    print(f"\n=== {name}")


def record(name, ok, detail=""):
    results.append((section, name, ok, detail))
    print(f"  {'OK  ' if ok else 'FAIL'} {name}" + ("" if ok else f"  -> {detail}"))
    return ok


def call(name, method, path, token=None, expect=200, body=None, params=None, check=None, extra_headers=None):
    headers = {"Authorization": f"Bearer {token}"} if token else {}
    headers.update(extra_headers or {})
    try:
        r = requests.request(method, KONG + path, headers=headers, json=body, params=params, timeout=30)
    except Exception as ex:  # noqa: BLE001
        record(name, False, f"{type(ex).__name__}: {ex}")
        return None
    expected = expect if isinstance(expect, (list, tuple, set)) else [expect]
    ok = r.status_code in expected
    detail = f"{method} {path} -> {r.status_code} (expected {expect}) {r.text[:300]}"
    data = None
    try:
        data = r.json()
    except ValueError:
        data = r.text
    if ok and check is not None:
        try:
            res = check(data)
            if res is not True and res is not None:
                ok, detail = False, f"{method} {path}: check failed: {res} | body={str(data)[:300]}"
            elif res is None:
                pass
        except Exception as ex:  # noqa: BLE001
            ok, detail = False, f"{method} {path}: check raised {type(ex).__name__}: {ex} | body={str(data)[:300]}"
    record(name, ok, detail)
    return data


def expect_eq(actual, expected, what=""):
    return True if actual == expected else f"{what} expected {expected!r}, got {actual!r}"


def token(username, password):
    r = requests.post(f"{KC}/realms/bank/protocol/openid-connect/token", data={
        "grant_type": "password", "client_id": "kong", "client_secret": "kong-client-secret",
        "username": username, "password": password}, timeout=30)
    r.raise_for_status()
    return r.json()["access_token"]


def client_token(client_id, secret):
    r = requests.post(f"{KC}/realms/bank/protocol/openid-connect/token", data={
        "grant_type": "client_credentials", "client_id": client_id, "client_secret": secret}, timeout=30)
    r.raise_for_status()
    return r.json()["access_token"]


def claims(tok):
    part = tok.split(".")[1]
    part += "=" * (-len(part) % 4)
    return json.loads(base64.urlsafe_b64decode(part))


def poll(fn, timeout=90, every=2):
    end = time.time() + timeout
    last = None
    while time.time() < end:
        try:
            last = fn()
        except requests.RequestException:
            last = None
        if last:
            return last
        time.sleep(every)
    return last


def kc_admin():
    r = requests.post(f"{KC}/realms/master/protocol/openid-connect/token", data={
        "grant_type": "password", "client_id": "admin-cli", "username": "admin", "password": "admin"}, timeout=30)
    r.raise_for_status()
    return r.json()["access_token"]


def kc_user(email):
    users = requests.get(f"{KC}/admin/realms/bank/users", params={"email": email, "exact": "true"},
                         headers={"Authorization": f"Bearer {kc_admin()}"}, timeout=30).json()
    return users[0] if users else None


def verify_email_via_mailpit(email):
    """Opens the link of the verification email, as the user would."""
    def find():
        msgs = requests.get(f"{MAILPIT}/api/v1/search", params={"query": f"to:{email}"}, timeout=30).json()
        return msgs.get("messages") or None
    msgs = poll(find, timeout=60)
    if not msgs:
        return "no verification email received"
    msg = requests.get(f"{MAILPIT}/api/v1/message/{msgs[0]['ID']}", timeout=30).json()
    links = re.findall(r"https?://[^\s\"<>]+action-token[^\s\"<>]+", msg.get("Text", "") + msg.get("HTML", ""))
    if not links:
        return "no action-token link in the email"
    session = requests.Session()
    r = session.get(html.unescape(links[0]), timeout=30)
    # Keycloak marks its cookies Secure; browsers send them to localhost over http, requests does not
    for c in session.cookies:
        c.secure = False
    # Keycloak may ask to confirm ("Click here to proceed")
    proceed = re.search(r'href="([^"]*login-actions[^"]*)"', r.text)
    if proceed:
        session.get(html.unescape(proceed.group(1)), timeout=30)
    user = poll(lambda: (u := kc_user(email)) and u.get("emailVerified") and u, timeout=20)
    return True if user else "email still not verified in Keycloak after opening the link"


def summary():
    """Prints the failed checks and returns the exit code."""
    failed = [r for r in results if not r[2]]
    print(f"\n==== {len(results) - len(failed)}/{len(results)} checks passed")
    for s, n, _, d in failed:
        print(f"FAIL [{s}] {n}: {d}")
    return 1 if failed else 0
