# Supabase Cloud Database & Authentication Setup Guide for Khata

This document provides the complete PostgreSQL schema, Row Level Security (RLS) policies, and step-by-step instructions to configure your Supabase backend for multi-device cloud synchronization.

---

## 1. Create a Supabase Project

1. Go to [https://supabase.com](https://supabase.com) and sign in.
2. Click **New Project** and select your organization.
3. Enter a project name (e.g., `khata-app`), database password, and region.
4. Once your project is created, navigate to **Project Settings > API**.
5. Copy your **Project URL** and **`anon` `public` API Key**.

---

## 2. Configure Android App Environment

In your `/home/delip/Khata/local.properties` file, add your Supabase credentials:

```properties
SUPABASE_URL=https://YOUR_PROJECT_REF.supabase.co
SUPABASE_ANON_KEY=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

*(Note: `local.properties` is git-ignored and keeps your keys private.)*

---

## 3. PostgreSQL Database Schema (SQL)

In the Supabase Dashboard, go to **SQL Editor > New Query**, paste the following DDL, and click **Run**:

```sql
-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================================================
-- 1. CUSTOMERS TABLE
-- ============================================================================
CREATE TABLE IF NOT EXISTS public.customers (
    id UUID PRIMARY KEY,
    shop_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    phone TEXT,
    address TEXT,
    notes TEXT,
    is_archived BOOLEAN NOT NULL DEFAULT FALSE,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    deleted_at BIGINT,
    device_name TEXT
);

CREATE INDEX IF NOT EXISTS idx_customers_shop_id ON public.customers(shop_id);
CREATE INDEX IF NOT EXISTS idx_customers_updated_at ON public.customers(updated_at);
CREATE INDEX IF NOT EXISTS idx_customers_deleted_at ON public.customers(deleted_at);

-- ============================================================================
-- 2. TRANSACTIONS TABLE
-- ============================================================================
CREATE TABLE IF NOT EXISTS public.transactions (
    id UUID PRIMARY KEY,
    shop_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    customer_id UUID NOT NULL REFERENCES public.customers(id) ON DELETE RESTRICT,
    type TEXT NOT NULL CHECK (type IN ('CREDIT', 'PAYMENT')),
    amount BIGINT NOT NULL CHECK (amount > 0),
    description TEXT,
    payment_method TEXT,
    transaction_date BIGINT NOT NULL,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    deleted_at BIGINT,
    device_name TEXT
);

CREATE INDEX IF NOT EXISTS idx_transactions_shop_id ON public.transactions(shop_id);
CREATE INDEX IF NOT EXISTS idx_transactions_customer_id ON public.transactions(customer_id);
CREATE INDEX IF NOT EXISTS idx_transactions_updated_at ON public.transactions(updated_at);
CREATE INDEX IF NOT EXISTS idx_transactions_deleted_at ON public.transactions(deleted_at);

-- ============================================================================
-- 3. TRANSACTION ITEMS TABLE
-- ============================================================================
CREATE TABLE IF NOT EXISTS public.transaction_items (
    id UUID PRIMARY KEY,
    shop_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    transaction_id UUID NOT NULL REFERENCES public.transactions(id) ON DELETE CASCADE,
    item_name TEXT NOT NULL,
    quantity DOUBLE PRECISION NOT NULL,
    unit_price BIGINT NOT NULL,
    total_price BIGINT NOT NULL,
    deleted_at BIGINT
);

CREATE INDEX IF NOT EXISTS idx_transaction_items_shop_id ON public.transaction_items(shop_id);
CREATE INDEX IF NOT EXISTS idx_transaction_items_transaction_id ON public.transaction_items(transaction_id);
```

---

## 4. Enable Row Level Security (RLS) & Security Policies

Row Level Security ensures that authenticated users can only access records matching their own `auth.uid()` (`shop_id`).

Run the following SQL in the Supabase SQL Editor:

```sql
-- GRANT TABLE PRIVILEGES TO AUTHENTICATED AND ANON ROLES
GRANT USAGE ON SCHEMA public TO authenticated, anon;
GRANT ALL ON public.customers TO authenticated, anon;
GRANT ALL ON public.transactions TO authenticated, anon;
GRANT ALL ON public.transaction_items TO authenticated, anon;

-- Enable RLS on all tables
ALTER TABLE public.customers ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.transactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.transaction_items ENABLE ROW LEVEL SECURITY;

-- ============================================================================
-- CUSTOMERS RLS POLICIES
-- ============================================================================
CREATE POLICY "Users can view their shop customers"
    ON public.customers FOR SELECT
    USING (auth.uid() = shop_id);

CREATE POLICY "Users can insert their shop customers"
    ON public.customers FOR INSERT
    WITH CHECK (auth.uid() = shop_id);

CREATE POLICY "Users can update their shop customers"
    ON public.customers FOR UPDATE
    USING (auth.uid() = shop_id);

CREATE POLICY "Users can delete their shop customers"
    ON public.customers FOR DELETE
    USING (auth.uid() = shop_id);

-- ============================================================================
-- TRANSACTIONS RLS POLICIES
-- ============================================================================
CREATE POLICY "Users can view their shop transactions"
    ON public.transactions FOR SELECT
    USING (auth.uid() = shop_id);

CREATE POLICY "Users can insert their shop transactions"
    ON public.transactions FOR INSERT
    WITH CHECK (auth.uid() = shop_id);

CREATE POLICY "Users can update their shop transactions"
    ON public.transactions FOR UPDATE
    USING (auth.uid() = shop_id);

CREATE POLICY "Users can delete their shop transactions"
    ON public.transactions FOR DELETE
    USING (auth.uid() = shop_id);

-- ============================================================================
-- TRANSACTION ITEMS RLS POLICIES
-- ============================================================================
CREATE POLICY "Users can view their shop transaction items"
    ON public.transaction_items FOR SELECT
    USING (auth.uid() = shop_id);

CREATE POLICY "Users can insert their shop transaction items"
    ON public.transaction_items FOR INSERT
    WITH CHECK (auth.uid() = shop_id);

CREATE POLICY "Users can update their shop transaction items"
    ON public.transaction_items FOR UPDATE
    USING (auth.uid() = shop_id);

CREATE POLICY "Users can delete their shop transaction items"
    ON public.transaction_items FOR DELETE
    USING (auth.uid() = shop_id);
```

---

## 5. Testing Multi-Device Synchronization

### Step 1: Account Login on Phone A
1. Open Khata on **Phone A**.
2. Go to **Settings > Cloud Synchronization**.
3. Register or Sign In with `shop@example.com`.

### Step 2: Create Record on Phone A
1. Create customer **"Hari Prasad"**.
2. Add a Credit of **Rs. 5,000**.
3. Tap **Sync Now** or wait a few seconds.
4. Verify Supabase Dashboard > Table Editor > `customers` & `transactions` rows appear.

### Step 3: Connect Phone B
1. Install Khata on **Phone B**.
2. Sign In with the same account (`shop@example.com`).
3. Tap **Sync Now**.
4. **"Hari Prasad"** with **Rs. 5,000** credit appears automatically on Phone B.

### Step 4: Add Payment on Phone B
1. On **Phone B**, record a payment of **Rs. 1,500** for Hari.
2. Tap **Sync Now**.
3. Tap **Sync Now** on **Phone A**.
4. Verify both phones show **Remaining Outstanding = Rs. 3,500**.
