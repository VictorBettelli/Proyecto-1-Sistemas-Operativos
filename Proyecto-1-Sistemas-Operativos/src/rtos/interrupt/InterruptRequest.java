/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package rtos.interrupt;

/**
 *
 * @author VictorB
 */
public class InterruptRequest {
    private InterruptType type;
    private int priority;           // Prioridad (1-alta, 5-baja)
    private String sourceDevice;    // Dispositivo fuente
    private String description;     // Descripción detallada
    private Object data;            // Datos asociados
    private long timestamp;         // Cuando se generó
    private boolean handled;        // Si ya fue atendida
    
    public InterruptRequest(InterruptType type, int priority, String sourceDevice) {
        this.type = type;
        this.priority = priority;
        this.sourceDevice = sourceDevice;
        this.timestamp = System.currentTimeMillis();
        this.handled = false;
        this.description = generateDescription();
    }
    
    private String generateDescription() {
        switch (type) {
            case MICROMETEORITE:
                return "Impacto detectado en " + sourceDevice + " - Prioridad CRÍTICA";
            case SOLAR_FLARE:
                return "Radiación solar elevada - Proteger sistemas";
            case GROUND_COMMAND:
                return "Comando recibido desde: " + sourceDevice;
            case IO_COMPLETION:
                return "Operación E/S completada en " + sourceDevice;
            case DEADLINE_MISSED:
                return "Proceso excedió deadline - Replanificar";
            case SYSTEM_ERROR:
                return "Error crítico en " + sourceDevice;
            default:
                return "Interrupción no especificada";
        }
    }
    
    // Getters
    public InterruptType getType() { 
        return type; 
    }
    
    public int getPriority() { 
        return priority; 
    }
    
    public String getSourceDevice() { 
        return sourceDevice; 
    }
    
    public String getDescription() { 
        return description; 
    }
    
    public Object getData() { 
        return data; 
    }
    
    public long getTimestamp() { 
        return timestamp; 
    }
    
    public boolean isHandled() { 
        return handled; 
    }
    
    // Setters
    public void setData(Object data) { 
        this.data = data; 
    }
    
    public void markHandled() { 
        this.handled = true; 
    }
    
    @Override
    public String toString() {
        return String.format("[IRQ] %s | Pri: %d | Fuente: %s | %s",
            type.toString(), priority, sourceDevice, description);
    }
}