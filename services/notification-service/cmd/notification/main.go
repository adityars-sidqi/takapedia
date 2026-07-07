package main

import (
	"context"
	"log"
	"os"
	"os/signal"
	"strings"
	"syscall"

	"github.com/adityars-sidqi/takapedia/services/notification-service/internal/consumer"
	"github.com/adityars-sidqi/takapedia/services/notification-service/internal/notification"
	"gorm.io/driver/postgres"
	"gorm.io/gorm"
)

func main() {
	dsn := getEnv("NOTIFICATION_DB_DSN",
		"host=localhost user=takapedia password=takapedia_dev dbname=notification_db port=5432 sslmode=disable")
	brokers := strings.Split(getEnv("KAFKA_BROKERS", "localhost:9092"), ",")
	topic := getEnv("KAFKA_TOPIC", "payment.completed")
	groupID := getEnv("KAFKA_GROUP_ID", "notification-service")

	db, err := gorm.Open(postgres.Open(dsn), &gorm.Config{
		TranslateError: true,
	})
	if err != nil {
		log.Fatalf("failed to connect to database: %v", err)
	}

	if err := db.AutoMigrate(&notification.Notification{}); err != nil {
		log.Fatalf("failed to migrate: %v", err)
	}

	service := notification.NewService(db, notification.LogSender{})
	cons := consumer.NewConsumer(brokers, topic, groupID, service)

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
