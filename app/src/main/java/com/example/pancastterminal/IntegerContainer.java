package com.example.pancastterminal;

import java.math.BigInteger;

public class IntegerContainer extends BigInteger {

    public IntegerContainer(int val)
    {
        super(String.format("%d", val));
    }

    public IntegerContainer(String val)
    {
        super(val);
    }

    public static IntegerContainer make(int val)
    {
        return new IntegerContainer(val);
    }

    public static IntegerContainer make(String val)
    {
        return new IntegerContainer(val);
    }

}
