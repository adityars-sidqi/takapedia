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

// countProcessedEvents menghitung baris di processed_events.
func countProcessedEvents(t *testing.T) int64 {
	t.Helper()
	var count int64
	if err := testDB.Model(&ProcessedEvent{}).Count(&count).Error; err != nil {
		t.Fatalf("failed to count processed events: %v", err)
	}
	return count
}

func TestReduceStock_HappyPath(t *testing.T) {
	cleanTables(t)
	service := NewService(testDB)

	productID := uuid.New()
	orderID := uuid.New()
	seedInventory(t, productID, 100)

	err := service.ReduceStock(orderID, productID, 3)

	if err != nil {
		t.Fatalf("expected no error, got: %v", err)
	}
	if got := getQuantity(t, productID); got != 97 {
		t.Errorf("expected quantity 97, got %d", got)
	}
	if got := countProcessedEvents(t); got != 1 {
		t.Errorf("expected 1 processed event, got %d", got)
	}
}

// Ini test paling penting: idempotency. Event sama diproses dua kali,
// stok hanya berkurang sekali.
func TestReduceStock_Idempotent(t *testing.T) {
	cleanTables(t)
	service := NewService(testDB)

	productID := uuid.New()
	orderID := uuid.New()
	seedInventory(t, productID, 100)

	// Proses pertama — sukses.
	if err := service.ReduceStock(orderID, productID, 3); err != nil {
		t.Fatalf("first call: expected no error, got: %v", err)
	}

	// Proses kedua dengan orderID SAMA — harus ditolak sebagai duplikat.
	err := service.ReduceStock(orderID, productID, 3)

	if err != ErrAlreadyProcessed {
		t.Errorf("second call: expected ErrAlreadyProcessed, got: %v", err)
	}
	// Stok harus TETAP 97, bukan 94 — inilah bukti idempotency.
	if got := getQuantity(t, productID); got != 97 {
		t.Errorf("expected quantity to stay 97 after duplicate, got %d", got)
	}
	// processed_events tetap 1 baris.
	if got := countProcessedEvents(t); got != 1 {
		t.Errorf("expected 1 processed event after duplicate, got %d", got)
	}
}

func TestReduceStock_ProductNotFound(t *testing.T) {
	cleanTables(t)
	service := NewService(testDB)

	orderID := uuid.New()
	unknownProduct := uuid.New() // tidak di-seed

	err := service.ReduceStock(orderID, unknownProduct, 3)

	if err == nil {
		t.Error("expected error for unknown product, got nil")
	}
}
