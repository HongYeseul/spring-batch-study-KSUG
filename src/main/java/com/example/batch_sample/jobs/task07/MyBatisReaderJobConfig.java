package com.example.batch_sample.jobs.task07;

import com.example.batch_sample.jobs.task06.Customer;
import com.example.batch_sample.jobs.task06.CustomerItemProcessor;
import com.example.batch_sample.jobs.task08.After20YearsItemProcessor;
import com.example.batch_sample.jobs.task08.LowerCaseItemProcessor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.batch.MyBatisBatchItemWriter;
import org.mybatis.spring.batch.MyBatisPagingItemReader;
import org.mybatis.spring.batch.builder.MyBatisBatchItemWriterBuilder;
import org.mybatis.spring.batch.builder.MyBatisPagingItemReaderBuilder;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.batch.item.support.CompositeItemProcessor;
import org.springframework.batch.item.support.builder.CompositeItemProcessorBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.List;

@Slf4j
@Configuration
public class MyBatisReaderJobConfig {
    /**
     * CHUNK 크기를 지정한다.
     */
    public static final int CHUNK_SIZE = 2;
    public static final String ENCODING = "UTF-8";
    public static final String MYBATIS_CHUNK_JOB = "MYBATIS_CHUNK_JOB";

    @Autowired
    DataSource dataSource;

    @Autowired
    SqlSessionFactory sqlSessionFactory;

    /**
     * DB 쿼리 결과를 읽을 수 있도록 ItemReader 를 반환한다.
     * @return MyBatisPagingItemReader
     */
    @Bean
    public MyBatisPagingItemReader<Customer> myBatisItemReader() throws Exception {

        return new MyBatisPagingItemReaderBuilder<Customer>()
                .sqlSessionFactory(sqlSessionFactory) // 세션 팩토리 지정
                .pageSize(CHUNK_SIZE) // 페이징 단위 지정
                .queryId("batch_sample.jobs.selectCustomers") // 네임스페이스 + SQL ID
                .build();
    }


    /**
     * FlatFileWriter
     */
    @Bean
    public FlatFileItemWriter<Customer> task07customerCursorFlatFileItemWriter() {
        return new FlatFileItemWriterBuilder<Customer>()
                .name("customerCursorFlatFileItemWriter")
                .resource(new FileSystemResource("./output/task08_add20Years_makeLowerCase.csv"))
                .encoding(ENCODING)
                .delimited().delimiter("\t")
                .names("Name", "Age", "Gender")
                .build();
    }

    /**
     * MyBatisWriter
     */
    @Bean
    public CompositeItemProcessor<Customer, Customer> compositeItemProcessor() {
        return new CompositeItemProcessorBuilder<Customer, Customer>()
                // ItemProcessor가 수행할 순서대로 배열을 만들어 전달
                .delegates(List.of(
                        new LowerCaseItemProcessor(),
                        new After20YearsItemProcessor()
                ))
                .build();
    }

    @Bean
    public Step task07customerJdbcCursorStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) throws Exception {
        log.info("------------------ Init customerJdbcCursorStep -----------------");

        return new StepBuilder("customerJdbcCursorStep", jobRepository)
                .<Customer, Customer>chunk(CHUNK_SIZE, transactionManager)
                .reader(myBatisItemReader())
                .processor(compositeItemProcessor())
                .writer(task07customerCursorFlatFileItemWriter())
                .build();
    }

    @Bean
    public Job task07customerJdbcCursorPagingJob(Step task07customerJdbcCursorStep, JobRepository jobRepository) {
        log.info("------------------ Init customerJdbcCursorPagingJob -----------------");
        return new JobBuilder(MYBATIS_CHUNK_JOB, jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(task07customerJdbcCursorStep)
                .build();
    }
}
