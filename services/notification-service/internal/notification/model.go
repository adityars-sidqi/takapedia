package notification

import (
	"time"

	"github.com/google/uuid"
)

// Jenis notifikasi berdasarkan outcome pembayaran.
const (
	TypePaymentSuccess = "PAYMENT_SUCCESS"
	TypePaymentFailed  = "PAYMENT_FAILED"
	TypePaymentExpired = "PAYMENT_EXPIRED"
)

// Notification adalah catatan (audit) notifikasi yang dikirim, SEKALIGUS kunci idempotency.
// EventID sebagai primary key: PaymentCompleted yang sama (redelivery) tak mengirim notifikasi ganda.
type Notification struct {
	EventID uuid.UUID `gorm:"type:uuid;primaryKey"`
	OrderID uuid.UUID `gorm:"type:uuid;not null"`
	Type    string    `gorm:"not null"`
	Channel string    `gorm:"not null"`
	Message string    `gorm:"not null"`
	SentAt  time.Time `gorm:"not null"`
}
