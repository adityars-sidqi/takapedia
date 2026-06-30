package main

import (
	"context"
	"log"
	"os"
	"os/signal"
	"strings"
	"syscall"

	"github.com/adityars-sidqi/takapedia/services/inventory-service/internal/consumer"
	"github.com/adityars-sidqi/takapedia/services/inventory-service/internal/inventory"
	"gorm.io/driver/postgres"
	"gorm.io/gorm"
)

func main() {
	// --- Konfigurasi dari environment ---
	dsn := getEnv("INVENTORY_DB_DSN",
		"host=localhost user=takapedia password=takapedia_dev dbname=inventory_db port=5432 sslmode=disable")
	brokers := strings.Split(getEnv("KAFKA_BROKERS", "localhost:9092"), ",")
	topic := getEnv("KAFKA_TOPIC", "order.created")
	groupID := getEnv("KAFKA_GROUP_ID", "inventory-service")

	// --- Koneksi database (GORM) ---
	// TranslateError: true WAJIB agar gorm.ErrDuplicatedKey terdeteksi.
	db, err := gorm.Open(postgres.Open(dsn), &gorm.Config{
		TranslateError: true,
	})
	if err != nil {
		log.Fatalf("failed to connect to database: %v", err)
	}

	// AutoMigrate membuat tabel inventories & processed_events kalau belum ada.
	if err := db.AutoMigrate(&inventory.Inventory{}, &inventory.ProcessedEvent{}); err != nil {
		log.Fatalf("failed to migrate: %v", err)
	}

	// --- Wiring ---
	service := inventory.NewService(db)
	cons := consumer.NewConsumer(brokers, topic, groupID, service)

	// --- Graceful shutdown via context ---
	ctx, stop := signal.NotifyContext(context.Background(), syscall.SIGINT, syscall.SIGTERM)
	defer stop()

	if err := cons.Run(ctx); err != nil {
		log.Fatalf("consumer error: %v", err)
	}
}

func getEnv(key, fallback string) string {
	if v := os.Getenv(key); v != "" {
		return v
	}
	return fallback
}
