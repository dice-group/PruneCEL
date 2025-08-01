package org.dice_research.cel.io.json;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.dice_research.cel.io.LearningProblem;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

/**
 * A simple class used to read learning problems from a JSON file. The file
 * should have the following structure:
 * 
 * <pre>
 * {
 *  "problems": {
 *  "problem-name-1": {
 *    "positive_examples": [
 *      "http://example.org/positive-1",
 *      "http://example.org/positive-2", 
 *    ... ],
 *    "negative_examples": [
 *      "http://example.org/negative-1",
 *      "http://example.org/negative-2", 
 *    ... ]
 *  },
 *  "problem-name-2": {
 *    ...
 *  }}}
 * </pre>
 * 
 * @author Michael R&ouml;der (michael.roeder@uni-paderborn.de)
 *
 */
public class JSONLearningProblemReader {

    public List<LearningProblem> readProblems(String file) throws IOException {
        try (FileReader fReader = new FileReader(file); JsonReader reader = new JsonReader(fReader);) {
            return readProblems(reader);
        }
    }

    public List<LearningProblem> readProblems(JsonReader reader) throws IOException {
        reader.beginObject();
        String key;
        while (reader.hasNext()) {
            key = reader.nextName();
            if ("problems".equals(key)) {
                return readProblemsAfterCheck(reader);
            }
        }
        reader.endObject();
        return null;
    }

    protected List<LearningProblem> readProblemsAfterCheck(JsonReader reader) throws IOException {
        JsonToken token = reader.peek();
        switch (token) {
        case BEGIN_ARRAY: {
            return readProblemsAsArray(reader);
        }
        case BEGIN_OBJECT: {
            return readProblemsAsObjects(reader);
        }
        default:
            throw new IOException("Unexpected JSON token in the \"problems\" object: " + token);
        }
    }

    public List<LearningProblem> readProblemsAsArray(JsonReader reader) throws IOException {
        List<LearningProblem> problems = new ArrayList<>();
        reader.beginArray();
        LearningProblem problem;
        while (reader.hasNext()) {
            problem = readProblem(reader);
            if (problem.getName() == null) {
                problem.setName(Integer.toString(problems.size()));
            }
            problems.add(problem);
        }
        reader.endArray();
        return problems;
    }

    public List<LearningProblem> readProblemsAsObjects(JsonReader reader) throws IOException {
        List<LearningProblem> problems = new ArrayList<>();
        reader.beginObject();
        while (reader.hasNext()) {
            problems.add(readNamedProblem(reader));
        }
        reader.endObject();
        return problems;
    }

    public LearningProblem readNamedProblem(JsonReader reader) throws IOException {
        String name = reader.nextName();
        LearningProblem problem = readProblem(reader);
        problem.setName(name);
        return problem;
    }

    public LearningProblem readProblem(JsonReader reader) throws IOException {
        List<String> positives = null;
        List<String> negatives = null;
        reader.beginObject();
        String key;
        StringBuilder name = new StringBuilder();
        while (reader.hasNext()) {
            key = reader.nextName();
            switch (key) {
            case "positive_examples": // falls through
            case "positives": {
                positives = readStringArray(reader);
                break;
            }
            case "negative_examples": // falls through
            case "negatives": {
                negatives = readStringArray(reader);
                break;
            }
            default: {
                // Use the additional data as name for the learning problem
                name.append(key).append('=').append(reader.nextString()).append(';');
                break;
            }
            }
        }
        reader.endObject();
        return new LearningProblem(positives, negatives, name.length() > 0 ? name.toString() : null);
    }

    public List<String> readStringArray(JsonReader reader) throws IOException {
        List<String> strings = new ArrayList<>();
        reader.beginArray();
        while (reader.hasNext()) {
            strings.add(reader.nextString());
        }
        reader.endArray();
        return strings;
    }

}
