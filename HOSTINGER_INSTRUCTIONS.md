# Hostinger Upload & Deployment Instructions for P6692

This package contains everything needed to deploy the **P6692 Gaming Platform** to Hostinger (Shared Hosting, Cloud Hosting, or VPS).

---

## 1. Fast Deploy to Hostinger Shared / Cloud Hosting (hPanel File Manager)

### Step 1: Open Hostinger File Manager
1. Log into your **Hostinger Control Panel (hPanel)**.
2. Select your domain or website.
3. Click on **Files** → **File Manager** (or connect via FTP / FileZilla).
4. Navigate to the `public_html/` folder (the root web directory).

### Step 2: Upload the ZIP
1. Upload `p6692-hostinger-publish.zip` into `public_html/`.
2. Right-click on `p6692-hostinger-publish.zip` and click **Extract**.
3. Choose `public_html/` as the extraction destination.
4. After extraction, your `public_html/` folder will have:
   - `index.html` (Landing page & download portal with all 24 games showcased)
   - `404.html` (Fallback redirect)
   - `.htaccess` (Configures APK MIME types and fast gzip compression)
   - `P6692.apk` (Latest v3.0 release APK with all 4 new games included)
   - `P6692-v3.0-debug.apk` (Direct versioned APK)
   - `backend/` (Node.js payment & wallet API server files)

### Step 3: Test the Website & Download Links
- Visit your website: `https://yourdomain.com`
- Click the **"DOWNLOAD P6692 APK (v3.0)"** button to verify direct APK downloading.

---

## 2. Deploying the Node.js Backend API (For JazzCash & Real Balance)

If you have a **Hostinger VPS** or **Hostinger Node.js Application** enabled in hPanel:

1. In hPanel, go to **Websites** → **Node.js** (or use SSH on VPS).
2. Set the application root to `backend/`.
3. Set the startup file to `server.js`.
4. Copy `backend/.env.example` to `backend/.env` and verify your JazzCash credentials:
   - `JAZZCASH_BUSINESS_NAME=Ahmed Shop`
   - `JAZZCASH_ACCOUNT_NUMBER=01104590863`
   - `JAZZCASH_MERCHANT_ID=MC895945`
   - `JAZZCASH_PASSWORD=z84t9fx8c8`
   - `JAZZCASH_INTEGRITY_SALT=498z5u0th4`
5. Run `npm install` inside the `backend` folder and start the service with `npm start` or `pm2 start server.js`.
