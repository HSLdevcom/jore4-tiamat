package org.rutebanken.tiamat.exporter.async;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import ma.glasnost.orika.CustomConverter;
import ma.glasnost.orika.MapperFacade;
import ma.glasnost.orika.MapperFactory;
import ma.glasnost.orika.MappingContext;
import ma.glasnost.orika.converter.builtin.PassThroughConverter;
import ma.glasnost.orika.impl.DefaultMapperFactory;
import ma.glasnost.orika.metadata.Type;
import org.locationtech.jts.geom.Point;
import org.rutebanken.tiamat.exception.JoreNetextExportException;
import org.rutebanken.tiamat.model.Quay;
import org.rutebanken.tiamat.model.StopPlace;
import org.rutebanken.tiamat.model.TopographicPlace;
import org.rutebanken.tiamat.model.ValidBetween;
import org.rutebanken.tiamat.model.Value;

public class JoreStopPlaceQuayPriorityIterator implements Iterator<StopPlace> {
    // +30 means a draft, we want to completely exclude those.
    private static final int MAX_PRIO = 29;
    private static final List<String> VALID_PRIVATE_CODE_TYPES = List.of("HSL/JORE-3", "HSL/JORE-4");
    private static final ZoneId HELSINKI = ZoneId.of("Europe/Helsinki");

    private final Iterator<StopPlace> sourceIterator;
    private final MapperFacade quayCopyMapper;

    public JoreStopPlaceQuayPriorityIterator(final Iterator<StopPlace> sourceIterator) {
        this.sourceIterator = sourceIterator;

        MapperFactory mapperFactory = new DefaultMapperFactory.Builder().build();

        mapperFactory.getConverterFactory()
                .registerConverter(
                        "stopPlacePassThroughId",
                        new PassThroughConverter(TopographicPlace.class));

        mapperFactory.getConverterFactory()
                .registerConverter(new PassThroughConverter(Point.class));

        mapperFactory.getConverterFactory()
                .registerConverter(new CustomConverter<Instant, Instant>() {
                    @Override
                    public Instant convert(Instant instant, Type<? extends Instant> type, MappingContext mappingContext) {
                        return Instant.from(instant);
                    }
                });

        this.quayCopyMapper = mapperFactory.getMapperFacade();
    }

    @Override
    public boolean hasNext() {
        return this.sourceIterator.hasNext();
    }

    @Override
    public StopPlace next() {
        final var stopPlace = this.sourceIterator.next();

        // Only process JORE StopPlaces
        final var stopPlacePrivateCode = stopPlace.getPrivateCode();
        if (stopPlacePrivateCode == null || !VALID_PRIVATE_CODE_TYPES.contains(stopPlacePrivateCode.getType())) {
            return stopPlace;
        }

        final var flattenedStops = prepareValidStops(stopPlace.getQuays())
                .values()
                .stream()
                .flatMap(JoreStopPlaceQuayPriorityIterator::flattenQuaysBasedOnValidity)
                .map(this::applyFlatValidityToStop)
                .collect(Collectors.toSet());

        stopPlace.setQuays(flattenedStops);

        return stopPlace;
    }

    private Quay applyFlatValidityToStop(QuayAndValidity quayAndValidity) {
        final var copy = quayCopyMapper.map(quayAndValidity.quay(), Quay.class);

        final var validity = quayAndValidity.validity();

        copy.setValidBetween(new ValidBetween(
                validity.validityStart().atStartOfDay(HELSINKI).toInstant(),
                validity.validIndefinitely()
                        ? null
                        : validity.validityEnd().atTime(LocalTime.MAX).atZone(HELSINKI).toInstant()
        ));


        copy.getKeyValues().put("priority", new Value(validity.priority().toString()));
        copy.getKeyValues().put("validityStart", new Value(validity.validityStart().toString()));
        if (validity.validIndefinitely()) {
            copy.getKeyValues().remove("validityEnd");
        } else {
            copy.getKeyValues().put("validityEnd", new Value(validity.validityEnd().toString()));
        }

        return copy;
    }

