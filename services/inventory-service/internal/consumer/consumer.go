package consumer

import (
	"context"
	"encoding/json"
	"errors"
	"log"

	"github.com/adityars-sidqi/takapedia/services/inventory-service/internal/event"
	"github.com/adityars-sidqi/takapedia/services/inventory-service/internal/inventory"
	"github.com/google/uuid"
	"github.com/segmentio/kafka-go"
)

// MessageHandler memproses satu payload pesan. Kasus normal (idempotent skip,
// JSON malformed) ditangani & di-log di dalam handler; loop tak boleh macet.
type MessageHandler func(value []byte)

type Consumer struct {
	reader *kafka.Reader
	handle MessageHandler
	name   string
}

func NewConsumer(brokers []string, topic, groupID, name string, handle MessageHandler) *Consumer {
	reader := kafka.NewReader(kafka.ReaderConfig{
		Brokers:     brokers,
		Topic:       topic,
		GroupID:     groupID,
		StartOffset: kafka.FirstOffset, // hanya dipakai kalau group belum punya committed offset
		MinBytes:    1,
		MaxBytes:    10e6,
	})
	return &Consumer{reader: reader, handle: handle, name: name}
}

// Run menjalankan poll loop sampai context dibatalkan.
func (c *Consumer) Run(ctx context.Context) error {
	log.Printf("[%s] consumer started, waiting for messages...", c.name)
	for {
		msg, err := c.reader.ReadMessage(ctx)
		if err != nil {
			if errors.Is(err, context.Canceled) {
				log.Printf("[%s] consumer shutting down...", c.name)
				return c.reader.Close()
			}
			log.Printf("[%s] error reading message: %v", c.name, err)
			continue
		}
		c.handle(msg.Value)
	}
}

// ReservationFailedPublisher menerbitkan event kompensasi saat reserve gagal (stok kurang).
type ReservationFailedPublisher interface {
	PublishReservationFailed(orderID, productID uuid.UUID, quantity int, reason string) error
}

// OrderCreatedHandler: order.created → reserve (potong stok saat order dibuat).
// Kalau stok kurang, publish StockReservationFailed sebagai kompensasi ke Order Service.
func OrderCreatedHandler(svc *inventory.Service, publisher ReservationFailedPublisher) MessageHandler {
	return func(value []byte) {
		var evt event.OrderCreatedEvent
		if err := json.Unmarshal(value, &evt); err != nil {
			log.Printf("order.created: gagal decode, skip: %v (raw: %s)", err, string(value))
			return
		}
		err := svc.Reserve(evt.OrderID, evt.ProductID, evt.Quantity)
		switch {
		case errors.Is(err, inventory.ErrAlreadyProcessed):
			log.Printf("order %s sudah di-reserve, skip", evt.OrderID)
		case errors.Is(err, inventory.ErrInsufficientStock):
			log.Printf("order %s: stok kurang → publish kompensasi StockReservationFailed", evt.OrderID)
			if perr := publisher.PublishReservationFailed(
				evt.OrderID, evt.ProductID, evt.Quantity, "INSUFFICIENT_STOCK"); perr != nil {
				log.Printf("gagal publish StockReservationFailed order %s: %v", evt.OrderID, perr)
			}
		case err != nil:
			log.Printf("gagal reserve order %s: %v", evt.OrderID, err)
		default:
			log.Printf("order %s reserved: stok produk %s dikurangi %d",
				evt.OrderID, evt.ProductID, evt.Quantity)
		}
	}
}

// PaymentCompletedHandler: payment.completed → confirm (SUCCESS) atau return (FAILED/EXPIRED).
func PaymentCompletedHandler(svc *inventory.Service) MessageHandler {
	return func(value []byte) {
		var evt event.PaymentCompletedEvent
		if err := json.Unmarshal(value, &evt); err != nil {
			log.Printf("payment.completed: gagal decode, skip: %v (raw: %s)", err, string(value))
			return
		}
		switch evt.Outcome {
		case event.OutcomeSuccess:
			logResult("confirm", evt.OrderID, svc.Confirm(evt.OrderID))
		case event.OutcomeFailed, event.OutcomeExpired:
			logResult("return", evt.OrderID, svc.Return(evt.OrderID))
		default:
			log.Printf("payment.completed: outcome tak dikenal %q untuk order %s, skip",
				evt.Outcome, evt.OrderID)
		}
	}
}

func logResult(action string, orderID uuid.UUID, err error) {
	switch {
	case errors.Is(err, inventory.ErrAlreadyProcessed):
		log.Printf("order %s sudah final (%s), skip", orderID, action)
	case errors.Is(err, inventory.ErrReservationNotFound):
		log.Printf("order %s: tak ada reservasi untuk %s (payment mendahului reserve?)", orderID, action)
	case err != nil:
		log.Printf("gagal %s order %s: %v", action, orderID, err)
	default:
		log.Printf("order %s %s ok", orderID, action)
	}
}
