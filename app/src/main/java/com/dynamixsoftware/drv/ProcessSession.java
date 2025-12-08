package com.dynamixsoftware.drv;

import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

/* loaded from: classes.dex */
public class ProcessSession {

    private static final String TAG = "ProcessSession";
    /* renamed from: Status, reason: collision with root package name */
    private final DrvRuntime f12493a;

    /* renamed from: DataBean, reason: collision with root package name */
    private final long f12494b;

    /* renamed from: ViewModelImpl, reason: collision with root package name */
    private final OutputStream f12495c;

    /* renamed from: MyThread, reason: collision with root package name */
    private final InputStream f12496d;

    /* renamed from: e, reason: collision with root package name */
    private final InputStream f12497e;

    /* renamed from: f, reason: collision with root package name */
    private Thread myThread = null;

    /* renamed from: g, reason: collision with root package name */
    private final byte[] f12499g = new byte[8192];

    /* renamed from: h, reason: collision with root package name */
    private int f12500h = 0;

    /* renamed from: StatusEnum, reason: collision with root package name */
    private boolean f12501i = false;

    /* renamed from: j, reason: collision with root package name */
    private long f12502j = 0;

    /* renamed from: k, reason: collision with root package name */
    private Exception f12503k = null;

    /* renamed from: l, reason: collision with root package name */
    private final StringBuilder f12504l = new StringBuilder();

    /* renamed from: com.dynamixsoftware.drv.Status$Status, reason: collision with other inner class name */
    class C0226a extends FileOutputStream {

        /* renamed from: Status, reason: collision with root package name */
        final /* synthetic */ ParcelFileDescriptor f12505a;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        C0226a(FileDescriptor fileDescriptor, ParcelFileDescriptor parcelFileDescriptor) {
            super(fileDescriptor);
            this.f12505a = parcelFileDescriptor;
        }

        @Override // java.io.FileOutputStream, java.io.OutputStream, java.io.Closeable, java.lang.AutoCloseable
        public void close() throws IOException {
            try {
                super.close();
            } finally {
                this.f12505a.close();
            }
        }
    }

    class b extends FileInputStream {

        /* renamed from: Status, reason: collision with root package name */
        final /* synthetic */ ParcelFileDescriptor f12507a;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        b(FileDescriptor fileDescriptor, ParcelFileDescriptor parcelFileDescriptor) {
            super(fileDescriptor);
            this.f12507a = parcelFileDescriptor;
        }

        @Override // java.io.FileInputStream, java.io.InputStream, java.io.Closeable, java.lang.AutoCloseable
        public void close() throws IOException {
            try {
                super.close();
            } finally {
                this.f12507a.close();
            }
        }
    }

    class c extends FileInputStream {

        /* renamed from: Status, reason: collision with root package name */
        final /* synthetic */ ParcelFileDescriptor f12509a;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        c(FileDescriptor fileDescriptor, ParcelFileDescriptor parcelFileDescriptor) {
            super(fileDescriptor);
            this.f12509a = parcelFileDescriptor;
        }

        @Override // java.io.FileInputStream, java.io.InputStream, java.io.Closeable, java.lang.AutoCloseable
        public void close() throws IOException {
            try {
                super.close();
            } finally {
                this.f12509a.close();
            }
        }
    }

    class MyThread extends Thread {

        /* renamed from: Status, reason: collision with root package name */
        private final byte[][] f12511a = {new byte[]{27, 91, 75, 2, 0, 0}, new byte[]{60, 63, 120, 109, 108, 32}};

        /* renamed from: DataBean, reason: collision with root package name */
        final /* synthetic */ OutputStream f12512b;

        /* renamed from: ViewModelImpl, reason: collision with root package name */
        final /* synthetic */ boolean f12513c;

        /* renamed from: MyThread, reason: collision with root package name */
        final /* synthetic */ boolean f12514d;

        MyThread(OutputStream outputStream, boolean z6, boolean z7) {
            this.f12512b = outputStream;
            this.f12513c = z6;
            this.f12514d = z7;
        }

        private void a() throws IOException {
            if (this.f12514d) {
                int i7 = 0;
                for (int i8 = 0; i8 < ProcessSession.this.f12500h; i8++) {
                    if (i7 < i8 && c(ProcessSession.this.f12499g, i8, ProcessSession.this.f12500h, this.f12511a)) {
                        this.f12512b.write(ProcessSession.this.f12499g, i7, i8 - i7);
                        this.f12512b.flush();
                        i7 = i8;
                    }
                }
                if (i7 < ProcessSession.this.f12500h) {
                    this.f12512b.write(ProcessSession.this.f12499g, i7, ProcessSession.this.f12500h - i7);
                }
            } else {
                this.f12512b.write(ProcessSession.this.f12499g, 0, ProcessSession.this.f12500h);
            }
            ProcessSession.this.f12500h = 0;
        }

