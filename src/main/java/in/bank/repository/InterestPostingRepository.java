package in.bank.repository;

import in.bank.entity.InterestPostingRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InterestPostingRepository extends JpaRepository<InterestPostingRecord, Long> {

    // Used for idempotency check — has this account already been posted for this month+year?
    boolean existsBySavingsAccount_IdAndPostingMonthAndPostingYear(
            Long accountId, Integer month, Integer year);

    // Fetch full history for one account
    List<InterestPostingRecord> findBySavingsAccount_IdOrderByPostingYearDescPostingMonthDesc(
            Long accountId);

    // Fetch all postings for a given period (admin view)
    List<InterestPostingRecord> findByPostingMonthAndPostingYear(
            Integer month, Integer year);

    // Get a specific posting for an account+period
    Optional<InterestPostingRecord> findBySavingsAccount_IdAndPostingMonthAndPostingYear(
            Long accountId, Integer month, Integer year);
}