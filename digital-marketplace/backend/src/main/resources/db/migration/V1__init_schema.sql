-- ===================================================================
-- Digital Marketplace - Initial Schema (v1.0)
-- ===================================================================

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255),              -- null if user only signs in via OAuth
    full_name VARCHAR(255),
    auth_provider VARCHAR(20) NOT NULL DEFAULT 'LOCAL',   -- LOCAL | GOOGLE
    country_code VARCHAR(2) NOT NULL DEFAULT 'IN',        -- drives INR vs USD pricing
    role VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER',         -- CUSTOMER | ADMIN
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    sku VARCHAR(64) NOT NULL UNIQUE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    theme VARCHAR(100),                      -- e.g. 'kids', 'elegant', 'funny'
    version VARCHAR(20) NOT NULL DEFAULT 'v1.0',
    price_inr_paise INTEGER NOT NULL,        -- store money as integer minor units
    price_usd_cents INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',   -- DRAFT | PUBLISHED | RETIRED
    created_by BIGINT REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- The original Canva export(s) uploaded by admin, plus editable text-zone metadata
CREATE TABLE product_assets (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    file_url TEXT NOT NULL,
    file_format VARCHAR(10) NOT NULL,        -- png | jpg | pdf | psd | etc (any accepted)
    is_template BOOLEAN NOT NULL DEFAULT true,
    -- where on the template the recipient name / message get overlaid
    name_zone JSONB,        -- {"x":100,"y":250,"fontSize":36,"color":"#333","fontFamily":"..."}
    message_zone JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    order_number VARCHAR(40) NOT NULL UNIQUE,          -- human/traceable id, e.g. ORD-20260916-XXXX
    idempotency_key VARCHAR(100) NOT NULL UNIQUE,       -- prevents duplicate order creation on retry
    user_id BIGINT NOT NULL REFERENCES users(id),
    product_id BIGINT NOT NULL REFERENCES products(id),
    currency VARCHAR(3) NOT NULL,             -- INR | USD
    amount INTEGER NOT NULL,                  -- minor units (paise/cents)
    gateway VARCHAR(20) NOT NULL,              -- RAZORPAY | STRIPE
    gateway_order_id VARCHAR(100),             -- id returned by gateway at order-creation
    status VARCHAR(20) NOT NULL DEFAULT 'CREATED', -- CREATED|PENDING|PAID|FAILED|CANCELLED
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_orders_user ON orders(user_id);
CREATE INDEX idx_orders_status ON orders(status);

-- One row per gateway webhook/payment confirmation event received (raw + verified)
CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id),
    gateway_payment_id VARCHAR(120) NOT NULL UNIQUE,   -- dedupe key: gateway's own payment id
    status VARCHAR(20) NOT NULL,                       -- SUCCESS | FAILED
    signature_verified BOOLEAN NOT NULL DEFAULT false,
    raw_payload JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE customizations (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id) UNIQUE,
    recipient_name VARCHAR(255) NOT NULL,
    custom_message TEXT NOT NULL,
    rendered_asset_url TEXT,          -- filled in only after order.status = PAID
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE downloads (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    order_id BIGINT NOT NULL REFERENCES orders(id),
    format VARCHAR(10) NOT NULL,       -- pdf | png | jpg
    download_url TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_downloads_user ON downloads(user_id);
