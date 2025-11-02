package com.experis.dbmanager.repository;

import com.experis.dbmanager.audit.AuditAwareImpl;
import com.experis.dbmanager.entity.Customer;
import com.experis.dbmanager.entity.Invoice;
import com.experis.dbmanager.enumerations.InvoiceStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = { "spring.cloud.stream.enabled=false" })
@Import(AuditAwareImpl.class)
class InvoiceRepositoryTest {

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @BeforeEach
    void setUp() {
        invoiceRepository.deleteAll();

        Customer customer = customerRepository.findByUsername("test_user_1").orElseThrow(
            () -> new IllegalStateException("Seeded customer 'test_user_1' not found. Check schema.sql")
        );

        Invoice invoice1 = new Invoice();
        invoice1.setCustomerId(customer.getCustomerId());
        invoice1.setInvoiceStatus(InvoiceStatus.INTERNAL_INVOICE_NEW);
        invoice1.setInvoice("<xml>invoice 1</xml>");
        invoice1.setCreatedAt(LocalDateTime.now().minusDays(1));
        invoice1.setCreatedBy("test-system");
        invoiceRepository.save(invoice1);

        Invoice invoice2 = new Invoice();
        invoice2.setCustomerId(customer.getCustomerId());
        invoice2.setInvoiceStatus(InvoiceStatus.INTERNAL_INVOICE_SENT);
        invoice2.setInvoice("<xml>invoice 2</xml>");
        invoice2.setCreatedAt(LocalDateTime.now());
        invoice2.setCreatedBy("test-system");
        invoiceRepository.save(invoice2);
    }

    @Test
    void findByInvoiceStatus_shouldReturnPagedResults() {
        Page<Invoice> resultPage = invoiceRepository.findByInvoiceStatus(InvoiceStatus.INTERNAL_INVOICE_NEW, PageRequest.of(0, 5));
        assertEquals(1, resultPage.getTotalElements());
        assertEquals(InvoiceStatus.INTERNAL_INVOICE_NEW, resultPage.getContent().get(0).getInvoiceStatus());
    }

    @Test
    void findByCreatedAtBetween_shouldReturnCorrectInvoices() {
        Page<Invoice> resultPage = invoiceRepository.findByCreatedAtBetween(
                LocalDateTime.now().minusDays(2), LocalDateTime.now(), PageRequest.of(0, 5));
        assertEquals(2, resultPage.getTotalElements(), "Should find two invoices in the last 2 days");
    }
}