    private static Map<String, List<QuayAndValidity>> prepareValidStops(final Set<Quay> quays) {
        return quays
                .stream()
                .map(QuayAndValidity::fromQuay)
                .filter((quayAndValidity -> quayAndValidity.validity().priority() <= MAX_PRIO))
                .filter(quayAndValidity ->
                        VALID_PRIVATE_CODE_TYPES.contains(
                                quayAndValidity.quay.getPrivateCode().getType()))
                .sorted(QuayAndValidity.groupByPriorityAndOrderByStartDate())
                .collect(Collectors.groupingBy(
                        // Group by public code
                        (pair) -> pair.quay().getPublicCode()
                ));
    }

    private static Stream<QuayAndValidity> flattenQuaysBasedOnValidity(final List<QuayAndValidity> quays) {
        if (quays.size() <= 1) {
            return quays.stream();
        }

        var flattened = new ArrayList<QuayAndValidity>();

        // Current priority set being processed
        var currentPriority = quays.getFirst().validity().priority();

        for  (final QuayAndValidity next : quays) {
            assertNoSamePriorityOverlaps(flattened, next);

            // No overlaps, and processing the same priority set → Safe to just append
            if (currentPriority.equals(next.validity().priority())) {
                flattened.add(next);
            } else { // Input is sorted -> next priority is higher
                currentPriority = next.validity().priority();
                flattened = spliceQuayIntoFlatTimeline(flattened, next);
            }
        }

        return flattened.stream();
    }

    private static void assertNoSamePriorityOverlaps(final List<QuayAndValidity> flattened,
            final QuayAndValidity next) {
        final var overlapped = flattened
                .stream()
                .filter(existing -> existing.validity().isOverlappedOnDatesBy(next.validity()))
                .findFirst();

        if (overlapped.isPresent()) {
            throw new JoreNetextExportException("Found overlapping stop versions with same priority! " + next.quay() + " | " + overlapped.get());
        }
    }

    private static ArrayList<QuayAndValidity> spliceQuayIntoFlatTimeline(
            final ArrayList<QuayAndValidity> flattened,
            final QuayAndValidity next) {
        if (next.validIndefinitely()) {
            return spliceIndefinitiveValidityPeriodQuayIntoFlatList(flattened, next);
        }

        return spliceClosedValidityPeriodQuayIntoFlatList(flattened, next);
    }

    /**
     * Simple case where the next version fully replaces x versions from the end of the list,
     * and/or possibly partially replaces the last element on the list.
     *
     * @param flattened Existing processing state
     * @param next Next version of higher priority to inder into the flattened stream
     * @return A list of stop version on a flat timeline.
     */
    private static ArrayList<QuayAndValidity> spliceIndefinitiveValidityPeriodQuayIntoFlatList(
            final ArrayList<QuayAndValidity> flattened,
            final QuayAndValidity next) {
        return flattened
                .stream()
                // Filter our those that are complete overridden by the higher prio
                .filter(existing -> !existing.startsAfterOrOnSameDate(next))
                // Splice in the `next` stop, potentially cutting existing ones.
                // Might generate invalid date ranges, cleanup in next step.
                .flatMap(existing -> {
                    // Existing end before the next one starts, no need to split.
                    if (existing.endsBeforeOtherStarts(next)) {
                        return Stream.of(existing);
                    }

                    // Existing overlaps with the next one.
                    // And due to previous asserts and filters this is the last item in `flattened`.
                    // End the existing version before the next one starts.
                    final var newEndDate = next.validity().validityStart().minusDays(1);
                    return Stream.of(existing.withEndDate(newEndDate), next);
                })
                .filter(QuayAndValidity::isValid)
                .collect(Collectors.toCollection((ArrayList::new)));
    }

