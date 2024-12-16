package com.example.batch_sample.jobs.task10;

import com.example.batch_sample.jobs.task06.Customer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * CustomService 를 생성자 파라미터로 받고, write 메소드를 구현
 */
@Slf4j
@Component
public class CustomItemWriter implements ItemWriter<Customer> {

    private final CustomService customService;

    public CustomItemWriter(CustomService customService) {
        this.customService = customService;
    }

    @Override
    public void write(Chunk<? extends Customer> chunk) throws Exception {
        for (Customer customer: chunk) {
            log.info("Call Porcess in CustomItemWriter...");
            Map<String, String> outputMap = customService.processToOtherService(customer);
            log.info("Customer OutputMap: " + outputMap);
        }
    }
}
