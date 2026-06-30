package inventory

import (
	"time"

	"github.com/google/uuid"
)

// Inventory menyimpan stok per produk.
type Inventory struct {
	ProductID uuid.UUID `gorm:"type:uuid;primaryKey"`
	Quantity  int       `gorm:"not null"`
}

// ProcessedEvent adalah kunci idempotency.
// OrderID sebagai primary key = penjaga anti-duplikasi yang ditegakkan DB.
type ProcessedEvent struct {
	OrderID     uuid.UUID `gorm:"type:uuid;primaryKey"`
	ProcessedAt time.Time `gorm:"not null"`
}
