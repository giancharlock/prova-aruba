package com.experis.dbmanager.service.consumer;

import com.experis.dbmanager.dto.CustomerDto;
import com.experis.dbmanager.dto.InvoiceDto;
import com.experis.dbmanager.dto.SdiNotificationDto;
import com.experis.dbmanager.enumerations.InvoiceStatus;
import com.experis.dbmanager.service.IDbManagerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = "eureka.client.enabled=false")
@ActiveProfiles("test")
@Import(TestChannelBinderConfiguration.class)
class KafkaConsumerServiceIntegrationTest {

    @Autowired
    private InputDestination input;

    @Autowired
    private OutputDestination output;

    @MockBean
    private IDbManagerService dbManagerService;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String INCOMING_INVOICE_INPUT = "consumeIncomingInvoice-in-0";
    private static final String SDI_NOTIFICATION_INPUT = "consumeSdiNotification-in-0";
    private static final String SAVED_INVOICE_OUTPUT = "publishSavedInvoice-out-0";
    private static final String OUTGOING_INVOICE_OUTPUT = "publishOutgoingInvoice-out-0";

    @Test
    void testConsumeIncomingInvoice_whenCustomerExists() throws IOException {
        // Arrange
        CustomerDto customerDto = new CustomerDto();
        customerDto.setCustomerId(1);
        InvoiceDto incomingInvoice = new InvoiceDto();
        incomingInvoice.setCustomer(customerDto);
        incomingInvoice.setInvoice("<xml>test</xml>");

        // Simulate the service generating an ID for the new invoice
        InvoiceDto savedInvoiceWithGeneratedId = new InvoiceDto();
        savedInvoiceWithGeneratedId.setInvoiceNumber(999); // A generated ID
        savedInvoiceWithGeneratedId.setCustomer(customerDto);
        savedInvoiceWithGeneratedId.setInvoiceStatus(InvoiceStatus.INTERNAL_INVOICE_TOBE_SENT);

        when(dbManagerService.findCustomerById(1)).thenReturn(Optional.of(customerDto));
        when(dbManagerService.createInvoice(anyInt(), any(InvoiceDto.class))).thenReturn(savedInvoiceWithGeneratedId);

        Message<InvoiceDto> message = MessageBuilder.withPayload(incomingInvoice).build();

        // Act
        input.send(message, INCOMING_INVOICE_INPUT);

        // Assert
        Message<byte[]> savedOutput = output.receive(100, SAVED_INVOICE_OUTPUT);
        Message<byte[]> outgoingOutput = output.receive(100, OUTGOING_INVOICE_OUTPUT);

        assertThat(savedOutput).isNotNull();
        assertThat(outgoingOutput).isNotNull();

        InvoiceDto savedResult = objectMapper.readValue(savedOutput.getPayload(), InvoiceDto.class);
        InvoiceDto outgoingResult = objectMapper.readValue(outgoingOutput.getPayload(), InvoiceDto.class);

        // Verify the invoice has the generated ID and correct status
        assertThat(savedResult.getInvoiceNumber()).isEqualTo(999);
        assertThat(savedResult.getInvoiceStatus()).isEqualTo(InvoiceStatus.INTERNAL_INVOICE_TOBE_SENT);
        assertThat(outgoingResult.getInvoiceNumber()).isEqualTo(999);
        assertThat(outgoingResult.getInvoiceStatus()).isEqualTo(InvoiceStatus.INTERNAL_INVOICE_TOBE_SENT);
    }

