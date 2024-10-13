package com.example.batch_sample.jobs.task01;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@AllArgsConstructor
@Configuration
public class BasicTaskJobConfiguration {

    // 트랜잭션 처리를 관리하는 역할을 한다. 배치 처리 중 실패가 발생하면 트랜잭션 롤백이 가능하다.
    private final PlatformTransactionManager transactionManager;

    @Bean
    public Tasklet greetingTasklet() {
        // 사용자 정의 tasklet 빈으로 등록
        return new GreetingTasklet();
    }

    /**
     * Step 객체를 생성하고 빈으로 등록하는 메서드
     * param: JobRepository, PlatformTransactionManager
     * 스프링 배치는 주로 Datasource 와 함께 작업되므로 transactionManager 클래스를 받는다.
     */
    @Bean
    public Step step(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        log.info("------------------ Init myStep -----------------");

        // StepBuilder 클래스를 이용하여 myStep 이라는 이름으로 Step을 생성한다. jobRepository는 Step의 실행 상태를 기록하고 관리한다.(필수)
        return new StepBuilder("myStep", jobRepository)
                // tasklet을 Step에 추가하고 greetingTasklet(실제로 실행할 작업 정의)을 통해 Step내 tasklet을 주입한다.
                .tasklet(greetingTasklet(), transactionManager)
                // Step을 생성하고 return 한다.
                .build();
    }

    /**
     * Job 객체를 생성하고 빈으로 등록하는 메서드
     * param: Step, JobRepository
     * Job은 Step의 집합이며 JobRepository 역시 필요하다.
     */
    @Bean
    public Job myJob(Step step, JobRepository jobRepository) {
        log.info("------------------ Init myJob -----------------");

        // JobBuilder 클래스를 이용하여 myJob 이라는 이름으로 Job을 생성한다. jobRepository는 job의 실행 상태를 기록하고 관리한다.(필수)
        return new JobBuilder("myJob", jobRepository)
                // incrementer() 메서드는 Job이 지속적으로 실행될때, Job의 유니크성을 구분할 수 있는 방법을 설정한다.
                // RunIdIncrementer는 배치 작업 실행 시마다 고유한 Job 실행 ID를 생성해주는 객체이다.
                .incrementer(new RunIdIncrementer())
                // Job이 실행될 때 첫 번째로 실행될 Step을 정의한다. 처음 시작하는 Step은 파라미터로 받은 step으로 등록했다.
                // 만약 여러 개의 Step이 필요하다면 추가적인 Step들을 next() 메서드를 통해 연결할 수 있다.
                .start(step)
                // Job을 생성하고 return 한다.
                .build();
    }
}
