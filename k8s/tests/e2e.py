"""End-to-end test of every endpoint of the bank system through Kong (kind deployment).

Run against a fresh deployment (k8s/README.md): python3 k8s/tests/e2e.py
"""
import json
import random
import sys
import time
import uuid
from datetime import date
from decimal import Decimal

import requests

from lib import (EUREKA, GRAFANA, KC, KONG, PROM, call, claims, client_token, expect_eq, poll, record, sec, summary,
                 token, verify_email_via_mailpit)

# ---------------------------------------------------------------------------------------------
RUN = uuid.uuid4().hex[:6]
A_EMAIL, A_PASS = f"ana.{RUN}@test.com", "Secure1!pass"
B_EMAIL_TYPED, B_EMAIL, B_PASS = f"John.{RUN}@Test.COM", f"john.{RUN}@test.com", "Secure2!pass"
B_EMAIL_FIXED = f"johndoe.{RUN}@test.com"
_ya, _da, _yb, _db = random.randint(60, 99), random.randint(1, 27), random.randint(60, 99), random.randint(1, 27)
A_CURP, A_RFC, A_BIRTHDAY = f"MALA{_ya:02d}06{_da:02d}MSLRPN09", f"MALA{_ya:02d}06{_da:02d}AB3", f"{_da:02d}/06/19{_ya:02d}"
A_CURP_BAD = f"MALA{_ya:02d}06{_da + 1:02d}MSLRPN09"
B_CURP, B_RFC, B_BIRTHDAY = f"DOEJ{_yb:02d}01{_db:02d}HDFLNS09", f"DOEJ{_yb:02d}01{_db:02d}AB1", f"{_db:02d}/01/19{_yb:02d}"
A_PHONE = "55" + str(int(time.time()))[-8:]
B_PHONE = "56" + str(int(time.time()))[-8:]

sec("Infrastructure")
for svc in ("user", "transaction", "account"):
    call(f"{svc} health through Kong", "GET", f"/{svc}/actuator/health", check=lambda d: expect_eq(d.get("status"), "UP"))
admin = token("admin-user", "admin123")
record("admin token has ADMIN role", "ADMIN" in claims(admin)["realm_access"]["roles"])
apps = requests.get(f"{EUREKA}/eureka/apps", headers={"Accept": "application/json"}, timeout=30).json()
names = sorted(a["name"] for a in apps["applications"]["application"])
record("Eureka has all services registered", names == ["ACCOUNT-SERVICE", "CONFIG-SERVER", "TRANSACTION-SERVICE", "USER-SERVICE"], str(names))
call("no token -> 401", "GET", "/api/v1/user/get-active", expect=401)
call("invalid token -> 401", "GET", "/api/v1/user/get-active", token="not-a-jwt", expect=401)

sec("Registration (user-service)")
call("register A", "POST", "/api/v1/user/register", body={"email": A_EMAIL, "password": A_PASS, "phoneCode": "+52", "phoneNumber": A_PHONE},
     check=lambda d: expect_eq(d.get("registerStatus"), "REGISTER"))
call("register duplicate email (other case) -> 409", "POST", "/api/v1/user/register",
     body={"email": A_EMAIL.upper(), "password": A_PASS, "phoneCode": "+52", "phoneNumber": "5500000001"}, expect=409)
call("register duplicate phone -> 409", "POST", "/api/v1/user/register",
     body={"email": f"x.{RUN}@test.com", "password": A_PASS, "phoneCode": "+52", "phoneNumber": A_PHONE}, expect=409)
call("register invalid body -> 400", "POST", "/api/v1/user/register", body={"email": "not-an-email", "password": "", "phoneCode": "+44", "phoneNumber": "1"}, expect=400)
call("register B (mixed-case email stored lowercase, same number other country code)", "POST", "/api/v1/user/register",
     body={"email": B_EMAIL_TYPED, "password": B_PASS, "phoneCode": "+1", "phoneNumber": A_PHONE},
     check=lambda d: expect_eq(d.get("email"), B_EMAIL, "email"))

a_tok = token(A_EMAIL, A_PASS)
b_tok = token(B_EMAIL, B_PASS)
record("A token has registration_status REGISTER", claims(a_tok).get("registration_status") == "REGISTER", str(claims(a_tok).get("registration_status")))
call("registration incomplete -> 403 outside registration", "GET", "/api/v1/user/me", token=a_tok, expect=403,
     check=lambda d: expect_eq(d.get("error"), "Registration incomplete"))
call("KYC before profile -> 409", "PUT", "/api/v1/user/register/me/kyc", token=a_tok,
     body={"curp": A_CURP, "rfc": A_RFC, "documentType": "NATIONAL_ID_CARD"}, expect=409)
