const crypto = require('crypto');

/**
 * CryptoEze Payment Gateway Official Helper
 * Integrates CryptoEze Pakistan unified payment gateway supporting:
 * - JazzCash mobile wallet payments
 * - Easypaisa mobile wallet payments
 * - USDT (BEP-20 / TRC-20)
 * - Automated Webhook IPN listeners
 * - B2C Payouts & Disbursements to JazzCash
 */
class CryptoEzeHelper {
  constructor(config = {}) {
    this.merchantId = config.merchantId || process.env.CRYPTOEZE_MERCHANT_ID || 'merch_8bbc74b534e28efb54a3de42';
    this.apiKey = config.apiKey || process.env.CRYPTOEZE_API_KEY || 'pk_live_1bbea1f4b9cab098a5189af471d580e9';
    this.secretKey = config.secretKey || process.env.CRYPTOEZE_SECRET_KEY || 'sk_live_3860';
    this.webhookSecret = config.webhookSecret || process.env.CRYPTOEZE_WEBHOOK_SECRET || 'whsec_4a6112';
    this.environment = config.environment || process.env.CRYPTOEZE_ENVIRONMENT || 'LIVE';
    this.webhookUrl = config.webhookUrl || process.env.CRYPTOEZE_WEBHOOK_URL || 'https://p6692.com/webhooks/cryptoeze';
    
    this.apiUrl = config.apiUrl || process.env.CRYPTOEZE_API_URL || 'https://cryptoeze.com/api/v1';

    this.settlementChannel = config.settlementChannel || 'JAZZCASH';
    this.settlementAccount = config.settlementAccount || '03001234567';
  }

  /**
   * Generates CryptoEze HMAC-SHA256 signature for request payloads
   */
  generateSignature(payloadString) {
    if (!this.webhookSecret) return '';
    return crypto
      .createHmac('sha256', this.webhookSecret)
      .update(typeof payloadString === 'string' ? payloadString : JSON.stringify(payloadString))
      .digest('hex');
  }

  /**
   * Verifies incoming webhook signature from CryptoEze server
   */
  verifyWebhook(payload, receivedSignature) {
    if (!receivedSignature || !this.webhookSecret) return false;
    const computedSignature = this.generateSignature(payload);
    try {
      return crypto.timingSafeEqual(
        Buffer.from(computedSignature, 'utf8'),
        Buffer.from(receivedSignature, 'utf8')
      );
    } catch (e) {
      return computedSignature.toLowerCase() === String(receivedSignature).toLowerCase();
    }
  }

  /**
   * Creates a payment invoice for JazzCash via CryptoEze
   */
  async createInvoice({ orderId, amountPkr, mobileNumber, channel = 'JAZZCASH', customerEmail = '', description = 'P6692 Wallet Deposit' }) {
    let cleanMobile = String(mobileNumber || '').trim().replace(/[^0-9]/g, '');
    if (cleanMobile.startsWith('92')) {
      cleanMobile = '0' + cleanMobile.slice(2);
    } else if (cleanMobile.length === 10 && cleanMobile.startsWith('3')) {
      cleanMobile = '0' + cleanMobile;
    }

    const payload = {
      merchant_id: this.merchantId,
      order_id: orderId,
      amount: parseFloat(amountPkr),
      currency: 'PKR',
      payment_method: channel.toUpperCase(), // 'JAZZCASH', 'EASYPAISA', 'USDT'
      customer_mobile: cleanMobile,
      customer_email: customerEmail || 'player@p6692.com',
      description: description,
      callback_url: process.env.CRYPTOEZE_CALLBACK_URL || 'https://api.p6692.com/api/cryptoeze/callback',
      webhook_url: process.env.CRYPTOEZE_WEBHOOK_URL || 'https://api.p6692.com/api/cryptoeze/webhook',
      created_at: Math.floor(Date.now() / 1000)
    };

    const signature = this.generateSignature(payload);

    console.log(`[CryptoEze API] Creating ${channel} invoice for Order: ${orderId} | Amount: PKR ${amountPkr} | Target: ${this.apiUrl}/invoices`);

    try {
      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), 10000);

      const response = await fetch(`${this.apiUrl}/invoices`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'application/json',
          'Authorization': `Bearer ${this.apiKey}`,
          'x-api-key': this.apiKey,
          'x-merchant-id': this.merchantId,
          'x-signature': signature
        },
        body: JSON.stringify(payload),
        signal: controller.signal
      });

      clearTimeout(timeoutId);
      const text = await response.text();
      let data;
      try {
        data = JSON.parse(text);
      } catch (err) {
        data = { raw: text };
      }

      return {
        success: response.ok || data.status === 'success' || data.success === true,
        httpStatus: response.status,
        data,
        payload,
        signature
      };
    } catch (error) {
      console.warn(`[CryptoEze API] Direct endpoint unreachable (${error.message}). Returning formatted invoice payload for frontend/sandbox switch.`);
      return {
        success: false,
        isNetworkError: true,
        error: error.message,
        payload,
        signature,
        fallbackInvoiceUrl: `https://checkout.cryptoeze.com/pay/${orderId}?method=jazzcash&amount=${amountPkr}`
      };
    }
  }

  /**
   * Dispatches B2C Payout / Withdrawal to user's JazzCash mobile account
   */
  async processWithdrawal({ withdrawalId, amountPkr, recipientMobile, accountTitle = 'Customer' }) {
    let cleanMobile = String(recipientMobile || '').trim().replace(/[^0-9]/g, '');
    if (cleanMobile.startsWith('92')) cleanMobile = '0' + cleanMobile.slice(2);
    else if (cleanMobile.length === 10 && cleanMobile.startsWith('3')) cleanMobile = '0' + cleanMobile;

    const payload = {
      merchant_id: this.merchantId,
      payout_id: withdrawalId,
      amount: parseFloat(amountPkr),
      currency: 'PKR',
      channel: 'JAZZCASH',
      recipient_mobile: cleanMobile,
      account_title: accountTitle,
      created_at: Math.floor(Date.now() / 1000)
    };

    const signature = this.generateSignature(payload);

    try {
      const response = await fetch(`${this.apiUrl}/payouts`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${this.apiKey}`,
          'x-api-key': this.apiKey,
          'x-signature': signature
        },
        body: JSON.stringify(payload)
      });

      const data = await response.json();
      return { success: response.ok, data };
    } catch (e) {
      return { success: false, error: e.message, simulated: true };
    }
  }
}

module.exports = CryptoEzeHelper;
