package com.experis.dbmanager.service.impl;

import com.experis.dbmanager.dto.CustomerDto;
import com.experis.dbmanager.dto.InvoiceDto;
import com.experis.dbmanager.entity.Customer;
import com.experis.dbmanager.entity.Invoice;
import com.experis.dbmanager.enumerations.CustomerType;
import com.experis.dbmanager.enumerations.InvoiceStatus;
import com.experis.dbmanager.exception.DbManagerAlreadyExistsException;
import com.experis.dbmanager.exception.ResourceNotFoundException;
import com.experis.dbmanager.mapper.ApplicationMapper;
import com.experis.dbmanager.repository.CustomerRepository;
import com.experis.dbmanager.repository.InvoiceRepository;
import com.experis.dbmanager.service.IDbManagerService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class DbManagerServiceImpl implements IDbManagerService {

    private final CustomerRepository customerRepository;
    private final InvoiceRepository invoiceRepository;
    private final ApplicationMapper mapper;

    // ... (Customer methods remain the same)
    @Override
    public CustomerDto createCustomer(CustomerDto customerDto) {
        customerRepository.findByUsername(customerDto.getUsername()).ifPresent(c -> {
            throw new DbManagerAlreadyExistsException("Customer already registered with username: " + customerDto.getUsername());
        });
        Customer customer = mapper.toCustomer(customerDto);
        customer.setCreatedAt(LocalDateTime.now());
        Customer savedCustomer = customerRepository.save(customer);
        return mapper.toCustomerDto(savedCustomer);
    }

    @Override
    public Optional<CustomerDto> findCustomerByUsername(String username) {
        return customerRepository.findByUsername(username)
                .map(mapper::toCustomerDto);
    }

    @Override
    public Optional<CustomerDto> findCustomerByTypeAndUsername(CustomerType customerType, String username) {
        return customerRepository.findByCustomerTypeAndUsername(customerType, username)
                .map(mapper::toCustomerDto);
    }

    @Override
    public Optional<CustomerDto> findCustomerByTypeAndId(CustomerType customerType, int customerId) {
        return customerRepository.findByCustomerTypeAndCustomerId(customerType, customerId)
                .map(mapper::toCustomerDto);
    }

    @Override
    public Page<CustomerDto> findAllCustomers(Pageable pageable) {
        Page<Customer> customerPage = customerRepository.findAll(pageable);
        List<CustomerDto> dtos = mapCustomersToDtos(customerPage.getContent());
        return new PageImpl<>(dtos, pageable, customerPage.getTotalElements());
    }

    @Override
    public CustomerDto updateCustomer(int customerId, CustomerDto customerDto) {
        Customer customer = customerRepository.findById(customerId).orElseThrow(
                () -> new ResourceNotFoundException("Customer", "customerId", String.valueOf(customerId))
        );
        customer.setUsername(customerDto.getUsername());
        customer.setEmail(customerDto.getEmail());
        customer.setCustomerType(CustomerType.valueOf(customerDto.getCustomerType()));
        customer.setUpdatedAt(LocalDateTime.now());
        Customer updatedCustomer = customerRepository.save(customer);
        return mapper.toCustomerDto(updatedCustomer);
    }

    @Override
    public boolean deleteCustomer(int customerId) {
        if (!customerRepository.existsById(customerId)) {
            throw new ResourceNotFoundException("Customer", "customerId", String.valueOf(customerId));
        }
        customerRepository.deleteById(customerId);
        return true;
    }

    @Override
    public Optional<CustomerDto> findCustomerById(int customerId) {
        Optional<Customer> result = customerRepository.findById(customerId);
        if (result.isEmpty()) {
            throw new ResourceNotFoundException("Customer", "customerId", String.valueOf(customerId));
        }
        return result.map(mapper::toCustomerDto);
    }

    // --- Invoice Operations ---

    @Override
    public InvoiceDto createInvoice(int customerId, InvoiceDto invoiceDto) {
        Customer customer = customerRepository.findById(customerId).orElseThrow(
                () -> new ResourceNotFoundException("Customer", "customerId", String.valueOf(customerId))
        );
        Invoice invoice = mapper.toInvoice(invoiceDto);
        invoice.setCustomerId(customerId);
        invoice.setCreatedAt(LocalDateTime.now());
        Invoice savedInvoice = invoiceRepository.save(invoice);
        return mapper.toInvoiceDto(savedInvoice, customer);
    }

    @Override
    public Optional<InvoiceDto> findInvoiceByNumber(int invoiceNumber) {
        return invoiceRepository.findByInvoiceNumber(invoiceNumber).map(invoice -> {
            Customer customer = customerRepository.findById(invoice.getCustomerId()).orElseThrow(
                    () -> new ResourceNotFoundException("Customer", "customerId", " for invoice " + invoice.getInvoiceNumber())
            );
            return mapper.toInvoiceDto(invoice, customer);
        });
    }

    @Override
    public Page<InvoiceDto> findInvoicesByStatus(InvoiceStatus status, Pageable pageable) {
        Page<Invoice> invoicePage = invoiceRepository.findByInvoiceStatus(status, pageable);
        List<InvoiceDto> dtos = mapInvoicesToDtos(invoicePage.getContent());
        return new PageImpl<>(dtos, pageable, invoicePage.getTotalElements());
    }

    @Override
    public Page<InvoiceDto> findInvoicesByCreationDate(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        LocalDateTime effectiveEndDate = (endDate == null) ? LocalDateTime.now() : endDate;
        Page<Invoice> invoicePage = invoiceRepository.findByCreatedAtBetween(startDate, effectiveEndDate, pageable);
        List<InvoiceDto> dtos = mapInvoicesToDtos(invoicePage.getContent());
        return new PageImpl<>(dtos, pageable, invoicePage.getTotalElements());
    }

    @Override
    public Page<InvoiceDto> findAllInvoices(Pageable pageable) {
        Page<Invoice> invoicePage = invoiceRepository.findAll(pageable);
        List<InvoiceDto> dtos = mapInvoicesToDtos(invoicePage.getContent());
        return new PageImpl<>(dtos, pageable, invoicePage.getTotalElements());
    }

    @Override
    public InvoiceDto updateInvoice(int invoiceNumber, InvoiceDto invoiceDto) {
        Invoice invoice = invoiceRepository.findByInvoiceNumber(invoiceNumber).orElseThrow(
                () -> new ResourceNotFoundException("Invoice", "invoiceNumber", String.valueOf(invoiceNumber))
        );
        invoice.setInvoiceStatus(invoiceDto.getInvoiceStatus());
        invoice.setInvoice(invoiceDto.getInvoice());
        invoice.setUpdatedAt(LocalDateTime.now());
        Invoice updatedInvoice = invoiceRepository.save(invoice);
        Customer customer = customerRepository.findById(updatedInvoice.getCustomerId()).orElseThrow(
                () -> new ResourceNotFoundException("Customer", "customerId", " for invoice " + updatedInvoice.getInvoiceNumber())
        );
        return mapper.toInvoiceDto(updatedInvoice, customer);
    }

    @Override
    public boolean deleteInvoice(int invoiceNumber) {
        if (!invoiceRepository.findByInvoiceNumber(invoiceNumber).isPresent()) {
            throw new ResourceNotFoundException("Invoice", "invoiceNumber", String.valueOf(invoiceNumber));
        }
        invoiceRepository.deleteByInvoiceNumber(invoiceNumber);
        return true;
    }

    private List<InvoiceDto> mapInvoicesToDtos(List<Invoice> invoices) {
        if (invoices.isEmpty()) {
            return List.of();
        }
        List<Integer> customerIds = invoices.stream()
                .map(Invoice::getCustomerId)
                .distinct()
                .toList();
        Map<Integer, Customer> customerMap = customerRepository.findAllById(customerIds).stream()
                .collect(Collectors.toMap(Customer::getCustomerId, Function.identity()));
        return invoices.stream()
                .map(invoice -> {
                    Customer customer = customerMap.get(invoice.getCustomerId());
                    if (customer == null) {
                        return null;
                    }
                    return mapper.toInvoiceDto(invoice, customer);
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
    }

    private List<CustomerDto> mapCustomersToDtos(List<Customer> customers) {
        if (customers.isEmpty()) {
            return List.of();
        }
        return customers.stream()
                .map(mapper::toCustomerDto)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
    }
}
