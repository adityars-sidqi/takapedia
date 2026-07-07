package publisher

import (
	"context"
	"encoding/json"

	"github.com/adityars-sidqi/takapedia/services/inventory-service/internal/event"
	"github.com/google/uuid"
	"github.com/segmentio/kafka-go"
)

// ReservationFailedPublisher menerbitkan StockReservationFailedEvent ke Kafka.
type ReservationFailedPublisher struct {
	writer *kafka.Writer
}

func NewReservationFailedPublisher(brokers []string, topic string) *ReservationFailedPublisher {
	return &ReservationFailedPublisher{
		writer: &kafka.Writer{
			Addr:     kafka.TCP(brokers...),
			Topic:    topic,
			Balancer: &kafka.LeastBytes{},
		},
	}
}

func (p *ReservationFailedPublisher) PublishReservationFailed(
	orderID, productID uuid.UUID, quantity int, reason string) error {
	payload, err := json.Marshal(event.StockReservationFailedEvent{
		OrderID:   orderID,
		ProductID: productID,
		Quantity:  quantity,
		Reason:    reason,
	})
	if err != nil {
		return err
	}
	return p.writer.WriteMessages(context.Background(),
		kafka.Message{Key: []byte(orderID.String()), Value: payload})
}

func (p *ReservationFailedPublisher) Close() error {
	return p.writer.Close()
}
