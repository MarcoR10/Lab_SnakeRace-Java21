/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.eci.arsw.primefinder;
import java.io.*;
import java.util.*;

public class Control extends Thread {

    private final static int NTHREADS = 3;
    private final static int MAXVALUE = 30000000;
    private final static int TMILISECONDS = 5000;
    private final int NDATA = MAXVALUE / NTHREADS;
    private PrimeFinderThread pft[];
    private int primos;
    private final Object bloqueo = new Object();
    private boolean candado = false;

    private Control() {
        super();
        this.pft = new PrimeFinderThread[NTHREADS];
        int i;
        for (i = 0; i < NTHREADS - 1; i++) {
            PrimeFinderThread elem = new PrimeFinderThread(i * NDATA, (i + 1) * NDATA, bloqueo, this);
            pft[i] = elem;
        }
        pft[i] = new PrimeFinderThread(i * NDATA, MAXVALUE + 1, bloqueo, this);
    }

    public static Control newControl() {
        return new Control();
    }

    @Override
    public void run() {
        for (int i = 0; i < NTHREADS; i++) {
            pft[i].start();
        }
    }

    public int primos() {
        for (int i = 0; i < NTHREADS; i++) {
            List<Integer> primes = pft[i].getPrimes();
            primos += primes.size();
        }
        return primos;
    }

    public void pausar() {
        synchronized (bloqueo) {
            candado = true;
        }
    }

    public void desbloquear() {
        synchronized (bloqueo) {
            candado = false;
            bloqueo.notifyAll();
        }
    }

    public boolean isPausado() {
        synchronized (bloqueo) {
            return candado;
        }
    }

    public static int getTMILISECONDS() {
        return TMILISECONDS;
    }
}
