package com.experis.scheduler.service.impl;

import com.experis.scheduler.constants.SchedulerConstants;
import com.experis.scheduler.dto.SchedulerDto;
import com.experis.scheduler.entity.Scheduler;
import com.experis.scheduler.exception.SchedulerAlreadyExistsException;
import com.experis.scheduler.exception.ResourceNotFoundException;
import com.experis.scheduler.mapper.SchedulerMapper;
import com.experis.scheduler.repository.SchedulerRepository;
import com.experis.scheduler.service.ISchedulerService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Random;

@Service
@AllArgsConstructor
public class SchedulerServiceImpl implements ISchedulerService {

    private SchedulerRepository schedulerRepository;

    /**
     * @param mobileNumber - Mobile Number of the Customer
     */
    @Override
    public void createScheduler(String mobileNumber) {
        Optional<Scheduler> optionalScheduler= schedulerRepository.findByMobileNumber(mobileNumber);
        if(optionalScheduler.isPresent()){
            throw new SchedulerAlreadyExistsException("Scheduler already registered with given mobileNumber "+mobileNumber);
        }
        schedulerRepository.save(createNewScheduler(mobileNumber));
    }

    /**
     * @param mobileNumber - Mobile Number of the Customer
     * @return the new scheduler details
     */
    private Scheduler createNewScheduler(String mobileNumber) {
        Scheduler newScheduler = new Scheduler();
        long randomSchedulerNumber = 100000000000L + new Random().nextInt(900000000);
        newScheduler.setSchedulerNumber(Long.toString(randomSchedulerNumber));
        newScheduler.setMobileNumber(mobileNumber);
        newScheduler.setSchedulerType(SchedulerConstants.CREDIT_SCHEDULER);
        newScheduler.setTotalLimit(SchedulerConstants.NEW_SCHEDULER_LIMIT);
        newScheduler.setAmountUsed(0);
        newScheduler.setAvailableAmount(SchedulerConstants.NEW_SCHEDULER_LIMIT);
        return newScheduler;
    }

    /**
     *
     * @param mobileNumber - Input mobile Number
     * @return Scheduler Details based on a given mobileNumber
     */
    @Override
    public SchedulerDto fetchScheduler(String mobileNumber) {
        Scheduler scheduler = schedulerRepository.findByMobileNumber(mobileNumber).orElseThrow(
                () -> new ResourceNotFoundException("Scheduler", "mobileNumber", mobileNumber)
        );
        return SchedulerMapper.mapToSchedulerDto(scheduler, new SchedulerDto());
    }

    /**
     *
     * @param schedulerDto - SchedulerDto Object
     * @return boolean indicating if the update of scheduler details is successful or not
     */
    @Override
    public boolean updateScheduler(SchedulerDto schedulerDto) {
        Scheduler scheduler = schedulerRepository.findBySchedulerNumber(schedulerDto.getSchedulerNumber()).orElseThrow(
                () -> new ResourceNotFoundException("Scheduler", "SchedulerNumber", schedulerDto.getSchedulerNumber()));
        SchedulerMapper.mapToScheduler(schedulerDto, scheduler);
        schedulerRepository.save(scheduler);
        return  true;
    }

    /**
     * @param mobileNumber - Input MobileNumber
     * @return boolean indicating if the delete of scheduler details is successful or not
     */
    @Override
    public boolean deleteScheduler(String mobileNumber) {
        Scheduler scheduler = schedulerRepository.findByMobileNumber(mobileNumber).orElseThrow(
                () -> new ResourceNotFoundException("Scheduler", "mobileNumber", mobileNumber)
        );
        schedulerRepository.deleteById(scheduler.getSchedulerId());
        return true;
    }


}
