package com.amc.bfsi.repository;

import com.amc.bfsi.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    List<Customer> findByCityIgnoreCase(String city);

    Optional<Customer> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("select c from Customer c where lower(c.name) like lower(concat('%', :q, '%')) "
         + "or lower(c.email) like lower(concat('%', :q, '%'))")
    List<Customer> search(@Param("q") String q);

    @Query("select c.city, count(c) from Customer c group by c.city")
    List<Object[]> countByCity();
}
