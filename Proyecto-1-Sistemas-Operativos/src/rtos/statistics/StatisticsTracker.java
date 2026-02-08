/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package rtos.statistics;
import rtos.model.Process;
import rtos.structures.LinkedList;

/**
 *
 * @author VictorB
 */

/**
 * Rastreador de estadísticas del simulador RTOS.
 * Solo métricas, sin lógica de simulación.
 */
public class StatisticsTracker {
    // ========== MÉTRICAS ACUMULADAS ==========
    private int totalProcessesCreated;
    private int totalProcessesCompleted;
    private int successfulMissions;
    private int totalDeadlinesMissed;
    private int totalInstructionsExecuted;
    private int cpuBusyCycles;
    private int cpuIdleCycles;
    private int totalWaitingTime;
    
    // ========== TIEMPO DE SIMULACIÓN ==========
    private int simulationStartCycle;
    private int currentCycle;
    
    // ========== HISTÓRICO PARA GRÁFICAS ==========
    private LinkedList<Integer> cpuUsageHistory;      // % uso CPU cada 10 ciclos
    private LinkedList<Integer> successRateHistory;   // % éxito cada 10 ciclos
    private LinkedList<Integer> throughputHistory;    // Procesos completados acumulados
    private LinkedList<Integer> deadlineMissHistory;  // Deadlines incumplidos acumulados
    
    // ========== PROCESOS PARA CÁLCULOS ==========
    private LinkedList<Process> completedProcesses;   // Para cálculos posteriores
    
    public StatisticsTracker() {
        reset();
    }
    
    /**
     * Reinicia todas las estadísticas.
     */
    public void reset() {
        totalProcessesCreated = 0;
        totalProcessesCompleted = 0;
        successfulMissions = 0;
        totalDeadlinesMissed = 0;
        totalInstructionsExecuted = 0;
        cpuBusyCycles = 0;
        cpuIdleCycles = 0;
        totalWaitingTime = 0;
        
        simulationStartCycle = 0;
        currentCycle = 0;
        
        cpuUsageHistory = new LinkedList<>();
        successRateHistory = new LinkedList<>();
        throughputHistory = new LinkedList<>();
        deadlineMissHistory = new LinkedList<>();
        
        completedProcesses = new LinkedList<>();
        
        System.out.println("✅ StatisticsTracker reiniciado");
    }
    
    // ========== REGISTRO DE EVENTOS ==========
    
    /**
     * Registra creación de un proceso.
     */
    public void recordProcessCreation(Process process) {
        totalProcessesCreated++;
    }
    
    /**
     * Registra finalización de un proceso.
     */
    public void recordProcessCompletion(Process process) {
        totalProcessesCompleted++;
        
        if (!process.isDeadlineMissed()) {
            successfulMissions++;
        } else {
            totalDeadlinesMissed++;
        }
        
        // Guardar para cálculos posteriores
        completedProcesses.add(process);
        
        // Calcular waiting time si tenemos datos
        if (process.getStartTime() > 0 && process.getCreationTime() >= 0) {
            int waitingTime = process.getStartTime() - process.getCreationTime();
            if (waitingTime > 0) {
                totalWaitingTime += waitingTime;
            }
        }
        
        // Registrar en histórico cada 5 procesos o cada 10 ciclos
        if (totalProcessesCompleted % 5 == 0 || currentCycle % 10 == 0) {
            recordHistory();
        }
    }
    
    /**
     * Registra ejecución de instrucciones (CPU ocupada).
     */
    public void recordInstructionExecution(int instructionCount) {
        totalInstructionsExecuted += instructionCount;
        cpuBusyCycles += instructionCount; // 1 instrucción = 1 ciclo ocupado
    }
    
    /**
     * Registra ciclo de CPU ociosa.
     */
    public void recordIdleCycle() {
        cpuIdleCycles++;
    }
    
    /**
     * Actualiza el ciclo actual.
     */
    public void setCurrentCycle(int cycle) {
        this.currentCycle = cycle;
        if (simulationStartCycle == 0) {
            simulationStartCycle = cycle;
        }
        
        // Registrar en histórico cada 10 ciclos
        if (cycle % 10 == 0) {
            recordHistory();
        }
    }
    
    /**
     * Registra datos en histórico para gráficas.
     */
    private void recordHistory() {
        cpuUsageHistory.add(calculateCPUUtilization());
        successRateHistory.add((int) calculateSuccessRate());
        throughputHistory.add(totalProcessesCompleted);
        deadlineMissHistory.add(totalDeadlinesMissed);
        
        // Mantener solo últimos 100 registros
        if (cpuUsageHistory.size() > 100) {
            cpuUsageHistory.remove(0);
            successRateHistory.remove(0);
            throughputHistory.remove(0);
            deadlineMissHistory.remove(0);
        }
    }
    
