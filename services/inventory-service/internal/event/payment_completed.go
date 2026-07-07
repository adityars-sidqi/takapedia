package event

import "github.com/google/uuid"

// Outcome pembayaran (mirror enum PaymentOutcome di Payment Service, Jackson → nama enum).
const (
	OutcomeSuccess = "SUCCESS"
	OutcomeFailed  = "FAILED"
	OutcomeExpired = "EXPIRED"
)

// PaymentCompletedEvent adalah mirror kontrak dari Payment Service (Java).
// Kita hanya butuh orderId + outcome; field lain (eventId, externalReference, dst)
// diabaikan — Go JSON melewati field tak dikenal tanpa error.
type PaymentCompletedEvent struct {
	OrderID uuid.UUID `json:"orderId"`
	Outcome string    `json:"outcome"`
}
