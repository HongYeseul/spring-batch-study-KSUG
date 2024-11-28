package com.example.batch_sample.jobs.task08;

import com.example.batch_sample.jobs.task06.Customer;
import org.springframework.batch.item.ItemProcessor;

/**
 * 나이에 20년을 더하는 ItemProcessor
 */
public class After20YearsItemProcessor implements ItemProcessor<Customer, Customer> {
    @Override
    public Customer process(Customer item) throws Exception {
        item.setAge(item.getAge() + 20);
        return item;
    }
}
