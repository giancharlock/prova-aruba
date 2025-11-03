package com.experis.dbmanager.service.consumer;

import com.experis.dbmanager.dto.InvoiceDto;
import com.experis.dbmanager.dto.SdiNotificationDto;
import com.experis.dbmanager.enumerations.InvoiceStatus;
import com.experis.dbmanager.service.IDbManagerService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

@Service
@Slf4j
@AllArgsConstructor
public class KafkaConsumerService {

    private final IDbManagerService dbManagerService;
    private final StreamBridge streamBridge;

    /**
     * Consumes invoices from the INCOMING_INVOICE topic.
     * Validates the customer and then saves the invoice, pushing it to the next topics.
     */
    @Bean
    public Consumer<InvoiceDto> consumeIncomingInvoice() {
        return invoiceDto -> {
            log.info("Received invoice {} for customer {}", invoiceDto.getInvoiceNumber(), invoiceDto.getCustomer().getCustomerId());

            boolean customerExists = dbManagerService.findCustomerById(invoiceDto.getCustomer().getCustomerId()).isPresent();

            InvoiceDto savedInvoice;
            if (customerExists) {
                log.info("Customer {} exists. Processing invoice.", invoiceDto.getCustomer().getCustomerId());
                invoiceDto.setInvoiceStatus(InvoiceStatus.INTERNAL_INVOICE_TOBE_SENT);
                savedInvoice = dbManagerService.createInvoice(invoiceDto.getCustomer().getCustomerId(), invoiceDto);

                // Sender to SAVED_INCOMING_INVOICE and OUTGOING_INVOICE
                streamBridge.send("publishSavedInvoice-out-0", savedInvoice);
                streamBridge.send("publishOutgoingInvoice-out-0", savedInvoice);
                log.info("Invoice {} sent to SAVED and OUTGOING topics.", savedInvoice.getInvoiceNumber());
            } else {
                log.warn("Customer {} does not exist. Marking invoice as invalid.", invoiceDto.getCustomer().getCustomerId());
                invoiceDto.setInvoiceStatus(InvoiceStatus.INTERNAL_INVOICE_INVALID);
                savedInvoice = dbManagerService.createInvoice(invoiceDto.getCustomer().getCustomerId(), invoiceDto);

                // Sender only to SAVED_INCOMING_INVOICE
                streamBridge.send("publishSavedInvoice-out-0", savedInvoice);
                log.info("Invoice {} sent to SAVED topic as invalid.", savedInvoice.getInvoiceNumber());
            }
        };
    }

    /**
     * Consumes notifications from the DSI_NOTIFICATION topic.
     * Updates the status of an existing invoice.
     */
    @Bean
    public Consumer<SdiNotificationDto> consumeSdiNotification() {
        return notification -> {
            log.info("Received SDI notification for invoice {} with status {}", notification.getInvoiceNumber(), notification.getStatus());

            dbManagerService.findInvoiceByNumber(notification.getInvoiceNumber()).ifPresentOrElse(invoice -> {
                log.info("Found invoice {}. Updating status to {}", invoice.getInvoiceNumber(), notification.getStatus());
                invoice.setInvoiceStatus(notification.getStatus());
                InvoiceDto updatedInvoice = dbManagerService.updateInvoice(invoice.getInvoiceNumber(), invoice);

                // Sender the updated invoice to SAVED_INCOMING_INVOICE
                streamBridge.send("publishSavedInvoice-out-0", updatedInvoice);
                log.info("Updated invoice {} sent to SAVED topic.", updatedInvoice.getInvoiceNumber());
            }, () -> {
                log.error("Invoice {} not found for SDI notification. Marking as invalid.", notification.getInvoiceNumber());
                InvoiceDto invalidInvoice = new InvoiceDto();
                invalidInvoice.setInvoiceNumber(notification.getInvoiceNumber());
                invalidInvoice.setInvoiceStatus(InvoiceStatus.INTERNAL_INVOICE_INVALID);
                // We don't have full customer data here, but we can create a placeholder
                // Or handle it as a specific event for invalid notifications
                streamBridge.send("publishSavedInvoice-out-0", invalidInvoice);
            });
        };
    }

    /**
     * Consumes confirmation from the SENT_INVOICE topic.
     * Updates the status of an existing invoice to SENT or NOT_SENT.
     */
    @Bean
    public Consumer<InvoiceDto> consumeSentInvoice() {
        return sentInvoice -> {
            log.info("Received sent confirmation for invoice {} with status {}", sentInvoice.getInvoiceNumber(), sentInvoice.getInvoiceStatus());

            dbManagerService.findInvoiceByNumber(sentInvoice.getInvoiceNumber()).ifPresentOrElse(invoice -> {
                log.info("Found invoice {}. Updating status to {}", invoice.getInvoiceNumber(), sentInvoice.getInvoiceStatus());
                invoice.setInvoiceStatus(InvoiceStatus.INTERNAL_INVOICE_SENT); // Should be SENT or NOT_SENT
                dbManagerService.updateInvoice(invoice.getInvoiceNumber(), invoice);
            }, () -> {
                log.error("Invoice {} not found for SENT confirmation. This may indicate a data consistency issue.", sentInvoice.getInvoiceNumber());
            });
        };
    }
}