    /**
     * Complex case where the next version might get inserted in the middle of the existing
     * flat timeline, replacing x-amount of existing fragments, and potentially splitting
     * them into smaller fragments.
     *
     * @param flattened Existing processing state
     * @param next Next version of higher priority to inder into the flattened stream
     * @return A list of stop version on a flat timeline.
     */
    private static ArrayList<QuayAndValidity> spliceClosedValidityPeriodQuayIntoFlatList(
            final ArrayList<QuayAndValidity> flattened,
            final QuayAndValidity next) {
        // Handle cases where the next version overrides some of the existing timeline.
        final var potentiallySpliced = flattened
                .stream()
                // Filter our those that are complete overridden by the higher prio
                .filter(existing ->
                        !(existing.startsAfterOrOnSameDate(next) && existing.endBeforeOrOnSameDate(next))
                )
                // Splice in the `next` stop, potentially cutting existing ones.
                // Not perfect algorithm, can insert the next item multiple times,
                // if there are 2 existing version partially overlapping with the next.
                // Next step is to deduplicate them out.
                // Might generate invalid date ranges, cleanup in next step.
                .flatMap(existing -> {
                    // Existing end before the next one starts, no need to split. OR
                    // Existing starts after the next one end, no need to split.
                    if (existing.endsBeforeOtherStarts(next) || existing.startsAfterOtherEnds(next)) {
                        return Stream.of(existing);
                    }

                    // Existing version starts before and extends past the next version.
                    // Split the existing version into 2 new parts.
                    if (existing.startsBefore(next) && existing.endsAfter(next)) {
                        return Stream.of(
                                new QuayAndValidity(
                                        existing.quay(),
                                        new Validity(
                                                existing.validity().validityStart(),
                                                next.validity().validityStart().minusDays(1),
                                                existing.validity().priority()
                                        )
                                ),
                                next,
                                new QuayAndValidity(
                                        existing.quay(),
                                        new Validity(
                                                next.validity().validityEnd().plusDays(1),
                                                existing.validity().validityEnd(),
                                                existing.validity().priority()
                                        )
                                )
                        );
                    }

                    // Existing version starts before and extends past next
                    // End the existing version before the next one
                    if (existing.startsBefore(next)) {
                        return Stream.of(
                                existing.withEndDate(next.validity().validityStart().minusDays(1)),
                                next
                        );
                    }

                    // Existing version starts during the next version and extends past it.
                    if (existing.endsAfter(next)) {
                        return Stream.of(
                                next,
                                existing.withStartDate(next.validity().validityEnd().plusDays(1))
                        );
                    }

                    throw new JoreNetextExportException(
                            "Unexpected logical branch while splicing new version into the flat stream!" +
                                    "\nExisting: " + existing +
                                    "\nNext: " + next
                    );
                });

        // It is possible that if there is a gap in the timeline and the next version
        // slots into that gap, that is not yet included in `potentiallySpliced` stream.
        // Thus, we need to make sure we have it within the version stream.
        return Stream.concat(potentiallySpliced, Stream.of(next))
                .filter(QuayAndValidity::isValid)
                // Ensure next is at correct position, if it was not spliced into the middle in `potentiallySpliced`
                .sorted(QuayAndValidity.orderByStartDate())
                .distinct() // Cleanup potential duplicates of `next`
                .collect(Collectors.toCollection((ArrayList::new)));
    }

    private record Validity(LocalDate validityStart, LocalDate validityEnd, Integer priority) {
        public static Validity fromQuay(final Quay quay) {
            final var validityStart = findLocalDateKeyValue(quay, "validityStart");
            final var validityEnd = Objects.requireNonNullElse(
                    findLocalDateKeyValue(quay, "validityEnd"),
                    LocalDate.MAX
            );
            final var priority = findIntegerKeyValue(quay, "priority");

            if  (validityStart == null) {
                throw new JoreNetextExportException("Found quay with no validity start time in KeyValues! Quay: " + quay);
            }

            if (priority == null) {
                throw new JoreNetextExportException("Found quay with no priority in KeyValues! Quay: " + quay);
            }

            return new Validity(validityStart, validityEnd, priority);
        }

        private static String findKeyValue(final Quay quay, final String key) {
            final var value = quay.getKeyValues().get(key);

            if (value == null) {
                return null;
            }

            if (value.getItems().size() > 1) {
                throw new JoreNetextExportException("Multiple keyValue values found for key " + key + " while mapping quay: " + quay + "!");
            }

            return value.getItems().stream().findFirst().orElse(null);
        }

        private static LocalDate findLocalDateKeyValue(final Quay quay, final String key) {
            final var str = findKeyValue(quay, key);

            if (str == null) {
                return null;
            }

            try {
                return LocalDate.parse(str);
            } catch (DateTimeParseException e) {
                throw new JoreNetextExportException(
                        "Was unable to parse KeyValue(" + key +"=" + str + ") as a LocalDate!");
            }
        }

