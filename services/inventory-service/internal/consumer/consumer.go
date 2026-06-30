package consumer

import (
	"context"
	"encoding/json"
	"errors"
	"log"

	"github.com/adityars-sidqi/takapedia/services/inventory-service/internal/event"
	"github.com/adityars-sidqi/takapedia/services/inventory-service/internal/inventory"
	"github.com/segmentio/kafka-go"
)

type Consumer struct {
	reader  *kafka.Reader
	service *inventory.Service
}

func NewConsumer(brokers []string, topic, groupID string, service *inventory.Service) *Consumer {
	reader := kafka.NewReader(kafka.ReaderConfig{
		Brokers:     brokers,
		Topic:       topic,
		GroupID:     groupID,
		StartOffset: kafka.FirstOffset, // hanya dipakai kalau group belum punya committed offset
		MinBytes:    1,
		MaxBytes:    10e6,
	})
	return &Consumer{reader: reader, service: service}
}

// Run menjalankan poll loop sampai context dibatalkan.
func (c *Consumer) Run(ctx context.Context) error {
	log.Println("inventory consumer started, waiting for messages...")
	for {
		// ReadMessage blocking sampai ada pesan atau context dibatalkan.
		// kafka-go otomatis meng-commit offset setelah ReadMessage (auto-commit).
		msg, err := c.reader.ReadMessage(ctx)
		if err != nil {
			// Context dibatalkan = shutdown normal.
			if errors.Is(err, context.Canceled) {
				log.Println("consumer shutting down...")
				return c.reader.Close()
			}
			log.Printf("error reading message: %v", err)
			continue
		}

		var evt event.OrderCreatedEvent
		if err := json.Unmarshal(msg.Value, &evt); err != nil {
			// JSON rusak / tak sesuai kontrak. Log dan lanjut — jangan macetkan loop.
			log.Printf("failed to decode event, skipping: %v (raw: %s)", err, string(msg.Value))
			continue
		}

		// Proses: kurangi stok, idempoten.
		err = c.service.ReduceStock(evt.OrderID, evt.ProductID, evt.Quantity)
		switch {
		case errors.Is(err, inventory.ErrAlreadyProcessed):
			log.Printf("order %s already processed, skipping", evt.OrderID)
		case err != nil:
			log.Printf("failed to process order %s: %v", evt.OrderID, err)
			// Catatan: untuk sekarang kita log & lanjut. Penanganan retry/DLQ
			// adalah lapisan berikutnya yang sengaja ditunda.
		default:
			log.Printf("order %s processed: product %s reduced by %d",
				evt.OrderID, evt.ProductID, evt.Quantity)
		}
	}
}
