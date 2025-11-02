package com.experis.dbmanager.repository;

import com.experis.dbmanager.entity.Customer;
import com.experis.dbmanager.enumerations.CustomerType;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Integer> {

    /**
     * Finds a customer by their username.
     * @param username The username to search for.
     * @return An Optional containing the customer if found.
     */
    Optional<Customer> findByUsername(String username);

    /**
     * Finds a customer by their type and username.
     * @param customerType The type of the customer (SDI or ARUBA).
     * @param username The username to search for.
     * @return An Optional containing the customer if found.
     */
    Optional<Customer> findByCustomerTypeAndUsername(CustomerType customerType, String username);

    /**
     * Finds a customer by their type and customer ID.
     * @param customerType The type of the customer.
     * @param customerId The ID of the customer.
     * @return An Optional containing the customer if found.
     */
    Optional<Customer> findByCustomerTypeAndCustomerId(CustomerType customerType, int customerId);

    @Override
    @Transactional
    void deleteById(Integer integer);
}
