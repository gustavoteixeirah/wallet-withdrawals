# Transaction History (Design Only)

This document outlines the design for the transaction history feature. This design supports listing a user's transactions with filters for date, status, and account.

-----

## 1\. Data Model & Optimizations

No new tables are required for this feature. The existing `wallet_withdrawals.wallet_withdrawals` table  serves as the single source of truth for all transaction history.

This feature will query the `wallet_withdrawals` table, scoped by the authenticated user's ID.

### Schema Modifications & Indexes

To support efficient filtering, the following changes are recommended:

1.  **Add `bank_account_id` Column:** As part of the "Account Management" feature, a `bank_account_id` (UUID, nullable) should be added to the `wallet_withdrawals` table. This links a withdrawal to a user's saved account and is the cleanest way to "filter by account."
2.  **Add Indexes:** Indexes are critical for a high-performance history/filter endpoint.

<!-- end list -->

```sql
-- Add a column to link to the user_bank_accounts table
ALTER TABLE wallet_withdrawals.wallet_withdrawals
ADD COLUMN bank_account_id UUID;

-- Add indexes for efficient filtering and sorting
CREATE INDEX idx_wallet_withdrawals_user_id 
  ON wallet_withdrawals.wallet_withdrawals (user_id);

CREATE INDEX idx_wallet_withdrawals_user_status_created
  ON wallet_withdrawals.wallet_withdrawals (user_id, status, created_at DESC);
  
CREATE INDEX idx_wallet_withdrawals_bank_account_id
  ON wallet_withdrawals.wallet_withdrawals (bank_account_id);
```

-----

## 2\. API Endpoint Design

A new paginated `GET` endpoint will be created to retrieve the transaction list.

### `GET /api/v1/wallet-withdrawals`

Retrieves a paginated list of wallet withdrawals for the *authenticated user*. The `user_id` is derived from the authentication token (e.g., JWT) to prevent Insecure Direct Object Reference (IDOR).

#### Query Parameters

| Parameter | Type | Optional | Description |
| :--- | :--- | :--- | :--- |
| `date_from` | `String` | Yes | Start date filter (ISO 8601, e.g., `2025-10-01`). |
| `date_to` | `String` | Yes | End date filter (ISO 8601, e.g., `2025-10-31`). |
| `status` | `String` | Yes | Filters by status (e.g., `COMPLETED`, `FAILED`). |
| `bank_account_id`| `UUID` | Yes | Filters by a specific saved bank account. |
| `page` | `Integer` | Yes | Page number for pagination (default: `0`). |
| `page_size` | `Integer` | Yes | Number of items per page (default: `20`). |

#### Example Success Response (200 OK)

The response includes pagination details and a list of transaction summaries.

```json
{
  "page": 0,
  "page_size": 20,
  "total_items": 1,
  "total_pages": 1,
  "items": [
    {
      "id": "a1b2c3d4-e5f6-7890-g1h2-i3j4k5l6m7n8",
      "status": "COMPLETED",
      "created_at": "2025-10-20T10:00:00Z",
      "amount": 100.00,
      "fee": 10.00,
      "amount_for_recipient": 90.00,
      "recipient_name": "John Doe",
      "recipient_account_last4": "4321"
    }
  ]
}
```

-----

## 3\. Response Data

The `items` array contains a list of `WalletWithdrawalSummary` objects. This is a read-only Data Transfer Object (DTO) designed to provide key information without exposing sensitive details.

### `WalletWithdrawalSummary` Fields

| Field | Type | Description |
| :--- | :--- | :--- |
| `id` | `UUID` | The unique transaction ID. |
| `status` | `String` | The final status (`COMPLETED`, `FAILED`, etc.). |
| `created_at` | `String` | The timestamp when the transaction was initiated. |
| `amount` | `Number` | The gross amount of the withdrawal. |
| `fee` | `Number` | The fee applied to the transaction. |
| `amount_for_recipient`| `Number` | The net amount sent to the recipient. |
| `recipient_name` | `String` | Combined first and last name of the recipient. |
| `recipient_account_last4`| `String` | The last 4 digits of the recipient's account number. |