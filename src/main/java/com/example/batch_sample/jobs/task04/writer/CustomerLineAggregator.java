package com.example.batch_sample.jobs.task04.writer;

import com.example.batch_sample.jobs.task04.Customer;
import org.springframework.batch.item.file.transform.LineAggregator;

import static com.example.batch_sample.jobs.task04.FlatFileItemJobConfig.DELIMITER_FOR_WRITER;

public class CustomerLineAggregator implements LineAggregator<Customer> {
    @Override
    public String aggregate(Customer item) {
        return item.getName() + DELIMITER_FOR_WRITER + item.getAge();
    }
}
