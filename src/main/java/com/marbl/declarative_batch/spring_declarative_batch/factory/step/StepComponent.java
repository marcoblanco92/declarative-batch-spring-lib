package com.marbl.declarative_batch.spring_declarative_batch.factory.step;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;

public interface StepComponent<I, O> {

    default ItemReader<I> reader() { return null; }
    default ItemProcessor<I, O> processor() { return null; }
    default ItemWriter<O> writer() { return null; }
}
