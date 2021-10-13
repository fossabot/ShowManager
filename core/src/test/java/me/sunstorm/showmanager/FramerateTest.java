package me.sunstorm.showmanager;

import me.sunstorm.showmanager.util.Framerate;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class FramerateTest {

    @Test
    public void testValidation() {
        assertThrows(IllegalArgumentException.class, () -> Framerate.set(32));
    }
}
