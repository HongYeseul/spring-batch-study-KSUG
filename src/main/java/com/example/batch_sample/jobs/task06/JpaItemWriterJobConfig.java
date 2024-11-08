package com.example.batch_sample.jobs.task06;

import jakarta.persistence.EntityManagerFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
public class JpaItemWriterJobConfig {

    public static final int CHUNK_SIZE = 100;
    public static final String ENCODING = "UTF-8";
    public static final String JPA_ITEM_WRITER_JOB = "JPA_ITEM_WRITER_JOB";

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @Bean
    public FlatFileItemReader<Customer> task06FlatFileItemReader() {

        return new FlatFileItemReaderBuilder<Customer>()
                .name("task06FlatFileItemReader")
                .resource(new ClassPathResource("./customer.csv"))
                .encoding(ENCODING)
                .delimited().delimiter(",")
                .names("name", "age", "gender")
                .targetType(Customer.class)
                .build();
    }

    /**
     * Customer 엔티티를 데이터베이스에 저장하기 위한 JpaItemWriter
     * @return Customer 엔티티의 영속화를 위한 JpaItemWriter 인스턴스를 반환
     * JpaItemWriter<Customer> 인스턴스를 반환하면 배치 프로세스가 flush를 통해 Customer 데이터를 데이터베이스에 저장하게 되는 것
     */
    @Bean
    public JpaItemWriter<Customer> jpaItemWriter() {
        return new JpaItemWriterBuilder<Customer>()
                .entityManagerFactory(entityManagerFactory)
                .usePersist(true) // 데이터베이스에 엔티티가 이미 존재하지 않는다고 가정하고 항상 새로운 데이터를 추가하는 방식
                .build();
    }


    @Bean
    public Step task06FlatFileStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        log.info("------------------ Init flatFileStep -----------------");

        return new StepBuilder("task06FlatFileStep", jobRepository)
                .<Customer, Customer>chunk(CHUNK_SIZE, transactionManager)
                .reader(task06FlatFileItemReader())
                .writer(jpaItemWriter())
                .build();
    }

    @Bean
    public Job task06FlatFileJob(Step task06FlatFileStep, JobRepository jobRepository) {
        log.info("------------------ Init flatFileJob -----------------");
        return new JobBuilder(JPA_ITEM_WRITER_JOB, jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(task06FlatFileStep)
                .build();
    }
}
