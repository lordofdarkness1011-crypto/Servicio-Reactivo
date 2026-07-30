package com.proyecto.unidad2.repository;

import com.proyecto.unidad2.model.Item;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ItemRepository extends ReactiveCrudRepository<Item, Long> {
}