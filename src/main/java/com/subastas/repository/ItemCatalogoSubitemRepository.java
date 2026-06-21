package com.subastas.repository;

import com.subastas.entity.ItemCatalogoSubitem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ItemCatalogoSubitemRepository extends JpaRepository<ItemCatalogoSubitem, Integer> {
    List<ItemCatalogoSubitem> findByItemCatalogo(Integer itemCatalogoId);
}
