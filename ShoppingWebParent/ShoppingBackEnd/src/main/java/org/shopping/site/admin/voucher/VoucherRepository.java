package org.shopping.site.admin.voucher;

import org.shopping.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VoucherRepository extends JpaRepository<Voucher, Integer> {
    Optional<Voucher> findByCodeAndActiveTrue(String code);
    List<Voucher> findByActiveTrue();
    boolean existsByCode(String code);

}
