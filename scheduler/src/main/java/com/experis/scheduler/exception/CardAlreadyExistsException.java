package com.experis.scheduler.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class SchedulerAlreadyExistsException extends RuntimeException {

    public SchedulerAlreadyExistsException(String message){
        super(message);
    }

}
