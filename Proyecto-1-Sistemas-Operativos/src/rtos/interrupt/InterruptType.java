/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package rtos.interrupt;

/**
 *
 * @author VictorB
 */
public enum InterruptType {
    MICROMETEORITE,      // Impacto de micro-meteorito
    SOLAR_FLARE,         // Ráfaga solar
    GROUND_COMMAND,      // Comando desde Tierra
    IO_COMPLETION,       // E/S completada
    DEADLINE_MISSED,     // Proceso no cumplió deadline
    SYSTEM_ERROR;        // Error del sistema
    
    @Override
    public String toString() {
        switch (this) {
            case MICROMETEORITE: return "Micro-meteorito";
            case SOLAR_FLARE: return "Ráfaga solar";
            case GROUND_COMMAND: return "Comando Tierra";
            case IO_COMPLETION: return "E/S Completada";
            case DEADLINE_MISSED: return "Deadline Incumplido";
            case SYSTEM_ERROR: return "Error del Sistema";
            default: return name();
        }
    }
}