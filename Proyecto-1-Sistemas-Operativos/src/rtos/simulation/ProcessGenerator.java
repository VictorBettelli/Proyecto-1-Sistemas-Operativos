/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package rtos.simulation;

/**
 *
 * @author VictorB
 */


import rtos.model.Process;
import rtos.model.ProcessType;
import java.util.Random;

/**
 * Generador de procesos aleatorios según especificaciones del PDF.
 * "Generar automáticamente un conjunto inicial de procesos con parámetros aleatorios"
 * "Botón 'Generar 20 Procesos Aleatorios'"
 */
public class ProcessGenerator {
    private final Random random;
    private int processCounter;
    
    // Nombres de procesos para satélite
    private static final String[] PROCESS_NAMES = {
        "Monitor de Temperatura",
        "Control de Altitud",
        "Comunicación Tierra", 
        "Procesamiento de Imágenes",
        "Gestión de Energía",
        "Navegación Estelar",
        "Diagnóstico de Sistemas",
        "Recolección de Datos",
        "Control de Propulsión",
        "Monitoreo de Radiación"
    };
    
    // Tipos de E/S para procesos
    private static final String[] IO_OPERATIONS = {
        "Lectura Sensor",
        "Transmisión Radio",
        "Escritura Memoria",
        "Calibración",
        "Diagnóstico"
    };
    
    public ProcessGenerator() {
        this.random = new Random();
        this.processCounter = 1000; // P1000, P1001, etc.
    }
    
    /**
     * Genera proceso aleatorio con parámetros variados.
     */
    public Process generateRandomProcess() {
        // ID único
        String id = "P" + (processCounter++);
        
        // Nombre aleatorio
        String name = PROCESS_NAMES[random.nextInt(PROCESS_NAMES.length)];
        
        // Tipo (30% periódico, 70% aperiódico)
        ProcessType type = random.nextDouble() < 0.3 ? ProcessType.PERIODIC : ProcessType.APERIODIC;
        
        // Parámetros según PDF: instrucciones, prioridad, deadline, periodo
        int totalInstructions = 10 + random.nextInt(91); // 10-100 instrucciones
        int priority = 1 + random.nextInt(5); // 1-5 (1 = más alta)
        int deadline = totalInstructions + 5 + random.nextInt(20); // Deadline justo después
        int period = type == ProcessType.PERIODIC ? 
                     Math.max(deadline + 10, totalInstructions * 2) : 0;
        
        // Crear proceso
        Process process = new Process(id, name, type, totalInstructions, priority, deadline, period);
        
        // 40% de probabilidad de requerir E/S
        if (random.nextDouble() < 0.4) {
            int ioStart = random.nextInt(Math.max(1, totalInstructions / 2));
            int ioDuration = 2 + random.nextInt(6); // 2-7 ciclos
            process.setIORequest(ioStart, ioDuration);
        }
        
        return process;
    }
    
    /**
     * Genera proceso de emergencia (alta prioridad).
     * Para botón "Añadir Proceso de Emergencia"
     */
    public Process generateEmergencyProcess() {
        String id = "EMG" + (processCounter++);
        
        // Proceso crítico
        Process process = new Process(
            id,
            "🚨 EMERGENCIA - Evento Crítico",
            ProcessType.APERIODIC,
            15,  // Pocas instrucciones
            1,   // Prioridad máxima
            25,  // Deadline corto
            0    // No periódico
        );
        
        // Siempre tiene E/S (comunicación)
        process.setIORequest(5, 3);
        
        return process;
    }
    
    /**
     * Genera proceso periódico típico.
     */
    public Process generatePeriodicProcess(String baseId, String name, int creationTime) {
        String id = baseId + "-" + (processCounter++);
        
        // Proceso periódico de monitoreo
        int totalInstructions = 20 + random.nextInt(31); // 20-50
        int priority = 2 + random.nextInt(3); // 2-4
        int period = 50 + random.nextInt(51); // 50-100 ciclos
        int deadline = period - 5; // Deadline antes del próximo período
        
        Process process = new Process(
            id, name, ProcessType.PERIODIC,
            totalInstructions, priority, deadline, period
        );
        
        // 50% de E/S
        if (random.nextDouble() < 0.5) {
            process.setIORequest(10, 4);
        }
        
        return process;
    }
    
    /**
     * Genera proceso de sistema (baja prioridad).
     */
    public Process generateSystemProcess() {
        String id = "SYS" + (processCounter++);
        
        Process process = new Process(
            id,
            "Mantenimiento del Sistema",
            ProcessType.APERIODIC,
            80 + random.nextInt(41), // 80-120
            5,  // Prioridad más baja
            200 + random.nextInt(101), // Deadline largo
            0
        );
        
        return process;
    }
    
    /**
     * Genera 20 procesos aleatorios (para el botón del PDF).
     */
    public Process[] generate20Processes() {
        Process[] processes = new Process[20];
        for (int i = 0; i < 20; i++) {
            processes[i] = generateRandomProcess();
        }
        return processes;
    }
    
    /**
     * Obtiene tipo de E/S aleatorio para mostrar en GUI.
     */
    public String getRandomIOType() {
        return IO_OPERATIONS[random.nextInt(IO_OPERATIONS.length)];
    }
    
    /**
     * Reinicia contador (para nueva simulación).
     */
    public void resetCounter() {
        processCounter = 1000;
    }
    
    /**
     * Obtiene número de procesos generados.
     */
    public int getGeneratedCount() {
        return processCounter - 1000;
    }
    
    /**
     * Genera proceso con parámetros específicos (para pruebas).
     */
    public Process generateCustomProcess(String id, String name, 
                                        int instructions, int priority, 
                                        int deadline, boolean requiresIO) {
        Process process = new Process(
            id, name, ProcessType.APERIODIC,
            instructions, priority, deadline, 0
        );
        
        if (requiresIO) {
            process.setIORequest(instructions / 3, 3);
        }
        
        return process;
    }
}
