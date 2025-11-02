package com.experis.dbmanager.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * Represents a generic event related to an invoice, used for Kafka communication.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceEventDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;


    private String eventType; // e.g., "INVOICE_VALIDATED", "INVOICE_SENT"
    private InvoiceDto payload; // The actual invoice data

}