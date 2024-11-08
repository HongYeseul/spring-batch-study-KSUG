package com.example.batch_sample.jobs.task06;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;

/**
 * 작동 순서를 보기 위해 전달 받은 Customer 객체에 대한 로그만 출력한다.
 */
@Slf4j
public class CustomerItemProcessor implements ItemProcessor<Customer, Customer> {
    @Override
    public Customer process(Customer item) throws Exception {
        log.info("Item Processor ------------------- {}", item);
        return item;
    }
}
