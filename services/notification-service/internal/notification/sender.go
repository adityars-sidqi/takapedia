package notification

import "log"

// Sender mengirim notifikasi ke user. Implementasi SMTP nyata ditunda;
// LogSender adalah stub yang cukup untuk mendemokan alur event → notifikasi.
type Sender interface {
	Send(recipient, subject, body string) error
}

type LogSender struct{}

func (LogSender) Send(recipient, subject, body string) error {
	log.Printf("[NOTIF] to=%s | subject=%q | %s", recipient, subject, body)
	return nil
}
