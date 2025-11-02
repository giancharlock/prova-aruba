package com.experis.scheduler.repository;

import com.experis.scheduler.entity.Scheduler;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SchedulerRepository extends JpaRepository<Scheduler, Long> {

    Optional<Scheduler> findByMobileNumber(String mobileNumber);

    Optional<Scheduler> findBySchedulerNumber(String schedulerNumber);

}
