/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package rtos.structures;

/**
 *
 * @author VictorB
 */
public class LinkedList<T> {
    private Node<T> head;
    private Node<T> tail;
    private int size;
    
    public LinkedList() {
        head = null;
        tail = null;
        size = 0;
    }
    
    /**
     * Agrega un elemento al final de la lista
     */
    public void add(T data) {
        Node<T> newNode = new Node<>(data);
        if (head == null) {
            head = newNode;
            tail = newNode;
        } else {
            tail.setNext(newNode);
            tail = newNode;
        }
        size++;
    }
    
    /**
     * Agrega un elemento al inicio de la lista
     */
    public void addFirst(T data) {
        Node<T> newNode = new Node<>(data);
        if (head == null) {
            head = newNode;
            tail = newNode;
        } else {
            newNode.setNext(head);
            head = newNode;
        }
        size++;
    }
    
    /**
     * Inserta un elemento en una posición específica
     */
    public void insert(int index, T data) {
        if (index < 0 || index > size) {
            throw new IndexOutOfBoundsException("Índice: " + index + ", Tamaño: " + size);
        }
        
        if (index == 0) {
            addFirst(data);
        } else if (index == size) {
            add(data);
        } else {
            Node<T> current = head;
            for (int i = 0; i < index - 1; i++) {
                current = current.getNext();
            }
            Node<T> newNode = new Node<>(data, current.getNext());
            current.setNext(newNode);
            size++;
        }
    }
    
    /**
     * Obtiene un elemento por índice
     */
    public T get(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Índice: " + index + ", Tamaño: " + size);
        }
        
        Node<T> current = head;
        for (int i = 0; i < index; i++) {
            current = current.getNext();
        }
        return current.getData();
    }
    
    /**
     * Remueve un elemento por índice
     * @return El elemento removido
     */
    public T remove(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Índice: " + index + ", Tamaño: " + size);
        }
        
        T removedData;
        
        if (index == 0) {
            removedData = head.getData();
            head = head.getNext();
            if (head == null) {
                tail = null;
            }
        } else {
            Node<T> current = head;
            for (int i = 0; i < index - 1; i++) {
                current = current.getNext();
            }
            
            removedData = current.getNext().getData();
            current.setNext(current.getNext().getNext());
            
            if (current.getNext() == null) {
                tail = current;
            }
        }
        
        size--;
        return removedData;
    }
    
    /**
     * Remueve la primera ocurrencia de un elemento específico
     * @param data El elemento a remover
     * @return true si se removió, false si no se encontró
     */
    public boolean remove(T data) {
        if (isEmpty()) return false;
        
        // Caso 1: Es el primer elemento
        if (head.getData().equals(data)) {
            head = head.getNext();
            if (head == null) {
                tail = null;
            }
            size--;
            return true;
        }
        
        // Caso 2: Buscar en medio/final
        Node<T> current = head;
        while (current.getNext() != null && !current.getNext().getData().equals(data)) {
            current = current.getNext();
        }
        
        // Caso 3: Encontrarlo
        if (current.getNext() != null) {
            current.setNext(current.getNext().getNext());
            if (current.getNext() == null) {
                tail = current;
            }
            size--;
            return true;
        }
        
        // Caso 4: No encontrado
        return false;
    }
    
    /**
     * Verifica si la lista contiene un elemento
     */
    public boolean contains(T data) {
        Node<T> current = head;
        while (current != null) {
            if (current.getData().equals(data)) {
                return true;
            }
            current = current.getNext();
        }
        return false;
    }
    
    /**
     * Busca el índice de un elemento
     */
    public int indexOf(T data) {
        Node<T> current = head;
        int index = 0;
        while (current != null) {
            if (current.getData().equals(data)) {
                return index;
            }
            current = current.getNext();
            index++;
        }
        return -1;
    }
    
    public int size() {
        return size;
    }
    
    public boolean isEmpty() {
        return size == 0;
    }
    
    public void clear() {
        head = null;
        tail = null;
        size = 0;
    }
    
    // Métodos de acceso para uso interno
    Node<T> getHead() {
        return head;
    }
    
    Node<T> getTail() {
        return tail;
    }
    
    void incrementSize() {
        size++;
    }
    
    void setHead(Node<T> newHead) {
        this.head = newHead;
    }
    
    Node<T> getNode(int index) {
        if (index < 0 || index >= size) return null;
        
        Node<T> current = head;
        for (int i = 0; i < index; i++) {
            current = current.getNext();
        }
        return current;
    }
    
    @Override
    public String toString() {
        if (isEmpty()) return "LinkedList[]";
        
        StringBuilder sb = new StringBuilder("LinkedList[");
        Node<T> current = head;
        while (current != null) {
            sb.append(current.getData());
            if (current.getNext() != null) sb.append(", ");
            current = current.getNext();
        }
        sb.append("]");
        return sb.toString();
    }
}
