package com.experis.dbmanager.controller;

import com.experis.dbmanager.constants.DbManagerConstants;
import com.experis.dbmanager.dto.CustomerDto;
import com.experis.dbmanager.dto.InvoiceDto;
import com.experis.dbmanager.dto.ResponseDto;
import com.experis.dbmanager.entity.Customer;
import com.experis.dbmanager.enumerations.CustomerType;
import com.experis.dbmanager.enumerations.InvoiceStatus;
import com.experis.dbmanager.repository.CustomerRepository;
import com.experis.dbmanager.repository.InvoiceRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "eureka.client.enabled=false")
@ActiveProfiles("test")
@Disabled
class DbManagerControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    private String baseUrl;
    private Customer savedCustomer;
    private List<Integer> toBeDeleted = new ArrayList<>();

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api";

        // Pulisce il DB e salva un cliente di test prima di ogni test
        invoiceRepository.deleteAll();

        Customer customer = new Customer();
        customer.setUsername("testuser_controller");
        customer.setPassword("password");
        customer.setEmail("testcontroller@example.com");
        customer.setCustomerType(CustomerType.ARUBA);
        customer.setCreatedAt(LocalDateTime.now());
        customer.setCreatedBy("test-setup");
        savedCustomer = customerRepository.save(customer);
        toBeDeleted.add(savedCustomer.getCustomerId());
    }

    @AfterEach
    void tearDown() {
        invoiceRepository.deleteAll();
        toBeDeleted.forEach(customerRepository::deleteById);
    }

    // --- Customer Endpoint Tests ---

    @Test
    void createCustomer_shouldReturn201() throws Exception {
        CustomerDto requestDto = new CustomerDto();
        requestDto.setUsername("new_customer");
        requestDto.setEmail("new@example.com");
        requestDto.setPassword("password");
        requestDto.setCustomerType(CustomerType.ARUBA.name());
        requestDto.setCreatedAt(LocalDateTime.now());
        requestDto.setCreatedBy("test-setup");

        ResponseEntity<CustomerDto> response = restTemplate.postForEntity(baseUrl + "/customers", requestDto, CustomerDto.class);

        Assertions.assertNotNull(response.getBody());
        toBeDeleted.add(response.getBody().getCustomerId());
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getUsername()).isEqualTo("new_customer");
        assertThat(response.getBody().getCustomerId()).isPositive();
    }

    @Test
    void createCustomer_whenAlreadyExists_shouldReturn400() throws Exception {
        CustomerDto requestDto = new CustomerDto();
        requestDto.setUsername(savedCustomer.getUsername()); // Usa un username esistente
        requestDto.setEmail("testcontroller@example.com");
        requestDto.setCustomerType(CustomerType.ARUBA.name());

        ResponseEntity<Object> response = restTemplate.postForEntity(baseUrl + "/customers", requestDto, Object.class);

        assertThat(response.getStatusCode().is4xxClientError()).isTrue();
    }

    @Test
    void updateCustomer_shouldReturn200() throws Exception {
        CustomerDto updateDto = new CustomerDto();
        updateDto.setUsername("updated_username");
        updateDto.setEmail("updated@example.com");
        updateDto.setCustomerType(CustomerType.SDI.name());

        HttpEntity<CustomerDto> requestEntity = new HttpEntity<>(updateDto);
        ResponseEntity<CustomerDto> response = restTemplate.exchange(
                baseUrl + "/customers/" + savedCustomer.getCustomerId(),
                HttpMethod.PUT,
                requestEntity,
                CustomerDto.class
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getUsername()).isEqualTo("updated_username");
        assertThat(response.getBody().getCustomerType()).isEqualTo(CustomerType.SDI.name());
    }

    @Test
    void deleteCustomer_shouldReturn200() throws Exception {
        ResponseEntity<ResponseDto> response = restTemplate.exchange(
                baseUrl + "/customers/" + savedCustomer.getCustomerId(),
                HttpMethod.DELETE,
                null,
                ResponseDto.class
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody().getStatusCode()).isEqualTo(DbManagerConstants.STATUS_200);

        // Verifica che il cliente sia stato effettivamente cancellato
        assertThat(customerRepository.findById(savedCustomer.getCustomerId())).isEmpty();
    }

    @Test
    void findCustomerByUsername_shouldReturnCustomer() throws Exception {
        ResponseEntity<CustomerDto> response = restTemplate.getForEntity(
                baseUrl + "/customers/search?username={username}",
                CustomerDto.class,
                savedCustomer.getUsername()
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getUsername()).isEqualTo(savedCustomer.getUsername());
    }

    // --- Invoice Endpoint Tests ---

    @Test
    void createInvoice_shouldReturn201() throws Exception {
        InvoiceDto requestDto = new InvoiceDto();
        requestDto.setInvoiceStatus(InvoiceStatus.INTERNAL_INVOICE_NEW);
        requestDto.setInvoice("<xml>test</xml>");

        ResponseEntity<InvoiceDto> response = restTemplate.postForEntity(
                baseUrl + "/customers/{customerId}/invoices",
                requestDto,
                InvoiceDto.class,
                savedCustomer.getCustomerId()
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getInvoiceNumber()).isPositive();
        assertThat(response.getBody().getCustomer().getCustomerId()).isEqualTo(savedCustomer.getCustomerId());
    }

    @Test
    void findInvoiceByNumber_shouldReturnInvoice() throws Exception {
        // Prima crea una fattura da trovare
        InvoiceDto createRequest = new InvoiceDto();
        createRequest.setInvoiceStatus(InvoiceStatus.INTERNAL_INVOICE_NEW);
        createRequest.setInvoice("<xml>find me</xml>");
        ResponseEntity<InvoiceDto> createResponse = restTemplate.postForEntity(
                baseUrl + "/customers/{customerId}/invoices",
                createRequest,
                InvoiceDto.class,
                savedCustomer.getCustomerId()
        );
        int newInvoiceNumber = createResponse.getBody().getInvoiceNumber();

        // Ora cercala
        ResponseEntity<InvoiceDto> findResponse = restTemplate.getForEntity(
                baseUrl + "/invoices/{invoiceNumber}",
                InvoiceDto.class,
                newInvoiceNumber
        );

        assertThat(findResponse.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(findResponse.getBody()).isNotNull();
        assertThat(findResponse.getBody().getInvoiceNumber()).isEqualTo(newInvoiceNumber);
        assertThat(findResponse.getBody().getInvoice()).isEqualTo("<xml>find me</xml>");
    }
}