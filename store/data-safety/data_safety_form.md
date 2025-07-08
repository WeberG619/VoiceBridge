# Google Play Console Data Safety Form

**App: VoiceBridge**  
**Last Updated: January 2025**

## Data Safety Summary

VoiceBridge is designed with privacy as a core principle. The app operates entirely offline and does not collect, store, or transmit any user data to external servers.

---

## Section 1: Data Collection and Security

### Does your app collect or share any of the required user data types?
**Answer: No**

VoiceBridge does not collect or share any user data. All processing happens locally on the user's device.

### Is all of the user data collected by your app encrypted in transit?
**Answer: Not applicable** - No data is transmitted

### Do you provide a way for users to request that their data is deleted?
**Answer: Yes** - Users can delete all app data by uninstalling the app

---

## Section 2: Data Types

### Personal Information
- **Name**: ❌ Not collected
- **Email address**: ❌ Not collected
- **User IDs**: ❌ Not collected
- **Address**: ❌ Not collected
- **Phone number**: ❌ Not collected

### Financial Information
- **User payment info**: ❌ Not collected
- **Purchase history**: ❌ Not collected
- **Credit score**: ❌ Not collected
- **Other financial info**: ❌ Not collected

### Health and Fitness
- **Health info**: ❌ Not collected
- **Fitness info**: ❌ Not collected

### Messages
- **Emails**: ❌ Not collected
- **SMS or MMS**: ❌ Not collected
- **Other in-app messages**: ❌ Not collected

### Photos and Videos
- **Photos**: ✅ Processed locally only
  - Purpose: OCR text extraction from documents
  - Optional: Yes
  - Collection: Temporary processing only
  - Sharing: Never shared
  - Deletion: Immediately after processing

### Audio Files
- **Voice or sound recordings**: ✅ Processed locally only
  - Purpose: Voice-to-text conversion for form filling
  - Optional: Yes (core feature but not required)
  - Collection: Temporary processing only
  - Sharing: Never shared
  - Deletion: Immediately after processing
- **Music files**: ❌ Not collected
- **Other audio files**: ❌ Not collected

### Files and Docs
- **Files and docs**: ❌ Not collected

### Calendar
- **Calendar events**: ❌ Not collected

### Contacts
- **Contacts**: ❌ Not collected

### App Activity
- **Page views and taps**: ❌ Not collected
- **In-app search history**: ❌ Not collected
- **Installed apps**: ❌ Not collected
- **Other user-generated content**: ❌ Not collected
- **Other app activity**: ❌ Not collected

### Web Browsing
- **Web browsing history**: ❌ Not collected

### App Info and Performance
- **Crash logs**: ❌ Not collected
- **Diagnostics**: ❌ Not collected
- **Other app performance data**: ❌ Not collected

### Device or Other IDs
- **Device or other IDs**: ❌ Not collected

---

## Section 3: Data Usage and Handling

### Voice Recordings
**Collected**: Temporarily processed  
**Purpose**: Voice-to-text conversion  
**Data handling**:
- Processed entirely on-device
- Never stored permanently
- Never transmitted off-device
- Immediately discarded after processing
- No cloud services used

### Camera/Photos
**Collected**: Temporarily processed  
**Purpose**: OCR text extraction  
**Data handling**:
- Processed entirely on-device
- Never stored permanently
- Never transmitted off-device
- Immediately discarded after processing
- No cloud services used

---

## Section 4: Security Practices

### Security measures
- All processing is done locally on-device
- No network communication for core features
- No user accounts or authentication
- App data stored in Android's secure app sandbox
- No third-party SDKs that collect data

### Independent security review
**Has your app undergone an independent security review?**
- No (not required for offline apps)

---

## Section 5: Accessibility Service Declaration

### Does your app use Accessibility Service?
**Answer: Yes**

### Purpose of Accessibility Service
VoiceBridge uses Android Accessibility Service to:
1. Identify form fields in other applications
2. Fill form fields with user-provided data
3. Enable hands-free interaction for users with disabilities
4. Provide voice-controlled navigation

### Data handled by Accessibility Service
- **What is accessed**: Form field information (labels, types)
- **What is NOT accessed**: Passwords, payment info, personal data
- **Storage**: No data from other apps is stored
- **Transmission**: No data is transmitted

### Compliance
- Follows Google Play's Accessibility Service guidelines
- Only uses permissions necessary for core functionality
- Clear disclosure in app description
- User consent required during onboarding

---

## Section 6: Target Audience

### Is your app designed for children?
**Answer: No**

### Target age groups
- 13-15 years
- 16-17 years  
- 18+ years

---

## Section 7: Developer Information

### Organization
- **Name**: VoiceBridge Team
- **Email**: support@voicebridge.app
- **Website**: https://voicebridge.app
- **Privacy Policy**: https://voicebridge.app/privacy-policy

### Data Protection Officer
- **Email**: privacy@voicebridge.app

---

## Additional Declarations

### AI/ML Usage
VoiceBridge uses on-device AI models:
- **Whisper**: Speech-to-text (offline)
- **LLaMA**: Text processing (offline)
- **Tesseract**: OCR (offline)

All AI processing is done locally without any cloud connectivity.

### Permissions Justification

1. **RECORD_AUDIO**
   - Purpose: Voice input for form filling
   - Essential for core functionality
   - Audio never leaves device

2. **CAMERA**
   - Purpose: Document scanning for OCR
   - Optional feature
   - Images never leave device

3. **ACCESSIBILITY_SERVICE**
   - Purpose: Form field interaction
   - Essential for automation features
   - No sensitive data collected

---

## Compliance Statement

VoiceBridge is committed to user privacy and complies with:
- Google Play Developer Program Policies
- General Data Protection Regulation (GDPR)
- California Consumer Privacy Act (CCPA)
- Children's Online Privacy Protection Act (COPPA)

The app's offline-first architecture ensures maximum privacy protection by design.

---

**Note**: This form should be reviewed before each app update to ensure accuracy.