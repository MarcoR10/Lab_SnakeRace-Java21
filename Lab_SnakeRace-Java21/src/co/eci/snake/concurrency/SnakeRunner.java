package co.eci.snake.concurrency;

import co.eci.snake.core.Board;
import co.eci.snake.core.Direction;
import co.eci.snake.core.Snake;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class SnakeRunner implements Runnable {

    private final Snake snake;
    private final Board board;
    private final int baseSleepMs = 80;
    private final int turboSleepMs = 40;
    //--------------------------------------------------------------------------//
    private final AtomicInteger turboTicks = new AtomicInteger(0);
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> scheduledTask;
    // --- Punto 3: pausa real del movimiento (no solo de la UI) ---
    private volatile boolean paused = false;
    //--------------------------------------------------------------------------//
    public SnakeRunner(Snake snake, Board board) {
        this.snake = snake;
        this.board = board;
    }
    //--------------------------------------------------------------------------//
    @Override
    public void run() {
        startMoving();
    }
    //--------------------------------------------------------------------------//
  /*
  garantizar que la verificación de estado y el inicio de la tarea sean seguros ante accesos concurrentes
  */
    //--------------------------------------------------------------------------//
    public synchronized void startMoving() {
        if (executor.isShutdown() || (scheduledTask != null && !scheduledTask.isDone())) {
            return;
        }
        scheduleStep(baseSleepMs);
    }
    //--------------------------------------------------------------------------//
  /*
  Reemplaza la espera activa de Thread.sleep() mediante la programación de tareas periódicas
  */
    //--------------------------------------------------------------------------//
    private synchronized void scheduleStep(long delayMs) {
        if (scheduledTask != null) {
            scheduledTask.cancel(false);
        }
        scheduledTask = executor.scheduleWithFixedDelay(this::step, 0, delayMs, TimeUnit.MILLISECONDS);
    }
    //--------------------------------------------------------------------------//
    /* Representa un solo paso/avance de la serpiente dentro del tablero. Reemplaza el contenido que antes residía dentro del bucle while */
    //--------------------------------------------------------------------------//
    private void step() {
        try {
            maybeTurn();
            var res = board.step(snake);

            if (res == Board.MoveResult.DIED) {
                // La serpiente murió: se detiene definitivamente, ya no se reprograman más pasos.
                stopForGood();
                return;
            }

            int currentTurbo = turboTicks.get();

            if (res == Board.MoveResult.HIT_OBSTACLE) {
                randomTurn();
            } else if (res == Board.MoveResult.ATE_TURBO) {
                if (currentTurbo <= 0) {
                    turboTicks.set(100);
                    scheduleStep(turboSleepMs);
                    return;
                }
                turboTicks.set(100);
            }

            if (currentTurbo > 0) {
                if (turboTicks.decrementAndGet() == 0) {
                    scheduleStep(baseSleepMs);
                }
            }
        } catch (Exception e) {
            Thread.currentThread().interrupt();
        }
    }
    //--------------------------------------------------------------------------//
    private void maybeTurn() {
        double p = (turboTicks.get() > 0) ? 0.05 : 0.10;
        if (ThreadLocalRandom.current().nextDouble() < p) {
            randomTurn();
        }
    }
    //--------------------------------------------------------------------------//
    private void randomTurn() {
        var dirs = Direction.values();
        snake.turn(dirs[ThreadLocalRandom.current().nextInt(dirs.length)]);
    }
    //--------------------------------------------------------------------------//
    // Punto 3: Control de ejecución seguro desde la UI (Pausar / Reanudar).
    //--------------------------------------------------------------------------//

    /** Cancela la tarea programada y cierra el executor: la serpiente ya no volverá a moverse. */
    private synchronized void stopForGood() {
        if (scheduledTask != null) scheduledTask.cancel(false);
        executor.shutdown();
    }

    /**
     * Pausa el movimiento de esta serpiente. No es instantáneo: si hay un paso en curso,
     * se espera (fuera del monitor, para no producir deadlock con scheduleStep) a que termine,
     * sometiendo una tarea vacía al mismo executor de un solo hilo. Así, cuando este método
     * retorna, se garantiza que la serpiente ya no se moverá más hasta resumeMovement(),
     * lo cual permite mostrar estadísticas consistentes (sin tearing) al pausar el juego.
     */
    public void pauseMovement() {
        if (!snake.isAlive()) return;
        synchronized (this) {
            paused = true;
            if (scheduledTask != null) scheduledTask.cancel(false);
        }
        try {
            executor.submit(() -> { }).get();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        } catch (Exception ignored) {
            // El executor pudo haberse cerrado justo porque la serpiente murió; no hay nada que esperar.
        }
    }

    /** Reanuda el movimiento, salvo que la serpiente ya haya muerto. */
    public synchronized void resumeMovement() {
        if (!snake.isAlive() || !paused) return;
        paused = false;
        scheduleStep(baseSleepMs);
    }
}