package com.sdt;

public class Main {
    public static void main(String[] args) {
        // Inicializa o líder
        Node lider = new Node("LIDER", true);
        lider.start();

        // Inicializa os nós
        Node no1 = new Node("[No1]", false);
        Node no2 = new Node("[No2]", false);
        Node no3 = new Node("[No3]", false);

        no1.start();
        no2.start();
        no3.start();

    }
}