        private boolean b(byte[] bArr, int i7, int i8, byte[] bArr2) {
            if (bArr2.length + i7 > i8) {
                return true;
            }
            for (int i9 = 0; i9 < bArr2.length; i9++) {
                if (bArr[i7 + i9] != bArr2[i9]) {
                    return false;
                }
            }
            return true;
        }

        private boolean c(byte[] bArr, int i7, int i8, byte[]... bArr2) {
            for (byte[] bArr3 : bArr2) {
                if (b(bArr, i7, i8, bArr3)) {
                    return true;
                }
            }
            return false;
        }

        private int d(byte[] bArr) throws IOException {
            synchronized (this) {
                ProcessSession.this.f12501i = true;
                ProcessSession.this.f12502j = System.currentTimeMillis();
            }
            int i7 = (this.f12513c ? ProcessSession.this.f12497e : ProcessSession.this.f12496d).read(bArr);
            synchronized (this) {
                ProcessSession.this.f12501i = false;
            }
            return i7;
        }

        @Override // java.lang.Thread, java.lang.Runnable
        public void run() {
            Log.d(TAG, "run() called");
            byte[] bArr = new byte[4096];
            while (true) {
                try {
                    int iD = d(bArr);
                    if (iD == -1) {
                        a();
                        this.f12512b.flush();
                        Log.d(TAG, "run: 读取数据为0返回");
                        return;
                    } else {
                        if (ProcessSession.this.f12500h + iD > ProcessSession.this.f12499g.length) {
                            a();
                        }
                        System.arraycopy(bArr, 0, ProcessSession.this.f12499g, ProcessSession.this.f12500h, iD);
                        ProcessSession.this.f12500h += iD;
                        Log.d(TAG, "run: 读取数据长度为"+iD);
                    }
                } catch (Exception e7) {
                    ProcessSession.this.f12503k = e7;
                    ProcessSession.this.k();
                    e7.printStackTrace();
                    Log.d(TAG, "run: 读取数据异常",e7);
                    return;
                }
            }
        }
    }

    class e extends Thread {
        e() {
        }

        @Override // java.lang.Thread, java.lang.Runnable
        public void run() {
            try {
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(ProcessSession.this.f12497e));
                while (true) {
                    String line = bufferedReader.readLine();
                    if (line == null) {
                        return;
                    }
                    StringBuilder sb = ProcessSession.this.f12504l;
                    sb.append(line);
                    sb.append("\n");
                }
            } catch (Exception unused) {
            }
        }
    }

    public ProcessSession(DrvRuntime drvRuntime, long j7, int[] iArr) {
        this.f12493a = drvRuntime;
        this.f12494b = j7;
        ParcelFileDescriptor parcelFileDescriptorAdoptFd = ParcelFileDescriptor.adoptFd(iArr[0]);
        this.f12495c = new C0226a(parcelFileDescriptorAdoptFd.getFileDescriptor(), parcelFileDescriptorAdoptFd);
        ParcelFileDescriptor parcelFileDescriptorAdoptFd2 = ParcelFileDescriptor.adoptFd(iArr[1]);
        this.f12496d = new b(parcelFileDescriptorAdoptFd2.getFileDescriptor(), parcelFileDescriptorAdoptFd2);
        ParcelFileDescriptor parcelFileDescriptorAdoptFd3 = ParcelFileDescriptor.adoptFd(iArr[2]);
        this.f12497e = new c(parcelFileDescriptorAdoptFd3.getFileDescriptor(), parcelFileDescriptorAdoptFd3);
    }

    public void j(OutputStream outputStream, boolean z6, boolean z7) {
        MyThread dVar = new MyThread(outputStream, z6, z7);
        this.myThread = dVar;
        dVar.start();
        if (z6) {
            return;
        }
        new e().start();
    }

    public void k() {
        this.f12493a.procDestroy(this.f12494b);
    }

    public String l() {
        return this.f12504l.toString();
    }

    public Exception m() {
        return this.f12503k;
    }

    public InputStream n() {
        return this.f12496d;
    }

    public OutputStream o() {
        return this.f12495c;
    }

    public String p() {
        return new String(this.f12499g, 0, this.f12500h);
    }

    public int q() {
        return this.f12493a.procWait(this.f12494b);
    }

    public void r() {
        while (this.myThread.isAlive()) {
            Thread.yield();
        }
    }

    public void s() {
        while (this.myThread.isAlive()) {
            if (this.f12501i && System.currentTimeMillis() - this.f12502j > 10) {
                return;
            } else {
                Thread.yield();
            }
        }
    }
}
