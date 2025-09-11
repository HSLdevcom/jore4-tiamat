package org.rutebanken.tiamat.exporter.async;

import com.google.common.collect.Iterables;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;
import static org.assertj.core.api.Assertions.assertThat;

public class JoreStopPlaceQuayPriorityIteratorTest {

    private static String REAL_LIKE_CASE = """
            ==|AAAAAAABBBBBBBBAAAA|
            10|AAAAAAAAAAAAAAAAAAA|
            20|_______BBBBBBBB____|
            """;

    private static String COMPLEX_ALG_TEST_CASE = """
            ==|_______________________________|
            10|AAAAAAAAAAABBBBBBBBBBCCCCCCCCCC|
            11|__________D__________E___FGHIJ_|
            12|_________________________KKKKK_|
            """;

    private record TestQuayInfo(
            String id,
            LocalDate validityStart,
            LocalDate validityEnd,
            int priority
    ) {}

    private record VersionSpan(String id, int start, int end) {}

    private record TestData(List<TestQuayInfo> expectedTimeline, List<TestQuayInfo> inputVersions) {
        private static final Pattern VERSION_SPAN = Pattern.compile("(?<ID>([A-Z])\2*)");

        public static TestData parse(final String data) {
            final var lines = data.lines().toList();
            assertThat(lines)
                    .as("Test data string should have at least 2 lines of data! Data:\n%s", data)
                    .hasSizeGreaterThanOrEqualTo(2);

            final var versionSpans = parsePriorityLines(data, lines);
        }

        private static List<VersionSpan> parsePriorityLines(
                final String data,
                final List<String> lines) {
            final HashSet<String> ids = new HashSet<>();
            final List<VersionSpan> allVersionSpans = new ArrayList<>();

            for (final String line : Iterables.skip(lines, 1)) {
                final var versionSpans = extractVersionSpans(line);
                allVersionSpans.addAll(versionSpans);

                for (final var version : versionSpans) {
                    assertThat(ids)
                            .as("Test data needs to contain unique ids for each span! Data: %s", data)
                            .doesNotContain(version.id);
                    ids.add(version.id);
                }
            }

            return allVersionSpans;
        }

        private static void assertTestDataLineFormat(final String line) {
            assertThat(line)
                    .as("Test data line (%s) does not match expected format!", line)
                    .matches("^\\d{2}|[A-Z_]+|$");
        }

        private static List<VersionSpan> extractVersionSpans(final String line) {
            final List<VersionSpan> versionSpans = new ArrayList<>();

            final var matcher = VERSION_SPAN.matcher(line);
            while (matcher.find()) {
                // We just need a single letter of the ID span
                final String id = matcher.group("ID").substring(0, 1);
                // Minus priority and separator length.
                final int start = matcher.start("ID") - 3;
                final int end = matcher.end("ID") - 3;

                versionSpans.add(new VersionSpan(id, start, end));
            }

            return versionSpans;
        }
    }
}
