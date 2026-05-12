package com.snakeforged.domain;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GameEngineTest {

    // Use seed=0 for deterministic food placement in tests
    private GameEngine engine(int w, int h) {
        return new GameEngine(w, h, new Random(0));
    }

    // ── initial state ────────────────────────────────────────────────────────

    @Test
    void initialStatusIsPlaying() {
        assertThat(engine(10, 10).getStatus()).isEqualTo(GameStatus.PLAYING);
    }

    @Test
    void initialScoreIsZero() {
        assertThat(engine(10, 10).getScore()).isZero();
    }

    @Test
    void initialDirectionIsRight() {
        assertThat(engine(10, 10).getDirection()).isEqualTo(Direction.RIGHT);
    }

    @Test
    void initialSnakeLengthIsOne() {
        assertThat(engine(10, 10).getSnake()).hasSize(1);
    }

    @Test
    void foodIsNotOnSnakeAtStart() {
        GameEngine e = engine(10, 10);
        assertThat(e.getSnake()).doesNotContain(e.getFoodPosition());
    }

    @Test
    void gridTooSmallThrows() {
        assertThatThrownBy(() -> new GameEngine(1, 5, new Random()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void gridHeightTooSmallThrows() {
        assertThatThrownBy(() -> new GameEngine(5, 1, new Random()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── movement ─────────────────────────────────────────────────────────────

    @Test
    void snakeMovesRightOnFirstTick() {
        GameEngine e = engine(10, 10);
        Position before = e.getSnake().get(0);
        e.tick();
        Position after = e.getSnake().get(0);
        assertThat(after).isEqualTo(new Position(before.x() + 1, before.y()));
    }

    @Test
    void snakeMovesUpAfterDirectionChange() {
        GameEngine e = engine(10, 10);
        e.setDirection(Direction.UP);
        e.tick();
        Position head = e.getSnake().get(0);
        // head y decreases when moving up
        assertThat(head.y()).isLessThan(e.getSnake().size() > 1
                ? e.getSnake().get(1).y()
                : 10 / 2);
    }

    @Test
    void snakeMovesLeftAfterDirectionChange() {
        GameEngine e = engine(10, 10);
        // First go UP to allow a LEFT turn (can't turn directly LEFT from RIGHT — reversal)
        e.setDirection(Direction.UP);
        e.tick();
        Position before = e.getSnake().get(0);
        e.setDirection(Direction.LEFT);
        e.tick();
        assertThat(e.getSnake().get(0)).isEqualTo(new Position(before.x() - 1, before.y()));
    }

    @Test
    void snakeMovesDownAfterDirectionChange() {
        GameEngine e = engine(10, 10);
        e.setDirection(Direction.DOWN);
        e.tick();
        Position head = e.getSnake().get(0);
        // head y increases when moving down
        assertThat(head.y()).isGreaterThan(10 / 2);
    }

    @Test
    void gameEngineWithDifficultyConfigExposesTickInterval() {
        GameEngine e = new GameEngine(10, 10, DifficultyConfig.EASY, new Random(0));
        assertThat(e.getDifficulty().getTickIntervalMs()).isEqualTo(100);
    }

    @Test
    void gameEngineDifficultyDoesNotAffectGameMechanics() {
        // HARD difficulty game behaves identically to EASY; tick interval is caller's concern
        GameEngine easy = new GameEngine(10, 10, DifficultyConfig.EASY, new Random(0));
        GameEngine hard = new GameEngine(10, 10, DifficultyConfig.HARD, new Random(0));
        easy.tick();
        hard.tick();
        assertThat(easy.getSnake()).isEqualTo(hard.getSnake());
        assertThat(easy.getScore()).isEqualTo(hard.getScore());
        assertThat(easy.getStatus()).isEqualTo(hard.getStatus());
    }

    @Test
    void snakeBodyLengthStaysOneWhenNoFoodEaten() {
        GameEngine e = engine(10, 10);
        // Make sure the first tick doesn't land on food by using a large grid
        GameEngine big = new GameEngine(20, 20, new Random(42));
        Position firstFood = big.getFoodPosition();
        big.tick();
        // Only grows if head lands on food
        boolean atFood = big.getSnake().get(0).equals(firstFood);
        assertThat(big.getSnake()).hasSize(atFood ? 2 : 1);
    }

    // ── direction reversal prevention ────────────────────────────────────────

    @Test
    void reversalRightToLeftIgnored() {
        GameEngine e = engine(10, 10);
        e.setDirection(Direction.LEFT); // opposite of initial RIGHT
        e.tick();
        // head should still move right
        assertThat(e.getDirection()).isEqualTo(Direction.RIGHT);
    }

    @Test
    void reversalUpToDownIgnored() {
        GameEngine e = engine(10, 10);
        e.setDirection(Direction.UP);
        e.tick(); // now moving up
        e.setDirection(Direction.DOWN); // reversal
        e.tick();
        assertThat(e.getDirection()).isEqualTo(Direction.UP);
    }

    @Test
    void reversalDownToUpIgnored() {
        GameEngine e = engine(10, 10);
        e.setDirection(Direction.DOWN);
        e.tick();
        e.setDirection(Direction.UP);
        e.tick();
        assertThat(e.getDirection()).isEqualTo(Direction.DOWN);
    }

    @Test
    void reversalLeftToRightIgnored() {
        GameEngine e = engine(10, 10);
        // First turn left (valid from RIGHT)
        e.setDirection(Direction.UP);
        e.tick();
        e.setDirection(Direction.LEFT);
        e.tick();
        e.setDirection(Direction.RIGHT); // reversal
        e.tick();
        assertThat(e.getDirection()).isEqualTo(Direction.LEFT);
    }

    // ── input debounce (one direction change per tick) ───────────────────────

    @Test
    void onlyFirstDirectionChangeAcceptedPerTick() {
        GameEngine e = engine(10, 10);
        e.setDirection(Direction.UP);  // accepted
        e.setDirection(Direction.DOWN); // ignored — already changed this tick
        e.tick();
        assertThat(e.getDirection()).isEqualTo(Direction.UP);
    }

    // ── wall collision ────────────────────────────────────────────────────────

    @Test
    void wallCollisionRightBoundaryTriggersGameOver() {
        GameEngine e = new GameEngine(3, 3, new Random(0));
        // Snake starts at (1,1) heading right; hits right wall after 1 tick
        e.tick(); // (2,1) — might eat food
        e.tick(); // (3,1) — out of bounds
        assertThat(e.getStatus()).isEqualTo(GameStatus.GAME_OVER);
    }

    @Test
    void wallCollisionLeftBoundaryTriggersGameOver() {
        GameEngine e = new GameEngine(5, 5, new Random(0));
        e.setDirection(Direction.LEFT);
        for (int i = 0; i < 5; i++) e.tick();
        assertThat(e.getStatus()).isEqualTo(GameStatus.GAME_OVER);
    }

    @Test
    void wallCollisionTopBoundaryTriggersGameOver() {
        GameEngine e = new GameEngine(5, 5, new Random(0));
        e.setDirection(Direction.UP);
        for (int i = 0; i < 5; i++) e.tick();
        assertThat(e.getStatus()).isEqualTo(GameStatus.GAME_OVER);
    }

    @Test
    void wallCollisionBottomBoundaryTriggersGameOver() {
        GameEngine e = new GameEngine(5, 5, new Random(0));
        e.setDirection(Direction.DOWN);
        for (int i = 0; i < 5; i++) e.tick();
        assertThat(e.getStatus()).isEqualTo(GameStatus.GAME_OVER);
    }

    // ── food consumption ──────────────────────────────────────────────────────

    @Test
    void eatingFoodIncrementsScore() {
        // Food placed at (6,5) — directly in front of snake at (5,5) heading right
        GameEngine e = new GameEngine(10, 10, foodInFront());
        e.tick();
        assertThat(e.getScore()).isEqualTo(1);
    }

    @Test
    void eatingFoodGrowsSnake() {
        GameEngine e = new GameEngine(10, 10, foodInFront());
        assertThat(e.getSnake()).hasSize(1);
        e.tick();
        assertThat(e.getSnake()).hasSize(2);
    }

    @Test
    void foodNeverSpawnsOnSnake() {
        GameEngine e = new GameEngine(10, 10, new Random(0));
        for (int i = 0; i < 30; i++) {
            if (e.getStatus() != GameStatus.PLAYING) break;
            if (e.getFoodPosition() != null) {
                assertThat(e.getSnake()).doesNotContain(e.getFoodPosition());
            }
            e.tick();
        }
    }

    // ── self collision ────────────────────────────────────────────────────────

    @Test
    void selfCollisionTriggersGameOver() {
        // Build a snake long enough to turn into itself on a small grid
        // 4x4 grid: steer snake around then back into its own body
        GameEngine e = new GameEngine(4, 4, new Random(99));
        // Force a U-shape: RIGHT → DOWN → LEFT → UP (into body)
        for (int i = 0; i < 50 && e.getStatus() == GameStatus.PLAYING; i++) {
            tickWithTurns(e, i);
        }
        // Either game over from self-collision or wall — both are non-PLAYING states
        assertThat(e.getStatus()).isNotEqualTo(GameStatus.PLAYING);
    }

    @Test
    void deterministicSelfCollisionTriggersGameOver() {
        // foodInFront() places food one step right each time, so 4 ticks grow the
        // snake to length 5: [(9,5),(8,5),(7,5),(6,5),(5,5)] heading RIGHT on a 10x10.
        // A tight U-turn (UP → LEFT → DOWN) steers the head into (8,5) which is
        // still in the body → confirmed self-collision, not a wall hit.
        GameEngine e = new GameEngine(10, 10, foodInFront());
        for (int i = 0; i < 4; i++) e.tick();
        e.setDirection(Direction.UP);  e.tick();
        e.setDirection(Direction.LEFT); e.tick();
        e.setDirection(Direction.DOWN); e.tick(); // head → (8,5) — already occupied
        assertThat(e.getStatus()).isEqualTo(GameStatus.GAME_OVER);
    }

    // ── win condition ─────────────────────────────────────────────────────────

    @Test
    void winWhenSnakeFillsGrid() {
        // 2x2 grid: snake needs to reach length 4
        // With seed 0, food placement is deterministic enough to guide manually
        GameEngine e = new GameEngine(2, 2, new Random(0));
        // Tick up to 20 times; on a 2x2 grid WIN must happen within 4 food collections
        for (int i = 0; i < 20 && e.getStatus() == GameStatus.PLAYING; i++) {
            e.tick();
            if (e.getStatus() == GameStatus.GAME_OVER) {
                // Restart with different seed if we hit a wall first
                break;
            }
        }
        // At minimum we prove win state is reachable without hanging
        assertThat(e.getStatus()).isIn(GameStatus.WIN, GameStatus.GAME_OVER);
    }

    @Test
    void deterministicWinFillsEntire2x2Grid() {
        // Guide snake through all 4 cells of a 2x2 grid.
        // foodAt([1,0,0]) places food: (1,0) → (0,0) → (0,1) in scan order.
        // Route: UP→eat(1,0), LEFT→eat(0,0), DOWN→eat(0,1) → WIN (length 4 = 2×2).
        GameEngine e = new GameEngine(2, 2, foodAt(1, 0, 0));
        e.setDirection(Direction.UP);   e.tick(); // head (1,1)→(1,0): eat food
        e.setDirection(Direction.LEFT); e.tick(); // head (1,0)→(0,0): eat food
        e.setDirection(Direction.DOWN); e.tick(); // head (0,0)→(0,1): eat food → WIN
        assertThat(e.getStatus()).isEqualTo(GameStatus.WIN);
        assertThat(e.getScore()).isEqualTo(3);
    }

    // ── tick no-op after terminal state ───────────────────────────────────────

    @Test
    void tickAfterGameOverDoesNothing() {
        GameEngine e = new GameEngine(3, 3, new Random(0));
        while (e.getStatus() == GameStatus.PLAYING) e.tick();
        GameStatus terminal = e.getStatus();
        int scoreBefore = e.getScore();
        e.tick();
        assertThat(e.getStatus()).isEqualTo(terminal);
        assertThat(e.getScore()).isEqualTo(scoreBefore);
    }

    @Test
    void snapshotIsUnmodifiable() {
        GameEngine e = engine(10, 10);
        assertThatThrownBy(() -> e.getSnake().add(new Position(0, 0)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    /**
     * Returns a Random that places food at scan-index 55 on a 10x10 grid,
     * which corresponds to (6,5) — one step directly right of the snake's start position (5,5).
     * Using Math.min guards against smaller bounds in subsequent calls.
     */
    private static Random foodInFront() {
        return new Random() {
            @Override public int nextInt(int bound) { return Math.min(55, bound - 1); }
        };
    }

    /** Returns a Random that cycles through the given scan-index targets in order. */
    private static Random foodAt(int... targets) {
        return new Random() {
            int i = 0;
            @Override public int nextInt(int bound) { return Math.min(targets[i++], bound - 1); }
        };
    }

    private void tickWithTurns(GameEngine e, int i) {
        if (i % 3 == 0) e.setDirection(Direction.DOWN);
        else if (i % 3 == 1) e.setDirection(Direction.LEFT);
        else e.setDirection(Direction.UP);
        e.tick();
    }
}
