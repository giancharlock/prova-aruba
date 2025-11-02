package com.experis.dbmanager.controller;

import com.experis.dbmanager.constants.DbManagerConstants;
import com.experis.dbmanager.dto.CustomerDto;
import com.experis.dbmanager.dto.InvoiceDto;
import com.experis.dbmanager.dto.ResponseDto;
import com.experis.dbmanager.enumerations.CustomerType;
import com.experis.dbmanager.enumerations.InvoiceStatus;
import com.experis.dbmanager.service.IDbManagerService;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Tag(
    name = "CRUD REST APIs for DB Manager",
    description = "REST APIs to Create, Read, Update, and Delete customer and invoice details."
)
@RestController
@RequestMapping(path="/api", produces = {MediaType.APPLICATION_JSON_VALUE})
@AllArgsConstructor
public class DbManagerController {

    private final IDbManagerService dbManagerService;

    // --- Customer Read Endpoints ---

    @Operation(
            summary = "Create Customer REST API",
            description = "REST API to create a new customer."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "HTTP Status CREATED"),
            @ApiResponse(responseCode = "400", description = "Customer already exists or invalid data"),
            @ApiResponse(responseCode = "500", description = "HTTP Status Internal Server Error")
    })
    @PostMapping("/customers")
    public ResponseEntity<CustomerDto> createCustomer(@Validated @RequestBody CustomerDto customerDto) {
        CustomerDto savedCustomer = dbManagerService.createCustomer(customerDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedCustomer);
    }

    @Operation(
            summary = "Update Customer REST API",
            description = "REST API to update an existing customer's details."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "HTTP Status OK"),
            @ApiResponse(responseCode = "404", description = "Customer not found"),
            @ApiResponse(responseCode = "500", description = "HTTP Status Internal Server Error")
    })
    @PutMapping("/customers/{customerId}")
    public ResponseEntity<CustomerDto> updateCustomer(
            @Parameter(description = "Unique ID of the customer to update") @PathVariable int customerId,
            @Validated @RequestBody CustomerDto customerDto) {
        CustomerDto updatedCustomer = dbManagerService.updateCustomer(customerId, customerDto);
        return ResponseEntity.ok(updatedCustomer);
    }

    @Operation(
            summary = "Delete Customer REST API",
            description = "REST API to delete a customer."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "HTTP Status OK"),
            @ApiResponse(responseCode = "404", description = "Customer not found"),
            @ApiResponse(responseCode = "500", description = "HTTP Status Internal Server Error")
    })
    @DeleteMapping("/customers/{customerId}")
    public ResponseEntity<ResponseDto> deleteCustomer(
            @Parameter(description = "Unique ID of the customer to delete") @PathVariable int customerId) {
        boolean isDeleted = dbManagerService.deleteCustomer(customerId);
        if (isDeleted) {
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseDto(DbManagerConstants.STATUS_200, DbManagerConstants.MESSAGE_200));
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ResponseDto(DbManagerConstants.STATUS_417, DbManagerConstants.MESSAGE_417_DELETE));
        }
    }

    @Operation(
            summary = "Fetch Customers REST API",
            description = "REST API to fetch all customers details."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "HTTP Status OK"),
            @ApiResponse(responseCode = "500", description = "HTTP Status Internal Server Error")
    })
    @GetMapping("/customers")
    public ResponseEntity<Page<CustomerDto>> findCustomers(Pageable pageable) {
        Page<CustomerDto> customerPage = dbManagerService.findAllCustomers(pageable);
        return ResponseEntity.ok(customerPage);
    }

    @Operation(
        summary = "Fetch Customer by Username REST API",
        description = "REST API to fetch customer details based on a username."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "HTTP Status OK"),
        @ApiResponse(responseCode = "404", description = "Customer not found"),
        @ApiResponse(responseCode = "500", description = "HTTP Status Internal Server Error")
    })
    @GetMapping("/customers/search")
    public ResponseEntity<CustomerDto> findCustomerByUsername(
            @Parameter(description = "Username of the customer to fetch") @RequestParam String username) {
        return dbManagerService.findCustomerByUsername(username)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
        summary = "Fetch Customer by Type and Username REST API",
        description = "REST API to fetch customer details based on customer type and username."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "HTTP Status OK"),
        @ApiResponse(responseCode = "404", description = "Customer not found"),
        @ApiResponse(responseCode = "500", description = "HTTP Status Internal Server Error")
    })
    @GetMapping("/customers/search/by-type-and-username")
    public ResponseEntity<CustomerDto> findCustomerByTypeAndUsername(
            @Parameter(description = "Type of the customer (SDI or ARUBA)") @RequestParam CustomerType type,
            @Parameter(description = "Username of the customer") @RequestParam String username) {
        return dbManagerService.findCustomerByTypeAndUsername(type, username)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
        summary = "Fetch Customer by Type and ID REST API",
        description = "REST API to fetch customer details based on customer type and ID."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "HTTP Status OK"),
        @ApiResponse(responseCode = "404", description = "Customer not found"),
        @ApiResponse(responseCode = "500", description = "HTTP Status Internal Server Error")
    })
    @GetMapping("/customers/search/by-type-and-id")
    public ResponseEntity<CustomerDto> findCustomerByTypeAndId(
            @Parameter(description = "Type of the customer (SDI or ARUBA)") @RequestParam CustomerType type,
            @Parameter(description = "Unique ID of the customer") @RequestParam int customerId) {
        return dbManagerService.findCustomerByTypeAndId(type, customerId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // --- Invoice Read Endpoints ---

    @Operation(
            summary = "Create Invoice REST API",
            description = "REST API to create a new invoice for a specific customer."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "HTTP Status CREATED"),
            @ApiResponse(responseCode = "404", description = "Customer not found"),
            @ApiResponse(responseCode = "500", description = "HTTP Status Internal Server Error")
    })
    @PostMapping("/customers/{customerId}/invoices")
    public ResponseEntity<InvoiceDto> createInvoice(
            @Parameter(description = "ID of the customer to whom the invoice belongs") @PathVariable int customerId,
            @Validated @RequestBody InvoiceDto invoiceDto) {
        InvoiceDto createdInvoice = dbManagerService.createInvoice(customerId, invoiceDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdInvoice);
    }

    @Operation(
            summary = "Update Invoice REST API",
            description = "REST API to update an existing invoice's details."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "HTTP Status OK"),
            @ApiResponse(responseCode = "404", description = "Invoice or associated Customer not found"),
            @ApiResponse(responseCode = "500", description = "HTTP Status Internal Server Error")
    })
    @PutMapping("/invoices/{invoiceNumber}")
    public ResponseEntity<InvoiceDto> updateInvoice(
            @Parameter(description = "Unique number of the invoice to update") @PathVariable int invoiceNumber,
            @Validated @RequestBody InvoiceDto invoiceDto) {
        InvoiceDto updatedInvoice = dbManagerService.updateInvoice(invoiceNumber, invoiceDto);
        return ResponseEntity.ok(updatedInvoice);
    }

    @Operation(
            summary = "Delete Invoice REST API",
            description = "REST API to delete an invoice."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "HTTP Status OK"),
            @ApiResponse(responseCode = "404", description = "Invoice not found"),
            @ApiResponse(responseCode = "500", description = "HTTP Status Internal Server Error")
    })
    @DeleteMapping("/invoices/{invoiceNumber}")
    public ResponseEntity<ResponseDto> deleteInvoice(
            @Parameter(description = "Unique number of the invoice to delete") @PathVariable int invoiceNumber) {
        boolean isDeleted = dbManagerService.deleteInvoice(invoiceNumber);
        if (isDeleted) {
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseDto(DbManagerConstants.STATUS_200, DbManagerConstants.MESSAGE_200));
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ResponseDto(DbManagerConstants.STATUS_417, DbManagerConstants.MESSAGE_417_DELETE));
        }
    }

    @Operation(
        summary = "Fetch Invoice by Number REST API",
        description = "REST API to fetch invoice details based on its number."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "HTTP Status OK"),
        @ApiResponse(responseCode = "404", description = "Invoice not found"),
        @ApiResponse(responseCode = "500", description = "HTTP Status Internal Server Error")
    })
    @GetMapping("/invoices/{invoiceNumber}")
    public ResponseEntity<InvoiceDto> findInvoiceByNumber(@PathVariable int invoiceNumber) {
        return dbManagerService.findInvoiceByNumber(invoiceNumber)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
        summary = "Fetch Invoices with Pagination REST API",
        description = "REST API to fetch invoices with pagination. Can filter by status or a creation date range. If no filters are provided, it returns all invoices paginated."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "HTTP Status OK"),
        @ApiResponse(responseCode = "500", description = "HTTP Status Internal Server Error")
    })
    @GetMapping("/invoices")
    public ResponseEntity<Page<InvoiceDto>> findInvoices(
            @Parameter(description = "Filter by invoice status") @RequestParam(required = false) InvoiceStatus status,
            @Parameter(description = "Start date for creation date range (YYYY-MM-DD)") @RequestParam(required = false) LocalDateTime startDate,
            @Parameter(description = "End date for creation date range (YYYY-MM-DD). Defaults to today if empty.") @RequestParam(required = false) LocalDateTime endDate,
            Pageable pageable
    ) {
        Page<InvoiceDto> invoicePage;
        if (status != null) {
            invoicePage = dbManagerService.findInvoicesByStatus(status, pageable);
        } else if (startDate != null) {
            invoicePage = dbManagerService.findInvoicesByCreationDate(startDate, endDate, pageable);
        } else {
            invoicePage = dbManagerService.findAllInvoices(pageable);
        }
        return ResponseEntity.ok(invoicePage);
    }

    // Endpoint needed to change invoice status to SdI sent invoices
    @Operation(
            summary = "Update Invoice Status REST API",
            description = "REST API to update an existing invoice's status after it is sent to SdI."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "HTTP Status OK"),
            @ApiResponse(responseCode = "404", description = "Invoice or associated Customer not found"),
            @ApiResponse(responseCode = "500", description = "HTTP Status Internal Server Error")
    })
    @PutMapping("/invoices/{invoiceNumber}/status")
    public ResponseEntity<InvoiceDto> updateInvoiceStatus(@PathVariable int invoiceNumber, @RequestBody InvoiceDto invoiceDto) {
        InvoiceDto updatedInvoice = dbManagerService.updateInvoice(invoiceNumber, invoiceDto);
        return ResponseEntity.ok(updatedInvoice);
    }
}
