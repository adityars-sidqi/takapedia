package event

import (
	"time"

	"github.com/google/uuid"
)

// OrderCreatedEvent adalah mirror kontrak dari Order Service (Java).
// Nama field JSON harus persis cocok dengan output Jackson (camelCase).
type OrderCreatedEvent struct {
	OrderID    uuid.UUID `json:"orderId"`
	ProductID  uuid.UUID `json:"productId"`
	Quantity   int       `json:"quantity"`
	OccurredAt time.Time `json:"occurredAt"`
}
