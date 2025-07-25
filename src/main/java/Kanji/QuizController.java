package Kanji;

import org.springframework.web.bind.annotation.*;
import jakarta.annotation.PostConstruct;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@CrossOrigin
public class QuizController {

    private static final Map<String, String> levelToFile = Map.of(
        "easy", "/漢字テスト初級.txt",
        "normal", "/漢字テスト中級.txt",
        "hard", "/漢字テスト上級.txt"
    );

    private final Map<String, List<KanjiQuiz>> originalMap = new HashMap<>();
    private final Map<String, List<KanjiQuiz>> quizMap = new HashMap<>();
    private final Map<String, Integer> indexMap = new HashMap<>();

    @PostConstruct
    public void init() {
        for (String level : levelToFile.keySet()) {
            loadFromFile(level);
        }
    }
    
    private static final int QUESTIONS_PER_SESSION = 20;
    private void loadFromFile(String level) {
        List<KanjiQuiz> list = new ArrayList<>();
        String filename = levelToFile.get(level);

        try (InputStream is = getClass().getResourceAsStream(filename);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                String[] arr = line.split(",", 3);
                if (arr.length == 3) {
                    list.add(new KanjiQuiz(arr[0].trim(), arr[1].trim(), arr[2].trim()));
                }
            }
            originalMap.put(level, list);

            // resetQuiz の代わりにここで初期シャッフル＋20問抽出
            List<KanjiQuiz> newQuizList = new ArrayList<>(list);
            Collections.shuffle(newQuizList);
            quizMap.put(level, newQuizList.subList(0, Math.min(QUESTIONS_PER_SESSION, newQuizList.size())));
            indexMap.put(level, 0);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @GetMapping("/kanji")
    public KanjiQuiz getNextKanji(@RequestParam(defaultValue = "normal") String level) {
        List<KanjiQuiz> quizList = quizMap.get(level);
        int idx = indexMap.getOrDefault(level, 0);

        if (quizList == null || idx >= quizList.size()) {
            return null;
        }

        indexMap.put(level, idx + 1);
        return quizList.get(idx);
    }

    @PostMapping("/check")
    public Map<String, Object> checkAnswer(@RequestBody CheckRequest r) {
        Map<String, Object> result = new HashMap<>();
        for (List<KanjiQuiz> list : originalMap.values()) {
            for (KanjiQuiz k : list) {
                if (k.getKanji().equals(r.getKanji())) {
                    boolean correct = k.getReading().equals(r.getAnswer());
                    result.put("correct", correct);
                    result.put("correctReading", k.getReading());
                    result.put("meaning", k.getMeaning());
                    return result;
                }
            }
        }
        result.put("correct", false);
        result.put("correctReading", null);
        result.put("meaning", null);
        return result;
    }

    @PostMapping("/reset")
    public void resetQuiz(@RequestParam(defaultValue = "normal") String level) {
        List<KanjiQuiz> original = originalMap.get(level);
        if (original != null) {
            List<KanjiQuiz> newQuizList = new ArrayList<>(original);
            Collections.shuffle(newQuizList);
            quizMap.put(level, newQuizList.subList(0, Math.min(QUESTIONS_PER_SESSION, newQuizList.size())));
            indexMap.put(level, 0);
        }
    }

    @PostMapping("/finish")
    public Map<String, Object> finishQuiz(@RequestBody Map<String, Integer> scoreData,
                                          @RequestParam(defaultValue = "normal") String level) {
        int score = scoreData.getOrDefault("score", 0);
        int total = scoreData.getOrDefault("total", 0);
        double accuracy = total > 0 ? (score * 100.0 / total) : 0.0;

        String filePath = "data/scores/quizRank_" + level + ".txt";
        Path path = Paths.get(filePath);
        List<Double> scores = new ArrayList<>();

        try {
            if (Files.exists(path)) {
                List<String> lines = Files.readAllLines(path);
                for (String line : lines) {
                    try {
                        scores.add(Double.parseDouble(line.trim()));
                    } catch (NumberFormatException ignored) {}
                }
            } else {
                // フォルダがなければ作る
                Files.createDirectories(path.getParent());
                Files.createFile(path);
            }

            scores.add(accuracy);
            scores.sort(Comparator.reverseOrder());

            int rank = scores.indexOf(accuracy) + 1;
            int totalPlayers = scores.size();

            // 書き込み
            List<String> toWrite = scores.stream()
                    .map(String::valueOf)
                    .collect(Collectors.toList());
            Files.write(path, toWrite, StandardCharsets.UTF_8);

            Map<String, Object> result = new HashMap<>();
            result.put("rank", rank);
            result.put("totalPlayers", totalPlayers);
            return result;

        } catch (IOException e) {
            e.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            error.put("rank", -1);
            error.put("totalPlayers", -1);
            return error;
        }
    }
}
