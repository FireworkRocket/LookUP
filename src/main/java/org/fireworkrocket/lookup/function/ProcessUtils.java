package org.fireworkrocket.lookup.function;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.IntByReference;

import java.util.Arrays;
import java.util.List;

public class ProcessUtils {

    /**
     * Kernel32 扩展接口，包含 SetProcessInformation 方法
     */
    public interface Kernel32Extended extends Kernel32 {
        Kernel32Extended INSTANCE = Native.load("kernel32", Kernel32Extended.class);

        /**
         * 设置进程信息
         * @param hProcess 进程句柄
         * @param ProcessInformationClass 进程信息类
         * @param ProcessInformation 进程信息指针
         * @param ProcessInformationSize 进程信息大小
         * @return 是否成功
         */
        boolean SetProcessInformation(WinNT.HANDLE hProcess, int ProcessInformationClass, Pointer ProcessInformation, int ProcessInformationSize);
    }

    /**
     * 进程电源限制状态结构体
     */
    public static class PROCESS_POWER_THROTTLING_STATE extends Structure {
        public int Version;
        public int ControlMask;
        public int StateMask;

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("Version", "ControlMask", "StateMask");
        }
    }

    /**
     * 设置进程为可挂起状态
     * @param pid 进程 ID
     */
    public static void setProcessSuspendable(int pid) {
        WinNT.HANDLE hProcess = Kernel32.INSTANCE.OpenProcess(WinNT.PROCESS_SET_INFORMATION, false, pid);
        if (hProcess != null) {
            PROCESS_POWER_THROTTLING_STATE state = new PROCESS_POWER_THROTTLING_STATE();
            state.Version = 1;
            state.ControlMask = 0x1; // PROCESS_POWER_THROTTLING_EXECUTION_SPEED
            state.StateMask = 0x1; // 启用限制

            Kernel32Extended.INSTANCE.SetProcessInformation(hProcess, 0x3, state.getPointer(), state.size());
            Kernel32.INSTANCE.CloseHandle(hProcess);
        }
    }

    /**
     * 列出所有进程
     * @return 进程 ID 数组
     */
    public static int[] listProcesses() {
        int[] processIds = new int[1024];
        IntByReference pBytesReturned = new IntByReference();
        if (Psapi.INSTANCE.EnumProcesses(processIds, processIds.length * 4, pBytesReturned)) {
            int numProcesses = pBytesReturned.getValue() / 4;
            int[] result = new int[numProcesses];
            System.arraycopy(processIds, 0, result, 0, numProcesses);
            return result;
        }
        return new int[0];
    }

    /**
     * Psapi 接口，包含 EnumProcesses 方法
     */
    public interface Psapi extends com.sun.jna.Library {
        Psapi INSTANCE = Native.load("psapi", Psapi.class);

        /**
         * 枚举进程
         * @param lpidProcess 进程 ID 数组
         * @param cb 数组大小
         * @param lpcbNeeded 返回的字节数
         * @return 是否成功
         */
        boolean EnumProcesses(int[] lpidProcess, int cb, IntByReference lpcbNeeded);
    }
}