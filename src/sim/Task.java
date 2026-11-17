package sim;

import java.util.List;

public record Task(int id, char op, int weight, List<Integer> parents) {}