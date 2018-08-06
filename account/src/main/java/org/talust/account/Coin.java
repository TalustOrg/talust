/*
 * MIT License
 *
 * Copyright (c) 2017-2018 talust.org talust.io
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */
package org.talust.account;


import org.talust.common.crypto.Utils;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * TALC 单位
 */
public final class Coin implements Comparable<Coin>, Serializable {

	private static final long serialVersionUID = 6978149202334427537L;

	public static final int SMALLEST_UNIT_EXPONENT = 8;

    public static final long COIN_VALUE = (long) Math.pow(10, SMALLEST_UNIT_EXPONENT);

    /**
     * 代币总量
     */
    public static final Coin MAX = Coin.valueOf(100000000).multiply(COIN_VALUE);
    
    /**
     * 0个 TALC
     */
    public static final Coin ZERO = Coin.valueOf(0);

    /**
     * 1个 TALC
     */
    public static final Coin COIN = Coin.valueOf(COIN_VALUE);

    /**
     * 0.01 TALC
     */
    public static final Coin CENT = COIN.divide(100);

    /**
     * 0.001 TALC
     */
    public static final Coin MILLICOIN = COIN.divide(1000);

    /**
     * 0.000001 TALC
     */
    public static final Coin MICROCOIN = MILLICOIN.divide(1000);

    /**
     * 数量
     */
    public final long value;

    private Coin(final long coin) {
        this.value = coin;
    }

    public static Coin valueOf(final long coin) {
        return new Coin(coin);
    }

    public int smallestUnitExponent() {
        return SMALLEST_UNIT_EXPONENT;
    }

    public long getValue() {
        return value;
    }

    public static Coin valueOf(final int coins, final int cents) {
    	Utils.checkState(cents < 100);
        Utils.checkState(cents >= 0);
        Utils.checkState(coins >= 0);
        final Coin coin = COIN.multiply(coins).add(CENT.multiply(cents));
        return coin;
    }

    public static Coin parseCoin(final String str) {
        try {
            long value = new BigDecimal(str).movePointRight(SMALLEST_UNIT_EXPONENT).toBigIntegerExact().longValue();
            return Coin.valueOf(value);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public Coin add(final Coin value) {
        return new Coin(this.value + value.value);
    }

    /** Alias for add */
    public Coin plus(final Coin value) {
        return add(value);
    }

    public Coin subtract(final Coin value) {
        return new Coin(this.value - value.value);
    }

    /** Alias for subtract */
    public Coin minus(final Coin value) {
        return subtract(value);
    }

    public Coin multiply(final long factor) {
        return new Coin(this.value * factor);
    }

    /** Alias for multiply */
    public Coin times(final long factor) {
        return multiply(factor);
    }

    /** Alias for multiply */
    public Coin times(final int factor) {
        return multiply(factor);
    }

    public Coin divide(final long divisor) {
        return new Coin(this.value / divisor);
    }

    /** Alias for divide */
    public Coin div(final long divisor) {
        return divide(divisor);
    }

    /** Alias for divide */
    public Coin div(final int divisor) {
        return divide(divisor);
    }

    public Coin[] divideAndRemainder(final long divisor) {
        return new Coin[] { new Coin(this.value / divisor), new Coin(this.value % divisor) };
    }

    public long divide(final Coin divisor) {
        return this.value / divisor.value;
    }
    
    /**
     * Returns true if and only if this instance represents a monetary value greater than zero,
     * otherwise false.
     */
    public boolean isPositive() {
        return signum() == 1;
    }

    /**
     * Returns true if and only if this instance represents a monetary value less than zero,
     * otherwise false.
     */
    public boolean isNegative() {
        return signum() == -1;
    }

    /**
     * Returns true if and only if this instance represents zero monetary value,
     * otherwise false.
     */
    public boolean isZero() {
        return signum() == 0;
    }

    /**
     * Returns true if the monetary value represented by this instance is greater than that
     * of the given other Coin, otherwise false.
     */
    public boolean isGreaterThan(Coin other) {
        return compareTo(other) > 0;
    }

    /**
     * Returns true if the monetary value represented by this instance is less than that
     * of the given other Coin, otherwise false.
     */
    public boolean isLessThan(Coin other) {
        return compareTo(other) < 0;
    }

    public Coin shiftLeft(final int n) {
        return new Coin(this.value << n);
    }

    public Coin shiftRight(final int n) {
        return new Coin(this.value >> n);
    }

    public int signum() {
        if (this.value == 0)
            return 0;
        return this.value < 0 ? -1 : 1;
    }

    public Coin negate() {
        return new Coin(-this.value);
    }

    /**
     * Returns the number of satoshis of this monetary value. It's deprecated in favour of accessing {@link #value}
     * directly.
     */
    public long longValue() {
        return this.value;
    }

    public String toText() {
    	
    	BigDecimal amount = new BigDecimal(value).divide(BigDecimal.valueOf(Coin.COIN.value));
    	
    	if(amount.longValue() == 0) {
    		return amount.toPlainString();
    	} else if(amount.doubleValue() % 100000000 == 0) {
    		return amount.longValue() / 100000000 + " 亿";
    	} else if(amount.doubleValue() % 10000000 == 0) {
    		return amount.longValue() / 10000000 + " 千万";
    	} else if(amount.doubleValue() % 10000 == 0) {
    		return amount.longValue() / 10000 + " 万";
    	} else {
    		return amount.toPlainString();
    	}
    }
    
    @Override
    public String toString() {
        return Long.toString(value);
    }
    
    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return this.value == ((Coin)o).value;
    }

    @Override
    public int hashCode() {
        return (int) this.value;
    }

    @Override
    public int compareTo(final Coin other) {
        return Long.compare(this.value, other.value);
    }
}
