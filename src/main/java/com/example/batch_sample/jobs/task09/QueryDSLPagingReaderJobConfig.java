package com.example.batch_sample.jobs.task10;


import com.example.batch_sample.jobs.task06.Customer;
import com.example.batch_sample.jobs.task06.CustomerItemProcessor;
import com.example.batch_sample.jobs.task06.QCustomer;
import jakarta.persistence.EntityManagerFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Slf4j
@Configuration
public class QueryDSLPagingReaderJobConfig {

    /**
     * CHUNK 크기를 지정한다.
     */
    public static final int CHUNK_SIZE = 2;
    public static final String ENCODING = "UTF-8";
    public static final String QUERYDSL_PAGING_CHUNK_JOB = "QUERYDSL_PAGING_CHUNK_JOB";

    @Autowired
    DataSource dataSource;

    @Autowired
    EntityManagerFactory entityManagerFactory;

//    @Bean
//    public QuerydslPagingItemReader<Customer> customerQuerydslPagingItemReader() throws Exception {
//
//        Function<JPAQueryFactory, JPAQuery<Customer>> query = jpaQueryFactory -> jpaQueryFactory.select(QCustomer.customer).from(QCustomer.customer);
//
//        return new QuerydslPagingItemReader<>("customerQuerydslPagingItemReader", entityManagerFactory, query, CHUNK_SIZE, false);
//    }

    /**
     * QuerydslPagingItemReaderBuilder 를 사용하여 Querydsl 기반의 페이징 가능한 ItemReader
     */
    @Bean
    public QuerydslPagingItemReader<Customer> customerQuerydslPagingItemReader() {
        return new QuerydslPagingItemReaderBuilder<Customer>()
                .name("customerQuerydslPagingItemReader")
                .entityManagerFactory(entityManagerFactory)
                // 페이징 처리 시 한 번에 처리할 데이터의 크기 설정 (2개씩 처리)
                .chunkSize(2)
                // Querydsl을 사용하여 동적 쿼리 정의
                .querySupplier(jpaQueryFactory ->
                        // Querydsl의 JPAQueryFactory를 통해 쿼리를 정의
                        jpaQueryFactory.select(QCustomer.customer)
                                .from(QCustomer.customer)
                                .where(QCustomer.customer.age.gt(20)))
                .build();
    }

    @Bean
    public FlatFileItemWriter<Customer> customerQuerydslFlatFileItemWriter() {

        return new FlatFileItemWriterBuilder<Customer>()
                .name("customerQuerydslFlatFileItemWriter")
                .resource(new FileSystemResource("./output/task09_customer_new_v2.csv"))
                .encoding(ENCODING)
                .delimited().delimiter("\t")
                .names("Name", "Age", "Gender")
                .build();
    }


    @Bean
    public Step customerQuerydslPagingStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) throws Exception {
        log.info("------------------ Init customerQuerydslPagingStep -----------------");

        return new StepBuilder("customerJpaPagingStep", jobRepository)
                .<Customer, Customer>chunk(CHUNK_SIZE, transactionManager)
                .reader(customerQuerydslPagingItemReader())
                .processor(new CustomerItemProcessor())
                .writer(customerQuerydslFlatFileItemWriter())
                .build();
    }

    @Bean
    public Job customerJpaPagingJob(Step customerJdbcPagingStep, JobRepository jobRepository) {
        log.info("------------------ Init customerJpaPagingJob -----------------");
        return new JobBuilder(QUERYDSL_PAGING_CHUNK_JOB, jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(customerJdbcPagingStep)
                .build();
    }
}
