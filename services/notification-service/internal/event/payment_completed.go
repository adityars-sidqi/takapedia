package event

import "github.com/google/uuid"

// Outcome pembayaran (mirror enum PaymentOutcome di Payment Service).
const (
	OutcomeSuccess = "SUCCESS"
	OutcomeFailed  = "FAILED"
	OutcomeExpired = "EXPIRED"
)

// PaymentCompletedEvent mirror kontrak dari Payment Service (Java).
// eventId dipakai sebagai kunci idempotency notifikasi. Field lain diabaikan bila tak dikenal.
type PaymentCompletedEvent struct {
	EventID           uuid.UUID `json:"eventId"`
	OrderID           uuid.UUID `json:"orderId"`
	Outcome           string    `json:"outcome"`
	ExternalReference string    `json:"externalReference"`
	FailureReason     string    `json:"failureReason"`
}