call("resend verification email", "POST", "/api/v1/user/register/me/verify-email", token=a_tok, expect=204)
call("B fixes mistyped email during registration (PUT /user/me/email)", "PUT", "/api/v1/user/me/email", token=b_tok,
     body={"email": B_EMAIL_FIXED}, check=lambda d: expect_eq(d.get("email"), B_EMAIL_FIXED))
B_EMAIL = B_EMAIL_FIXED

profile_a = {"names": "Ana", "firstSurname": "Martinez", "secondSurname": "Lopez", "gender": "FEMALE", "birthday": A_BIRTHDAY, "countryOfBirth": "MX"}
profile_b = {"names": "John", "firstSurname": "Doe", "gender": "MALE", "birthday": B_BIRTHDAY, "countryOfBirth": "US"}
call("profile A", "PUT", "/api/v1/user/register/me/profile", token=a_tok, body=profile_a, expect=204)
b_tok = token(B_EMAIL, B_PASS)
call("profile B without second surname", "PUT", "/api/v1/user/register/me/profile", token=b_tok, body=profile_b, expect=204)
call("profile underage -> 400", "PUT", "/api/v1/user/register/me/profile", token=a_tok,
     body={**profile_a, "birthday": date.today().strftime("%d/%m/%Y")}, expect=[400, 409])
call("KYC with unverified email -> 409", "PUT", "/api/v1/user/register/me/kyc", token=a_tok,
     body={"curp": A_CURP, "rfc": A_RFC, "documentType": "NATIONAL_ID_CARD"}, expect=409)

record("A verifies the email with the link from Mailpit", (r := verify_email_via_mailpit(A_EMAIL)) is True, str(r))
record("B verifies the new email with the link from Mailpit", (r := verify_email_via_mailpit(B_EMAIL)) is True, str(r))

a_tok = token(A_EMAIL, A_PASS)
b_tok = token(B_EMAIL, B_PASS)
call("KYC without documentType -> 400", "PUT", "/api/v1/user/register/me/kyc", token=a_tok,
     body={"curp": A_CURP, "rfc": A_RFC}, expect=400)
call("KYC with CURP not matching birthday -> 400", "PUT", "/api/v1/user/register/me/kyc", token=a_tok,
     body={"curp": A_CURP_BAD, "rfc": A_RFC, "documentType": "NATIONAL_ID_CARD"}, expect=400)
call("KYC A (default SAVING / MX)", "PUT", "/api/v1/user/register/me/kyc", token=a_tok,
     body={"curp": A_CURP, "rfc": A_RFC, "documentType": "NATIONAL_ID_CARD"}, expect=204)
call("KYC B (CHECKING / MX)", "PUT", "/api/v1/user/register/me/kyc", token=b_tok,
     body={"curp": B_CURP, "rfc": B_RFC, "documentType": "PASSPORT", "accountType": "CHECKING", "currency": "MX"}, expect=204)
call("KYC with a CURP already used -> 409", "PUT", "/api/v1/user/register/me/kyc", token=b_tok,
     body={"curp": A_CURP, "rfc": A_RFC, "documentType": "PASSPORT"}, expect=409)

a_tok = token(A_EMAIL, A_PASS)
b_tok = token(B_EMAIL, B_PASS)
record("A token: CUSTOMER_BASIC + KYC", "CUSTOMER_BASIC" in claims(a_tok)["realm_access"]["roles"] and claims(a_tok).get("registration_status") == "KYC")

sec("Account opening (Kafka: user-service outbox -> account-service)")
def account_of(tok):
    r = requests.get(f"{KONG}/api/v1/account/me", headers={"Authorization": f"Bearer {tok}"}, timeout=30)
    return r.json() if r.status_code == 200 else None
acc_a = poll(lambda: account_of(a_tok), timeout=120)
acc_b = poll(lambda: account_of(b_tok), timeout=120)
record("A account opened from the KYC event", bool(acc_a), "no account after 120s")
record("B account opened from the KYC event", bool(acc_b), "no account after 120s")
if not (acc_a and acc_b):
    print("Cannot continue without accounts"); sys.exit(1)
record("A account: SAVING MX OPEN with lowercase email", (acc_a.get("accountType"), acc_a.get("currency"), acc_a.get("accountStatus"), acc_a.get("email")) == ("SAVING", "MX", "OPEN", A_EMAIL), str(acc_a))
record("B account: CHECKING MX with the fixed email", (acc_b.get("accountType"), acc_b.get("currency"), acc_b.get("email")) == ("CHECKING", "MX", B_EMAIL), str(acc_b))
ACC_A, CLABE_A = acc_a["accountNumber"], acc_a["clabe"]
ACC_B, CLABE_B = acc_b["accountNumber"], acc_b["clabe"]

