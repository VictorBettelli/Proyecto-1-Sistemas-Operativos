/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package rtos.utils;

/**
 *
 * @author VictorB
 */
/**
 * Implementación simple de semáforo para sincronización de hilos.
 * Cumple con los requisitos del proyecto para exclusión mutua.
 */
public class Semaphore {
    private int permits;
    
    /**
     * Crea un semáforo con el número inicial de permisos.
     * @param initialPermits Número inicial de permisos
     */
    public Semaphore(int initialPermits) {
        if (initialPermits < 0) {
            throw new IllegalArgumentException("Permisos iniciales no pueden ser negativos");
        }
        this.permits = initialPermits;
    }
    
    /**
     * Adquiere un permiso, bloqueando si es necesario.
     * @throws InterruptedException si el hilo es interrumpido
     */
    public synchronized void acquire() throws InterruptedException {
        while (permits <= 0) {
            wait();  // Espera hasta que haya permisos
        }
        permits--;   // Toma un permiso
    }
    
    /**
     * Libera un permiso.
     */
    public synchronized void release() {
        permits++;
        notify();  // Notifica a un hilo esperando
    }
    
    /**
     * Libera múltiples permisos.
     * @param n número de permisos a liberar
     */
    public synchronized void release(int n) {
        if (n < 0) {
            throw new IllegalArgumentException("No se pueden liberar permisos negativos");
        }
        permits += n;
        for (int i = 0; i < n; i++) {
            notify();  // Notifica a n hilos
        }
    }
    
    /**
     * Intenta adquirir un permiso sin bloquear.
     * @return true si adquirió el permiso, false si no disponible
     */
    public synchronized boolean tryAcquire() {
        if (permits > 0) {
            permits--;
            return true;
        }
        return false;
    }
    
    /**
     * Obtiene el número de permisos disponibles.
     * @return permisos disponibles
     */
    public synchronized int availablePermits() {
        return permits;
    }
    
    @Override
    public String toString() {
        return "Semaphore[permits=" + permits + "]";
    }
}
