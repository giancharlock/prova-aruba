package com.experis.dbmanager.repository;

import com.experis.dbmanager.entity.Invoice;
import com.experis.dbmanager.enumerations.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Integer> {

    @Transactional(readOnly = true)
    Optional<Invoice> findByInvoiceNumber(int invoiceNumber);

    @Transactional(readOnly = true)
    Page<Invoice> findByInvoiceStatus(InvoiceStatus invoiceStatus, Pageable pageable);

    @Transactional(readOnly = true)
    Page<Invoice> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    @Transactional(readOnly = true)
    Page<Invoice> findAll(Pageable pageable);

    @Transactional
    void deleteByInvoiceNumber(int invoiceNumber);

    @Transactional(readOnly = true)
    List<Invoice> findByInvoiceStatusAndStatusLastUpdatedAtBefore(InvoiceStatus invoiceStatus, LocalDateTime timeoutThreshold);

}
