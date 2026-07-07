package inventory

import (
	"testing"

	"github.com/google/uuid"
)

// seedInventory menaruh satu produk dengan stok awal tertentu.
func seedInventory(t *testing.T, productID uuid.UUID, quantity int) {
	t.Helper()
	inv := Inventory{ProductID: productID, Quantity: quantity}
	if err := testDB.Create(&inv).Error; err != nil {
		t.Fatalf("failed to seed inventory: %v", err)
	}
}

// getQuantity membaca stok terkini sebuah produk.
func getQuantity(t *testing.T, productID uuid.UUID) int {
	t.Helper()
	var inv Inventory
	if err := testDB.First(&inv, "product_id = ?", productID).Error; err != nil {
		t.Fatalf("failed to read inventory: %v", err)
	}
	return inv.Quantity
}

// getReservationStatus membaca status reservasi sebuah order.
func getReservationStatus(t *testing.T, orderID uuid.UUID) string {
	t.Helper()
	var res Reservation
	if err := testDB.First(&res, "order_id = ?", orderID).Error; err != nil {
		t.Fatalf("failed to read reservation: %v", err)
	}
	return res.Status
}

// countReservations menghitung baris di reservations.
func countReservations(t *testing.T) int64 {
	t.Helper()
	var count int64
	if err := testDB.Model(&Reservation{}).Count(&count).Error; err != nil {
		t.Fatalf("failed to count reservations: %v", err)
	}
	return count
}

func TestReserve_HappyPath(t *testing.T) {
	cleanTables(t)
	service := NewService(testDB)

	productID := uuid.New()
	orderID := uuid.New()
	seedInventory(t, productID, 100)

	if err := service.Reserve(orderID, productID, 3); err != nil {
		t.Fatalf("expected no error, got: %v", err)
	}
	if got := getQuantity(t, productID); got != 97 {
		t.Errorf("expected quantity 97, got %d", got)
	}
	if got := getReservationStatus(t, orderID); got != StatusDeducted {
		t.Errorf("expected status DEDUCTED, got %s", got)
	}
	if got := countReservations(t); got != 1 {
		t.Errorf("expected 1 reservation, got %d", got)
	}
}

// Idempotency: order.created yang sama diproses dua kali, stok hanya berkurang sekali.
func TestReserve_Idempotent(t *testing.T) {
	cleanTables(t)
	service := NewService(testDB)

	productID := uuid.New()
	orderID := uuid.New()
	seedInventory(t, productID, 100)

	if err := service.Reserve(orderID, productID, 3); err != nil {
		t.Fatalf("first call: expected no error, got: %v", err)
	}

	err := service.Reserve(orderID, productID, 3)
	if err != ErrAlreadyProcessed {
		t.Errorf("second call: expected ErrAlreadyProcessed, got: %v", err)
	}
	if got := getQuantity(t, productID); got != 97 {
		t.Errorf("expected quantity to stay 97 after duplicate, got %d", got)
	}
	if got := countReservations(t); got != 1 {
		t.Errorf("expected 1 reservation after duplicate, got %d", got)
	}
}

// Stok kurang → ErrInsufficientStock, stok tetap, TAPI reservasi dicatat FAILED
// (bukan rollback) supaya idempoten & memicu kompensasi.
func TestReserve_InsufficientStock(t *testing.T) {
	cleanTables(t)
	service := NewService(testDB)

	productID := uuid.New()
	orderID := uuid.New()
	seedInventory(t, productID, 2)

	err := service.Reserve(orderID, productID, 3)
	if err != ErrInsufficientStock {
		t.Errorf("expected ErrInsufficientStock, got: %v", err)
	}
	if got := getQuantity(t, productID); got != 2 {
		t.Errorf("expected quantity to stay 2, got %d", got)
	}
	if got := countReservations(t); got != 1 {
		t.Errorf("expected reservation recorded (FAILED), got %d", got)
	}
	if got := getReservationStatus(t, orderID); got != StatusFailed {
		t.Errorf("expected status FAILED, got %s", got)
	}

	// Redelivery order.created yang sama → ditolak duplikat (kompensasi tak dobel).
	if err := service.Reserve(orderID, productID, 3); err != ErrAlreadyProcessed {
		t.Errorf("duplicate after failed: expected ErrAlreadyProcessed, got: %v", err)
	}
	if got := countReservations(t); got != 1 {
		t.Errorf("expected still 1 reservation after duplicate, got %d", got)
	}
}

