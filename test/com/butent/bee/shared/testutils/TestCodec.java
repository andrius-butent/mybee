package com.butent.bee.shared.testutils;

import com.butent.bee.shared.Pair;
import com.butent.bee.shared.utils.Codec;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests {@link com.butent.bee.shared.utils.Codec}.
 */
@SuppressWarnings("static-method")
public class TestCodec {

  @Before
  public void setUp() throws Exception {
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public final void testAdler32ByteArray() {
    byte[] btExpected1 = {0, 97, 0, 98, 0, 99};
    byte[] btExpected2 = {0, 113, 0, 119, 0, 101};
    byte[] btExpected4 = {97, 98, 99};

    assertEquals(Integer.toHexString(38600999), Codec.adler32(btExpected4));
    assertEquals("3740127", Codec.adler32(btExpected1));
    assertEquals("405014e", Codec.adler32(btExpected2));
  }

  @Test
  public final void testAdler32String() {
    assertEquals("20350398", Codec.adler32("Wikipedia"));
    assertEquals("3740127", Codec.adler32("abc"));
    assertEquals("405014e", Codec.adler32("qwe"));
  }

  @Test
  public final void testCrc16ByteArray() {
    byte[] btExpected1 = {0, 97, 0, 98, 0, 99};
    byte[] btExpected2 = {0, 113, 0, 119, 0, 101};

    assertEquals("3f5c", Codec.crc16(btExpected1));
    assertEquals("3a0c", Codec.crc16(btExpected2));
  }

  @Test
  public final void testCrc16String() {
    assertEquals("3f5c", Codec.crc16("abc"));
    assertEquals("3a0c", Codec.crc16("qwe"));
  }

  @Test
  public final void testCrc32ByteArray() {
    byte[] btExpected1 = {0, 97, 0, 98, 0, 99};
    byte[] btExpected2 = {0, 113, 0, 119, 0, 101};

    assertEquals("8a78d0f2", Codec.crc32(btExpected1));
    assertEquals("191683de", Codec.crc32(btExpected2));
  }

  @Test
  public final void testCrc32DirectByteArray() {
    byte[] btExpected1 = {0, 97, 0, 98, 0, 99};
    byte[] btExpected2 = {0, 113, 0, 119, 0, 101};

    assertEquals("8a78d0f2", Codec.crc32Direct(btExpected1));
    assertEquals("191683de", Codec.crc32Direct(btExpected2));
  }

  @Test
  public final void testCrc32DirectString() {
    assertEquals("8a78d0f2", Codec.crc32Direct("abc"));
    assertEquals("191683de", Codec.crc32Direct("qwe"));
  }

  @Test
  public final void testCrc32String() {
    assertEquals("8a78d0f2", Codec.crc32("abc"));
    assertEquals("191683de", Codec.crc32("qwe"));
  }

  @Test
  public final void testDecodeBase64() {
    String g = "";
    for (int i = 0; i < 240; i++) {
      g += "A string";
    }
    String fc = Codec.encodeBase64(g);
    assertEquals(g, Codec.decodeBase64(fc));

    assertEquals("A string", Codec.decodeBase64("AEEAIABzAHQAcgBpAG4AZw=="));
  }

  @Test
  public final void testDeserializeLength() {

    Pair<Integer, Integer> a = Pair.of(53, 3);
    Pair<Integer, Integer> b = Pair.of(8, 2);

    assertEquals(a.getA(), Codec.deserializeLength("253", 0).getA());
    assertEquals(a.getB(), Codec.deserializeLength("253", 0).getB());

    assertEquals(b.getA(), Codec.deserializeLength("18A String", 0).getA());
    assertEquals(b.getB(), Codec.deserializeLength("18A String", 0).getB());

    assertEquals((Object) 0, Codec.deserializeLength("08A String", 0).getA());
    assertEquals((Object) 1, Codec.deserializeLength("08A String", 0).getB());
  }

  @Test
  public final void testEncodeBase64() {
    String longBase64 = "";
    String longString = "";

    for (int i = 0; i < 2000; i++) {
      longBase64 += "AGEAYgBj";
      longString += "abc";
    }

    assertEquals(longBase64, Codec.encodeBase64(longString));
    assertEquals("AGE=", Codec.encodeBase64("a"));
  }

  @Test
  public final void testEscapeHtml() {
    assertEquals("", Codec.escapeHtml(""));
    assertEquals("", Codec.escapeHtml(null));
    assertEquals("This is an &amp;", Codec.escapeHtml("This is an &"));
    assertEquals("A  A", Codec.escapeHtml("A  A"));
  }

  @Test
  public final void testEscapeUnicode() {

    assertEquals("", Codec.escapeUnicode(""));
    assertEquals("", Codec.escapeUnicode(null));

    assertEquals("null char &#x0;", Codec.escapeUnicode("null char \u0000"));
    assertEquals("Backspace &#x8; character",
        Codec.escapeUnicode("Backspace \u0008 character"));
    assertEquals("+ ", Codec.escapeUnicode("+ "));
    assertEquals("normal string", Codec.escapeUnicode("normal string"));
  }

  @Test
  public final void testFromBase64() {
    byte[] btExpected1 = {105, -73, 28};
    byte[] btExpected2 = {97};
    byte[] btExpected3 = {83, 116, 114, 105, 110, 103};

    assertArrayEquals(btExpected1, Codec.fromBase64("abcc"));
    assertArrayEquals(btExpected2, Codec.fromBase64("YQ=="));
    assertArrayEquals(btExpected3, Codec.fromBase64("U3RyaW5n"));
  }

  @Test
  public final void testFromBytes() {
    byte[] btExpected2 = {0, 97};
    byte[] btExpected3 = {0, 83, 0, 116, 0, 114, 0, 105, 0, 110, 0, 103};

    assertEquals("a", Codec.fromBytes(btExpected2));
    assertEquals("String", Codec.fromBytes(btExpected3));
  }

  @Test
  public final void testIsValidUnicodeChar() {
    assertEquals(true, Codec.isValidUnicodeChar('a'));
    assertEquals(false, Codec.isValidUnicodeChar('\u0000'));
    assertEquals(true, Codec.isValidUnicodeChar('\u0021'));
    assertEquals(false, Codec.isValidUnicodeChar('\u007f'));
    assertEquals(false, Codec.isValidUnicodeChar('\u028d'));
    assertEquals(true, Codec.isValidUnicodeChar('\u0244'));
    assertEquals(true, Codec.isValidUnicodeChar('\u0415'));
  }

  @Test
  public final void testMd5() {
    assertEquals("eebae6b863620dc2e7f2bc7754bda625", Codec.md5("A string"));
    assertEquals("8c327a8598a4596fb9ae0046a12e7db2", Codec.md5("Check"));
  }

  @Test
  public final void testSerializeLength() {
    assertEquals("0", Codec.serializeLength(0));
    assertEquals("15", Codec.serializeLength(5));
    assertEquals("215", Codec.serializeLength(15));
  }

  @Test
  public final void testSerializeWithLength() {

    StringBuilder sb = new StringBuilder();
    Codec.serializeWithLength(sb, null);
    assertEquals("0", sb.toString());

    StringBuilder sb2 = new StringBuilder();
    Codec.serializeWithLength(sb2, "Hello world");
    assertEquals("211Hello world", sb2.toString());

    Codec.serializeWithLength(sb2, "A string");
    assertEquals("211Hello world18A string", sb2.toString());

    Codec.serializeWithLength(sb2, "    ");
    assertEquals("211Hello world18A string14    ", sb2.toString());

    StringBuilder sb3 = new StringBuilder();
    Codec.serializeWithLength(sb3, "");
    assertEquals("0", sb3.toString());
  }

  @Test
  public final void testToBytesString() {
    byte[] btExpected1 = {0, 32, 0, 33, 0, 35};
    byte[] btExpected2 = {0, 113, 0, 119, 0, 101, 0, 32, 0, 33, 0, 35};
    byte[] btExpected3 = {0, 113, 0, 119, 0, 101, 0, 32, 0, 33, 0, 35, 0, 101};

    assertArrayEquals(btExpected1, Codec.toBytes(" !#"));
    assertArrayEquals(btExpected2, Codec.toBytes("qwe !#"));
    assertArrayEquals(btExpected3, Codec.toBytes("qwe !#e"));
  }

  @Test
  public final void testToBytesStringInt() {
    byte[] btExpected1 = {0, 32, 0, 33, 0, 35};
    byte[] btExpected2 = {0, 101, 0, 32, 0, 33, 0, 35, 0, 101};

    assertArrayEquals(btExpected1, Codec.toBytes(" !#", 0));
    assertArrayEquals(btExpected1, Codec.toBytes("qwe !#", 3));
    assertArrayEquals(btExpected2, Codec.toBytes("qwe !#e", 2));
  }

  @Test
  public final void testToBytesStringIntInt() {
    byte[] btExpected1 = {0, 32, 0, 33, 0, 35};
    byte[] btExpected2 = {0, 33, 0, 35};

    assertArrayEquals(btExpected1, Codec.toBytes(" !#", 0, 3));
    assertArrayEquals(btExpected2, Codec.toBytes(" !#", 1, 3));
    assertArrayEquals(btExpected1, Codec.toBytes("qwe !#erty", 3, 6));
    assertArrayEquals(btExpected1, Codec.toBytes("qwe !#", 3, 6));
  }

  @Test
  public final void testToHexByteArray() {

    byte[] bt = {0, 32, 0, 33, 0, 35};
    byte[] bt2 = null;
    byte[] bt3 = {};

    assertEquals("002000210023", Codec.toHex(bt));
    assertEquals(null, Codec.toHex(bt3));
    assertEquals(null, Codec.toHex(bt2));
  }

  @Test
  public final void testToHexChar() {

    assertEquals("0063", Codec.toHex('c'));
    assertEquals("0020", Codec.toHex(' '));
    assertEquals("0023", Codec.toHex('\u0023'));
  }

  @Test
  public final void testToHexCharArray() {

    char[] mas = {'a', 'b', 'c'};
    char[] mas2 = {};
    char[] mas3 = {' '};
    char[] mas4 = null;

    assertEquals("006100620063", Codec.toHex(mas));
    assertEquals(null, Codec.toHex(mas2));
    assertEquals("0020", Codec.toHex(mas3));
    assertEquals(null, Codec.toHex(mas4));
  }
}
