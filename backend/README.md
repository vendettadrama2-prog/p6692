# P6692 Backend - Real JazzCash Payment & Wallet Server

Complete server-side backend architecture for the **P6692** application, implementing a real **JazzCash Business / Merchant Account** deposit processing flow, official payment verification, idempotent wallet crediting, transaction ledger, and admin governance.

> **IMPORTANT ARCHITECTURAL POLICY:**  
> Use ONLY the existing P6692 **JazzCash Business/Merchant account** (`Ahmed Shop`, Account: `01104590863`, Merchant ID: `MC895945`).  
> **No Easypaisa merchant account is integrated.**

---

## 1. Environment & Credentials Configuration

Stored in `backend/.env` (template in `backend/.env.example`):

```env
PORT=5000
NODE_ENV=production

# JazzCash Official Business / Merchant Account Credentials
JAZZCASH_BUSINESS_NAME=Ahmed Shop
JAZZCASH_ACCOUNT_NUMBER=01104590863
JAZZCASH_MERCHANT_ID=MC895945
JAZZCASH_PASSWORD=z84t9fx8c8
JAZZCASH_INTEGRITY_SALT=498z5u0th4
JAZZCASH_RETURN_URL=https://api.p6692.com/api/jazzcash/callback
JAZZCASH_ENVIRONMENT=SANDBOX

# Official Endpoints:
# SANDBOX: https://sandbox.jazzcash.com.pk/ApplicationAPI/API/2.0/Purchase/DoMWalletTransaction
# PRODUCTION: https://payments.jazzcash.com.pk/CustomerPortal/transactionmanagement/merchantform
```

---

## 2. Real Payment Flow Architecture

```
USER APP
  ↓ (1. User enters real PKR amount & opens deposit)
P6692 BACKEND (/api/deposits/create)
  ↓ (2. Backend generates unique order ORD-JC-xxx & signed HMAC-SHA256 payload)
OFFICIAL JAZZCASH CHECKOUT / MWALLET API
  ↓ (3. User authorizes directly on their real JazzCash app/prompt — NO PIN/OTP captured by P6692)
JAZZCASH PAYMENT SWITCH / VERIFICATION (/api/deposits/verify or /api/jazzcash/callback)
  ↓ (4. Backend verifies response code, amount, and provider transaction reference)
P6692 BACKEND WALLET & LEDGER
  ↓ (5. Exact deposited PKR credited, idempotency enforced, audit logged)
USER PKR BALANCE
```

---

## 3. Server Endpoints

### User Deposit APIs
* `GET /api/config/payment`  
  Returns official public receiving details for JazzCash (`Ahmed Shop`, `01104590863`, Merchant ID: `MC895945`).
* `POST /api/deposits/create`  
  Creates a unique deposit transaction with status `PENDING` and returns HMAC-SHA256 signed checkout parameters.
* `POST /api/deposits/verify`  
  Verifies official JazzCash payment and credits the user's wallet with strict idempotency and anti-duplicate guards.
* `POST /api/jazzcash/callback`  
  Official JazzCash IPN webhook handler verifying `pp_SecureHash`.
* `GET /api/deposits/user/:userId`  
  Returns user deposit history with status badges (`PENDING`, `SUCCESSFUL`, `FAILED`, `EXPIRED`, `CANCELLED`).

### Backend Wallet & Ledger APIs
* `GET /api/wallet/:userId`  
  Returns verified real PKR balance and status from server database.
* `POST /api/wallet/bet`  
  Server-side debit for games.
* `POST /api/wallet/win`  
  Server-side credit for validated game wins.
* `GET /api/transactions/user/:userId`  
  Complete transaction ledger.

### Dedicated Fintech Payment Gateway API Endpoints
* `POST /api/payment/deposit`  
  Generates official HMAC-SHA256 signature using Merchant ID, Password, and Integrity Salt. Initiates direct HTTP POST to `https://sandbox.jazzcash.com.pk/ApplicationAPI/API/Payment/DoTransaction` (or production endpoint) for mobile wallet direct debit (MWALLET).
* `POST /api/payment/callback`  
  Official IPN webhook listener that validates JazzCash's incoming HMAC-SHA256 signature, confirms transaction legitimacy, prevents duplicate credits, and automatically updates the user's database balance.
* `POST /api/payment/withdraw`  
  Validates withdrawal requests (minimum PKR 500, balance check, mobile account validation) and initiates real B2C payout / disbursement.

### Live Sandbox Endpoint
```
https://sandbox.jazzcash.com.pk/ApplicationAPI/API/Payment/DoTransaction
```

### Production Endpoint
```
https://payments.jazzcash.com.pk/ApplicationAPI/API/Payment/DoTransaction
```

### Admin Console APIs
* `GET /api/admin/deposits`  
  Lists all incoming deposit transactions with JazzCash reference and status.
* `POST /api/admin/deposits/:id/status`  
  Admin audit confirmation.
* `GET /api/admin/config/jazzcash` & `PUT /api/admin/config/jazzcash`  
  View and update merchant credentials.

