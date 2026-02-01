/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package rtos.utils;

/**
 *
 * @author VictorB
 */
import rtos.interrupt.InterruptRequest;

/**
 * Comparador para ordenar InterruptRequest por prioridad.
 * Prioridad más alta (número mayor) va primero.
 * MICROMETEORITE=5 (máxima) -> GROUND_COMMAND=1 (mínima)
 */
public class InterruptComparator implements Comparator<InterruptRequest> {
    
    @Override
    public int compare(InterruptRequest irq1, InterruptRequest irq2) {
        // Ordenar por prioridad DESCENDENTE (mayor prioridad primero)
        // Retorna negativo si irq1 > irq2 (irq1 va primero)
        // Retorna positivo si irq1 < irq2 (irq2 va primero)
        // Retorna 0 si igual prioridad
        
        if (irq1.getPriority() > irq2.getPriority()) {
            return -1;  // irq1 tiene MAYOR prioridad, va primero
        } else if (irq1.getPriority() < irq2.getPriority()) {
            return 1;   // irq2 tiene MAYOR prioridad, va primero
        }
        return 0;       // Misma prioridad
    }
}