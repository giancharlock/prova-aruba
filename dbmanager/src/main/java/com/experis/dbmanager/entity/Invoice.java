package com.experis.dbmanager.entity;

import com.experis.dbmanager.enumerations.InvoiceStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(name = "invoice")
@Getter @Setter @ToString
public class Invoice extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "invoice_number")
    private int invoiceNumber;

    @Column(name = "customer_id")
    private int customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "invoice_status")
    private InvoiceStatus invoiceStatus;

    @Lob
    @Column(name = "invoice", columnDefinition = "TEXT")
    private String invoice;

    @Column(name="status_last_updated_at", insertable = false)
    private LocalDateTime statusLastUpdatedAt;

    @Column(name="callback")
    private String callback;

}
