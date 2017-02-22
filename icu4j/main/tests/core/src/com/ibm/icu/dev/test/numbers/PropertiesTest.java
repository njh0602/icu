// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.dev.test.numbers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import com.ibm.icu.impl.number.Parse.ParseMode;
import com.ibm.icu.impl.number.Properties;
import com.ibm.icu.impl.number.formatters.CurrencyFormat.CurrencyStyle;
import com.ibm.icu.impl.number.formatters.PaddingFormat.PaddingLocation;
import com.ibm.icu.text.CompactDecimalFormat.CompactStyle;
import com.ibm.icu.text.CurrencyPluralInfo;
import com.ibm.icu.text.MeasureFormat.FormatWidth;
import com.ibm.icu.util.Currency;
import com.ibm.icu.util.Currency.CurrencyUsage;
import com.ibm.icu.util.MeasureUnit;
import com.ibm.icu.util.ULocale;

public class PropertiesTest {

  @Test
  public void testBasicEquals() {
    Properties p1 = new Properties();
    Properties p2 = new Properties();
    assertEquals(p1, p2);

    p1.setPositivePrefix("abc");
    assertNotEquals(p1, p2);
    p2.setPositivePrefix("xyz");
    assertNotEquals(p1, p2);
    p1.setPositivePrefix("xyz");
    assertEquals(p1, p2);
  }

  @Test
  public void testFieldCoverage() {
    Properties p1 = new Properties();
    Properties p2 = new Properties();
    Properties p3 = new Properties();
    Properties p4 = new Properties();

    Set<Integer> hashCodes = new HashSet<>();
    Field[] fields = Properties.class.getDeclaredFields();
    for (Field field : fields) {
      if (Modifier.isStatic(field.getModifiers())) {
        continue;
      }

      // Check for getters and setters
      String fieldNamePascalCase =
          Character.toUpperCase(field.getName().charAt(0)) + field.getName().substring(1);
      String getterName = "get" + fieldNamePascalCase;
      String setterName = "set" + fieldNamePascalCase;
      Method getter, setter;
      try {
        getter = Properties.class.getMethod(getterName);
        assertEquals(
            "Getter does not return correct type", field.getType(), getter.getReturnType());
      } catch (NoSuchMethodException e) {
        fail("Could not find method " + getterName + " for field " + field);
        continue;
      } catch (SecurityException e) {
        fail("Could not access method " + getterName + " for field " + field);
        continue;
      }
      try {
        setter = Properties.class.getMethod(setterName, field.getType());
        assertEquals(
            "Method " + setterName + " does not return correct type",
            Properties.class,
            setter.getReturnType());
      } catch (NoSuchMethodException e) {
        fail("Could not find method " + setterName + " for field " + field);
        continue;
      } catch (SecurityException e) {
        fail("Could not access method " + setterName + " for field " + field);
        continue;
      }

      try {
        // Check for getter, equals, and hash code behavior
        Object val0 = getSampleValueForType(field.getType(), 0);
        Object val1 = getSampleValueForType(field.getType(), 1);
        Object val2 = getSampleValueForType(field.getType(), 2);
        assertNotEquals(val0, val1);
        setter.invoke(p1, val0);
        setter.invoke(p2, val0);
        assertEquals(p1, p2);
        assertEquals(p1.hashCode(), p2.hashCode());
        assertEquals(getter.invoke(p1), getter.invoke(p2));
        assertEquals(getter.invoke(p1), val0);
        assertNotEquals(getter.invoke(p1), val1);
        hashCodes.add(p1.hashCode());
        setter.invoke(p1, val1);
        assertNotEquals("Field " + field + " is missing from equals()", p1, p2);
        assertNotEquals(getter.invoke(p1), getter.invoke(p2));
        assertNotEquals(getter.invoke(p1), val0);
        assertEquals(getter.invoke(p1), val1);
        setter.invoke(p1, val0);
        assertEquals("Field " + field + " setter might have side effects", p1, p2);
        assertEquals(p1.hashCode(), p2.hashCode());
        assertEquals(getter.invoke(p1), getter.invoke(p2));
        setter.invoke(p1, val1);
        setter.invoke(p2, val1);
        assertEquals(p1, p2);
        assertEquals(p1.hashCode(), p2.hashCode());
        assertEquals(getter.invoke(p1), getter.invoke(p2));
        setter.invoke(p1, val2);
        setter.invoke(p1, val1);
        assertEquals("Field " + field + " setter might have side effects", p1, p2);
        assertEquals(p1.hashCode(), p2.hashCode());
        assertEquals(getter.invoke(p1), getter.invoke(p2));
        hashCodes.add(p1.hashCode());

        // Check for clone behavior
        Properties copy = p1.clone();
        assertEquals("Field " + field + " did not get copied in clone", p1, copy);
        assertEquals(p1.hashCode(), copy.hashCode());
        assertEquals(getter.invoke(p1), getter.invoke(copy));

        // Check for copyFrom behavior
        setter.invoke(p1, val0);
        assertNotEquals(p1, p2);
        assertNotEquals(getter.invoke(p1), getter.invoke(p2));
        p2.copyFrom(p1);
        assertEquals("Field " + field + " is missing from copyFrom()", p1, p2);
        assertEquals(p1.hashCode(), p2.hashCode());
        assertEquals(getter.invoke(p1), getter.invoke(p2));

        // Load values into p3 and p4 for clear() behavior test
        setter.invoke(p3, getSampleValueForType(field.getType(), 3));
        hashCodes.add(p3.hashCode());
        setter.invoke(p4, getSampleValueForType(field.getType(), 4));
        hashCodes.add(p4.hashCode());
      } catch (IllegalAccessException e) {
        fail("Could not access method for field " + field);
      } catch (IllegalArgumentException e) {
        fail("Could call method for field " + field);
      } catch (InvocationTargetException e) {
        fail("Could invoke method on target for field " + field);
      }
    }

    // Check for clear() behavior
    assertNotEquals(p3, p4);
    p3.clear();
    p4.clear();
    assertEquals("A field is missing from the clear() function", p3, p4);

    // A good hashCode() implementation should produce very few collisions.  We added at most
    // 4*fields.length codes to the set.  We'll say the implementation is good if we had at least
    // fields.length unique values.
    // TODO: Should the requirement be stronger than this?
    assertTrue(
        "Too many hash code collisions: " + hashCodes.size() + " out of " + (fields.length * 4),
        hashCodes.size() >= fields.length);
  }

