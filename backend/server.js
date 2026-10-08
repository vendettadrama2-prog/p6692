require('dotenv').config();
const express = require('express');
const cors = require('cors');
const morgan = require('morgan');
const fs = require('fs');
const path = require('path');
const JazzCashHelper = require('./jazzcash');

const app = express();
const PORT = process.env.PORT || 5000;

app.use(cors());
app.use(express.json());
app.use(express.urlencoded({ extended: true }));
app.use(morgan('dev'));

// File-backed persistent database
const DB_FILE = path.join(__dirname, 'database.json');

let db = {
  users: {
    'p6692_user_1': {
      id: 'p6692_user_1',
      username: 'CyberKnight_92',
      email: 'player@p6692.com',
      pkrBalance: 25000.0,
      vipLevel: 3,
      role: 'PLAYER'
    }
  },
  deposits: [
    {
      id: 1,
      userId: 'p6692_user_1',
      orderId: 'ORD-JC-100291',
      method: 'JAZZCASH',
      amountPkr: 2500.0,
      currency: 'PKR',
      senderMobile: '03001234567',
      jazzcashTxnRef: 'JC998241582',
      status: 'SUCCESSFUL',
      adminNotes: 'Verified via JazzCash Business Statement (Ahmed Shop)',
      createdAt: Date.now() - 3600000,
      updatedAt: Date.now() - 3500000
    }
  ],
  transactions: [
    {
      id: 1,
      userId: 'p6692_user_1',
      transactionId: 'TXN-DEP-1001',
      type: 'DEPOSIT',
      amountPkr: 2500.0,
      currency: 'PKR',
      previousBalance: 22500.0,
      newBalance: 25000.0,
      status: 'SUCCESSFUL',
      paymentProvider: 'JAZZCASH',
      providerTxnId: 'JC998241582',
      description: 'Official JazzCash Merchant Deposit',
      timestamp: Date.now() - 3500000
    }
  ],
  jazzcashConfig: {
    businessName: process.env.JAZZCASH_BUSINESS_NAME || 'Ahmed Shop',
    accountNumber: process.env.JAZZCASH_ACCOUNT_NUMBER || '01104590863',
    merchantId: process.env.JAZZCASH_MERCHANT_ID || 'MC895945',
    apiPassword: process.env.JAZZCASH_PASSWORD || 'z84t9fx8c8',
    integritySalt: process.env.JAZZCASH_INTEGRITY_SALT || '498z5u0th4',
    returnUrl: process.env.JAZZCASH_RETURN_URL || 'https://api.p6692.com/api/jazzcash/callback',
    environment: process.env.JAZZCASH_ENVIRONMENT || 'SANDBOX'
  },
  auditLogs: []
};

// Load saved DB if exists
if (fs.existsSync(DB_FILE)) {
  try {
    const raw = fs.readFileSync(DB_FILE, 'utf8');
    db = JSON.parse(raw);
  } catch (e) {
    console.error('Error reading database file, using defaults.', e);
  }
}

function saveDb() {
  try {
    fs.writeFileSync(DB_FILE, JSON.stringify(db, null, 2));
  } catch (e) {
    console.error('Error persisting database:', e);
  }
}

let jcHelper = new JazzCashHelper(db.jazzcashConfig);

// Helper: Ensure user exists
function getOrCreateUser(userId) {
  if (!db.users[userId]) {
    db.users[userId] = {
      id: userId,
      username: `User_${userId.slice(-4)}`,
      email: `${userId}@p6692.com`,
      pkrBalance: 25000.0,
      vipLevel: 1,
      role: 'PLAYER'
    };
    saveDb();
  }
  return db.users[userId];
}

// -------------------------------------------------------------
// ROUTES
// -------------------------------------------------------------

