package com.experis.dbmanager.dto;

import com.experis.dbmanager.enumerations.InvoiceStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceDto extends BaseDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private int invoiceNumber;
    private InvoiceStatus invoiceStatus;
    private String invoice;
    private CustomerDto customer;
    private String callback;
    private LocalDateTime statusLastUpdatedAt;
    private String correlationId;
}
