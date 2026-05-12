package com.snakeforged.domain;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Pure tick-driven game engine with no framework or threading dependencies.
 * The caller controls tick rate; one call to {@link #tick()} advances the game one step.
 *
 * <p>Grid coordinates: (0,0) is top-left, x increases right, y increases down.
 */
public class GameEngine {

    private final int width;
    private final int height;
    private final Random random;
    private final DifficultyConfig difficulty;

    private final Deque<Position> snake = new ArrayDeque<>();
    private Position food;
    private Direction currentDirection;
    private Direction pendingDirection;
    private GameStatus status;
    private int score;

    // True once setDirection() has been called this tick; prevents double-turn within one tick.
    private boolean directionChangedThisTick;

    /**
     * Creates a new game on a grid of the given dimensions.
     *
     * @param width       number of columns (must be >= 2)
     * @param height      number of rows (must be >= 2)
     * @param difficulty  tick-interval configuration; caller uses
     *                    {@link DifficultyConfig#getTickIntervalMs()} to schedule ticks
     * @param random      injectable Random so tests can use a fixed seed
     */
    public GameEngine(int width, int height, DifficultyConfig difficulty, Random random) {
        if (width < 2 || height < 2) {
            throw new IllegalArgumentException("Grid must be at least 2x2");
        }
        this.width = width;
        this.height = height;
        this.difficulty = difficulty;
        this.random = random;

        // Snake starts in the middle heading right, length 1
        Position start = new Position(width / 2, height / 2);
        snake.addFirst(start);
        currentDirection = Direction.RIGHT;
        pendingDirection = Direction.RIGHT;
        status = GameStatus.PLAYING;
        score = 0;
        directionChangedThisTick = false;

        food = spawnFood();
    }

    /**
     * Creates a new game with the given difficulty using an unseeded Random.
     * Use the 4-arg constructor in tests to inject a seeded Random.
     */
    public GameEngine(int width, int height, DifficultyConfig difficulty) {
        this(width, height, difficulty, new Random());
    }

    /** Creates a new game at MEDIUM difficulty with an injectable Random (test-friendly). */
    public GameEngine(int width, int height, Random random) {
        this(width, height, DifficultyConfig.MEDIUM, random);
    }

    /** Convenience constructor: MEDIUM difficulty, unseeded Random. */
    public GameEngine(int width, int height) {
        this(width, height, DifficultyConfig.MEDIUM, new Random());
    }

    /**
     * Advances the game by one step.
     * No-op when the game is already in GAME_OVER or WIN state.
     */
    public void tick() {
        if (status != GameStatus.PLAYING) {
            return;
        }

        currentDirection = pendingDirection;
        directionChangedThisTick = false;

        Position head = snake.peekFirst();
        Position next = head.move(currentDirection);

        // Wall collision
        if (next.x() < 0 || next.x() >= width || next.y() < 0 || next.y() >= height) {
            status = GameStatus.GAME_OVER;
            return;
        }

        // Self collision — check against all segments except the tail that will vacate
        List<Position> body = new ArrayList<>(snake);
        body.remove(body.size() - 1); // tail moves away
        if (body.contains(next)) {
            status = GameStatus.GAME_OVER;
            return;
        }

        // Move: add new head
        snake.addFirst(next);

        if (next.equals(food)) {
            // Ate food: grow (keep tail), increment score, spawn new food
            score++;
            if (snake.size() == width * height) {
                status = GameStatus.WIN;
                return;
            }
            food = spawnFood();
        } else {
            // Normal move: remove tail
            snake.removeLast();
        }
    }

    /**
     * Requests a direction change for the next tick.
     * Silently ignored if {@code direction} is opposite to the current direction
     * (reversal prevention) or if a direction change has already been accepted this tick.
     */
    public void setDirection(Direction direction) {
        if (directionChangedThisTick) {
            return;
        }
        if (!direction.isOpposite(currentDirection)) {
            pendingDirection = direction;
            directionChangedThisTick = true;
        }
    }

    public GameStatus getStatus()          { return status; }
    public int getScore()                  { return score; }
    public Direction getDirection()        { return currentDirection; }
    public Position getFoodPosition()      { return food; }
    public DifficultyConfig getDifficulty(){ return difficulty; }

    /** Returns an unmodifiable snapshot of the snake body, head first. */
    public List<Position> getSnake() {
        return List.copyOf(snake);
    }

    // ── private helpers ──────────────────────────────────────────────────────

    private Position spawnFood() {
        Set<Position> occupied = new HashSet<>(snake);
        int freeCount = width * height - occupied.size();
        if (freeCount == 0) {
            // Grid is full; no food needed (WIN is imminent)
            return null;
        }
        // Pick the nth free cell to avoid repeated retry loops on crowded grids
        int target = random.nextInt(freeCount);
        int seen = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Position p = new Position(x, y);
                if (!occupied.contains(p)) {
                    if (seen == target) {
                        return p;
                    }
                    seen++;
                }
            }
        }
        throw new IllegalStateException("Failed to place food — grid state inconsistent");
    }
}
