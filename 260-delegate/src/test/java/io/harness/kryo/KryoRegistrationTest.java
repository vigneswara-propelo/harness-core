/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.kryo;

import static io.harness.rule.OwnerRule.JOHANNES;

import static java.lang.Integer.parseInt;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.packages.HarnessPackages;
import io.harness.rule.Owner;
import io.harness.serializer.KryoRegistrar;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Registration;
import com.esotericsoftware.kryo.util.DefaultClassResolver;
import com.esotericsoftware.kryo.util.IntMap;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.reflections.Reflections;

public class KryoRegistrationTest extends CategoryTest {
  private static final String KRYO_REGISTRATION_FILE = "kryo-registrations.txt";

  private static void log(String message) {
    System.out.println(message);
  }

  /**
   * Compares all registered classes found during runtime with the expected registrations defined in
   * kryo-registrations.txt. This test was added to ensure that when moving classes between packages, one does not
   * remove it from the source registrar and forget to add it to the destination registrar.
   */
  @Test
  @Owner(developers = JOHANNES)
  @Category(UnitTests.class)
  public void testKryoRegistrarForUnexpectedChanges()
      throws IOException, InstantiationException, IllegalAccessException, NoSuchFieldException {
    log("Get all classes registered with Kryo.");
    SortedMap<Integer, String> registeredClasses = getAllClassesRegisteredWithKryo();

    log(String.format("Load expected kryo registrations from resources file '%s'.", KRYO_REGISTRATION_FILE));
    Map<Integer, String> expectedRegisteredClasses = loadAllExpectedKryoRegistrations();

    // ensure all registered classes match the expected registrations
    for (Map.Entry<Integer, String> registeredEntry : registeredClasses.entrySet()) {
      assertThat(expectedRegisteredClasses.containsKey(registeredEntry.getKey()))
          .withFailMessage(
              "Found new registration with id '%d' that doesn't exist in list of expected registrations (class: '%s')",
              registeredEntry.getKey(), registeredEntry.getValue())
          .isEqualTo(true);
      assertThat(expectedRegisteredClasses.get(registeredEntry.getKey()))
          .withFailMessage("Found registration with id '%d' for class '%s' which doesn't match expected class '%s'.",
              registeredEntry.getKey(), registeredEntry.getValue(),
              expectedRegisteredClasses.get(registeredEntry.getKey()))
          .isEqualTo(registeredEntry.getValue());
    }

    // ensure all entries in registration file are registered
    for (Map.Entry<Integer, String> expectedRegisteredEntry : expectedRegisteredClasses.entrySet()) {
      assertThat(registeredClasses.containsKey(expectedRegisteredEntry.getKey()))
          .withFailMessage("Expected registration with id '%d' wasn't found (class: '%s').",
              expectedRegisteredEntry.getKey(), expectedRegisteredEntry.getValue())
          .isEqualTo(true);
    }
  }

  private static SortedMap<Integer, String> getAllClassesRegisteredWithKryo()
      throws InstantiationException, IllegalAccessException, NoSuchFieldException {
    Kryo kryo = new Kryo();
    SortedMap<Integer, String> registeredClasses = new TreeMap<>();

    log("Load all registrar classes.");

    Set<Class<? extends KryoRegistrar>> registrarClasses = getAllKryoRegistrars();
    for (Class<? extends KryoRegistrar> registrarClass : registrarClasses) {
      log(String.format("Loading registrar '%s'.", registrarClass.getName()));
      registrarClass.newInstance().register(kryo);
    }

    log("Extract all registered classes from kryo.");

    // We need to access a private field from the kryo resolver in order to get all registrations.
    DefaultClassResolver classResolver = (DefaultClassResolver) kryo.getClassResolver();
    Field idToRegistrationField = classResolver.getClass().getDeclaredField("idToRegistration");
    idToRegistrationField.setAccessible(true);
    IntMap<Registration> idToRegistration = (IntMap<Registration>) idToRegistrationField.get(classResolver);

    IntMap.Keys registeredKyroIds = idToRegistration.keys();
    while (registeredKyroIds.hasNext) {
      int registrationId = registeredKyroIds.next();
      Registration registration = kryo.getRegistration(registrationId);
      registeredClasses.put(registration.getId(), registration.getType().getName());
    }

    return registeredClasses;
  }

  private static Set<Class<? extends KryoRegistrar>> getAllKryoRegistrars() {
    Set<Class<? extends KryoRegistrar>> result = new HashSet<>();
    Reflections reflections =
        new Reflections(HarnessPackages.IO_HARNESS, HarnessPackages.SOFTWARE_WINGS, "io.serializer");
    result.addAll(reflections.getSubTypesOf(KryoRegistrar.class));
    return result;
  }

  private static Map<Integer, String> loadAllExpectedKryoRegistrations() throws IOException {
    Map<Integer, String> expectedRegisteredClasses = new HashMap<>();
    List<String> lines =
        IOUtils.readLines(KryoRegistrationTest.class.getClassLoader().getResourceAsStream(KRYO_REGISTRATION_FILE),
            StandardCharsets.UTF_8);
    int previousId = -1;
    for (String line : lines) {
      int firstIdx = line.indexOf(':');
      int lastIdx = line.lastIndexOf(':');

      // ignore lines without entries or multiple colons
      if (firstIdx == -1 || firstIdx != lastIdx) {
        continue;
      }

      Integer id = parseInt(line.substring(0, line.indexOf(':')));
      String name = line.substring(line.indexOf(':') + 1).trim();

      assertThat(id)
          .withFailMessage("Found entry with id %d after entry with id %d."
                  + "Please ensure the entries in '%s' are in ascending order and there are no duplicates.",
              id, previousId, KRYO_REGISTRATION_FILE)
          .isGreaterThan(previousId);

      expectedRegisteredClasses.put(id, name);

      previousId = id;
    }

    return expectedRegisteredClasses;
  }
}
