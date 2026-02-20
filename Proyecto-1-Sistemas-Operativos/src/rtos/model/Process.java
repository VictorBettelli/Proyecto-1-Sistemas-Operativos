/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package rtos.model;


/**
 *
 * @author luisf
 */
/**
 * Clase que representa un proceso en el sistema RTOS
 * Implementa el PCB (Process Control Block) con todos sus campos
 */
public class Process {
    // Identificación
    private String id;
    private String name;
    
    // Tipo y estado
    private ProcessType type;
    private ProcessState state;
    
    // Características de ejecución
    private int totalInstructions;
    private int executedInstructions;
    private int priority;           // 1 = más alta, 5 = más baja
    private int deadline;           // Tiempo límite absoluto
    private int remainingDeadline;  // Deadline restante
    private int period;             // Solo para procesos periódicos
    private int remainingPeriod;    // Periodo restante
    
    // Operaciones de E/S
    private boolean requiresIO;
    private int ioStartCycle;       // Ciclo en que inicia la E/S
    private int ioDuration;         // Duración de la E/S
    private int ioCompletionTime;   // Ciclo en que termina la E/S
    private int blockedTime;        // Ciclo en que se bloqueó
       
    // Registros del PCB
    private int programCounter;
    private int memoryAddressRegister;
    
    // Tiempos de ejecución
    private int creationTime;       // Ciclo de creación
    private int startTime;          // Ciclo de inicio de ejecución (-1 si no ha empezado)
    private int completionTime;     // Ciclo de finalización (-1 si no ha terminado)
    
    // Estadísticas y métricas
    private int waitingTime;        // Tiempo total en colas
    private int turnaroundTime;     // Tiempo total en el sistema
    private boolean deadlineMissed; // Indica si incumplió su deadline
    
    // ========== CONSTRUCTORES ==========
    
    /**
     * Constructor completo para cualquier tipo de proceso
     */
    public Process(String id, String name, ProcessType type,
                  int totalInstructions, int priority,
                  int deadline, int period) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.totalInstructions = totalInstructions;
        this.priority = priority;
        this.deadline = deadline;
        this.period = period;
        
        // Valores por defecto
        this.executedInstructions = 0;
        this.remainingDeadline = deadline;
        this.remainingPeriod = period;
        
        this.state = ProcessState.NEW;
        
        // Inicialización de campos de E/S
        this.requiresIO = false;
        this.ioStartCycle = 0;
        this.ioDuration = 0;
        this.ioCompletionTime = 0;
        this.blockedTime = -1;      // -1 significa no bloqueado
        
        // Inicialización de registros
        this.programCounter = 0;
        this.memoryAddressRegister = 0;
        
        // Inicialización de tiempos
        this.creationTime = 0;  // Se actualiza al agregar al sistema
        this.startTime = -1;
        this.completionTime = -1;
        