        private static Integer findIntegerKeyValue(final Quay quay, final String key) {
            final var str = findKeyValue(quay, key);

            if (str == null) {
                return null;
            }

            try {
                return Integer.parseInt(str);
            } catch (final NumberFormatException e) {
                throw new JoreNetextExportException(
                        "Was unable to parse KeyValue(" + key +"=" + str + ") as an Integer!");
            }
        }

        public boolean validIndefinitely() {
            return this.validityEnd.equals(LocalDate.MAX);
        }

        public boolean isOverlappedOnDatesBy(Validity other) {
            if (!this.priority.equals(other.priority())) {
                return false;
            }

            if (this.validIndefinitely()) {
                return true;
            }

            return !this.validityEnd.isBefore(other.validityStart());
        }
    }

    private record QuayAndValidity(Quay quay, Validity validity) {
        public static QuayAndValidity fromQuay(final Quay quay) {
            return new QuayAndValidity(quay, Validity.fromQuay(quay));
        }

        public QuayAndValidity withStartDate(LocalDate startDate) {
            return new QuayAndValidity(
                    this.quay,
                    new Validity(
                            startDate,
                            this.validity.validityEnd(),
                            this.validity.priority()));
        }

        public QuayAndValidity withEndDate(LocalDate endDate) {
            return new QuayAndValidity(
                    this.quay,
                    new Validity(
                            this.validity.validityStart(),
                            endDate,
                            this.validity.priority()));
        }

        /**
         * Order by validity start date, grouped by priority, with higher prio groups at the end.
         */
        public static Comparator<QuayAndValidity> groupByPriorityAndOrderByStartDate() {
            return (a, b) -> {
                if (a.validity().priority() < b.validity().priority()) {
                    return -1;
                }

                if (a.validity().priority() > b.validity().priority()) {
                    return 1;
                }

                return a.validity().validityStart().compareTo(b.validity().validityStart());
            };
        }

        /**
         * Order by validity start date assuming flat timeline, with no regards to priority.
         */
        public static Comparator<QuayAndValidity> orderByStartDate() {
            return Comparator.comparing(it -> it.validity().validityStart());
        }

        public boolean isValid() {
            // Validity start is NOT after end
            return !this.validity().validityStart().isAfter(this.validity().validityEnd());
        }

        public boolean validIndefinitely() {
            return this.validity.validIndefinitely();
        }

        /**
         * This version starts before the other version does.
         * @param other another version
         * @return test result
         */
        public boolean startsBefore(final QuayAndValidity other) {
            return this.validity.validityStart.isBefore(other.validity.validityStart());
        }

        /**
         * This version starts after, or on the same date as the other version does.
         * @param other another version
         * @return test result
         */
        public boolean startsAfterOrOnSameDate(final QuayAndValidity other) {
            // NOT before → Same or after
            return !this.validity.validityStart.isBefore(other.validity.validityStart());
        }

        /**
         * This version starts after the other version has ended.
         * @param other another version
         * @return test result
         */
        public boolean startsAfterOtherEnds(final QuayAndValidity other) {
            return this.validity.validityEnd.isBefore(other.validity.validityStart());
        }

        /**
         * This version ends after the other version has already ended.
         * @param other another version
         * @return test result
         */
        public boolean endsAfter(final QuayAndValidity other) {
            return this.validity.validityEnd.isAfter(other.validity.validityEnd());
        }

        /**
         * This version end before the other version does.
         * @param other another version
         * @return test result
         */
        public boolean endsBefore(final QuayAndValidity other) {
            return this.validity.validityEnd().isBefore(other.validity.validityEnd());
        }

        /**
         * This version ends before the other version has even started.
         * @param other another version
         * @return test result
         */
        public boolean endsBeforeOtherStarts(final QuayAndValidity other) {
            return this.validity.validityEnd().isBefore(other.validity.validityStart());
        }

        /**
         * This version ends before or on the same date as the other version does.
         * @param other another version
         * @return test result
         */
        public boolean endBeforeOrOnSameDate(final QuayAndValidity other) {
            // NOT after → Same or before
            return !this.validity.validityEnd.isAfter(other.validity.validityEnd());
        }
    }
}
