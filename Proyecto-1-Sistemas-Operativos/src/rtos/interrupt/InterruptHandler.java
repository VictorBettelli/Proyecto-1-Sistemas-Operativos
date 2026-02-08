/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package rtos.interrupt;

import rtos.structures.PriorityQueue;
import rtos.structures.LinkedList;
import rtos.scheduler.SchedulerManager;
import rtos.utils.InterruptComparator;
import rtos.utils.Semaphore;
/**
 *
 * @author VictorB
 */
/**
 * Manejador de interrupciones con Threads y Semáforos.
 * Implementa el patrón Worker Thread para procesar interrupciones asíncronamente.
 * Cumple con los requerimientos del proyecto: uso de Threads y Semáforos.
 */
public class InterruptHandler {
    private PriorityQueue<InterruptRequest> interruptQueue;
    
    // Lista de handlers registrados
    private LinkedList<HandlerEntry> handlerRegistry;
    
    // Threads trabajadores
    private LinkedList<InterruptWorker> workers;
    
    // Referencia al planificador
    private SchedulerManager schedulerManager;
    
    // Callback para notificar a SimulationEngine
    private InterruptCallback interruptCallback;
    
    // Control de ejecución
    private volatile boolean running;
    
    // Semaforos para sincronización
    private Semaphore queueSemaphore;
    private Semaphore workerSemaphore;
    
    // Número de workers activos
    private final int NUM_WORKERS = 2;
    
    /**
     * Callback para notificar interrupciones a SimulationEngine.
     */
    public interface InterruptCallback {
        void onInterrupt(InterruptRequest request);
    }
    
    /**
     * Entrada en el registro de handlers.
     */
    private static class HandlerEntry {
        InterruptType type;
        Runnable handler;
        String description;
        boolean requiresDedicatedThread;
        
        HandlerEntry(InterruptType type, Runnable handler, 
                    String description, boolean requiresDedicatedThread) {
            this.type = type;
            this.handler = handler;
            this.description = description;
            this.requiresDedicatedThread = requiresDedicatedThread;
        }
        
        @Override
        public String toString() {
            return String.format("Handler[%s: %s]", type, description);
        }
    }
    
    /**
     * Thread trabajador que procesa interrupciones.
     */
    private class InterruptWorker extends Thread {
        private boolean active;
        private int processedCount;
        
        public InterruptWorker(String name) {
            super(name);
            this.active = true;
            this.processedCount = 0;
        }
        
