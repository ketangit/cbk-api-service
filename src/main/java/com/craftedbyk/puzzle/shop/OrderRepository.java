package com.craftedbyk.puzzle.shop;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<CustomerOrder, Long> {

  Optional<CustomerOrder> findByPublicRef(String publicRef);
}