    @Test
    void testConsumeIncomingInvoice_whenCustomerNotExists() throws IOException {
        // Arrange
        CustomerDto customerDto = new CustomerDto();
        customerDto.setCustomerId(2);
        InvoiceDto incomingInvoice = new InvoiceDto();
        incomingInvoice.setCustomer(customerDto);

        InvoiceDto savedInvalidInvoice = new InvoiceDto();
        savedInvalidInvoice.setInvoiceNumber(888); // A generated ID
        savedInvalidInvoice.setCustomer(customerDto);
        savedInvalidInvoice.setInvoiceStatus(InvoiceStatus.INTERNAL_INVOICE_INVALID);

        when(dbManagerService.findCustomerById(2)).thenReturn(Optional.empty());
        when(dbManagerService.createInvoice(anyInt(), any(InvoiceDto.class))).thenReturn(savedInvalidInvoice);

        Message<InvoiceDto> message = MessageBuilder.withPayload(incomingInvoice).build();

        // Act
        input.send(message, INCOMING_INVOICE_INPUT);

        // Assert
        Message<byte[]> savedOutput = output.receive(100, SAVED_INVOICE_OUTPUT);
        Message<byte[]> outgoingOutput = output.receive(100, OUTGOING_INVOICE_OUTPUT); // Should be null

        assertThat(savedOutput).isNotNull();
        assertThat(outgoingOutput).isNull();

        InvoiceDto savedResult = objectMapper.readValue(savedOutput.getPayload(), InvoiceDto.class);
        assertThat(savedResult.getInvoiceNumber()).isEqualTo(888);
        assertThat(savedResult.getInvoiceStatus()).isEqualTo(InvoiceStatus.INTERNAL_INVOICE_INVALID);
    }

    @Test
    void testConsumeSdiNotification_whenInvoiceExists() throws IOException {
        // Arrange
        int existingInvoiceNumber = 123;
        SdiNotificationDto notification = new SdiNotificationDto(1, existingInvoiceNumber, InvoiceStatus.INTERNAL_INVOICE_DELIVERED);
        
        InvoiceDto existingInvoice = new InvoiceDto();
        existingInvoice.setInvoiceNumber(existingInvoiceNumber);

        InvoiceDto updatedInvoice = new InvoiceDto();
        updatedInvoice.setInvoiceNumber(existingInvoiceNumber);
        updatedInvoice.setInvoiceStatus(InvoiceStatus.INTERNAL_INVOICE_DELIVERED);

        when(dbManagerService.findInvoiceByNumber(existingInvoiceNumber)).thenReturn(Optional.of(existingInvoice));
        when(dbManagerService.updateInvoice(anyInt(), any(InvoiceDto.class))).thenReturn(updatedInvoice);

        Message<SdiNotificationDto> message = MessageBuilder.withPayload(notification).build();

        // Act
        input.send(message, SDI_NOTIFICATION_INPUT);

        // Assert
        Message<byte[]> savedOutput = output.receive(100, SAVED_INVOICE_OUTPUT);
        assertThat(savedOutput).isNotNull();

        InvoiceDto savedResult = objectMapper.readValue(savedOutput.getPayload(), InvoiceDto.class);
        assertThat(savedResult.getInvoiceNumber()).isEqualTo(existingInvoiceNumber);
        assertThat(savedResult.getInvoiceStatus()).isEqualTo(InvoiceStatus.INTERNAL_INVOICE_DELIVERED);
    }

    @Test
    void testConsumeSdiNotification_whenInvoiceNotExists() throws IOException {
        // Arrange
        int nonExistentInvoiceNumber = 404;
        SdiNotificationDto notification = new SdiNotificationDto(1, nonExistentInvoiceNumber, InvoiceStatus.INTERNAL_INVOICE_DELIVERED);

        when(dbManagerService.findInvoiceByNumber(nonExistentInvoiceNumber)).thenReturn(Optional.empty());

        Message<SdiNotificationDto> message = MessageBuilder.withPayload(notification).build();

        // Act
        input.send(message, SDI_NOTIFICATION_INPUT);

        // Assert
        Message<byte[]> savedOutput = output.receive(100, SAVED_INVOICE_OUTPUT);
        assertThat(savedOutput).isNotNull();

        InvoiceDto savedResult = objectMapper.readValue(savedOutput.getPayload(), InvoiceDto.class);
        assertThat(savedResult.getInvoiceStatus()).isEqualTo(InvoiceStatus.INTERNAL_INVOICE_INVALID);
        assertThat(savedResult.getInvoiceNumber()).isEqualTo(nonExistentInvoiceNumber);
    }
}
