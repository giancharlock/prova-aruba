package com.experis.dbmanager.repository;

import com.experis.dbmanager.audit.AuditAwareImpl;
import com.experis.dbmanager.entity.Customer;
import com.experis.dbmanager.enumerations.CustomerType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE) // Use the application's datasource (PostgreSQL)
@TestPropertySource(properties = { "spring.cloud.stream.enabled=false" })
@Sql(statements = {
    "INSERT INTO customer (username, password, email, customer_type, created_at, created_by) VALUES ('sdi_user', 'sdi_password', 'sdi@governo.it', 'SDI', CURRENT_DATE, 'system') ON CONFLICT (username) DO NOTHING;",
    "INSERT INTO customer (username, password, email, customer_type, created_at, created_by) VALUES ('test_user_1', 'aruba_pwd_1', 'test1@aruba.it', 'ARUBA', CURRENT_DATE, 'system') ON CONFLICT (username) DO NOTHING;",
    "INSERT INTO customer (username, password, email, customer_type, created_at, created_by) VALUES ('test_user_2', 'aruba_pwd_2', 'test2@aruba.it', 'ARUBA', CURRENT_DATE, 'system') ON CONFLICT (username) DO NOTHING;",
    "INSERT INTO customer (username, password, email, customer_type, created_at, created_by) VALUES ('test_user_3', 'aruba_pwd_3', 'test3@aruba.it', 'ARUBA', CURRENT_DATE, 'system') ON CONFLICT (username) DO NOTHING;"
})
@Import(AuditAwareImpl.class)
class CustomerRepositoryTest {

    @Autowired
    private CustomerRepository customerRepository;

    @Test
    void findByUsername_whenSdiUserExists_shouldReturnSdiUser() {
        Optional<Customer> foundCustomer = customerRepository.findByUsername("sdi_user");
        assertTrue(foundCustomer.isPresent(), "sdi_user should be found");
        assertEquals("sdi_user", foundCustomer.get().getUsername());
        assertEquals(CustomerType.SDI, foundCustomer.get().getCustomerType());
    }

    @Test
    void findByUsername_whenArubaUserExists_shouldReturnArubaUser() {
        Optional<Customer> foundCustomer = customerRepository.findByUsername("test_user_1");
        assertTrue(foundCustomer.isPresent(), "test_user_1 should be found");
        assertEquals("test_user_1", foundCustomer.get().getUsername());
        assertEquals(CustomerType.ARUBA, foundCustomer.get().getCustomerType());
    }

    @Test
    void findByUsername_whenUserDoesNotExist_shouldReturnEmpty() {
        Optional<Customer> foundCustomer = customerRepository.findByUsername("non_existent_user");
        assertFalse(foundCustomer.isPresent(), "A non-existent user should not be found");
    }

    @Test
    void findByCustomerTypeAndUsername_whenMatchExists_shouldReturnCustomer() {
        Optional<Customer> foundCustomer = customerRepository.findByCustomerTypeAndUsername(CustomerType.ARUBA, "test_user_2");
        assertTrue(foundCustomer.isPresent(), "ARUBA user 'test_user_2' should be found");
        assertEquals("test_user_2", foundCustomer.get().getUsername());
    }

    @Test
    void findByCustomerTypeAndUsername_whenTypeMismatch_shouldReturnEmpty() {
        Optional<Customer> foundCustomer = customerRepository.findByCustomerTypeAndUsername(CustomerType.ARUBA, "sdi_user");
        assertFalse(foundCustomer.isPresent(), "Should not find sdi_user with type ARUBA");
    }
}