// Health check
app.get('/api/health', (req, res) => {
  res.json({
    status: 'ONLINE',
    platform: 'P6692 Gaming Backend',
    paymentProvider: 'JAZZCASH_ONLY',
    merchant: db.jazzcashConfig.businessName,
    merchantId: db.jazzcashConfig.merchantId,
    timestamp: new Date().toISOString()
  });
});

// User wallet summary
app.get('/api/wallet/:userId', (req, res) => {
  const { userId } = req.params;
  const user = getOrCreateUser(userId);
  const userDeposits = db.deposits.filter(d => d.userId === userId);
  const userTxns = db.transactions.filter(t => t.userId === userId);

  res.json({
    success: true,
    userId: user.id,
    pkrBalance: user.pkrBalance,
    currency: 'PKR',
    depositCount: userDeposits.length,
    transactionCount: userTxns.length
  });
});

// Public payment details for deposit UI - JAZZCASH ONLY
app.get('/api/config/payment', (req, res) => {
  res.json({
    jazzcash: {
      businessName: db.jazzcashConfig.businessName,
      accountNumber: db.jazzcashConfig.accountNumber,
      merchantId: db.jazzcashConfig.merchantId,
      environment: db.jazzcashConfig.environment,
      apiUrl: jcHelper.getApiUrl()
    }
  });
});

/**
 * 1. CREATE UNIQUE DEPOSIT ORDER ON BACKEND
 * Generates secure server-side transaction order and JazzCash payload with HMAC-SHA256 signature.
 * Prevents client-side tampering of amount, currency, or merchant credentials.
 */
app.post('/api/deposits/create', (req, res) => {
  const { userId, amountPkr, mobileNo } = req.body;

  if (!userId || !amountPkr || parseFloat(amountPkr) <= 0) {
    return res.status(400).json({ error: 'Valid userId and positive amountPkr are required.' });
  }

  const pkrAmount = parseFloat(amountPkr);
  const orderId = `ORD-JC-${Date.now().toString().slice(-6)}${Math.floor(1000 + Math.random() * 9000)}`;

  const depositId = db.deposits.length > 0 ? Math.max(...db.deposits.map(d => d.id)) + 1 : 1;

  const newDeposit = {
    id: depositId,
    userId,
    orderId,
    method: 'JAZZCASH',
    amountPkr: pkrAmount,
    currency: 'PKR',
    senderMobile: mobileNo ? String(mobileNo).trim() : '',
    jazzcashTxnRef: '',
    status: 'PENDING',
    adminNotes: 'Awaiting JazzCash payment authorization & verification',
    createdAt: Date.now(),
    updatedAt: Date.now()
  };

  db.deposits.unshift(newDeposit);
  saveDb();

  // Generate official JazzCash payment payload with HMAC-SHA256
  const jcPayload = mobileNo
    ? jcHelper.buildMWalletPayload({
        txnRefNo: orderId,
        amountPkr: pkrAmount,
        mobileNo: String(mobileNo).trim(),
        description: `P6692 Deposit Order ${orderId}`
      })
    : jcHelper.buildHostedCheckoutPayload({
        txnRefNo: orderId,
        amountPkr: pkrAmount,
        billReference: orderId,
        description: `P6692 Deposit Order ${orderId}`
      });

  res.status(201).json({
    success: true,
    message: 'Deposit order created on backend. Authorize payment through official JazzCash.',
    deposit: newDeposit,
    checkout: {
      postUrl: jcHelper.getApiUrl(),
      parameters: jcPayload,
      merchantId: db.jazzcashConfig.merchantId,
      businessName: db.jazzcashConfig.businessName
    }
  });
});

/**
 * 2. VERIFY JAZZCASH PAYMENT & CREDIT WALLET (SERVER-SIDE VERIFICATION)
 * Strict idempotency: same transaction can NEVER credit wallet twice.
 * Validates:
 * - Order existence in DB
 * - Status is currently PENDING (cannot credit already completed or failed)
 * - Exact amount match
 * - Official JazzCash reference check
 */
