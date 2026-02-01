/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package rtos.scheduler;

/**
 *
 * @author luisf
 */
import rtos.model.Process;
import rtos.model.ProcessState;
import rtos.structures.Queue;
import rtos.interrupt.InterruptType;
import rtos.interrupt.InterruptRequest;
import rtos.structures.LinkedList;
/**
 * Gestiona múltiples algoritmos de planificación y permite cambiar entre ellos
 *
 */

public class SchedulerManager {
    private Scheduler currentScheduler;
    private FCFSScheduler fcfsScheduler;
    private RoundRobinScheduler rrScheduler;
    private SRTScheduler srtScheduler;
    private PriorityScheduler priorityScheduler;
    private EDFScheduler edfScheduler;
    
    // Para manejo de logs (necesario para interrupciones)
    private LinkedList<String> eventLogs;
    
    public enum Algorithm {
        FCFS, ROUND_ROBIN, SRT, PRIORITY, EDF
    }
    
    public SchedulerManager() {
        // Crear todos los schedulers
        this.fcfsScheduler = new FCFSScheduler();
        this.rrScheduler = new RoundRobinScheduler(4); // Quantum por defecto: 4
        this.srtScheduler = new SRTScheduler();
        this.priorityScheduler = new PriorityScheduler();
        this.edfScheduler = new EDFScheduler();
        
        // Establecer FCFS como scheduler por defecto
        this.currentScheduler = fcfsScheduler;
        
        // Inicializar logs
        this.eventLogs = new LinkedList<>();
    }
    
    // ========== MÉTODOS EXISTENTES (no cambiar) ==========
    /**
     * Cambia el algoritmo de planificación actual
     */
    
    public void switchAlgorithm(Algorithm algorithm) {
        System.out.println("Cambiando algoritmo a: " + algorithm);
        addLogEntry("Cambio de algoritmo a: " + algorithm);
        
        // Mover procesos del scheduler actual al nuevo scheduler
        Queue<Process> currentQueue = currentScheduler.getReadyQueue();
        
        // Guardar referencia al scheduler anterior
        Scheduler oldScheduler = currentScheduler;
        
        // Cambiar al nuevo scheduler
        switch(algorithm) {
            case FCFS:
                currentScheduler = fcfsScheduler;
                break;
            case ROUND_ROBIN:
                currentScheduler = rrScheduler;
                break;
            case SRT:
                currentScheduler = srtScheduler;
                break;
            case PRIORITY:
                currentScheduler = priorityScheduler;
                break;
            case EDF:
                currentScheduler = edfScheduler;
                break;
        }
        
        // Transferir procesos del scheduler anterior al nuevo
        transferProcesses(oldScheduler, currentScheduler, currentQueue);
    }
    
    /**
     * Agrega un proceso al scheduler actual
     */
    
    public void addProcess(Process process) {
        currentScheduler.addProcess(process);
    }
    
    /**
     * Obtiene el próximo proceso a ejecutar
     */
    
    public Process getNextProcess() {
        return currentScheduler.getNextProcess();
    }
    
    /**
     * Verifica si no hay procesos en el scheduler actual
     */
    
    public boolean isEmpty() {
        return currentScheduler.isEmpty();
    }
    
    /**
     * Obtiene la cola de listos del scheduler actual
     */
    
    public Queue<Process> getReadyQueue() {
        return currentScheduler.getReadyQueue();
    }
    
    /**
     * Obtiene el nombre del algoritmo actual
     */
    
    public String getCurrentAlgorithmName() {
        return currentScheduler.getName();
    }
    
    /**
     * Obtiene el algoritmo actual
     */
    
    public Algorithm getCurrentAlgorithm() {
        if (currentScheduler instanceof FCFSScheduler) return Algorithm.FCFS;
        if (currentScheduler instanceof RoundRobinScheduler) return Algorithm.ROUND_ROBIN;
        if (currentScheduler instanceof SRTScheduler) return Algorithm.SRT;
        if (currentScheduler instanceof PriorityScheduler) return Algorithm.PRIORITY;
        if (currentScheduler instanceof EDFScheduler) return Algorithm.EDF;
        return Algorithm.FCFS;
    }
    
    /**
     * Establece el quantum para Round Robin
     */
    
    public void setQuantum(int quantum) {
        if (rrScheduler != null) {
            rrScheduler.setQuantum(quantum);
        }
    }
    
    /**
     * Transfiere procesos entre schedulers
     */
    
