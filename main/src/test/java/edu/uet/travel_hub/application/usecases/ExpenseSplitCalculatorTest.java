package edu.uet.travel_hub.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class ExpenseSplitCalculatorTest {
    private final ExpenseSplitCalculator calculator = new ExpenseSplitCalculator();

    @Test
    void splitEqual_assignsRemainderToLastUsersAndPreservesTotal() {
        Map<Long, BigDecimal> result = calculator.splitEqual(new BigDecimal("100000"), List.of(1L, 2L, 3L));

        assertThat(result).containsEntry(1L, new BigDecimal("33333"));
        assertThat(result).containsEntry(2L, new BigDecimal("33333"));
        assertThat(result).containsEntry(3L, new BigDecimal("33334"));
        assertThat(result.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualByComparingTo(new BigDecimal("100000"));
    }
}
