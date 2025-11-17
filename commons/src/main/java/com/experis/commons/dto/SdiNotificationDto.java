package com.experis.commons.dto;

import com.experis.commons.enumerations.InvoiceStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SdiNotificationDto extends BaseDto implements Serializable {
    private Integer customerId;
    private Integer invoiceNumber;
    private InvoiceStatus status;
    private String correlationId;
}
