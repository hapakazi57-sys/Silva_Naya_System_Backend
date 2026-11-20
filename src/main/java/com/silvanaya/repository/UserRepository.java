package com.silvanaya.repository;

import com.silvanaya.model.*;
import com.silvanaya.model.Enums.*;
import org.springframework.data.jpa.repository.*;
import java.math.BigDecimal;
import java.util.*;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailIgnoreCase(String email);
}
