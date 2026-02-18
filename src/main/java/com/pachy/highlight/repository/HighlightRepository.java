package com.pachy.highlight.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pachy.highlight.entity.Highlight;

import java.util.List;

public interface HighlightRepository extends JpaRepository<Highlight, Long> {
  List<Highlight> findAllByVideoId(String videoId);
}
