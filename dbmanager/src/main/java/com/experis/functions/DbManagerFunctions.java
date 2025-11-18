package com.experis.functions;

import com.experis.commons.dto.InvoiceDto;
import com.experis.commons.dto.SdiNotificationDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Function;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DbManagerFunctions {

    private final com.experis.service.consumer.KafkaConsumerService kafkaConsumerService;

    @Bean
    public Function<InvoiceDto,InvoiceDto> incomingInvoice() {
        return invoiceDto -> {
            try {
                kafkaConsumerService.processIncomingInvoice(invoiceDto);
                return invoiceDto;
            } catch (Exception e) {
                String msg = "Error deserializing incoming invoice: " +invoiceDto.toString();
                log.error(msg, e);
                throw new RuntimeException(msg,e);
            }
        };
    }

    @Bean
    public Function<SdiNotificationDto,SdiNotificationDto> sdiNotification() {
        return notification -> {
            try {
                kafkaConsumerService.processSdiNotification(notification);
                return notification;
            } catch (Exception e) {
                String msg = "Error deserializing SDI notification: "+ notification.toString();
                log.error(msg, e);
                throw new RuntimeException(msg,e);
            }
        };
    }

    @Bean
    public Function<InvoiceDto,InvoiceDto> sentInvoice() {
        return invoiceDto -> {
            try {
                kafkaConsumerService.processSentInvoice(invoiceDto);
                return invoiceDto;
            } catch (Exception e) {
                String msg = "Error deserializing sent invoice: "+ invoiceDto.toString();
                log.error(msg, e);
                throw new RuntimeException(msg,e);
            }
        };
    }
}
