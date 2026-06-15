package com.chatbot.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import opennlp.tools.doccat.DoccatFactory;
import opennlp.tools.doccat.DoccatModel;
import opennlp.tools.doccat.DocumentCategorizerME;
import opennlp.tools.doccat.DocumentSample;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.ObjectStreamUtils;
import opennlp.tools.util.TrainingParameters;

import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class NlpService {

    private final GroqService groqService;

    @Autowired
    public NlpService(GroqService groqService) {
        this.groqService = groqService;
    }

    private DocumentCategorizerME categorizer;
    private TokenizerME tokenizer;
    private final Map<String, List<String>> intentResponses = new HashMap<>();
    private final Random random = new Random();

    // Regex patterns for strict matching (Fast-track rule engine)
    private final Map<Pattern, String> ruleEngine = new LinkedHashMap<>();

    @PostConstruct
    public void init() {
        loadRules();
        loadTokenizer();
        trainModel();
    }

    private void loadRules() {
        // Strict command matching examples
        ruleEngine.put(Pattern.compile("^/help$", Pattern.CASE_INSENSITIVE), "Here are some commands you can use: /help, /clear, /version.");
        ruleEngine.put(Pattern.compile("^/version$", Pattern.CASE_INSENSITIVE), "AI Chatbot version 1.0.0");
        ruleEngine.put(Pattern.compile("^/clear$", Pattern.CASE_INSENSITIVE), "Clearing the chat history is handled by the client.");
    }

    private void loadTokenizer() {
        try {
            InputStream is = new ClassPathResource("models/en-token.bin").getInputStream();
            TokenizerModel model = new TokenizerModel(is);
            this.tokenizer = new TokenizerME(model);
            System.out.println("Loaded OpenNLP Tokenizer model.");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to load tokenizer model.");
        }
    }

    private void trainModel() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            InputStream is = new ClassPathResource("faq-data.json").getInputStream();
            List<Map<String, Object>> faqData = mapper.readValue(is, new TypeReference<>() {});

            List<DocumentSample> samples = new ArrayList<>();

            for (Map<String, Object> item : faqData) {
                String intent = (String) item.get("intent");
                List<String> patterns = (List<String>) item.get("patterns");
                List<String> responses = (List<String>) item.get("responses");

                intentResponses.put(intent, responses);

                for (String pattern : patterns) {
                    String[] tokens = normalize(pattern);
                    samples.add(new DocumentSample(intent, tokens));
                }
            }

            ObjectStream<DocumentSample> sampleStream = ObjectStreamUtils.createObjectStream(samples);

            TrainingParameters params = new TrainingParameters();
            params.put(TrainingParameters.ITERATIONS_PARAM, 100);
            params.put(TrainingParameters.CUTOFF_PARAM, 0);

            DoccatModel model = DocumentCategorizerME.train("en", sampleStream, params, new DoccatFactory());
            this.categorizer = new DocumentCategorizerME(model);

            System.out.println("NLP Model trained successfully with " + samples.size() + " samples.");

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to train NLP Model");
        }
    }

    public String processAndRespond(String input) {
        // 1. Fast-Track Rule-Based Verification
        String ruleMatch = checkRules(input);
        if (ruleMatch != null) {
            return ruleMatch;
        }

        // 2. Normalization
        String[] tokens = normalize(input);

        // 3. Machine Learning Classification
        if (categorizer != null) {
            double[] probabilities = categorizer.categorize(tokens);
            String bestIntent = categorizer.getBestCategory(probabilities);

            double maxProb = 0;
            for (double p : probabilities) {
                if (p > maxProb) maxProb = p;
            }

            // Only trust the classifier if it is extremely confident
            // and the matched intent is not the generic fallback bucket
            if (maxProb >= 0.85 && !"unknown".equals(bestIntent)) {
                return getRandomResponse(bestIntent);
            }
        }

        // 4. Groq API — the intelligent fallback for all factual/general questions
        return groqService.ask(input);
    }

    private String[] normalize(String input) {
        String lowerInput = input.toLowerCase().replaceAll("[^a-z0-9\\s]", "");
        if (tokenizer != null) {
            return tokenizer.tokenize(lowerInput);
        }
        return lowerInput.split("\\s+");
    }

    private String checkRules(String input) {
        for (Map.Entry<Pattern, String> entry : ruleEngine.entrySet()) {
            Matcher matcher = entry.getKey().matcher(input.trim());
            if (matcher.matches()) {
                return entry.getValue();
            }
        }
        return null;
    }

    private String getRandomResponse(String intent) {
        List<String> responses = intentResponses.get(intent);
        if (responses != null && !responses.isEmpty()) {
            return responses.get(random.nextInt(responses.size()));
        }
        return "I have no response for that intent.";
    }
}
