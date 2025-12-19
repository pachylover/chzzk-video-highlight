package com.pacny.highlight.repository;

import com.pacny.highlight.entity.Highlight;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface HighlightRepository extends JpaRepository<Highlight, UUID> {
}