call("financial step A", "PUT", "/api/v1/user/register/me/financial", token=a_tok,
     body={"occupationType": "EMPLOYEE", "employerName": "Tech SA", "incomeSource": "SALARY", "monthlyIncome": 25000.00, "maritalStatus": "SINGLE"}, expect=204)
call("financial step B", "PUT", "/api/v1/user/register/me/financial", token=b_tok,
     body={"occupationType": "SELF_EMPLOYED", "incomeSource": "BUSINESS", "monthlyIncome": 40000.00, "maritalStatus": "MARRIED"}, expect=204)
call("financial step again -> 409", "PUT", "/api/v1/user/register/me/financial", token=b_tok,
     body={"occupationType": "SELF_EMPLOYED", "incomeSource": "BUSINESS", "monthlyIncome": 1, "maritalStatus": "MARRIED"}, expect=409)
a_tok = token(A_EMAIL, A_PASS)
b_tok = token(B_EMAIL, B_PASS)
record("A token: registration COMPLETE", claims(a_tok).get("registration_status") == "COMPLETE")

sec("user-service queries")
me = call("GET /user/me", "GET", "/api/v1/user/me", token=a_tok, check=lambda d: expect_eq(d.get("email"), A_EMAIL))
UID_A = me["id"]
me_b = call("GET /user/me (B)", "GET", "/api/v1/user/me", token=b_tok)
UID_B = me_b["id"]
call("GET /profile/me", "GET", "/api/v1/user/profile/me", token=a_tok, check=lambda d: expect_eq(d.get("secondSurname"), "Lopez"))
call("GET /profile/me (B without second surname)", "GET", "/api/v1/user/profile/me", token=b_tok, check=lambda d: expect_eq(d.get("secondSurname"), None))
call("GET /kyc/me", "GET", "/api/v1/user/kyc/me", token=a_tok)
call("GET /financial-info/me", "GET", "/api/v1/user/financial-info/me", token=a_tok)
call("admin get-by-id", "GET", f"/api/v1/user/get-by-id/{UID_A}", token=admin)
call("admin get-by-email ignoring case", "GET", f"/api/v1/user/get-by-email/{A_EMAIL.upper()}", token=admin, check=lambda d: expect_eq(d.get("id"), UID_A))
call("admin get-active (paged)", "GET", "/api/v1/user/get-active", token=admin, params={"page": 0, "size": 100})
call("admin get-active size>100 -> 400", "GET", "/api/v1/user/get-active", token=admin, params={"size": 101}, expect=400)
call("admin profile get-detail", "GET", f"/api/v1/user/profile/get-detail/{UID_A}", token=admin)
call("admin profile search by name", "GET", "/api/v1/user/profile/search/martinez lo", token=admin, check=lambda d: True if any(c["id"] == UID_A for c in d["content"]) else "A not found")
call("admin profile search finds name without second surname", "GET", "/api/v1/user/profile/search/john doe", token=admin, check=lambda d: True if any(c["id"] == UID_B for c in d["content"]) else "B not found")
call("admin profiles by status COMPLETE", "GET", "/api/v1/user/profile/get-by-status/COMPLETE", token=admin)
call("admin kyc get-detail", "GET", f"/api/v1/user/kyc/get-detail/{UID_A}", token=admin)
call("admin kyc get-by-rfc", "GET", f"/api/v1/user/kyc/get-by-rfc/{A_RFC}", token=admin)
call("admin kyc get-by-curp", "GET", f"/api/v1/user/kyc/get-by-curp/{A_CURP}", token=admin)
call("admin financial get-detail", "GET", f"/api/v1/user/financial-info/get-detail/{UID_A}", token=admin)
for path in (f"/api/v1/user/get-by-id/{UID_A}", "/api/v1/user/get-active", f"/api/v1/user/kyc/get-detail/{UID_A}", "/api/v1/user/outbox/failed"):
    call(f"customer on admin endpoint {path.split('/api/v1/user')[1]} -> 403", "GET", path, token=a_tok, expect=403)
call("change phone", "PUT", "/api/v1/user/me/phone", token=a_tok, body={"phoneCode": "+52", "phoneNumber": "57" + A_PHONE[2:]})
call("change phone to one in use (same code) -> 409", "PUT", "/api/v1/user/me/phone", token=b_tok, body={"phoneCode": "+52", "phoneNumber": "57" + A_PHONE[2:]}, expect=409)
call("outbox failed events (admin)", "GET", "/api/v1/user/outbox/failed", token=admin)
call("outbox retry unknown id -> 404", "POST", f"/api/v1/user/outbox/{uuid.uuid4()}/retry", token=admin, expect=404)

