package uk.gov.rsf.util;

public enum HashingAlgorithm {
    SHA256 {
        @Override
        public String toString() {
            return "sha-256";
        }
    }
}