  /**
   * Creates a valid sample instance of the given type. Used to simulate getters and setters.
   *
   * @param type The type to generate.
   * @param seed An integer seed, guaranteed to be positive. The same seed should generate two
   *     instances that are equal. A different seed should in general generate two instances that
   *     are not equal; this might not always be possible, such as with booleans or enums where
   *     there are limited possible values.
   * @return An instance of the specified type.
   */
  Object getSampleValueForType(Class<?> type, int seed) {
    if (type == Integer.TYPE) {
      return seed * 1000001;

    } else if (type == Boolean.TYPE) {
      return (seed % 2) == 0;

    } else if (type == BigDecimal.class) {
      if (seed == 0) return null;
      return new BigDecimal(seed * 1000002);

    } else if (type == CharSequence.class) {
      if (seed == 0) return null;
      return BigInteger.valueOf(seed * 1000003).toString(32);

    } else if (type == CompactStyle.class) {
      if (seed == 0) return null;
      CompactStyle[] values = CompactStyle.values();
      return values[seed % values.length];

    } else if (type == Currency.class) {
      if (seed == 0) return null;
      Object[] currencies = Currency.getAvailableCurrencies().toArray();
      return currencies[seed % currencies.length];

    } else if (type == CurrencyPluralInfo.class) {
      if (seed == 0) return null;
      ULocale[] locales = ULocale.getAvailableLocales();
      return CurrencyPluralInfo.getInstance(locales[seed % locales.length]);

    } else if (type == CurrencyStyle.class) {
      if (seed == 0) return null;
      CurrencyStyle[] values = CurrencyStyle.values();
      return values[seed % values.length];

    } else if (type == CurrencyUsage.class) {
      if (seed == 0) return null;
      CurrencyUsage[] values = CurrencyUsage.values();
      return values[seed % values.length];

    } else if (type == FormatWidth.class) {
      if (seed == 0) return null;
      FormatWidth[] values = FormatWidth.values();
      return values[seed % values.length];

    } else if (type == MathContext.class) {
      if (seed == 0) return null;
      RoundingMode[] modes = RoundingMode.values();
      return new MathContext(seed, modes[seed % modes.length]);

    } else if (type == MeasureUnit.class) {
      if (seed == 0) return null;
      Object[] units = MeasureUnit.getAvailable().toArray();
      return units[seed % units.length];

    } else if (type == PaddingLocation.class) {
      if (seed == 0) return null;
      PaddingLocation[] values = PaddingLocation.values();
      return values[seed % values.length];

    } else if (type == ParseMode.class) {
      if (seed == 0) return null;
      ParseMode[] values = ParseMode.values();
      return values[seed % values.length];

    } else if (type == RoundingMode.class) {
      if (seed == 0) return null;
      RoundingMode[] values = RoundingMode.values();
      return values[seed % values.length];

    } else {
      fail("Don't know how to handle type " + type + ". Please add it to getSampleValueForType().");
      return null;
    }
  }
}
