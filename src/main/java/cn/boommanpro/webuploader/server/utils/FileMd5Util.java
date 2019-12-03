package cn.boommanpro.webuploader.server.utils;

import java.lang.reflect.Method;
import java.nio.MappedByteBuffer;
import java.security.AccessController;
import java.security.PrivilegedAction;

import lombok.extern.slf4j.Slf4j;

/**
 * 文件md5值
 *
 * @author wangqimeng
 * @date 2019/12/02 17:14
 */
@Slf4j
public class FileMd5Util {

    private FileMd5Util() {
    }
    /**
     * 在MappedByteBuffer释放后再对它进行读操作的话就会引发jvm crash，在并发情况下很容易发生
     * 正在释放时另一个线程正开始读取，于是crash就发生了。所以为了系统稳定性释放前一般需要检 查是否还有线程在读或写
     *
     * @param mappedByteBuffer mappedByteBuffer
     */
    public static void freedMappedByteBuffer(final MappedByteBuffer mappedByteBuffer) {
        try {
            if (mappedByteBuffer == null) {
                return;
            }

            mappedByteBuffer.force();
            AccessController.doPrivileged(new PrivilegedAction<Object>() {
                @Override
                public Object run() {
                    try {
                        Method getCleanerMethod = mappedByteBuffer.getClass().getMethod("cleaner", new Class[0]);
                        getCleanerMethod.setAccessible(true);
                        sun.misc.Cleaner cleaner = (sun.misc.Cleaner) getCleanerMethod.invoke(mappedByteBuffer,
                                new Object[0]);
                        cleaner.clean();
                    } catch (Exception e) {
                        log.error("clean MappedByteBuffer error!!!", e);
                    }
                    log.debug("clean MappedByteBuffer completed!!!");
                    return null;
                }
            });

        } catch (Exception e) {
            log.error("释放空间出现异常:", e);
        }
    }
}
