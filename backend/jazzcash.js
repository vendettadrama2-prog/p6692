const crypto = require('crypto');

/**
 * JazzCash Official Merchant Production Helper
 * Implements HMAC-SHA256 calculation according to JazzCash HTTP POST / M-Wallet & B2C specifications.
 * Supports:
 * - C2B MWALLET (Mobile Wallet Direct Debit API)
 * - Hosted Checkout (Customer Redirection / Form POST)
 * - Transaction Status Inquiry API
 * - B2C Merchant to Account (Disbursement / Withdrawal API)
 * - Webhook / IPN response verification
 */
class JazzCashHelper {
  constructor(config = {}) {
    this.merchantId = config.merchantId || process.env.JAZZCASH_MERCHANT_ID || 'MC895945';
    this.password = config.apiPassword || config.password || process.env.JAZZCASH_PASSWORD || 'z84t9fx8c8';
    this.integritySalt =
      config.integritySalt ||
      process.env.JAZZCASH_INTEGERITY_SALT ||
      process.env.JAZZCASH_INTEGRITY_SALT ||
      '498z5u0th4';
    this.returnUrl =
      config.returnUrl || process.env.JAZZCASH_RETURN_URL || 'https://api.p6692.com/api/jazzcash/callback';
    this.environment = config.environment || process.env.JAZZCASH_ENVIRONMENT || 'PRODUCTION';

    // Official production & sandbox endpoints
    this.c2bApiUrl =
      process.env.JAZZCASH_C2B_API_URL ||
      (this.environment === 'PRODUCTION'
        ? 'https://payments.jazzcash.com.pk/ApplicationAPI/API/2.0/Purchase/DoMWalletTransaction'
        : 'https://sandbox.jazzcash.com.pk/ApplicationAPI/API/2.0/Purchase/DoMWalletTransaction');

    this.hostedCheckoutUrl =
      process.env.JAZZCASH_API_URL ||
      (this.environment === 'PRODUCTION'
        ? 'https://payments.jazzcash.com.pk/CustomerPortal/transactionmanagement/merchantform'
        : 'https://sandbox.jazzcash.com.pk/ApplicationAPI/API/2.0/Purchase/DoMWalletTransaction');

    this.statusInquiryUrl =
      process.env.JAZZCASH_STATUS_INQUIRY_URL ||
      (this.environment === 'PRODUCTION'
        ? 'https://payments.jazzcash.com.pk/ApplicationAPI/API/2.0/Purchase/TransactionStatusInquiry'
        : 'https://sandbox.jazzcash.com.pk/ApplicationAPI/API/2.0/Purchase/TransactionStatusInquiry');

    this.b2cApiUrl =
      process.env.JAZZCASH_B2C_API_URL ||
      (this.environment === 'PRODUCTION'
        ? 'https://payments.jazzcash.com.pk/ApplicationAPI/API/2.0/Payouts/MerchantToAccount'
        : 'https://sandbox.jazzcash.com.pk/ApplicationAPI/API/2.0/Payouts/MerchantToAccount');
  }

  getApiUrl() {
    return this.hostedCheckoutUrl;
  }

  getC2BApiUrl() {
    return this.c2bApiUrl;
  }

  getStatusInquiryUrl() {
    return this.statusInquiryUrl;
  }

  getB2CApiUrl() {
    return this.b2cApiUrl;
  }

  /**
   * Generates JazzCash HMAC-SHA256 Secure Hash
   * Rules per JazzCash Official Technical Specification:
   * 1. Sort parameter keys alphabetically ascending
   * 2. Exclude empty, undefined, null strings and pp_SecureHash itself
   * 3. Prepend Integrity Salt followed by & and values: Salt&val1&val2...
   * 4. Compute HMAC-SHA256 with Integrity Salt as secret key
   * 5. Convert to uppercase hex string
   */
  generateSecureHash(params) {
    if (!this.integritySalt) {
      console.warn('[JazzCash] Integrity Salt is missing or empty.');
      return '';
    }

    const sortedKeys = Object.keys(params)
      .filter(
        (k) =>
          k !== 'pp_SecureHash' &&
          params[k] !== undefined &&
          params[k] !== null &&
          String(params[k]).trim() !== ''
      )
      .sort();

    let stringToHash = this.integritySalt;
    for (const key of sortedKeys) {
      stringToHash += `&${params[key]}`;
    }

    return crypto
      .createHmac('sha256', this.integritySalt)
      .update(stringToHash)
      .digest('hex')
      .toUpperCase();
  }