    private void transferProcesses(Scheduler from, Scheduler to, Queue<Process> queue) {
        // Copiar procesos de la cola anterior
        Queue<Process> tempQueue = new Queue<>();
        
        // Crear una copia de los procesos
        while (!queue.isEmpty()) {
            Process p = queue.dequeue();
            tempQueue.enqueue(p);
        }
        
        // Transferir procesos al nuevo scheduler
        while (!tempQueue.isEmpty()) {
            Process p = tempQueue.dequeue();
            p.setState(ProcessState.READY); // Asegurar que esté en estado READY
            to.addProcess(p);
            // También agregar de vuelta a la cola original para mantener consistencia
            queue.enqueue(p);
        }
    }
    
    // ========== NUEVOS MÉTODOS PARA INTERRUPCIONES ==========
    
    /**
     * Maneja una interrupción de emergencia (MICROMETEORITE, SYSTEM_ERROR)
     */
    
    public void handleEmergency() {
        String message = "🚨 EMERGENCIA: Activando protocolos de seguridad";
        System.out.println(message);
        addLogEntry(message);
        
        // En una implementación real: suspender procesos no críticos
        // Priorizar procesos de sistema
        // Cambiar posiblemente a EDF para manejar deadlines críticos
    }
    
    /**
     * Notifica la finalización de una operación de E/S
     */
    
    public void notifyIOCompletion() {
        String message = "✅ E/S Completada: Revisando procesos bloqueados";
        System.out.println(message);
        addLogEntry(message);
        
        // En una implementación real: mover procesos de BLOCKED a READY
        // queueBlockedToReady();
    }
    
    /**
     * Maneja un deadline incumplido
     */
    
    public void handleDeadlineMissed() {
        String message = "⏰ Deadline Incumplido: Replanificando tareas";
        System.out.println(message);
        addLogEntry(message);
        
        // Sugerencia: Cambiar a EDF si no está ya activo
        if (!(currentScheduler instanceof EDFScheduler)) {
            System.out.println("   -> Cambiando a EDF para mejor manejo de deadlines");
            switchAlgorithm(Algorithm.EDF);
        }
    }
    
    /**
     * Maneja un error del sistema
     */
    
    public void handleSystemError() {
        String message = "❌ Error del Sistema: Iniciando diagnóstico";
        System.out.println(message);
        addLogEntry(message);
        
        // En una implementación real: 
        // - Pausar procesos no críticos
        // - Ejecutar rutinas de recuperación
        // - Notificar a procesos de monitoreo
    }
    
    /**
     * Método genérico para manejar cualquier interrupción
     */
    
    public void onCriticalInterrupt(InterruptRequest request) {
        String message = String.format("⚡ Interrupción Crítica: %s (Prioridad: %d)", 
                                      request.getType(), request.getPriority());
        System.out.println(message);
        addLogEntry(message);
        
        // Acciones específicas según tipo
        switch (request.getType()) {
            case MICROMETEORITE:
            case SYSTEM_ERROR:
                handleEmergency();
                break;
            case DEADLINE_MISSED:
                handleDeadlineMissed();
                break;
            case IO_COMPLETION:
                notifyIOCompletion();
                break;
            case SOLAR_FLARE:
                addLogEntry("Ráfaga solar: Reduciendo consumo energético");
                break;
            case GROUND_COMMAND:
                addLogEntry("Comando Tierra: Procesando instrucciones");
                break;
        }
    }
    
    /**
     * Registra un evento en el log del sistema
     */
    
    public void logEvent(String message) {
        addLogEntry(message);
    }
    
    /**
     * Agrega una entrada al log interno
     */
    
    private void addLogEntry(String message) {
        String timestamp = String.format("[%tT]", System.currentTimeMillis());
        String logEntry = timestamp + " " + message;
        eventLogs.add(logEntry);
        
        // Mantener solo los últimos 100 logs
        if (eventLogs.size() > 100) {
            eventLogs.remove(0);
        }
    }
    
    /**
     * Obtiene los últimos logs del sistema
     */
    
    public LinkedList<String> getEventLogs() {
        return eventLogs;
    }
    
    /**
     * Obtiene los últimos N logs
     */
    
    public LinkedList<String> getRecentLogs(int count) {
        LinkedList<String> recent = new LinkedList<>();
        int start = Math.max(0, eventLogs.size() - count);
        
        for (int i = start; i < eventLogs.size(); i++) {
            recent.add(eventLogs.get(i));
        }
        
        return recent;
    }
    
    /**
     * Limpia los logs del sistema
     */
    
    public void clearLogs() {
        eventLogs.clear();
        addLogEntry("Logs del sistema limpiados");
    }
    
    /**
     * Método para simular una interrupción (para pruebas)
     */
    
    public void simulateInterrupt(InterruptType type, int priority, String source) {
        String message = String.format("[SIM] Interrupción simulada: %s (Pri: %d, Fuente: %s)", 
                                      type, priority, source);
        System.out.println(message);
        addLogEntry(message);
    }
}
