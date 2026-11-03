# Early Checkout, Cancellation and Extend Final Review

## 1. Product Decisions

- Early checkout: unused booking-night value; penalty 20%; refund 80%. Example: 2,000,000 / 400,000 / 1,600,000 VND.
- Cancellation: existing configured full/partial policy; never the early-checkout rule.
- Extend: retain assigned room or reject; no automatic move; unassigned remains unassigned.
- Refund: `BANK_TRANSFER` only.

## 2. Current vs New Behavior

Existing code had cancellation eligibility/refunds, nightly snapshots, room assignment at check-in, checkout locks, `CHECKED_OUT -> DIRTY`, and outstanding-balance protection. It lacked refund destinations, early checkout, and extension commands. Cancellation HTTP commands now use booking ID; booking code remains human-facing.

## 3. Backend Changes

Added stay-modification API/service/DTOs/errors, snapshot-based calculation and extension. Cancellation now requires destination data only when its existing policy produces a refund.

## 4. Frontend Changes

Added a reusable bank-transfer refund form to cancellation, hidden when no refund applies and disabled during submission.

## 5. API Changes

Added early-checkout quote/execute and extend endpoints under `/api/v1/bookings/{bookingId}`; changed cancellation command to that identifier.

## 6. Database Changes

Migration `V202609041200` extends `payment_refunds`; no duplicate refund model was created.

## 7. Payment Changes

Refunds remain `PENDING`; provider success is never implied. Extension increases total/outstanding. The current payment-request API only supports initial `PENDING` bookings, so settlement of confirmed/checked-in extension balance is not defined; checkout protection remains active.

## 8. Refund Changes

Cancellation uses `booking-cancellation:{bookingId}` and early checkout `early-checkout:{bookingId}`. Amounts are backend-derived and checked against paid amount.

## 9. Concurrency Strategy

Pessimistic order is booking -> room type (extension) -> room. Availability is checked inside the transaction; unique refund keys are the final duplicate guard.

## 10. Idempotency

Retries observe terminal booking state. No provider refund callback exists in this repository.

## 11. Security

Early checkout is staff-only. Extension/cancellation enforce ownership without disclosing arbitrary IDs. Client financial amounts are not accepted; account numbers are not logged or returned.

## 12. Test Results

See the final implementation handoff for executed command results.

## 13. Remaining Limitations

- No defined payment request settles extension balance for an already confirmed/checked-in booking.
- No complete receptionist search flow exposes future stays for early checkout in the current frontend.
- Refund-destination encryption/tokenization is not defined by current architecture; plaintext logging is prohibited.

## 14. NOT VERIFIED

Provider refund execution/callback and production PostgreSQL race behavior unless listed as passed in the final handoff.