        @Override
        public void run() {
            System.out.println(getName() + " iniciado.");
            
            while (active && running) {
                try {
                    // Esperar por trabajo
                    workerSemaphore.acquire();
                    
                    if (!active || !running) break;
                    
                    // Obtener interrupción de la cola
                    queueSemaphore.acquire();
                    InterruptRequest request = null;
                    if (!interruptQueue.isEmpty()) {
                        request = interruptQueue.extractMin();
                    }
                    queueSemaphore.release();
                    
                    if (request != null) {
                        processInterrupt(request);
                        processedCount++;
                    }
                    
                } catch (InterruptedException e) {
                    System.out.println(getName() + " interrumpido.");
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            
            System.out.println(getName() + " finalizado. Procesó " + processedCount + " interrupciones.");
        }
        
        public void stopWorker() {
            active = false;
            this.interrupt();
        }
        
        public int getProcessedCount() {
            return processedCount;
        }
    }
    
    /**
     * Constructor principal.
     */
    public InterruptHandler(SchedulerManager schedulerManager) {
        this.interruptQueue = new PriorityQueue<>(new InterruptComparator());
        this.handlerRegistry = new LinkedList<>();
        this.workers = new LinkedList<>();
        this.schedulerManager = schedulerManager;
        this.running = true;
        this.interruptCallback = null;
        
        // Inicializar semáforos
        this.queueSemaphore = new Semaphore(1);
        this.workerSemaphore = new Semaphore(0);
        
        // Configurar handlers por defecto
        setupDefaultHandlers();
        
        // Iniciar threads trabajadores
        startWorkerThreads();
        
        System.out.println("✅ InterruptHandler iniciado con " + NUM_WORKERS + " workers.");
    }
    
    /**
     * Registra un callback para notificar interrupciones.
     */
    public void registerInterruptCallback(InterruptCallback callback) {
        this.interruptCallback = callback;
        System.out.println("✅ Callback de interrupciones registrado");
    }
    
    /**
     * Inicia los threads trabajadores.
     */
    private void startWorkerThreads() {
        for (int i = 0; i < NUM_WORKERS; i++) {
            InterruptWorker worker = new InterruptWorker("InterruptWorker-" + (i + 1));
            worker.start();
            workers.add(worker);
        }
    }
    
    /**
     * Configura los handlers por defecto.
     */
    private void setupDefaultHandlers() {
        // MICROMETEORITE - Máxima prioridad
        registerHandler(InterruptType.MICROMETEORITE, 
            () -> handleMicrometeorite(),
            "Emergencia: impacto de micro-meteorito", 
            true);
        
        // SOLAR_FLARE - Alta prioridad
        registerHandler(InterruptType.SOLAR_FLARE,
            () -> handleSolarFlare(),
            "Alerta: ráfaga solar detectada",
            false);
        
        // GROUND_COMMAND - Media prioridad
        registerHandler(InterruptType.GROUND_COMMAND,
            () -> handleGroundCommand(),
            "Comando recibido desde estación terrestre",
            false);
        
        // IO_COMPLETION - Baja prioridad
        registerHandler(InterruptType.IO_COMPLETION,
            () -> handleIOCompletion(),
            "Operación de E/S completada",
            false);
        
        // DEADLINE_MISSED - Alta prioridad
        registerHandler(InterruptType.DEADLINE_MISSED,
            () -> handleDeadlineMissed(),
            "Proceso no cumplió deadline",
            false);
        
        // SYSTEM_ERROR - Máxima prioridad
        registerHandler(InterruptType.SYSTEM_ERROR,
            () -> handleSystemError(),
            "Error crítico del sistema",
            true);
    }
    
    /**
     * Registra un handler para un tipo de interrupción.
     */
    public void registerHandler(InterruptType type, Runnable handler, 
                               String description, boolean requiresDedicatedThread) {
        handlerRegistry.add(new HandlerEntry(type, handler, description, requiresDedicatedThread));
        System.out.println("Handler registrado: " + type + " - " + description);
    }
    
    /**
     * Genera una nueva interrupción.
     */
    public void raiseInterrupt(InterruptType type, int priority, String source) {
        if (!running) {
            System.out.println("InterruptHandler detenido, ignorando interrupción.");
            return;
        }
        
        InterruptRequest request = new InterruptRequest(type, priority, source);
        
        try {
            // Agregar a la cola
            queueSemaphore.acquire();
            interruptQueue.insert(request);
            queueSemaphore.release();
            
            // Señalizar que hay trabajo
            workerSemaphore.release();
            
            // Log
            logEvent("Interrupción GENERADA: " + request);
            
            // Si es de máxima prioridad, forzar procesamiento inmediato
            if (priority >= 4) {
                System.out.println("⚠️  Interrupción de ALTA PRIORIDAD - Notificando inmediatamente");
                // Notificar directamente al callback si está registrado
                if (interruptCallback != null) {
                    interruptCallback.onInterrupt(request);
                }
                workerSemaphore.release(); // Asegurar procesamiento rápido
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Error al generar interrupción: " + e.getMessage());
        }
    }
    
    /**
     * Procesa una interrupción específica.
     */
    private void processInterrupt(InterruptRequest request) {
        logEvent("Procesando interrupción: " + request.getType());
        
        // Buscar handler
        HandlerEntry entry = findHandlerEntry(request.getType());
        
        if (entry != null) {
            // Notificar a SimulationEngine ANTES de ejecutar handler
            if (interruptCallback != null && request.getPriority() >= 3) {
                interruptCallback.onInterrupt(request);
            }
            
            if (entry.requiresDedicatedThread) {
                createDedicatedThread(entry, request);
            } else {
                executeHandler(entry.handler, request);
            }
            
            request.markHandled();
            logEvent("Interrupción ATENDIDA: " + request.getType());
        } else {
            System.out.println("⚠️  No hay handler registrado para: " + request.getType());
        }
    }
    
    /**
     * Crea un thread dedicado para interrupciones críticas.
     */
    private void createDedicatedThread(HandlerEntry entry, InterruptRequest request) {
        Thread dedicatedThread = new Thread(() -> {
            logEvent("Thread DEDICADO iniciado para: " + entry.type);
            
            // Ejecutar handler
            executeHandler(entry.handler, request);
            
            logEvent("Thread DEDICADO finalizado para: " + entry.type);
        }, "Dedicated-ISR-" + entry.type);
        
        dedicatedThread.start();
    }
    
    /**
     * Ejecuta un handler de interrupción.
     */
    private void executeHandler(Runnable handler, InterruptRequest request) {
        try {
            long startTime = System.currentTimeMillis();
            handler.run();
            long duration = System.currentTimeMillis() - startTime;
            
            logEvent(String.format("Handler ejecutado en %d ms para: %s", 
                     duration, request.getType()));
                     
        } catch (Exception e) {
            System.out.println("❌ ERROR en handler para " + request.getType() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Busca un handler entry por tipo.
     */
    private HandlerEntry findHandlerEntry(InterruptType type) {
        for (int i = 0; i < handlerRegistry.size(); i++) {
            HandlerEntry entry = handlerRegistry.get(i);
            if (entry.type == type) {
                return entry;
            }
        }
        return null;
    }
    
    // ========== HANDLERS ESPECÍFICOS ==========
    
    private void handleMicrometeorite() {
        System.out.println("🚨🚨🚨 ALERTA MÁXIMA: IMPACTO DE MICRO-METEORITO DETECTADO");
        System.out.println("   -> Activando protocolos de emergencia");
        System.out.println("   -> Aislando secciones afectadas");
        System.out.println("   -> Redirigiendo potencia a sistemas críticos");
        
        if (schedulerManager != null) {
            schedulerManager.handleEmergency();
        }
    }
    
    private void handleSolarFlare() {
        System.out.println("⚠️⚠️ ALERTA: RÁFAGA SOLAR DETECTADA");
        System.out.println("   -> Reduciendo potencia en paneles solares");
        System.out.println("   -> Orientando nave para protección");
        System.out.println("   -> Activando blindaje electromagnético");
    }
    
    private void handleGroundCommand() {
        System.out.println("📡 COMANDO DESDE TIERRA RECIBIDO");
        System.out.println("   -> Procesando instrucciones...");
        System.out.println("   -> Validando autorización...");
        System.out.println("   -> Ejecutando comando...");
    }
    
    private void handleIOCompletion() {
        System.out.println("✅ OPERACIÓN DE E/S COMPLETADA");
        if (schedulerManager != null) {
            schedulerManager.notifyIOCompletion();
        }
    }
    
    private void handleDeadlineMissed() {
        System.out.println("⏰⏰ DEADLINE INCUMPLIDO DETECTADO");
        System.out.println("   -> Revisando procesos atrasados");
        System.out.println("   -> Recalculando planificación");
        
        if (schedulerManager != null) {
            schedulerManager.handleDeadlineMissed();
        }
    }
    
    private void handleSystemError() {
        System.out.println("❌❌❌ ERROR CRÍTICO DEL SISTEMA");
        System.out.println("   -> Iniciando diagnóstico automático");
        System.out.println("   -> Activando sistemas redundantes");
        System.out.println("   -> Notificando a estación terrestre");
        
        if (schedulerManager != null) {
            schedulerManager.handleSystemError();
        }
    }
    
    // ========== MÉTODOS DE LOGGING ==========
    
    private void logEvent(String message) {
        String timestamp = String.format("[%tT]", System.currentTimeMillis());
        String threadName = Thread.currentThread().getName();
        String logMessage = timestamp + " [" + threadName + "] " + message;
        
        System.out.println(logMessage);
        
        if (schedulerManager != null) {
            schedulerManager.logEvent(logMessage);
        }
    }
    
    // ========== MÉTODOS PÚBLICOS ==========
    
    /**
     * Detiene el InterruptHandler y todos sus threads.
     */
    public void shutdown() {
        System.out.println("Deteniendo InterruptHandler...");
        running = false;
        
        for (int i = 0; i < workers.size(); i++) {
            workers.get(i).stopWorker();
        }
        
        workerSemaphore.release(workers.size());
        
        System.out.println("InterruptHandler detenido.");
    }
    
    /**
     * Obtiene el número de interrupciones pendientes.
     */
    public int getPendingInterruptCount() {
        try {
            queueSemaphore.acquire();
            int count = interruptQueue.size();
            queueSemaphore.release();
            return count;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return 0;
        }
    }
    
    /**
     * Obtiene una copia de las interrupciones pendientes.
     */
    public LinkedList<InterruptRequest> getPendingInterrupts() {
        LinkedList<InterruptRequest> copy = new LinkedList<>();
        try {
            queueSemaphore.acquire();
            for (int i = 0; i < interruptQueue.size(); i++) {
                copy.add(interruptQueue.get(i));
            }
            queueSemaphore.release();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return copy;
    }
    
    /**
     * Obtiene estadísticas de los workers.
     */
    public String getWorkerStats() {
        StringBuilder stats = new StringBuilder();
        stats.append("=== Estadísticas de Workers ===\n");
        for (int i = 0; i < workers.size(); i++) {
            InterruptWorker worker = workers.get(i);
            stats.append(String.format("  %s: %d interrupciones procesadas\n", 
                         worker.getName(), worker.getProcessedCount()));
        }
        stats.append("Interrupciones pendientes: ").append(getPendingInterruptCount());
        return stats.toString();
    }
    
    /**
     * Genera una interrupción aleatoria (para pruebas).
     */
    public void generateRandomInterrupt() {
        InterruptType[] types = InterruptType.values();
        InterruptType randomType = types[(int) (Math.random() * types.length)];
        
        int priority = switch (randomType) {
            case MICROMETEORITE -> 5;
            case SYSTEM_ERROR -> 4;
            case SOLAR_FLARE -> 3;
            case DEADLINE_MISSED -> 2;
            case GROUND_COMMAND, IO_COMPLETION -> 1;
            default -> 1;
        };
        
        String[] devices = {"Paneles Solares", "Sistema de Navegación", 
                           "Comunicaciones", "Sensores", "Propulsión", "CPU", "RAM"};
        String randomDevice = devices[(int) (Math.random() * devices.length)];
        
        raiseInterrupt(randomType, priority, randomDevice);
    }
    
    /**
     * Verifica si el handler está en ejecución.
     */
    public boolean isRunning() {
        return running;
    }
    
    /**
     * Obtiene la lista de handlers registrados.
     */
    public LinkedList<HandlerEntry> getRegisteredHandlers() {
        LinkedList<HandlerEntry> copy = new LinkedList<>();
        for (int i = 0; i < handlerRegistry.size(); i++) {
            copy.add(handlerRegistry.get(i));
        }
        return copy;
    }
}