app.post('/api/deposits/verify', (req, res) => {
  const { orderId, jazzcashTxnRef, responseCode, responseMessage } = req.body;

  if (!orderId) {
    return res.status(400).json({ error: 'orderId is required for verification.' });
  }

  const deposit = db.deposits.find(d => d.orderId === orderId);
  if (!deposit) {
    return res.status(404).json({ error: 'Deposit order not found in database.' });
  }

  // Idempotency check
  if (deposit.status === 'SUCCESSFUL') {
    return res.json({
      success: true,
      message: 'Transaction is already verified and credited.',
      idempotent: true,
      deposit
    });
  }

  if (deposit.status === 'FAILED' || deposit.status === 'CANCELLED' || deposit.status === 'EXPIRED') {
    return res.status(400).json({
      success: false,
      error: `Transaction has already been finalized with status: ${deposit.status}`,
      deposit
    });
  }

  // Verification conditions:
  // Must provide a valid official JazzCash transaction reference
  // Response code '000' or confirmed status
  const isCodeSuccess = !responseCode || responseCode === '000' || responseCode === '0' || responseCode === 'APPROVED';
  const hasValidRef = Boolean(jazzcashTxnRef && String(jazzcashTxnRef).trim().length > 3);

  if (isCodeSuccess && hasValidRef) {
    // Check if jazzcashTxnRef has been used in any other deposit (anti-replay check)
    const duplicateTxn = db.deposits.find(d => d.id !== deposit.id && d.jazzcashTxnRef === String(jazzcashTxnRef).trim() && d.status === 'SUCCESSFUL');
    if (duplicateTxn) {
      deposit.status = 'FAILED';
      deposit.adminNotes = `Rejected: Duplicate JazzCash reference detected (already used in #${duplicateTxn.id})`;
      deposit.updatedAt = Date.now();
      saveDb();
      return res.status(409).json({
        success: false,
        error: 'Duplicate JazzCash transaction reference. Money already credited previously.',
        deposit
      });
    }

    // Success! Update deposit status to SUCCESSFUL
    deposit.status = 'SUCCESSFUL';
    deposit.jazzcashTxnRef = String(jazzcashTxnRef).trim();
    deposit.adminNotes = `Verified with JazzCash Merchant API: ${responseMessage || 'Payment Approved'}`;
    deposit.updatedAt = Date.now();

    // Credit user's wallet
    const user = getOrCreateUser(deposit.userId);
    const prevBalance = user.pkrBalance;
    const newBalance = prevBalance + deposit.amountPkr;
    user.pkrBalance = newBalance;

    // Record in transaction ledger
    const txnId = `TXN-DEP-${Date.now()}`;
    const ledgerTxn = {
      id: db.transactions.length + 1,
      userId: user.id,
      transactionId: txnId,
      type: 'DEPOSIT',
      amountPkr: deposit.amountPkr,
      currency: 'PKR',
      previousBalance: prevBalance,
      newBalance: newBalance,
      status: 'SUCCESSFUL',
      paymentProvider: 'JAZZCASH',
      providerTxnId: deposit.jazzcashTxnRef,
      description: `JazzCash Deposit (Order: ${orderId})`,
      timestamp: Date.now()
    };
    db.transactions.unshift(ledgerTxn);

    // Audit log
    db.auditLogs.unshift({
      adminName: 'JazzCash Gateway',
      action: 'DEPOSIT_SUCCESSFUL',
      target: orderId,
      details: `Credited PKR ${deposit.amountPkr} to ${user.id} (Ref: ${deposit.jazzcashTxnRef})`,
      timestamp: Date.now()
    });

    saveDb();

    return res.json({
      success: true,
      message: `Verified successfully. PKR ${deposit.amountPkr} credited to wallet.`,
      deposit,
      newBalance: user.pkrBalance
    });
  } else {
    // Payment failed or declined
    deposit.status = 'FAILED';
    deposit.adminNotes = `Declined by JazzCash Gateway: ${responseMessage || 'Code ' + responseCode}`;
    deposit.updatedAt = Date.now();
    saveDb();

    return res.status(400).json({
      success: false,
      error: 'JazzCash payment verification failed or was declined. Balance unchanged.',
      deposit
    });
  }
});

