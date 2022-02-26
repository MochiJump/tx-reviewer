package com.mochijump.reviewer.interfaces;

import com.mochijump.reviewer.models.OwnershipState;

import java.time.Instant;
import java.util.Optional;

public interface StateMaintainer {
    void addValueToFluxState(final String name, final Instant instant, final Double value, final Double amount);
    void addValueToMaster(final String name, final Instant instant, final Double value, final Double amount);
    void addValueToMaster(final String name, final Instant instant, final Double value, final Double amount, final Optional<Boolean> isLp);
    void addLossToMaster(final String name, final Instant instant, final Double amount);
    void addLossToMaster(final String name, final Instant instant, final Double amount, final Optional<Boolean> isLp);
    void cleanupState();
    void finalizeMasterToInFlux();
    OwnershipState getOwnershipState();
    void loadStateFromFile(final Optional<String> path);
    void removeValueFromFluxState(final String name, final Double amountSold, final Double value, final Instant timeofSale);
    void removeValueFromMaster(final String name, final Instant instant, final Double amountSold, final Double value);
    void saveCurrentState(final OwnershipState state);
    void addZeroCostBasis(final String name, final Instant instant, final Double value);
}
