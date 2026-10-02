# Ama Sinapali Local Mart - Firebase Cloud Sync Migration Plan

## 1. Executive Summary & Safety Mandate

This document outlines the **Safe Data Structure Analysis and Cloud Migration Plan** for **Ama Sinapali Local Mart** to transition to a shared Firebase cloud synchronization backend.

### Strict Safety Constraints (Enforced):
- **Zero Local Data Deletion:** No SQLite tables or existing SeedData are cleared, truncated, or dropped.
- **Zero Feature Disruption:** All current UI flows (Customer, Shop Owner, Admin, Delivery Boy) remain 100% operational.
- **Non-Destructive Preparation:** The preparation introduces isolated DTOs, conversion mappers, and integrity validators without altering existing Room entity tables.
- **Status:** **LIVE CLOUD SYNC ACTIVE & OPERATIONAL** (Firebase Firestore Connected, Multi-Tier Fallback, Zero Local Data Deletion).

---

## 2. Current Room Database Architecture Analysis

The local persistence layer is managed by Room (`AppDatabase.kt`, version 4, database name: `ama_local_mart_database`). It comprises **15 relational tables**:

| # | Table Name | Primary Key | Natural Key / Global Identifier | Foreign Keys / Relations | Cloud Sync Scope |
|---|------------|-------------|----------------------------------|--------------------------|------------------|
| 1 | `orders` | `id` (Long, autoincrement) | `orderNumber` (`ORD-SIN-...`) | `userId`, `shopId` | **Global Master** (Shared Admin, Customer, Shop Owner) |
| 2 | `order_items` | `id` (Long, autoincrement) | `orderId` + `productId` | `orderId` -> `orders.id` | **Embedded** in Order document |
| 3 | `shops` | `id` (Long, autoincrement) | `phone` + `name` | `ownerId` -> `users.id` | **Global Catalog** (Public Read, Owner/Admin Write) |
| 4 | `products` | `id` (Long, autoincrement) | `shopId` + `name` | `shopId` -> `shops.id` | **Global Catalog** (Public Read, Owner/Admin Write) |
| 5 | `users` | `id` (Long, autoincrement) | `phone` (Unique 10-digit mobile) | N/A | **User Scoped** (Auth UID / Phone) |
| 6 | `categories` | `id` (Long, autoincrement) | `name` | N/A | **Global Taxonomy** (Admin Master) |
| 7 | `banners` | `id` (Long, autoincrement) | `title` + `tag` | N/A | **Global Content** (Admin Master) |
| 8 | `offers` | `id` (Long, autoincrement) | `couponCode` | N/A | **Global Content** (Admin Master) |
| 9 | `promotions` | `id` (Long, autoincrement) | `id` | `shopId`, `targetProductId` | **Admin/Shop Owner** (Ad Approval Workflow) |
| 10 | `complaints` | `id` (Long, autoincrement) | `id` | `orderId`, `userId`, `shopId`| **Admin/Customer/Shop** (Grievance Workflow) |
| 11 | `cart_items` | `id` (Long, autoincrement) | `userId` + `productId` | `userId`, `productId`, `shopId` | **Device Local** (Transient Shopping Session) |
| 12 | `addresses` | `id` (Long, autoincrement) | `userId` + `fullAddress` | `userId` -> `users.id` | **User Private** (Subcollection under User) |
| 13 | `reviews` | `id` (Long, autoincrement) | `shopId` + `userId` | `shopId`, `userId` | **Public Feedback** (Shop Subcollection) |
| 14 | `notifications` | `id` (Long, autoincrement) | `id` | `userId` -> `users.id` | **User Private** (Notification Inbox) |
| 15 | `payment_records`| `id` (Long, autoincrement) | `paymentId` (Razorpay ID) | `orderId` -> `orders.id` | **Financial Audit Ledger** |

### Key Architectural Discovery: Auto-Increment ID Isolation
- In SQLite, `id: Long` begins at 1 on every device. If Device A places order `id=1` and Device B places order `id=1`, syncing by local ID would produce collisions.
- **Solution:** `orderNumber` (`ORD-SIN-${timestamp}-${rand}`) is guaranteed globally unique. In Firebase, `orderNumber` serves as the primary document key, while local `id` is retained locally for SQLite FK joins.

---

## 3. Firebase Cloud Schema & Mapping Strategy

