package com.experis.receiver.service.impl;

import com.experis.receiver.constants.ReceiverConstants;
import com.experis.receiver.dto.ReceiverDto;
import com.experis.receiver.dto.ReceiverMsgDto;
import com.experis.receiver.entity.Receiver;
import com.experis.receiver.entity.Customer;
import com.experis.receiver.exception.CustomerAlreadyExistsException;
import com.experis.receiver.exception.ResourceNotFoundException;
import com.experis.receiver.mapper.ReceiverMapper;
import com.experis.receiver.repository.ReceiverRepository;
import com.experis.receiver.service.IReceiverService;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Random;

@Service
@AllArgsConstructor
public class ReceiverServiceImpl  implements IReceiverService {

    private static final Logger log = LoggerFactory.getLogger(ReceiverServiceImpl.class);

    private final StreamBridge streamBridge;



}
