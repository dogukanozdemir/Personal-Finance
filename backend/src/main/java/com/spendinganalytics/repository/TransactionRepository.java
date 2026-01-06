package com.spendinganalytics.repository;

import com.spendinganalytics.entity.Transaction;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepository
    extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

  Optional<Transaction> findByDedupHash(String dedupHash);

  @Query("select t from Transaction t where t.transactionDate between ?1 and ?2 and t.amount < 0")
  List<Transaction> findSpendingBetween(
      LocalDate transactionDateStart, LocalDate transactionDateEnd);

  @Query("SELECT t.dedupHash FROM Transaction t WHERE t.dedupHash IN :hashes")
  Set<String> findExistingDedupHashes(@Param("hashes") Set<String> hashes);
}
