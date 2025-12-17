package com.example.quiz;

import com.example.quiz.model.Answer;
import com.example.quiz.model.Chapter;
import com.example.quiz.model.Question;
import com.example.quiz.model.User;
import com.example.quiz.repository.AnswerRepository;
import com.example.quiz.repository.ChapterRepository;
import com.example.quiz.repository.QuestionRepository;
import com.example.quiz.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@SpringBootApplication
public class QuizLoginDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(QuizLoginDemoApplication.class, args);
    }

    @Bean
    public CommandLineRunner syncDataFromJson(Environment env,
                                              ResourceLoader resourceLoader,
                                              ObjectProvider<ObjectMapper> objectMapperProvider,
                                              UserRepository userRepository,
                                              ChapterRepository chapterRepository,
                                              QuestionRepository questionRepository,
                                              AnswerRepository answerRepository,
                                              PlatformTransactionManager transactionManager) {
        return args -> {
            boolean enabled = env.getProperty("app.data-sync.enabled", Boolean.class, true);
            if (!enabled) {
                return;
            }

            String location = env.getProperty("app.data-sync.location", "classpath:data.json");
            Resource resource = resourceLoader.getResource(location);
            if (!resource.exists()) {
                throw new IllegalStateException("data sync resource not found: " + location);
            }

            ObjectMapper objectMapper = objectMapperProvider.getIfAvailable(ObjectMapper::new);

            new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                JsonNode root;
                try (InputStream is = resource.getInputStream()) {
                    root = objectMapper.readTree(is);
                } catch (Exception e) {
                    throw new IllegalStateException("failed to read data sync json: " + location, e);
                }

                Map<String, String> desiredUsers = new LinkedHashMap<>();
                JsonNode usersNode = root.path("users");
                if (usersNode.isArray()) {
                    for (JsonNode u : usersNode) {
                        String username = u.path("username").asText(null);
                        String password = u.path("password").asText(null);
                        if (username != null && !username.isBlank()) {
                            desiredUsers.put(username, password == null ? "" : password);
                        }
                    }
                }

                Map<String, User> existingUsersByUsername = userRepository.findAll().stream()
                        .filter(u -> u.getUsername() != null)
                        .collect(Collectors.toMap(User::getUsername, u -> u, (a, b) -> a, LinkedHashMap::new));

                Set<String> desiredUsernames = desiredUsers.keySet();
                for (User existing : new ArrayList<>(existingUsersByUsername.values())) {
                    if (!desiredUsernames.contains(existing.getUsername())) {
                        List<Answer> answers = answerRepository.findByUser(existing);
                        if (!answers.isEmpty()) {
                            answerRepository.deleteAll(answers);
                        }
                        userRepository.delete(existing);
                    }
                }

                for (Map.Entry<String, String> e : desiredUsers.entrySet()) {
                    User user = existingUsersByUsername.get(e.getKey());
                    if (user == null) {
                        user = new User(e.getKey(), e.getValue());
                    } else {
                        user.setPassword(e.getValue());
                    }
                    userRepository.save(user);
                }

                Map<Integer, Chapter> existingChaptersBySortOrder = chapterRepository.findAll().stream()
                        .filter(c -> c.getSortOrder() != null)
                        .collect(Collectors.toMap(Chapter::getSortOrder, c -> c, (a, b) -> a, HashMap::new));

                Map<String, Question> existingQuestionsByTitle = questionRepository.findAll().stream()
                        .filter(q -> q.getTitle() != null && !q.getTitle().isBlank())
                        .collect(Collectors.toMap(Question::getTitle, q -> q, (a, b) -> a, HashMap::new));

                Set<Integer> desiredChapterOrders = new HashSet<>();
                Set<String> desiredQuestionTitles = new HashSet<>();

                JsonNode chaptersNode = root.path("chapters");
                if (chaptersNode.isArray()) {
                    int chapterIndex = 1;
                    for (JsonNode c : chaptersNode) {
                        int chapterOrder = chapterIndex++;
                        desiredChapterOrders.add(chapterOrder);

                        String title = c.path("title").asText("");
                        Chapter chapter = existingChaptersBySortOrder.get(chapterOrder);
                        if (chapter == null) {
                            chapter = new Chapter();
                        }
                        chapter.setTitle(title);
                        chapter.setDescription(null);
                        chapter.setSortOrder(chapterOrder);
                        chapter = chapterRepository.save(chapter);

                        JsonNode questionsNode = c.path("questions");
                        if (questionsNode.isArray()) {
                            int questionIndex = 1;
                            for (JsonNode q : questionsNode) {
                                int questionOrder = questionIndex++;
                                String questionNumber = chapterOrder + "." + questionOrder;

                                String qTitle = q.path("title").asText("");
                                desiredQuestionTitles.add(qTitle);
                                
                                String qDescription = q.path("description").asText("");
                                double totalScore = q.path("total_score").asDouble(0.0);

                                Question question = existingQuestionsByTitle.get(qTitle);
                                if (question == null) {
                                    question = new Question();
                                    question.setTitle(qTitle);
                                }
                                question.setQuestionNumber(questionNumber);
                                question.setDescription(qDescription);
                                question.setTotalScore(totalScore);
                                question.setSortOrder(questionOrder);
                                question.setChapter(chapter);
                                questionRepository.save(question);
                            }
                        }
                    }
                }

                List<Question> extraQuestions = questionRepository.findAll().stream()
                        .filter(q -> q.getTitle() == null || !desiredQuestionTitles.contains(q.getTitle()))
                        .collect(Collectors.toList());
                if (!extraQuestions.isEmpty()) {
                    List<Answer> answersToDelete = new ArrayList<>();
                    for (Question q : extraQuestions) {
                        answersToDelete.addAll(answerRepository.findByQuestion(q));
                    }
                    if (!answersToDelete.isEmpty()) {
                        answerRepository.deleteAll(answersToDelete);
                    }
                    questionRepository.deleteAll(extraQuestions);
                }

                List<Chapter> extraChapters = chapterRepository.findAll().stream()
                        .filter(ch -> ch.getSortOrder() == null || !desiredChapterOrders.contains(ch.getSortOrder()))
                        .collect(Collectors.toList());
                if (!extraChapters.isEmpty()) {
                    chapterRepository.deleteAll(extraChapters);
                }
            });
        };
    }
}