    // ========== CÁLCULO DE MÉTRICAS ==========
    
    /**
     * Calcula tasa de éxito (% procesos que cumplieron deadline).
     */
    public double calculateSuccessRate() {
        if (totalProcessesCompleted == 0) {
            return 100.0; // Por defecto 100% si no hay procesos
        }
        return (successfulMissions * 100.0) / totalProcessesCompleted;
    }
    
    /**
     * Calcula throughput (procesos completados por ciclo).
     */
    public double calculateThroughput() {
        int elapsedCycles = currentCycle - simulationStartCycle;
        if (elapsedCycles == 0) {
            return 0.0;
        }
        return totalProcessesCompleted / (double) elapsedCycles;
    }
    
    /**
     * Calcula utilización del CPU (% ciclos ocupados).
     */
    public int calculateCPUUtilization() {
        int totalCycles = cpuBusyCycles + cpuIdleCycles;
        if (totalCycles == 0) {
            return 0;
        }
        return (cpuBusyCycles * 100) / totalCycles;
    }
    
    /**
     * Calcula tiempo de espera promedio.
     */
    public double calculateAverageWaitingTime() {
        if (totalProcessesCompleted == 0) {
            return 0.0;
        }
        return totalWaitingTime / (double) totalProcessesCompleted;
    }
    
    /**
     * Calcula turnaround time promedio.
     */
    public double calculateAverageTurnaroundTime() {
        if (completedProcesses.isEmpty()) {
            return 0.0;
        }
        
        double totalTurnaround = 0;
        int count = 0;
        
        for (int i = 0; i < completedProcesses.size(); i++) {
            Process p = completedProcesses.get(i);
            if (p.getCompletionTime() > 0 && p.getCreationTime() >= 0) {
                totalTurnaround += (p.getCompletionTime() - p.getCreationTime());
                count++;
            }
        }
        
        return count > 0 ? totalTurnaround / count : 0.0;
    }
    
    /**
     * Calcula tiempo de respuesta promedio (start time - creation time).
     */
    public double calculateAverageResponseTime() {
        if (completedProcesses.isEmpty()) {
            return 0.0;
        }
        
        double totalResponse = 0;
        int count = 0;
        
        for (int i = 0; i < completedProcesses.size(); i++) {
            Process p = completedProcesses.get(i);
            if (p.getStartTime() > 0 && p.getCreationTime() >= 0) {
                totalResponse += (p.getStartTime() - p.getCreationTime());
                count++;
            }
        }
        
        return count > 0 ? totalResponse / count : 0.0;
    }
    
    // ========== GETTERS BÁSICOS ==========
    
    public int getTotalProcessesCreated() {
        return totalProcessesCreated;
    }
    
    public int getTotalProcessesCompleted() {
        return totalProcessesCompleted;
    }
    
    public int getSuccessfulMissions() {
        return successfulMissions;
    }
    
    public int getTotalDeadlinesMissed() {
        return totalDeadlinesMissed;
    }
    
    public int getTotalInstructionsExecuted() {
        return totalInstructionsExecuted;
    }
    
    public int getCpuBusyCycles() {
        return cpuBusyCycles;
    }
    
    public int getCpuIdleCycles() {
        return cpuIdleCycles;
    }
    
    public double getSuccessRate() {
        return calculateSuccessRate();
    }
    
    public double getThroughput() {
        return calculateThroughput();
    }
    
    public int getCPUUtilization() {
        return calculateCPUUtilization();
    }
    
    public double getAverageWaitingTime() {
        return calculateAverageWaitingTime();
    }
    
    public double getAverageTurnaroundTime() {
        return calculateAverageTurnaroundTime();
    }
    
    public double getAverageResponseTime() {
        return calculateAverageResponseTime();
    }
    
    // ========== DATOS PARA GRÁFICAS ==========
    
    public LinkedList<Integer> getCPUUtilizationHistory() {
        return cpuUsageHistory;
    }
    
    public LinkedList<Integer> getSuccessRateHistory() {
        return successRateHistory;
    }
    
    public LinkedList<Integer> getThroughputHistory() {
        return throughputHistory;
    }
    
    public LinkedList<Integer> getDeadlineMissHistory() {
        return deadlineMissHistory;
    }
    
    /**
     * Obtiene las últimas N métricas para gráficas.
     */
    public LinkedList<Integer> getLastNCPUUtilizations(int n) {
        return getLastNFromList(cpuUsageHistory, n);
    }
    
    public LinkedList<Integer> getLastNSuccessRates(int n) {
        return getLastNFromList(successRateHistory, n);
    }
    
    private LinkedList<Integer> getLastNFromList(LinkedList<Integer> list, int n) {
        LinkedList<Integer> result = new LinkedList<>();
        int start = Math.max(0, list.size() - n);
        
        for (int i = start; i < list.size(); i++) {
            result.add(list.get(i));
        }
        
        return result;
    }
    
