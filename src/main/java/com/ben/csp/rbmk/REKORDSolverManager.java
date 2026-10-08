package com.ben.csp.rbmk;

/**
 * REKORD Solver Manager - Handles native library loading and initialization.
 * 
 * SGB: This class ensures the rbmk_solver native library is loaded exactly once
 * per JVM process and provides access to all solver functions.
 */
public class REKORDSolverManager {

    private static boolean isInitialized = false;
    
    /**
     * Path to native solver library relative to application root.
     * Platform-specific: .dylib (macOS), .so (Linux), .dll (Windows)
     */
    private static final String LIBRARY_PATH = "rbmk_solver";

    public REKORDSolverManager() {
        // Initialize on first use for thread safety
    }

    /**
     * Load native solver library and initialize.
     * Must be called once before using any solver functions.
     */
    public synchronized static void loadLibrary(String applicationPath) throws UnsatisfiedLinkError {
        if (isInitialized) {
            return; // Already loaded
        }

        try {
            String os = System.getProperty("os.name", "generic").toLowerCase();
            
            // Determine platform-specific library suffix
            String librarySuffix = null;
            if (os.contains("mac")) {
                librarySuffix = ".dylib";
                System.loadLibrary(LIBRARY_PATH + ".dylib");
            } else if (os.contains("win")) {
                librarySuffix = "";
                System.loadLibrary(LIBRARY_PATH);
            } else if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
                librarySuffix = ".so";
                System.loadLibrary(LIBRARY_PATH + ".so");
            } else {
                throw new IllegalStateException("Unsupported OS: " + os);
            }
            
            isInitialized = true;
            System.out.println("✅ REKORD Solver Library loaded successfully: " + LIBRARY_PATH + librarySuffix);
            
        } catch (UnsatisfiedLinkError e) {
            System.err.println("❌ ERROR: Could not load native solver library. Check that rbmk_solver.* exists.");
            throw e;
        }
    }

    /**
     * Load native solver library from a specific path (e.g., JAR resources).
     */
    public synchronized static void loadLibraryFromClassPath(String relativePath) {
        if (isInitialized) {
            return;
        }

        try {
            // Try to load from classpath
            java.net.URL url = REKORDSolverManager.class.getResource(relativePath);
            if (url != null) {
                System.loadLibrary(LIBRARY_PATH);
                isInitialized = true;
                System.out.println("✅ Loaded solver library from classpath: " + relativePath);
            } else {
                throw new IllegalStateException("Native library not found in classpath: " + relativePath);
            }
        } catch (Exception e) {
            // Fallback to default path loading
            loadLibrary("");
        }
    }

    /**
     * Check if native library is loaded.
     */
    public static boolean isLoaded() {
        return isInitialized;
    }
}
