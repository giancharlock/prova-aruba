package com.experis.dbmanager.dto;

import com.experis.dbmanager.enumerations.InvoiceStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SdiNotificationDto implements Serializable {
    private Integer customerId;
    private Integer invoiceNumber;
    private InvoiceStatus status;
}
