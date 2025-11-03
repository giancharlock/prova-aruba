package com.experis.sender.functions;

import com.experis.dbmanager.dto.InvoiceDto;
import com.experis.dbmanager.enumerations.InvoiceStatus;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.SimpleMailMessage;

import java.util.function.Consumer;

@Configuration
@AllArgsConstructor
@Slf4j
public class SenderFunctions {

    private final JavaMailSender mailSender;
    private final StreamBridge streamBridge;

    /**
     * Consuma le fatture dal topic OUTGOING_INVOICE,
     * tenta l'invio via email (simulazione SdI) e
     * pubblica l'esito (SENT o NOT_SENT) sul topic SENT_INVOICE.
     */
    @Bean
    public Consumer<InvoiceDto> consumeOutgoingInvoice() {
        return invoiceDto -> {
            log.info("Ricevuta fattura {} da inviare a SdI (simulato via email).", invoiceDto.getInvoiceNumber());

            InvoiceStatus newStatus;
            try {
                // 1. Simula l'invio a SdI
                sendEmailNotification(invoiceDto);
                log.info("Invio a SdI (email) per fattura {} completato con successo.", invoiceDto.getInvoiceNumber());
                newStatus = InvoiceStatus.INTERNAL_INVOICE_SENT;

            } catch (Exception e) {
                // 2. Gestione fallimento invio email
                log.error("Errore durante l'invio a SdI (email) per fattura {}: {}", invoiceDto.getInvoiceNumber(), e.getMessage());
                newStatus = InvoiceStatus.INTERNAL_INVOICE_NOT_SENT;
                // NOTA: Non rilanciamo l'eccezione. Vogliamo che il fallimento sia gestito
                // e notificato a dbmanager, non che il binder ritenti all'infinito l'invio email.
            }

            invoiceDto.setInvoiceStatus(newStatus);

            // 3. Invia il risultato (successo o fallimento) al topic SENT_INVOICE
            // Se questo invio fallisce, l'intera funzione consumer sarà ritentata dal binder Kafka.
            boolean sent = streamBridge.send("publishSentInvoice-out-0", invoiceDto);

            if (sent) {
                log.info("Stato aggiornato {} inviato a SENT_INVOICE per fattura {}.", invoiceDto.getInvoiceStatus(), invoiceDto.getInvoiceNumber());
            } else {
                log.error("Errore critico: Impossibile inviare a SENT_INVOICE per fattura {}. Il messaggio sarà ritentato.", invoiceDto.getInvoiceNumber());
                // Rilancia un'eccezione per forzare il retry del binder
                throw new RuntimeException("Impossibile inviare a SENT_INVOICE. Ritentativo in corso.");
            }
        };
    }

    /**
     * Metodo helper per comporre e inviare l'email di simulazione.
     */
    private void sendEmailNotification(InvoiceDto invoiceDto) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("fatture@sender.com"); // Mittente fittizio
        message.setTo("sdi@governo.it");       // Destinatario fittizio per SdI

        // Cerca di usare l'email del cliente per il ReplyTo
        String customerEmail = "cliente@sconosciuto.com";
        if (invoiceDto.getCustomer() != null && invoiceDto.getCustomer().getEmail() != null) {
            customerEmail = invoiceDto.getCustomer().getEmail();
        }
        message.setReplyTo(customerEmail);
        message.setSubject("Invio Fattura SdI: " + invoiceDto.getInvoiceNumber());

        String text = String.format(
                "Invio fattura in corso per conto del cliente %s (ID: %d).\n\n" +
                        "Numero Fattura: %d\n" +
                        "Contenuto XML (estratto): \n%s",
                (invoiceDto.getCustomer() != null ? invoiceDto.getCustomer().getUsername() : "Sconosciuto"),
                (invoiceDto.getCustomer() != null ? invoiceDto.getCustomer().getCustomerId() : 0),
                invoiceDto.getInvoiceNumber(),
                (invoiceDto.getInvoice() != null && invoiceDto.getInvoice().length() > 200) ?
                        invoiceDto.getInvoice().substring(0, 200) + "..." :
                        invoiceDto.getInvoice()
        );

        message.setText(text);

        // Questo comando lancia l'eccezione se il mail server non risponde
        mailSender.send(message);
    }
}