sec("account-service")
call("my balance starts at 0", "GET", "/api/v1/account/me/balance", token=a_tok, check=lambda d: expect_eq(Decimal(str(d["balance"])), Decimal("0")))
call("admin balance of account", "GET", f"/api/v1/account/balance/{ACC_A}", token=admin)
call("customer balance of any account -> 403", "GET", f"/api/v1/account/balance/{ACC_B}", token=a_tok, expect=403)
call("admin get-by-account-number", "GET", f"/api/v1/account/get-by-account-number/{ACC_A}", token=admin)
call("admin get-by-clabe", "GET", f"/api/v1/account/get-by-clabe/{CLABE_A}", token=admin, check=lambda d: expect_eq(d.get("accountNumber"), ACC_A))
call("admin get-by-email ignoring case", "GET", f"/api/v1/account/get-by-email/{A_EMAIL.upper()}", token=admin, check=lambda d: expect_eq(d.get("accountNumber"), ACC_A))
call("customer get-by-account-number -> 403", "GET", f"/api/v1/account/get-by-account-number/{ACC_B}", token=a_tok, expect=403)
call("movement endpoint with customer token -> 403", "POST", "/api/v1/account/movement/deposit", token=a_tok,
     body={"accountNumber": ACC_A, "amount": 1000, "currency": "MX"}, expect=403, extra_headers={"Idempotency-Key": str(uuid.uuid4())})
card = call("issue card", "POST", "/api/v1/account/card", token=a_tok, expect=[200, 201],
            check=lambda d: True if len(d.get("cardNumber", "")) == 16 and len(d.get("cvv", "")) == 3 else "missing number/cvv")
CARD_ID, CARD_NO, CVV = card["id"], card["cardNumber"], card["cvv"]
call("issue second card -> 409", "POST", "/api/v1/account/card", token=a_tok, expect=409)
call("my cards (masked)", "GET", "/api/v1/account/card/me", token=a_tok, check=lambda d: True if CARD_NO not in json.dumps(d) else "full number exposed")
call("admin get-by-card-number", "GET", f"/api/v1/account/get-by-card-number/{CARD_NO}", token=admin, check=lambda d: expect_eq(d.get("accountNumber"), ACC_A))
call("other customer blocks my card -> 403/404", "POST", f"/api/v1/account/card/{CARD_ID}/block", token=b_tok, expect=[403, 404])
call("block card", "POST", f"/api/v1/account/card/{CARD_ID}/block", token=a_tok, check=lambda d: expect_eq(d.get("cardStatus"), "BLOCKED"))
call("unblock card", "POST", f"/api/v1/account/card/{CARD_ID}/unblock", token=a_tok, check=lambda d: expect_eq(d.get("cardStatus"), "ACTIVE"))
call("admin sets TRANSFER DAILY limit 2000 / 5", "PUT", "/api/v1/account/limit", token=admin,
     body={"accountNumber": ACC_A, "limitType": "TRANSFER", "timePeriod": "DAILY", "maxAmount": 2000.00, "maxTransactions": 5})
call("customer sets a limit -> 403", "PUT", "/api/v1/account/limit", token=a_tok,
     body={"accountNumber": ACC_A, "limitType": "TRANSFER", "timePeriod": "DAILY", "maxAmount": 999999, "maxTransactions": 99}, expect=403)
limits = call("my limits", "GET", "/api/v1/account/limit/me", token=a_tok, check=lambda d: True if len(d) == 1 else f"{len(d)} limits")
call("admin limits of account", "GET", f"/api/v1/account/limit/{ACC_A}", token=admin)

sec("transaction-service: creation")
def tx(name, kind, body, tok=a_tok, status="COMPLETED", expect=200):
    return call(name, "POST", f"/api/v1/transaction/create/{kind}", token=tok, body=body, expect=expect,
                check=(lambda d: expect_eq(d.get("transactionStatus"), status, "status")) if status else None)

