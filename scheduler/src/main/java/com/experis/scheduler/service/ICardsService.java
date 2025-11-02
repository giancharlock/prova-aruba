package com.experis.scheduler.service;

import com.experis.scheduler.dto.SchedulerDto;

public interface ISchedulerService {

    /**
     *
     * @param mobileNumber - Mobile Number of the Customer
     */
    void createScheduler(String mobileNumber);

    /**
     *
     * @param mobileNumber - Input mobile Number
     *  @return Scheduler Details based on a given mobileNumber
     */
    SchedulerDto fetchScheduler(String mobileNumber);

    /**
     *
     * @param schedulerDto - SchedulerDto Object
     * @return boolean indicating if the update of scheduler details is successful or not
     */
    boolean updateScheduler(SchedulerDto schedulerDto);

    /**
     *
     * @param mobileNumber - Input Mobile Number
     * @return boolean indicating if the delete of scheduler details is successful or not
     */
    boolean deleteScheduler(String mobileNumber);

}
