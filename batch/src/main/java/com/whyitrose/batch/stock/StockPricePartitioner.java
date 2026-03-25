package com.whyitrose.batch.stock;

import com.whyitrose.domain.common.Status;
import com.whyitrose.domain.stock.Stock;
import com.whyitrose.domain.stock.StockRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockPricePartitioner implements Partitioner {

    private final StockRepository stockRepository;

    @Value("${stock.price.start-id:0}")
    private long startId;

    @Value("${stock.price.end-id:9223372036854775807}")
    private long endId;

    @Value("${stock.price.single-ticker:}")
    private String singleTicker;

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        long fromId = Math.max(0L, startId);
        long toId = Math.max(fromId, endId);
        String tickerFilter = singleTicker == null ? "" : singleTicker.trim().toUpperCase(Locale.ROOT);

        List<Long> ids = stockRepository.findByStatusOrderByIdAsc(Status.ACTIVE).stream()
                .filter(s -> s.getId() != null && s.getId() >= fromId && s.getId() <= toId)
                .filter(s -> tickerFilter.isBlank() || tickerFilter.equalsIgnoreCase(s.getTicker()))
                .map(Stock::getId)
                .collect(Collectors.toList());

        log.info("파티셔닝 시작: 대상 종목 {}개, gridSize={}", ids.size(), gridSize);

        Map<String, ExecutionContext> partitions = new HashMap<>();
        int actualGridSize = Math.min(gridSize, ids.size());
        if (actualGridSize == 0) {
            log.warn("처리할 종목이 없습니다.");
            return partitions;
        }

        int partitionSize = (int) Math.ceil((double) ids.size() / actualGridSize);
        for (int i = 0; i < actualGridSize; i++) {
            int from = i * partitionSize;
            int to = Math.min(from + partitionSize - 1, ids.size() - 1);

            ExecutionContext context = new ExecutionContext();
            context.putLong("startId", ids.get(from));
            context.putLong("endId", ids.get(to));

            String partitionName = String.format("partition_%d (id: %d~%d, %d종목)",
                    i, ids.get(from), ids.get(to), to - from + 1);
            partitions.put(partitionName, context);

            log.info("파티션 생성: {} → stock id {}~{} ({}종목)",
                    i, ids.get(from), ids.get(to), to - from + 1);
        }

        return partitions;
    }
}