dep = tx("admin deposit 5000 into A by CLABE", "deposit", {"amount": 5000.00, "description": "Cash deposit", "targetIdentifier": CLABE_A, "targetIdentifierType": "CLABE", "currency": "MX"}, tok=admin)
tx("customer deposit -> 403", "deposit", {"amount": 5, "description": "x", "targetIdentifier": ACC_A, "targetIdentifierType": "ACCOUNT", "currency": "MX"}, status=None, expect=403)
tx("deposit in another currency -> DECLINED", "deposit", {"amount": 10, "description": "usd", "targetIdentifier": ACC_A, "targetIdentifierType": "ACCOUNT", "currency": "US"}, tok=admin, status="DECLINED")
t1 = tx("transfer A->B 1500 by account number", "transfer", {"amount": 1500.00, "description": "February rent", "currency": "MX", "sourceIdentifier": ACC_A, "sourceIdentifierType": "ACCOUNT", "targetIdentifier": ACC_B, "targetIdentifierType": "ACCOUNT"})
tx("transfer A->B 100 CLABE to CLABE", "transfer", {"amount": 100.00, "description": "Payment with CLABE 100% ok", "currency": "MX", "sourceIdentifier": CLABE_A, "sourceIdentifierType": "CLABE", "targetIdentifier": CLABE_B, "targetIdentifierType": "CLABE"})
tx("transfer over the daily limit -> DECLINED", "transfer", {"amount": 600.00, "description": "Over the limit", "currency": "MX", "sourceIdentifier": ACC_A, "sourceIdentifierType": "ACCOUNT", "targetIdentifier": ACC_B, "targetIdentifierType": "ACCOUNT"}, status="DECLINED")
tx("transfer from another customer's account -> 403", "transfer", {"amount": 1, "description": "steal", "currency": "MX", "sourceIdentifier": ACC_B, "sourceIdentifierType": "ACCOUNT", "targetIdentifier": ACC_A, "targetIdentifierType": "ACCOUNT"}, status=None, expect=403)
tx("transfer to the same account -> 400", "transfer", {"amount": 1, "description": "self", "currency": "MX", "sourceIdentifier": ACC_A, "sourceIdentifierType": "ACCOUNT", "targetIdentifier": CLABE_A, "targetIdentifierType": "CLABE"}, status=None, expect=400)
tx("transfer to unknown account -> 404", "transfer", {"amount": 1, "description": "nobody", "currency": "MX", "sourceIdentifier": ACC_A, "sourceIdentifierType": "ACCOUNT", "targetIdentifier": "ACC-0000000000000000", "targetIdentifierType": "ACCOUNT"}, status=None, expect=404)
tx("transfer negative amount -> 400", "transfer", {"amount": -5, "description": "neg", "currency": "MX", "sourceIdentifier": ACC_A, "sourceIdentifierType": "ACCOUNT", "targetIdentifier": ACC_B, "targetIdentifierType": "ACCOUNT"}, status=None, expect=400)
pur = tx("card purchase 349.99", "purchase", {"amount": 349.99, "description": "Department store", "sourceAccountNumber": ACC_A, "sourceCardNumber": CARD_NO, "cvv": CVV, "merchantCode": "MERC-001", "merchantCategory": "RETAIL", "posId": "POS-1", "currency": "MX"})
wrong_cvv = "000" if CVV != "000" else "111"
tx("card purchase with wrong cvv -> DECLINED", "purchase", {"amount": 10, "description": "bad cvv", "sourceAccountNumber": ACC_A, "sourceCardNumber": CARD_NO, "cvv": wrong_cvv, "merchantCode": "MERC-001", "currency": "MX"}, status="DECLINED")
tx("service payment 875.50", "service-payment", {"amount": 875.50, "description": "Electricity bill", "sourceAccountNumber": ACC_A, "paymentReference": "LUZ-1", "serviceType": "ELECTRICITY", "currency": "MX"})
tx("service payment without funds -> DECLINED", "service-payment", {"amount": 999999.00, "description": "Too much", "sourceAccountNumber": ACC_A, "paymentReference": "LUZ-2", "serviceType": "ELECTRICITY", "currency": "MX"}, status="DECLINED")
tx("withdrawal 1000 + fee 25", "withdrawal", {"amount": 1000.00, "description": "ATM", "sourceAccountNumber": ACC_A, "atmLocation": "CDMX", "transactionFee": 25.00, "currency": "MX", "securityVerificationMethod": "PIN", "withdrawalMethod": "DEBIT_CARD"})
expected_a = Decimal("5000") - Decimal("1500") - Decimal("100") - Decimal("349.99") - Decimal("875.50") - Decimal("1025")
call(f"A balance is {expected_a}", "GET", "/api/v1/account/me/balance", token=a_tok, check=lambda d: expect_eq(Decimal(str(d["balance"])), expected_a))
call("B balance is 1600", "GET", "/api/v1/account/me/balance", token=b_tok, check=lambda d: expect_eq(Decimal(str(d["balance"])), Decimal("1600")))

