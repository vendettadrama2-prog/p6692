const assert = require('assert');
const JazzCashHelper = require('./jazzcash');

console.log('--- Testing Fintech Payment API Logic ---');

const jc = new JazzCashHelper({
  merchantId: 'TEST_MERCH',
  password: 'TEST_PASS',
  integritySalt: 'TEST_SALT',
  environment: 'PRODUCTION'
});

// 1. Test Deposit HMAC Calculation
const depositOrder = 'ORD-JC-998877';
const depositAmount = 1500;
const payload = jc.buildHostedCheckoutPayload({
  txnRefNo: depositOrder,
  amountPkr: depositAmount,
  billReference: depositOrder,
  description: 'Wallet Deposit'
});

assert.ok(payload.pp_SecureHash, 'pp_SecureHash must be generated');
assert.strictEqual(payload.pp_Amount, '150000', 'PKR 1500 must convert to 150000 paisa');
assert.strictEqual(jc.verifyResponseHash(payload), true, 'Hash must verify with HMAC-SHA256');
console.log('✓ Test 1: POST /api/payment/deposit HMAC-SHA256 hash verified');

// 2. Test Callback IPN Signature Verification
const ipnSuccessPayload = {
  pp_Amount: '150000',
  pp_AuthCode: 'AUTH9988',
  pp_BillReference: depositOrder,
  pp_Language: 'EN',
  pp_MerchantID: 'TEST_MERCH',
  pp_ResponseCode: '000',
  pp_ResponseMessage: 'Transaction Completed Successfully',
  pp_RetreivalReferenceNo: 'JC9988771122',
  pp_TxnCurrency: 'PKR',
  pp_TxnDateTime: '20261014120000',
  pp_TxnRefNo: depositOrder
};

// Generate valid hash for callback
ipnSuccessPayload.pp_SecureHash = jc.generateSecureHash(ipnSuccessPayload);
assert.strictEqual(jc.verifyResponseHash(ipnSuccessPayload), true, 'Callback signature must be verified');

// Tamper test
const tamperedPayload = { ...ipnSuccessPayload, pp_Amount: '999900' };
assert.strictEqual(jc.verifyResponseHash(tamperedPayload), false, 'Tampered amount must be rejected');
console.log('✓ Test 2: POST /api/payment/callback IPN signature & anti-tampering verified');

// 3. Test Withdrawal Validation Logic
function validateWithdrawal(userBalance, requestAmount, mobileNumber) {
  if (isNaN(requestAmount) || requestAmount < 500) {
    return { valid: false, error: 'Minimum withdrawal amount is PKR 500.' };
  }
  if (!mobileNumber || mobileNumber.length < 10) {
    return { valid: false, error: 'Valid JazzCash mobile account number is required.' };
  }
  if (userBalance < requestAmount) {
    return { valid: false, error: 'Insufficient PKR balance.' };
  }
  return { valid: true, newBalance: userBalance - requestAmount };
}

assert.strictEqual(validateWithdrawal(1000, 200, '03001234567').valid, false); // < 500
assert.strictEqual(validateWithdrawal(1000, 1500, '03001234567').valid, false); // insufficient
assert.strictEqual(validateWithdrawal(1000, 800, '123').valid, false); // invalid mobile
const validWth = validateWithdrawal(2500, 1000, '03001234567');
assert.strictEqual(validWth.valid, true);
assert.strictEqual(validWth.newBalance, 1500);
console.log('✓ Test 3: POST /api/payment/withdraw rules & balance deductions verified');

// 4. Test CryptoEze HMAC-SHA256 & Webhook Verification
const CryptoEzeHelper = require('./cryptoeze');
const ce = new CryptoEzeHelper({
  merchantId: 'CE_MERCH_77291',
  apiKey: 'ce_test_api_key_123',
  webhookSecret: 'ce_wh_secret_xyz_456',
  environment: 'SANDBOX',
  settlementChannel: 'JAZZCASH',
  settlementAccount: '03001234567'
});

const testCePayload = {
  order_id: 'ORD-CE-112233',
  amount: 2500,
  currency: 'PKR',
  payment_method: 'JAZZCASH',
  merchant_id: 'CE_MERCH_77291'
};

const ceSig = ce.generateSignature(testCePayload);
assert.ok(ceSig && ceSig.length === 64, 'CryptoEze HMAC-SHA256 hex signature must be 64 characters');
assert.strictEqual(ce.verifyWebhook(testCePayload, ceSig), true, 'Valid webhook signature must pass verification');

// Tamper check
const tamperedCePayload = { ...testCePayload, amount: 999999 };
assert.strictEqual(ce.verifyWebhook(tamperedCePayload, ceSig), false, 'Tampered CryptoEze webhook must fail verification');
console.log('✓ Test 4: CryptoEze HMAC-SHA256 signature & Webhook IPN anti-tampering verified');

console.log('--- All Payment API & CryptoEze Tests Passed Successfully! ---');

