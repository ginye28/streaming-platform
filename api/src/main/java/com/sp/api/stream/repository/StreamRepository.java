package com.sp.api.stream.repository;

import com.sp.api.stream.entity.Stream;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StreamRepository extends JpaRepository<Stream, Long> {

}