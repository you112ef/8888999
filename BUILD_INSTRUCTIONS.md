# 🔨 Build Instructions - Sperm Analyzer AI

## 📋 Prerequisites

### Required Software:
- **Android Studio**: Arctic Fox or newer
- **JDK**: OpenJDK 11 or newer
- **Android SDK**: API level 21-34
- **Gradle**: 8.1+ (included with wrapper)

### Required Hardware:
- **RAM**: 8GB minimum, 16GB recommended
- **Storage**: 10GB free space
- **Android Device**: API 21+ for testing

## 🚀 Quick Start

### 1. Clone and Setup
```bash
# Navigate to the android app directory
cd android-app

# Make gradlew executable (Linux/Mac)
chmod +x gradlew

# Verify Gradle wrapper
./gradlew --version
```

### 2. **CRITICAL**: Add AI Model
⚠️ **The app will NOT work without a real trained model!**

Place your trained PyTorch model at:
```
android-app/app/src/main/assets/sperm_analyzer.torchscript.pt
```

See `MODEL_INSTRUCTIONS.md` for detailed model requirements.

### 3. Build Debug APK
```bash
# Clean build
./gradlew clean

# Build debug APK
./gradlew assembleDebug

# Output location:
# app/build/outputs/apk/debug/app-debug.apk
```

### 4. Build Release APK
```bash
# Build optimized release APK
./gradlew assembleRelease

# Output location:
# app/build/outputs/apk/release/app-release-unsigned.apk
```

## 📱 Installation

### Install on Connected Device
```bash
# Install debug version
./gradlew installDebug

# Or install release version
./gradlew installRelease

# Or use ADB directly
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Manual Installation
1. Copy APK to Android device
2. Enable "Unknown Sources" in device settings
3. Open APK file to install

## 🧪 Testing

### Run Unit Tests
```bash
./gradlew test
```

### Run Instrumented Tests
```bash
./gradlew connectedAndroidTest
```

### Performance Testing
```bash
# Profile the app
./gradlew assembleDebug -Pandroid.enableProfiles=true
```

## 🔍 Troubleshooting

### Common Issues:

#### 1. Model Loading Fails
```
Error: "Error loading AI model: File not found"
```
**Solution**: Ensure `sperm_analyzer.torchscript.pt` exists in `app/src/main/assets/`

#### 2. PyTorch Dependency Issues
```
Error: "Could not resolve org.pytorch:pytorch_android"
```
**Solution**: Check internet connection and Gradle repositories

#### 3. Camera Permission Denied
```
Error: "Camera permission is required"
```
**Solution**: Grant camera permission in device settings

#### 4. Out of Memory During Build
```
Error: "GC overhead limit exceeded"
```
**Solution**: Increase Gradle heap size in `gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx4096m
```

#### 5. NDK Issues
```
Error: "No toolchains found in the NDK"
```
**Solution**: Install Android NDK through SDK Manager

### Performance Issues:

#### Slow Inference
- Ensure model is optimized for mobile
- Check device RAM availability
- Reduce input image resolution if needed

#### Camera Lag
- Lower camera preview resolution
- Optimize UI thread operations
- Use background thread for analysis

## 📊 Build Variants

### Debug Build
- Includes debugging symbols
- Verbose logging enabled
- Not optimized
- Larger APK size

### Release Build
- Optimized and minified
- Logging removed (ProGuard)
- Smaller APK size
- Better performance

## 🔧 Advanced Configuration

### Signing Release APK
1. Generate signing key:
```bash
keytool -genkey -v -keystore release-key.keystore -alias sperm-analyzer -keyalg RSA -keysize 2048 -validity 10000
```

2. Add to `app/build.gradle`:
```gradle
android {
    signingConfigs {
        release {
            storeFile file('release-key.keystore')
            storePassword 'your_password'
            keyAlias 'sperm-analyzer'
            keyPassword 'your_password'
        }
    }
    buildTypes {
        release {
            signingConfig signingConfigs.release
        }
    }
}
```

### ProGuard Optimization
Enable additional optimizations in `app/build.gradle`:
```gradle
buildTypes {
    release {
        minifyEnabled true
        shrinkResources true
        proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
    }
}
```

### Memory Optimization
Add to `android-app/app/src/main/AndroidManifest.xml`:
```xml
<application
    android:largeHeap="true"
    android:hardwareAccelerated="true">
```

## 📱 Device Requirements

### Minimum Requirements:
- **OS**: Android 5.0 (API 21)
- **RAM**: 2GB
- **Storage**: 100MB free
- **Camera**: Rear camera required

### Recommended Requirements:
- **OS**: Android 8.0+ (API 26+)
- **RAM**: 4GB+
- **Storage**: 500MB free
- **Camera**: Autofocus support
- **CPU**: ARM64 or x86_64

## 🚀 Deployment

### Google Play Store
1. Build signed release APK
2. Test on multiple devices
3. Prepare store listing
4. Upload through Play Console

### Direct Distribution
1. Build release APK
2. Host on secure server
3. Provide download instructions
4. Include installation guide

## 📋 Checklist Before Release

- [ ] AI model is properly trained and tested
- [ ] All permissions are correctly requested
- [ ] Camera functionality works on target devices
- [ ] Analysis results are accurate
- [ ] Performance is acceptable on minimum spec devices
- [ ] No memory leaks or crashes
- [ ] Proper error handling implemented
- [ ] User documentation created
- [ ] Legal compliance verified

## 🆘 Getting Help

### Build Issues:
1. Check Android Studio error console
2. Verify all dependencies are downloaded
3. Clean and rebuild project
4. Check device compatibility

### Runtime Issues:
1. Check device logs: `adb logcat`
2. Verify model file integrity
3. Test camera permissions
4. Monitor memory usage

### Performance Issues:
1. Profile with Android Studio profiler
2. Optimize model size and complexity
3. Reduce image processing resolution
4. Use background threads appropriately

---

**⚠️ IMPORTANT**: This application requires a real, trained AI model to function. Do not attempt to build without first obtaining a properly trained YOLOv8 model for sperm analysis.