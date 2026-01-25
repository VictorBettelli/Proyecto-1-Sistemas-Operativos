/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package main.structures;

/**
 * @author luisf
 * @param <T>
 */
public class CustomQueue<T> {
    // El campo 'list' puede ser final porque solo se asigna en el constructor
    private final CustomLinkedList<T> list;

    public CustomQueue() {
        list = new CustomLinkedList<>();
    }
    
    /**
     * Agrega un elemento al final de la cola
     * @param item Elemento a encolar
     */
    public void enqueue(T item) {
        list.add(item);
    }
    
    /**
     * Elimina y retorna el primer elemento de la cola
     * @return Primer elemento o null si la cola está vacía
     */
    public T dequeue() {
        if (list.isEmpty()) {
            return null;
        }
        return list.removeFirst();
    }
    
    /**
     * Retorna el primer elemento sin eliminarlo
     * @return Primer elemento o null si la cola está vacía
     */
    public T peek() {
        if (list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }
    
    /**
     * Verifica si la cola está vacía
     * @return true si la cola está vacía
     */
    public boolean isEmpty() {
        return list.isEmpty();
    }
    
    /**
     * Retorna el tamaño de la cola
     * @return Número de elementos en la cola
     */
    public int size() {
        return list.size();
    }
    
    /**
     * Vacía la cola
     */
    public void clear() {
        list.clear();
    }
    
    /**
     * Verifica si un elemento está en la cola
     * @param item Elemento a buscar
     * @return true si el elemento está presente
     */
    public boolean contains(T item) {
        return list.contains(item);
    }
    
    /**
     * Elimina un elemento específico de la cola
     * @param item Elemento a eliminar
     * @return Elemento eliminado o null si no se encontró
     */
    public T remove(T item) {
        return list.remove(item);
    }
    
    /**
     * Obtiene un elemento por su índice
     * @param index Índice del elemento (0-based)
     * @return Elemento en la posición especificada
     */
    public T get(int index) {
        if (index < 0 || index >= size()) {
            return null;
        }
        return list.get(index);
    }
    
    /**
     * Convierte la cola a un array
     * @return Array con todos los elementos
     */
    public Object[] toArray() {
        return list.toArray();
    }
    
    /**
     * Retorna una representación en String de la cola
     * @return String con los elementos de la cola
     */
    @Override
    public String toString() {
        if (isEmpty()) {
            return "CustomQueue[]";
        }
        
        StringBuilder sb = new StringBuilder("CustomQueue[");
        Object[] elements = list.toArray();
        for (int i = 0; i < elements.length; i++) {
            sb.append(elements[i]);
            if (i < elements.length - 1) {
                sb.append(", ");
            }
        }
        sb.append("]");
        return sb.toString();
    }
}