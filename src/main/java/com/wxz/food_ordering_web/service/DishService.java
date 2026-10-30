package com.wxz.food_ordering_web.service;

import com.wxz.food_ordering_web.entity.Dish;
import com.wxz.food_ordering_web.repo.DishRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DishService {
    private final DishRepository repo;

    public DishService(DishRepository repo) {
        this.repo = repo;
    }

    public List<Dish> listAll() {
        return repo.findAll(Sort.by(Sort.Direction.ASC, "dishId"));
    }

    public Dish getById(Long id) {
        return repo.findById(id).orElseThrow(() -> new RuntimeException("Dish not found: " + id));
    }

    public void create(Dish dish) {
        dish.setDishId(null); // 确保走自增
        repo.save(dish);
    }

    public void update(Long id, Dish dish) {
        dish.setDishId(id);
        repo.save(dish);
    }

    public void delete(Long id) {
        repo.deleteById(id);
    }
}