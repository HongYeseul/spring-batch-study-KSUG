package com.example.batch_sample.jobs.task10;

import com.example.batch_sample.jobs.task06.Customer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
public class CustomService {

    public Map<String, String> processToOtherService(Customer item) {

        log.info("Call API to OtherService....");

        return Map.of("code", "200", "message", "OK");
    }
}
