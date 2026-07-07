package event

import "github.com/google/uuid"

// StockReservationFailedEvent diterbitkan saat reserve gagal karena stok kurang.
// Dikonsumsi Order Service untuk membatalkan order (compensating transaction).
type StockReservationFailedEvent struct {
	OrderID   uuid.UUID `json:"orderId"`
	ProductID uuid.UUID `json:"productId"`
	Quantity  int       `json:"quantity"`
	Reason    string    `json:"reason"`
}
