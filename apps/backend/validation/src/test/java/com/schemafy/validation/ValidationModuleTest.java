package com.schemafy.validation;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ValidationModuleTest {

  @Test
  void loadsModuleClass() throws Exception {
    Assertions.assertNotNull(Class.forName("com.schemafy.validation.ValidationModule"));
  }

}
