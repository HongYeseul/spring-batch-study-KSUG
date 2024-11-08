package com.example.batch_sample.jobs.task06;

import jakarta.persistence.EntityManagerFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.Collections;

@Slf4j
@Configuration
public class JpaPagingReaderJobConfig {

    public static final int CHUNK_SIZE = 2;
    public static final String ENCODING = "UTF-8";
    public static final String JPA_PAGING_CHUNK_JOB = "JPA_PAGING_CHUNK_JOB";

    @Autowired
    DataSource dataSource;

    @Autowired
    EntityManagerFactory entityManagerFactory;


    @Bean
    public JpaPagingItemReader<Customer> task06CustomerJpaPagingItemReader() throws Exception {
        JpaPagingItemReader<Customer> jpaPagingItemReader = new JpaPagingItemReader<>();

        // JPQL 쿼리를 이용하여 20살 이상을 가져오도록 한다.
        jpaPagingItemReader.setQueryString(
                "SELECT c FROM Customer c WHERE c.age > :age order by id desc"
        );

        // 엔티티 매니저 지정
        jpaPagingItemReader.setEntityManagerFactory(entityManagerFactory);

        // 한 번에 읽어올 Page 크기 지정. 보통 청크 크기와 맞춰준다.
        jpaPagingItemReader.setPageSize(CHUNK_SIZE);

        // JPQL 쿼리에 전달할 파라미터 지정
        jpaPagingItemReader.setParameterValues(Collections.singletonMap("age", 20));

        return jpaPagingItemReader;
    }

    /**
     * Builder를 이용하는 방법
     * 일반 방식과 동일하다.
     */
    @Bean
    public JpaPagingItemReader<Customer> task06CustomerJpaPagingItemReaderBuilder() throws Exception {

        return new JpaPagingItemReaderBuilder<Customer>()
                .name("customerJpaPagingItemReader")
                .queryString("SELECT c FROM Customer c WHERE c.age > :age order by id desc")
                .pageSize(CHUNK_SIZE)
                .entityManagerFactory(entityManagerFactory)
                .parameterValues(Collections.singletonMap("age", 20))
                .build();
    }

    @Bean
    public FlatFileItemWriter<Customer> task06CustomerJpaFlatFileItemWriter() {
        log.info("------------------ task06CustomerJpaFlatFileItemWriter -----------------");

        return new FlatFileItemWriterBuilder<Customer>()
                .name("task06CustomerJpaFlatFileItemWriter")
                .resource(new FileSystemResource("./output/task_06_customer_with_jpa.csv"))
                .encoding(ENCODING)
                .delimited().delimiter("\t")
                .names("Name", "Age", "Gender")
                .build();
    }


    @Bean
    public Step customerJpaPagingStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) throws Exception {
        log.info("------------------ Init customerJpaPagingStep -----------------");

        return new StepBuilder("customerJpaPagingStep", jobRepository)
                .<Customer, Customer>chunk(CHUNK_SIZE, transactionManager)
                .reader(task06CustomerJpaPagingItemReaderBuilder())
                .processor(new CustomerItemProcessor())
                .writer(task06CustomerJpaFlatFileItemWriter())
                .build();
    }

    @Bean
    public Job customerJpaPagingJob(Step customerJpaPagingStep, JobRepository jobRepository) {
        log.info("------------------ Init customerJpaPagingJob -----------------");
        return new JobBuilder(JPA_PAGING_CHUNK_JOB, jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(customerJpaPagingStep)
                .build();
    }
}
