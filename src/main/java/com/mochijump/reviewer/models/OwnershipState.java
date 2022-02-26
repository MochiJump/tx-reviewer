package com.mochijump.reviewer.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class OwnershipState {
    private Map<String, EntryTreeSet<Entry>> inFlux = new HashMap<>();
    private EntryTreeSet<EntryWithVerb> masterSet = new EntryTreeSet<>();
    private List<IrsFormat> shortTermIrsFormatList = new ArrayList<>();
    private List<IrsFormat> longTermIrsFormatList = new ArrayList<>();
    private Double ShortTermGains = 0D;
    private Double LongTermGains = 0D;
    private String wallet = "do not leave blank";

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class IrsFormat {
        private String description;
        private Instant dateAcquired;
        private Instant dateSold;
        private Double proceeds;
        private Double cost;
        private Double gain;
    }


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Entry {
        private Instant time;
        private Double amount;
        private Double value;
    }

    @Data
    public static class EntryWithVerb extends Entry {
        private String coinName;
        private Verb verb;
        private boolean isLpToken;

        public EntryWithVerb(
                final Instant time, final Double amount, final Double value, final Verb verb, final String coinName) {
            super(time, amount, value);
            this.verb = verb;
            this.coinName = coinName;
        }
    }

    public enum Verb {
        BUY, SELL, LOSE
    }

    public static class EntryComparator implements Comparator<Entry> {

        @Override
        public int compare(Entry first, Entry second) {
            int result = compareEntry(first, second);
            if (result == 0 && EntryWithVerb.class.isAssignableFrom(first.getClass()) &&
                    EntryWithVerb.class.isAssignableFrom(second.getClass())) {
                result = ((EntryWithVerb) first).getVerb().compareTo(((EntryWithVerb) second).getVerb());
            }
            if (result == 0) {
                if (first.equals(second)){
                    return 0;
                }
                throw new IllegalStateException(
                        String.format(
                                "entries received whose compare is equal to 0, this should not happen it will result "
                                        + "in data being lost entry1=%s entry2=%s", first, second));
            }
            return result;
        }
    }

    public static class EntryTreeSet<E extends Entry> extends TreeSet<E> {
        public EntryTreeSet() {
            super(new EntryComparator());
        }
    }

    private static int compareEntry(final Entry first, final Entry second) {
        int r1 = Long.compare(first.getTime().toEpochMilli(), second.getTime().toEpochMilli());
        if (r1 == 0) {
            int r2 = Double.compare(first.getAmount(), second.getAmount());
            if (r2 == 0) {
                int r3 = Double.compare(first.getValue(), second.getValue());
                return r3;
            }
            return r2;
        } else {
            return r1;
        }
    }
}
