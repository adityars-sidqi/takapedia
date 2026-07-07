package notification

import (
	"testing"

	"github.com/adityars-sidqi/takapedia/services/notification-service/internal/event"
	"github.com/google/uuid"
)

// fakeSender mencatat pemanggilan Send supaya bisa di-assert.
type fakeSender struct {
	count int
}

func (f *fakeSender) Send(recipient, subject, body string) error {
	f.count++
	return nil
}

func countNotifications(t *testing.T) int64 {
	t.Helper()
	var c int64
	if err := testDB.Model(&Notification{}).Count(&c).Error; err != nil {
		t.Fatalf("count failed: %v", err)
	}
	return c
}

func successEvent(eventID, orderID uuid.UUID) event.PaymentCompletedEvent {
	return event.PaymentCompletedEvent{
		EventID:           eventID,
		OrderID:           orderID,
		Outcome:           event.OutcomeSuccess,
		ExternalReference: "VA-BCA-12345678",
	}
}

func TestNotify_recordsAndSends_onSuccess(t *testing.T) {
	cleanTable(t)
	sender := &fakeSender{}
	service := NewService(testDB, sender)
	eventID, orderID := uuid.New(), uuid.New()

	if err := service.Notify(successEvent(eventID, orderID)); err != nil {
		t.Fatalf("expected no error, got: %v", err)
	}
	if sender.count != 1 {
		t.Errorf("expected 1 send, got %d", sender.count)
	}
	if got := countNotifications(t); got != 1 {
		t.Errorf("expected 1 notification, got %d", got)
	}

	var n Notification
	if err := testDB.First(&n, "event_id = ?", eventID).Error; err != nil {
		t.Fatalf("read notification: %v", err)
	}
	if n.Type != TypePaymentSuccess {
		t.Errorf("expected type %s, got %s", TypePaymentSuccess, n.Type)
	}
}

// Idempotency: eventId sama diproses dua kali → notifikasi hanya sekali.
func TestNotify_idempotent_onDuplicateEvent(t *testing.T) {
	cleanTable(t)
	sender := &fakeSender{}
	service := NewService(testDB, sender)
	eventID, orderID := uuid.New(), uuid.New()

	if err := service.Notify(successEvent(eventID, orderID)); err != nil {
		t.Fatalf("first call: %v", err)
	}

	err := service.Notify(successEvent(eventID, orderID))
	if err != ErrAlreadyProcessed {
		t.Errorf("second call: expected ErrAlreadyProcessed, got: %v", err)
	}
	if sender.count != 1 {
		t.Errorf("expected send only once, got %d", sender.count)
	}
	if got := countNotifications(t); got != 1 {
		t.Errorf("expected 1 notification after duplicate, got %d", got)
	}
}

func TestNotify_typeFailed_onFailedOutcome(t *testing.T) {
	cleanTable(t)
	sender := &fakeSender{}
	service := NewService(testDB, sender)
	eventID, orderID := uuid.New(), uuid.New()

	evt := event.PaymentCompletedEvent{
		EventID:       eventID,
		OrderID:       orderID,
		Outcome:       event.OutcomeFailed,
		FailureReason: "GATEWAY_UNAVAILABLE",
	}
	if err := service.Notify(evt); err != nil {
		t.Fatalf("notify: %v", err)
	}

	var n Notification
	if err := testDB.First(&n, "event_id = ?", eventID).Error; err != nil {
		t.Fatalf("read notification: %v", err)
	}
	if n.Type != TypePaymentFailed {
		t.Errorf("expected type %s, got %s", TypePaymentFailed, n.Type)
	}
}