sec("transaction-service: queries")
Q = "/api/v1/transaction/query"
base = {"sourceIdentifier": ACC_A, "status": "COMPLETED"}
call("history", "GET", f"{Q}/history", token=a_tok, params=base, check=lambda d: True if len(d) == 6 else f"{len(d)} transactions")
call("history by CLABE", "GET", f"{Q}/history", token=a_tok, params={**base, "sourceIdentifier": CLABE_A, "sourceIdentifierType": "CLABE"}, check=lambda d: True if len(d) == 6 else f"{len(d)}")
call("history by CARD", "GET", f"{Q}/history", token=a_tok, params={**base, "sourceIdentifier": CARD_NO, "sourceIdentifierType": "CARD"}, check=lambda d: True if len(d) == 6 else f"{len(d)}")
call("history DECLINED", "GET", f"{Q}/history", token=a_tok, params={**base, "status": "DECLINED"}, check=lambda d: True if len(d) == 4 else f"{len(d)}")
call("receiver B sees the incoming transfers as CREDIT", "GET", f"{Q}/history", token=b_tok, params={"sourceIdentifier": ACC_B, "status": "COMPLETED"},
     check=lambda d: True if len(d) == 2 and all(t["direction"] == "CREDIT" for t in d) else str([(t["transactionType"], t["direction"]) for t in d]))
call("B cannot read A's history (empty)", "GET", f"{Q}/history", token=b_tok, params=base, check=lambda d: expect_eq(len(d), 0))
call("all (paged)", "GET", f"{Q}/all", token=a_tok, params={**base, "page": 0, "size": 2}, check=lambda d: expect_eq(d["page"]["totalElements"], 6))
call("all invalid page -> 400", "GET", f"{Q}/all", token=a_tok, params={**base, "page": -1}, expect=400)
call("status-year", "GET", f"{Q}/status-year", token=a_tok, params={**base, "year": date.today().year}, check=lambda d: expect_eq(len(d), 6))
call("max", "GET", f"{Q}/max", token=a_tok, params=base, check=lambda d: expect_eq(Decimal(str(d["amount"])), Decimal("5000")))
call("date-range", "GET", f"{Q}/date-range", token=a_tok, params={**base, "startDate": f"{date.today().year}-01-01T00:00:00", "endDate": f"{date.today().year}-12-31T23:59:59"}, check=lambda d: expect_eq(len(d), 6))
call("date-range inverted -> 400", "GET", f"{Q}/date-range", token=a_tok, params={**base, "startDate": "2030-01-01T00:00:00", "endDate": "2020-01-01T00:00:00"}, expect=400)
call("amount-range", "GET", f"{Q}/amount-range", token=a_tok, params={**base, "minAmount": 300, "maxAmount": 1000}, check=lambda d: expect_eq(len(d), 3))
call("amount-range inverted -> 400", "GET", f"{Q}/amount-range", token=a_tok, params={**base, "minAmount": 10, "maxAmount": 1}, expect=400)
code = t1["transactionCode"]
call("by transaction code (sender: DEBIT)", "GET", f"{Q}/{code}", token=a_tok, check=lambda d: expect_eq(d.get("direction"), "DEBIT"))
call("by transaction code (receiver: CREDIT)", "GET", f"{Q}/{code}", token=b_tok, check=lambda d: expect_eq(d.get("direction"), "CREDIT"))
call("unknown transaction code -> 404", "GET", f"{Q}/{uuid.uuid4()}", token=a_tok, expect=404)
call("identifier-type CLABE (deposit + transfer by CLABE)", "GET", f"{Q}/identifier-type", token=a_tok, params={**base, "identifierType": "CLABE"}, check=lambda d: expect_eq(len(d), 2))
call("keyword", "GET", f"{Q}/keyword", token=a_tok, params={**base, "keyword": "RENT"}, check=lambda d: expect_eq(len(d), 1))
call("keyword % is literal", "GET", f"{Q}/keyword", token=a_tok, params={**base, "keyword": "%"}, check=lambda d: expect_eq(len(d), 1))
call("by-accounts (admin)", "GET", f"{Q}/by-accounts", token=admin, params={"accountNumbers": [ACC_A, ACC_B]}, check=lambda d: True if len(d) == 6 else f"{len(d)}")
call("by-accounts as customer -> 403", "GET", f"{Q}/by-accounts", token=a_tok, params={"accountNumbers": ACC_A}, expect=403)

