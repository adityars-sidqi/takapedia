package consumer

import (
	"context"
	"encoding/json"
	"errors"
	"log"

	"github.com/adityars-sidqi/takapedia/services/notification-service/internal/event"
	"github.com/adityars-sidqi/takapedia/services/notification-service/internal/notification"
	"github.com/segmentio/kafka-go"
)

type Consumer struct {
	reader  *kafka.Reader
	service *notification.Service
}

func NewConsumer(brokers []string, topic, groupID string, service *notification.Service) *Consumer {
	reader := kafka.NewReader(kafka.ReaderConfig{
		Brokers:     brokers,
		Topic:       topic,
		GroupID:     groupID,
		StartOffset: kafka.FirstOffset,
		MinBytes:    1,
		MaxBytes:    10e6,
	})
	return &Consumer{reader: reader, service: service}
}

func (c *Consumer) Run(ctx context.Context) error {
	log.Println("notification consumer started, waiting for messages...")
	for {
		msg, err := c.reader.ReadMessage(ctx)
		if err != nil {
			if errors.Is(err, context.Canceled) {
				log.Println("consumer shutting down...")
				return c.reader.Close()
			}
			log.Printf("error reading message: %v", err)
			continue
		}

		var evt event.PaymentCompletedEvent
		if err := json.Unmarshal(msg.Value, &evt); err != nil {
			log.Printf("failed to decode event, skipping: %v (raw: %s)", err, string(msg.Value))
			continue
		}

		err = c.service.Notify(evt)
		switch {
		case errors.Is(err, notification.ErrAlreadyProcessed):
			log.Printf("event %s already notified, skipping", evt.EventID)
		case err != nil:
			log.Printf("failed to notify for event %s: %v", evt.EventID, err)
		default:
			log.Printf("notification sent for order %s (%s)", evt.OrderID, evt.Outcome)
		}
	}
}