        // Inicialización de estadísticas
        this.waitingTime = 0;
        this.turnaroundTime = 0;
        this.deadlineMissed = false;
    }
    
    /**
     * Constructor simplificado para procesos aperiódicos
     */
    public Process(String id, String name, int totalInstructions,
                  int priority, int deadline) {
        this(id, name, ProcessType.APERIODIC, totalInstructions,
             priority, deadline, 0);
    }
    
    /**
     * Constructor simple para procesos de prueba
     */
    public Process(String id, String name) {
        this(id, name, ProcessType.APERIODIC,
             50,     // totalInstructions por defecto
             3,      // prioridad media
             200,    // deadline por defecto
             0);     // period (0 para aperiódico)
    }
    
    // ========== MÉTODOS DE EJECUCIÓN ==========
        /**
     * Establece el ciclo en que el proceso se bloqueó
     */
    public void setBlockedTime(int cycle) {
        this.blockedTime = cycle;
    }
    
    /**
     * Obtiene el ciclo en que el proceso se bloqueó
     */
    public int getBlockedTime() {
        return blockedTime;
    }
    
    /**
     * Limpia el estado de bloqueo (cuando la E/S termina)
     */
    public void clearBlocked() {
        this.blockedTime = -1;
    }

    /**
     * Ejecuta una instrucción del proceso
     * @return true si el proceso ha terminado, false si aún quedan instrucciones
     */
    public boolean executeInstruction() {
        if (executedInstructions >= totalInstructions) {
            return false; // Ya terminó
        }

        // ¡AVANZAR UNA INSTRUCCIÓN!
        executedInstructions++;
        programCounter++;
        memoryAddressRegister++;

        System.out.println("      📝 " + id + " PC: " + programCounter + 
                          " (" + executedInstructions + "/" + totalInstructions + ")");

        // Verificar si terminó
        if (executedInstructions >= totalInstructions) {
            return true;
        }

        return false;
    }
    
    /**
     * Actualiza el deadline restante
     * Se llama en cada ciclo de reloj
     */
    public void updateDeadline() {
        if (remainingDeadline > 0) {
            remainingDeadline--;
            // Si se agota el deadline y el proceso no terminó
            if (remainingDeadline <= 0 && state != ProcessState.TERMINATED) {
                deadlineMissed = true;
            }
        }
    }
    
    /**
     * Actualiza el periodo restante (para procesos periódicos)
     */
    public void updatePeriod() {
        if (type == ProcessType.PERIODIC && remainingPeriod > 0) {
            remainingPeriod--;
        }
    }
    
    /**
     * Reinicia el periodo (cuando termina un ciclo periódico)
     */
    public void resetPeriod() {
        if (type == ProcessType.PERIODIC) {
            remainingPeriod = period;
        }
    }
    
    /**
     * Configura una operación de E/S para el proceso
     */
    public void setIORequest(int startCycle, int duration) {
        this.requiresIO = true;
        this.ioStartCycle = startCycle;
        this.ioDuration = duration;
        this.ioCompletionTime = startCycle + duration;
    }
    
    /**
     * Verifica si la E/S ha terminado
     */
    public boolean isIOCompleted(int currentCycle) {
        return requiresIO && currentCycle >= ioCompletionTime;
    }
    
    /**
     * Finaliza la E/S y marca el proceso como listo
     */
    public void completeIO() {
        if (state == ProcessState.BLOCKED) {
            state = ProcessState.READY;
        }
    }
    
    // ========== MÉTODOS DE CÁLCULO ==========
    
    /**
     * Calcula el turnaround time (tiempo total en el sistema)
     */
    public void calculateTurnaroundTime() {
        if (completionTime > 0 && creationTime >= 0) {
            turnaroundTime = completionTime - creationTime;
        }
    }
    
    /**
     * Calcula el waiting time (tiempo en colas)
     */
    public void calculateWaitingTime(int currentTime) {
        if (startTime > 0) {
            waitingTime = currentTime - startTime - executedInstructions;
        }
    }
    
    /**
     * Finaliza el proceso con el tiempo actual
     */
    public void finishProcess(int currentTime) {
        this.completionTime = currentTime;
        this.state = ProcessState.TERMINATED;
        calculateTurnaroundTime();
    }
    
    // ========== GETTERS Y SETTERS ==========
    
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public ProcessType getType() { return type; }
    public void setType(ProcessType type) { this.type = type; }

    public ProcessState getState() { return state; }
    public void setState(ProcessState state) { this.state = state; }

    public int getTotalInstructions() { return totalInstructions; }
    public void setTotalInstructions(int totalInstructions) { this.totalInstructions = totalInstructions; }

    public int getExecutedInstructions() { return executedInstructions; }
    public void setExecutedInstructions(int executedInstructions) { this.executedInstructions = executedInstructions; }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    public int getDeadline() { return deadline; }
    public void setDeadline(int deadline) { this.deadline = deadline; }

    public int getRemainingDeadline() { return remainingDeadline; }
    public void setRemainingDeadline(int remainingDeadline) { this.remainingDeadline = remainingDeadline; }

    public int getPeriod() { return period; }
    public void setPeriod(int period) { this.period = period; }

    public int getRemainingPeriod() { return remainingPeriod; }
    public void setRemainingPeriod(int remainingPeriod) { this.remainingPeriod = remainingPeriod; }

    public boolean isRequiresIO() { return requiresIO; }
    public void setRequiresIO(boolean requiresIO) { this.requiresIO = requiresIO; }

    public int getIoStartCycle() { return ioStartCycle; }
    public void setIoStartCycle(int ioStartCycle) { this.ioStartCycle = ioStartCycle; }

    public int getIoDuration() { return ioDuration; }
    public void setIoDuration(int ioDuration) { this.ioDuration = ioDuration; }

    public int getIoCompletionTime() { return ioCompletionTime; }
    public void setIoCompletionTime(int ioCompletionTime) { this.ioCompletionTime = ioCompletionTime; }

    public int getProgramCounter() { return programCounter; }
    public void setProgramCounter(int programCounter) { this.programCounter = programCounter; }

    public int getMemoryAddressRegister() { return memoryAddressRegister; }
    public void setMemoryAddressRegister(int memoryAddressRegister) { this.memoryAddressRegister = memoryAddressRegister; }

    public int getCreationTime() { return creationTime; }
    public void setCreationTime(int creationTime) { this.creationTime = creationTime; }

    public int getStartTime() { return startTime; }
    public void setStartTime(int startTime) { this.startTime = startTime; }

    public int getCompletionTime() { return completionTime; }
    public void setCompletionTime(int completionTime) { this.completionTime = completionTime; }

    public int getWaitingTime() { return waitingTime; }
    public void setWaitingTime(int waitingTime) { this.waitingTime = waitingTime; }

    public int getTurnaroundTime() { return turnaroundTime; }
    public void setTurnaroundTime(int turnaroundTime) { this.turnaroundTime = turnaroundTime; }

    public boolean isDeadlineMissed() { return deadlineMissed; }
    public void setDeadlineMissed(boolean deadlineMissed) { this.deadlineMissed = deadlineMissed; }

    // ========== MÉTODOS DE UTILIDAD ==========
    
    /**
     * Verifica si el proceso ha terminado
     */
    public boolean isFinished() {
        return executedInstructions >= totalInstructions;
    }
    
    /**
     * Verifica si el proceso está listo para ejecutar
     */
    public boolean isReady() {
        return state == ProcessState.READY || state == ProcessState.READY_SUSPENDED;
    }
    
    
    /**
     * Verifica si el proceso está suspendido
     */
    public boolean isSuspended() {
        return state == ProcessState.READY_SUSPENDED || state == ProcessState.BLOCKED_SUSPENDED;
    }
    
    /**
     * Representación en cadena del proceso
     */
    @Override
    public String toString() {
        return String.format("Process[ID=%s, Name=%s, State=%s, PC=%d, Instructions=%d/%d, Deadline=%d/%d]",
                id, name, state, programCounter, executedInstructions, totalInstructions, 
                remainingDeadline, deadline);
    }
}