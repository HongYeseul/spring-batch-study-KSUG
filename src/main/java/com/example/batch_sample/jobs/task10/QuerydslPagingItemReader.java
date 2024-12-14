package com.example.batch_sample.jobs.task10;

import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.batch.item.database.AbstractPagingItemReader;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

public class QuerydslPagingItemReader<T> extends AbstractPagingItemReader<T> {
    /**
     * AbstractPagingItemReader은 어댑터 패턴으로, 상속받는 쪽은 doReadPage만 구현하면 된다.
     */

    private EntityManager em;
    private final Function<JPAQueryFactory, JPAQuery<T>> querySupplier;

    private final Boolean alwaysReadFromZero;

    public QuerydslPagingItemReader(EntityManagerFactory entityManagerFactory, Function<JPAQueryFactory, JPAQuery<T>> querySupplier, int chunkSize) {
        this(ClassUtils.getShortName(QuerydslPagingItemReader.class), entityManagerFactory, querySupplier, chunkSize, false);
    }

    /**
     * 생성자
     * @param name ItemReader를 구분하기 위한 이름
     * @param entityManagerFactory JPA를 이용하기 위해 entityManagerFactory를 전달함
     * @param querySupplier JpaQeury를 생성하기 위한 Functional Interface
     * @param chunkSize 한 번에 페이징 처리할 페이지 크기
     * @param alwaysReadFromZero 항상 0부터 페이징을 읽을지 여부를 지정
     *                           만약 paging 처리된 데이터 자체를 수정하는 경우 배치처리 누락이 발생할 수 있으므로 이를 해결하기 위한 방안으로 사용됨
     */
    public QuerydslPagingItemReader(String name, EntityManagerFactory entityManagerFactory, Function<JPAQueryFactory, JPAQuery<T>> querySupplier, int chunkSize, Boolean alwaysReadFromZero) {
        super.setPageSize(chunkSize);
        setName(name);
        this.querySupplier = querySupplier;
        this.em = entityManagerFactory.createEntityManager();
        this.alwaysReadFromZero = alwaysReadFromZero;

    }

    /**
     * 기본적으로 AbstractPagingItemReader에 자체 구현되어 있지만 EntityManager 자원을 해제하기 위해 em.close()를 수행한다.
     */
    @Override
    protected void doClose() throws Exception {
        if (em != null)
            em.close();
        super.doClose();
    }

    /**
     * 실제로 구현해야 할 추상 메서드
     */
    @Override
    protected void doReadPage() {
        initQueryResult();

        JPAQueryFactory jpaQueryFactory = new JPAQueryFactory(em); // 함수형 인터페이스로 지정된 queryDSL에 적용할 QueryFactory
        long offset = 0;
        // alwaysReadFromZero가 false라면 offset과 limit을 계속 이동하면서 조회하도록 offset을 계산한다.
        if (!alwaysReadFromZero) {
            offset = (long) getPage() * getPageSize();
        }

        JPAQuery<T> query = querySupplier.apply(jpaQueryFactory).offset(offset).limit(getPageSize());
        // 우리가 제공한 querySupplier에 JPAQueryFactory를 적용하여 JPAQuery를 생성하도록 한다.
        // 페이징을 위해 offset, limit을 계산된 offset 과 pageSize(청크 크기)를 지정하여 페이징 처리를 하도록 한다.

        List<T> queryResult = query.fetch();
        // 결과를 fetch 후 fetch된 내역을 result에 담는다.


        for (T entity: queryResult) {
            em.detach(entity); // 변경이 실제 DB에 반영되지 않도록 영속성 객체에서 제외 시킨다.
            results.add(entity);
        }
    }

    private void initQueryResult() {
        if (CollectionUtils.isEmpty(results)) {
            results = new CopyOnWriteArrayList<>();
        } else {
            results.clear();
        }
    }
}
