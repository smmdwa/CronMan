package com.distribute.executor.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FileUtil {
    public static long copyTo(InputStream inputStream, OutputStream outputStream, byte[] buffer) throws IOException {
        try {
            long total = 0;
            for (;;) {
                System.out.println("input:"+inputStream);
                int res = inputStream.read(buffer);
                if (res == -1) {
                    break;
                }
                if (res > 0) {
                    total += res;
                    if (outputStream != null) {
                        outputStream.write(buffer, 0, res);
                    }
                }
            }
            outputStream.flush();
            //out = null;
            inputStream.close();
            inputStream = null;
            return total;
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }

}
