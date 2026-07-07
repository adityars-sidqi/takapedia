package notification

import (
	"errors"
	"fmt"

	"github.com/adityars-sidqi/takapedia/services/notification-service/internal/event"
	"github.com/google/uuid"
	"gorm.io/gorm"
	"time"
)

// ErrAlreadyProcessed = event sudah pernah menghasilkan notifikasi (idempotent, bukan kegagalan).
var ErrAlreadyProcessed = errors.New("event already processed")

type Service struct {
	db     *gorm.DB
	sender Sender
}

func NewService(db *gorm.DB, sender Sender) *Service {
	return &Service{db: db, sender: sender}
}

// Notify mencatat + mengirim notifikasi untuk sebuah PaymentCompletedEvent, idempoten.
// EventID PRIMARY KEY: insert duplikat → ErrAlreadyProcessed (tak mengirim ulang).
func (s *Service) Notify(evt event.PaymentCompletedEvent) error {
	notif := Notification{
		EventID: evt.EventID,
		OrderID: evt.OrderID,
		Type:    notificationType(evt.Outcome),
		Channel: "EMAIL",
		Message: buildMessage(evt),
		SentAt:  time.Now(),
	}

	if err := s.db.Create(&notif).Error; err != nil {
		if errors.Is(err, gorm.ErrDuplicatedKey) {
			return ErrAlreadyProcessed
		}
		return err
	}

	// Record tersimpan (idempoten). Kirim notifikasi (stub) sebagai best-effort.
	return s.sender.Send(recipient(evt.OrderID), subject(evt.Outcome), notif.Message)
}

func notificationType(outcome string) string {
	switch outcome {
	case event.OutcomeSuccess:
		return TypePaymentSuccess
	case event.OutcomeExpired:
		return TypePaymentExpired
	default:
		return TypePaymentFailed
	}
}

func subject(outcome string) string {
	switch outcome {
	case event.OutcomeSuccess:
		return "Pembayaran Berhasil — Invoice"
	case event.OutcomeExpired:
		return "Pembayaran Kedaluwarsa"
	default:
		return "Pembayaran Gagal"
	}
}

func buildMessage(evt event.PaymentCompletedEvent) string {
	switch evt.Outcome {
	case event.OutcomeSuccess:
		return fmt.Sprintf("Pembayaran untuk order %s berhasil. Terima kasih telah berbelanja!", evt.OrderID)
	case event.OutcomeExpired:
		return fmt.Sprintf("Pembayaran untuk order %s kedaluwarsa. Silakan pesan ulang.", evt.OrderID)
	default:
		return fmt.Sprintf("Pembayaran untuk order %s gagal (%s).", evt.OrderID, evt.FailureReason)
	}
}

// recipient: belum ada email user (event tak membawa userId/email). Stub berbasis orderId.
func recipient(orderID uuid.UUID) string {
	return fmt.Sprintf("user-of-order-%s@example.test", orderID)
}
