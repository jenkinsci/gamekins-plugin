package org.example;

import org.example.anotherAnotherPackage.*;
import org.example.anotherPackage.CSVParser;


import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

//TestClass used to test the mapping of the gumTree algorithm
public class GumTreeTestClass {

    public String[] getList(String name, int... numbers) {
        StringBuilder stringBuilder = new StringBuilder(name);
        for (int number: numbers) {
            stringBuilder.append(number);
        }
        String[] list = stringBuilder.toString().split("x");
        return list;
    }

    public void doSomething(CSVParser csvParser) {
        //Does nothing special
        csvParser.notify();
        System.out.println("Notified everyone");
    }

    public static BigInteger doSomethingBig() {
        List<Double> doubleList = new ArrayList<>();
        doubleList.add(3.14);
        doubleList.add(3.14);

        return BigInteger.ONE;
    }

    public Additive doSomethingElse() {
        int result = 3 + 4;
        if (result > 2) {
            result = result * 2;
        }
        System.out.println(result);

        return Additive.A;
    }
}