package com.glvov.springairag.advisor;

import lombok.Builder;
import lombok.Getter;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.util.StringUtils;

import java.util.Map;

import static com.glvov.springairag.utils.FileLoader.loadFile;

/**
 * This advisor enriches the user's original query with additional
 * domain-relevant terms to improve the accuracy of RAG (Retrieval-Augmented Generation).
 * <br><br>
 * WHY THIS IS NEEDED:
 * <br><br>
 * RAG sometimes returns chunks that are semantically weak or irrelevant,
 * even if more appropriate chunks exist. This happens when the similarity
 * search scores a generic chunk unexpectedly high.
 * <br><br>
 * Example:
 * Imagine the vector store contains extensive technical documentation about
 * a developer named John Smith and his achievements, split into chunks.
 * One of the chunks contains the information: "John Smith is allergic to flowers."
 * Another chunk contains irrelevant text like: "Nothing here yet."
 * <br>
 * If the user asks: "What makes John Smith sneeze?", the RAG system may incorrectly assign
 * a high similarity score to the irrelevant chunk ("Nothing here yet"), causing it to be selected
 * instead of the useful one.
 * <br><br>
 * HOW THIS ADVISOR HELPS: <br><br>
 * The advisor expands the user query by adding contextually important keywords.
 * It uses its own dedicated ChatClient instance to generate the final enriched query,
 * ensuring deterministic, isolated behavior regardless of the main LLM settings.
 * <br><br>
 * Example:
 * <br>
 * Input query: "What makes John Smith sneeze?"
 * <br>
 * Expanded query sent to the RAG retriever: "What makes John Smith sneeze allergy medicine immunity"
 * <br><br>
 * Because the enriched query includes the term "allergy",
 * the correct chunk (“... allergic to flowers”) receives a higher similarity score.
 * <br>
 * <br>
 * BENEFITS:
 * - The correct chunk becomes more likely to be included in RAG top-K results.
 * - If, for example, the {@link SearchRequest.Builder#topK(int)} is configured with value 3,
 * a relevant chunk that might otherwise rank 4th can now rank higher
 * and be included in the final LLM context.
 * <br>
 * <br>
 * NOTE:
 * This expansion is used ONLY for similarity search score improvement for relevant docs in RAG.
 * The LLM still receives the original user question as-is.
 * The advisor uses its own chat client to get the final version of enriched question
 */
@Builder
public class ExpansionQueryAdvisor implements BaseAdvisor {

    public static final String ENRICHED_QUESTION = "ENRICHED_QUESTION";

    private static final String ORIGINAL_QUESTION = "ORIGINAL_QUESTION";

    private static final String EXPANSION_QUERY_PROMPT = loadFile("/ai/prompts/expansion_query.txt");

    @Getter
    private final int order;

    private final ChatClient chatClient;


    public static ExpansionQueryAdvisorBuilder builder(ChatClient chatClient) {
        return new ExpansionQueryAdvisorBuilder().chatClient(chatClient);
    }

    @Override
    public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
        String userQuestion = chatClientRequest.prompt().getUserMessage().getText();
        String enrichedQuestion = chatClient
                .prompt()
                .user(new PromptTemplate(EXPANSION_QUERY_PROMPT).render(Map.of("question", userQuestion)))
                .call()
                .content();

        if (!StringUtils.hasText(enrichedQuestion)) {
            return chatClientRequest;
        }

        return chatClientRequest.mutate()
                .context(ENRICHED_QUESTION, enrichedQuestion)
                .context(ORIGINAL_QUESTION, userQuestion) // for logging purposes
                .build();
    }

    @Override
    public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
        return chatClientResponse;
    }
}
