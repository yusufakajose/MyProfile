Webhook receiver verification guide

Verify each incoming webhook request using these steps (recommended window: 5–10 minutes):

1) Extract headers:
   - X-Webhook-Signature (hex HMAC)
   - X-Webhook-Signature-Alg (HMAC-SHA256)
   - X-Webhook-Signature-Version (v1)
   - X-Webhook-Timestamp (epoch seconds)
   - X-Webhook-Nonce (hex)

2) Clock skew / replay window:
   - Convert X-Webhook-Timestamp to time and ensure it is within 600 seconds (10 minutes) of your server clock.
   - Reject if outside window.
   - Maintain a short-lived cache (e.g., Redis) of seen nonces for the same timestamp window. If the same nonce appears again, reject as replay.

3) Recompute signature:
   - Base string: "<timestamp>.<nonce>.<rawBody>"
   - Compute HMAC-SHA256 using the secret you configured in your LinkGrove account.
   - hex(hmac) must equal X-Webhook-Signature (constant-time compare).

4) Respond with 2xx on success. On transient errors, return 5xx – the sender uses exponential backoff with jitter and will retry up to 6 attempts, with a daily per-destination retry cap.

Reference implementation (Node/pseudocode):

```js
const crypto = require('crypto');
function safeEqual(a, b) {
  const ab = Buffer.from(a, 'hex');
  const bb = Buffer.from(b, 'hex');
  if (ab.length !== bb.length) return false;
  return crypto.timingSafeEqual(ab, bb);
}

function verifyWebhook(req, rawBody, secret) {
  const sig = req.header('X-Webhook-Signature');
  const alg = req.header('X-Webhook-Signature-Alg');
  const ver = req.header('X-Webhook-Signature-Version');
  const ts = parseInt(req.header('X-Webhook-Timestamp') || '0', 10);
  const nonce = req.header('X-Webhook-Nonce');
  if (alg !== 'HMAC-SHA256' || ver !== 'v1') return false;
  if (Math.abs(Date.now()/1000 - ts) > 600) return false; // 10 min
  if (replayCache.has(nonce)) return false; // implement TTL cache ~10 min
  const base = `${ts}.${nonce}.${rawBody}`;
  const h = crypto.createHmac('sha256', Buffer.from(secret, 'utf8')).update(base).digest('hex');
  const ok = safeEqual(h, sig);
  if (ok) replayCache.add(nonce); // set TTL
  return ok;
}
```


