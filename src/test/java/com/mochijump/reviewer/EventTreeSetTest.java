package com.mochijump.reviewer;


import com.mochijump.reviewer.models.OwnershipState;
import org.junit.jupiter.api.Test;

import java.time.Instant;

public class EventTreeSetTest {

    @Test
    public void treeSet_sameTimeStampExists(){
        OwnershipState.EntryTreeSet entryTreeSet = new OwnershipState.EntryTreeSet();
        OwnershipState.Entry entry1 = new OwnershipState.Entry(Instant.parse("2021-04-09T05:02:57Z"), 1D, 1D);
        OwnershipState.Entry entry2 = new OwnershipState.Entry(Instant.parse("2021-04-09T05:02:57Z"), 2D, 2D);
        OwnershipState.Entry entry3 = new OwnershipState.Entry(Instant.parse("2021-04-09T05:02:55Z"), 3D, 3D);
        entryTreeSet.add(entry1);
        entryTreeSet.add(entry2);
        entryTreeSet.add(entry3);
        assert entryTreeSet.size() == 3;
        assert ((OwnershipState.Entry)entryTreeSet.first()).getAmount() == 3D;
    }
}
