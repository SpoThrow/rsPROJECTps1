package tools.glclasspathprobe;

/**
 * Phase 7.2a runtime classpath probe - the live-gate companion to the LWJGL 2
 * removal.
 *
 * WHY THIS EXISTS
 * 7.2a dropped deps\lwjgl.jar (LWJGL 2) from every classpath because it and
 * deps\lwjgl3\ both declare org.lwjgl.opengl.*, and LWJGL 2 was winning the
 * lookup: its GL30 has no GL14 ancestor, so GL30.GL_DEPTH_COMPONENT24 did not
 * resolve and ui\GlScene.java would not compile.
 *
 * The COMPILE error is now gone, but two things no compile and no live play
 * session can see:
 *
 *   1. The LIVE CAPTURE runs renderer=software, so it never touches org.lwjgl
 *      at all - it would pass identically if the collision came back.
 *   2. A compile only proves GL_DEPTH_COMPONENT24 resolves for javac. It does
 *      NOT prove which JAR the running JVM picks, and the runtime failure is
 *      worse: LWJGL 2's static GL entry points route through an LWJGL 2 context
 *      that GLFW never initialises, so the GL arm would die at its first call.
 *
 * This probe loads the classes on the EXACT runtime classpath and asserts the
 * LWJGL 3 arrangement specifically: GL30 must reach GL14 by INHERITANCE, and
 * the constant must read back. Under LWJGL 2 that lookup throws
 * NoSuchFieldException - which is the same failure as the original bug.
 *
 * Loaded with initialize=false so class static initializers (which reach for
 * GLFW/display state) do not run; the constant is read by reflection instead.
 *
 * Exits 0 (CLASSPATH_PROBE_OK) or 1 (CLASSPATH_PROBE_FAIL).
 */
public final class ClasspathProbe {

    private static ClassLoader loader;

    private static void chk(String name) {
        try {
            Class<?> c = Class.forName(name, false, loader);
            String from = c.getProtectionDomain().getCodeSource() == null
                    ? "<bootstrap/jdk>"
                    : c.getProtectionDomain().getCodeSource().getLocation().toString();
            System.out.println("  OK    " + name);
            System.out.println("        <- " + from);
        } catch (Throwable t) {
            System.out.println("  FAIL  " + name + "  : " + t);
        }
    }

    public static void main(String[] args) {
        loader = ClasspathProbe.class.getClassLoader();

        System.out.println("classpath entries: "
                + System.getProperty("java.class.path").split(java.io.File.pathSeparator).length);
        System.out.println();

        chk("org.lwjgl.glfw.GLFW");
        chk("org.lwjgl.opengl.GL30");
        chk("org.lwjgl.opengl.GL21");
        chk("org.lwjgl.opengl.GL14");
        chk("ui.GlScene");
        chk("ui.GlSceneRenderer");

        System.out.println();
        System.out.println("THE COLLISION CHECK - GL30 must INHERIT GL_DEPTH_COMPONENT24 from GL14:");
        try {
            Class<?> gl30 = Class.forName("org.lwjgl.opengl.GL30", false, loader);
            Class<?> gl14 = Class.forName("org.lwjgl.opengl.GL14", false, loader);

            // LWJGL 3: GL30 -> GL21 -> GL20 -> GL15 -> GL14, so getField finds it
            // by inheritance. LWJGL 2: no GL14 ancestor -> NoSuchFieldException,
            // which is exactly the original bug, reappearing at runtime.
            java.lang.reflect.Field f = gl30.getField("GL_DEPTH_COMPONENT24");
            int value = f.getInt(null);
            boolean inherited = f.getDeclaringClass() != gl30;

            System.out.println("  OK    GL30.GL_DEPTH_COMPONENT24 = " + value);
            System.out.println("        declared on  : " + f.getDeclaringClass().getName()
                    + (inherited ? "  (INHERITED - the LWJGL 3 arrangement)" : "  (on GL30 itself)"));
            System.out.println("        GL30 extends : " + gl30.getSuperclass().getName());
            System.out.println("        GL14 value   : "
                    + gl14.getField("GL_DEPTH_COMPONENT24").getInt(null));

            boolean lwjgl3 = inherited && f.getDeclaringClass() == gl14 && ancestor(gl30, gl14);
            if (lwjgl3) {
                System.out.println();
                System.out.println("CLASSPATH_PROBE_OK");
                return;
            }
            System.out.println();
            System.out.println("CLASSPATH_PROBE_FAIL: GL30 does not reach GL14 by inheritance,"
                    + " so this is NOT the LWJGL 3 hierarchy (LWJGL 2 is shadowing again).");
        } catch (Throwable t) {
            System.out.println("  FAIL  GL_DEPTH_COMPONENT24 did not resolve: " + t);
            System.out.println();
            System.out.println("CLASSPATH_PROBE_FAIL: " + t.getClass().getName()
                    + " - this is the ORIGINAL 7.2a bug, back at runtime.");
        }
        System.exit(1);
    }

    private static boolean ancestor(Class<?> sub, Class<?> maybeAncestor) {
        for (Class<?> c = sub; c != null; c = c.getSuperclass()) {
            if (c == maybeAncestor) {
                return true;
            }
        }
        return false;
    }
}
