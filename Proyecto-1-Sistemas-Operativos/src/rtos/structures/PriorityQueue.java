/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package rtos.structures;
import rtos.utils.Comparator;  // ← NUESTRO Comparator, NO java.util.Comparator
/**
 *
 * @author VictorB
 */


/**
 * Cola de prioridad personalizada para algoritmos EDF y Prioridad
 * Implementación propia - NO usar java.util.PriorityQueue
 * @param <T> Tipo de dato almacenado
 */
public class PriorityQueue<T> {
    private LinkedList<T> list;
    private Comparator<T> comparator;
    
    /**
     * Constructor
     * @param comparator Comparador personalizado para ordenar elementos
     */
    public PriorityQueue(Comparator<T> comparator) {
        this.list = new LinkedList<>();
        this.comparator = comparator;
    }
    
    /**
     * Insertar elemento manteniendo el orden
     * @param data Elemento a insertar
     */
    public void insert(T data) {
        if (list.isEmpty()) {
            list.add(data);
            return;
        }
        
        // Encontrar posición correcta (orden ascendente según comparator)
        int index = 0;
        while (index < list.size() && comparator.compare(data, list.get(index)) > 0) {
            index++;
        }
        
        // Insertar en posición encontrada
        list.insert(index, data);
    }
    
    /**
     * Obtener y remover el elemento con mayor prioridad (menor según comparator)
     * @return Elemento con mayor prioridad, o null si está vacía
     */
    public T extractMin() {
        if (list.isEmpty()) {
            return null;
        }
        return list.remove(0);
    }
    
    /**
     * Ver elemento con mayor prioridad sin removerlo
     * @return Elemento con mayor prioridad, o null si está vacía
     */
    public T peekMin() {
        if (list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }
    
    /**
     * Verificar si la cola está vacía
     * @return true si está vacía
     */
    public boolean isEmpty() {
        return list.isEmpty();
    }
    
    /**
     * Obtener tamaño de la cola
     * @return Cantidad de elementos
     */
    public int size() {
        return list.size();
    }
    
    /**
     * Vaciar la cola
     */
    public void clear() {
        list.clear();
    }
    
    /**
     * Verificar si contiene un elemento
     * @param data Elemento a buscar
     * @return true si contiene el elemento
     */
    public boolean contains(T data) {
        return list.contains(data);
    }
    
    /**
     * Obtener elemento por índice (para debugging)
     * @param index Índice del elemento
     * @return Elemento en esa posición
     */
    public T get(int index) {
        return list.get(index);
    }
    
    @Override
    public String toString() {
        if (list.isEmpty()) {
            return "PriorityQueue[]";
        }
        
        StringBuilder sb = new StringBuilder("PriorityQueue[");
        for (int i = 0; i < list.size(); i++) {
            sb.append(list.get(i));
            if (i < list.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append("]");
        return sb.toString();
    }
}