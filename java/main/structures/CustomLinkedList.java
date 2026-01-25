package main.structures;

public class CustomLinkedList<T> {
    private Node<T> head;
    private Node<T> tail;
    private int size;
    
    // Clase Node interna
    private static class Node<T> {
        T data;
        Node<T> next;
        
        Node(T data) {
            this.data = data;
            this.next = null;
        }
    }
    
    public CustomLinkedList() {
        head = null;
        tail = null;
        size = 0;
    }
    
    public void add(T data) {
        Node<T> newNode = new Node<>(data);
        if (head == null) {
            head = newNode;
            tail = newNode;
        } else {
            tail.next = newNode;
            tail = newNode;
        }
        size++;
    }
    
    public void addFirst(T data) {
        Node<T> newNode = new Node<>(data);
        if (head == null) {
            head = newNode;
            tail = newNode;
        } else {
            newNode.next = head;
            head = newNode;
        }
        size++;
    }
    
    public T removeFirst() {
        if (head == null) return null;
        
        T data = head.data;
        head = head.next;
        if (head == null) tail = null;
        size--;
        return data;
    }
    
    public T remove(T data) {
        if (head == null) return null;
        
        if (head.data.equals(data)) {
            return removeFirst();
        }
        
        Node<T> current = head;
        while (current.next != null && !current.next.data.equals(data)) {
            current = current.next;
        }
        
        if (current.next == null) return null;
        
        T removedData = current.next.data;
        current.next = current.next.next;
        
        if (current.next == null) tail = current;
        size--;
        return removedData;
    }
    
    public T get(int index) {
        if (index < 0 || index >= size) return null;
        
        Node<T> current = head;
        for (int i = 0; i < index; i++) {
            current = current.next;
        }
        return current.data;
    }
    
    public boolean contains(T data) {
        Node<T> current = head;
        while (current != null) {
            if (current.data.equals(data)) return true;
            current = current.next;
        }
        return false;
    }
    
    public boolean isEmpty() {
        return size == 0;
    }
    
    public int size() {
        return size;
    }
    
    public void clear() {
        head = null;
        tail = null;
        size = 0;
    }
    
    // Para iteración
    public Object[] toArray() {
        Object[] array = new Object[size];
        Node<T> current = head;
        int index = 0;
        while (current != null) {
            array[index++] = current.data;
            current = current.next;
        }
        return array;
    }
}
