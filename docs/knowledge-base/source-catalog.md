# HANBAY HOTEL — Knowledge Source Catalog

## Authority levels

- **High**: implemented domain/service rule confirmed by canonical requirement/workflow.
- **Medium**: implemented configurable behavior or API contract without detailed business prose.
- **Excluded**: no authoritative rule; not eligible as canonical policy.

## Business rule eligibility matrix

| Knowledge | Source | Authority | Eligible? | Reason |
|---|---|---:|---:|---|
| PENDING holds inventory 15 minutes | BR/SRS; BookingFactory; BookingRepository | High | Yes | Requirement and implementation agree |
| Overlap-based availability | Architecture/workflow; BookingRepository | High | Yes | Core booking invariant |
| Adult/child/max-guest capacity | Architecture; RoomType; booking DTO/service | High | Yes | Enforced by backend |
| Automatic child age classification | Booking DTO accepts counts, not ages | Excluded | No | No age threshold enforced by current booking API |
| Child under 6 is free | None | Excluded | No | Not defined |
| Payment confirms active PENDING booking when fully paid | Workflow; PaymentWebhookServiceImpl | High | Yes | Implemented transactional flow |
| Underpayment does not confirm | PaymentWebhookServiceImpl | High | Yes | Amount threshold enforced |
| Overpayment/late payment creates pending refund | Architecture; PaymentWebhookServiceImpl | High | Yes | Implemented reconciliation |
| Cash collected at checkout | CounterPaymentService; CheckOutServiceImpl | High | Yes | Records actual cash, receipt and transaction; zero final debt required for normal checkout |
| POS integration | No provider contract | Excluded | No | Not implemented |
| Cancellation 48h/24h/50% | API/workflow; cancellation properties/policy | High | Yes | Configured and enforced |
| Check-in default time 14:00 | Cancellation config/policy | Medium | Yes | Used for cancellation; check-in endpoint validates date, not hour |
| Early check-in fee | None | Excluded | No | Not defined |
| Check-out time/late fee | None | Excluded | No | Not defined |
| Check-in only CONFIRMED on check-in date | SRS/API; CheckInServiceImpl | High | Yes | Enforced |
| Check-in room must match type and be AVAILABLE | SRS/API; CheckInServiceImpl | High | Yes | Enforced |
| Check-out requires zero outstanding balance | Architecture/API; CheckOutServiceImpl | High | Yes | Enforced |
| Check-out makes room DIRTY | BR/SRS/workflow; CheckOutServiceImpl | High | Yes | Canonical transition |
| Early checkout refund 80%, penalty 20% unused nights | Product decision; StayModificationServiceImpl | High | Yes | Calculated from snapshots, then offset against debt and additional charges |
| Early checkout makes room DIRTY | Stay modification requirements/service | High | Yes | Same terminal transition as normal checkout |
| Cancellation and early checkout refund destination | Refund DTO/schema/services | High | Yes | BANK_TRANSFER only when refund applies |
| Extend stay retains assigned physical room | Product decision; StayModificationServiceImpl | High | Yes | Same room or reject; no automatic move |
| Unassigned extension remains unassigned | Product decision; StayModificationServiceImpl | High | Yes | Room-type availability until check-in |
| Settlement of extension balance | PaymentServiceImpl; PaymentWebhookServiceImpl; CounterPaymentService | High | Yes | Additional payment or checkout collection; booking retains CONFIRMED/CHECKED_IN |
| Unpaid initial booking checked in to pay at checkout | CheckInServiceImpl; PaymentWebhookServiceImpl | Excluded | No | Initial PENDING cannot check in; no pay-later confirmation flow |
| DIRTY -> CLEANING -> AVAILABLE | BR/SRS/workflow; Room/Housekeeping service | High | Yes | Canonical transition |
| AVAILABLE -> MAINTENANCE -> AVAILABLE | Architecture; Room/maintenance service | High | Yes | Enforced |
| Active add-on selection, quantity 1..20 | SRS/API; DTO/service | High | Yes | Enforced |
| Add-on price snapshot | BR/architecture; BookingServiceImpl | High | Yes | Implemented invariant |
| Pet/smoking/breakfast/pool/parking/transfer | None | Excluded | No | Not defined |
| Real address/phone/email | None | Excluded | No | Demo project; must not invent |

## Canonical source statement

Knowledge base được xây dựng từ business requirements, SRS, workflows, API contract và
business rules trong source code của HANBAY HOTEL. Không sử dụng tài liệu khách sạn bên ngoài
hoặc output do AI tạo.
