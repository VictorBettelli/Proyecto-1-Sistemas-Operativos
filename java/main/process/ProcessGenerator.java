/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package main.process;

/**
 *
 * @author luisf
 */
public class ProcessGenerator {

    private static final String[] PROCESS_NAMES = {
        "Monitoreo Altitud", "Control Temperatura", "Transmisión Datos",
        "Recepción Comandos", "Gestión Energía", "Navegación",
        "Procesamiento Imágenes", "Diagnóstico Sistema", "Backup Memoria",
        "Calibración Sensores", "Comunicación Tierra", "Gestión Órbita"
    };
    
    // Usamos un contador para generar IDs únicos para nombres
    private int nameCounter = 1;
    
    public ProcessGenerator() {
        // No necesitamos inicializar Random
    }
    
    public Process generateRandomProcess() {
        // Generar nombre con contador único
        String baseName = PROCESS_NAMES[getRandomInt(0, PROCESS_NAMES.length - 1)];
        String name = baseName + " " + nameCounter++;
        
        // Generar parámetros aleatorios usando Math.random()
        int instructions = getRandomInt(10, 100); // 10-100 instrucciones
        int priority = getRandomInt(1, 10); // 1-10
        int deadline = instructions + getRandomInt(5, 25); // deadline > tiempo ejecución
        boolean isPeriodic = getRandomBoolean(0.4); // 40% son periódicos
        boolean requiresIO = getRandomBoolean(0.3); // 30% requieren E/S
        int period = isPeriodic ? deadline * 2 : 0; // Período para tareas periódicas
        
        return new Process(name, instructions, priority, deadline, 
                          isPeriodic, period, requiresIO);
    }
    
    public Process[] generate20Processes() {
        Process[] processes = new Process[20];
        for (int i = 0; i < 20; i++) {
            processes[i] = generateRandomProcess();
        }
        return processes;
    }
    
    public Process generateEmergencyProcess() {
        Process p = generateRandomProcess();
        p.setPriority(1); // Máxima prioridad para emergencias
        return p;
    }
    
    // Métodos auxiliares para generar números aleatorios
    private int getRandomInt(int min, int max) {
        return min + (int)(Math.random() * ((max - min) + 1));
    }
    
    private boolean getRandomBoolean(double probability) {
        return Math.random() < probability;
    }
    
    private double getRandomDouble(double min, double max) {
        return min + (Math.random() * (max - min));
    }
}