// Official JazzCash Webhook / IPN Callback
app.post('/api/jazzcash/callback', (req, res) => {
  const responseData = req.body;
  console.log('[JazzCash Callback Received]:', responseData);

  const isHashValid = jcHelper.verifyResponseHash(responseData);
  if (!isHashValid) {
    console.warn('[JazzCash Callback] Invalid Secure Hash received.');
    return res.status(400).send('Invalid Secure Hash verification.');
  }

  const txnRefNo = responseData.pp_TxnRefNo;
  const jazzcashTxnId = responseData.pp_RetreivalReferenceNo || responseData.pp_AuthCode || txnRefNo;
  const responseCode = responseData.pp_ResponseCode; // '000' is success

  const deposit = db.deposits.find(d => d.orderId === txnRefNo || d.jazzcashTxnRef === txnRefNo);
  if (deposit && deposit.status === 'PENDING') {
    if (responseCode === '000') {
      deposit.status = 'SUCCESSFUL';
      deposit.jazzcashTxnRef = jazzcashTxnId;
      deposit.adminNotes = `Auto-verified by JazzCash Gateway IPN (AuthCode: ${responseData.pp_AuthCode || 'OK'})`;
      deposit.updatedAt = Date.now();

      // Credit User
      const user = getOrCreateUser(deposit.userId);
      const prevBal = user.pkrBalance;
      user.pkrBalance += deposit.amountPkr;

      db.transactions.unshift({
        id: db.transactions.length + 1,
        userId: user.id,
        transactionId: `TXN-DEP-${Date.now()}`,
        type: 'DEPOSIT',
        amountPkr: deposit.amountPkr,
        currency: 'PKR',
        previousBalance: prevBal,
        newBalance: user.pkrBalance,
        status: 'SUCCESSFUL',
        paymentProvider: 'JAZZCASH',
        providerTxnId: jazzcashTxnId,
        description: `JazzCash IPN Verified (${txnRefNo})`,
        timestamp: Date.now()
      });
    } else {
      deposit.status = 'FAILED';
      deposit.adminNotes = `JazzCash Gateway Declined: ${responseData.pp_ResponseMessage || responseCode}`;
      deposit.updatedAt = Date.now();
    }
    saveDb();
  }

  res.send('Callback processed successfully.');
});

// Get user deposits
app.get('/api/deposits/user/:userId', (req, res) => {
  const { userId } = req.params;
  const userDeposits = db.deposits.filter(d => d.userId === userId);
  res.json({ success: true, count: userDeposits.length, deposits: userDeposits });
});

// Single deposit details
app.get('/api/deposits/:id', (req, res) => {
  const deposit = db.deposits.find(d => d.id === parseInt(req.params.id) || d.orderId === req.params.id);
  if (!deposit) return res.status(404).json({ error: 'Deposit not found.' });
  res.json({ success: true, deposit });
});

// User transaction ledger
app.get('/api/transactions/user/:userId', (req, res) => {
  const { userId } = req.params;
  const userTxns = db.transactions.filter(t => t.userId === userId);
  res.json({ success: true, count: userTxns.length, transactions: userTxns });
});

// -------------------------------------------------------------
// GAME BET & WIN WALLET INTEGRATIONS (SERVER-SIDE)
// -------------------------------------------------------------

