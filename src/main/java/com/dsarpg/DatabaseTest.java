
package com.dsarpg;

public class DatabaseTest {

    public static void main(String[] args) {
        if (Database.testConnection()) {
            System.out.println("Ready to save game progress!");
        } else {
            System.out.println("Please check your MySQL settings.");
        }
    }
}
