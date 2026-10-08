const crypto = require('crypto');

const JWT_SECRET = process.env.JWT_SECRET || 'p6692_jwt_hmac_sha256_secure_signing_token_99824!';
const ADMIN_SECRET_KEY = process.env.ADMIN_SECRET_KEY || 'p6692_super_admin_secret_key_2026!';

/**
 * Base64URL encoding/decoding for standards-compliant JWT without heavy external dependencies
 */
function base64UrlEncode(str) {
  return Buffer.from(str)
    .toString('base64')
    .replace(/=/g, '')
    .replace(/\+/g, '-')
    .replace(/\//g, '_');
}

function base64UrlDecode(str) {
  let base64 = str.replace(/-/g, '+').replace(/_/g, '/');
  while (base64.length % 4) {
    base64 += '=';
  }
  return Buffer.from(base64, 'base64').toString('utf8');
}

/**
 * Signs payload into a valid JWT token
 */
function signJwt(payload, expiresInSeconds = 86400) {
  const header = { alg: 'HS256', typ: 'JWT' };
  const now = Math.floor(Date.now() / 1000);
  const fullPayload = {
    ...payload,
    iat: now,
    exp: now + expiresInSeconds
  };

  const encodedHeader = base64UrlEncode(JSON.stringify(header));
  const encodedPayload = base64UrlEncode(JSON.stringify(fullPayload));
  const dataToSign = `${encodedHeader}.${encodedPayload}`;

  const signature = crypto
    .createHmac('sha256', JWT_SECRET)
    .update(dataToSign)
    .digest('base64')
    .replace(/=/g, '')
    .replace(/\+/g, '-')
    .replace(/\//g, '_');

  return `${dataToSign}.${signature}`;
}

/**
 * Verifies and decodes JWT token
 */
function verifyJwt(token) {
  if (!token || typeof token !== 'string') return null;
  const parts = token.split('.');
  if (parts.length !== 3) return null;

  const [encodedHeader, encodedPayload, signature] = parts;
  const dataToSign = `${encodedHeader}.${encodedPayload}`;

  const expectedSignature = crypto
    .createHmac('sha256', JWT_SECRET)
    .update(dataToSign)
    .digest('base64')
    .replace(/=/g, '')
    .replace(/\+/g, '-')
    .replace(/\//g, '_');

  if (signature !== expectedSignature) {
    return null;
  }

  try {
    const payload = JSON.parse(base64UrlDecode(encodedPayload));
    const now = Math.floor(Date.now() / 1000);
    if (payload.exp && payload.exp < now) {
      return null; // Expired
    }
    return payload;
  } catch (e) {
    return null;
  }
}

/**
 * Strict Admin Auth Middleware with RBAC
 * Requires:
 * 1. Valid JWT Token (Authorization: Bearer <token>) WITH role === 'ADMIN'
 *    OR Admin Secret Key/Password (x-admin-key: <ADMIN_SECRET_KEY> or Authorization: AdminKey <key>)
 * 
 * Non-admin users or invalid credentials strictly receive: 403 Forbidden
 */
function adminAuthMiddleware(req, res, next) {
  const authHeader = req.headers['authorization'] || '';
  const adminKeyHeader = req.headers['x-admin-key'] || req.headers['admin-secret'] || '';

  // 1. Check direct Admin Secret Key header
  if (adminKeyHeader && adminKeyHeader === ADMIN_SECRET_KEY) {
    req.adminUser = { id: 'admin_sys', role: 'ADMIN', username: 'SuperAdmin' };
    return next();
  }

  // 2. Check Authorization header for Bearer JWT or AdminKey
  if (authHeader) {
    if (authHeader.startsWith('AdminKey ')) {
      const key = authHeader.replace('AdminKey ', '').trim();
      if (key === ADMIN_SECRET_KEY) {
        req.adminUser = { id: 'admin_sys', role: 'ADMIN', username: 'SuperAdmin' };
        return next();
      }
    } else if (authHeader.startsWith('Bearer ')) {
      const token = authHeader.replace('Bearer ', '').trim();
      const decoded = verifyJwt(token);

      if (decoded && decoded.role && String(decoded.role).toUpperCase() === 'ADMIN') {
        req.adminUser = decoded;
        return next();
      }
    }
  }

  // 3. Fallback check for adminKey in request body/query for convenience in tooling
  const reqKey = req.body?.adminSecret || req.query?.adminKey;
  if (reqKey && reqKey === ADMIN_SECRET_KEY) {
    req.adminUser = { id: 'admin_sys', role: 'ADMIN', username: 'SuperAdmin' };
    return next();
  }

  // Strict 403 Forbidden for any unauthorized or standard non-admin user
  return res.status(403).json({
    success: false,
    error: '403 Forbidden: Access denied. Valid Admin JWT token with role=ADMIN or Admin Secret Key is required.',
    code: 'ADMIN_ACCESS_REQUIRED'
  });
}

module.exports = {
  signJwt,
  verifyJwt,
  adminAuthMiddleware,
  ADMIN_SECRET_KEY
};
