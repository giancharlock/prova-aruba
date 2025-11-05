package com.experis.dbmanager.service.impl;

import com.experis.dbmanager.dto.CustomerDto;
import com.experis.dbmanager.dto.InvoiceDto;
import com.experis.dbmanager.entity.Customer;
import com.experis.dbmanager.enumerations.CustomerType;
import com.experis.dbmanager.entity.Invoice;
import com.experis.dbmanager.enumerations.InvoiceStatus;
import com.experis.dbmanager.exception.DbManagerAlreadyExistsException;
import com.experis.dbmanager.exception.ResourceNotFoundException;
import com.experis.dbmanager.mapper.ApplicationMapper;
import com.experis.dbmanager.repository.CustomerRepository;
import com.experis.dbmanager.repository.InvoiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Disabled
class DbManagerServiceImplTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private ApplicationMapper mapper;

    @InjectMocks
    private DbManagerServiceImpl dbManagerService;

    private Customer customer;
    private CustomerDto customerDto;
    private Invoice invoice;
    private InvoiceDto invoiceDto;

    @BeforeEach
    void setUp() {
        customer = new Customer();
        customer.setCustomerId(1);
        customer.setUsername("testuser");
        customer.setCustomerType(CustomerType.ARUBA);

        customerDto = new CustomerDto();
        customerDto.setCustomerId(1);
        customerDto.setUsername("testuser");
        customerDto.setCustomerType("ARUBA");

        invoice = new Invoice();
        invoice.setInvoiceNumber(101);
        invoice.setCustomerId(1);
        invoice.setInvoiceStatus(InvoiceStatus.INTERNAL_INVOICE_NEW);

        invoiceDto = new InvoiceDto();
        invoiceDto.setInvoiceNumber(101);
        invoiceDto.setCustomer(customerDto);
    }

    @Test
    void createCustomer_whenUsernameIsNew_shouldCreateCustomer() {
        // Arrange
        when(customerRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(mapper.toCustomer(any(CustomerDto.class))).thenReturn(customer);
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);
        when(mapper.toCustomerDto(any(Customer.class))).thenReturn(customerDto);

        // Act
        CustomerDto result = dbManagerService.createCustomer(customerDto);

        // Assert
        assertNotNull(result);
        assertEquals(customerDto.getUsername(), result.getUsername());
        verify(customerRepository, times(1)).findByUsername("testuser");
        verify(customerRepository, times(1)).save(customer);
    }

    @Test
    void createCustomer_whenUsernameExists_shouldThrowException() {
        // Arrange
        when(customerRepository.findByUsername(anyString())).thenReturn(Optional.of(customer));

        // Act & Assert
        assertThrows(DbManagerAlreadyExistsException.class, () -> {
            dbManagerService.createCustomer(customerDto);
        });
        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    void updateCustomer_whenCustomerExists_shouldUpdateAndReturnDto() {
        // Arrange
        when(customerRepository.findById(anyInt())).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);
        when(mapper.toCustomerDto(any(Customer.class))).thenReturn(customerDto);

        // Act
        CustomerDto result = dbManagerService.updateCustomer(1, customerDto);

        // Assert
        assertNotNull(result);
        verify(customerRepository, times(1)).findById(1);
        verify(customerRepository, times(1)).save(customer);
    }

    @Test
    void updateCustomer_whenCustomerDoesNotExist_shouldThrowException() {
        // Arrange
        when(customerRepository.findById(anyInt())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            dbManagerService.updateCustomer(99, customerDto);
        });
        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    void createInvoice_whenCustomerExists_shouldCreateInvoice() {
        // Arrange
        when(customerRepository.findById(anyInt())).thenReturn(Optional.of(customer));
        when(mapper.toInvoice(any(InvoiceDto.class))).thenReturn(invoice);
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(invoice);
        when(mapper.toInvoiceDto(any(Invoice.class), any(Customer.class))).thenReturn(invoiceDto);

        // Act
        InvoiceDto result = dbManagerService.createInvoice(1, invoiceDto);

        // Assert
        assertNotNull(result);
        assertEquals(101, result.getInvoiceNumber());
        verify(invoiceRepository, times(1)).save(invoice);
    }

    @Test
    void findInvoicesByStatus_shouldFetchCustomersEfficiently() {
        // Arrange
        Invoice invoice2 = new Invoice();
        invoice2.setInvoiceNumber(102);
        invoice2.setCustomerId(2);
        List<Invoice> invoices = List.of(invoice, invoice2);

        Customer customer2 = new Customer();
        customer2.setCustomerId(2);
        List<Customer> customers = List.of(customer, customer2);

        Page<Invoice> invoicePage = new PageImpl<>(invoices);
        Pageable pageable = PageRequest.of(0, 10);

        when(invoiceRepository.findByInvoiceStatus(any(InvoiceStatus.class), any(Pageable.class))).thenReturn(invoicePage);
        when(customerRepository.findAllById(List.of(1, 2))).thenReturn(customers);
        // Mocking the mapper for each invoice
        when(mapper.toInvoiceDto(eq(invoice), eq(customer))).thenReturn(new InvoiceDto());
        when(mapper.toInvoiceDto(eq(invoice2), eq(customer2))).thenReturn(new InvoiceDto());

        // Act
        Page<InvoiceDto> resultPage = dbManagerService.findInvoicesByStatus(InvoiceStatus.INTERNAL_INVOICE_NEW, pageable);

        // Assert
        assertEquals(2, resultPage.getContent().size());
        // This is the crucial verification for the N+1 optimization
        verify(customerRepository, times(1)).findAllById(anyList());
        verify(customerRepository, never()).findById(anyInt()); // Ensure we are not calling findById in a loop
    }
}
