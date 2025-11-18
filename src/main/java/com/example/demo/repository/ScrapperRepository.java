package com.example.demo.repository;

import com.example.demo.entity.Scrapper;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ScrapperRepository extends JpaRepository<Scrapper, Long> {

    Optional<Scrapper> findByUrl(String url);

    boolean existsByUrl(String url);
}
