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

// Status siklus hidup sebuah reservasi (saga).
const (
	// StatusDeducted: stok sudah dipotong saat order dibuat (VA diminta), menunggu hasil bayar.
	StatusDeducted = "DEDUCTED"
	// StatusConfirmed: pembayaran sukses — potongan stok jadi final.
	StatusConfirmed = "CONFIRMED"
	// StatusReturned: pembayaran gagal/expired — stok sudah dikembalikan (kompensasi).
	StatusReturned = "RETURNED"
)

// Reservation adalah state saga per order SEKALIGUS kunci idempotency.
// OrderID sebagai primary key mencegah proses ganda order.created (INSERT duplikat gagal).
// productId + quantity disimpan di sini karena PaymentCompleted hanya membawa orderId —
// tanpa catatan ini Inventory tak tahu berapa yang harus dikembalikan saat bayar gagal.
type Reservation struct {
	OrderID   uuid.UUID `gorm:"type:uuid;primaryKey"`
	ProductID uuid.UUID `gorm:"type:uuid;not null"`
	Quantity  int       `gorm:"not null"`
	Status    string    `gorm:"not null"`
	CreatedAt time.Time
	UpdatedAt time.Time
}
