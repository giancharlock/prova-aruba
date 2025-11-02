package com.experis.scheduler.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

@Schema(name = "Scheduler",
        description = "Schema to hold Scheduler information"
)
@Data
public class SchedulerDto {

    @NotEmpty(message = "Mobile Number can not be a null or empty")
    @Pattern(regexp="(^$|[0-9]{10})",message = "Mobile Number must be 10 digits")
    @Schema(
            description = "Mobile Number of Customer", example = "4354437687"
    )
    private String mobileNumber;

    @NotEmpty(message = "Scheduler Number can not be a null or empty")
    @Pattern(regexp="(^$|[0-9]{12})",message = "SchedulerNumber must be 12 digits")
    @Schema(
            description = "Scheduler Number of the customer", example = "100646930341"
    )
    private String schedulerNumber;

    @NotEmpty(message = "SchedulerType can not be a null or empty")
    @Schema(
            description = "Type of the scheduler", example = "Credit Scheduler"
    )
    private String schedulerType;

    @Positive(message = "Total scheduler limit should be greater than zero")
    @Schema(
            description = "Total amount limit available against a scheduler", example = "100000"
    )
    private int totalLimit;

    @PositiveOrZero(message = "Total amount used should be equal or greater than zero")
    @Schema(
            description = "Total amount used by a Customer", example = "1000"
    )
    private int amountUsed;

    @PositiveOrZero(message = "Total available amount should be equal or greater than zero")
    @Schema(
            description = "Total available amount against a scheduler", example = "90000"
    )
    private int availableAmount;

}
