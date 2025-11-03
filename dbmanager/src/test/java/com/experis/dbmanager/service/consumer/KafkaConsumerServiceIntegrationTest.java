package com.experis.dbmanager.service.consumer;

import com.experis.dbmanager.entity.Customer;
import com.experis.dbmanager.dto.CustomerDto;
import com.experis.dbmanager.dto.InvoiceDto;
import com.experis.dbmanager.dto.SdiNotificationDto;
import com.experis.dbmanager.enumerations.InvoiceStatus;
import com.experis.dbmanager.repository.CustomerRepository;
import com.experis.dbmanager.repository.InvoiceRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.Arrays;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1,
        topics = {
                KafkaConsumerServiceIntegrationTest.INCOMING_INVOICE_INPUT,
                KafkaConsumerServiceIntegrationTest.SDI_NOTIFICATION_INPUT,
                KafkaConsumerServiceIntegrationTest.SAVED_INVOICE_OUTPUT,
                KafkaConsumerServiceIntegrationTest.OUTGOING_INVOICE_OUTPUT
        })
class KafkaConsumerServiceIntegrationTest {

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    public static final String INCOMING_INVOICE_INPUT = "INCOMING_INVOICE";
    public static final String SDI_NOTIFICATION_INPUT = "DSI_NOTIFICATION";
    public static final String SAVED_INVOICE_OUTPUT = "SAVED_INCOMING_INVOICE";
    public static final String OUTGOING_INVOICE_OUTPUT = "OUTGOING_INVOICE";

    private Consumer<String, String> consumer;

    @BeforeEach
    void setUp() {
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("test-group", "true", embeddedKafkaBroker);
        consumer = new DefaultKafkaConsumerFactory<String, String>(consumerProps).createConsumer();
        embeddedKafkaBroker.consumeFromAllEmbeddedTopics(consumer);

        // Clean up database before each test
        invoiceRepository.deleteAll();
        customerRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        consumer.close();
    }

    @Test
    void testConsumeIncomingInvoice_whenCustomerExists() throws Exception {
        // Arrange
        Customer customer = new Customer();
        customer.setUsername("test-user");
        customer.setEmail("test@test.com");
        customer.setPassword("pwd");
        Customer savedCustomer = customerRepository.save(customer);

        CustomerDto customerDto = new CustomerDto();
        customerDto.setCustomerId(savedCustomer.getCustomerId());

        InvoiceDto incomingInvoice = new InvoiceDto();
        incomingInvoice.setCustomer(customerDto);
        incomingInvoice.setInvoice("<xml>test</xml>");

        // Act
        kafkaTemplate.send(INCOMING_INVOICE_INPUT, incomingInvoice);

        // Assert
        // Check SAVED_INVOICE_OUTPUT
        ConsumerRecord<String, String> savedRecord = KafkaTestUtils.getSingleRecord(consumer, SAVED_INVOICE_OUTPUT, Duration.ofMillis(5000L));
        assertNotNull(savedRecord);
        InvoiceDto savedResult = objectMapper.readValue(savedRecord.value(), InvoiceDto.class);
        assertThat(savedResult.getInvoiceStatus()).isEqualTo(InvoiceStatus.INTERNAL_INVOICE_TOBE_SENT);
        assertThat(savedResult.getCustomer().getCustomerId()).isEqualTo(savedCustomer.getCustomerId());

        // Check OUTGOING_INVOICE_OUTPUT
        ConsumerRecord<String, String> outgoingRecord = KafkaTestUtils.getSingleRecord(consumer, OUTGOING_INVOICE_OUTPUT,  Duration.ofMillis(5000L));
        assertNotNull(outgoingRecord);
        InvoiceDto outgoingResult = objectMapper.readValue(outgoingRecord.value(), InvoiceDto.class);
        assertThat(outgoingResult.getInvoiceStatus()).isEqualTo(InvoiceStatus.INTERNAL_INVOICE_TOBE_SENT);
        assertThat(outgoingResult.getInvoiceNumber()).isEqualTo(savedResult.getInvoiceNumber());
    }

