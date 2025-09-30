# LinkGrove API Reference

All endpoints are prefixed with `/api`. Responses use JSON unless otherwise noted.

## Table of Contents

1. [Authentication](#authentication)
2. [Public Profile](#public-profile)
3. [Links](#links)
4. [Analytics](#analytics)
5. [Redirect & QR Codes](#redirect--qr-codes)
6. [Webhooks](#webhooks)
7. [User Settings](#user-settings)
8. [Error Responses](#error-responses)

---

## Authentication

### POST `/auth/register`

Register a new user account.

**Request:**
```json
{
  "username": "alice",
  "email": "alice@example.com",
  "password": "securePassword123"
}
```

**Response (201):**
```json
{
  "id": 42,
  "username": "alice",
  "email": "alice@example.com",
  "emailVerified": false,
  "createdAt": "2025-09-30T12:00:00Z"
}
```

**Errors:**
- `400` — Validation failed (username taken, weak password, invalid email)
- `429` — Rate limit exceeded

---

### POST `/auth/login`

Authenticate and receive JWT tokens.

**Request:**
```json
{
  "username": "alice",
  "password": "securePassword123"
}
```

**Response (200):**
```json
{
  "accessToken": "eyJhbGc...",
  "refreshToken": "dGhpc2...",
  "expiresIn": 3600
}
```

**Errors:**
- `401` — Invalid credentials
- `403` — Account locked due to failed login attempts
- `429` — Rate limit exceeded

---

### POST `/auth/refresh`

Exchange a refresh token for a new access token.

**Request:**
```json
{
  "refreshToken": "dGhpc2..."
}
```

**Response (200):**
```json
{
  "accessToken": "eyJhbGc...",
  "expiresIn": 3600
}
```

**Errors:**
- `401` — Invalid or expired refresh token

---

### POST `/auth/logout`

Revoke the current refresh token.

**Headers:**
```
Authorization: Bearer eyJhbGc...
```

**Request:**
```json
{
  "refreshToken": "dGhpc2..."
}
```

**Response (200):**
```json
{
  "message": "Logged out successfully"
}
```

---

### GET `/auth/health`

Public health check endpoint.

**Response (200):**
```json
{
  "status": "UP"
}
```

---

## Public Profile

### GET `/public/{username}`

Retrieve a user's public profile and visible links.

**Response (200):**
```json
{
  "username": "alice",
  "displayName": "Alice W.",
  "bio": "Developer & creator",
  "avatarUrl": "https://...",
  "theme": "dark",
  "links": [
    {
      "id": 1,
      "title": "My Portfolio",
      "url": "https://alice.dev",
      "isVisible": true,
      "order": 0
    }
  ]
}
```

**Errors:**
- `404` — User not found

---

### GET `/public/meta/{username}`

SEO metadata for a user's profile.

**Response (200):**
```json
{
  "title": "Alice W. | LinkGrove",
  "description": "Developer & creator",
  "ogImage": "https://localhost:3001/images/og-default.png",
  "url": "https://localhost:3001/alice"
}
```

---

## Links

All link endpoints require authentication (`Authorization: Bearer <token>`).

### GET `/links`

List all links for the authenticated user.

**Query Params:**
- `search` (optional) — Filter by title/URL
- `tagId` (optional) — Filter by tag
- `page` (int, default 0)
- `size` (int, default 20)

**Response (200):**
```json
{
  "content": [
    {
      "id": 1,
      "title": "Portfolio",
      "url": "https://alice.dev",
      "alias": "portfolio",
      "isVisible": true,
      "clickCount": 42,
      "tags": ["work"],
      "createdAt": "2025-09-01T10:00:00Z"
    }
  ],
  "totalElements": 10,
  "totalPages": 1,
  "number": 0
}
```

---

### POST `/links`

Create a new link.

**Request:**
```json
{
  "title": "My Blog",
  "url": "https://blog.alice.dev",
  "alias": "blog",
  "isVisible": true,
  "tags": ["personal"]
}
```

**Response (201):**
```json
{
  "id": 5,
  "title": "My Blog",
  "url": "https://blog.alice.dev",
  "alias": "blog",
  "isVisible": true,
  "clickCount": 0,
  "tags": ["personal"],
  "createdAt": "2025-09-30T12:30:00Z"
}
```

**Errors:**
- `400` — Validation failed (invalid URL, alias already taken)
- `429` — Rate limit exceeded

---

### PATCH `/links/{id}`

Update an existing link.

**Request:**
```json
{
  "title": "Updated Blog",
  "isVisible": false
}
```

**Response (200):**
```json
{
  "id": 5,
  "title": "Updated Blog",
  "url": "https://blog.alice.dev",
  "alias": "blog",
  "isVisible": false,
  "clickCount": 12,
  "tags": ["personal"],
  "updatedAt": "2025-09-30T13:00:00Z"
}
```

**Errors:**
- `404` — Link not found or unauthorized

---

### DELETE `/links/{id}`

Delete a link and all associated analytics.

**Response (204):** No content

**Errors:**
- `404` — Link not found or unauthorized

---

## Analytics

All analytics endpoints require authentication.

### GET `/analytics/overview`

Aggregate stats for the authenticated user.

**Response (200):**
```json
{
  "totalClicks": 1542,
  "uniqueVisitors": 823,
  "totalLinks": 15,
  "topLink": {
    "id": 1,
    "title": "Portfolio",
    "clicks": 342
  }
}
```

---

### GET `/analytics/timeseries`

Daily click counts for a specific link.

**Query Params:**
- `linkId` (required)
- `startDate` (ISO 8601, optional)
- `endDate` (ISO 8601, optional)

**Response (200):**
```json
[
  {
    "date": "2025-09-28",
    "clicks": 45,
    "uniqueVisitors": 32
  },
  {
    "date": "2025-09-29",
    "clicks": 67,
    "uniqueVisitors": 51
  }
]
```

---

### GET `/analytics/referrers`

Top referrers for a link.

**Query Params:**
- `linkId` (required)
- `limit` (int, default 10)

**Response (200):**
```json
[
  {
    "referrer": "twitter.com",
    "clicks": 123
  },
  {
    "referrer": "direct",
    "clicks": 89
  }
]
```

---

### GET `/analytics/devices`

Device breakdown for a link.

**Query Params:**
- `linkId` (required)

**Response (200):**
```json
[
  {
    "device": "mobile",
    "clicks": 234
  },
  {
    "device": "desktop",
    "clicks": 156
  },
  {
    "device": "tablet",
    "clicks": 12
  }
]
```

---

### GET `/analytics/countries`

Geographic distribution of clicks (requires GeoIP enabled).

**Query Params:**
- `linkId` (required)
- `limit` (int, default 10)

**Response (200):**
```json
[
  {
    "countryCode": "US",
    "clicks": 456
  },
  {
    "countryCode": "GB",
    "clicks": 123
  }
]
```

**Note:** Returns empty array if `GEOIP_ENABLED=false`.

---

### GET `/analytics/sources`

Traffic sources for a link (utm_source or src param).

**Query Params:**
- `linkId` (required)

**Response (200):**
```json
[
  {
    "source": "newsletter",
    "clicks": 89
  },
  {
    "source": "qr",
    "clicks": 45
  }
]
```

---

### GET `/analytics/variants`

A/B variant performance for a link.

**Query Params:**
- `linkId` (required)

**Response (200):**
```json
[
  {
    "variantKey": "blue_cta",
    "clicks": 234,
    "uniqueVisitors": 187
  },
  {
    "variantKey": "red_cta",
    "clicks": 198,
    "uniqueVisitors": 152
  }
]
```

---

### GET `/analytics/export/csv`

Download analytics data as CSV.

**Query Params:**
- `linkId` (optional, all links if omitted)
- `startDate` (ISO 8601, optional)
- `endDate` (ISO 8601, optional)

**Response (200):**
```
Content-Type: text/csv
Content-Disposition: attachment; filename="analytics-2025-09-30.csv"

date,link_id,link_title,clicks,unique_visitors
2025-09-28,1,Portfolio,45,32
2025-09-29,1,Portfolio,67,51
```

---

## Redirect & QR Codes

Public endpoints (no authentication required).

### GET `/r/{linkId}`

Redirect to the link's destination URL. Tracks analytics.

**Query Params (optional):**
- `variant` — A/B variant key
- `utm_source`, `utm_medium`, `utm_campaign` — UTM tracking
- `src` — Alternate source tracking

**Response (302):**
```
Location: https://alice.dev
```

**Errors:**
- `404` — Link not found or not visible

---

### GET `/r/a/{alias}`

Redirect via link alias instead of ID.

**Response (302):**
```
Location: https://alice.dev
```

---

### GET `/r/{linkId}/qr.png`

Generate a QR code as PNG.

**Query Params:**
- `size` (int, 128–1024, default 256)
- `margin` (int, 0–4, default 1)
- `fg` (hex color, default `000000`)
- `bg` (hex color, default `ffffff`)
- `ecc` (L/M/Q/H, default M)
- `utm` (`1` to append UTM params)
- `logo` (https URL to .png/.jpg/.jpeg)

**Response (200):**
```
Content-Type: image/png
ETag: "abc123..."
Cache-Control: public, max-age=86400, immutable
Content-Disposition: inline; filename="link-1-qr.png"

[binary PNG data]
```

**Errors:**
- `400` — Invalid params (insufficient color contrast, unsupported logo URL)
- `404` — Link not found

---

### GET `/r/{linkId}/qr.svg`

Generate a QR code as SVG. Same params as PNG endpoint.

**Response (200):**
```
Content-Type: image/svg+xml
ETag: "def456..."
Cache-Control: public, max-age=86400, immutable

<svg xmlns="http://www.w3.org/2000/svg" ...>
```

---

### HEAD `/r/{linkId}/qr.png`

Check QR code existence and ETag without downloading.

**Response (200):**
```
ETag: "abc123..."
Content-Length: 5432
```

---

## Webhooks

Requires authentication. Configure webhooks to receive `link.click` events.

### GET `/webhooks`

List all webhook configurations for the user.

**Response (200):**
```json
[
  {
    "id": 1,
    "url": "https://myapp.com/webhook",
    "events": ["link.click"],
    "secret": "whsec_...",
    "isActive": true,
    "createdAt": "2025-09-15T10:00:00Z"
  }
]
```

---

### POST `/webhooks`

Create a new webhook.

**Request:**
```json
{
  "url": "https://myapp.com/webhook",
  "events": ["link.click"],
  "secret": "your-hmac-secret"
}
```

**Response (201):**
```json
{
  "id": 2,
  "url": "https://myapp.com/webhook",
  "events": ["link.click"],
  "secret": "whsec_...",
  "isActive": true
}
```

---

### PATCH `/webhooks/{id}`

Update a webhook (e.g., toggle active status).

**Request:**
```json
{
  "isActive": false
}
```

**Response (200):**
```json
{
  "id": 2,
  "isActive": false
}
```

---

### DELETE `/webhooks/{id}`

Delete a webhook configuration.

**Response (204):** No content

---

### GET `/webhooks/{id}/deliveries`

List recent delivery attempts for a webhook.

**Response (200):**
```json
[
  {
    "id": 123,
    "webhookId": 2,
    "eventType": "link.click",
    "statusCode": 200,
    "success": true,
    "attemptCount": 1,
    "createdAt": "2025-09-30T12:00:00Z"
  },
  {
    "id": 124,
    "webhookId": 2,
    "eventType": "link.click",
    "statusCode": 500,
    "success": false,
    "attemptCount": 3,
    "nextRetryAt": "2025-09-30T12:15:00Z",
    "createdAt": "2025-09-30T12:05:00Z"
  }
]
```

---

### POST `/webhooks/{id}/deliveries/{deliveryId}/retry`

Manually retry a failed webhook delivery.

**Response (200):**
```json
{
  "id": 124,
  "success": true,
  "statusCode": 200,
  "attemptCount": 4
}
```

---

### Webhook Payload Example

When a link is clicked, LinkGrove sends:

**Headers:**
```
X-Webhook-Signature: abc123...
X-Webhook-Signature-Alg: HMAC-SHA256
X-Webhook-Signature-Version: v1
X-Webhook-Timestamp: 1696089600
X-Webhook-Nonce: def456...
Content-Type: application/json
```

**Body:**
```json
{
  "event": "link.click",
  "timestamp": "2025-09-30T12:00:00Z",
  "data": {
    "linkId": 1,
    "alias": "portfolio",
    "userId": 42,
    "clickedAt": "2025-09-30T12:00:00Z",
    "referrer": "twitter.com",
    "device": "mobile",
    "countryCode": "US",
    "source": "newsletter",
    "variant": "blue_cta"
  }
}
```

**Verification (Node.js example):**
```javascript
const crypto = require('crypto');

function verifyWebhook(req, rawBody, secret) {
  const sig = req.headers['x-webhook-signature'];
  const ts = parseInt(req.headers['x-webhook-timestamp'], 10);
  const nonce = req.headers['x-webhook-nonce'];
  
  // Check timestamp (10 min window)
  if (Math.abs(Date.now() / 1000 - ts) > 600) return false;
  
  // Compute HMAC
  const base = `${ts}.${nonce}.${rawBody}`;
  const hmac = crypto.createHmac('sha256', secret)
    .update(base)
    .digest('hex');
  
  return crypto.timingSafeEqual(
    Buffer.from(hmac, 'hex'),
    Buffer.from(sig, 'hex')
  );
}
```

---

## User Settings

### GET `/settings/profile`

Get current user's profile settings.

**Response (200):**
```json
{
  "username": "alice",
  "displayName": "Alice W.",
  "bio": "Developer & creator",
  "avatarUrl": "https://...",
  "theme": "dark",
  "isPublic": true
}
```

---

### PATCH `/settings/profile`

Update profile settings.

**Request:**
```json
{
  "displayName": "Alice Walker",
  "bio": "Full-stack developer",
  "theme": "light"
}
```

**Response (200):**
```json
{
  "username": "alice",
  "displayName": "Alice Walker",
  "bio": "Full-stack developer",
  "theme": "light"
}
```

---

### POST `/settings/password`

Change password.

**Request:**
```json
{
  "currentPassword": "oldPassword123",
  "newPassword": "newSecurePassword456"
}
```

**Response (200):**
```json
{
  "message": "Password updated successfully"
}
```

**Errors:**
- `400` — Current password incorrect or new password too weak

---

### POST `/settings/email/verify`

Request email verification.

**Response (200):**
```json
{
  "message": "Verification email sent"
}
```

---

### POST `/settings/email/verify/{token}`

Confirm email verification with token.

**Response (200):**
```json
{
  "message": "Email verified successfully"
}
```

**Errors:**
- `400` — Invalid or expired token

---

## Error Responses

All errors follow this format:

```json
{
  "timestamp": "2025-09-30T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed: alias already taken",
  "path": "/api/links"
}
```

### Common Status Codes

- `400 Bad Request` — Validation error, malformed input
- `401 Unauthorized` — Missing or invalid authentication token
- `403 Forbidden` — Insufficient permissions or account locked
- `404 Not Found` — Resource does not exist
- `429 Too Many Requests` — Rate limit exceeded (check `X-RateLimit-*` headers)
- `500 Internal Server Error` — Unexpected server error

### Rate Limit Headers

When approaching or exceeding rate limits:

```
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 5
X-RateLimit-Reset: 1696093200
```

- `Limit` — Maximum requests allowed in the window
- `Remaining` — Requests left in current window
- `Reset` — Timestamp when the window resets (epoch seconds)

---

## Additional Resources

- **Backend README**: `/backend/README.md` — Tracing, local dev setup
- **Developer Setup**: `/docs/DEV_SETUP.md` — Environment config, testing
- **OTLP Tracing**: `/docs/OTLP_TRACING.md` — Distributed tracing with Jaeger
- **Webhook Verification**: `/backend/README.md` — Detailed HMAC signature guide

For issues or feature requests, see the project `ROADMAP.md` or open a GitHub issue.
