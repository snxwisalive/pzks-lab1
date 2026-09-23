package parser;

public record AnalysisError(int position, String message) {
    @Override
    public String toString() {
        return "Position %d: %s".formatted(position, message);
    }
}