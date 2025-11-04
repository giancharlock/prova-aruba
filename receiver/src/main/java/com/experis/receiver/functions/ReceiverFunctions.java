package com.experis.receiver.functions;

import com.experis.dbmanager.dto.InvoiceDto;
import com.experis.dbmanager.dto.SdiNotificationDto;
import com.experis.receiver.service.consumer.KafkaConsumerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Function;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class ReceiverFunctions {

    private final KafkaConsumerService kafkaConsumerService;

    @Bean
    public Function<InvoiceDto,InvoiceDto> savedInvoice() {
        return invoiceDto -> {
            try {
                kafkaConsumerService.processSavedInvoice(invoiceDto);
                return invoiceDto;
            } catch (Exception e) {
                String msg = "Error deserializing incoming saved invoice: " +invoiceDto.toString();
                log.error(msg, e);
                throw new RuntimeException(msg,e);
            }
        };
    }

    @Bean
    public Function<SdiNotificationDto,SdiNotificationDto> updatedInvoice() {
        return sdiNotificationDto -> {
            try {
                kafkaConsumerService.processSavedInvoice(sdiNotificationDto);
                return sdiNotificationDto;
            } catch (Exception e) {
                String msg = "Error deserializing incoming saved invoice: " +sdiNotificationDto.toString();
                log.error(msg, e);
                throw new RuntimeException(msg,e);
            }
        };
    }
}
