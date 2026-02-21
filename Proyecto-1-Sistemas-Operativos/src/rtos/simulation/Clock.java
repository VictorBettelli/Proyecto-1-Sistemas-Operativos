/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package rtos.simulation;

/**
 *
 * @author VictorB
 */


/**
 * Reloj global del sistema según especificaciones del PDF.
 * "El número de ciclo de reloj global debe ser visible en todo momento"
 */
public class Clock {
    private int currentCycle;
    private long realStartTime; // Para calcular tiempo real transcurrido
    private boolean isRunning;
    
    public Clock() {
        this.currentCycle = 0;
        this.realStartTime = System.currentTimeMillis();
        this.isRunning = false;
    }
    
    /**
     * Avanza un ciclo de reloj.
     * Según PDF: "PC y MAR incrementarán una unidad por cada ciclo del reloj"
     */
    // Versión que devuelve el ciclo actual
    public int tick() {
        currentCycle++;
        return currentCycle;
    }
    
    /**
     * Avanza múltiples ciclos.
     */
    public void tick(int cycles) {
        if (cycles > 0) {
            currentCycle += cycles;
        }
    }
    
    /**
     * Obtiene el ciclo actual.
     */
    public int getCurrentCycle() {
        return currentCycle;
    }
    
    /**
     * Establece el ciclo actual.
     */
    public void setCurrentCycle(int cycle) {
        if (cycle >= 0) {
            this.currentCycle = cycle;
        }
    }
    
    /**
     * Reinicia el reloj a cero.
     */
    public void reset() {
        currentCycle = 0;
        realStartTime = System.currentTimeMillis();
    }
    
    /**
     * Calcula tiempo real transcurrido desde inicio.
     */
    public long getElapsedRealTime() {
        return System.currentTimeMillis() - realStartTime;
    }
    
    /**
     * Calcula velocidad de simulación (ciclos/segundo real).
     */
    public double getSimulationSpeed() {
        long elapsed = getElapsedRealTime();
        if (elapsed == 0) return 0.0;
        return (currentCycle * 1000.0) / elapsed;
    }
    
    /**
     * Verifica si ha pasado cierto número de ciclos.
     * Útil para procesos periódicos.
     */
    public boolean hasCyclesPassed(int cycles) {
        return currentCycle >= cycles;
    }
    
    /**
     * Calcula ciclos restantes hasta ciclo objetivo.
     */
    public int getCyclesUntil(int targetCycle) {
        return Math.max(0, targetCycle - currentCycle);
    }
    
    /**
     * Formato para mostrar en GUI.
     */
    public String getFormattedTime() {
        return String.format("Ciclo: %d", currentCycle);
    }
    
    /**
     * Información detallada del reloj.
     */
    public String getClockInfo() {
        return String.format(
            "Reloj[%d ciclos, %.1f ciclos/seg]",
            currentCycle,
            getSimulationSpeed()
        );
    }
    
    @Override
    public String toString() {
        return getFormattedTime();
    }
}