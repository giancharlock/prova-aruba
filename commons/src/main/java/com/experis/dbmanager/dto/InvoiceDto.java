package com.experis.dbmanager.dto;

import com.experis.dbmanager.enumerations.InvoiceStatus;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class InvoiceDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private int invoiceNumber;
    private InvoiceStatus invoiceStatus;
    private String invoice;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;
    private CustomerDto customer;
    private String callback;
}
