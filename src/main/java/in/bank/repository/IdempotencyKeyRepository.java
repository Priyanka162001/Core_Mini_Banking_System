package in.bank.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import in.bank.entity.IdempotencyKey;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, String> {
}