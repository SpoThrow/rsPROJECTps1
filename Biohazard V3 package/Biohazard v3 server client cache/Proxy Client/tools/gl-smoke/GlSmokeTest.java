import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

/**
 * Phase 7.1 smoke test, CORE-PROFILE version.
 *
 * The first attempt proved: hidden window + context, default-framebuffer readback, and a
 * COMPLETE offscreen FBO with depth - but then ABORTED at glMatrixMode, because GLFW gave a
 * CORE profile at 3.3 and fixed-function does not exist there. That is itself a finding:
 * a Phase 7 port cannot use GL 2.x fixed-function.
 *
 * So this version drives the path 7.2 would actually use - shader program + VBO + FBO - which
 * makes it stronger evidence than the immediate-mode version would have been:
 *   1. a core-profile context on a HIDDEN window (no AWT parent, no Display.setParent),
 *   2. GLSL compiles and links,
 *   3. a vertex buffer draws into the offscreen FBO,
 *   4. glReadPixels returns what was drawn, and an UNDRAWN pixel does NOT (the control),
 *   5. the FBO's depth buffer discriminates - a nearer triangle wins at the same pixel.
 */
public class GlSmokeTest {

    private static final int W = 765;
    private static final int H = 503;

    private static final String VS =
            "#version 330 core\n"
            + "layout(location = 0) in vec3 aPos;\n"
            + "void main() { gl_Position = vec4(aPos, 1.0); }\n";

    private static final String FS =
            "#version 330 core\n"
            + "uniform vec4 uColor;\n"
            + "out vec4 fragColor;\n"
            + "void main() { fragColor = uColor; }\n";