sec("transaction-service: analytics and support")
AN = "/api/v1/transaction/analytics"
call("group-by-type", "GET", f"{AN}/group-by-type", token=admin, params=base, check=lambda d: True if set(d) == {"DEPOSIT", "TRANSFER", "PURCHASE", "SERVICE", "WITHDRAWAL"} else str(set(d)))
call("sum-by-type", "GET", f"{AN}/sum-by-type", token=a_tok, params=base, check=lambda d: expect_eq(Decimal(str(d["TRANSFER"])), Decimal("1600")))
call("partition-by-amount", "GET", f"{AN}/partition-by-amount", token=admin, params={**base, "amount": 1000}, check=lambda d: expect_eq(len(d["true"]), 2))
call("type-summary", "GET", f"{AN}/type-summary", token=admin, params=base, check=lambda d: expect_eq(d["TRANSFER"]["count"], 2))
call("total-amount", "GET", f"{AN}/total-amount", token=a_tok, params={**base, "type": "TRANSFER"}, check=lambda d: expect_eq(Decimal(str(d)), Decimal("1600")))
call("average-days", "GET", f"{AN}/average-days", token=admin, params=base)
call("max-by-type", "GET", f"{AN}/max-by-type", token=admin, params=base, check=lambda d: expect_eq(Decimal(str(d["TRANSFER"]["amount"])), Decimal("1500")))
call("count-by-month (year-month keys)", "GET", f"{AN}/count-by-month", token=a_tok, params=base, check=lambda d: expect_eq(d.get(date.today().strftime("%Y-%m")), 6))
call("average-amount", "GET", f"{AN}/average-amount", token=a_tok, params=base, check=lambda d: expect_eq(Decimal(str(d["TRANSFER"])), Decimal("800")))
call("receiver totals do not include incoming transfers", "GET", f"{AN}/sum-by-type", token=b_tok, params={"sourceIdentifier": ACC_B, "status": "COMPLETED"}, check=lambda d: expect_eq(d, {}))
for p in ("group-by-type", "type-summary", "average-days", "max-by-type"):
    call(f"customer on admin analytics /{p} -> 403", "GET", f"{AN}/{p}", token=a_tok, params=base, expect=403)
call("support unique descriptions", "GET", f"/api/v1/transaction/support/unique-descriptions/{ACC_A}", token=a_tok, check=lambda d: True if "rent" in d else str(d))
call("support all descriptions", "GET", f"/api/v1/transaction/support/all-descriptions/{ACC_A}", token=a_tok, check=lambda d: True if "Electricity bill" in d else str(d))

sec("transaction-service: lifecycle")
L = "/api/v1/transaction"
call("customer reverse -> 403", "POST", f"{L}/{pur['transactionCode']}/reverse", token=a_tok, expect=403)
call("admin reverses the purchase", "POST", f"{L}/{pur['transactionCode']}/reverse", token=admin, check=lambda d: expect_eq(d.get("transactionStatus"), "REVERSED"))
call("reverse twice -> 409", "POST", f"{L}/{pur['transactionCode']}/reverse", token=admin, expect=409)
expected_a += Decimal("349.99")
call(f"A balance after the reversal is {expected_a}", "GET", "/api/v1/account/me/balance", token=a_tok, check=lambda d: expect_eq(Decimal(str(d["balance"])), expected_a))
call("cancel a COMPLETED transaction -> 409", "POST", f"{L}/{code}/cancel", token=a_tok, expect=409)
call("cancel transaction of another user -> 404", "POST", f"{L}/{dep['transactionCode']}/cancel", token=b_tok, expect=404)
call("stuck transactions (admin)", "GET", f"{L}/stuck", token=admin, check=lambda d: expect_eq(d, []))
call("stuck as customer -> 403", "GET", f"{L}/stuck", token=a_tok, expect=403)
call("retry a transaction that is not stuck -> 409", "POST", f"{L}/{code}/retry", token=admin, expect=409)
call("retry unknown -> 404", "POST", f"{L}/{uuid.uuid4()}/retry", token=admin, expect=404)
call("block card again", "POST", f"/api/v1/account/card/{CARD_ID}/block", token=a_tok)
tx("purchase with a blocked card -> DECLINED", "purchase", {"amount": 10, "description": "blocked", "sourceAccountNumber": ACC_A, "sourceCardNumber": CARD_NO, "cvv": CVV, "merchantCode": "MERC-001", "currency": "MX"}, status="DECLINED")
call("cancel card", "POST", f"/api/v1/account/card/{CARD_ID}/cancel", token=a_tok, check=lambda d: expect_eq(d.get("cardStatus"), "CANCELLED"))
call("issue a new card after cancelling", "POST", "/api/v1/account/card", token=a_tok, expect=[200, 201])
call("admin removes the limit", "DELETE", f"/api/v1/account/limit/{limits[0]['id']}", token=admin, expect=204)
tx("transfer 600 allowed once the limit is removed", "transfer", {"amount": 600.00, "description": "After limit", "currency": "MX", "sourceIdentifier": ACC_A, "sourceIdentifierType": "ACCOUNT", "targetIdentifier": ACC_B, "targetIdentifierType": "ACCOUNT"})

