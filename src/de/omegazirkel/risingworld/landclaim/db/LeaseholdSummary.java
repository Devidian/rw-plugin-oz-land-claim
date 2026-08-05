package de.omegazirkel.risingworld.landclaim.db;

public record LeaseholdSummary(int total, int occupied, int rented, long dailyRentIncome) {
    public int unoccupied() {
        return Math.max(0, total - occupied);
    }
}