    public static void main(String[] args) {
        if (!GLFW.glfwInit()) {
            System.out.println("glfwInit FAILED");
            System.exit(1);
        }

        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
        long win = GLFW.glfwCreateWindow(W, H, "biohazard-gl-smoke", 0, 0);
        if (win == 0) {
            System.out.println("glfwCreateWindow FAILED (hidden window)");
            GLFW.glfwTerminate();
            System.exit(1);
        }
        GLFW.glfwMakeContextCurrent(win);
        GL.createCapabilities();

        System.out.println("GL_VERSION  : " + GL11.glGetString(GL11.GL_VERSION));
        System.out.println("GL_RENDERER : " + GL11.glGetString(GL11.GL_RENDERER));
        System.out.println("GLSL        : " + GL11.glGetString(GL20.GL_SHADING_LANGUAGE_VERSION));
        System.out.println("window      : HIDDEN, standalone HWND - NOT setParent, NO AWT parent");
        System.out.println();

        boolean ok = true;

        // ---- 2: shader program
        int program = buildProgram();
        ok &= program != 0;
        System.out.println("shader program      : " + (program != 0 ? "COMPILED + LINKED" : "FAILED"));
        if (program == 0) {
            finish(win, false);
        }

        // ---- 3: the offscreen target
        int fbo = GL30.glGenFramebuffers();
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo);
        int colorRb = GL30.glGenRenderbuffers();
        GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, colorRb);
        GL30.glRenderbufferStorage(GL30.GL_RENDERBUFFER, GL11.GL_RGBA8, W, H);
        GL30.glFramebufferRenderbuffer(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0,
                GL30.GL_RENDERBUFFER, colorRb);
        int depthRb = GL30.glGenRenderbuffers();
        GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, depthRb);
        GL30.glRenderbufferStorage(GL30.GL_RENDERBUFFER, GL30.GL_DEPTH_COMPONENT24, W, H);
        GL30.glFramebufferRenderbuffer(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT,
                GL30.GL_RENDERBUFFER, depthRb);

        int status = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
        boolean complete = status == GL30.GL_FRAMEBUFFER_COMPLETE;
        System.out.println("FBO status          : " + (complete ? "COMPLETE (RGBA8 + depth24)"
                : "INCOMPLETE 0x" + Integer.toHexString(status)));
        ok &= complete;

        int vao = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vao);
        int vbo = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL30.glEnableVertexAttribArray(0);
        GL30.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 3 * Float.BYTES, 0);

        GL11.glViewport(0, 0, W, H);
        GL11.glEnable(GL11.GL_DEPTH_TEST);

        // ---- 4: draw + readback + control
        GL11.glClearColor(0f, 0f, 0f, 1f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        drawTriangle(program, 100, 100, 400, 100, 250, 400, 0.0f, 1f, 0f, 0f);
        GL11.glFinish();

        int[] inside = readPixel(250, 200);
        int[] outside = readPixel(20, 20);
        System.out.println("FBO inside triangle : r=" + inside[0] + " g=" + inside[1] + " b=" + inside[2]
                + "   (expect ~255,0,0)");
        System.out.println("FBO outside triangle: r=" + outside[0] + " g=" + outside[1] + " b=" + outside[2]
                + "   (expect ~0,0,0)");
        ok &= inside[0] > 240 && inside[1] < 15 && inside[2] < 15;
        ok &= outside[0] < 15 && outside[1] < 15 && outside[2] < 15;

        boolean controlHeld = outside[0] < 15 && outside[1] < 15 && outside[2] < 15;
        System.out.println("readback control    : " + (controlHeld
                ? "an UNDRAWN pixel is NOT the drawn colour (good - readback discriminates)"
                : "FAILED - readback returns something constant, so it proves nothing"));
        ok &= controlHeld;

        // ---- 5: depth discrimination in the FBO
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        drawTriangle(program, 100, 100, 400, 100, 250, 400, 0.3f, 0f, 1f, 0f);    // green, FARTHER
        drawTriangle(program, 100, 100, 400, 100, 250, 400, -0.3f, 0f, 0f, 1f);   // blue, NEARER
        GL11.glFinish();
        int[] nearer = readPixel(250, 200);
        System.out.println("depth test          : green@z=+0.3 then blue@z=-0.3 -> r=" + nearer[0]
                + " g=" + nearer[1] + " b=" + nearer[2] + "   (expect ~0,0,255: the NEARER one)");
        ok &= nearer[2] > 240 && nearer[1] < 15 && nearer[0] < 15;

        int err = GL11.glGetError();
        System.out.println("glGetError          : " + (err == GL11.GL_NO_ERROR ? "GL_NO_ERROR"
                : "0x" + Integer.toHexString(err)));
        ok &= err == GL11.GL_NO_ERROR;

        finish(win, ok);
    }

    /** Uploads one triangle (pixel coords + ndc z) and draws it in a flat colour. */
    private static void drawTriangle(int program, float x1, float y1, float x2, float y2,
                                     float x3, float y3, float z, float r, float g, float b) {
        float[] verts = {
            ndcX(x1), ndcY(y1), z,
            ndcX(x2), ndcY(y2), z,
            ndcX(x3), ndcY(y3), z,
        };
        FloatBuffer fb = BufferUtils.createFloatBuffer(9);
        fb.put(verts).flip();
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, fb, GL15.GL_DYNAMIC_DRAW);
        GL20.glUseProgram(program);
        GL20.glUniform4f(GL20.glGetUniformLocation(program, "uColor"), r, g, b, 1f);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 3);
    }

    private static float ndcX(float x) {
        return (x / W) * 2f - 1f;
    }

    private static float ndcY(float y) {
        return (y / H) * 2f - 1f;
    }

    private static int buildProgram() {
        int vs = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
        GL20.glShaderSource(vs, VS);
        GL20.glCompileShader(vs);
        if (GL20.glGetShaderi(vs, GL20.GL_COMPILE_STATUS) == 0) {
            System.out.println("VS compile failed: " + GL20.glGetShaderInfoLog(vs));
            return 0;
        }
        int fs = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
        GL20.glShaderSource(fs, FS);
        GL20.glCompileShader(fs);
        if (GL20.glGetShaderi(fs, GL20.GL_COMPILE_STATUS) == 0) {
            System.out.println("FS compile failed: " + GL20.glGetShaderInfoLog(fs));
            return 0;
        }
        int p = GL20.glCreateProgram();
        GL20.glAttachShader(p, vs);
        GL20.glAttachShader(p, fs);
        GL20.glLinkProgram(p);
        if (GL20.glGetProgrami(p, GL20.GL_LINK_STATUS) == 0) {
            System.out.println("link failed: " + GL20.glGetProgramInfoLog(p));
            return 0;
        }
        return p;
    }

    private static int[] readPixel(int x, int y) {
        ByteBuffer b = BufferUtils.createByteBuffer(4);
        GL11.glReadPixels(x, y, 1, 1, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, b);
        return new int[] { b.get(0) & 0xff, b.get(1) & 0xff, b.get(2) & 0xff };
    }

    private static void finish(long win, boolean ok) {
        GLFW.glfwDestroyWindow(win);
        GLFW.glfwTerminate();
        System.out.println();
        System.out.println(ok ? "GL_SMOKE_OK" : "GL_SMOKE_FAIL");
        System.exit(ok ? 0 : 1);
    }

    private GlSmokeTest() {
    }
}
