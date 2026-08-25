package com.sp.api.category.service;

import com.sp.api.category.dto.CategoryResponse;
import com.sp.api.category.dto.CreateCategoryRequest;
import com.sp.api.category.entity.Category;
import com.sp.api.category.repository.CategoryRepository;
import com.sp.api.common.exception.ConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public List<CategoryResponse> findAll() {
        return categoryRepository.findAllByOrderByNameAsc().stream()
                .map(CategoryResponse::from)
                .toList();
    }

    @Transactional
    public CategoryResponse create(CreateCategoryRequest request) {

        if (categoryRepository.existsByName(request.getName())) {
            throw new ConflictException("이미 존재하는 카테고리입니다.");
        }

        return CategoryResponse.from(categoryRepository.save(new Category(request.getName())));
    }
}
