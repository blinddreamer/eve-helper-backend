package com.example.pandatribe.repositories.interfaces;

import com.example.pandatribe.models.market.ItemPrice;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ItemPriceRepository extends CrudRepository<ItemPrice, String> {
}
