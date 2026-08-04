package com.sp.api.stream.repository;

import com.sp.api.stream.entity.Stream;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StreamRepository extends JpaRepository<Stream, Long> {

    List<Stream> findByTitleContainingIgnoreCase(String keyword);

    List<Stream> findTop10ByOrderByViewCountDesc();

    List<Stream> findTop10ByOrderByCreatedAtDesc();
}