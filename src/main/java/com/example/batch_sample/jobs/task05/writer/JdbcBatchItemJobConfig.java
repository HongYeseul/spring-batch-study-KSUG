package com.example.batch_sample.jobs.task05.writer;

import com.example.batch_sample.jobs.task05.Customer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Slf4j
@Configuration
public class JdbcBatchItemJobConfig {

    /**
     * CHUNK 크기를 지정한다.
     */
    public static final int CHUNK_SIZE = 100;
    public static final String ENCODING = "UTF-8";
    public static final String JDBC_BATCH_WRITER_CHUNK_JOB = "JDBC_BATCH_WRITER_CHUNK_JOB";

    @Autowired
    DataSource dataSource;

    @Bean
    public FlatFileItemReader<Customer> task05FlatFileItemReader() {
        // FlatFileItemReader를 생성하여 CSV 파일에서 데이터를 읽어오는 설정을 합니다.
        return new FlatFileItemReaderBuilder<Customer>()
                .name("Task05FlatFileItemReader") // Reader 의 이름을 설정
                .resource(new ClassPathResource("./customer.csv")) // 읽어올 CSV 파일 경로를 지정
                .encoding(ENCODING) // 파일 인코딩 설정
                .delimited().delimiter(",") // 파일의 구분자를 ','로 설정
                .names("name", "age", "gender") // CSV 파일의 컬럼 이름을 설정하여 매핑
                .targetType(Customer.class) // 매핑할 대상 객체 타입을 Customer로 설정
                .build();
    }

    @Bean
    public JdbcBatchItemWriter<Customer> task05FlatFileItemWriter() {
        // JdbcBatchItemWriter를 생성하여 데이터를 데이터베이스에 삽입하는 설정을 합니다.
        return new JdbcBatchItemWriterBuilder<Customer>()
                .dataSource(dataSource) // 사용할 데이터베이스의 DataSource 설정
                .sql("INSERT INTO customer (name, age, gender) VALUES (:name, :age, :gender)") // 삽입할 SQL 쿼리 정의
                .itemSqlParameterSourceProvider(new CustomItemSqlParameterSourceProvider()) // SQL 쿼리에 매핑할 파라미터 제공자 설정
                .build();
    }


    @Bean
    public Step tesk05FlatFileStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        log.info("------------------ Init flatFileStep -----------------");

        return new StepBuilder("flatFileStep", jobRepository)
                .<Customer, Customer>chunk(CHUNK_SIZE, transactionManager)
                .reader(task05FlatFileItemReader())
                .writer(task05FlatFileItemWriter())
                .build();
    }

    @Bean
    public Job task05FlatFileJob(Step tesk05FlatFileStep, JobRepository jobRepository) {
        log.info("------------------ Init flatFileJob -----------------");
        return new JobBuilder(JDBC_BATCH_WRITER_CHUNK_JOB, jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(tesk05FlatFileStep)
                .build();
    }
}
