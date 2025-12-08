package com.dynamixsoftware.drv;

import java.io.File;
import java.util.Hashtable;

/* loaded from: classes.dex */
public abstract class DrvRuntime {

    /* renamed from: Status, reason: collision with root package name */
    private static final Hashtable f12492a = new Hashtable();

    static final class libescpr extends DrvRuntime {
        libescpr() {
        }

        @Override // com.dynamixsoftware.drv.DrvRuntime
        native void procDestroy(long j7);

        @Override // com.dynamixsoftware.drv.DrvRuntime
        native long procExec(String[] strArr, String[] strArr2, String str, int[] iArr);

        @Override // com.dynamixsoftware.drv.DrvRuntime
        native int procWait(long j7);
    }

    static final class libgutenprint extends DrvRuntime {
        libgutenprint() {
        }

        @Override // com.dynamixsoftware.drv.DrvRuntime
        native void procDestroy(long j7);

        @Override // com.dynamixsoftware.drv.DrvRuntime
        native long procExec(String[] strArr, String[] strArr2, String str, int[] iArr);

        @Override // com.dynamixsoftware.drv.DrvRuntime
        native int procWait(long j7);
    }

    static final class libhplip extends DrvRuntime {
        libhplip() {
        }

        @Override // com.dynamixsoftware.drv.DrvRuntime
        native void procDestroy(long j7);

        @Override // com.dynamixsoftware.drv.DrvRuntime
        native long procExec(String[] strArr, String[] strArr2, String str, int[] iArr);

        @Override // com.dynamixsoftware.drv.DrvRuntime
        native int procWait(long j7);
    }

    static final class libsplix extends DrvRuntime {
        libsplix() {
        }

        @Override // com.dynamixsoftware.drv.DrvRuntime
        native void procDestroy(long j7);

        @Override // com.dynamixsoftware.drv.DrvRuntime
        native long procExec(String[] strArr, String[] strArr2, String str, int[] iArr);

        @Override // com.dynamixsoftware.drv.DrvRuntime
        native int procWait(long j7);
    }

    public static ProcessSession a(String[] strArr, String[] strArr2) {
        File file = new File(strArr[0]);
        String str = file.getName().split("\\.")[0];
        Hashtable hashtable = f12492a;
        DrvRuntime drvRuntimeC = (DrvRuntime) hashtable.get(str);
        if (drvRuntimeC == null) {
            b(file.getAbsolutePath());
            drvRuntimeC = c(str);
            hashtable.put(str, drvRuntimeC);
        }
        if (strArr2 == null) {
            strArr2 = new String[0];
        }
        String[] strArr3 = new String[strArr2.length * 2];
        for (int i7 = 0; i7 < strArr2.length; i7++) {
            String[] strArrSplit = strArr2[i7].split("=");
            int i8 = i7 * 2;
            strArr3[i8] = strArrSplit[0];
            strArr3[i8 + 1] = strArrSplit.length > 1 ? strArrSplit[1] : "";
        }
        int[] iArr = new int[3];
        if (file.getParentFile() != null) {
            file = file.getParentFile();
        }
        return new ProcessSession(drvRuntimeC, drvRuntimeC.procExec(strArr, strArr3, file.getAbsolutePath(), iArr), iArr);
    }

    private static void b(String str) {
        System.load(str);
    }

    private static DrvRuntime c(String str) {
        str.getClass();
        switch (str) {
            case "libgutenprint":
                return new libgutenprint();
            case "libescpr":
                return new libescpr();
            case "libhplip":
                return new libhplip();
            case "libsplix":
                return new libsplix();
            default:
                throw new RuntimeException("unknown driver");
        }
    }

    abstract void procDestroy(long j7);

    abstract long procExec(String[] strArr, String[] strArr2, String str, int[] iArr);

    abstract int procWait(long j7);
}
