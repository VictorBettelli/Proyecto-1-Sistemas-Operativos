/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package rtos.structures;

/**
 *
 * @author VictorB
 */
public class Queue<T> {
    private LinkedList<T> list;
    
    public Queue() {
        list = new LinkedList<>();
    }
    
    public void enqueue(T data) {
        list.add(data);
    }
    
    public T dequeue() {
        if (isEmpty()) return null;
        return list.remove(0);
    }
    
    public T peek() {
        if (isEmpty()) return null;
        return list.get(0);
    }
    
    public boolean isEmpty() {
        return list.isEmpty();
    }
    
    public int size() {
        return list.size();
    }
    
    public void clear() {
        list.clear();
    }
    
    public boolean contains(T data) {
        return list.contains(data);
    }
    
    public LinkedList<T> toLinkedList() {
        LinkedList<T> copy = new LinkedList<>();
        for (int i = 0; i < list.size(); i++) {
            copy.add(list.get(i));
        }
        return copy;
    }
    
    @Override
    public String toString() {
        return "Queue" + list.toString().replace("LinkedList", "");
    }
}
