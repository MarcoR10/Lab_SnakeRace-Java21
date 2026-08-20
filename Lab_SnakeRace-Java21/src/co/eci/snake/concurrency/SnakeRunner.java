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
}