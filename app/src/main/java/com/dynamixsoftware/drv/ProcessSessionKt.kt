package com.dynamixsoftware.drv

import android.os.ParcelFileDescriptor
import java.io.*

/**
 * 原 ProcessSession.java 的 Kotlin 完整还原版
 * 负责管理 Native 进程的 I/O 和生命周期
 */
class ProcessSessionKt(
    // 对应 f12493a
    private val runtime: DrvRuntime,
    // 对应 f12494b
    private val pid: Long,
    fds: IntArray
) {

    // 对应 f12495c: DataBean (Write to process STDIN)
    private val stdIn: OutputStream
    // 对应 f12496d: MyThread (Read from process STDOUT)
    private val stdOut: InputStream
    // 对应 f12497e: e (Read from process STDERR)
    private val stdErr: InputStream

    // 对应 f12498f: f (传输线程)
    private var transferThread: Thread? = null

    // 对应 f12499g: g (头部缓冲区 8KB)
    private val headBuffer = ByteArray(8192)

    // 对应 f12500h: h (当前缓冲区使用长度)
    private var headBufferLength = 0

    // 对应 f12501i: StatusEnum (是否正在读取)
    private var isReading = false

    // 对应 f12502j: j (最后读取时间戳)
    private var lastReadTime: Long = 0

    // 对应 f12503k: k (保存的异常)
    private var savedException: Exception? = null

    // 对应 f12504l: l (错误日志构建器)
    private val logBuilder = StringBuilder()

    init {
        // 初始化流，使用内部辅助类来确保 FD 被关闭
        val pfd0 = ParcelFileDescriptor.adoptFd(fds[0])
        this.stdIn = AutoCloseParcelOutputStream(pfd0.fileDescriptor, pfd0)

        val pfd1 = ParcelFileDescriptor.adoptFd(fds[1])
        this.stdOut = AutoCloseParcelInputStream(pfd1.fileDescriptor, pfd1)

        val pfd2 = ParcelFileDescriptor.adoptFd(fds[2])
        this.stdErr = AutoCloseParcelInputStream(pfd2.fileDescriptor, pfd2)
    }

    /**
     * 对应 j(OutputStream outputStream, boolean z6, boolean z7)
     * 启动流传输
     */
    fun startStreams(targetOutput: OutputStream, readFromStderr: Boolean, enableHeaderCheck: Boolean) {
        val thread = StreamTransferThread(targetOutput, readFromStderr, enableHeaderCheck)
        this.transferThread = thread
        thread.start()

        // 如果不从 Stderr 读取（即主要读取 Stdout），则启动另一个线程收集 Stderr 日志
        if (!readFromStderr) {
            ErrorCollectorThread().start()
        }
    }

    // 对应 k()
    fun destroyProcess() {
        this.runtime.procDestroy(this.pid)
    }

    // 对应 l()
    fun getErrorLog(): String {
        return this.logBuilder.toString()
    }

    // 对应 m()
    fun getException(): Exception? {
        return this.savedException
    }

    // 对应 n()
    fun getStdOut(): InputStream {
        return this.stdOut
    }

    // 对应 o()
    fun getStdIn(): OutputStream {
        return this.stdIn
    }

    // 对应 p() - 返回缓冲区中的头部数据字符串
    fun getHeadOutputString(): String {
        return String(this.headBuffer, 0, this.headBufferLength)
    }

    // 对应 q()
    fun waitForProcess(): Int {
        return this.runtime.procWait(this.pid)
    }

    // 对应 r()
    fun waitForThread() {
        while (this.transferThread?.isAlive == true) {
            Thread.yield()
        }
    }

    // 对应 s() - 带超时的等待逻辑
    fun waitForThreadWithTimeout() {
        while (this.transferThread?.isAlive == true) {
            // 如果正在读取且距离上次读取超过 10ms，则退出等待（防止死锁？）
            if (this.isReading && System.currentTimeMillis() - this.lastReadTime > 10) {
                return
            } else {
                Thread.yield()
            }
        }
    }

    /* ================= 内部类还原 ================= */

    // 对应原代码: class C0226a
    private class AutoCloseParcelOutputStream(fd: FileDescriptor, private val pfd: ParcelFileDescriptor) : FileOutputStream(fd) {
        override fun close() {
            try {
                super.close()
            } finally {
                pfd.close()
            }
        }
    }

    // 对应原代码: class b
    private class AutoCloseParcelInputStream(fd: FileDescriptor, private val pfd: ParcelFileDescriptor) : FileInputStream(fd) {
        override fun close() {
            try {
                super.close()
            } finally {
                pfd.close()
            }
        }
    }

    // 对应原代码: class c (虽然逻辑和 b 一样，但原代码就是分开的，这里保持一致)
    // 实际上原代码 c 用于 stdErr
    private class AutoCloseParcelErrorStream(fd: FileDescriptor, private val pfd: ParcelFileDescriptor) : FileInputStream(fd) {
        override fun close() {
            try {
                super.close()
            } finally {
                pfd.close()
            }
        }
    }

    // 对应原代码: class MyThread (核心传输线程)
    private inner class StreamTransferThread(
        private val targetStream: OutputStream,
        private val readFromStderr: Boolean, // 对应 f12513c
        private val enableHeaderCheck: Boolean // 对应 f12514d
    ) : Thread() {

        // 对应 f12511a: 魔法头，用于检测数据流类型
        // [0]: ESC [ K ... (可能是打印机控制码)
        // [1]: "<?xml "
        private val magicHeaders = arrayOf(
            byteArrayOf(27, 91, 75, 2, 0, 0),
            byteArrayOf(60, 63, 120, 109, 108, 32)
        )

        // 对应 MyThread 类中的 method ProcessSession()
        // 这是一个非常特殊的 Flush 逻辑：它会扫描缓冲区，只有匹配 magicHeaders 时才写入
        @Throws(IOException::class)
        private fun flushBuffer() {
            if (this.enableHeaderCheck) {
                var processedIndex = 0
                // 遍历当前头部缓冲区的所有数据
                for (i in 0 until this@ProcessSessionKt.headBufferLength) {
                    // 如果找到了匹配的头
                    if (processedIndex < i && checkHeaders(this@ProcessSessionKt.headBuffer, i, this@ProcessSessionKt.headBufferLength, magicHeaders)) {
                        // 写入从上次处理位置到当前位置的数据
                        this.targetStream.write(this@ProcessSessionKt.headBuffer, processedIndex, i - processedIndex)
                        this.targetStream.flush()
                        processedIndex = i
                    }
                }
                // 写入剩余部分
                if (processedIndex < this@ProcessSessionKt.headBufferLength) {
                    this.targetStream.write(this@ProcessSessionKt.headBuffer, processedIndex, this@ProcessSessionKt.headBufferLength - processedIndex)
                }
            } else {
                // 如果没开启检查，直接写入所有缓冲区数据
                this.targetStream.write(this@ProcessSessionKt.headBuffer, 0, this@ProcessSessionKt.headBufferLength)
            }
            // 重置缓冲区长度
            this@ProcessSessionKt.headBufferLength = 0
        }

        // 对应 MyThread 类中的 method b()
        private fun matchBytes(src: ByteArray, offset: Int, max: Int, target: ByteArray): Boolean {
            if (target.size + offset > max) {
                return true
            }
            for (i in target.indices) {
                if (src[offset + i] != target[i]) {
                    return false
                }
            }
            return true
        }

        // 对应 MyThread 类中的 method c()
        private fun checkHeaders(src: ByteArray, offset: Int, max: Int, targets: Array<ByteArray>): Boolean {
            for (target in targets) {
                if (matchBytes(src, offset, max, target)) {
                    return true
                }
            }
            return false
        }

        // 对应 MyThread 类中的 method MyThread()
        // 读取数据并更新状态
        @Throws(IOException::class)
        private fun readData(buffer: ByteArray): Int {
            synchronized(this) {
                this@ProcessSessionKt.isReading = true
                this@ProcessSessionKt.lastReadTime = System.currentTimeMillis()
            }
            // 根据标志位决定读取 StdErr 还是 StdOut
            val stream = if (this.readFromStderr) this@ProcessSessionKt.stdErr else this@ProcessSessionKt.stdOut
            val bytesRead = stream.read(buffer)

            synchronized(this) {
                this@ProcessSessionKt.isReading = false
            }
            return bytesRead
        }

        override fun run() {
            val tempBuffer = ByteArray(4096)
            while (true) {
                try {
                    val bytesRead = readData(tempBuffer)
                    if (bytesRead == -1) {
                        flushBuffer()
                        this.targetStream.flush()
                        return
                    } else {
                        // 如果加上新读取的数据会超过头部缓冲区大小(8192)，则先 flush
                        if (this@ProcessSessionKt.headBufferLength + bytesRead > this@ProcessSessionKt.headBuffer.size) {
                            flushBuffer()
                        }
                        // 将新数据追加到头部缓冲区
                        System.arraycopy(tempBuffer, 0, this@ProcessSessionKt.headBuffer, this@ProcessSessionKt.headBufferLength, bytesRead)
                        this@ProcessSessionKt.headBufferLength += bytesRead
                    }
                } catch (e: Exception) {
                    this@ProcessSessionKt.savedException = e
                    this@ProcessSessionKt.destroyProcess()
                    return
                }
            }
        }
    }

    // 对应原代码: class e (错误流收集线程)
    private inner class ErrorCollectorThread : Thread() {
        override fun run() {
            try {
                val reader = BufferedReader(InputStreamReader(this@ProcessSessionKt.stdErr))
                while (true) {
                    val line = reader.readLine() ?: return
                    val sb = this@ProcessSessionKt.logBuilder
                    sb.append(line)
                    sb.append("\n")
                }
            } catch (ignored: Exception) {
            }
        }
    }
}