  /**
   * Verifies response from JazzCash callback / IPN webhook / inquiry / B2C
   */
  verifyResponseHash(responseParams) {
    if (!responseParams || typeof responseParams !== 'object') return false;
    const receivedHash = responseParams.pp_SecureHash;
    if (!receivedHash) return false;

    const calculatedHash = this.generateSecureHash(responseParams);
    return receivedHash.toUpperCase() === calculatedHash.toUpperCase();
  }

  /**
   * Builds C2B M-Wallet Purchase API payload
   * Official JazzCash C2B M-Wallet API directly initiates transaction to customer's mobile wallet.
   */
  buildC2BPayload({ txnRefNo, amountPkr, mobileNo, cnic = '', description = 'P6692 Real PKR Deposit' }) {
    const now = new Date();
    const formattedDateTime = now.toISOString().replace(/[-:T]/g, '').slice(0, 14);
    const expiryDateTime = new Date(now.getTime() + 60 * 60 * 1000).toISOString().replace(/[-:T]/g, '').slice(0, 14);

    // Amount in paisa (PKR * 100)
    const amountInPaisa = Math.round(amountPkr * 100).toString();

    // Standardize Pakistani mobile number: 03XXXXXXXXX
    let cleanMobile = String(mobileNo || '').trim().replace(/[^0-9]/g, '');
    if (cleanMobile.startsWith('92')) {
      cleanMobile = '0' + cleanMobile.slice(2);
    } else if (cleanMobile.length === 10 && cleanMobile.startsWith('3')) {
      cleanMobile = '0' + cleanMobile;
    }

    const payload = {
      pp_Version: '2.0',
      pp_TxnType: 'MWALLET',
      pp_Language: 'EN',
      pp_MerchantID: this.merchantId,
      pp_SubMerchantID: '',
      pp_Password: this.password,
      pp_BankID: 'TBANK',
      pp_ProductID: 'RE_ENT',
      pp_TxnRefNo: txnRefNo,
      pp_Amount: amountInPaisa,
      pp_TxnCurrency: 'PKR',
      pp_TxnDateTime: formattedDateTime,
      pp_BillReference: `P6692-${txnRefNo}`,
      pp_Description: description,
      pp_TxnExpiryDateTime: expiryDateTime,
      pp_ReturnURL: this.returnUrl,
      pp_MobileNumber: cleanMobile
    };

    if (cnic && String(cnic).trim().length >= 6) {
      payload.pp_CNIC = String(cnic).trim().slice(-6); // Last 6 digits or full as per JazzCash requirements
    }

    payload.pp_SecureHash = this.generateSecureHash(payload);
    return payload;
  }

  /**
   * Prepares Hosted Checkout form payload
   */
  buildHostedCheckoutPayload({ txnRefNo, amountPkr, billReference = '', description = 'P6692 Real PKR Deposit' }) {
    const now = new Date();
    const formattedDateTime = now.toISOString().replace(/[-:T]/g, '').slice(0, 14);
    const expiryDateTime = new Date(now.getTime() + 60 * 60 * 1000).toISOString().replace(/[-:T]/g, '').slice(0, 14);

    const amountInPaisa = Math.round(amountPkr * 100).toString();

    const payload = {
      pp_Version: '2.0',
      pp_TxnType: '',
      pp_Language: 'EN',
      pp_MerchantID: this.merchantId,
      pp_SubMerchantID: '',
      pp_Password: this.password,
      pp_BankID: 'TBANK',
      pp_ProductID: 'RE_ENT',
      pp_TxnRefNo: txnRefNo,
      pp_Amount: amountInPaisa,
      pp_TxnCurrency: 'PKR',
      pp_TxnDateTime: formattedDateTime,
      pp_BillReference: billReference || `P6692-${txnRefNo}`,
      pp_Description: description,
      pp_TxnExpiryDateTime: expiryDateTime,
      pp_ReturnURL: this.returnUrl
    };

    payload.pp_SecureHash = this.generateSecureHash(payload);
    return payload;
  }