func TestReserve_ProductNotFound(t *testing.T) {
	cleanTables(t)
	service := NewService(testDB)

	err := service.Reserve(uuid.New(), uuid.New(), 3) // produk tak di-seed
	if err != ErrInsufficientStock {
		t.Errorf("expected ErrInsufficientStock for unknown product, got: %v", err)
	}
}

// Pembayaran sukses: reservasi jadi CONFIRMED, stok TIDAK berubah (sudah dipotong saat reserve).
func TestConfirm_HappyPath(t *testing.T) {
	cleanTables(t)
	service := NewService(testDB)

	productID := uuid.New()
	orderID := uuid.New()
	seedInventory(t, productID, 100)
	mustReserve(t, service, orderID, productID, 3)

	if err := service.Confirm(orderID); err != nil {
		t.Fatalf("expected no error, got: %v", err)
	}
	if got := getReservationStatus(t, orderID); got != StatusConfirmed {
		t.Errorf("expected status CONFIRMED, got %s", got)
	}
	if got := getQuantity(t, productID); got != 97 {
		t.Errorf("expected quantity to stay 97 after confirm, got %d", got)
	}
}

func TestConfirm_Idempotent(t *testing.T) {
	cleanTables(t)
	service := NewService(testDB)

	productID := uuid.New()
	orderID := uuid.New()
	seedInventory(t, productID, 100)
	mustReserve(t, service, orderID, productID, 3)

	if err := service.Confirm(orderID); err != nil {
		t.Fatalf("first confirm: expected no error, got: %v", err)
	}
	if err := service.Confirm(orderID); err != ErrAlreadyProcessed {
		t.Errorf("second confirm: expected ErrAlreadyProcessed, got: %v", err)
	}
}

// Pembayaran gagal/expired: stok dikembalikan (kompensasi), reservasi jadi RETURNED.
func TestReturn_RestoresStock(t *testing.T) {
	cleanTables(t)
	service := NewService(testDB)

	productID := uuid.New()
	orderID := uuid.New()
	seedInventory(t, productID, 100)
	mustReserve(t, service, orderID, productID, 3) // stok → 97

	if err := service.Return(orderID); err != nil {
		t.Fatalf("expected no error, got: %v", err)
	}
	if got := getQuantity(t, productID); got != 100 {
		t.Errorf("expected quantity restored to 100, got %d", got)
	}
	if got := getReservationStatus(t, orderID); got != StatusReturned {
		t.Errorf("expected status RETURNED, got %s", got)
	}
}

// Return dua kali tidak boleh menambah stok dua kali.
func TestReturn_Idempotent(t *testing.T) {
	cleanTables(t)
	service := NewService(testDB)

	productID := uuid.New()
	orderID := uuid.New()
	seedInventory(t, productID, 100)
	mustReserve(t, service, orderID, productID, 3)

	if err := service.Return(orderID); err != nil {
		t.Fatalf("first return: expected no error, got: %v", err)
	}
	if err := service.Return(orderID); err != ErrAlreadyProcessed {
		t.Errorf("second return: expected ErrAlreadyProcessed, got: %v", err)
	}
	if got := getQuantity(t, productID); got != 100 {
		t.Errorf("expected quantity to stay 100 (no double restore), got %d", got)
	}
}

func TestReturn_ReservationNotFound(t *testing.T) {
	cleanTables(t)
	service := NewService(testDB)

	if err := service.Return(uuid.New()); err != ErrReservationNotFound {
		t.Errorf("expected ErrReservationNotFound, got: %v", err)
	}
}

// Defense-in-depth: setelah CONFIRMED (bayar sukses), Return terlambat TIDAK boleh
// mengembalikan stok — guard status menutup celah ini.
func TestReturn_AfterConfirm_DoesNotRestore(t *testing.T) {
	cleanTables(t)
	service := NewService(testDB)

	productID := uuid.New()
	orderID := uuid.New()
	seedInventory(t, productID, 100)
	mustReserve(t, service, orderID, productID, 3)

	if err := service.Confirm(orderID); err != nil {
		t.Fatalf("confirm: expected no error, got: %v", err)
	}
	if err := service.Return(orderID); err != ErrAlreadyProcessed {
		t.Errorf("late return: expected ErrAlreadyProcessed, got: %v", err)
	}
	if got := getQuantity(t, productID); got != 97 {
		t.Errorf("expected quantity to stay 97 (no restore after confirm), got %d", got)
	}
}

func mustReserve(t *testing.T, service *Service, orderID, productID uuid.UUID, qty int) {
	t.Helper()
	if err := service.Reserve(orderID, productID, qty); err != nil {
		t.Fatalf("setup reserve failed: %v", err)
	}
}
