package com.distribute.remoting.utils.Strategy;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;


/**
 * @author liushaoming
 * <p>
 * Utilities for byte process
 */
public final class ByteUtils {
    public static final String DEFAULT_CHARSET_NAME = "utf-8";
    public static final Charset DEFAULT_CHARSET = Charset
            .forName(DEFAULT_CHARSET_NAME);
    /**
     * if it is testing,check key argument even if use binary protocol. The user
     * must never change this value at all.
     */
    public static boolean testing;

    private ByteUtils() {
    }

    public static boolean isNumber(String string) {
        if (string == null || string.isEmpty()) {
            return false;
        }
        int i = 0;
        if (string.charAt(0) == '-') {
            if (string.length() > 1) {
                i++;
            } else {
                return false;
            }
        }
        for (; i < string.length(); i++) {
            if (!Character.isDigit(string.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static final byte[] getBytes(String k) {
        if (k == null || k.length() == 0) {
            throw new IllegalArgumentException("Key must not be blank");
        }
        try {
            return k.getBytes(DEFAULT_CHARSET_NAME);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }


    private static int maxKeyLength = 250;


    public static final int normalizeCapacity(int requestedCapacity) {
        switch (requestedCapacity) {
            case 0:
            case 1 << 0:
            case 1 << 1:
            case 1 << 2:
            case 1 << 3:
            case 1 << 4:
            case 1 << 5:
            case 1 << 6:
            case 1 << 7:
            case 1 << 8:
            case 1 << 9:
            case 1 << 10:
            case 1 << 11:
            case 1 << 12:
            case 1 << 13:
            case 1 << 14:
            case 1 << 15:
            case 1 << 16:
            case 1 << 17:
            case 1 << 18:
            case 1 << 19:
            case 1 << 21:
            case 1 << 22:
            case 1 << 23:
            case 1 << 24:
            case 1 << 25:
            case 1 << 26:
            case 1 << 27:
            case 1 << 28:
            case 1 << 29:
            case 1 << 30:
            case Integer.MAX_VALUE:
                return requestedCapacity;
        }

        int newCapacity = 1;
        while (newCapacity < requestedCapacity) {
            newCapacity <<= 1;
            if (newCapacity < 0) {
                return Integer.MAX_VALUE;
            }
        }
        return newCapacity;
    }

    public static final boolean stepBuffer(ByteBuffer buffer, int remaining) {
        if (buffer.remaining() >= remaining) {
            buffer.position(buffer.position() + remaining);
            return true;
        } else {
            return false;
        }
    }

    public static String getString(byte[] bytes) {
        try {
            return new String(bytes, DEFAULT_CHARSET_NAME);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public static void byte2hex(byte b, StringBuffer buf) {
        char[] hexChars = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                'A', 'B', 'C', 'D', 'E', 'F'};
        int high = ((b & 0xf0) >> 4);
        int low = (b & 0x0f);
        buf.append(hexChars[high]);
        buf.append(hexChars[low]);
    }

    public static void int2hex(int a, StringBuffer str) {
        str.append(Integer.toHexString(a));
    }

    public static void short2hex(int a, StringBuffer str) {
        str.append(Integer.toHexString(a));
    }

    public static void getBytes(long i, int index, byte[] buf) {
        long q;
        int r;
        int pos = index;
        byte sign = 0;

        if (i < 0) {
            sign = '-';
            i = -i;
        }

// Get 2 digits/iteration using longs until quotient fits into an int
        while (i > Integer.MAX_VALUE) {
            q = i / 100;
// really: r = i - (q * 100);
            r = (int) (i - ((q << 6) + (q << 5) + (q << 2)));
            i = q;
            buf[--pos] = DigitOnes[r];
            buf[--pos] = DigitTens[r];
        }

// Get 2 digits/iteration using ints
        int q2;
        int i2 = (int) i;
        while (i2 >= 65536) {
            q2 = i2 / 100;
// really: r = i2 - (q * 100);
            r = i2 - ((q2 << 6) + (q2 << 5) + (q2 << 2));
            i2 = q2;
            buf[--pos] = DigitOnes[r];
            buf[--pos] = DigitTens[r];
        }

// Fall thru to fast mode for smaller numbers
// assert(i2 <= 65536, i2);
        for (; ; ) {
            q2 = (i2 * 52429) >>> (16 + 3);
            r = i2 - ((q2 << 3) + (q2 << 1)); // r = i2-(q2*10) ...
            buf[--pos] = digits[r];
            i2 = q2;
            if (i2 == 0)
                break;
        }
        if (sign != 0) {
            buf[--pos] = sign;
        }
    }

    /**
     * Places characters representing the integer i into the character array
     * buf. The characters are placed into the buffer backwards starting with
     * the least significant digit at the specified index (exclusive), and
     * working backwards from there.
     * <p/>
     * Will fail if i == Integer.MIN_VALUE
     */
    static void getBytes(int i, int index, byte[] buf) {
        int q, r;
        int pos = index;
        byte sign = 0;

        if (i < 0) {
            sign = '-';
            i = -i;
        }

// Generate two digits per iteration
        while (i >= 65536) {
            q = i / 100;
// really: r = i - (q * 100);
            r = i - ((q << 6) + (q << 5) + (q << 2));
            i = q;
            buf[--pos] = DigitOnes[r];
            buf[--pos] = DigitTens[r];
        }

// Fall thru to fast mode for smaller numbers
// assert(i <= 65536, i);
        for (; ; ) {
            q = (i * 52429) >>> (16 + 3);
            r = i - ((q << 3) + (q << 1)); // r = i-(q*10) ...
            buf[--pos] = digits[r];
            i = q;
            if (i == 0)
                break;
        }
        if (sign != 0) {
            buf[--pos] = sign;
        }
    }

    /**
     * All possible chars for representing a number as a String
     */
    final static byte[] digits = {'0', '1', '2', '3', '4', '5', '6', '7', '8',
            '9', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l',
            'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y',
            'z'};

    final static byte[] DigitTens = {'0', '0', '0', '0', '0', '0', '0', '0',
            '0', '0', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '2',
            '2', '2', '2', '2', '2', '2', '2', '2', '2', '3', '3', '3', '3',
            '3', '3', '3', '3', '3', '3', '4', '4', '4', '4', '4', '4', '4',
            '4', '4', '4', '5', '5', '5', '5', '5', '5', '5', '5', '5', '5',
            '6', '6', '6', '6', '6', '6', '6', '6', '6', '6', '7', '7', '7',
            '7', '7', '7', '7', '7', '7', '7', '8', '8', '8', '8', '8', '8',
            '8', '8', '8', '8', '9', '9', '9', '9', '9', '9', '9', '9', '9',
            '9',};

    final static byte[] DigitOnes = {'0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0',
            '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '1', '2', '3',
            '4', '5', '6', '7', '8', '9', '0', '1', '2', '3', '4', '5', '6',
            '7', '8', '9', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '1', '2',
            '3', '4', '5', '6', '7', '8', '9', '0', '1', '2', '3', '4', '5',
            '6', '7', '8', '9', '0', '1', '2', '3', '4', '5', '6', '7', '8',
            '9',};

    final static int[] sizeTable = {9, 99, 999, 9999, 99999, 999999, 9999999,
            99999999, 999999999, Integer.MAX_VALUE};

    // Requires positive x
    public static final int stringSize(int x) {
        for (int i = 0; ; i++)
            if (x <= sizeTable[i])
                return i + 1;
    }

    // Requires positive x
    public static final int stringSize(long x) {
        long p = 10;
        for (int i = 1; i < 19; i++) {
            if (x < p)
                return i;
            p = 10 * p;
        }
        return 19;
    }

    final static int[] byte_len_array = new int[256];

    static {
        for (int i = Byte.MIN_VALUE; i <= Byte.MAX_VALUE; ++i) {
            int size = (i < 0) ? stringSize(-i) + 1 : stringSize(i);
            byte_len_array[i & 0xFF] = size;
        }
    }
}