  /**
   * Builds B2C Merchant to Account (Disbursement / Real Withdrawal) Payload
   * Dispatches real PKR funds directly from Merchant account to user's JazzCash mobile account / bank.
   */
  buildB2CPayload({ txnRefNo, amountPkr, recipientMobile, accountTitle = 'P6692 Customer', description = 'P6692 PKR Withdrawal' }) {
    const now = new Date();
    const formattedDateTime = now.toISOString().replace(/[-:T]/g, '').slice(0, 14);

    const amountInPaisa = Math.round(amountPkr * 100).toString();

    let cleanMobile = String(recipientMobile || '').trim().replace(/[^0-9]/g, '');
    if (cleanMobile.startsWith('92')) {
      cleanMobile = '0' + cleanMobile.slice(2);
    } else if (cleanMobile.length === 10 && cleanMobile.startsWith('3')) {
      cleanMobile = '0' + cleanMobile;
    }

    const payload = {
      pp_Version: '2.0',
      pp_TxnType: 'B2C',
      pp_Language: 'EN',
      pp_MerchantID: this.merchantId,
      pp_Password: this.password,
      pp_TxnRefNo: txnRefNo,
      pp_Amount: amountInPaisa,
      pp_TxnCurrency: 'PKR',
      pp_TxnDateTime: formattedDateTime,
      pp_ReceiverMobile: cleanMobile,
      pp_ReceiverAccountTitle: accountTitle,
      pp_Description: description
    };

    payload.pp_SecureHash = this.generateSecureHash(payload);
    return payload;
  }

  /**
   * Executes official C2B M-Wallet API call to JazzCash
   */
  async executeC2BPayment(payload) {
    const endpoint = this.c2bApiUrl;
    console.log(`[JazzCash C2B Request] POST to ${endpoint} | Ref: ${payload.pp_TxnRefNo} | Amount: ${payload.pp_Amount}`);

    try {
      const response = await fetch(endpoint, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'application/json'
        },
        body: JSON.stringify(payload)
      });

      const text = await response.text();
      let data;
      try {
        data = JSON.parse(text);
      } catch (e) {
        // JazzCash may return URL-encoded form string or XML
        const urlParams = new URLSearchParams(text);
        data = Object.fromEntries(urlParams.entries());
      }

      console.log(`[JazzCash C2B Response] Code: ${data.pp_ResponseCode || data.responseCode} | Msg: ${data.pp_ResponseMessage || data.responseMessage}`);
      return { success: true, data };
    } catch (error) {
      console.error('[JazzCash C2B Request Failed]:', error.message);
      return { success: false, error: error.message };
    }
  }

  /**
   * Executes official B2C Disbursement / Withdrawal API call to JazzCash
   */
  async executeB2CDisbursement(payload) {
    const endpoint = this.b2cApiUrl;
    console.log(`[JazzCash B2C Payout] POST to ${endpoint} | Ref: ${payload.pp_TxnRefNo} | Recipient: ${payload.pp_ReceiverMobile} | Amount: ${payload.pp_Amount}`);

    try {
      const response = await fetch(endpoint, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'application/json'
        },
        body: JSON.stringify(payload)
      });

      const text = await response.text();
      let data;
      try {
        data = JSON.parse(text);
      } catch (e) {
        const urlParams = new URLSearchParams(text);
        data = Object.fromEntries(urlParams.entries());
      }

      console.log(`[JazzCash B2C Response] Code: ${data.pp_ResponseCode || data.responseCode} | Msg: ${data.pp_ResponseMessage || data.responseMessage}`);
      return { success: true, data };
    } catch (error) {
      console.error('[JazzCash B2C Request Failed]:', error.message);
      return { success: false, error: error.message };
    }
  }

  /**
   * Builds Status Inquiry Payload to verify an existing transaction directly with JazzCash server
   */
  buildStatusInquiryPayload(txnRefNo) {
    const payload = {
      pp_Version: '2.0',
      pp_TxnType: '',
      pp_MerchantID: this.merchantId,
      pp_Password: this.password,
      pp_TxnRefNo: txnRefNo
    };

    payload.pp_SecureHash = this.generateSecureHash(payload);
    return payload;
  }
}

module.exports = JazzCashHelper;