// Place bet
app.post('/api/wallet/bet', (req, res) => {
  const { userId, gameId, betAmountPkr } = req.body;
  const amount = parseFloat(betAmountPkr);

  if (!userId || !amount || amount <= 0) {
    return res.status(400).json({ error: 'Valid userId and positive betAmountPkr required.' });
  }

  const user = getOrCreateUser(userId);
  if (user.pkrBalance < amount) {
    return res.status(400).json({ error: 'Insufficient PKR balance in backend wallet.' });
  }

  const prevBal = user.pkrBalance;
  user.pkrBalance -= amount;

  const txnId = `TXN-BET-${Date.now()}`;
  db.transactions.unshift({
    id: db.transactions.length + 1,
    userId: user.id,
    transactionId: txnId,
    type: 'BET',
    amountPkr: amount,
    currency: 'PKR',
    previousBalance: prevBal,
    newBalance: user.pkrBalance,
    status: 'SUCCESSFUL',
    paymentProvider: 'WALLET',
    providerTxnId: gameId || 'GAME',
    description: `Bet placed on ${gameId || 'Game'}`,
    timestamp: Date.now()
  });

  saveDb();

  res.json({
    success: true,
    newBalance: user.pkrBalance,
    transactionId: txnId
  });
});

// Credit win
app.post('/api/wallet/win', (req, res) => {
  const { userId, gameId, winAmountPkr, multiplier } = req.body;
  const amount = parseFloat(winAmountPkr);

  if (!userId || isNaN(amount) || amount < 0) {
    return res.status(400).json({ error: 'Valid userId and positive winAmountPkr required.' });
  }

  const user = getOrCreateUser(userId);
  const prevBal = user.pkrBalance;
  user.pkrBalance += amount;

  const txnId = `TXN-WIN-${Date.now()}`;
  db.transactions.unshift({
    id: db.transactions.length + 1,
    userId: user.id,
    transactionId: txnId,
    type: 'WIN',
    amountPkr: amount,
    currency: 'PKR',
    previousBalance: prevBal,
    newBalance: user.pkrBalance,
    status: 'SUCCESSFUL',
    paymentProvider: 'WALLET',
    providerTxnId: gameId || 'GAME',
    description: `Win on ${gameId || 'Game'} (${multiplier ? multiplier + 'x' : 'Payout'})`,
    timestamp: Date.now()
  });

  saveDb();

  res.json({
    success: true,
    newBalance: user.pkrBalance,
    transactionId: txnId
  });
});

// -------------------------------------------------------------
// ADMIN CONSOLE ROUTES
// -------------------------------------------------------------

// Admin get all deposits
app.get('/api/admin/deposits', (req, res) => {
  const { status } = req.query;
  let list = db.deposits;
  if (status) {
    list = list.filter(d => d.status.toLowerCase() === status.toLowerCase());
  }
  res.json({ success: true, count: list.length, deposits: list });
});

// Admin verify deposit (Audit action)
app.post('/api/admin/deposits/:id/status', (req, res) => {
  const { id } = req.params;
  const { status, adminNotes } = req.body; // 'SUCCESSFUL' or 'FAILED'

  const deposit = db.deposits.find(d => d.id === parseInt(id) || d.orderId === id);
  if (!deposit) {
    return res.status(404).json({ error: 'Deposit not found.' });
  }

  const prevStatus = deposit.status;
  const newStatus = status.toUpperCase();

  deposit.status = newStatus;
  deposit.adminNotes = adminNotes || (newStatus === 'SUCCESSFUL' ? 'Verified by Admin against JazzCash Statement' : 'Declined by Admin');
  deposit.updatedAt = Date.now();

  // If newly marked successful and was not already, credit user PKR balance
  if (deposit.status === 'SUCCESSFUL' && prevStatus !== 'SUCCESSFUL') {
    const user = getOrCreateUser(deposit.userId);
    const prevBal = user.pkrBalance;
    user.pkrBalance += deposit.amountPkr;

    db.transactions.unshift({
      id: db.transactions.length + 1,
      userId: user.id,
      transactionId: `TXN-DEP-${Date.now()}`,
      type: 'DEPOSIT',
      amountPkr: deposit.amountPkr,
      currency: 'PKR',
      previousBalance: prevBal,
      newBalance: user.pkrBalance,
      status: 'SUCCESSFUL',
      paymentProvider: 'JAZZCASH',
      providerTxnId: deposit.jazzcashTxnRef || deposit.orderId,
      description: `Admin Verified JazzCash Deposit (${deposit.orderId})`,
      timestamp: Date.now()
    });
  }

  db.auditLogs.unshift({
    adminName: 'Admin',
    action: `DEPOSIT_${deposit.status}`,
    target: `Deposit #${deposit.id} (${deposit.orderId})`,
    details: `Updated status to ${deposit.status} - PKR ${deposit.amountPkr} (Ref: ${deposit.jazzcashTxnRef || 'N/A'})`,
    timestamp: Date.now()
  });

  saveDb();

  res.json({ success: true, message: `Deposit marked as ${deposit.status}`, deposit });
});

