/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package rtos.structures;

/**
 *
 * @author VictorB
 */
public class Node<T> {
    public T data;
    public Node<T> next;
    
    public Node(T data) {
        this.data = data;
        this.next = null;
    }
    
    public Node(T data, Node<T> next) {
        this.data = data;
        this.next = next;
    }
    
    public T getData() {
        return data;
    }
    
    public void setData(T data) {
        this.data = data;
    }
    
    public Node<T> getNext() {
        return next;
    }
    
    public void setNext(Node<T> next) {
        this.next = next;
    }
    
    public boolean hasNext() {
        return next != null;
    }
    
    @Override
    public String toString() {
        return "Node{" + data + "}";
    }
}
