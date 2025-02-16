package kr.co.s1.mobilecard.s1mobilecard;

import android.util.Log;

/* loaded from: classes.dex */
public class JniS1Pass {
    public native int getError();

    public native String getLibraryVer();

    public native int loadKeyFile(String str);

    public native byte[] makeCommandAPDU(byte[] bArr, String str, String str2);

    public JniS1Pass() {
        try {
            System.loadLibrary("JniS1Pass");
            Log.i("JNI", "JNI loaded");
        } catch (UnsatisfiedLinkError e) {
            Log.e("JNI","JNI error:" + e);
        }
    }
}