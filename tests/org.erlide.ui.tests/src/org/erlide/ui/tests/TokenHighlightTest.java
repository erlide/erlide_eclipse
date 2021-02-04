package org.erlide.ui.tests;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.List;

import org.eclipse.swt.graphics.RGB;
import org.erlide.ui.prefs.TokenHighlight;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Enclosed.class)
public class TokenHighlightTest {

    @RunWith(Parameterized.class)
    public static class ParameterizedTokenHighlightTest {

        @Parameters
        public static List<Object[]> colors() {
        // @formatter:off
        return Arrays.asList(new Object[][] {
            { "abc", new RGB(0xaa, 0xbb, 0xcc) },
            { "#a34", new RGB(0xaa, 0x33, 0x44) },
            { "abcdef", new RGB(0xab, 0xcd, 0xef) },
            { "#abc012", new RGB(0xab, 0xc0, 0x12) }
        });
        // @formatter:on
        }

        @Parameter(0)
        public String input;
        @Parameter(1)
        public RGB expected;

        @Test
        public void shouldParseCssStrings() {
            assertThat(TokenHighlight.getRgbFromCss(input), is(expected));
        }
    }

    public static class SimpleTokenHighlightTest {

        @Test(expected = IllegalArgumentException.class)
        public void detectBadColorValue() {
            TokenHighlight.getRgbFromCss("#1234");
        }
    }
}
