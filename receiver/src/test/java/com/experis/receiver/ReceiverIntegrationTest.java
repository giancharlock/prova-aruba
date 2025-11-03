package com.experis.receiver;

import com.experis.dbmanager.dto.CustomerDto;
import com.experis.dbmanager.dto.InvoiceDto;
import com.experis.dbmanager.dto.SdiNotificationDto;
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
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = "springdoc.api-docs.enabled=false"
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ReceiverIntegrationTest {

    private static final String TOPIC_INCOMING_INVOICE = "INCOMING_INVOICE";
    private static final String TOPIC_DSI_NOTIFICATION = "DSI_NOTIFICATION";
    private static final String TOPIC_SAVED_INVOICE = "SAVED_INCOMING_INVOICE";
    private static final String TOPIC_BUSINESS_DLT = "business.DLT";
    private static final String CALLBACK_URL = "http://mock.callback.url/notify";

    @Container
    private static final KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.0.1"));

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.stream.kafka.binder.brokers", kafka::getBootstrapServers);
        registry.add("eureka.client.enabled", () -> "false");
    }

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private RestTemplate restTemplate;

    private KafkaProducer<String, Object> kafkaProducer;
    private MockRestServiceServer mockCallbackServer;

    @BeforeEach
    void setUp() {
        mockCallbackServer = MockRestServiceServer.bindTo(restTemplate).build();

        Properties producerProps = new Properties();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class.getName());
        kafkaProducer = new KafkaProducer<>(producerProps);
    }

    @AfterEach
    void tearDown() {
        if (kafkaProducer != null) {
            kafkaProducer.close();
        }
        mockCallbackServer.reset();
    }

    private <T> KafkaConsumer<String, T> createConsumer(Class<T> valueType) {
        Properties consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "receiver-integration-test-" + UUID.randomUUID());
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class.getName());
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        JsonDeserializer<T> jsonDeserializer = new JsonDeserializer<>(valueType, objectMapper);
        jsonDeserializer.setRemoveTypeHeaders(false);
        jsonDeserializer.addTrustedPackages("*");
        jsonDeserializer.setUseTypeHeaders(false);

        return new KafkaConsumer<>(consumerProps, new StringDeserializer(), jsonDeserializer);
    }

    private <T> T awaitAndReceive(KafkaConsumer<String, T> consumer, int expectedInvoiceNumber) {
        final T[] receivedMessage = (T[]) new Object[1];
        await().atMost(15, TimeUnit.SECONDS).until(() -> {
            ConsumerRecords<String, T> records = consumer.poll(Duration.ofMillis(200));
            for (ConsumerRecord<String, T> record : records) {
                if (record.value() instanceof InvoiceDto && ((InvoiceDto) record.value()).getInvoiceNumber() == expectedInvoiceNumber) {
                    receivedMessage[0] = record.value();
                    return true;
                } else if (record.value() instanceof SdiNotificationDto && ((SdiNotificationDto) record.value()).getInvoiceNumber() == expectedInvoiceNumber) {
                    receivedMessage[0] = record.value();
                    return true;
                }
            }
            return false;
        });
        return receivedMessage[0];
    }

    @Test
    @Order(1)
    @DisplayName("SalvaFatturaInterna: Happy Path Completo")
    void testSalvaFatturaInterna_FullAsyncFlow() throws Exception {
        try (KafkaConsumer<String, InvoiceDto> kafkaConsumer = createConsumer(InvoiceDto.class)) {
            kafkaConsumer.subscribe(Collections.singletonList(TOPIC_INCOMING_INVOICE));

            InvoiceDto requestInvoice = new InvoiceDto(901, null, "<xml>Contenuto fattura 901</xml>", null, null, null, null, new CustomerDto(1, "test_user_1", null, null, null, null, null, null, null), CALLBACK_URL);

            mockCallbackServer.expect(requestTo(CALLBACK_URL)).andRespond(withSuccess());

            MvcResult mvcResult = mockMvc.perform(post("/api/salvaFatturaInterna")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestInvoice)))
                    .andExpect(request().asyncStarted()).andReturn();

            InvoiceDto receivedInvoice = awaitAndReceive(kafkaConsumer, 901);
            assertThat(receivedInvoice).isNotNull();
            assertThat(receivedInvoice.getInvoiceStatus()).isEqualTo(InvoiceStatus.INTERNAL_INVOICE_NEW);

            receivedInvoice.setInvoiceStatus(InvoiceStatus.INTERNAL_INVOICE_TOBE_SENT);
            kafkaProducer.sender(new ProducerRecord<>(TOPIC_SAVED_INVOICE, receivedInvoice));
            kafkaProducer.flush();

            mockMvc.perform(asyncDispatch(mvcResult)).andExpect(status().isOk());
            mockCallbackServer.verify();
        }
    }

    @Test
    @Order(2)
    @DisplayName("SalvaFatturaEsterna: Happy Path Completo")
    void testSalvaFatturaEsterna_HappyPath() throws Exception {
        try (KafkaConsumer<String, InvoiceDto> kafkaConsumer = createConsumer(InvoiceDto.class)) {
            kafkaConsumer.subscribe(Collections.singletonList(TOPIC_INCOMING_INVOICE));

            InvoiceDto requestInvoice = new InvoiceDto(902, null, null, null, null, null, null, new CustomerDto(2, "test_user_2", null, null, null, null, null, null, null), CALLBACK_URL);

            mockCallbackServer.expect(requestTo(CALLBACK_URL)).andRespond(withSuccess());

            MvcResult mvcResult = mockMvc.perform(post("/api/salvaFatturaEsterna")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestInvoice)))
                    .andExpect(request().asyncStarted()).andReturn();

            InvoiceDto receivedInvoice = awaitAndReceive(kafkaConsumer, 902);
            assertThat(receivedInvoice).isNotNull();
            assertThat(receivedInvoice.getInvoiceStatus()).isEqualTo(InvoiceStatus.EXTERNAL_INVOICE);

            receivedInvoice.setInvoiceStatus(InvoiceStatus.INTERNAL_INVOICE_TOBE_SENT);
            kafkaProducer.sender(new ProducerRecord<>(TOPIC_SAVED_INVOICE, receivedInvoice));
            kafkaProducer.flush();

            mockMvc.perform(asyncDispatch(mvcResult)).andExpect(status().isOk());
            mockCallbackServer.verify();
        }
    }

    @Test
    @Order(3)
    @DisplayName("NotificaSdi: Happy Path Completo")
    void testNotificaSdi_HappyPath() throws Exception {
        try (KafkaConsumer<String, SdiNotificationDto> kafkaConsumer = createConsumer(SdiNotificationDto.class)) {
            kafkaConsumer.subscribe(Collections.singletonList(TOPIC_DSI_NOTIFICATION));

            SdiNotificationDto notification = new SdiNotificationDto(1, 901, InvoiceStatus.INTERNAL_INVOICE_DELIVERED);

            MvcResult mvcResult = mockMvc.perform(post("/api/notificaSdI")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(notification)))
                    .andExpect(request().asyncStarted()).andReturn();

            SdiNotificationDto receivedNotification = awaitAndReceive(kafkaConsumer, 901);
            assertThat(receivedNotification).isNotNull();

            InvoiceDto savedInvoiceResponse = new InvoiceDto(notification.getInvoiceNumber(), notification.getStatus(), null, null, null, null, null, new CustomerDto(notification.getCustomerId(), null, null, null, null, null, null, null, null), null);
            kafkaProducer.sender(new ProducerRecord<>(TOPIC_SAVED_INVOICE, savedInvoiceResponse));
            kafkaProducer.flush();

            mockMvc.perform(asyncDispatch(mvcResult)).andExpect(status().isOk());
        }
    }

    @Test
    @Order(4)
    @DisplayName("Caso Limite: Fallimento Callback invia a DLT")
    void testSalvaFattura_whenCallbackFails_sendsToDlt() throws Exception {
        try (KafkaConsumer<String, InvoiceDto> kafkaConsumer = createConsumer(InvoiceDto.class)) {
            kafkaConsumer.subscribe(Arrays.asList(TOPIC_INCOMING_INVOICE, TOPIC_BUSINESS_DLT));

            InvoiceDto requestInvoice = new InvoiceDto(903, null, null, null, null, null, null, new CustomerDto(3, null, null, null, null, null, null, null, null), CALLBACK_URL);

            mockCallbackServer.expect(requestTo(CALLBACK_URL)).andRespond(withServerError());

            MvcResult mvcResult = mockMvc.perform(post("/api/salvaFatturaInterna")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestInvoice)))
                    .andExpect(request().asyncStarted()).andReturn();

            InvoiceDto receivedOnIncoming = awaitAndReceive(kafkaConsumer, 903);
            assertThat(receivedOnIncoming).isNotNull();

            receivedOnIncoming.setInvoiceStatus(InvoiceStatus.INTERNAL_INVOICE_TOBE_SENT);
            kafkaProducer.sender(new ProducerRecord<>(TOPIC_SAVED_INVOICE, receivedOnIncoming));
            kafkaProducer.flush();

            mockMvc.perform(asyncDispatch(mvcResult)).andExpect(status().isServiceUnavailable());
            mockCallbackServer.verify();

            InvoiceDto receivedOnDlt = awaitAndReceive(kafkaConsumer, 903);
            assertThat(receivedOnDlt).isNotNull();
        }
    }

    @Test
    @Order(5)
    @DisplayName("Caso Limite: Timeout della risposta da Kafka")
    void testSalvaFattura_whenResponseTimesOut() throws Exception {
        InvoiceDto requestInvoice = new InvoiceDto(904, null, null, null, null, null, null, new CustomerDto(4, null, null, null, null, null, null, null, null), CALLBACK_URL);

        MvcResult mvcResult = mockMvc.perform(post("/api/salvaFatturaInterna")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestInvoice)))
                .andExpect(request().asyncStarted()).andReturn();

        // Non inviamo una risposta su TOPIC_SAVED_INVOICE, forzando il timeout.
        // Il test attenderà che il timeout configurato in application-test.yml (2s) scatti.
        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isGatewayTimeout());
    }
}
