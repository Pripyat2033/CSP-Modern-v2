# 🛡️ CSP-Modern Safety Protocol

## Zero Deletion Policy - NEVER VIOLATE THIS

### The Rule:
**You can NEVER delete any files from this project without explicit confirmation.**

This is absolute. No exceptions.

---

## How to Safely Request File Deletion (If Truly Necessary)

### Method 1: Ask Me First (RECOMMENDED)
```
"Please confirm deletion of [filename]"
"Can you help me remove [file]? What will break if I delete it?"
```

I will then:
- Tell you what will break
- Provide exact commands to do it safely
- Get your explicit confirmation before proceeding

### Method 2: Delete Git Hook Temporarily (If Needed)
1. Remove hook: `rm .git/hooks/pre-commit`
2. Proceed with deletion
3. Restore hook afterward: recreate above hooks

### Method 3: Use Git Commands Safely
```bash
# To delete a file safely with backup:
mv filename.bak backup/
git rm filename.bak
git commit -m "Removing deprecated [filename]"
```

---

## What This Project Protects

These files are CRITICAL and should NEVER be deleted:

### Core Files (Off-Limits):
- `src/main/java/**/*` - All Java source
- `src/main/resources/**/*` - All assets, models, textures
- `.gitignore` - Git configuration
- `gradlew`, `gradlew.bat` - Gradle wrapper scripts
- `build.gradle`, `settings.gradle` - Build configuration
- `fabric.mod.json` - Mod metadata
- `gradle.properties` - Version settings

### Protected Directories:
- `src/main/java/com/ben/csp/agent/**/*` - All agent files (MANDATORY)
- `src/main/java/com/ben/csp/block/**/*` - All blocks
- `src/main/resources/assets/**/*` - All game assets

---

## Recovery Procedures (If Files Disappear)

### Emergency Recovery:
1. **Check GitHub immediately:** https://github.com/Pripyat2033/CSP-Modern-v2
2. **Switch to latest branch:**
   ```bash
   git checkout 2026-work
   ```
3. **If files are missing:**
   ```bash
   git status
   git fetch origin && git pull origin 2026-work
   ```
4. **Test build:**
   ```bash
   ./gradlew clean build --no-daemon
   ```

### Files Never Lost:
- Your code is ALWAYS backed up on GitHub
- `git checkout` can restore ANY file instantly
- Full Git history available for any point in time

---

## What to Do If You See Unusual Behavior

### Signs of Potential Issues:
1. Files disappearing unexpectedly
2. VS Code workspace not loading
3. "Could not find [class]" errors after changes
4. Gradle build failing with mysterious errors

### Immediate Actions:
1. **Check Git status:** `git status`
2. **Verify files exist:** `ls -la src/main/java/com/ben/csp/`
3. **Pull from GitHub:** `git pull origin 2026-work`
4. **Build test:** `./gradlew build --no-daemon`

---

## Current Active Protections

✅ **Git Pre-commit Hook** - Blocks deletion attempts  
✅ **Git Prepare-commit Message** - Warns about deletions in commit messages  
✅ **Environment Variable** (`SPOT_DELETION_BLOCK`) - System-wide protection  
✅ **GitHub Backup** - Instant recovery from remote  

### How to Verify Protection is Active:
```bash
ls -la .git/hooks/pre-commit  # Should show executable file
echo $SPOT_DELETION_BLOCK     # Should print "true" if set
```

---

## If You Need to Refactor or Remove Features

**ALWAYS use these methods:**

1. **Comment out code** (don't delete):
   ```java
   // @deprecated This will be removed later
   public void oldMethod() { }
   ```

2. **Create feature branch**:
   ```bash
   git checkout -b refactor-feature-removal
   ```

3. **Add deprecation warnings** to files you might remove:
   ```java
   @Deprecated // TODO: Remove when migrating to new system
   public class OldBlock extends Block { }
   ```

4. **Get explicit confirmation before deletion:**
   - Ask: "What will break if I delete this file?"
   - List all files that would be removed
   - Provide alternatives (commenting, deprecating)

---

## Summary: The Golden Rules

1. ✅ **NEVER delete** without asking first
2. ✅ **ALWAYS use git checkout** to recover missing files
3. ✅ **PULL from GitHub regularly**: `git pull origin 2026-work`
4. ✅ **Test build after changes**: `./gradlew build --no-daemon`
5. ✅ **Check .git hooks are active**: They protect against deletion
6. ✅ **Use deprecation** instead of immediate removal

---

## Contact for Help

If you ever feel uncertain about what to do with a file, ask:
- "Should I delete [filename]?"
- "How do I safely remove this feature?"
- "What happens if I modify this file?"

Better safe than sorry! 🛡️

---

**Last Updated:** October 2026  
**Safety Protocol Version:** 1.0  
**Backup Location:** https://github.com/Pripyat2033/CSP-Modern-v2