    // ========== REPORTES ==========
    
    /**
     * Genera reporte completo de estadísticas.
     */
    public String generateReport() {
        StringBuilder report = new StringBuilder();
        report.append("╔════════════════════════════════════════╗\n");
        report.append("║      ESTADÍSTICAS DE SIMULACIÓN       ║\n");
        report.append("╠════════════════════════════════════════╣\n");
        report.append(String.format("║ Ciclos simulados: %22d ║\n", 
                                   currentCycle - simulationStartCycle));
        report.append(String.format("║ Procesos creados: %22d ║\n", totalProcessesCreated));
        report.append(String.format("║ Procesos completados: %17d ║\n", totalProcessesCompleted));
        report.append(String.format("║ Misiones exitosas: %20d ║\n", successfulMissions));
        report.append(String.format("║ Deadlines incumplidos: %16d ║\n", totalDeadlinesMissed));
        report.append(String.format("║ Tasa de éxito: %23.2f%% ║\n", calculateSuccessRate()));
        report.append(String.format("║ Throughput: %25.3f ║\n", calculateThroughput()));
        report.append(String.format("║ Utilización CPU: %21d%% ║\n", calculateCPUUtilization()));
        report.append(String.format("║ Tiempo espera promedio: %14.2f ║\n", calculateAverageWaitingTime()));
        report.append(String.format("║ Turnaround promedio: %17.2f ║\n", calculateAverageTurnaroundTime()));
        report.append(String.format("║ Tiempo respuesta promedio: %11.2f ║\n", calculateAverageResponseTime()));
        report.append(String.format("║ Instrucciones ejecutadas: %14d ║\n", totalInstructionsExecuted));
        report.append("╚════════════════════════════════════════╝\n");
        
        return report.toString();
    }
    
    /**
     * Genera reporte corto para mostrar en GUI.
     */
    public String generateShortReport() {
        return String.format(
            "Éxito: %.1f%% | CPU: %d%% | Throughput: %.3f | Espera: %.1f",
            calculateSuccessRate(),
            calculateCPUUtilization(),
            calculateThroughput(),
            calculateAverageWaitingTime()
        );
    }
    
    /**
     * Genera datos en formato CSV para exportar.
     */
    public String generateCSV() {
        StringBuilder csv = new StringBuilder();
        csv.append("Metrica,Valor\n");
        csv.append(String.format("Ciclos Simulados,%d\n", currentCycle - simulationStartCycle));
        csv.append(String.format("Procesos Creados,%d\n", totalProcessesCreated));
        csv.append(String.format("Procesos Completados,%d\n", totalProcessesCompleted));
        csv.append(String.format("Misiones Exitosas,%d\n", successfulMissions));
        csv.append(String.format("Deadlines Incumplidos,%d\n", totalDeadlinesMissed));
        csv.append(String.format("Tasa de Exito,%.2f\n", calculateSuccessRate()));
        csv.append(String.format("Throughput,%.3f\n", calculateThroughput()));
        csv.append(String.format("Utilizacion CPU,%d\n", calculateCPUUtilization()));
        csv.append(String.format("Tiempo Espera Promedio,%.2f\n", calculateAverageWaitingTime()));
        csv.append(String.format("Turnaround Promedio,%.2f\n", calculateAverageTurnaroundTime()));
        csv.append(String.format("Instrucciones Ejecutadas,%d\n", totalInstructionsExecuted));
        
        return csv.toString();
    }
    
    /**
     * Exporta datos históricos para gráficas.
     */
    public String exportChartData() {
        StringBuilder data = new StringBuilder();
        data.append("Ciclo,CPU%,Exito%,Throughput,DeadlineMiss\n");
        
        int startCycle = Math.max(0, simulationStartCycle);
        int historySize = cpuUsageHistory.size();
        
        for (int i = 0; i < historySize; i++) {
            int cycle = startCycle + (i * 10); // Cada 10 ciclos
            int cpu = i < cpuUsageHistory.size() ? cpuUsageHistory.get(i) : 0;
            int success = i < successRateHistory.size() ? successRateHistory.get(i) : 0;
            int throughput = i < throughputHistory.size() ? throughputHistory.get(i) : 0;
            int deadlineMiss = i < deadlineMissHistory.size() ? deadlineMissHistory.get(i) : 0;
            
            data.append(String.format("%d,%d,%d,%d,%d\n", 
                cycle, cpu, success, throughput, deadlineMiss));
        }
        
        return data.toString();
    }
    
    /**
     * Limpia procesos completados antiguos para liberar memoria.
     */
    public void cleanupOldData() {
        // Mantener solo últimos 1000 procesos completados
        while (completedProcesses.size() > 1000) {
            completedProcesses.remove(0);
        }
    }
}
