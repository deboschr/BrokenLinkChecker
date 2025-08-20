package com.unpar.skripsi.model;

public enum Algorithm {
    BFS, DFS;


    public static Algorithm fromLabel(String label) {
        if (label == null) throw new IllegalArgumentException("Algorithm is null");

        String s = label.trim().toUpperCase();

        if (s.contains("BFS")) return BFS;
        if (s.contains("DFS")) return DFS;

        throw new IllegalArgumentException("Unknown algorithm label: " + label);
    }
}
