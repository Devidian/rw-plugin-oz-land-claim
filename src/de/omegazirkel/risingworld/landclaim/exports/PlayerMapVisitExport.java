package de.omegazirkel.risingworld.landclaim.exports;

/** Base64-encoded bitmap for one 256 by 256 chunk sector. */
public record PlayerMapVisitExport(int sectorX, int sectorZ, String bitmap, long updatedAtMs) { }
