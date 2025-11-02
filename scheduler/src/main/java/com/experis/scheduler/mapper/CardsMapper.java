package com.experis.scheduler.mapper;

import com.experis.scheduler.dto.SchedulerDto;
import com.experis.scheduler.entity.Scheduler;

public class SchedulerMapper {

    public static SchedulerDto mapToSchedulerDto(Scheduler scheduler, SchedulerDto schedulerDto) {
        schedulerDto.setSchedulerNumber(scheduler.getSchedulerNumber());
        schedulerDto.setSchedulerType(scheduler.getSchedulerType());
        schedulerDto.setMobileNumber(scheduler.getMobileNumber());
        schedulerDto.setTotalLimit(scheduler.getTotalLimit());
        schedulerDto.setAvailableAmount(scheduler.getAvailableAmount());
        schedulerDto.setAmountUsed(scheduler.getAmountUsed());
        return schedulerDto;
    }

    public static Scheduler mapToScheduler(SchedulerDto schedulerDto, Scheduler scheduler) {
        scheduler.setSchedulerNumber(schedulerDto.getSchedulerNumber());
        scheduler.setSchedulerType(schedulerDto.getSchedulerType());
        scheduler.setMobileNumber(schedulerDto.getMobileNumber());
        scheduler.setTotalLimit(schedulerDto.getTotalLimit());
        scheduler.setAvailableAmount(schedulerDto.getAvailableAmount());
        scheduler.setAmountUsed(schedulerDto.getAmountUsed());
        return scheduler;
    }

}