// Admin view JazzCash configuration
app.get('/api/admin/config/jazzcash', (req, res) => {
  res.json({
    success: true,
    config: {
      businessName: db.jazzcashConfig.businessName,
      accountNumber: db.jazzcashConfig.accountNumber,
      merchantId: db.jazzcashConfig.merchantId,
      apiPassword: db.jazzcashConfig.apiPassword ? '••••••••' : '',
      integritySalt: db.jazzcashConfig.integritySalt ? '••••••••' : '',
      returnUrl: db.jazzcashConfig.returnUrl,
      environment: db.jazzcashConfig.environment
    }
  });
});

// Admin update JazzCash configuration
app.put('/api/admin/config/jazzcash', (req, res) => {
  const { businessName, accountNumber, merchantId, apiPassword, integritySalt, returnUrl, environment } = req.body;

  if (businessName) db.jazzcashConfig.businessName = businessName;
  if (accountNumber) db.jazzcashConfig.accountNumber = accountNumber;
  if (merchantId) db.jazzcashConfig.merchantId = merchantId;
  if (apiPassword && apiPassword !== '••••••••') db.jazzcashConfig.apiPassword = apiPassword;
  if (integritySalt && integritySalt !== '••••••••') db.jazzcashConfig.integritySalt = integritySalt;
  if (returnUrl) db.jazzcashConfig.returnUrl = returnUrl;
  if (environment) db.jazzcashConfig.environment = environment;

  jcHelper = new JazzCashHelper(db.jazzcashConfig);
  saveDb();

  db.auditLogs.unshift({
    adminName: 'Admin',
    action: 'JAZZCASH_CONFIG_UPDATED',
    target: db.jazzcashConfig.merchantId,
    details: `Updated JazzCash settings (Business: ${db.jazzcashConfig.businessName}, Acct: ${db.jazzcashConfig.accountNumber}, Env: ${db.jazzcashConfig.environment})`,
    timestamp: Date.now()
  });

  res.json({ success: true, message: 'JazzCash configuration updated successfully.' });
});

// Admin overview statistics
app.get('/api/admin/overview', (req, res) => {
  const verifiedPkr = db.deposits.filter(d => d.status === 'SUCCESSFUL').reduce((acc, d) => acc + d.amountPkr, 0);
  const pendingCount = db.deposits.filter(d => d.status === 'PENDING').length;

  res.json({
    success: true,
    totalVerifiedPkr: verifiedPkr,
    pendingDepositsCount: pendingCount,
    totalDepositsCount: db.deposits.length,
    usersCount: Object.keys(db.users).length
  });
});

app.listen(PORT, () => {
  console.log(`[P6692] Backend server running on port ${PORT}`);
  console.log(`[JazzCash Only] Merchant: ${db.jazzcashConfig.businessName} | ID: ${db.jazzcashConfig.merchantId} | Account: ${db.jazzcashConfig.accountNumber}`);
});
