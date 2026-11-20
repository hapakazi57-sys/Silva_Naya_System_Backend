package com.silvanaya.repository;

import com.silvanaya.model.Expense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findByDateBetweenOrderByDateDescIdDesc(LocalDate from, LocalDate to);
}