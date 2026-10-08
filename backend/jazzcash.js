const crypto = require('crypto');

/**
 * JazzCash Official Merchant Helper
 * Implements HMAC-SHA256 calculation according to JazzCash HTTP POST / M-Wallet specifications.
 * Supports MWALLET (Mobile Account), Hosted Checkout (Redirection / Page), and Transaction Status Inquiry.
 */
class JazzCashHelper {
  constructor(config = {}) {
    this.merchantId = config.merchantId || process.env.JAZZCASH_MERCHANT_ID || '';
    this.password = config.apiPassword || config.password || process.env.JAZZCASH_PASSWORD || '';
    this.integritySalt = config.integritySalt || process.env.JAZZCASH_INTEGRITY_SALT || '';
    this.returnUrl = config.returnUrl || process.env.JAZZCASH_RETURN_URL || 'https://api.p6692.com/api/jazzcash/callback';
    this.environment = config.environment || process.env.JAZZCASH_ENVIRONMENT || 'SANDBOX';
  }

  getApiUrl() {
    if (this.environment === 'PRODUCTION') {
      return 'https://payments.jazzcash.com.pk/CustomerPortal/transactionmanagement/merchantform';
    }
    return 'https://sandbox.jazzcash.com.pk/ApplicationAPI/API/2.0/Purchase/DoMWalletTransaction';
  }

  getStatusInquiryUrl() {
    if (this.environment === 'PRODUCTION') {
      return 'https://payments.jazzcash.com.pk/ApplicationAPI/API/2.0/Purchase/TransactionStatusInquiry';
    }
    return 'https://sandbox.jazzcash.com.pk/ApplicationAPI/API/2.0/Purchase/TransactionStatusInquiry';
  }

  /**
   * Generates JazzCash HMAC-SHA256 Secure Hash
   * Rules per JazzCash documentation:
   * 1. Sort parameter keys alphabetically ascending
   * 2. Exclude empty, undefined, null strings and pp_SecureHash itself
   * 3. Prepend Integrity Salt followed by & and values: Salt&val1&val2...
   * 4. Compute HMAC-SHA256 with Integrity Salt as secret key
   * 5. Convert to upper-case hex string
   */
  generateSecureHash(params) {
    if (!this.integritySalt) {
      console.warn('[JazzCash] Integrity Salt is missing or empty.');
      return '';
    }

    const sortedKeys = Object.keys(params)
      .filter((k) => k !== 'pp_SecureHash' && params[k] !== undefined && params[k] !== null && String(params[k]).trim() !== '')
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
   * Prepares Hosted Checkout payload for user web/browser authorization
   */
  buildHostedCheckoutPayload({ txnRefNo, amountPkr, billReference = '', description = 'P6692 Real PKR Deposit' }) {
    const now = new Date();
    const formattedDateTime = now.toISOString().replace(/[-:T]/g, '').slice(0, 14);
    const expiryDateTime = new Date(now.getTime() + 60 * 60 * 1000).toISOString().replace(/[-:T]/g, '').slice(0, 14);

    const amountInPaisa = Math.round(amountPkr * 100).toString();

    const payload = {
      pp_Version: '2.0',
      pp_TxnType: '', // Generic hosted checkout
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
      pp_ReturnURL: this.returnUrl,
      pp_SecureHash: ''
    };

    payload.pp_SecureHash = this.generateSecureHash(payload);
    return payload;
  }

  /**
   * Builds parameters for Mobile Wallet (MWALLET) API request where user authorizes on their JazzCash device
   * NOTE: Does NOT ask for or store PIN/OTP. JazzCash pushes authorization prompt directly to user's registered phone.
   */
  buildMWalletPayload({ txnRefNo, amountPkr, mobileNo, cnic = '', description = 'P6692 Real PKR Deposit' }) {
    const now = new Date();
    const formattedDateTime = now.toISOString().replace(/[-:T]/g, '').slice(0, 14);
    const expiryDateTime = new Date(now.getTime() + 60 * 60 * 1000).toISOString().replace(/[-:T]/g, '').slice(0, 14);

    // Amount in paisa (PKR * 100)
    const amountInPaisa = Math.round(amountPkr * 100).toString();

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
      pp_MobileNumber: mobileNo
    };

    if (cnic && cnic.trim() !== '') {
      payload.pp_CNIC = cnic.trim();
    }

    payload.pp_SecureHash = this.generateSecureHash(payload);
    return payload;
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

  /**
   * Verifies response from JazzCash callback / IPN webhook / inquiry
   */
  verifyResponseHash(responseParams) {
    if (!responseParams || typeof responseParams !== 'object') return false;
    const receivedHash = responseParams.pp_SecureHash;
    if (!receivedHash) return false;

    const calculatedHash = this.generateSecureHash(responseParams);
    return receivedHash.toUpperCase() === calculatedHash.toUpperCase();
  }
}

module.exports = JazzCashHelper;
