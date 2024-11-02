package com.example.batch_sample.jobs.task05;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.PagingQueryProvider;
import org.springframework.batch.item.database.builder.JdbcPagingItemReaderBuilder;
import org.springframework.batch.item.database.support.SqlPagingQueryProviderFactoryBean;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
public class JdbcPagingReaderJobConfig {

    /**
     * CHUNK 크기를 지정한다.
     */
    public static final int CHUNK_SIZE = 2;
    public static final String ENCODING = "UTF-8";
    public static final String JDBC_PAGING_CHUNK_JOB = "JDBC_PAGING_CHUNK_JOB";

    @Autowired
    DataSource dataSource;

    @Bean
    public JdbcPagingItemReader<Customer> jdbcPagingItemReader() throws Exception {

        Map<String, Object> parameterValue = new HashMap<>();
        parameterValue.put("age", 20);

        return new JdbcPagingItemReaderBuilder<Customer>()
                .name("jdbcPagingItemReader")
                .fetchSize(CHUNK_SIZE)
                .dataSource(dataSource)
                .rowMapper(new BeanPropertyRowMapper<>(Customer.class))
                .queryProvider(queryProvider())
                .parameterValues(parameterValue)
                .build();
    }

    @Bean
    public PagingQueryProvider queryProvider() throws Exception {
        /**
         * SqlPagingQueryProviderFactoryBean
         * Spring Batch에서 페이징 쿼리를 쉽게 생성하기 위한 팩토리 빈입니다.
         * 이 클래스를 사용하면 데이터베이스 종류에 따라 최적화된 페이징 쿼리를 자동으로 생성할 수 있습니다.
         */
        SqlPagingQueryProviderFactoryBean queryProvider = new SqlPagingQueryProviderFactoryBean();
        queryProvider.setDataSource(dataSource);  // DB 설정하기
        queryProvider.setSelectClause("id, name, age, gender"); // 사용할 쿼리의 SELECT 절을 지정하는 메서드(읽어올 컬럼들 정의)
        queryProvider.setFromClause("from customer"); // 조회할 테이블
        queryProvider.setWhereClause("where age >= :age"); // 조건 절

        Map<String, Order> sortKeys = new HashMap<>(1); // 페이징 쿼리의 정렬 기준을 설정하는 메서드
        sortKeys.put("id", Order.DESCENDING);

        queryProvider.setSortKeys(sortKeys);

        return queryProvider.getObject();
    }

    @Bean
    public FlatFileItemWriter<Customer> customerFlatFileItemWriter() {
        return new FlatFileItemWriterBuilder<Customer>()
                .name("customerFlatFileItemWriter")
                .resource(new FileSystemResource("./output/task05-JdbcPagingReader.csv"))
                .encoding(ENCODING)
                .delimited().delimiter("\t")
                .names("name", "age", "gender")
                .build();
    }


    @Bean
    public Step customerJdbcPagingStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) throws Exception {
        log.info("------------------ Init customerJdbcPagingStep -----------------");

        return new StepBuilder("customerJdbcPagingStep", jobRepository)
                .<Customer, Customer>chunk(CHUNK_SIZE, transactionManager)
                .reader(jdbcPagingItemReader())
                .writer(customerFlatFileItemWriter())
                .build();
    }

    @Bean
    public Job customerJdbcPagingJob(Step customerJdbcPagingStep, JobRepository jobRepository) {
        log.info("------------------ Init customerJdbcPagingJob -----------------");
        return new JobBuilder(JDBC_PAGING_CHUNK_JOB, jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(customerJdbcPagingStep)
                .build();
    }
}
