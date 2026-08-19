package edu.eci.arsw.primefinder;
import java.util.*;

import java.util.*;

public class Main {

    public static void main(String[] args) {

        Timer time = new Timer();
        Control control = Control.newControl();
        control.start();
        TimerTask tarea = new TimerTask() {
            @Override
            public void run() {
                control.pausar();
                int num = control.primos();
                System.out.println("\n--------------------------------------------------");
                System.out.println(" Números primos encontrados hasta ahora: " + num);
                System.out.println("--------------------------------------------------");
                System.out.println(" Presiona ENTER para reanudar los hilos ");
                System.out.println("--------------------------------------------------");
                try {
                    System.in.read();
                    while (System.in.available() > 0) {
                        System.in.read();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                control.desbloquear();
            }
        };
        time.schedule(tarea, Control.getTMILISECONDS(), Control.getTMILISECONDS());
    }
}