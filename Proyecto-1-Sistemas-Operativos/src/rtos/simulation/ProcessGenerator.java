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
import rtos.model.Process;
import rtos.model.ProcessType;

/**
 * Generador de procesos aleatorios según especificaciones del PDF.
 * "Generar automáticamente un conjunto inicial de procesos con parámetros aleatorios"
 * "Botón 'Generar 20 Procesos Aleatorios'"
 */
public class ProcessGenerator {
    private final java.util.Random random;
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
    
    public ProcessGenerator() {
        this.random = new java.util.Random();
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
        
        // Parámetros: instrucciones, prioridad, deadline, periodo
        int totalInstructions = 10 + random.nextInt(91); // 10-100 instrucciones
        int priority = 1 + random.nextInt(5); // 1-5 (1 = más alta)
        int deadline = totalInstructions + 5 + random.nextInt(20);
        int period = type == ProcessType.PERIODIC ? 
                     Math.max(deadline + 10, totalInstructions * 2) : 0;
        
        // Crear proceso
        Process process = new Process(id, name, type, totalInstructions, priority, deadline, period);
        
        // 40% de probabilidad de requerir E/S
        if (random.nextDouble() < 0.4) {
            int ioStart = 1 + random.nextInt(Math.max(1, totalInstructions / 2));
            int ioDuration = 2 + random.nextInt(6); // 2-7 ciclos
            process.setIORequest(ioStart, ioDuration); // ✅ ESTE MÉTODO DEBE EXISTIR
        }
        
        return process;
    }
    
    /**
     * Genera proceso de emergencia (alta prioridad).
     */
    public Process generateEmergencyProcess() {
        String id = "EMG" + (processCounter++);
        
        Process process = new Process(
            id,
            "🚨 EMERGENCIA - Impacto",
            ProcessType.APERIODIC,
            15,  // Pocas instrucciones
            1,   // Prioridad máxima
            25,  // Deadline corto
            0    // No periódico
        );
        
        // Siempre tiene E/S
        process.setIORequest(5, 3); // ✅ ESTE MÉTODO DEBE EXISTIR
        
        return process;
    }
    
    /**
     * Genera proceso periódico típico.
     */
    public Process generatePeriodicProcess(String baseId, String name, int creationTime) {
        String id = baseId + "-" + (processCounter++);
        
        int totalInstructions = 20 + random.nextInt(31); // 20-50
        int priority = 2 + random.nextInt(3); // 2-4
        int period = 50 + random.nextInt(51); // 50-100 ciclos
        int deadline = period - 5;
        
        Process process = new Process(
            id, name, ProcessType.PERIODIC,
            totalInstructions, priority, deadline, period
        );
        
        // 50% de E/S
        if (random.nextDouble() < 0.5) {
            process.setIORequest(10, 4); // ✅ ESTE MÉTODO DEBE EXISTIR
        }
        
        return process;
    }
    
    /**
     * Genera 20 procesos aleatorios.
     */
    public Process[] generate20Processes() {
        Process[] processes = new Process[20];
        for (int i = 0; i < 20; i++) {
            processes[i] = generateRandomProcess();
        }
        return processes;
    }
    
    /**
     * Reinicia contador.
     */
    public void resetCounter() {
        processCounter = 1000;
    }
}