    @Test
    void testConsumeIncomingInvoice_whenCustomerNotExists() throws Exception {
        // Arrange
        CustomerDto customerDto = new CustomerDto();
        customerDto.setCustomerId(999); // Non-existent customer
        InvoiceDto incomingInvoice = new InvoiceDto();
        incomingInvoice.setCustomer(customerDto);

        // Act
        kafkaTemplate.send(INCOMING_INVOICE_INPUT, incomingInvoice);

        // Assert
        // Check SAVED_INVOICE_OUTPUT
        ConsumerRecord<String, String> savedRecord = KafkaTestUtils.getSingleRecord(consumer, SAVED_INVOICE_OUTPUT,  Duration.ofMillis(5000L));
        assertNotNull(savedRecord);
        InvoiceDto savedResult = objectMapper.readValue(savedRecord.value(), InvoiceDto.class);
        assertThat(savedResult.getInvoiceStatus()).isEqualTo(InvoiceStatus.INTERNAL_INVOICE_INVALID);

        // Check that nothing is sent to OUTGOING_INVOICE_OUTPUT
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("temp-group", "true", embeddedKafkaBroker);
        try (Consumer<String, String> tempConsumer = new DefaultKafkaConsumerFactory<String, String>(consumerProps).createConsumer()) {
            tempConsumer.subscribe(Arrays.asList(OUTGOING_INVOICE_OUTPUT));
            assertThat(KafkaTestUtils.getRecords(tempConsumer,  Duration.ofMillis(1000L)).count()).isZero();
        }
    }

    @Test
    void testConsumeSdiNotification_whenInvoiceExists() throws Exception {
        // Arrange
        // Create an existing invoice in DB
        com.experis.dbmanager.entity.Invoice existingInvoiceEntity = new com.experis.dbmanager.entity.Invoice();
        existingInvoiceEntity.setCustomerId(1);
        existingInvoiceEntity.setInvoiceStatus(InvoiceStatus.INTERNAL_INVOICE_TOBE_SENT);
        existingInvoiceEntity.setInvoice("<xml></xml>");
        com.experis.dbmanager.entity.Invoice savedInvoice = invoiceRepository.save(existingInvoiceEntity);
        int existingInvoiceNumber = savedInvoice.getInvoiceNumber();

        SdiNotificationDto notification = new SdiNotificationDto(1, existingInvoiceNumber, InvoiceStatus.INTERNAL_INVOICE_DELIVERED);

        // Act
        kafkaTemplate.send(SDI_NOTIFICATION_INPUT, notification);

        // Assert
        ConsumerRecord<String, String> savedRecord = KafkaTestUtils.getSingleRecord(consumer, SAVED_INVOICE_OUTPUT,  Duration.ofMillis(5000L));
        assertNotNull(savedRecord);
        InvoiceDto savedResult = objectMapper.readValue(savedRecord.value(), InvoiceDto.class);
        assertThat(savedResult.getInvoiceNumber()).isEqualTo(existingInvoiceNumber);
        assertThat(savedResult.getInvoiceStatus()).isEqualTo(InvoiceStatus.INTERNAL_INVOICE_DELIVERED);
    }

    @Test
    void testConsumeSdiNotification_whenInvoiceNotExists() throws Exception {
        // Arrange
        int nonExistentInvoiceNumber = 404;
        SdiNotificationDto notification = new SdiNotificationDto(1, nonExistentInvoiceNumber, InvoiceStatus.INTERNAL_INVOICE_DELIVERED);

        // Act
        kafkaTemplate.send(SDI_NOTIFICATION_INPUT, notification);

        // Assert
        ConsumerRecord<String, String> savedRecord = KafkaTestUtils.getSingleRecord(consumer, SAVED_INVOICE_OUTPUT,  Duration.ofMillis(5000L));
        assertNotNull(savedRecord);
        InvoiceDto savedResult = objectMapper.readValue(savedRecord.value(), InvoiceDto.class);
        assertThat(savedResult.getInvoiceStatus()).isEqualTo(InvoiceStatus.INTERNAL_INVOICE_INVALID);
        assertThat(savedResult.getInvoiceNumber()).isEqualTo(nonExistentInvoiceNumber);
    }
}
