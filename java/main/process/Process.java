/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package main.process;

/**
 *
 * @author luisf
 */
public class Process {
    private static int nextId = 1;
    private final int id;
    private final String name;
    private ProcessState state;
    private int priority;
    private final int totalInstructions;
    private int executedInstructions;
    private final int deadline; // en ciclos de reloj
    private int arrivalTime;
    private int remainingTime;
    private final boolean isPeriodic;
    private final int period; // para tareas periódicas
    private final boolean requiresIO;
    private int ioStartCycle;
    private int ioDuration;
    private int pc; // Program Counter
    private int mar; // Memory Address Register
    
    // Campos adicionales que necesitarás
    private int waitTime;  // Tiempo de espera
    private int finishTime; // Tiempo de finalización
    
    public Process(String name, int totalInstructions, int priority, 
               int deadline, boolean isPeriodic, int period, boolean requiresIO) {
        this.id = nextId++;
        this.name = name;
        this.state = ProcessState.NEW;
        this.priority = priority;
        this.totalInstructions = totalInstructions;
        this.executedInstructions = 0;
        this.deadline = deadline;
        this.arrivalTime = 0;
        this.remainingTime = totalInstructions;
        this.isPeriodic = isPeriodic;
        this.period = period;
        this.requiresIO = requiresIO;
        this.pc = 0;
        this.mar = 0;
        this.waitTime = 0;
        this.finishTime = 0;

        if (requiresIO) {
            // Usar porcentaje en lugar de Math.random()
            this.ioStartCycle = (int)(totalInstructions * 0.7);
            this.ioDuration = 5; // Valor fijo o calcular con lógica determinista
        } else {
            this.ioStartCycle = 0;
            this.ioDuration = 0;
        }
}
    
    // Constructor simplificado (sin periodo)
    public Process(String name, int totalInstructions, int priority, 
                   int deadline, boolean isPeriodic, boolean requiresIO) {
        this(name, totalInstructions, priority, deadline, isPeriodic, 
             isPeriodic ? (int)(deadline * 0.8) : 0, requiresIO);
    }
    
    // Getters y Setters
    public int getId() { return id; }
    public String getName() { return name; }
    public ProcessState getState() { return state; }
    public void setState(ProcessState state) { this.state = state; }
    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }
    public int getTotalInstructions() { return totalInstructions; }
    public int getExecutedInstructions() { return executedInstructions; }
    public int getDeadline() { return deadline; }
    public int getArrivalTime() { return arrivalTime; }
    public void setArrivalTime(int time) { this.arrivalTime = time; }
    public int getRemainingTime() { return remainingTime; }
    public boolean isPeriodic() { return isPeriodic; }
    public int getPeriod() { return period; }  // GETTER AÑADIDO
    public boolean requiresIO() { return requiresIO; }
    public int getIOStartCycle() { return ioStartCycle; }
    public int getIODuration() { return ioDuration; }
    public int getPC() { return pc; }
    public int getMAR() { return mar; }
    public int getWaitTime() { return waitTime; }
    public void setWaitTime(int waitTime) { this.waitTime = waitTime; }
    public int getFinishTime() { return finishTime; }
    public void setFinishTime(int finishTime) { this.finishTime = finishTime; }
    
    public void executeInstruction() {
        executedInstructions++;
        remainingTime--;
        pc++;
        mar++;
        
        // Verificar si es momento de E/S
        if (requiresIO && executedInstructions == ioStartCycle) {
            state = ProcessState.BLOCKED;
        }
    }
    
    public void incrementWaitTime() {
        waitTime++;
    }
    
    public boolean isIOComplete(int currentCycle) {
        if (!requiresIO) return false;
        int ioEndCycle = ioStartCycle + ioDuration;
        return currentCycle >= ioEndCycle;
    }
    
    public boolean isDeadlineMissed(int currentCycle) {
        return currentCycle > arrivalTime + deadline;
    }
    
    public boolean isFinished() {
        return executedInstructions >= totalInstructions;
    }
    
    public int getTimeUntilDeadline(int currentCycle) {
        return (arrivalTime + deadline) - currentCycle;
    }
    
    public int getTurnaroundTime() {
        return finishTime - arrivalTime;
    }
    
    public int getResponseTime() {
        // Para simplificar, asumimos que el tiempo de respuesta 
        // es cuando empieza a ejecutarse por primera vez
        return waitTime;
    }
    
    // Método para tareas periódicas
    public boolean shouldRespawn(int currentCycle) {
        if (!isPeriodic) return false;
        return currentCycle % period == 0;
    }
    
    public Process createNextPeriod() {
        if (!isPeriodic) return null;
        
        // Crear nueva instancia para el siguiente período
        Process next = new Process(name + " (Periodic)", totalInstructions, 
                                   priority, deadline, true, period, requiresIO);
        next.setArrivalTime(this.arrivalTime + period);
        return next;
    }
    
    @Override
    public String toString() {
        return String.format("P%d: %s [Estado: %s, Inst: %d/%d, Deadline: %d, Prioridad: %d]", 
                id, name, state, executedInstructions, totalInstructions, deadline, priority);
    }
    
    // Método para información detallada del PCB
    public String getPCBInfo() {
        return String.format(
            "PCB ID: %d | Nombre: %s | Estado: %s | PC: %d | MAR: %d | " +
            "Prioridad: %d | Deadline Restante: %d | " +
            "Instrucciones: %d/%d | Requiere E/S: %s",
            id, name, state, pc, mar, priority, 
            (deadline - (executedInstructions - arrivalTime)),
            executedInstructions, totalInstructions, requiresIO ? "Sí" : "No"
        );
    }
}
