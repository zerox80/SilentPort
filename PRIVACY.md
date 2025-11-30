# Privacy Policy for SilentPort

**Last Updated:** November 2025  
**Version:** 1.0

SilentPort is a **Zero-Profit** and **Zero-Data** project. We firmly believe that privacy tools must adhere to the highest standards of data minimization.

This policy serves not only as a legal document but as technical proof of our core promise: **"What happens on your device, stays on your device."**

---

## 1. Core Principle: 100% Local Processing

SilentPort does not collect, store, share, or transmit **any** personal data to external servers.

* **No Server Communication:** The app does not communicate with any backend. There are no login servers, no analytics servers, and no advertising networks.
* **Local Database:** All usage statistics are stored in an isolated database (`AppDatabase`) directly on your device, inaccessible to other apps.

---

## 2. Technical Analysis of Permissions

SilentPort requests permissions that may seem sensitive. Here is a transparent technical explanation of why they are necessary and how we ensure they cannot be misused.

### 2.1 Local Firewall (`BIND_VPN_SERVICE`)
This is the core function of the firewall. Android requires the use of the VPN interface to filter network traffic.

* **Mechanism:** The app creates a *local loopback interface* on your device.
* **No Real VPN:** A connection to an external VPN server (tunnel) is **never** established. Your IP address is never masked or rerouted to a third party.
* **Traffic Handling:**
    * **Blocked Apps:** Packets are sent to a local "sinkhole" socket and immediately discarded (`drainPackets()` method in source code).
    * **Allowed Apps:** Traffic bypasses the filter entirely. SilentPort does not inspect, log, or analyze the payload of your data packets.

### 2.2 Usage Statistics (`PACKAGE_USAGE_STATS`)
Required to identify unused apps.

* **What we read:** We query the Android system (`UsageStatsManager`) strictly for the timestamp of the last usage (`MOVE_TO_FOREGROUND`).
* **What we do NOT read:** We do not see what you do inside an app, what content you view, or who you communicate with.
* **Storage:** Only the app package name (e.g., `com.example.app`) and the last usage timestamp are stored locally.

### 2.3 App List (`QUERY_ALL_PACKAGES`)
Required to display a list of all installed applications so you can manage them. This data never leaves your device's RAM during runtime.

### 2.4 Internet Access (`INTERNET`)
This permission is declared in the manifest but is **not actively used** by the current app logic to transmit data.

* **Security Guarantee:** Since the app is open source, you can verify that no tracker libraries or API calls to external URLs exist within the codebase.

---

## 3. Our "Zero Data" Security Promise

We don't just claim privacy; we guarantee it through verifiable technical measures:

1.  **No Trackers or Ads:**
    Reviewing our `build.gradle.kts` file proves: There are **no** libraries included from Facebook, Google Analytics, Firebase Crashlytics, or any ad networks.

2.  **Disabled Cloud Backup:**
    We have explicitly disabled Android's automatic cloud backup for SilentPort (`data_extraction_rules.xml`). This means: Even if you use Google Drive backups for your phone, your SilentPort usage profile is **never** uploaded to the cloud.

3.  **Open Source (GPLv3):**
    The complete source code is public. Any security researcher can verify that no backdoors exist.

---

## 4. Your Rights (GDPR / CCPA Compliance)

Since we do not collect data, there is no complex process required to request data deletion. You have full control by default:

* **Right to Access & Portability:** Since all data is local, you already physically possess it on your device.
* **Right to Erasure:**
    * Uninstall the app, and all data is **immediately and permanently** deleted.
    * Alternatively: Go to "Settings > Apps > SilentPort > Storage > Clear Data".
* **Right to Object:** You can revoke any permission (e.g., Usage Access) at any time in Android settings. The app will function with limited features but will respect your decision not to share that access.

---

## 5. Contact

SilentPort is a community-driven project. If you have questions regarding security or the source code, please open an issue in our public repository.

**Maintainer:** [Your Name / Project Name]  
**Project URL:** [Link to GitHub Repo]
