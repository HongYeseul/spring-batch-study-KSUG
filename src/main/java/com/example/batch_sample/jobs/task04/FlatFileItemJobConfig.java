package com.example.batch_sample.jobs.task04;

import com.example.batch_sample.jobs.task04.writer.AggregateCustomerProcessor;
import com.example.batch_sample.jobs.task04.writer.CustomerFooter;
import com.example.batch_sample.jobs.task04.writer.CustomerHeader;
import com.example.batch_sample.jobs.task04.writer.CustomerLineAggregator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Configuration
public class FlatFileItemJobConfig {

    /**
     * CHUNK 크기를 지정한다.
     */
    public static final int CHUNK_SIZE = 100;
    public static final String ENCODING = "UTF-8";
    public static final String FLAT_FILE_CHUNK_JOB = "FLAT_FILE_CHUNK_JOB";

    public static final String DELIMITER_FOR_WRITER = "\t";

    private final ConcurrentHashMap<String, Integer> aggregateInfos = new ConcurrentHashMap<>();

    private final ItemProcessor<Customer, Customer> itemProcessor = new AggregateCustomerProcessor(aggregateInfos);

    @Bean
    public FlatFileItemReader<Customer> flatFileItemReader() {

        return new FlatFileItemReaderBuilder<Customer>()
                .name("FlatFileItemReader") // FlatFileItemReader 의 이름 지정
                .resource(new ClassPathResource("./customer.csv")) // 읽을 대상 추가
                .encoding(ENCODING) // 저장할 파일의 인코딩 타입
                .delimited().delimiter(",") // 구분자 설정
                .names("name", "age", "gender") // 매핑 될 클래스의 필드 명
                .targetType(Customer.class) // 구분 된 데이터를 넣을 클래스 지정
                .build();
    }

    @Bean
    public FlatFileItemWriter<Customer> flatFileItemWriter() {
        return new FlatFileItemWriterBuilder<Customer>()
                .name("flatFileItemWriter")
                .resource(new FileSystemResource("./output/customer_new.csv")) // FlatFileItemWriter 의 이름 지정
                .encoding(ENCODING) // 저장할 파일의 인코딩 타입
                .delimited().delimiter(DELIMITER_FOR_WRITER) // 구분자 설정
                .names("name", "age", "gender") // 매핑 될 클래스의 필드 명
                .append(false) // true(기존 파일에 이어쓰기) false(덮어 쓰기)
                .lineAggregator(new CustomerLineAggregator()) // Line 구분자 지정
                .headerCallback(new CustomerHeader()) // 출력 파일 헤더 지정
                .footerCallback(new CustomerFooter(aggregateInfos)) // 출력 파일 푸터 지정
                .build();
    }


    @Bean
    public Step flatFileStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        log.info("------------------ Init flatFileStep -----------------");

        return new StepBuilder("flatFileStep", jobRepository)
                .<Customer, Customer>chunk(CHUNK_SIZE, transactionManager)
                .reader(flatFileItemReader())
                .processor(itemProcessor)
                .writer(flatFileItemWriter())
                .build();
    }

    @Bean
    public Job flatFileJob(Step flatFileStep, JobRepository jobRepository) {
        log.info("------------------ Init flatFileJob -----------------");
        return new JobBuilder(FLAT_FILE_CHUNK_JOB, jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(flatFileStep)
                .build();
    }
}
