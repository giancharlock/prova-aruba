package com.experis.dbmanager.service;

import com.experis.dbmanager.dto.CustomerDto;
import com.experis.dbmanager.dto.InvoiceDto;
import com.experis.dbmanager.enumerations.CustomerType;
import com.experis.dbmanager.enumerations.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
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
