package org.fireworkrocket.lookup.exception;

import com.sun.jna.Platform;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinNT;
import com.sun.management.HotSpotDiagnosticMXBean;
import org.fireworkrocket.lookup.Main;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.lang.management.*;
import java.net.URI;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.fireworkrocket.lookup.Main.logFilename;

public class MemoryMonitor {
    private static final long MAX_MEMORY = Runtime.getRuntime().maxMemory();
    static File Dump = null;
    public static void init() {
        long usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        EventQueue.invokeLater(() -> {
            try {
                // 先捕获内存快照
                String heapDumpPath = "Logs/heapdump-"+System.currentTimeMillis()+".hprof";
                Dump = new File(generateHeapDump(heapDumpPath));
                // 再调用GC
                System.gc();
                // 显示警告
                showAlert(usedMemory);
            } catch (IOException e) {
                System.exit(1);
            }
        });
    }

    public static void showAlert(long usedMemory) {
        Frame frame = new Frame("Run out of memory (OOM)");
        frame.setSize(700, 400);
        frame.setBackground(Color.RED);
        frame.setLayout(new BorderLayout());

        // 创建菜单栏
        MenuBar menuBar = getMenuBar(frame);

        frame.setMenuBar(menuBar);

        Label label = new Label("Memory usage is too high! Used memory: " + (usedMemory / (1024 * 1024)) + " MB / " + (MAX_MEMORY / (1024 * 1024)) + " MB", Label.CENTER);
        label.setForeground(Color.YELLOW);
        label.setFont(new Font("Arial", Font.BOLD, 15));
        frame.add(label, BorderLayout.NORTH);

        // 将 textArea 声明为类的成员变量
        TextArea textArea = new TextArea(); // 初始化 textArea
        textArea.setEditable(false);
        frame.add(textArea, BorderLayout.CENTER);

        // 获取系统信息
        StringBuilder systemInfo = new StringBuilder();
        systemInfo.append("很抱歉，LookUp已将内存耗尽，如果你想报告此Bug，请在复制此文本框内所有内容后到https://github.com/FireworkRocket/LookUP/issues提出一个新的问题\n");
        systemInfo.append("临时解决方案：将JVM参数-Xmx设置为更高的值，例如-Xmx4G（不推荐）\n");
        systemInfo.append("Java 运行时供应商: ").append(System.getProperty("java.vendor")).append("\n");
        systemInfo.append("Java 运行时版本: ").append(System.getProperty("java.version")).append("\n");
        systemInfo.append("Java 虚拟机规范版本: ").append(System.getProperty("java.vm.specification.version")).append("\n");

        systemInfo.append("CPU 核心数: ").append(Runtime.getRuntime().availableProcessors()).append("\n");
        systemInfo.append("最大内存: ").append(MAX_MEMORY).append(" bytes\n");
        systemInfo.append("已用内存: ").append(usedMemory).append(" bytes\n");

        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapMemoryUsage = memoryMXBean.getHeapMemoryUsage();
        systemInfo.append("堆内存使用情况: ").append(heapMemoryUsage).append("\n");

        MemoryUsage nonHeapMemoryUsage = memoryMXBean.getNonHeapMemoryUsage();
        systemInfo.append("非堆内存使用情况: ").append(nonHeapMemoryUsage).append("\n");

        com.sun.management.OperatingSystemMXBean osBean = (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        systemInfo.append("操作系统: ").append(osBean.getName()).append(" ").append(osBean.getVersion()).append("\n");
        systemInfo.append("系统负载: ").append(osBean.getCpuLoad()).append("\n");
        systemInfo.append("系统架构: ").append(osBean.getArch()).append("\n");
        systemInfo.append("可用处理器: ").append(osBean.getAvailableProcessors()).append("\n");
        systemInfo.append("总物理内存: ").append(osBean.getTotalMemorySize()).append(" bytes\n");
        systemInfo.append("可用物理内存: ").append(osBean.getFreeMemorySize()).append(" bytes\n");
        systemInfo.append("总交换空间: ").append(osBean.getTotalSwapSpaceSize()).append(" bytes\n");
        systemInfo.append("可用交换空间: ").append(osBean.getFreeSwapSpaceSize()).append(" bytes\n");

        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        systemInfo.append("JVM 启动时间: ").append(runtimeMXBean.getStartTime()).append("\n");
        systemInfo.append("JVM 正常运行时间: ").append(runtimeMXBean.getUptime()).append(" ms\n");
        systemInfo.append("JVM 名称: ").append(runtimeMXBean.getVmName()).append("\n");
        systemInfo.append("JVM 版本: ").append(runtimeMXBean.getVmVersion()).append("\n");
        systemInfo.append("JVM 供应商: ").append(runtimeMXBean.getVmVendor()).append("\n");
        systemInfo.append("JVM 参数: ").append(runtimeMXBean.getInputArguments()).append("\n");

        // 获取 JVM 详细状态信息
        CompilationMXBean compilationMXBean = ManagementFactory.getCompilationMXBean();
        systemInfo.append("JIT 编译器名称: ").append(compilationMXBean.getName()).append("\n");
        systemInfo.append("总编译时间: ").append(compilationMXBean.getTotalCompilationTime()).append(" ms\n");

        ClassLoadingMXBean classLoadingMXBean = ManagementFactory.getClassLoadingMXBean();
        systemInfo.append("已加载类数量: ").append(classLoadingMXBean.getLoadedClassCount()).append("\n");
        systemInfo.append("总加载类数量: ").append(classLoadingMXBean.getTotalLoadedClassCount()).append("\n");
        systemInfo.append("已卸载类数量: ").append(classLoadingMXBean.getUnloadedClassCount()).append("\n");

        for (GarbageCollectorMXBean gcMXBean : ManagementFactory.getGarbageCollectorMXBeans()) {
            systemInfo.append("垃圾收集器名称: ").append(gcMXBean.getName()).append("\n");
            systemInfo.append("垃圾收集次数: ").append(gcMXBean.getCollectionCount()).append("\n");
            systemInfo.append("垃圾收集总时间: ").append(gcMXBean.getCollectionTime()).append(" ms\n");
        }

        // 使用JNA获取更多系统信息
        if (Platform.isWindows()) {
            WinNT.MEMORYSTATUSEX memoryStatus = new WinNT.MEMORYSTATUSEX();
            if (Kernel32.INSTANCE.GlobalMemoryStatusEx(memoryStatus)) {
                systemInfo.append("物理内存总量: ").append(memoryStatus.ullTotalPhys.longValue()).append(" bytes\n");
                systemInfo.append("可用物理内存: ").append(memoryStatus.ullAvailPhys.longValue()).append(" bytes\n");
            }

            WinBase.FILETIME idleTime = new WinBase.FILETIME();
            WinBase.FILETIME kernelTime = new WinBase.FILETIME();
            WinBase.FILETIME userTime = new WinBase.FILETIME();
            if (Kernel32.INSTANCE.GetSystemTimes(idleTime, kernelTime, userTime)) {
                systemInfo.append("系统空闲时间: ").append(idleTime.toDWordLong().longValue()).append(" ms\n");
                systemInfo.append("内核空闲时间: ").append(kernelTime.toDWordLong().longValue()).append(" ms\n");
                systemInfo.append("用户时间: ").append(userTime.toDWordLong().longValue()).append(" ms\n");
            }
        }

        // 获取所有线程的堆栈跟踪信息
        Map<Thread, StackTraceElement[]> allStackTraces = Thread.getAllStackTraces();
        for (Map.Entry<Thread, StackTraceElement[]> entry : allStackTraces.entrySet()) {
            Thread thread = entry.getKey();
            StackTraceElement[] stackTraceElements = entry.getValue();
            systemInfo.append("线程: ").append(thread.getName()).append("\n");
            for (StackTraceElement element : stackTraceElements) {
                systemInfo.append("\tat ").append(element).append("\n");
            }
        }

        // 添加类路径和行号信息
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        if (stackTrace.length > 2) {
            StackTraceElement caller = stackTrace[2];
            systemInfo.append("坏消息从哪个地方蹦出来了？ ").append(caller.getClassName()).append(".").append(caller.getMethodName())
                    .append("(").append(caller.getFileName()).append(":").append(caller.getLineNumber()).append(")\n");
        }

        textArea.setText(systemInfo.toString());

        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                frame.dispose();
                System.exit(1); // 退出整个程序
            }
        });

        frame.setVisible(true);

        // 将系统信息写入日志文件
        try (PrintWriter writer = new PrintWriter(new FileWriter(logFilename, true))) {
            writer.println(systemInfo);
        } catch (IOException ex) {
            System.exit(1);
        }
    }

    private static MenuBar getMenuBar(Frame frame) {
        MenuBar menuBar = new MenuBar();
        Menu fileMenu = getMenu(frame);

        Menu reportMenu = new Menu("Report");
        MenuItem reportItem = new MenuItem("Open GitHub issues");
        reportItem.addActionListener(_ -> {
            try {
                Desktop.getDesktop().browse(new URI("https://github.com/FireworkRocket/LookUP/issues"));
            } catch (Exception ex) {
                System.exit(1);
            }
        });
        reportMenu.add(reportItem);

        Menu restartMenu = new Menu("Restart");
        MenuItem restartItem = getMenuItem(frame);
        restartMenu.add(restartItem);

        menuBar.add(fileMenu);
        menuBar.add(reportMenu);
        menuBar.add(restartMenu);
        return menuBar;
    }

    private static MenuItem getMenuItem(Frame frame) {
        MenuItem restartItem = new MenuItem("Restart");
        restartItem.addActionListener(_ -> {
            try {
                System.gc();
                Main.main(new String[0]);
                frame.dispose();
            } catch (Exception ex) {
                System.exit(1);
            }
        });
        return restartItem;
    }

    private static Menu getMenu(Frame frame) {
        Menu fileMenu = new Menu("File");
        MenuItem saveItem = new MenuItem("Save to ...");
        saveItem.addActionListener(_ -> {
            FileDialog fileDialog = new FileDialog(frame, "Save to ...", FileDialog.SAVE);
            fileDialog.setVisible(true);
            String directory = fileDialog.getDirectory();
            String file = fileDialog.getFile();
            if (directory != null && file != null) {
                try {
                    File zipFile = new File(directory, file.endsWith(".zip") ? file : file + ".zip");
                    try (FileOutputStream fos = new FileOutputStream(zipFile);
                         ZipOutputStream zos = new ZipOutputStream(fos)) {

                        addToZipFile(new File(logFilename), zos);
                        addToZipFile(Dump, zos);

                    }
                } catch (IOException ex) {
                    System.exit(1);
                }
            }
        });
        fileMenu.add(saveItem);
        return fileMenu;
    }

    private static void addToZipFile(File file, ZipOutputStream zos) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            ZipEntry zipEntry = new ZipEntry(file.getName());
            zos.putNextEntry(zipEntry);

            byte[] bytes = new byte[1024];
            int length;
            while ((length = fis.read(bytes)) >= 0) {
                zos.write(bytes, 0, length);
            }
            zos.closeEntry();
        }
    }

    private static String generateHeapDump(String filePath) throws IOException {
        HotSpotDiagnosticMXBean mxBean = ManagementFactory.newPlatformMXBeanProxy(
                ManagementFactory.getPlatformMBeanServer(),
                "com.sun.management:type=HotSpotDiagnostic",
                HotSpotDiagnosticMXBean.class);
        mxBean.dumpHeap(filePath, false);
        return filePath;
    }

}