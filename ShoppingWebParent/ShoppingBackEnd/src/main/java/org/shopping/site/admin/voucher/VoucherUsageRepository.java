package org.shopping.site.admin.voucher;

import org.shopping.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VoucherUsageRepository extends JpaRepository<VoucherUsage, Integer> {
    long countByVoucher_Id(Integer voucherId);
    long countByVoucher_IdAndUser_Id(Integer voucherId, Integer userId);
    boolean existsByOrder_Id(Integer orderId);
}
