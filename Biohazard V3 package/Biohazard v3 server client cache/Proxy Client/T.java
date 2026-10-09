public class T {
  public static void main(String[] a){
    // Reproduce the software's row rule: row = (jRaw & 0x3f80) >> 7  (Texture.java:1995)
    // against the texel index our shader would compute for the same value.
    int[] texels = {194,167,256,-5,136,140,131,108,66,-18};
    System.out.println("texel  software row ((raw & 0x3f80)>>7)   GL clamp   GL wrap ((t%128)+128)%128");
    for (int t : texels){
      int raw = t*128;
      int swRow = (raw & 0x3f80) >> 7;
      int clamp = Math.max(0, Math.min(127, t));
      int wrap = ((t % 128) + 128) % 128;
      System.out.printf("%6d %8d %26d %10d %8d %s%n", t, swRow, swRow, clamp, wrap,
        (swRow==wrap ? "wrap MATCHES" : "WRAP MISMATCH") + (swRow==clamp ? " ; clamp also ok" : ""));
    }
  }
}
