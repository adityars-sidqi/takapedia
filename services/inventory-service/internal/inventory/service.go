package inventory

import (
	"errors"
	"time"

	"github.com/google/uuid"
	"gorm.io/gorm"
)

// ErrAlreadyProcessed menandakan event dengan orderId ini sudah pernah diproses.
// Ini bukan kegagalan — ini hasil normal idempotency.
var ErrAlreadyProcessed = errors.New("event already processed")

type Service struct {
	db *gorm.DB
}

func NewService(db *gorm.DB) *Service {
	return &Service{db: db}
}

// ReduceStock mengurangi stok untuk sebuah produk, secara idempoten.
// Seluruh operasi dibungkus satu transaksi: catat processed event + kurangi stok
// berhasil bersama atau gagal bersama.
func (s *Service) ReduceStock(orderID, productID uuid.UUID, quantity int) error {
	return s.db.Transaction(func(tx *gorm.DB) error {
		// Langkah 1: coba catat orderId sebagai "sudah diproses".
		// Karena OrderID adalah PRIMARY KEY, INSERT duplikat akan gagal secara atomik.
		// Inilah yang menutup race condition — DB yang menegakkan keunikan,
		// bukan cek-lalu-simpan manual yang punya celah.
		processed := ProcessedEvent{
			OrderID:     orderID,
			ProcessedAt: time.Now(),
		}
		if err := tx.Create(&processed).Error; err != nil {
			// Kalau gagal karena duplicate key, berarti sudah diproses.
			if errors.Is(err, gorm.ErrDuplicatedKey) {
				return ErrAlreadyProcessed
			}
			// Error lain (koneksi, dll) dikembalikan apa adanya.
			return err
		}

		// Langkah 2: insert berhasil = event baru. Sekarang kurangi stok.
		// Pakai ekspresi SQL "quantity - ?" agar pengurangan atomik di level DB,
		// bukan baca-lalu-tulis di aplikasi (yang juga rawan race).
		result := tx.Model(&Inventory{}).
			Where("product_id = ?", productID).
			UpdateColumn("quantity", gorm.Expr("quantity - ?", quantity))

		if result.Error != nil {
			return result.Error
		}
		if result.RowsAffected == 0 {
			// Produk tidak ada di inventory. Untuk sekarang, kembalikan error.
			// (Keputusan desain yang bisa ditinjau ulang nanti — lihat catatan.)
			return errors.New("product not found in inventory")
		}

		return nil
	})
}
