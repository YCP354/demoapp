package com.dynamixsoftware.drv

import java.io.File
import java.util.Hashtable

/**
 * 原 DrvRuntime.java 的 Kotlin 完整还原版
 */
abstract class DrvRuntimeKt {

    // 对应原代码: private static final Hashtable f12492a
    // 缓存已加载的驱动实例
    companion object {
        private val loadedDrivers = Hashtable<String, DrvRuntimeKt>()

        // 对应原代码: static final class libhplip extends DrvRuntime
        internal class LibHplip : DrvRuntimeKt() {
            external override fun procDestroy(pid: Long)
            external override fun procExec(args: Array<String>, env: Array<String>, workDir: String, fds: IntArray): Long
            external override fun procWait(pid: Long): Int
        }

        /**
         * 对应原代码: public static ProcessSession ProcessSession(String[] strArr, String[] strArr2)
         * 这是主要的静态工厂方法，用于启动进程。
         *
         * @param commandArgs 命令参数 (strArr)
         * @param envVars 环境变量 (strArr2)
         */
        @JvmStatic
        fun exec(commandArgs: Array<String>, envVars: Array<String>?): ProcessSessionKt {
            var file = File(commandArgs[0])
            val libName = file.name.split("\\.".toRegex())[0]

            var runtime = loadedDrivers[libName]
            if (runtime == null) {
                loadNativeLibrary(file.absolutePath)
                runtime = createDriverInstance(libName)
                loadedDrivers[libName] = runtime
            }

            // 处理环境变量
            val safeEnvVars = envVars ?: emptyArray()
            val flattenedEnv = Array(safeEnvVars.size * 2) { "" }
            for (i in safeEnvVars.indices) {
                val split = safeEnvVars[i].split("=".toRegex()).toTypedArray()
                val idx = i * 2
                flattenedEnv[idx] = split[0]
                flattenedEnv[idx + 1] = if (split.size > 1) split[1] else ""
            }

            val fileDescriptors = IntArray(3) // [stdin, stdout, stderr]

            // 确定工作目录
            if (file.parentFile != null) {
                file = file.parentFile
            }

            // 执行 Native 方法
            val pid = runtime.procExec(commandArgs, flattenedEnv, file.absolutePath, fileDescriptors)

            // 返回 ProcessSession (原代码中的 ProcessSession 类)
            return ProcessSessionKt(runtime, pid, fileDescriptors)
        }

        // 对应原代码: private static void b(String str)
        private fun loadNativeLibrary(path: String) {
            System.load(path)
        }

        // 对应原代码: private static DrvRuntime c(String str)
        private fun createDriverInstance(name: String): DrvRuntimeKt {
            return when (name) {
                "libgutenprint" -> LibGutenPrint()
                "libescpr" -> LibEscPr()
                "libhplip" -> LibHplip()
                "libsplix" -> LibSplix()
                else -> throw RuntimeException("unknown driver")
            }
        }
    }

    // Native 方法定义
    abstract fun procDestroy(pid: Long)
    abstract fun procExec(args: Array<String>, env: Array<String>, workDir: String, fds: IntArray): Long
    abstract fun procWait(pid: Long): Int

    // 对应原代码: static final class libescpr extends DrvRuntime
    internal class LibEscPr : DrvRuntimeKt() {
        override external fun procDestroy(pid: Long)
        override external fun procExec(args: Array<String>, env: Array<String>, workDir: String, fds: IntArray): Long
        override external fun procWait(pid: Long): Int
    }

    // 对应原代码: static final class libgutenprint extends DrvRuntime
    internal class LibGutenPrint : DrvRuntimeKt() {
        override external fun procDestroy(pid: Long)
        override external fun procExec(args: Array<String>, env: Array<String>, workDir: String, fds: IntArray): Long
        override external fun procWait(pid: Long): Int
    }

    // 对应原代码: static final class libsplix extends DrvRuntime
    internal class LibSplix : DrvRuntimeKt() {
        override external fun procDestroy(pid: Long)
        override external fun procExec(args: Array<String>, env: Array<String>, workDir: String, fds: IntArray): Long
        override external fun procWait(pid: Long): Int
    }
}