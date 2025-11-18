package com.experis.service;

import com.experis.commons.dto.CustomerDto;
import com.experis.commons.dto.InvoiceDto;
import com.experis.commons.enumerations.CustomerType;
import com.experis.commons.enumerations.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Optional;

public interface IDbManagerService {

    //Customer
    CustomerDto createCustomer(CustomerDto customerDto);
    Optional<CustomerDto> findCustomerByUsername(String username);
    Optional<CustomerDto> findCustomerByTypeAndUsername(CustomerType customerType, String username);
    Optional<CustomerDto> findCustomerByTypeAndId(CustomerType customerType, int customerId);
    CustomerDto updateCustomer(int customerId, CustomerDto customerDto);
    Page<CustomerDto> findAllCustomers(Pageable pageable);
    boolean deleteCustomer(int customerId);
    Optional<CustomerDto> findCustomerById(int customerId);


    // Invoice

    InvoiceDto createInvoice(int customerId, InvoiceDto invoiceDto);
    Optional<InvoiceDto> findInvoiceByNumber(int invoiceNumber);
    Page<InvoiceDto> findInvoicesByStatus(InvoiceStatus status, Pageable pageable);
    Page<InvoiceDto> findInvoicesByCreationDate(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    Page<InvoiceDto> findAllInvoices(Pageable pageable);
    InvoiceDto updateInvoice(int invoiceNumber, InvoiceDto invoiceDto);
    boolean deleteInvoice(int invoiceNumber);

}
