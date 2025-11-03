package com.experis.sender;

import com.experis.dbmanager.dto.CustomerDto;
import com.experis.dbmanager.dto.InvoiceDto;
import com.experis.dbmanager.enumerations.InvoiceStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.test.context.ActiveProfiles;
import io.restassured.RestAssured;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class SenderIntegrationTest {

    private static final String TOPIC_OUTGOING_INVOICE = "OUTGOING_INVOICE";
    private static final String TOPIC_SENT_INVOICE = "SENT_INVOICE";
    
    @Value("${spring.cloud.stream.kafka.binder.brokers}")
    private String kafkaBootstrapServers;

    @Autowired
    private ObjectMapper objectMapper;

    private KafkaProducer<String, InvoiceDto> kafkaProducer;

    @BeforeEach
    void setUp() {
        // Configura RestAssured per interrogare l'API di MailHog su localhost
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = 8025;

        // Configura un producer Kafka per inviare il messaggio di input al broker esterno
        Properties producerProps = new Properties();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers);
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class.getName());
        kafkaProducer = new KafkaProducer<>(producerProps);
    }

    @AfterEach
    void tearDown() {
        if (kafkaProducer != null) {
            kafkaProducer.close();
        }
        // Pulisce le email in MailHog dopo ogni test per mantenere l'isolamento
        given().when().delete("/api/v1/messages").then().statusCode(200);
    }

    private KafkaConsumer<String, InvoiceDto> createConsumer() {
        Properties consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers);
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "sender-integration-test-" + UUID.randomUUID());
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class.getName());
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        JsonDeserializer<InvoiceDto> jsonDeserializer = new JsonDeserializer<>(InvoiceDto.class, objectMapper);
        jsonDeserializer.setRemoveTypeHeaders(false);
        jsonDeserializer.addTrustedPackages("*");
        jsonDeserializer.setUseTypeHeaders(false);

        return new KafkaConsumer<>(consumerProps, new StringDeserializer(), jsonDeserializer);
    }

    @Test
    @DisplayName("Should consume from OUTGOING_INVOICE, send email, and produce to SENT_INVOICE")
    void testFullSenderFlow() {
        try (KafkaConsumer<String, InvoiceDto> sentInvoiceConsumer = createConsumer()) {
            sentInvoiceConsumer.subscribe(Collections.singletonList(TOPIC_SENT_INVOICE));

            // 1. Arrange: Crea una fattura di test da inviare
            CustomerDto customer = new CustomerDto();
            customer.setEmail("destinatario@sdi.gov.it");
            InvoiceDto invoiceToSend = new InvoiceDto();
            invoiceToSend.setInvoiceNumber(12345);
            invoiceToSend.setInvoice("<xml>Contenuto Fattura</xml>");
            invoiceToSend.setCustomer(customer);

            // 2. Act: Invia la fattura al topic OUTGOING_INVOICE sul broker Kafka esterno
            kafkaProducer.send(new ProducerRecord<>(TOPIC_OUTGOING_INVOICE, invoiceToSend));
            kafkaProducer.flush();

            // 3. Assert: Verifica il messaggio di conferma su SENT_INVOICE
            final InvoiceDto[] receivedConfirmation = {null};
            await().atMost(10, TimeUnit.SECONDS).until(() -> {
                ConsumerRecords<String, InvoiceDto> records = sentInvoiceConsumer.poll(Duration.ofMillis(200));
                if (!records.isEmpty()) {
                    ConsumerRecord<String, InvoiceDto> record = records.iterator().next();
                    receivedConfirmation[0] = record.value();
                    return true;
                }
                return false;
            });

            assertThat(receivedConfirmation[0]).isNotNull();
            assertThat(receivedConfirmation[0].getInvoiceNumber()).isEqualTo(12345);
            assertThat(receivedConfirmation[0].getInvoiceStatus()).isEqualTo(InvoiceStatus.INTERNAL_INVOICE_SENT);

            // 4. Assert: Verifica che l'email sia stata ricevuta dal server MailHog esterno
            given().when().get("/api/v2/messages")
                    .then().log().ifValidationFails()
                    .statusCode(200)
                    .body("total", equalTo(1))
                    .body("items[0].Content.Headers.To[0]", equalTo("sdi@governo.it"))
                    .body("items[0].Content.Body", containsString("<xml>Contenuto Fattura</xml>"));
        }
    }
}