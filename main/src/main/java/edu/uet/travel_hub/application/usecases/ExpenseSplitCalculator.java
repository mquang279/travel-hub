package edu.uet.travel_hub.application.usecases;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class ExpenseSplitCalculator {
    public Map<Long, BigDecimal> splitEqual(BigDecimal totalAmount, List<Long> userIds) {
        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Expense amount must be greater than zero");
        }
        if (userIds == null || userIds.isEmpty()) {
            throw new IllegalArgumentException("Split users must not be empty");
        }

        List<Long> distinctUserIds = userIds.stream().distinct().toList();
        BigInteger total = totalAmount.setScale(0, RoundingMode.UNNECESSARY).toBigIntegerExact();
        BigInteger[] quotientAndRemainder = total.divideAndRemainder(BigInteger.valueOf(distinctUserIds.size()));
        int remainder = quotientAndRemainder[1].intValueExact();

        Map<Long, BigDecimal> splits = new LinkedHashMap<>();
        for (int i = 0; i < distinctUserIds.size(); i++) {
            BigInteger amount = quotientAndRemainder[0];
            if (i >= distinctUserIds.size() - remainder) {
                amount = amount.add(BigInteger.ONE);
            }
            splits.put(distinctUserIds.get(i), new BigDecimal(amount));
        }
        return splits;
    }
}
