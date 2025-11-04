package com.experis.receiver.service.consumer;

import com.experis.dbmanager.dto.InvoiceDto;
import com.experis.dbmanager.dto.SdiNotificationDto;
import com.experis.receiver.service.IReceiverService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaConsumerService {
    private final IReceiverService receiverService;

    public void processSavedInvoice(InvoiceDto savedInvoice) {
        receiverService.handleSavedInvoice(savedInvoice);
    }

    public void processSavedInvoice(SdiNotificationDto sdiNotificationDto) {
        receiverService.handleUpdatedInvoice(sdiNotificationDto);
    }

}
