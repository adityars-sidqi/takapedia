package main

import (
	"context"
	"log"
	"os"
	"os/signal"
	"strings"
	"sync"
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
	orderTopic := getEnv("KAFKA_TOPIC", "order.created")
	paymentTopic := getEnv("KAFKA_PAYMENT_TOPIC", "payment.completed")
	groupID := getEnv("KAFKA_GROUP_ID", "inventory-service")

	// --- Koneksi database (GORM) ---
	// TranslateError: true WAJIB agar gorm.ErrDuplicatedKey terdeteksi.
	db, err := gorm.Open(postgres.Open(dsn), &gorm.Config{
		TranslateError: true,
	})
	if err != nil {
		log.Fatalf("failed to connect to database: %v", err)
	}

	// AutoMigrate membuat tabel inventories & reservations kalau belum ada.
	if err := db.AutoMigrate(&inventory.Inventory{}, &inventory.Reservation{}); err != nil {
		log.Fatalf("failed to migrate: %v", err)
	}

	// --- Wiring: dua consumer (saga) ---
	// order.created → reserve (potong stok); payment.completed → confirm/return (kompensasi).
	service := inventory.NewService(db)
	consumers := []*consumer.Consumer{
		consumer.NewConsumer(brokers, orderTopic, groupID, "order.created",
			consumer.OrderCreatedHandler(service)),
		consumer.NewConsumer(brokers, paymentTopic, groupID, "payment.completed",
			consumer.PaymentCompletedHandler(service)),
	}

	// --- Graceful shutdown via context ---
	ctx, stop := signal.NotifyContext(context.Background(), syscall.SIGINT, syscall.SIGTERM)
	defer stop()

	var wg sync.WaitGroup
	for _, c := range consumers {
		wg.Add(1)
		go func(c *consumer.Consumer) {
			defer wg.Done()
			if err := c.Run(ctx); err != nil {
				log.Printf("consumer error: %v", err)
			}
		}(c)
	}
	wg.Wait()
}

func getEnv(key, fallback string) string {
	if v := os.Getenv(key); v != "" {
		return v
	}
	return fallback
}
