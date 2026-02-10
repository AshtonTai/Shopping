package org.shopping.site.admin.voucher;

import org.shopping.entity.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@Transactional
public class VoucherService {

    @Autowired
    private VoucherRepository voucherRepo;
    @Autowired private VoucherUsageRepository usageRepo;

    public Voucher validateVoucher(String code, Integer userId) {
        Voucher voucher = voucherRepo.findByCodeAndActiveTrue(code)
                .orElseThrow(() -> new IllegalArgumentException("Invalid voucher code"));

        LocalDateTime now = LocalDateTime.now();
        if (voucher.getValidFrom() != null && now.isBefore(voucher.getValidFrom())) {
            throw new IllegalArgumentException("Voucher not yet valid");
        }
        if (voucher.getValidUntil() != null && now.isAfter(voucher.getValidUntil())) {
            throw new IllegalArgumentException("Voucher has expired");
        }

        if (voucher.getUsageLimit() != null) {
            long totalUsage = usageRepo.countByVoucher_Id(voucher.getId());
            if (totalUsage >= voucher.getUsageLimit()) {
                throw new IllegalArgumentException("Voucher usage limit exceeded");
            }
        }

        if (voucher.isSingleUsePerCustomer()) {
            long userUsage = usageRepo.countByVoucher_IdAndUser_Id(voucher.getId(), userId);
            if (userUsage > 0) {
                throw new IllegalArgumentException("Voucher already used by this customer");
            }
        }

        return voucher;
    }

    public BigDecimal calculateDiscount(Voucher voucher, BigDecimal cartTotal) {
        switch (voucher.getType()) {
            case PERCENTAGE:
                return cartTotal.multiply(voucher.getValue().divide(BigDecimal.valueOf(100)));
            case FIXED_AMOUNT:
                return voucher.getValue().min(cartTotal); // Don't exceed cart total
            case FREE_SHIPPING:
                return BigDecimal.ZERO; // Free shipping handled separately
            default:
                return BigDecimal.ZERO;
        }
    }

    public void recordVoucherUsage(Voucher voucher, Integer userId, Integer orderId) {
        VoucherUsage usage = new VoucherUsage();

        Voucher voucherRef = new Voucher();
        voucherRef.setId(voucher.getId());

        User userRef = new User();
        userRef.setId(userId);

        Order orderRef = new Order();
        orderRef.setId(orderId);

        usage.setVoucher(voucherRef);
        usage.setUser(userRef);
        usage.setOrder(orderRef);
        usage.setUsedAt(LocalDateTime.now());

        usageRepo.save(usage);

        // Update usage count
        voucher.setUsageCount(voucher.getUsageCount() + 1);
        voucherRepo.save(voucher);
    }
}
