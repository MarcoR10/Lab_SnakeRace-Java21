package co.eci.snake.core;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicInteger;

public final class Snake {

    private final Deque<Position> body = new ArrayDeque<>();
    private volatile Direction direction;
    private int maxLength = 5;

    // --- Punto 3: estado de vida para poder mostrar estadísticas al pausar ---
    private static final AtomicInteger DEATH_SEQUENCE = new AtomicInteger(0);
    private volatile boolean alive = true;
    private volatile int deathOrder = -1; // -1 mientras siga viva

    private Snake(Position start, Direction dir) {
        body.addFirst(start);
        this.direction = dir;
    }

    public static Snake of(int x, int y, Direction dir) {
        return new Snake(new Position(x, y), dir);
    }

    public Direction direction() { return direction; }

    public void turn(Direction dir) {
        if ((direction == Direction.UP && dir == Direction.DOWN) ||
                (direction == Direction.DOWN && dir == Direction.UP) ||
                (direction == Direction.LEFT && dir == Direction.RIGHT) ||
                (direction == Direction.RIGHT && dir == Direction.LEFT)) {
            return;
        }
        this.direction = dir;
    }

    public Position head() { return body.peekFirst(); }

    public Deque<Position> snapshot() { return new ArrayDeque<>(body); }

    public void advance(Position newHead, boolean grow) {
        body.addFirst(newHead);
        if (grow) maxLength++;
        while (body.size() > maxLength) body.removeLast();
    }

    public int length() { return body.size(); }

    public boolean isAlive() { return alive; }

    /** Marca la serpiente como muerta. Idempotente: solo la primera llamada asigna el orden de muerte. */
    public void kill() {
        if (alive) {
            alive = false;
            deathOrder = DEATH_SEQUENCE.incrementAndGet();
        }
    }

    /** Orden en el que murió (1 = la primera en morir). -1 si sigue viva. */
    public int deathOrder() { return deathOrder; }

}
