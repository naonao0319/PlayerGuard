package net.nekozouneko.playerguard.scheduler;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchedulerSupportTest {

    @Test
    void isClassPresentReturnsTrueForExistingClass() {
        assertTrue(SchedulerSupport.isClassPresent("java.lang.String"));
    }

    @Test
    void isClassPresentReturnsFalseForMissingClass() {
        assertFalse(SchedulerSupport.isClassPresent("net.nekozouneko.playerguard.NoSuchClass12345"));
    }
}