sec("Admin user management (Keycloak + Kafka propagation)")
call("role CUSTOMER_PREMIUM", "PUT", f"/api/v1/user/{UID_A}/role", token=admin, body={"role": "CUSTOMER_PREMIUM"}, check=lambda d: expect_eq(d.get("role"), "CUSTOMER_PREMIUM"))
call("role ADMIN through the API -> 400", "PUT", f"/api/v1/user/{UID_A}/role", token=admin, body={"role": "ADMIN"}, expect=400)
a_tok = token(A_EMAIL, A_PASS)
record("A token has CUSTOMER_PREMIUM and keeps CUSTOMER_BASIC", {"CUSTOMER_PREMIUM", "CUSTOMER_BASIC"} <= set(claims(a_tok)["realm_access"]["roles"]))
call("block B", "PUT", f"/api/v1/user/{UID_B}/active", token=admin, body={"active": False}, check=lambda d: expect_eq(d.get("active"), False))
call("blocked B rejected by user-service (old token)", "GET", "/api/v1/user/me", token=b_tok, expect=403, check=lambda d: expect_eq(d.get("error"), "User blocked"))
record("blocked B cannot log in", requests.post(f"{KC}/realms/bank/protocol/openid-connect/token", data={"grant_type": "password", "client_id": "kong", "client_secret": "kong-client-secret", "username": B_EMAIL, "password": B_PASS}, timeout=30).status_code in (400, 401))
frozen = poll(lambda: requests.get(f"{KONG}/api/v1/account/get-by-account-number/{ACC_B}", headers={"Authorization": f"Bearer {admin}"}, timeout=30).json().get("accountStatus") == "FROZEN", timeout=90)
record("B account FROZEN through Kafka", bool(frozen))
tx("transfer to a frozen account -> DECLINED", "transfer", {"amount": 1, "description": "to frozen", "currency": "MX", "sourceIdentifier": ACC_A, "sourceIdentifierType": "ACCOUNT", "targetIdentifier": ACC_B, "targetIdentifierType": "ACCOUNT"}, status="DECLINED")
call("unblock B", "PUT", f"/api/v1/user/{UID_B}/active", token=admin, body={"active": True})
reopened = poll(lambda: requests.get(f"{KONG}/api/v1/account/get-by-account-number/{ACC_B}", headers={"Authorization": f"Bearer {admin}"}, timeout=30).json().get("accountStatus") == "OPEN", timeout=90)
record("B account OPEN again through Kafka", bool(reopened))
NEW_A = f"ana.new.{RUN}@test.com"
call("A changes email after KYC", "PUT", "/api/v1/user/me/email", token=a_tok, body={"email": NEW_A.upper()}, check=lambda d: expect_eq(d.get("email"), NEW_A))
propagated = poll(lambda: requests.get(f"{KONG}/api/v1/account/get-by-account-number/{ACC_A}", headers={"Authorization": f"Bearer {admin}"}, timeout=30).json().get("email") == NEW_A, timeout=90)
record("new email propagated to the account through Kafka", bool(propagated))
record("A logs in with the new email", bool(token(NEW_A, A_PASS)))
call("email already in use -> 409", "PUT", "/api/v1/user/me/email", token=token(NEW_A, A_PASS), body={"email": B_EMAIL}, expect=409)

sec("Monitoring")
prom_tok = client_token("prometheus", "prometheus-secret")
call("metrics without token -> 401", "GET", "/user/actuator/prometheus", expect=401)
call("metrics with customer token -> 403", "GET", "/account/actuator/prometheus", token=token(NEW_A, A_PASS), expect=403)
call("metrics with Prometheus' token", "GET", "/transaction/actuator/prometheus", token=prom_tok, check=lambda d: True if "bank_transaction_attempts_total" in d else "business metric missing")
targets = requests.get(f"{PROM}/api/v1/targets", timeout=30).json()["data"]["activeTargets"]
up = sorted(t["labels"]["application"] for t in targets if t["health"] == "up")
record("Prometheus scrapes the 5 applications", up == ["account-service", "config-server", "eureka-server", "transaction-service", "user-service"], str([(t["labels"].get("application"), t["health"], t["lastError"][:60]) for t in targets]))
q = poll(lambda: len(r := requests.get(f"{PROM}/api/v1/query", params={"query": "sum by (type, status) (bank_transaction_attempts_total)"}, timeout=30).json()["data"]["result"]) >= 4 and r, timeout=60) or []
record("Prometheus has the transaction metrics", len(q) >= 4, str(q)[:200])
record("Grafana healthy", requests.get(f"{GRAFANA}/api/health", timeout=30).json().get("database") == "ok")
ds = requests.get(f"{GRAFANA}/api/datasources/uid/prometheus/health", auth=("admin", "admin"), timeout=30).json()
record("Grafana datasource reaches Prometheus", ds.get("status") == "OK", str(ds))
dash = requests.get(f"{GRAFANA}/api/dashboards/uid/bank-services", auth=("admin", "admin"), timeout=30)
record("Grafana dashboard provisioned", dash.status_code == 200 and len(dash.json()["dashboard"]["panels"]) == 17, str(dash.status_code))

# ---------------------------------------------------------------------------------------------
sys.exit(summary())
