# Accounts Management

## Data Model

A new table, `user_bank_accounts`, stores account information. Sensitive data is encrypted.

### SQL Definition

```sql
CREATE TABLE wallet_withdrawals.user_bank_accounts
(
    id                       UUID PRIMARY KEY,
    user_id                  BIGINT       NOT NULL,
    bank_name                VARCHAR(255) NOT NULL,
    recipient_first_name     VARCHAR(255) NOT NULL,
    recipient_last_name      VARCHAR(255) NOT NULL,
    account_number_last4     VARCHAR(4)   NOT NULL,

    -- Encrypted PII
    encrypted_routing_number TEXT         NOT NULL,
    encrypted_national_id    TEXT         NOT NULL,
    encrypted_account_number TEXT         NOT NULL,

    created_at               TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at               TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Index for fast lookup by user
CREATE INDEX idx_user_bank_accounts_user_id ON wallet_withdrawals.user_bank_accounts (user_id);
```

### Column Explanations

| Column | Type | Constraints | Explanation |
| :--- | :--- | :--- | :--- |
| `id` | `UUID` | `PRIMARY KEY` | Unique identifier for the saved bank account. |
| `user_id` | `BIGINT` | `NOT NULL` | The user who owns this record. |
| `bank_name` | `VARCHAR(255)`| `NOT NULL` | The name of the bank. |
| `recipient_first_name` | `VARCHAR(255)`| `NOT NULL` | First name of the account holder. |
| `recipient_last_name` | `VARCHAR(255)`| `NOT NULL` | Last name of the account holder. |
| `account_number_last4` | `VARCHAR(4)` | `NOT NULL` | Last 4 digits of the account number for display. |
| `encrypted_routing_number` | `TEXT` | `NOT NULL` | Encrypted routing number. |
| `encrypted_national_id` | `TEXT` | `NOT NULL` | Encrypted national ID. |
| `encrypted_account_number` | `TEXT` | `NOT NULL` | Encrypted full account number. |

-----

## Save Multiple Bank Accounts

This is enabled by the `user_bank_accounts` table, which links bank accounts to a `user_id`. A user can add multiple entries to this table via a new `POST /api/v1/bank-accounts` endpoint. The service receives the account details, encrypts the sensitive fields, and saves the new record.

-----

## Select One for Each Withdrawal

The `POST /api/v1/wallet_withdraw` endpoint is modified. Instead of sending all recipient details, a user sends a `bank_account_id` along with the `amount`.

The service then:

1.  Fetches the saved account from the `user_bank_accounts` table (checking ownership).
2.  Decrypts the account details in memory to build the `Recipient` value object.
3.  Passes this `Recipient` to the `WalletWithdraw` aggregate, which snapshots (denormalizes) the information into the `wallet_withdrawals` table for immutability.

-----

## Manage Account Data Securely

Security is handled in three ways:

* **Authentication:** All endpoints (`POST`, `GET`, `DELETE`) require an authenticated user.
* **Authorization:** All database queries are scoped by the authenticated `user_id` to prevent one user from accessing another's data.
* **Encryption:** Sensitive fields (`encrypted_routing_number`, `encrypted_national_id`, `encrypted_account_number`) are stored encrypted at rest. They are **never** returned to the user; only display-safe fields like `account_number_last4` are exposed via `GET /api/v1/bank-accounts`.