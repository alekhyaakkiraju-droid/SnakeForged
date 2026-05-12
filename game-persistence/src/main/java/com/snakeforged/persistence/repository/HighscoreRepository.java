package com.snakeforged.persistence.repository;

import com.snakeforged.persistence.entity.HighscoreEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HighscoreRepository extends JpaRepository<HighscoreEntry, Long> {

    /** Returns at most 10 entries for the given difficulty, ordered by score descending. */
    List<HighscoreEntry> findTop10ByDifficultyOrderByScoreDesc(String difficulty);
}
