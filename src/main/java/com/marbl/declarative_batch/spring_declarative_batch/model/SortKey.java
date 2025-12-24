package com.marbl.declarative_batch.spring_declarative_batch.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.batch.item.database.Order;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SortKey {

    private String key;
    private Order order;
}
