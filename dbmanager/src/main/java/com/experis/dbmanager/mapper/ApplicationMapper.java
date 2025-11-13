package com.experis.dbmanager.mapper;

import com.experis.dbmanager.dto.CustomerDto;
import com.experis.dbmanager.dto.InvoiceDto;
import com.experis.dbmanager.dto.SdiNotificationDto;
import com.experis.dbmanager.entity.Customer;
import com.experis.dbmanager.enumerations.CustomerType;
import com.experis.dbmanager.entity.Invoice;
import org.springframework.stereotype.Component;

@Component
public class ApplicationMapper {

    public CustomerDto toCustomerDto(Customer customer) {
        if (customer == null) {
            return null;
        }
        CustomerDto customerDto = new CustomerDto();
        customerDto.setCustomerId(customer.getCustomerId());
        customerDto.setUsername(customer.getUsername());
        customerDto.setPassword(customer.getPassword());
        customerDto.setEmail(customer.getEmail());
        customerDto.setCustomerType(String.valueOf(customer.getCustomerType()));
        customerDto.setCreatedAt(customer.getCreatedAt());
        customerDto.setCreatedBy(customer.getCreatedBy());
        customerDto.setUpdatedAt(customer.getUpdatedAt());
        customerDto.setUpdatedBy(customer.getUpdatedBy());
        return customerDto;
    }

    public Customer toCustomer(CustomerDto customerDto) {
        if (customerDto == null) {
            return null;
        }
        Customer customer = new Customer();
        customer.setCustomerId(customerDto.getCustomerId());
        customer.setUsername(customerDto.getUsername());
        customer.setPassword(customerDto.getPassword());
        customer.setEmail(customerDto.getEmail());
        customer.setCustomerType(CustomerType.valueOf(customerDto.getCustomerType()));
        customer.setCreatedAt(customerDto.getCreatedAt());
        customer.setCreatedBy(customerDto.getCreatedBy());
        customer.setUpdatedAt(customerDto.getUpdatedAt());
        customer.setUpdatedBy(customerDto.getUpdatedBy());
        return customer;
    }

    public InvoiceDto toInvoiceDto(Invoice invoice, Customer customer) {
        if (invoice == null) {
            return null;
        }
        InvoiceDto invoiceDto = new InvoiceDto();
        invoiceDto.setInvoiceNumber(invoice.getInvoiceNumber());
        invoiceDto.setInvoiceStatus(invoice.getInvoiceStatus());
        invoiceDto.setInvoice(invoice.getInvoice());
        invoiceDto.setCreatedAt(invoice.getCreatedAt());
        invoiceDto.setCreatedBy(invoice.getCreatedBy());
        invoiceDto.setUpdatedAt(invoice.getUpdatedAt());
        invoiceDto.setUpdatedBy(invoice.getUpdatedBy());
        invoiceDto.setCustomer(toCustomerDto(customer));
        invoiceDto.setCallback(invoice.getCallback());
        invoiceDto.setStatusLastUpdatedAt(invoice.getStatusLastUpdatedAt());
        invoiceDto.setCorrelationId(invoice.getCorrelationId());
        return invoiceDto;
    }

    public Invoice toInvoice(InvoiceDto invoiceDto) {
        if (invoiceDto == null) {
            return null;
        }
        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber(invoiceDto.getInvoiceNumber());
        invoice.setInvoiceStatus(invoiceDto.getInvoiceStatus());
        invoice.setInvoice(invoiceDto.getInvoice());
        invoice.setCreatedAt(invoiceDto.getCreatedAt());
        invoice.setCreatedBy(invoiceDto.getCreatedBy());
        invoice.setUpdatedAt(invoiceDto.getUpdatedAt());
        invoice.setUpdatedBy(invoiceDto.getUpdatedBy());
        if (invoiceDto.getCustomer() != null) {
            invoice.setCustomerId(invoiceDto.getCustomer().getCustomerId());
        }
        invoice.setCallback(invoiceDto.getCallback());
        invoice.setStatusLastUpdatedAt(invoiceDto.getStatusLastUpdatedAt());
        invoice.setCorrelationId(invoiceDto.getCorrelationId());
        return invoice;
    }

    public Invoice toInvoice(SdiNotificationDto notificationDto) {
        if (notificationDto == null) {
            return null;
        }
        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber(notificationDto.getInvoiceNumber());
        invoice.setInvoiceStatus(notificationDto.getStatus());
        invoice.setCreatedAt(notificationDto.getCreatedAt());
        invoice.setCreatedBy(notificationDto.getCreatedBy());
        invoice.setUpdatedAt(notificationDto.getUpdatedAt());
        invoice.setUpdatedBy(notificationDto.getUpdatedBy());
        if (notificationDto.getCustomerId() != null) {
            invoice.setCustomerId(notificationDto.getCustomerId());
        }
        invoice.setCorrelationId(notificationDto.getCorrelationId());
        return invoice;
    }

}
