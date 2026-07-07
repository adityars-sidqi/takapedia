package inventory

import (
	"errors"

	"github.com/google/uuid"
	"gorm.io/gorm"
)

// ErrAlreadyProcessed menandakan transisi ini sudah pernah dilakukan (idempotency).
// Ini bukan kegagalan — hasil normal saat event duplikat atau terlambat datang.
var ErrAlreadyProcessed = errors.New("event already processed")

// ErrInsufficientStock: stok tak cukup (atau produk tak ada) untuk dipotong.
var ErrInsufficientStock = errors.New("insufficient stock or product not found")

// ErrReservationNotFound: tak ada reservasi untuk orderId (mis. PaymentCompleted
// datang tanpa order.created yang mendahului).
var ErrReservationNotFound = errors.New("reservation not found")

type Service struct {
	db *gorm.DB
}

func NewService(db *gorm.DB) *Service {
	return &Service{db: db}
}

// Reserve memotong stok saat order dibuat (VA diminta), secara idempoten.
// Satu transaksi: catat Reservation(DEDUCTED) + potong stok — sukses bersama atau gagal bersama.
func (s *Service) Reserve(orderID, productID uuid.UUID, quantity int) error {
	return s.db.Transaction(func(tx *gorm.DB) error {
		// Langkah 1: catat reservasi. OrderID PRIMARY KEY → INSERT duplikat gagal atomik.
		res := Reservation{
			OrderID:   orderID,
			ProductID: productID,
			Quantity:  quantity,
			Status:    StatusDeducted,
		}
		if err := tx.Create(&res).Error; err != nil {
			if errors.Is(err, gorm.ErrDuplicatedKey) {
				return ErrAlreadyProcessed
			}
			return err
		}

		// Langkah 2: potong stok atomik, HANYA jika cukup (quantity >= yang diminta).
		result := tx.Model(&Inventory{}).
			Where("product_id = ? AND quantity >= ?", productID, quantity).
			UpdateColumn("quantity", gorm.Expr("quantity - ?", quantity))
		if result.Error != nil {
			return result.Error
		}
		if result.RowsAffected == 0 {
			// Stok kurang atau produk tak ada → rollback (reservasi ikut batal).
			return ErrInsufficientStock
		}
		return nil
	})
}

// Confirm menandai reservasi CONFIRMED saat pembayaran sukses.
// Stok TIDAK berubah (sudah dipotong saat Reserve). Idempoten via guard status.
func (s *Service) Confirm(orderID uuid.UUID) error {
	return s.db.Transaction(func(tx *gorm.DB) error {
		result := tx.Model(&Reservation{}).
			Where("order_id = ? AND status = ?", orderID, StatusDeducted).
			UpdateColumn("status", StatusConfirmed)
		if result.Error != nil {
			return result.Error
		}
		if result.RowsAffected == 0 {
			// Tidak ada reservasi DEDUCTED (belum ada / sudah terminal) → skip idempoten.
			return ErrAlreadyProcessed
		}
		return nil
	})
}

// Return mengembalikan stok saat pembayaran gagal/expired (kompensasi saga).
// Idempoten: hanya transisi pertama DEDUCTED → RETURNED yang menambah stok.
func (s *Service) Return(orderID uuid.UUID) error {
	return s.db.Transaction(func(tx *gorm.DB) error {
		var res Reservation
		if err := tx.First(&res, "order_id = ?", orderID).Error; err != nil {
			if errors.Is(err, gorm.ErrRecordNotFound) {
				return ErrReservationNotFound
			}
			return err
		}

		// Transisi kondisional DEDUCTED → RETURNED. RowsAffected 0 = sudah terminal.
		result := tx.Model(&Reservation{}).
			Where("order_id = ? AND status = ?", orderID, StatusDeducted).
			UpdateColumn("status", StatusReturned)
		if result.Error != nil {
			return result.Error
		}
		if result.RowsAffected == 0 {
			return ErrAlreadyProcessed
		}

		// Kembalikan stok sebanyak yang dulu dipotong (dari catatan reservasi).
		return tx.Model(&Inventory{}).
			Where("product_id = ?", res.ProductID).
			UpdateColumn("quantity", gorm.Expr("quantity + ?", res.Quantity)).Error
	})
}
