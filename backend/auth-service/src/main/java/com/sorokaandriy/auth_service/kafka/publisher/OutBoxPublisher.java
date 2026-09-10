package com.sorokaandriy.auth_service.kafka.publisher;

import com.sorokaandriy.auth_service.entity.OutBox;
import com.sorokaandriy.auth_service.repository.OutBoxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class OutBoxPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final OutBoxRepository repository;
    private final PlatformTransactionManager transactionManager;


    @Scheduled(fixedDelay = 3000)
    public void publish() {
        List<OutBox> unpublished = repository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc();
        TransactionTemplate template = new TransactionTemplate(transactionManager);

        for (OutBox outBox : unpublished) {
            template.execute(status -> {
                try {
                    kafkaTemplate.send(
                            outBox.getTopic(),
                            outBox.getAggregateId(),
                            outBox.getPayload()
                    ).get();

                    outBox.setPublishedAt(Instant.now());
                    repository.save(outBox);

                    log.info("Published outbox message {} to topic {}", outBox.getId(), outBox.getTopic());
                    return null;

                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Publishing interrupted", ex);

                } catch (Exception ex) {
                    log.error("Failed to publish outbox message {} to topic {}",
                            outBox.getId(), outBox.getTopic(), ex);
                    status.setRollbackOnly();
                    return null;
                }
            });
        }
    }
}
