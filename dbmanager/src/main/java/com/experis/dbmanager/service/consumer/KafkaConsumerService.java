package com.experis.dbmanager.service.consumer;

import com.experis.dbmanager.dto.InvoiceDto;
import com.experis.dbmanager.dto.SdiNotificationDto;
import com.experis.dbmanager.enumerations.InvoiceStatus;
import com.experis.dbmanager.service.IDbManagerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaConsumerService {

    private final IDbManagerService dbManagerService;
    private final StreamBridge streamBridge;

    public void processIncomingInvoice(InvoiceDto invoiceDto) {
        if (invoiceDto.getCustomer() == null) {
            log.error("Received invoice with missing customer information. Discarding.");
            return;
        }

        log.info("Received invoice {} for customer {}", invoiceDto.getInvoiceNumber(), invoiceDto.getCustomer().getCustomerId());

        boolean customerExists = dbManagerService.findCustomerById(invoiceDto.getCustomer().getCustomerId()).isPresent();

        InvoiceDto savedInvoice;
        if (customerExists) {
            log.info("Customer {} exists. Processing invoice with status {}.", invoiceDto.getCustomer().getCustomerId(), invoiceDto.getInvoiceStatus());
            if (!isInternalInvoice(invoiceDto)) {
                dbManagerService.createInvoice(invoiceDto.getCustomer().getCustomerId(), invoiceDto);
            }else {
                invoiceDto.setInvoiceStatus(InvoiceStatus.INTERNAL_INVOICE_TOBE_SENT);
                savedInvoice = dbManagerService.createInvoice(invoiceDto.getCustomer().getCustomerId(), invoiceDto);

                streamBridge.send("publishSavedInvoice-out-0", savedInvoice);
                log.info("Invoice {} sent to SAVED topic.", savedInvoice.getInvoiceNumber());

                // Send to outgoing topic only if it's an internal invoice
                if (isInternalInvoice(savedInvoice)) {
                    streamBridge.send("publishOutgoingInvoice-out-0", savedInvoice);
                    log.info("Internal invoice {} sent to OUTGOING topic.", savedInvoice.getInvoiceNumber());
                }
            }

        } else {
            log.warn("Customer {} does not exist. Marking invoice as invalid.", invoiceDto.getCustomer().getCustomerId());
            invoiceDto.setInvoiceStatus(InvoiceStatus.INTERNAL_INVOICE_INVALID);
            savedInvoice = dbManagerService.createInvoice(invoiceDto.getCustomer().getCustomerId(), invoiceDto);

            streamBridge.send("publishSavedInvoice-out-0", savedInvoice);
            log.info("Invoice {} sent to SAVED topic as invalid.", savedInvoice.getInvoiceNumber());
        }
    }

    private boolean isInternalInvoice(InvoiceDto invoice) {
        if(invoice.getInvoiceStatus().equals(InvoiceStatus.EXTERNAL_INVOICE)){
            return false;
            }
        return true;
    }

    public void processSdiNotification(SdiNotificationDto notification) {
        log.info("Received SDI notification for invoice {} with status {}", notification.getInvoiceNumber(), notification.getStatus());

        dbManagerService.findInvoiceByNumber(notification.getInvoiceNumber()).ifPresentOrElse(invoice -> {
            log.info("Found invoice {}. Updating status to {}", invoice.getInvoiceNumber(), notification.getStatus());
            invoice.setInvoiceStatus(notification.getStatus());
            InvoiceDto updatedInvoice = dbManagerService.updateInvoice(invoice.getInvoiceNumber(), invoice);

            streamBridge.send("publishSavedInvoice-out-0", updatedInvoice);
            log.info("Updated invoice {} sent to SAVED topic.", updatedInvoice.getInvoiceNumber());
        }, () -> {
            log.error("Invoice {} not found for SDI notification. Marking as invalid.", notification.getInvoiceNumber());
            InvoiceDto invalidInvoice = new InvoiceDto();
            invalidInvoice.setInvoiceNumber(notification.getInvoiceNumber());
            invalidInvoice.setInvoiceStatus(InvoiceStatus.INTERNAL_INVOICE_INVALID);
            streamBridge.send("publishSavedInvoice-out-0", invalidInvoice);
        });
    }

    public void processSentInvoice(InvoiceDto sentInvoice) {
        log.info("Received sent confirmation for invoice {} with status {}", sentInvoice.getInvoiceNumber(), sentInvoice.getInvoiceStatus());

        dbManagerService.findInvoiceByNumber(sentInvoice.getInvoiceNumber()).ifPresentOrElse(invoice -> {
            log.info("Found invoice {}. Updating status to {}", invoice.getInvoiceNumber(), sentInvoice.getInvoiceStatus());
            invoice.setInvoiceStatus(InvoiceStatus.INTERNAL_INVOICE_SENT);
            dbManagerService.updateInvoice(invoice.getInvoiceNumber(), invoice);
        }, () -> {
            log.error("Invoice {} not found for SENT confirmation. This may indicate a data consistency issue.", sentInvoice.getInvoiceNumber());
        });
    }
}
