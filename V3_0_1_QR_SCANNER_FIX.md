# v3.0.1 QR Scanner Fix

The QR join screen uses Google Code Scanner (`play-services-code-scanner:16.1.0`).

Correct Java package imports:

```java
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning;
```

The previous v3.0 package mistakenly used `com.google.android.gms.mlkit.codescanner`, which does not contain these API classes. That caused the cascading Android Studio errors for `GmsBarcodeScanner`, `GmsBarcodeScanning`, `startScan()`, `getRawValue()`, and the failure listener lambda.

The AndroidManifest now also requests the Google Code Scanner module (`barcode_ui`) for install-time download where supported.
