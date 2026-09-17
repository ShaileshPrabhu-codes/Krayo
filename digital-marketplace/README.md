# CardBazaar — Digital Birthday Card Marketplace (v1.0)

A full-stack starter for selling personalizable digital birthday cards.
Stack: **Spring Boot (Java 21) + Angular 22 + PostgreSQL**, payments via **Razorpay (India/UPI)** and **Stripe (international/cards)**.

## What v1.0 includes

- Admin uploads a card template (any file format, e.g. a Canva export) with a theme and price
- Customer browses cards, picks one, enters a recipient name + custom message
- Login/register (email+password, JWT-based)
- Checkout: ₹49 for Indian customers (via UPI/Razorpay), $10 for everyone else (via Stripe)
- Your UPI ID is **never** stored or exposed in this codebase — it lives only inside your Razorpay dashboard
- Payment confirmation only happens via a **signature-verified webhook** — never a client-side redirect — so a spoofed "success" cannot happen
- Duplicate-payment protection: unique idempotency keys + a strict order state machine (`CREATED → PENDING → PAID/FAILED`)
- Once paid, the HD card is rendered server-side (name/message overlaid on the template) and is downloadable anytime from "My Purchases"

## Folder structure

```
digital-marketplace/
├── backend/     Spring Boot API (Java 21)
└── frontend/    Angular 22 app
```

## 1. Backend setup

### Prerequisites
- Java 21+
- Maven 3.9+
- PostgreSQL 15+ (local, or a free instance from Neon/Supabase)

### Steps

```bash
cd backend
cp .env.example .env        # then fill in real values
```

Create the database:
```sql
CREATE DATABASE marketplace;
```

Export the env vars (or use a tool like `direnv`/`dotenv-cli`) and run:

```bash
export $(cat .env | xargs)   # Linux/macOS
mvn spring-boot:run
```

Flyway will automatically create all tables on first run (`V1__init_schema.sql`).

The API starts on **http://localhost:8080**.

### Creating your first admin user

There's no UI for this yet in v1.0 — register a normal account via the API,
then manually promote it in Postgres:

```sql
UPDATE users SET role = 'ADMIN' WHERE email = 'you@example.com';
```

### Uploading your first card (as admin)

```bash
curl -X POST http://localhost:8080/api/v1/admin/products \
  -H "Authorization: Bearer <your JWT>" \
  -F "title=Elegant Rose Birthday Card" \
  -F "theme=elegant" \
  -F "priceInrPaise=4900" \
  -F "priceUsdCents=1000" \
  -F "file=@/path/to/your-canva-export.png"
```

## 2. Payment gateway setup

### Razorpay (India / UPI)
1. Sign up at razorpay.com, complete KYC.
2. Dashboard → Settings → Payment Methods → enable UPI. Your UPI ID is configured **only here** — never in code.
3. Dashboard → Settings → API Keys → generate Key Id / Key Secret → put in `.env`.
4. Dashboard → Settings → Webhooks → add `https://yourdomain.com/api/v1/payments/webhook/razorpay`, subscribe to `payment.captured` and `payment.failed`, copy the webhook secret → put in `.env`.

### Stripe (International / Cards)
1. Sign up at stripe.com.
2. Developers → API Keys → copy Publishable + Secret key → put in `.env`.
3. Developers → Webhooks → add `https://yourdomain.com/api/v1/payments/webhook/stripe`, subscribe to `payment_intent.succeeded` and `payment_intent.payment_failed`, copy the signing secret → put in `.env`.

**Note:** webhooks need a public HTTPS URL. Use `ngrok http 8080` while developing locally.

## 3. Frontend setup

### Prerequisites
- Node.js 20+

### Steps

```bash
cd frontend
npm install
npm start
```

Opens on **http://localhost:4200**, already wired to call the backend at `http://localhost:8080/api/v1` (see `src/environments/environment.ts`).

Before deploying, update `src/environments/environment.prod.ts` with your real backend URL, then:

```bash
npm run build
```

Deploy the `dist/frontend` folder to any static host (Vercel, Netlify, Cloudflare Pages, or your own server via Nginx).

### Note on the Stripe checkout UI
The checkout component includes a placeholder for Stripe. For a complete international
checkout, install `@stripe/stripe-js`, mount Stripe Elements' card input, and call
`stripe.confirmPayment()` with the `clientSecret` returned as `frontendToken` in the
order-creation response. The Razorpay path is fully wired already.

## 4. Deploying on your own server (free-tier friendly)

| Component | Suggested free host |
|---|---|
| Backend | Render, Railway |
| Frontend | Vercel, Netlify, Cloudflare Pages |
| Database | Neon, Supabase (free Postgres) |
| Uploaded files / rendered cards | Mount a persistent disk on your host, or move `CardRenderService`/`ProductService` to write to S3-compatible storage (Cloudflare R2 free tier) instead of local disk |

Set all the `.env` values as environment variables in your hosting provider's dashboard —
never commit real secrets to git.

## 5. Versioning

- Product versioning is tracked per-row (`products.version`, starting `v1.0`) so old
  purchases stay tied to the exact card version sold.
- API is versioned by URL (`/api/v1/...`) so a future `/api/v2/...` won't break this frontend.
- Tag releases in git: `git tag v1.0.0 && git push --tags`.

## 6. What's deliberately left for the next iteration

- Admin UI for uploading cards (currently via API/curl only)
- Multi-seller support (structure already allows it — `products.created_by`)
- Google/OAuth login (dependency already included; wire up `spring-boot-starter-oauth2-client`)
- Email receipts / notifications
- Rate limiting on auth & order endpoints
