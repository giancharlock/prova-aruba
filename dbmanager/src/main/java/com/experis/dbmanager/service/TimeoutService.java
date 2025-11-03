// Crea un nuovo file, es. TimeoutService.java

package com.experis.dbmanager.service;

import com.experis.dbmanager.entity.Invoice;
import com.experis.dbmanager.enumerations.InvoiceStatus;
import com.experis.dbmanager.repository.InvoiceRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@AllArgsConstructor
public class TimeoutService {

    private final InvoiceRepository invoiceRepository;
    private final StreamBridge streamBridge;
    private static final long TIMEOUT_MINUTES = 5;

    @Scheduled(fixedRate = 60000) // Esegui ogni 60 secondi
    public void checkForTimedOutInvoices() {
        log.info("Running scheduled check for timed-out invoices...");

        LocalDateTime timeoutThreshold = LocalDateTime.now().minusMinutes(TIMEOUT_MINUTES);

        // Cerca le fatture che sono in attesa di una risposta da troppo tempo
        List<Invoice> timedOutInvoices = invoiceRepository.findByInvoiceStatusAndStatusLastUpdatedAtBefore(
                InvoiceStatus.INTERNAL_INVOICE_TOBE_SENT,
                timeoutThreshold
        );

        if (timedOutInvoices.isEmpty()) {
            log.info("No timed-out invoices found.");
            return;
        }

        log.warn("Found {} timed-out invoices. Moving them to DLT.", timedOutInvoices.size());

        for (Invoice invoice : timedOutInvoices) {
            log.warn("Invoice {} has timed out. Current status: {}", invoice.getInvoiceNumber(), invoice.getInvoiceStatus());

            //Aggiorna lo stato della fattura a uno stato di errore/timeout
            invoice.setInvoiceStatus(InvoiceStatus.INVOICE_ERROR);
            invoiceRepository.save(invoice);

            // Invia ESPLICITAMENTE la fattura a un topic DLT dedicato
            // Usiamo un topic DLT specifico per i timeout di business
            streamBridge.send("business-dlt-out-0", invoice);
            log.info("Invoice {} sent to business DLT.", invoice.getInvoiceNumber());
        }
    }
}