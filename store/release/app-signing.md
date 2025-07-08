# VoiceBridge App Signing Guide

## Overview

This guide covers the app signing process for VoiceBridge releases on Google Play Store.

## Signing Configuration

### 1. Generate Upload Key (First Time Only)

```bash
keytool -genkey -v -keystore voicebridge-upload.keystore \
  -alias voicebridge-upload \
  -keyalg RSA -keysize 2048 \
  -validity 10000
```

**Key Details:**
- Keystore: `voicebridge-upload.keystore`
- Alias: `voicebridge-upload`
- Algorithm: RSA 2048-bit
- Validity: 10,000 days (~27 years)

### 2. Configure Gradle Signing

Create `android/keystore.properties` (DO NOT commit to git):

```properties
storeFile=../keystores/voicebridge-upload.keystore
storePassword=YOUR_STORE_PASSWORD
keyAlias=voicebridge-upload
keyPassword=YOUR_KEY_PASSWORD
```

Update `android/app/build.gradle`:

```gradle
def keystorePropertiesFile = rootProject.file("keystore.properties")
def keystoreProperties = new Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(new FileInputStream(keystorePropertiesFile))
}

android {
    signingConfigs {
        release {
            keyAlias keystoreProperties['keyAlias']
            keyPassword keystoreProperties['keyPassword']
            storeFile file(keystoreProperties['storeFile'])
            storePassword keystoreProperties['storePassword']
        }
    }
    
    buildTypes {
        release {
            signingConfig signingConfigs.release
            minifyEnabled true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
}
```

### 3. Build Signed Release

```bash
cd android
./gradlew bundleRelease
```

Output: `android/app/build/outputs/bundle/release/app-release.aab`

## Google Play App Signing

### Benefits of Play App Signing:
1. Google manages your app signing key
2. Automatic APK optimization
3. Dynamic delivery support
4. Key upgrade if compromised
5. Support for app bundles

### Setup Process:

1. **First Upload**
   - Upload your signed AAB to Play Console
   - Google generates app signing key
   - Your upload key verifies your identity

2. **Download Certificates**
   - App signing certificate
   - Upload certificate
   - Save for API integrations

3. **Key Management**
   - Store upload keystore securely
   - Back up to multiple locations
   - Never share or commit to git

## Release Signing Checklist

- [ ] Increment version code in `build.gradle`
- [ ] Update version name in `build.gradle`
- [ ] Run all tests: `./gradlew test`
- [ ] Build release bundle: `./gradlew bundleRelease`
- [ ] Test release build on device
- [ ] Verify ProGuard/R8 rules
- [ ] Check APK size (<150MB)
- [ ] Sign with upload key
- [ ] Upload to Play Console
- [ ] Complete release notes

## Security Best Practices

### Keystore Security:
1. **Never commit keystores to git**
2. Store in secure password manager
3. Use strong, unique passwords
4. Enable 2FA on Play Console
5. Limit access to signing keys

### Backup Strategy:
1. Encrypted cloud backup
2. Physical secure storage
3. Team password manager
4. Document recovery process

### CI/CD Integration:

For GitHub Actions (using secrets):

```yaml
- name: Decode Keystore
  env:
    ENCODED_KEYSTORE: ${{ secrets.KEYSTORE_BASE64 }}
  run: |
    echo $ENCODED_KEYSTORE | base64 -d > voicebridge-upload.keystore

- name: Build Release Bundle
  env:
    KEYSTORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}
    KEY_ALIAS: ${{ secrets.KEY_ALIAS }}
    KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
  run: |
    ./gradlew bundleRelease \
      -Pandroid.injected.signing.store.file=voicebridge-upload.keystore \
      -Pandroid.injected.signing.store.password=$KEYSTORE_PASSWORD \
      -Pandroid.injected.signing.key.alias=$KEY_ALIAS \
      -Pandroid.injected.signing.key.password=$KEY_PASSWORD
```

## Troubleshooting

### Common Issues:

1. **"Keystore was tampered with"**
   - Wrong password
   - Corrupted file
   - Wrong keystore file

2. **"The APK is not signed"**
   - Missing signing config
   - Build type not set to release
   - Gradle sync needed

3. **"Upload key not recognized"**
   - Using wrong keystore
   - Key alias mismatch
   - Not enrolled in Play App Signing

### Verification Commands:

```bash
# Verify keystore
keytool -list -v -keystore voicebridge-upload.keystore

# Verify signed AAB
jarsigner -verify -verbose app-release.aab

# Check certificate fingerprint
keytool -printcert -jarfile app-release.aab
```

## Emergency Procedures

### Lost Upload Key:
1. Contact Play Console support
2. Provide proof of ownership
3. Request upload key reset
4. Generate new upload key
5. Update all CI/CD systems

### Compromised Key:
1. Immediately contact Google
2. Request key rotation
3. Generate new keys
4. Update all systems
5. Monitor for unauthorized uploads

## Resources

- [Play Console Help - App Signing](https://support.google.com/googleplay/android-developer/answer/9842756)
- [Android Developers - Sign Your App](https://developer.android.com/studio/publish/app-signing)
- [Upload Key Reset Form](https://support.google.com/googleplay/android-developer/contact/key)

---

**Important**: This document contains sensitive security information. Store securely and limit access to authorized team members only.