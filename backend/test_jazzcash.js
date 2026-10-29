const assert = require('assert');
const JazzCashHelper = require('./jazzcash');

console.log('--- Running JazzCash Production Payment Unit Tests ---');

// Test 1: Configuration & Environment Initialization
const jcSandbox = new JazzCashHelper({
  merchantId: 'TEST_MERCHANT',
  password: 'TEST_PASSWORD',
  integritySalt: 'TEST_SALT',
  environment: 'SANDBOX'
});

assert.strictEqual(jcSandbox.environment, 'SANDBOX');
assert.ok(jcSandbox.getC2BApiUrl().includes('sandbox.jazzcash.com.pk'), 'Sandbox C2B URL must point to sandbox');
assert.ok(jcSandbox.getStatusInquiryUrl().includes('sandbox.jazzcash.com.pk'), 'Sandbox Inquiry URL must point to sandbox');
console.log('✓ Test 1: Sandbox environment configuration verified');

const jcProd = new JazzCashHelper({
  merchantId: 'PROD_MERCHANT',
  password: 'PROD_PASSWORD',
  integritySalt: 'PROD_SALT',
  environment: 'PRODUCTION'
});

assert.strictEqual(jcProd.environment, 'PRODUCTION');
assert.ok(jcProd.getC2BApiUrl().includes('payments.jazzcash.com.pk'), 'Production C2B URL must point to live gateway');
assert.ok(jcProd.getStatusInquiryUrl().includes('payments.jazzcash.com.pk'), 'Production Inquiry URL must point to live gateway');
console.log('✓ Test 2: Production environment configuration verified');

// Test 3: Cryptographic HMAC-SHA256 signature generation and validation
const testPayload = jcProd.buildHostedCheckoutPayload({
  txnRefNo: 'ORD-JC-TEST101',
  amountPkr: 2500,
  billReference: 'BILL-101',
  description: 'Deposit Test'
});

assert.ok(testPayload.pp_SecureHash, 'pp_SecureHash must be generated');
assert.strictEqual(testPayload.pp_Amount, '250000', 'PKR 2500 must equal 250000 paisa');

const isValidSignature = jcProd.verifyResponseHash(testPayload);
assert.strictEqual(isValidSignature, true, 'Valid signature must pass verifyResponseHash');
console.log('✓ Test 3: HMAC-SHA256 generation and verification verified');

// Test 4: Signature tampering detection
const tamperedPayload = { ...testPayload, pp_Amount: '500000' }; // altered amount
assert.strictEqual(jcProd.verifyResponseHash(tamperedPayload), false, 'Tampered amount must fail signature check');
console.log('✓ Test 4: Anti-tampering signature check verified');

// Test 5: Status Inquiry Payload Construction
const inquiryPayload = jcProd.buildStatusInquiryPayload('ORD-JC-TEST101');
assert.strictEqual(inquiryPayload.pp_TxnRefNo, 'ORD-JC-TEST101');
assert.strictEqual(jcProd.verifyResponseHash(inquiryPayload), true, 'Inquiry payload signature must be valid');
console.log('✓ Test 5: Status inquiry payload construction verified');

// Test 6: testConnection Handshake Validation
(async () => {
  const result = await jcProd.testConnection();
  assert.strictEqual(result.success, true);
  assert.strictEqual(result.environment, 'PRODUCTION');
  console.log('✓ Test 6: Admin testConnection HMAC-SHA256 handshake verified');
  console.log('--- All 6 JazzCash Backend Tests Passed Successfully! ---');
})();
