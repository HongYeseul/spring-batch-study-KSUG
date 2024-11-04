package com.example.batch_sample.jobs.task04.exercise;


import lombok.Getter;
import lombok.Setter;

/**
 * 지수 데이터를 읽어들여서 저장할 객체
 */
@Getter
@Setter
public class Indicator {

    /**
     * 이름
     */
    private String name;
    /**
     * 1번째 월 지수
     */
    private float month_1;
    /**
     * 2번째 월 지수
     */
    private float month_2;
    /**
     * 3번째 월 지수
     */
    private float month_3;
    /**
     * 4번째 월 지수
     */
    private float month_4;
    /**
     * 5번째 월 지수
     */
    private float month_5;
    /**
     * 6번째 월 지수
     */
    private float month_6;

}