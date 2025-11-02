package com.experis.scheduler.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.*;

@Entity
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class Scheduler extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long schedulerId;

    private String mobileNumber;

    private String schedulerNumber;

    private String schedulerType;

    private int totalLimit;

    private int amountUsed;

    private int availableAmount;

}