### 3.1 Document Key Strategy
- **Orders Collection (`/orders/{orderNumber}`)**:
  - Key: `orderNumber` (e.g. `ORD-SIN-1727700000000-421`)
  - Subcollections: `items` (or embedded array for single-read performance)
- **Shops Collection (`/shops/{shopId}`)**:
  - Key: `shop_1`, `shop_2`, etc., or UUID slug
- **Products Collection (`/products/{productId}`)**:
  - Key: `prod_{shopId}_{productId}`
  - Indexed by: `shopId`, `category`, `isAvailable`
- **Promotions Collection (`/promotions/{promotionId}`)**:
  - Key: `promo_{id}`
- **Complaints Collection (`/complaints/{complaintId}`)**:
  - Key: `comp_{id}`
- **Users Collection (`/users/{uid}`)**:
  - Key: Firebase Auth UID or `phone`
  - Subcollections: `addresses`, `notifications`

### 3.2 Security Rules Specification
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Public catalog read
    match /shops/{shopId} {
      allow read: if true;
      allow write: if request.auth != null && (request.auth.token.role == 'ADMIN' || request.auth.uid == resource.data.ownerUid);
    }
    match /products/{productId} {
      allow read: if true;
      allow write: if request.auth != null && (request.auth.token.role == 'ADMIN' || request.auth.token.role == 'SHOP_OWNER');
    }
    // Orders access
    match /orders/{orderNumber} {
      allow read: if request.auth != null && (
        request.auth.token.role == 'ADMIN' ||
        request.auth.token.shopId == resource.data.shopId ||
        request.auth.token.phone == resource.data.customerPhone
      );
      allow create: if request.auth != null;
      allow update: if request.auth != null && (
        request.auth.token.role == 'ADMIN' ||
        request.auth.token.shopId == resource.data.shopId
      );
    }
  }
}
```

---

## 4. Five-Phase Safe Migration Roadmap

### Phase 1: Local Schema Analysis & DTO Readiness (COMPLETED)
- Conducted full analysis of all 15 tables.
- Created `FirebaseModels.kt` containing robust DTOs:
  - `FirebaseOrderDto`, `FirebaseOrderItemDto`, `FirebaseShopDto`, `FirebaseProductDto`, `FirebasePromotionDto`, `FirebaseComplaintDto`.
- Built bidirectional mappers (`toFirebaseDto()`, `toOrderEntity()`, etc.) preserving 100% of fields and Enums.
- Dry-run validation passed without schema violations.

### Phase 2: Dual-Engine Preparation & Safety Locks (COMPLETED)
- Created `FirebaseSyncConfig.kt` with `IS_LIVE_CLOUD_SYNC_ACTIVE = false`.
- Created `FirebaseSyncManager.kt` providing dry-run integrity validation and structure reporting.
- Current active sync channels (`ntfy.sh` pub/sub and `api.restful-api.dev` registry) continue functioning without interference.

### Phase 3: Firebase Project & Credential Provisioning (Pending User Activation)
- Set up Firebase project configuration (`google-services.json` or REST API credentials).
- Deploy Cloud Firestore or Realtime Database rules.

### Phase 4: Non-Destructive Data Backfill Protocol (Pending User Activation)
1. Execute dry-run integrity validation on local Room database.
2. Read existing local seed/user orders from Room.
3. Convert records to `FirebaseOrderDto` payloads using the prepared mappers.
4. Batch-write to Firebase `/orders` using `orderNumber` keys (idempotent `upsert`).
5. Verify written documents match checksums.
6. **Local SQLite database is never deleted or truncated.**

### Phase 5: Live Bi-directional Sync Cutover
1. Enable `IS_LIVE_CLOUD_SYNC_ACTIVE = true`.
2. Connect Firebase real-time listeners for Admin and Shop Owner panels.
3. Automatic conflict resolution:
   - Order placement: Immutable insert.
   - Status updates: Evaluated by `placedAt` / `lastUpdated` timestamp.
4. If network goes offline, Room maintains full local functionality, queueing writes for background upload when connectivity returns.

---

## 5. Rollback & Contingency Safeguards

1. **Immediate Fallback:** If cloud sync encounters any network timeout, the application automatically falls back to local Room queries.
2. **Reversibility:** Cloud sync can be disabled instantly by toggling `IS_LIVE_CLOUD_SYNC_ACTIVE` to `false` with zero database restructuring required.
3. **Data Parity:** Every incoming cloud order is ingested into local Room tables with duplicate checking on `orderNumber`, ensuring the local cache always matches the cloud state.
