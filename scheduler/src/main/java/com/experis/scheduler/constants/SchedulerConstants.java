package com.experis.scheduler.constants;

public final class SchedulerConstants {

    private SchedulerConstants() {
        // restrict instantiation
    }

    public static final String  CREDIT_SCHEDULER = "Credit Scheduler";
    public static final int  NEW_SCHEDULER_LIMIT = 1_00_000;
    public static final String  STATUS_201 = "201";
    public static final String  MESSAGE_201 = "Scheduler created successfully";
    public static final String  STATUS_200 = "200";
    public static final String  MESSAGE_200 = "Request processed successfully";
    public static final String  STATUS_417 = "417";
    public static final String  MESSAGE_417_UPDATE= "Update operation failed. Please try again or contact Dev team";
    public static final String  MESSAGE_417_DELETE= "Delete operation failed. Please try again or contact Dev team";
    // public static final String  STATUS_500 = "500";
    // public static final String  MESSAGE_500 = "An error occurred. Please try again or contact Dev team";

}
