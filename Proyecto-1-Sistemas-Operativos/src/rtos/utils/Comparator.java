/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package rtos.utils;
import rtos.model.Process;
/**
 *
 * @author VictorB
 */
public interface Comparator<T> {
    int compare(T o1, T o2);
    
    
    
    // ========== IMPLEMENTACIONES CONCRETAS ==========
    
    /**
     * Comparador para EDF (Earliest Deadline First)
     * Ordena por deadline restante ascendente (menor primero)
     */
    class DeadlineComparator implements Comparator<Process> {
        @Override
        public int compare(Process p1, Process p2) {
            // Menor deadline primero (más urgente)
            if (p1.getRemainingDeadline() < p2.getRemainingDeadline()) return -1;
            if (p1.getRemainingDeadline() > p2.getRemainingDeadline()) return 1;
            return 0;
        }
    }
    
    /**
     * Comparador para Prioridad Estática
     * Prioridad más alta (número menor) va primero
     */
    class PriorityComparator implements Comparator<Process> {
        @Override
        public int compare(Process p1, Process p2) {
            // Menor número = mayor prioridad (1 > 2 > 3...)
            if (p1.getPriority() < p2.getPriority()) return -1;
            if (p1.getPriority() > p2.getPriority()) return 1;
            return 0;
        }
    }
    
    /**
     * Comparador para SRT (Shortest Remaining Time)
     * Ordena por tiempo restante de ejecución
     */
    class RemainingTimeComparator implements Comparator<Process> {
        @Override
        public int compare(Process p1, Process p2) {
            // Calcular tiempo restante para cada proceso
            int remaining1 = p1.getTotalInstructions() - p1.getExecutedInstructions();
            int remaining2 = p2.getTotalInstructions() - p2.getExecutedInstructions();
            
            // Menor tiempo restante primero (SRT)
            if (remaining1 < remaining2) return -1;
            if (remaining1 > remaining2) return 1;
            return 0;
        }
    }
    
    /**
     * Comparador para FCFS (First Come First Served)
     * Ordena por tiempo de creación
     */
    class ArrivalTimeComparator implements Comparator<Process> {
        @Override
        public int compare(Process p1, Process p2) {
            // CORRECCIÓN: Cambiar "pl" por "p1"
            // Menor tiempo de creación primero
            if (p1.getCreationTime() < p2.getCreationTime()) return -1;
            if (p1.getCreationTime() > p2.getCreationTime()) return 1;
            return 0;
        }
    }
    
    /**
     * Comparador para Periodo (RMS - Rate Monotonic Scheduling)
     * Ordena por periodo (menor periodo = mayor prioridad)
     */
    class PeriodComparator implements Comparator<Process> {
        @Override
        public int compare(Process p1, Process p2) {
            // Solo para procesos periódicos
            // Menor periodo primero (más frecuente = mayor prioridad)
            if (p1.getPeriod() < p2.getPeriod()) return -1;
            if (p1.getPeriod() > p2.getPeriod()) return 1;
            return 0;
        }
    }
    
    /**
     * Comparador para Deadlines absolutos (no restantes)
     */
    class AbsoluteDeadlineComparator implements Comparator<Process> {
        @Override
        public int compare(Process p1, Process p2) {
            // Menor deadline absoluto primero
            if (p1.getDeadline() < p2.getDeadline()) return -1;
            if (p1.getDeadline() > p2.getDeadline()) return 1;
            return 0;
        }
    }
}
