# 🔄 CSP Modern Upgrade to Minecraft 26.x + Java 25

## Current Status: **READY FOR UPGRADE**

Your mod has been configured for **Minecraft 26.3** with **Fabric Loader 0.19.5**, but we need **Java 25** for Gradle builds (Minecraft 26.x requires it).

---

## 📋 **Quick Setup Steps:**

### Step 1: Install Java 25

**Option A - Quick (Manual):**
1. Download OpenJDK 25 MSI from: https://github.com/AdoptOpenJDK/openjdk25-binaries/releases
2. Look for `OpenJDK25U-jdk_x64_windows_hotspot_25.0.1*.msi` or similar
3. Install it (installs to `C:\Users\Ben\AppData\Local\Programs\Eclipse Adoptium\jdk-25.x.x`)

**Option B - Automated:**
Run: `.\install-java25.ps1`

---

### Step 2: Build with Java 25

Use the batch file (recommended):
```bash
.\gradle-build-with-java25.bat
```

Or set JAVA_HOME manually:
```bash
set JAVA_HOME=C:\Users\Ben\AppData\Local\Programs\Eclipse Adoptium\jdk-25.0.1
cd C:\Users\Ben\Documents\CSP-Modern
gradlew.bat clean build --no-daemon
```

---

### Step 3: Copy JAR to Minecraft Mods

Once build succeeds, copy the new JAR:
```bash
C:\Users\Ben\Documents\CSP-Modern\build\libs\csp-modern-0.1.0.jar
```

Paste into your Minecraft 26.x mods folder:
```
C:\Users\Ben\AppData\Roaming\.minecraft\mods\
```

---

## 🎮 **Running Your Game:**

After installing Java 25, you'll need to launch Minecraft 26.3 with it:

1. Open your Minecraft Launcher (Microsoft Store or official launcher)
2. Go to Settings → Installations
3. Select "Chernobyl Scientific Project - Modern" profile
4. Click "Edit" → Set Java Executable Path to Java 25:
   ```
   C:\Users\Ben\AppData\Local\Programs\Eclipse Adoptium\jdk-25.x.x\bin\java.exe
   ```

OR use the launcher's automatic Java detection (it should find both JDK 21 and 25).

---

## ✅ **Version Matrix:**

| Component | Version | Purpose |
|-----------|---------|---------|
| Minecraft Game | 26.3 | Latest stable release |
| Fabric Loader | 0.19.5 | Mod loading system |
| Java (Game Client) | 25 | Required by Mojang for MC 26.x |
| Java (Builds) | 25 | Gradle needs matching version |
| CSP Modern Mod | 0.1.0 | Your mod version |

---

## 🐛 **Troubleshooting:**

### "Minecraft 26.3 requires Java 25" error:
- You need to install Java 25 and set it as launcher's Java path

### Gradle build fails with "requires Java 25":
- Run `.\gradle-build-with-java25.bat` or set JAVA_HOME manually before building

### Fabric mod incompatibility warnings:
- Delete any old `csp-modern-[old-version].jar` from your mods folder
- Only the newly built `csp-modern-0.1.0.jar` should be present

---

## 📊 **What Changed in Our Configuration:**

**gradle.properties:**
```properties
minecraft_version=26.3          # Was: 1.20.1
yarn_mappings=26.3+build.3      # Was: 1.20.1+build.10
loader_version=0.19.5           # Was: 0.15.11
```

**fabric.mod.json:**
```json
"depends": {
  "minecraft": "~26.3",          # Was: ~1.20.1
  "java": ">=21",                # Was: >=17 (now >=21 for consistency)
  "fabricloader": ">=0.19.5"     # Was: >=0.15.11
}
```

---

## 🎯 **Next Steps After Build:**

1. ✅ Verify build succeeds with Java 25
2. ✅ Install mod in your Minecraft 26.x launcher profile  
3. ✅ Check game loads without errors
4. ✅ Test reactor physics simulation initializes
5. ✅ Validate ICMS communication devices work
6. ✅ Explore training simulator features

---

**Need help?** Run the build batch file and share any error output - I'll help debug!
